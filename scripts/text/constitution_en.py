#!/usr/bin/env python3
"""
The Constitution of Ukraine in English, as one JSON file in the same shape as the Ukrainian one:
the preamble, the chapters and every article word for word, with the editorial notes kept apart.

Input: source/constitution-en-2026-05-18.pdf, the official English translation published by the
Verkhovna Rada (zakonst.rada.gov.ua/docs/en/constitution.pdf, "Офіційний переклад", 18.05.2026),
for the same edition as the Ukrainian text (amendments up to Law No. 27-IX of 03.09.2019).
Only the layout is read from it: a paragraph starts where the PDF indents the first line.
The two misprints of the PDF itself are put right in ERRATA, each checked against the Ukrainian text.

    python3 scripts/text/constitution_en.py  ->  app/src/main/assets/constitution-en.json
Needs pdftotext (poppler-utils).
"""
import hashlib, html, json, os, re, subprocess, sys

HERE = os.path.dirname(os.path.abspath(__file__))
SRC = os.path.join(HERE, "source", "constitution-en-2026-05-18.pdf")
SRC_SHA256 = "5a22dd3546c1a00b7761598d01670ef4fedce1f99c3c979eb39040f0a36d37d5"
OUT = os.path.join(HERE, "..", "..", "app", "src", "main", "assets", "constitution-en.json")

# Misprints of the PDF itself, each checked against the Ukrainian text and the PDF's own pages. Most are
# lost superscripts (148-1 printed as 1481, 131-2 as 131-1312). Each must match exactly once.
# Before the text is cut into articles: a misprinted article number and a chapter title without its full stop
# (the Ukrainian title is "КАБІНЕТ МІНІСТРІВ УКРАЇНИ. ІНШІ ОРГАНИ ВИКОНАВЧОЇ ВЛАДИ").
ERRATA = [
    ("Article 1481. The State ensures funding", "Article 148-1. The State ensures funding"),
    ("CABINET OF MINISTERS OF UKRAINE OTHER BODIES OF", "CABINET OF MINISTERS OF UKRAINE. OTHER BODIES OF"),
]
# Inside one article (by number) or one chapter's own paragraphs ("XV"), with the Ukrainian words they follow
ERRATA_AT = [
    ("116", "clause 9-92 pursuant", "clause 9-2 pursuant"),                    # "пунктом 9-2"
    ("148-1", "with Article 131-2 pursuant", "with Article 148-1 pursuant"),   # "статтею 148-1"
    ("151-2", "with Article 151-1 pursuant", "with Article 151-2 pursuant"),   # "статтею 151-2"
    ("XV", "by Article 149-1491 of the Constitution", "by Article 149-1 of the Constitution"),  # "статтею 149-1"
    ("XV", "and Article 131-1312 of this Constitution", "and Article 131-2 of this Constitution"),  # "статті 131-2"
    ("XV", "161. From the day of entry into force", "16-1. From the day of entry into force"),  # "16-1."
    ("XV", "with paragraph 161 pursuant", "with paragraph 16-1 pursuant"),     # "пунктом 16-1"
]

CHAPTER = re.compile(r"^Chapter ([IVXL]+)$")
ARTICLE = re.compile(r"^Article (\d{1,3}(?:-\d)?)\.\s*(.*)$")
INDENT = 85.0      # first lines of paragraphs start at x = 93; other lines at x = 70
HEADING_H = 12.6   # chapter numbers and titles are set larger and bold


def lines_of(pdf):
    out = subprocess.run(["pdftotext", "-bbox-layout", pdf, "-"], check=True, capture_output=True, text=True).stdout
    for line in re.findall(r"<line [^>]*>(.*?)</line>", out, re.S):
        words = re.findall(r'<word xMin="([\d.]+)" yMin="([\d.]+)" xMax="[\d.]+" yMax="([\d.]+)">([^<]*)</word>', line)
        if not words:
            continue
        x = float(words[0][0])
        heading = all(abs(float(w[2]) - float(w[1]) - HEADING_H) < 0.05 for w in words)
        yield x, heading, html.unescape(" ".join(w[3] for w in words)).strip()


def paragraphs(pdf):
    """Joins the PDF's lines into paragraphs; headings stay lines of their own."""
    para = None
    for x, heading, text in lines_of(pdf):
        if heading or text == "***":
            if para:
                yield para
            para = None
            yield ("H" if heading else "") + "\x00" + text
            continue
        starts = x >= INDENT or para is None or text.startswith("{") and para.endswith("}")
        if starts:
            if para:
                yield para
            para = text
        else:
            # A hyphenated word broken at the end of a line ("self-" / "government") stays together,
            # hyphen kept: the PDF breaks only at real hyphens. Other lines join with a space.
            para = para + ("" if para.endswith("-") else " ") + text
    if para:
        yield para


