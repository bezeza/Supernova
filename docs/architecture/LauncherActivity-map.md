# Карта `LauncherActivity.kt`

Файл: `app/src/main/java/com/supernova/launcher/LauncherActivity.kt`.

Номери рядків зняті з коміту `5b63c43` («App version bumped to 28 (v0.2.8)»), 5333 рядки, 272 508 байт. На цю дату файл форка збігається байт-у-байт з `tanujnotes/Supernova` `main` (SHA-256 `3151d923f07751178601998a6f09ff06d2a6bb5ce8e1f0d687ecbce01857cad0`). Після наступного fast-forward upstream діапазони поїдуть. Шукайте функцію за ім’ям, не за старим номером.

На цьому файлі 135 комітів з 2026-10-14 по 2026-11-21. Це композиційний корінь, не весь лаунчер. Поруч уже винесено (рядки теж на `5b63c43`): `DrawerController` 2654, `LiveDragController` 1745, `WidgetController` 1024, `SearchController` 776, `FeedController` 691, `WorkspacePagerAdapter` 627, `FolderPopup` 543, `WorkspaceDrops` 479, `ItemIcons` 382, `HomeEditController` 371.

```140:155:app/src/main/java/com/supernova/launcher/LauncherActivity.kt
class LauncherActivity :
    AppCompatActivity(),
    WorkspacePagerAdapter.Host,
    LiveDragController.Host,
    WidgetController.Host,
    com.supernova.launcher.ui.VoiceSearch.Host {
    // ...
    private lateinit var drawer: DrawerController
    private lateinit var drag: LiveDragController
    private lateinit var widgets: WidgetController
    private lateinit var search: SearchController
    private lateinit var feed: FeedController
```

Правила форка, який має синкатися з upstream: [FORK.md](../../FORK.md). Коротко: не різати цей файл на класи заради форка. Новий код — пакет `com.supernova.launcher.fork` і один запланований виклик `ForkHooks.attach(this)` в кінці `onCreate`. Пакета і виклику в дереві ще немає.

## 1. Зони

Логічна зона часто розірвана: малювання вже в контролері, політика лишилась тут.

| Зона | Рядки | Обсяг | Ключове |
|---|---|---|---|
| Імпорти і шапка | 1–146 | ~146 | Чотири `Host` |
| Поля, прапорці, збірка контролерів | 147–656 | ~510 | `viewModel`, `actions`, `privateSpaceCard`, `workspaceDrops`, SAF/role |
| Lifecycle, конфіг, badges, onboarding | 657–1053 | ~397 | `onCreate`, receivers, `observeBadges` |
| Арбітраж дотиків | 1055–1382 | ~328 | `dispatchTouchEvent`, drawer pull, dock swipe |
| Сторінки workspace | 1384–1472, 3870–3982, 5068–5221, 608–655 | ~430 | pager, undo сторінок, edit mode, previews |
| Drawer (лише клеї) | 1474–1512, 2616–2704 | ~130 | `setupDrawer`, `openDrawer` / `hideDrawer` |
| Search | 1523–1621, 3035–3085, 4502–4504 | ~170 | `setupSearch`, меню барів |
| Дозволи | 493–495, 882–932, 1625–1646, 1664–1682, 3131–3136 | ~120 | HOME role, CALL, calendar, contacts |
| Feed | 1648–1773 і `cards` 172–178 | ~140 | `setupFeed`, `FeedCardRoutes` |
| Dock | 410–441, 1783–1863, 2095–2614, 2910–2931 | ~750 | сторінки дока, `renderDock`, геометрія, chrome |
| Стан → екран | 1865–1986 | ~122 | `observeState`, `applyScrollEffect` |
| System chrome / insets | 1990–2068, 2359–2394, 2070–2077 | ~150 | статус/nav, orientation, edge-to-edge |
| Home press | 996–1003, 2690–2861 | ~180 | `resetToHome`, `onNewIntent` |
| Запуск застосунків | 2863–3146 | ~284 | анімація, shortcut, intent URI |
| Контекстні меню | 3148–3710 | ~563 | drawer/workspace/shortcut menus, Edit sheet |
| Undo | 3722–3923 | ~200 | item, folder sort, page, move-to-drawer |
| Папки | 340–347, 4070–4498 | ~540 | home folder, drawer folder, checklists |
| Drag-and-drop `Host` | 321–338, 4580–4895, 3927–3929 | ~360 | drop у док, `viewForItem` |
| Icon swipe | 4932–5066 | ~135 | `attachIconSwipe`, `launchSwipeAction` |
| Wallpaper picker | 4897–4930, 577–580, 934–951 | ~55 | crop, colors listener |
| Widget `Host` | 5231–5262, створення на 672 | ~40 | resize frame, `onActivityResult` |
| Іконпаки | 404, 544–572, 1503, 3676–3709 | ~50 | не зона, а виклики `itemIcons` / `AppEditSheet` |
| Міст у Settings | 1721, 3047, 3078, 4537, 5186 | кілька викликів | `SettingsActivity` + extras |
| Константи | 5264–5332 | ~69 | `companion object` |

