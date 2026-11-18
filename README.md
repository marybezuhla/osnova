# Основа · Osnova

Конституція України для Android: чинна редакція українською й офіційний переклад англійською, без інтернету. Безкоштовно, без реклами й акаунтів. Потрібен Android 8 або новіший (64-бітний ARM, як майже в усіх телефонах).

> Відгуки й помилки: [Issues](../../issues).

## Особливості

- 📜 **Слово в слово.** Текст збирається із закріплених джерел (сайт Верховної Ради й офіційний переклад), і збірка перевіряє, що в ньому нічого не змінено вручну.
- 🔎 **Пошук** обома мовами, з різними формами слів; «ст. 24» чи «розділ XV» відкривають статтю чи розділ одразу.
- 💬 **Цитати з посиланням, як у юристів:** уся стаття, вибрані частини або виділений текст — «Частина друга статті 85 Конституції України».
- 🔊 **Читання вголос** без інтернету жіночими голосами Основи (українською з правильними наголосами й англійською) або голосом телефона; читає й з вимкненим екраном.
- 🔖 **Закладки** й «Продовжити» з того місця, де ви зупинилися; мову тексту можна змінити на будь-якій статті.
- 🎨 **Теми.** Світла чи темна, пʼять денних і пʼять нічних тем або кольори телефона (Android 12+); типово Небо й Зоряна ніч.
- ♿ **Доступність:** TalkBack, великий шрифт, мова тексту позначена для голосу.

## Встановлення

- 📲 **Напряму:** завантажте файл `.apk` на телефоні звідси: [github.com/marybezuhla/osnova/releases/latest](https://github.com/marybezuhla/osnova/releases/latest) і відкрийте його.
- 🔄 **З оновленнями:** скористайтеся безкоштовним застосунком Obtainium. Його можна встановити з F-Droid: [f-droid.org/packages/dev.imranr.obtainium.fdroid](https://f-droid.org/packages/dev.imranr.obtainium.fdroid) (F-Droid — це безкоштовний каталог застосунків для Android, щось на зразок Google Play. Усі застосунки там мають відкритий код, без реклами й стеження, а перед публікацією їх перевіряють і збирають самі укладачі каталогу). В Obtainium натисніть «Додати застосунок» і вставте посилання `github.com/marybezuhla/osnova`.
- 🔊 **Голоси (необовʼязково):** Читання вголос → «Завантажити» (152 і 64 МБ) → «Вибрати файл». Файл перевіряється за контрольною сумою. Голос, що вже є в Основі, можна передати тому, хто без інтернету («Поділитися файлом»).

## Прозорість

- **Дозволи** лише для служби на передньому плані, щоб читати вголос із вимкненим екраном. Збірка зупиняється, якщо зʼявляється дозвіл на інтернет.
- **Підпис:** справжній APK підписано сертифікатом із відбитком SHA-256 `6F:49:52:46:34:98:17:B9:60:BF:82:14:AC:4A:B3:83:19:35:CA:BE:6E:C3:81:B7:99:D7:60:D9:40:85:22:4A`. Перевірити: `apksigner verify --print-certs osnova-<версія>.apk` (з Android SDK Build Tools) має показати саме цей відбиток.
- **Збірка:** APK збирається з цього коду в GitHub Actions і підписується лише після схвалення авторки. Тексти — `scripts/text/`.
- **Як створено:** код написано за допомогою ШІ (Claude, Anthropic) під керівництвом і з тестуванням авторки.
- **Застереження:** це не юридична консультація. Офіційний текст: [zakon.rada.gov.ua](https://zakon.rada.gov.ua/laws/show/254к/96-вр).

Kotlin · Jetpack Compose · ONNX Runtime · eSpeak NG. Ліцензія GNU GPL версії 3 або (на ваш вибір) будь-якої пізнішої (GPL-3.0-or-later). Бібліотеки й тексти — [NOTICE.md](NOTICE.md), приватність — [PRIVACY.md](PRIVACY.md), вразливості — [SECURITY.md](SECURITY.md).

---

# Osnova · English

The Constitution of Ukraine for Android: the current edition in Ukrainian and the official English translation, offline. Free, no ads, no accounts. Requires Android 8 or newer (64-bit ARM, as almost every phone has).

> Feedback and bugs: [Issues](../../issues).

## Features

- 📜 **Word for word.** The text is built from pinned sources (the Verkhovna Rada's site and the official translation), and the build checks nothing in it was changed by hand.
- 🔎 **Search** in both languages, with word forms; "article 24" or "chapter XV" opens the article or chapter at once.
- 💬 **Quotes cited as lawyers cite them:** the whole article, chosen parts or selected text — "Article 85(2) of the Constitution of Ukraine".
- 🔊 **Reading aloud** offline in Osnova's female voices (Ukrainian with the right stresses, and English) or the phone's voice; it reads with the screen off too.
- 🔖 **Bookmarks** and "Continue reading" from where you stopped; the text's language can be switched on any article.
- 🎨 **Themes.** Light or dark, five day and five night themes or the phone's colours (Android 12+); Sky and Starry night by default.
- ♿ **Accessibility:** TalkBack, large fonts, the text's language marked for the voice.

## Install

- 📲 **Directly:** on your phone, download the `.apk` file from [github.com/marybezuhla/osnova/releases/latest](https://github.com/marybezuhla/osnova/releases/latest) and open it.
- 🔄 **With updates:** use the free app Obtainium, available on F-Droid: [f-droid.org/packages/dev.imranr.obtainium.fdroid](https://f-droid.org/packages/dev.imranr.obtainium.fdroid) (F-Droid is a free catalogue of Android apps, a bit like Google Play: every app there is open source, without ads or tracking, and is checked and built by the catalogue's maintainers before publishing). In Obtainium tap "Add App" and paste `github.com/marybezuhla/osnova`.
- 🔊 **Voices (optional):** Reading aloud → Download (152 and 64 MB) → Choose file. The file is checked by its checksum. A voice already in Osnova can be passed on to someone without internet (Share file).

## Transparency

- **Permissions** only for a foreground service, to read aloud with the screen off. The build stops if an internet permission appears.
- **Signature:** the genuine APK is signed with a certificate whose SHA-256 fingerprint is `6F:49:52:46:34:98:17:B9:60:BF:82:14:AC:4A:B3:83:19:35:CA:BE:6E:C3:81:B7:99:D7:60:D9:40:85:22:4A`. To check it: `apksigner verify --print-certs osnova-<version>.apk` (from the Android SDK Build Tools) must show this fingerprint.
- **Build:** the APK is built from this code in GitHub Actions and signed only after the author's approval. Texts: `scripts/text/`.
- **How it was made:** the code was written with AI (Claude, Anthropic), directed and tested by the author.
- **Disclaimer:** this is not legal advice. The official text: [zakon.rada.gov.ua](https://zakon.rada.gov.ua/laws/show/254к/96-вр).

Kotlin · Jetpack Compose · ONNX Runtime · eSpeak NG. Licence: GNU GPL version 3 or (at your option) any later version (GPL-3.0-or-later). Libraries and texts: [NOTICE.md](NOTICE.md), privacy: [PRIVACY.md](PRIVACY.md), vulnerabilities: [SECURITY.md](SECURITY.md).