def main():
    if hashlib.sha256(open(SRC, "rb").read()).hexdigest() != SRC_SHA256:
        sys.exit("The source PDF is not the pinned one")
    items = list(paragraphs(SRC))
    joined = "\x01".join(items)
    for wrong, right in ERRATA:
        if joined.count(wrong) != 1:
            sys.exit(f"Erratum not found exactly once: {wrong}")
        joined = joined.replace(wrong, right)
    items = joined.split("\x01")

    start = next(i for i, p in enumerate(items) if p.startswith("The Verkhovna Rada of Ukraine, on behalf of"))
    preamble, sections, closing, translation = [], [], [], None
    current_section, current_article = None, None
    i = start
    while i < len(items):
        p = items[i]
        i += 1
        kind, _, text = p.partition("\x00") if "\x00" in p else ("", "", p)
        text = re.sub(r"\s+", " ", text).strip()
        if not text:
            continue
        if text == "***":
            for rest in items[i:]:
                t = re.sub(r"\s+", " ", rest.partition("\x00")[2] if "\x00" in rest else rest).strip()
                if t.startswith("Офіційний переклад"):
                    translation = t
                elif t:
                    closing.append(t)
            break
        if kind != "H" and CHAPTER.match(text):
            kind = "H"
        elif kind != "H" and current_section is not None and not current_section["title"] and text.isupper():
            kind = "H"
        if kind == "H":
            m = CHAPTER.match(text)
            if m:
                current_section = {"num": m.group(1), "title": "", "articles": [], "text": [], "notes": []}
                current_article = None
                sections.append(current_section)
            elif current_section is not None:
                current_section["title"] = (current_section["title"] + " " + text).strip()
            else:
                sys.exit(f"A heading outside a chapter: {text}")
            continue
        if text.startswith("{") and text.endswith("}"):
            note = text[1:-1].strip()
            target = current_article if current_article is not None else current_section
            if target is not None:
                target["notes"].append(note)
            else:
                preamble.append({"note": note})
            continue
        m = ARTICLE.match(text)
        if m and current_section is not None:
            current_article = {"num": m.group(1), "parts": [m.group(2)] if m.group(2) else [], "notes": []}
            current_section["articles"].append(current_article)
            continue
        if current_section is None:
            preamble.append({"text": text})
        elif current_article is not None:
            current_article["parts"].append(text)
        else:
            current_section["text"].append(text)

    for where, wrong, right in ERRATA_AT:
        lists = [a for s in sections for a in s["articles"] if a["num"] == where] or [s for s in sections if s["num"] == where]
        if len(lists) != 1:
            sys.exit(f"No single place {where} for an erratum")
        hits = [(key, i) for key in ("parts", "notes", "text") for i, t in enumerate(lists[0].get(key, [])) if wrong in t]
        if len(hits) != 1 or sum(lists[0][k][j].count(wrong) for k, j in hits) != 1:
            sys.exit(f"Erratum not found exactly once in {where}: {wrong}")
        key, i = hits[0]
        lists[0][key][i] = lists[0][key][i].replace(wrong, right)

    nums = [a["num"] for s in sections for a in s["articles"]]
    if len(nums) != len(set(nums)):
        sys.exit("Article numbers repeat")
    if any(not a["parts"] for s in sections for a in s["articles"]):
        sys.exit("An article without text")
    if len(sections) != 14 or sections[-1]["num"] != "XV" or not sections[-1]["text"]:
        sys.exit("Unexpected chapters")
    if not translation:
        sys.exit("The translation's credit line is missing")

    out = {
        "title": "Constitution of Ukraine",
        "act": "Constitution of Ukraine of 28.06.1996 No. 254к/96-ВР",
        "edition": "As amended up to Law No. 27-IX of 03.09.2019 (in force from 01.01.2020)",
        "source": "https://zakonst.rada.gov.ua/docs/en/constitution.pdf",
        "translation": translation,
        "license": "Official translation of an official document; source: Verkhovna Rada of Ukraine",
        "preamble": preamble,
        "sections": sections,
        "closing": closing,
    }
    with open(OUT, "w", encoding="utf-8") as f:
        json.dump(out, f, ensure_ascii=False, separators=(",", ":"))
        f.write("\n")
    print(f"Constitution (en): {len(sections)} chapters, {len(nums)} articles")


if __name__ == "__main__":
    main()