**Lifecycle (657–727, 736–799, 953–1053).** `onCreate` збирає екран: wallpaper flag, accent, `setContentView`, `WidgetController`, back-callback, потім `setupWorkspace` → `setupDrawer` → `setupSearch` → `setupFeed` → `setupDock` → `LiveDragController` → `setupEditMode` → `observeState` → onboarding → badges → `restoreDockPage` → wallpaper listener і `ACTION_SCREEN_OFF`. Back закриває шари в фіксованому порядку: popup драга, resize frame, edit mode, desktop previews, search, feed, query drawer (677–697). `onStart`/`onStop` тримають `startedSinceResume`. `relayoutForConfiguration` (756–799) зносить popups і кеші пікселів.

**Жести (1055–1139).** `dispatchTouchEvent` лише роздає потік: `pageSwipeGuard` → private space → `pageDrag` → `drag.routeGesture` → активний drawer pull → resize в edit mode → якщо drawer/search/edit/previews відкриті, жестів home немає → resize → feed (`CONSUMED` / `TAKE_OVER`) → відкритий feed → swipe, що вже забрав потік → scroll віджета → `trackDrawerPull` → `HomeGestureDetector` → `trackDockSwipe` → `super`. Icon swipe (4937–5066) вішається на view іконки і забирає той самий `ACTION_DOWN`, що й drawer pull (`claimHomeSwipe`, 4992).

**Сторінки.** `setupWorkspace` (1384–1442) ставить адаптер, fling-settle, hardware layer на час ефекту, wallpaper offset і індикатор. Видалення сторінок з Undo — 3870–3982. Edit mode — 5154–5221 (`PageDragController` + `HomeEditController`). Desktop previews — lazy 608–647.

**Dock — найбільший суцільний шматок, який ще живе тут.** `dockPage` (410) — поле activity, не модель. `renderDock` (2100–2181) і `dockSignature` перебудовують слоти. Геометрія 2217–2357 (`sharedRowPx`, `dockSlotPx`, `dockIconBoxPx`, `desktopIconNeedPx`) ділить ширину з пошуковим баром. `applyDockChrome` (2396–2605) розкладає dock, search bar, стрілку, pager і індикатор через `BottomChrome.layout`. `dockChromeWatch` (1820–1835) дропає до `MAX_DOCK_CHROME_HOLDS` (2) кадри, поки висота дока не встоялась. Свайп сторінок дока — 1343–1382.

**Drawer, search, feed** тут — ламбди в конструктори. Самі контролери більші за ці функції. `setupDrawer` (1474–1512) віддає кліки, бейджі й іконки папок у `itemIcons`. `setupSearch` (1523–1621) зшиває пошук із папками, картками і long-press. `setupFeed` (1648–1773) — `FeedController.Host`: дозволи, інтенти календаря/контактів, Settings секції Feed.

**Home (2757–2826).** `onNewIntent` розрізняє extra `EXTRA_ADD_FEED_WIDGET`, Home з іншого застосунку (`startedSinceResume`) і Home вже на лаунчері (`pressedOnHome`: фокус довше за `HOME_FOCUS_SETTLE_MS` = 100 мс і без `FLAG_ACTIVITY_BROUGHT_TO_FRONT`). Далі `resetToHome`, політика сторінки (`keepPageForHomePress`) і double-home action через `actions.run`. Той самий reset викликає `screenOffReceiver` (996–1002). Коміти `#317` від 2026-11-21 сидять саме тут.

**Меню і папки.** `showWorkspaceItemMenu` (3506–3645) і `showDrawerItemMenu` (3297–3392) — найтовстіші меню. `showFolderPopup` (4157–4311) і `showDrawerFolderPopup` (4392–4498) дублюють картку: touch interceptor, drag, wallpaper zoom, `activeFolderPopup`. Private space (221–291) пише в ті самі `activeFolderPopup` / `activeFolderAdapter`.

**Drag-and-drop.** Рух у `LiveDragController`. Activity відповідає на `handleDropToDock` (4657–4699), `viewForItem` (4785–4877), `canMergeIntoDock`, `closeItemSources`. Куди падає елемент на сторінку, уже в `WorkspaceDrops` (321–338 лише Host).

**Іконпаки — не блок цього файлу.** Каталог і `ItemIcons` (рядок 404) малюють іконки. Тут лише прокидання: `customFolderIcon(..., iconPack, iconName, iconRaw)` на 1503, `pickPhoto` (561–572), `showAppEditSheet` (3676–3709). Коментар у `observeState` (1901–1907) пояснює поріг `iconsReady`: інакше з іконпаком спочатку малюються стокові іконки.

**Settings, дозволи, інтенти — розсип.** Settings: `EXTRA_OPEN_SEARCH_BAR` (3047), `EXTRA_SECTION_TITLE_RES` для feed (1721) і drawer search (3078), `EXTRA_DRAWER_GROUP` (4537), голий `SettingsActivity` з edit mode (5186). Дозволи: `requestHomeRoleLauncher` (493), `requestCallPermission` (1627), `requestFeedCalendarPermission` / `requestFeedContactsPermission` (1638–1646). Запуск: `launchApp`, `launchShortcut`, `launchIntentUri` (3123), `openUri` (1775).

