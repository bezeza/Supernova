# Agents

Read [FORK.md](FORK.md) and [docs/architecture/LauncherActivity-map.md](docs/architecture/LauncherActivity-map.md) before editing this repo. `main` of bezeza/Supernova is a fast-forward mirror of [tanujnotes/Supernova](https://github.com/tanujnotes/Supernova). Line numbers in the map are for commit `5b63c43` and drift on every upstream sync.

## Hard rules

- Fork features go in the new package `com.supernova.launcher.fork`. The only planned edit inside `LauncherActivity` is one line at the end of `onCreate`: `ForkHooks.attach(this)`. The hook is already in the tree (PR #4); do not add a second call.
- Optional hooks, only when a feature cannot work otherwise: a single delegate call as the first line of `dispatchTouchEvent`, and a single delegate call as the first line of `onNewIntent` after `super`.
- Add fork UI in code. Do not edit `app/src/main/res/layout/activity_launcher.xml` for fork UI.
- Do not change `namespace` (`com.supernova.launcher`) or the icon-pack intent action and extra strings listed in FORK.md.
- Do not restructure `LauncherActivity.kt` (no extractions, no reordered functions) on a branch that must keep syncing with upstream. The do-not-touch list is in FORK.md.
- Keep pull requests small, one topic each.
- Before opening a PR, run `./gradlew testDebugUnitTest detekt lintDebug`. Report the real result of each command; do not claim one passed unless you ran it.
- Sync `main` from upstream with `git merge --ff-only` only. Do not force-push or rebase `main` to catch up. `main` is already at upstream v0.2.8 (`5b63c43`).
- Do not add `.github/workflows/ci.yml`, `.github/dependabot.yml`, or `distributionSha256Sum`. Those are open PR #2 (`ci-dependabot-gradle-checksum`), CI green, not merged.
- Do not invent branch purposes. The verified map is in FORK.md. `dev` and the name `ccr-*` have no written purpose in the repo.
