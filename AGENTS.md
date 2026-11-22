# Agents

Read [FORK.md](FORK.md) and [docs/architecture/LauncherActivity-map.md](docs/architecture/LauncherActivity-map.md) before editing this repo. `main` of bezeza/Supernova is a fork of [tanujnotes/Supernova](https://github.com/tanujnotes/Supernova) with its own commits, so it is synced with a merge, not a fast-forward. Line numbers in the map are for commit `5b63c43` and drift on every upstream sync.

## Hard rules

- Fork features go in the new package `com.supernova.launcher.fork`. The only planned edit inside `LauncherActivity` is one line at the end of `onCreate`: `ForkHooks.attach(this)`. That hook is **not in the tree yet**. Do not add it unless the task is to implement that plan.
- Optional hooks, only when a feature cannot work otherwise: a single delegate call as the first line of `dispatchTouchEvent`, and a single delegate call as the first line of `onNewIntent` after `super`.
- Add fork UI in code. Do not edit `app/src/main/res/layout/activity_launcher.xml` for fork UI.
- Do not change `namespace` (`com.supernova.launcher`) or the icon-pack intent action and extra strings listed in FORK.md.
- Do not restructure `LauncherActivity.kt` (no extractions, no reordered functions) on a branch that must keep syncing with upstream. The do-not-touch list is in FORK.md.
- Keep pull requests small, one topic each.
- Before opening a PR, run `./gradlew testDebugUnitTest detekt lintDebug`. `lintDebug` is known to fail on `translations-uk-ru-odesa` (see FORK.md); do not claim it passed there.
- Sync `main` from upstream only by the procedure in [FORK.md](FORK.md#синхронізація-з-оригіналом-upstream): a `sync/upstream-YYYY-MM-DD` branch, a merge, a pull request, and the owner's OK. Never push to `tanujnotes/Supernova`. Do not force-push or rebase `main` to catch up.
- Do not invent branch purposes. The verified map is in FORK.md. `dev` and the name `ccr-*` have no written purpose in the repo.