## 2. Залежності і спільний стан

Шина одна: `viewModel.state` (`LauncherUiState`). `observeState` (1865–1973) — єдиний колектор. Спочатку все, чому не потрібні іконки (`applyScrollEffect`, `applySystemChrome`, `feed.onPrefs`), і лише після `state.iconsReady` — drawer, search, pager, edit mode, індикатор, `renderDock`. Пропуск `onlyPageMovedFrom` навмисний: свайп сторінки не перебудовує док.

Спільні мутабельні поля:

- **Шари поверх home:** `activeFolderPopup`, `activeFolderAdapter`, `drag.activeItemPopup`, `homeMenuDialog`, `desktopPreviews`, `editMode.isOpen`, `drawer.isVisible`, `search.isOpen`, `feed.isOpen`. Їх читають back-callback, `privateSpaceCard.homeIsClear`, `resetToHome`, `dispatchTouchEvent`.
- **Док:** `dockPage`, `renderedDock`, `dockAutoClosed`, `dockToggledOff`, `dockHeightPx`, `dockSlotWidthPx`, `appliedDockChrome`.
- **Жест одного пальця:** `drawerPullArmed` / `drawerPullActive`, `swipeOwnsStream`, `pageSwipeGuard`, `gestureDetector`.
- **Home-прес:** `startedSinceResume`, `focusGainedAt`, `drawerKeptBehindLaunch`, `cardSwipeClose`.
- **Іконки:** `itemIcons`, `lastItemIcons`, `renderedBadges`. Badges (834–872) перемальовують сторінку, док і відкриту папку.
- **Геометрія вікна:** `systemInsets`, `isLandscape`, `lastConfig*`. Їх споживають system chrome, dock chrome і `desktopIconNeedPx` (його питає drag settle).

`ActionRunner.Host` (194–216) зводить жести, Home, action shortcuts і картки на `openDrawer`, shade, search, `launchApp`, `toggleDock`, feed, private space, previews. `cards` (`FeedCardRoutes`) спільні для search і feed. `launchWithAnimation` (2939) — єдина точка переходу в застосунок.

Винесення «зони» майже завжди тягне `Host` з колбеками назад в activity. Так уже зроблені `ActionRunner`, `PrivateSpaceCard`, `WorkspaceDrops`, `FeedController`, `HomeEditController`. Залишок файлу — реалізація цих `Host` плюс клеї.

## 3. Кандидати на винесення

Ранжир, якби файл був ваш і щодня мерджити чужий `main` не треба. Для форка, який синкається з upstream, ці виноси невигідні: конфлікт переїде в новий файл і в тонкий `Host`, який лишиться в activity. Див. [FORK.md](../../FORK.md).

1. **Dock chrome і геометрія** (`renderDock`, `dockSignature`, `sharedRowPx` / `dockSlotPx` / `dockContentWidth`, `applyDockChrome`, `dockBackgroundDrawable`, `dockChromeWatch`, `showMainDockPage`). Близько 2095–2614 плюс 1783–1863, ~750 рядків. Найзв’язніший великий шматок. Ризик середній: `applyDockChrome` кличе `widgets.bindSearchBarWidget` і марджіни pager. Константи дока в `companion` (5293–5329) треба забрати разом.
2. **Контекстні меню** (3148–3710 і 3984–4066). ~650 рядків. Ризик високий: меню запускають, ховають, пінять, відкривають Settings і drag.
3. **Картки папок** (`showFolderPopup`, `showDrawerFolderPopup`, `folderMemberMenu`, checklist). ~500 рядків, два паралельні popup. Private space уже винесений і все одно пише в `activeFolderPopup`.
4. **Запуск** (`launchWithAnimation`, `launchOptions`, `launchApp`, `launchItem`, `launchShortcut`, `launchIntentUri`). ~280 рядків. `LaunchPrelude` уже винесений. `registerForActivityResult` для дзвінка має лишитись ініціалізацією activity.
5. **System chrome** (`applySystemChrome`, `applyStatusBarVisibility`, `homeOrientationRequest`, `setUpEdgeToEdge`, `applyWindowInsets`). ~150 рядків. Найспокійніший невеликий виніс, guard `appliedSystemChrome`.
6. **Icon swipe** (4937–5066). ~130 рядків. Ризик середній через `gestureDetector` і `drawerPullArmed`.
7. **`observeState`.** ~110 рядків, це список інтеграцій. Не виносити.
8. **`dispatchTouchEvent`.** Політика вже в одній функції на 85 рядків. Порядок — продукт. Не виносити.
9. **Home-прес (`onNewIntent` + `resetToHome`).** ~180 рядків, політика свіжа (`#317`, 2026-11-21). Не виносити, поки її ще міняють.
10. **Іконпаки, Settings, дозвільні контракти.** Виносити звідси немає куди: іконпаки в `ItemIcons`, Settings — це `startActivity` з extra, `registerForActivityResult` прив’язаний до `ComponentActivity`.
