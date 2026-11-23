# Fork notes

**English summary.** [bezeza/Supernova](https://github.com/bezeza/Supernova) is a fork of [tanujnotes/Supernova](https://github.com/tanujnotes/Supernova). Fork `main` is already fast-forwarded to upstream v0.2.8, commit `5b63c43`. Keep it that way. Fork-only features live on `translations-uk-ru-odesa` (not on `main`). CI, Dependabot, and the Gradle wrapper checksum are in open [PR #2](https://github.com/bezeza/Supernova/pull/2) (check `build` is green, waiting for merge). Do not add those files again. New fork behavior belongs in `com.supernova.launcher.fork`, entered by one call, `ForkHooks.attach(this)`, at the end of `LauncherActivity.onCreate`. That package and call are **not implemented yet**. Do not change `namespace`, `applicationId`, or the icon-pack intent action and extra strings. The map of `LauncherActivity.kt` is [docs/architecture/LauncherActivity-map.md](docs/architecture/LauncherActivity-map.md). Line numbers there are for `5b63c43` and will drift.

## Що це за форк

Репозиторій `bezeza/Supernova` — форк `tanujnotes/Supernova` (GitHub: `fork: true`, `parent: tanujnotes/Supernova`). Застосунок той самий: Android-лаунчер Supernova, Kotlin, пакет `com.supernova.launcher`.

`main` форка не містить власних комітів. Його тримають дзеркалом upstream, щоб підтягувати щоденні зміни Tanuj без конфліктів. Усе, що належить лише форку (переклади, свої фічі, CI), живе на інших гілках, доки це свідомо не змерджено в `main`.

Карта `LauncherActivity.kt` і правила, чого в ній не чіпати: [docs/architecture/LauncherActivity-map.md](docs/architecture/LauncherActivity-map.md).

## Як синкати `main` (лише fast-forward)

> **Застаріло.** `main` уже має власні коміти, тож fast-forward не проходить. Чинна процедура: розділ «Синхронізація з оригіналом (upstream)» нижче.

Перевірено 2026-11-21: `git ls-remote` для `refs/heads/main` у `tanujnotes/Supernova` і `bezeza/Supernova` дає один SHA, `5b63c43b9e0f61a3efd9755bd2f1f1a92a091294`.

```bash
git remote add upstream https://github.com/tanujnotes/Supernova.git   # один раз
git fetch upstream
git checkout main
git merge --ff-only upstream/main
git push origin main
```

`--ff-only` проходить, лише поки в `origin/main` немає комітів, яких немає в upstream. Якщо команда відмовляється, `main` уже не чисте дзеркало: його не переписують force-push-ем і не ребейзять «щоб наздогнати». Тоді рішення за власником репозиторію.

Інші гілки цим кроком не оновлюються. Вони розійшлися з upstream ще на v0.2.4 або v0.2.6 (див. карту нижче) і самі по собі fast-forward до сьогоднішнього `main` не лягають.

## Синхронізація з оригіналом (upstream)

Тут і далі `origin` — це `bezeza/Supernova`, `upstream` — `tanujnotes/Supernova`.

1. **Синхронізуємо регулярно, бажано щотижня.** Чим рідше, тим більше конфліктів: на `LauncherActivity.kt` upstream робить по 10–15 комітів у піковий день.
2. **Fast-forward більше не працює**, бо `main` має власні коміти. Кнопку «Sync fork» на GitHub не використовуємо, якщо вона пропонує discard commits: вона знищила б наші зміни.
3. **Кроки:**

   ```bash
   git remote add upstream https://github.com/tanujnotes/Supernova.git   # один раз
   git fetch upstream
   git checkout -b sync/upstream-YYYY-MM-DD origin/main
   git merge upstream/main
   ```

   Конфлікти розв'язуємо так: **логіка береться з upstream, наші зміни (переклади, `ForkHooks`, CI, docs) зберігаємо.** Найімовірніші місця конфліктів: `LauncherActivity.kt`, `values*/strings.xml`, `build.gradle` (`build.gradle.kts`).
4. **Нові рядки з upstream `values/strings.xml` перекладаємо** через `tools/gen_locale.py`. Джерела перекладів лежать у `tools/uk.txt` і `tools/ru.txt` (формат `ім'я|текст`, множини в чотирьох формах). Новий рядок спершу додаємо туди, а не лише в XML, потім:

   ```bash
   python3 tools/gen_locale.py x tools/uk.txt values-uk
   python3 tools/gen_locale.py x tools/ru.txt values-ru
   ```

   Скрипт повідомляє про відсутні й зайві рядки та про розбіжності плейсхолдерів (`theme_schedule_range` і `notification_access_restricted` дають відомі хибні попередження про `\u` — їх можна ігнорувати).
5. **Пушимо гілку й відкриваємо PR у `bezeza/Supernova`** (НЕ в `tanujnotes`). Чекаємо, поки CI (lint, detekt, assembleRelease) стане зеленим. Локально перед пушем: `./gradlew testDebugUnitTest detekt lintDebug assembleDebug`.
6. **Мерджимо лише після OK власника.** Перед мерджем вручну перевіряємо головне за [docs/testing/manual-test-plan.md](docs/testing/manual-test-plan.md).
7. **Нічого не пушимо в upstream `tanujnotes/Supernova`.** Ключі й секрети не комітимо (`keystore.properties`, `*.jks`, `*.keystore` уже в `.gitignore`).

## Карта гілок

Станом на 2026-11-21, порівняння GitHub `main...гілка` (`ahead` — коміти гілки, яких немає в `main`; `behind` — коміти `main`, яких немає в гілці).

| Гілка | Кінець | Відносно `main` | База | Призначення |
|---|---|---|---|---|
| `main` | `5b63c43` | збігається з upstream | — | Дзеркало upstream. Сюди не кладуть форк-фічі, доки `main` має лишатися fast-forward. |
| `translations-uk-ru-odesa` | `e620219` | diverged, +12 / −74 | `1bf20b8`, v0.2.4 | Українська, російська й одеська локалі (`values-uk`, `values-ru`, `values-b+ru+Cyrl+UA+odesa`), вибір мови, далі теки шухляди, трансліт пошуку, док до 7 сторінок. У `main` цих перекладів немає: на `main` лише `values` і `values-night`. |
| `feature/more-dock-pages-and-reset-folder-order` | `b4583d5` | diverged, +8 / −74 | той самий `1bf20b8` | Перші 8 комітів лінії перекладів, від «Add Ukrainian, Russian and Odesa translations…» до «Move apps within a drawer folder». Вони входять у `translations-uk-ru-odesa`. Назва гілки ширша за вміст: комітів про 9 сторінок дока і «скинути порядок папки» на ній немає. |
| `cursor/dock-pages-and-folder-order-0a37` | `443a1d8` | diverged, +10 / −74 | той самий `1bf20b8` | Гілка [PR #1](https://github.com/bezeza/Supernova/pull/1) «Більше сторінок дока (до 9) і „Скинути порядок папки“». Відкритий, база PR — `translations-uk-ru-odesa`. Два коміти понад `feature/…`: `109407f` (док до 9 сторінок) і `443a1d8` (скидання ручного порядку теки). Цих двох комітів у поточному `translations-uk-ru-odesa` немає; там замість них інший коміт, `48b7eef` («Dock up to 7 pages…»). |
| `dev` | `7e86f2f` | behind, +0 / −56 | кінєць гілки і є предок `main` | Призначення в репозиторії не записане. Фактично це зріз upstream «Privacy policy» (2026-11-18). Власних комітів немає, `main` попереду на 56. |
| `ccr-c6390468-djw86f` | `98584d4` | diverged, +18 / −25 | `f547998`, v0.2.6 | Назва `ccr-*` у репозиторії не розшифрована. Коміти поверх лінії перекладів: merge upstream v0.2.6, `.github/workflows/build-apk.yml`, збірка підписаного release APK, потім slim-збірка (en/ru, `armeabi-v7a` + `arm64-v8a`, xxhdpi). У `main` цього workflow немає. |
| `ci-dependabot-gradle-checksum` | `7c2b9f5` | ahead, +1 / −0 | прямо на `main` | [PR #2](https://github.com/bezeza/Supernova/pull/2), відкритий, чекає мерджу. Один коміт: `.github/workflows/ci.yml`, `.github/dependabot.yml`, `distributionSha256Sum` для Gradle 9.7.1. Перевірка `build` на `7c2b9f5` завершилась `success` (run `44957860794`). Цих файлів на `main` ще немає; другий раз їх не додають. |

Інших гілок `feature/*` і `ccr-*` на origin немає.

## Фічі на `translations-uk-ru-odesa`

Цього на `main` (v0.2.8) немає. Перевірено дерево `e620219` і README цієї гілки.

- Переклади інтерфейсу: українська (`values-uk`), російська (`values-ru`). Одеський варіант російської (`values-b+ru+Cyrl+UA+odesa`) на цій гілці був, з `main` його прибрано 2026-11-22, плюс `xml/locales_config.xml`.
- Теки шухляди: «Move up» / «Move down», «Move to folder…» (`move_to_folder`), ручний порядок усередині теки (`manualOrder`), «Reset order» (`reset_folder_order`), «Merge into…» (`merge_folder`).
- Автосортування нових застосунків за вмістом власних тек і підказка покласти щойно встановлене в теку, яка пасує (`suggest_folder`: «%1$s fits the folder %2$s», дія `suggest_folder_action`: «Put it there»).
- Пошук із чужою розкладкою клавіатури і за вимовленим ім’ям. У `AppMatcher` є пара `"вайбер" to "viber"` (і «вибер», «телеграм» та інші).
- Док до 7 сторінок: `Pref.DOCK_PAGES_MAX = 7`. [PR #1](https://github.com/bezeza/Supernova/pull/1) піднімає стелю до 9 (`DOCK_PAGES_MAX = 9` на `443a1d8`). Цей коміт у кінець гілки перекладів не входить: там окремий коміт `48b7eef` («Dock up to 7 pages…»).
- README гілки пише «Smaller release build (R8 and resource shrinking)». Виміру «5 МБ замість 28 МБ» у git немає. На `main` v0.2.8 `isMinifyEnabled` і `isShrinkResources` уже увімкнені; diff `app/build.gradle.kts` проти цієї гілки — лише `versionCode` / `versionName`. Профіль `-Pslim` (en/ru, `armeabi-v7a` + `arm64-v8a`, xxhdpi) є на `ccr-c6390468-djw86f`, не на гілці перекладів.

## Стиль меню «Категорії»

Налаштування → Меню → Стиль меню → **Категорії** (як App Library в iOS і HyperOS). За замовчуванням лишається `VERTICAL_GRID`.

**Вигляд.** Плитки-категорії по 2 в ряд (на широкому екрані, в ландшафті чи на планшеті, більше: плитка не ширша за 200 dp), кругле напівпрозоре тло, підпис під плиткою. У плитці 3 великі іконки (тап запускає застосунок) і кут із 4 мініатюрами (тап відкриває категорію через наявний `FolderPopup`). Довге натискання на плитку дає меню папки, на іконку меню застосунку. Бейджі сповіщень лишаються на великих іконках (на мініатюрах їх нема: надто дрібні). Зверху необов'язковий рядок «Нещодавно встановлені» (4 іконки, за `installTime`), перемикач у налаштуваннях.

**Дані.** Категорії це наявні FOLDER-групи (розумні й користувацькі), у тому ж порядку, що й плитки папок у звичайному меню. Порядок застосунків у плитці дає `viewModel.drawerFolderApps`, тобто поточне `DrawerSort` (або ручний порядок папки). Застосунки поза папками (без `keepInMain` це решта) йдуть нижче під заголовком «Інші застосунки» сіткою на 4 колонки, щоб жоден застосунок не зник із меню.

**Як вбудовано.** Категорії малюють окремий `RecyclerView` у `drawerListContainer`, поверх звичайного списку. Він видимий, лише коли стиль Категорії, відкрита вкладка «Усі» і поле пошуку порожнє. Пошук і вкладки лишаються звичайними списками меню. Для решти коду `DrawerController` стиль Категорії це `VERTICAL_GRID` (`currentStyle`), тож розкладка, колонки, швидка прокрутка й тло працюють як раніше.

**Вигляд.** Підпис плитки по центру під нею, без значка (значок вмикається перемикачем «Показувати значки категорій», типово вимкнений; діє й на назву на екрані категорії). Заливка плитки: колір тексту меню з прозорістю, що йде за прозорістю фону меню (`CategoryTint`: 15% на суцільному меню до 25% на прозорому), тож крізь неї видно шпалери. «Пошук унизу» (типово вимкнений) ставить рядок пошуку внизу меню лише в стилі Категорії, окремо від загального налаштування рядка пошуку; сховане загальним налаштуванням лишається сховане. Налаштування: `categoriesShowIcons`, `categoriesLaunchLarge`, `categoriesSearchBottom` (Pref.kt, Models.kt).

**Налаштування категорії (PR A).** Довге натискання на плитці відкриває аркуш категорії (`CategoryOptionsDialog`): «Сортування всередині» (як у меню / вручну / за абеткою / за частотою запуску), «Приховати категорію» і «Меню папки…» (звичайне меню папки). Сортування змінює і великі іконки плитки, і екран категорії. Прихована категорія зникає з плиток, її застосунки лишаються в пошуку; унизу списку рядок «Приховані категорії: N» повертає їх. Вибір великих іконок ставить сортування «вручну», бо інакше сортування скасувало б вибір. Усе зберігається в одному налаштуванні `categoriesOptions`: JSON за id папки (`CategoryOptionsStore`), типові значення не пишуться, невідоме читається як типове, формат `DrawerGroup` не змінено. «Скільки нових застосунків» (`categoriesRecentCount`, 2–8, типово 4) керує рядком нещодавніх.

**Екран категорії.** Перемикач «Тап по великій іконці» (`categoriesLaunchLarge`, типово ввімкнений, як в iOS): тап по великій іконці плитки запускає застосунок (довге натискання дає його меню), а тап по кутику з малими іконками, по тлу чи підпису відкриває категорію; ціль кутика більша за його клітинку (`TileGeometry`) і лежить над сусідніми великими іконками. Вимкнений: будь-який тап по плитці відкриває категорію, а застосунки запускаються з її екрана. Довге натискання на тлі дає меню папки. Тап по плитці відкриває категорію на весь екран меню: зверху стрілка назад і назва зі значком, нижче сітка застосунків тим самим `AppDrawerAdapter`, що й меню (другий екземпляр `categoryAdapter` з тими самими іконками, підписами, бейджами, тапом і довгим натисканням; колонки, ширина плитки і відступи копіюються з меню). Свайп вліво між вкладками меню працює і на плитках, і на екрані категорії (там лише вперед: свайп вправо це Назад); для цього `addTabSwipe` вішає той самий `TabSwipeDrag` на списки Категорій. Назад ведуть: жест чи кнопка «Назад» (через `clearQuery()`, який меню і так викликає першим), стрілка і свайп вправо. Екран закривається, коли меню ховається, коли плитки зникають (пошук, вкладка, інший стиль) і коли категорію видалено. Кнопка в шапці екрана «Вибрати великі іконки»: по черзі тапаєте до трьох застосунків, і вони стають першими в ручному порядку папки (`moveAppInFolder` до початку в зворотному порядку); Назад під час вибору лише скасовує вибір. Меню довгого натискання застосунка тут не розширювалось, бо це потребувало б правок `LauncherActivity`. `showFolderPopup` й інші стилі меню не змінені.

**Файли форку:** `fork/categories/` (`CategoryLayout` розкладка 3+4 і «нещодавні», `CategoriesController`, `CategoriesAdapter`, `CategoryTileCard`, `CategoryIcons`, `CategoryGlyphs`, `CategoriesPreview`, `CategoriesModels`, `CategoryScreen`, `CategoryHeader`, `CategoryOptions`, `CategoryOrder`, `CategoryOptionsDialog`, `CategoryScreenState`, `LargePicker`, `CategoryTint`, `TileGeometry`), `fork/ForkHooks.kt`, `ui/settings/ForkSettings.kt` (перемикач), `res/values*/strings_fork.xml` (en, uk, ru; генератор `gen_locale.py` їх не чіпає). Тести: `fork/categories/CategoryLayoutTest`, `CategoryGlyphsTest`, `CategoryScreenStateTest`, `LargePickerTest`, `CategoryTintTest`, `TileGeometryTest`, `CategoryOptionsStoreTest`, `CategoryOrderTest`.

**Іконки категорій.** Двадцять затверджених категорій (Соцмережі, Месенджери, Банки й фінанси, Держава і зв’язок, Покупки, Транспорт, Кіно і відео, Музика і аудіо, Фото і монтаж, Ігри, Робота і пошта, ШІ, Медицина, Годинники, Теми і віджети, Мережа і VPN, Інструменти, Системні, Навчання, Інше) мають значок: `res/drawable/ic_fork_cat_*.xml` (groups, chat, account_balance, badge, shopping_bag, directions_car, movie, music_note, photo_library, sports_esports, work, smart_toy, medical_services, watch, palette, vpn_lock, build, settings, school, apps). Це [Material Symbols Outlined](https://github.com/google/material-design-icons) (Google, ліцензія Apache 2.0), 24 dp. Значок шукається за назвою категорії (`CategoryGlyphs`), білий, фарбується кодом з кольору меню. Показується дрібним значком перед назвою плитки й у вкладці з такою назвою (якщо в неї немає свого значка й вона не збігається з розумною категорією). Теку з іншою назвою значок не отримує.

**Файли автора, зачеплені гачками.** Тут чекати конфліктів при синхронізації:

| Файл | Що змінено |
|---|---|
| `ui/DrawerConfig.kt` | `CATEGORIES` доданий у **кінець** `DrawerStyle` (старі бекапи читаються). Не переставляти |
| `ui/DrawerController.kt` | імпорти `fork.*`; поля `categoryAdapter`, `categories` і `categoriesStyle` після `val adapter`; `openCategoryScreen` (тап по плитці), `onCategoryAppClick` і `placeCategoryLarge` (вибір великих), `changeCategoryOptions` і порядок/приховування в `submitCategories`, параметр `onCellClick` у `newCellAdapter`; перший рядок `clearQuery()` і `resetSearch()` закривають екран категорії; `searchAtBottom` і ключ `applyStyle` знають про `categoriesSearchBottom`; у `applyStyle` `currentStyle` для Категорій дорівнює `VERTICAL_GRID`; гілка `when (currentStyle)` розширена до `VERTICAL_GRID, CATEGORIES`; `applyListVisibility` (видимість списку); виклики `applyListVisibility()` у обробнику тексту пошуку й у скиданні пошуку; блок `categories.submit` в кінці `submit()`; `categories.rebind()` у `notifyContentChanged()` і при зміні іконок в `onState`. Усі місця позначені `Fork` |
| `data/Models.kt` | поле `LauncherPrefs.categoriesShowRecent` (за замовчуванням `true`), після `autoSortNewApps` |
| `data/Pref.kt` | запис `val categoriesShowRecent = bool("categories_show_recent")`, після `autoSortNewApps` |
| `ui/settings/EnumLabels.kt` | підпис `style_categories` для `DrawerStyle.CATEGORIES` |
| `ui/settings/preview/SettingsPreviewView.kt` | гілка `else if` у `buildDrawer`: прев'ю двох плиток |
| `ui/TabIcons.kt` | `groupGlyph`: перед буквою вкладка пробує значок категорії (`CategoryGlyphs`) |

## Правила форка

Код фіч форка живе в пакеті `com.supernova.launcher.fork`. Пакет уже є (`fork/ForkHooks.kt`).

Єдина точка входу — останній рядок `LauncherActivity.onCreate`, після реєстрації `screenOffReceiver` (зараз функція закінчується на рядку 727, коміт `5b63c43`):

```kotlin
com.supernova.launcher.fork.ForkHooks.attach(this)
```

Виклик іде ще з `onCreate`, тобто до стану `STARTED`, тож з `attach` можна і підписатися на lifecycle, і викликати `registerForActivityResult`. Далі форк слухає `onStart` / `onResume` / `onDestroy` і `viewModel.state` сам. Окремі рядки в ці методи не додають. Пакет і виклик уже в коді.

Другий і третій гачки — лише якщо без них фічу не зробити, і лише одним рядком на початку функції, делегатом у `fork`:

- перший рядок `dispatchTouchEvent` (зараз рядок 1055);
- перший рядок `onNewIntent` (зараз рядок 2757), одразу після `super`.

UI форка додають кодом у корінь розкладки, не правками `app/src/main/res/layout/activity_launcher.xml` і не новими гілками в `applyDockChrome`.

Не змінювати:

- `namespace` у `app/build.gradle.kts`: `com.supernova.launcher`.
- Рядки intent-ів іконпаків. Їх читають чужі паки й шаблони. У `AndroidManifest.xml`: дії `org.adw.launcher.THEMES`, `com.gau.go.launcherex.theme`, `com.supernova.launcher.APPLY_ICON_THEME`, `com.anddoes.launcher.SET_THEME`, `org.adw.launcher.SET_THEME`. У `ApplyIconPackActivity` extra: `com.supernova.launcher.extra.ICON_THEME_PACKAGE`, `com.anddoes.launcher.THEME_PACKAGE_NAME`, `org.adw.launcher.theme.NAME`, `icon_pack_package`, `packageName`. У `IconPackRepository` ті самі theme-дії для пошуку паків.
- Гарячі місця `LauncherActivity` зі списку нижче. Номери рядків — на `5b63c43`; після синку upstream їх треба перечитати по карті, а не переносити механічно.

Не чіпати в `LauncherActivity.kt`, якщо завдання не є свідомим портом саме цієї поведінки upstream:

- порядок у `dispatchTouchEvent` (1055–1139) і `trackDrawerPull` (1199–1266);
- back-callback в `onCreate` (677–697);
- `observeState` і поріг `iconsReady` (1865–1973);
- `onNewIntent`, `resetToHome`, `pressedOnHome`, `keepPageForHomePress`, `screenOffReceiver`;
- `renderDock`, `dockSignature`, `applyDockChrome`, `sharedRowPx` і константи дока в `companion`;
- `showFolderPopup`, `showDrawerFolderPopup` і слот `activeFolderPopup`;
- `viewForItem` і `handleDropToDock`;
- сигнатури конструкторів `DrawerController`, `SearchController`, `FeedController`, `LiveDragController`.

Чому так: на цьому файлі 135 комітів з 2026-10-14 по 2026-11-21, у пікові дні по 10–15. Винесення функцій або правка їхнього тіла конфліктить майже на кожному підтягуванні upstream. Деталі зон і ранжир винесення — у карті.

PR маленькі: одна тема на один PR. Перед PR:

```bash
./gradlew testDebugUnitTest detekt lintDebug
```

На `main` ці команди — поріг. На `translations-uk-ru-odesa` `lintDebug` зараз не зелений (див. нижче), тож PR з тієї гілки цей пункт не закриє, доки lint не полагоджено.

## Відомі проблеми
Факти на 2026-11-22.

- **Одеську локаль прибрано з `main` (2026-11-22)**: теку `values-b+ru+Cyrl+UA+odesa`, `tools/make_odesa.py`, `tools/odesa*.txt`, пункт у виборі мови (`AppLanguage`) і запис у `locales_config.xml`. Хто мав її обраною, при запуску отримує українську: `AppLanguage.migrateRemoved()`, один виклик у `SupernovaApp.onCreate`. У бекапі налаштувань мови немає (вона зберігається як локаль застосунку), тож старі бекапи цього не зачіпають. Відновити можна з історії git (коміт перед цією зміною) або з гілки `translations-uk-ru-odesa`.

- **`main` більше не чисте дзеркало upstream.** У ньому вже власні коміти: CI ([PR #2](https://github.com/bezeza/Supernova/pull/2)), ці нотатки ([PR #3](https://github.com/bezeza/Supernova/pull/3)), переклади й фічі гілки перекладів ([PR #5](https://github.com/bezeza/Supernova/pull/5)). Тож `git merge --ff-only upstream/main` з розділу про синк більше не пройде, поки upstream не зрушить далі; підтягувати його доведеться звичайним merge з розв'язанням конфліктів. Розділи вище описують стан до цих PR і ще не переписані.
- **Той самий `applicationId`, що в upstream:** `com.supernova.launcher` (`app/build.gradle.kts`). Debug-збірка додає суфікс `.debug`, тож debug ставиться поруч. Release-ідентифікатор збігається з F-Droid/upstream збіркою.
- **Фічі форка лежать поза пакетом `fork`.** Код із PR #5 (`search/AppQuery.kt`, `ui/settings/ForkSettings.kt`, `data/FolderAutoSort.kt`, меню папок у `LauncherActivity`) з'явився до правила про `com.supernova.launcher.fork`. Новий код пишуть через `ForkHooks` ([PR #4](https://github.com/bezeza/Supernova/pull/4), злито).
- **Перевірки.** На `main` проходить `./gradlew testDebugUnitTest detekt lintDebug assembleDebug` без змін baseline. Нові файли форка пройшли detekt без baseline; не додавайте записів у `detekt-baseline.xml` і `lint-baseline.xml`, а виправляйте код. Release-збірка (R8) збирається; на пристрої нічого з фіч форка не запускалось.
- **«Скинути порядок папки» дублюється.** На `main` вона вже є з PR #5 (простий варіант). Коміт `443a1d8` з [PR #1](https://github.com/bezeza/Supernova/pull/1) (живе оновлення картки, Undo) з нею конфліктує у 7 файлах і змінює `detekt-baseline.xml`; рішення за власником.

## Що лишилось

- **Розширені налаштування категорій і папок, чотири PR за порядком (затверджено власником).** A: налаштування категорії, зроблено в цьому PR. B: розмір плитки (звичайна / широка: рядок на всю ширину, назва зліва, 5–6 іконок середнього розміру і «+N» понад це / маленька, одна колонка) і кількість великих іконок 3 / 1 / 0 (де 0 це сітка 3×3 малих). D: кнопка «+» у вікні папки і режим «на весь екран» окремо для папки. C: налаштування окремої папки для папок меню й головного екрана (колонки, вигляд іконки 2×2 / 3×3 / одна головна, назва, прозорість тла, сортування). Нові поля мають типові значення, щоб старі бекапи читались; код у `fork`, гачки позначені `Fork`. Червоні крапки сповіщень на плитках теж окремим PR.
- Док до 9 сторінок уже на `main`: [PR #9](https://github.com/bezeza/Supernova/pull/9), коміт `03b78a6` (`DOCK_PAGES_MAX = 9`). Це була перша половина PR #1.
- Вирішити долю PR #1: його «скидання порядку» вже частково на `main`, див. вище.
- Дати форку власний `applicationId`, щоб release не збігався з upstream.
- Прибрати зайві гілки. `dev` повністю входить у `main`. `feature/more-dock-pages-and-reset-folder-order` і `translations-uk-ru-odesa` тепер у `main` через PR #5 (squash). Назва `ccr-*` у репозиторії не розшифрована. Видаляти гілки лише після підтвердження власника.

Заплановане місце для нового коду форка — пакет `com.supernova.launcher.fork` і один виклик `ForkHooks.attach(this)`, описані вище. Виклик `ForkHooks.attach(this)` уже стоїть наприкінці `LauncherActivity.onCreate`.
