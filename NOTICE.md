# Сторонні бібліотеки й тексти · Third-party libraries and texts

Основа поширюється за ліцензією GPL-3.0-or-later ([LICENSE](LICENSE)). У застосунок входять такі бібліотеки (усі — Apache License 2.0):

Osnova is distributed under GPL-3.0-or-later ([LICENSE](LICENSE)). The app includes these libraries (all under the Apache License 2.0):

| Бібліотека · Library | Автор · Author | Ліцензія · Licence |
|---|---|---|
| Jetpack Compose (UI, Foundation, Runtime, Animation) | Google (The Android Open Source Project) | Apache-2.0 |
| Compose Material 3 | Google (The Android Open Source Project) | Apache-2.0 |
| AndroidX Core, Activity, Lifecycle, SavedState, Annotation, Collection та інші бібліотеки AndroidX, від яких вони залежать · and other AndroidX libraries they depend on | Google (The Android Open Source Project) | Apache-2.0 |
| Kotlin standard library | JetBrains | Apache-2.0 |
| kotlinx.coroutines | JetBrains | Apache-2.0 |
| Значки · Icons (search, close, bookmark, back, settings) | Google, Material Icons | Apache-2.0 |

Повний текст Apache License 2.0: https://www.apache.org/licenses/LICENSE-2.0

Точний перелік із версіями й контрольними сумами — у [gradle/verification-metadata.xml](gradle/verification-metadata.xml) та [app/build.gradle.kts](app/build.gradle.kts). · The exact list with versions and checksums: [gradle/verification-metadata.xml](gradle/verification-metadata.xml) and [app/build.gradle.kts](app/build.gradle.kts).

Лише для тестів (не входять у застосунок) · Tests only (not in the app): JUnit (EPL-1.0), Hamcrest (BSD-3-Clause), Robolectric (MIT), AndroidX Test (Apache-2.0), JSON-java (Public Domain).

## Тексти · Texts

- `app/src/main/assets/constitution-uk.json` (з `scripts/text/constitution_uk.py`) — Конституція України від 28.06.1996 № 254к/96-ВР, редакція від 01.01.2020, слово в слово з [zakon.rada.gov.ua](https://zakon.rada.gov.ua/laws/show/254к/96-вр) (джерело: `scripts/text/source/constitution-uk-2020-01-01.txt`, звірено з сайтом 17.11.2026). · The Constitution of Ukraine, edition of 01.01.2020, word for word from zakon.rada.gov.ua.
- `app/src/main/assets/constitution-en.json` (з `scripts/text/constitution_en.py`) — офіційний переклад англійською, опублікований Верховною Радою ([zakonst.rada.gov.ua/docs/en/constitution.pdf](https://zakonst.rada.gov.ua/docs/en/constitution.pdf), «Офіційний переклад – Сергій Тюпа, 18.05.2026»; джерело: `scripts/text/source/constitution-en-2026-05-18.pdf`, SHA-256 `5a22dd3546c1a00b7761598d01670ef4fedce1f99c3c979eb39040f0a36d37d5`). · The official English translation published by the Verkhovna Rada.
  - У PDF є кілька друкарських вад (загублені надрядкові цифри: «Article 1481» замість 148-1, «131-1312» замість 131-2, «161» замість 16-1, «9-92» замість 9-2, «149-1491» замість 149-1, два неправильні номери статей у примітках, пропущена крапка в назві розділу VI). Їх виправлено явним списком у `scripts/text/constitution_en.py`, кожне звірено з українським текстом. · The PDF has a few misprints (lost superscripts, two wrong article numbers in notes, a missing full stop in the title of Chapter VI); they are put right by an explicit list in `scripts/text/constitution_en.py`, each checked against the Ukrainian text.

Офіційні документи та їхні офіційні переклади не охороняються авторським правом (Закон України «Про авторське право і суміжні права» № 2811-IX, ст. 8, ч. 1, п. 3). · Official documents and their official translations are not protected by copyright (Law of Ukraine No. 2811-IX, art. 8(1)(3)). Джерело · Source: Верховна Рада України · Verkhovna Rada of Ukraine.
