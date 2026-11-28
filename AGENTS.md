# Agents

Read [FORK.md](FORK.md) and [docs/architecture/LauncherActivity-map.md](docs/architecture/LauncherActivity-map.md) before editing this repo. `main` of bezeza/Supernova carries the fork's own merged work on top of [tanujnotes/Supernova](https://github.com/tanujnotes/Supernova): it is no longer a fast-forward mirror, and upstream is merged into it with a merge commit. Line numbers in the map are for commit `5b63c43` and drift on every upstream sync.

## Hard rules

- Fork features go in the new package `com.supernova.launcher.fork`. The only planned edit inside `LauncherActivity` is one line at the end of `onCreate`: `ForkHooks.attach(this)`. The hook is already in the tree (PR #4); do not add a second call.
- One more delegate call, approved by the owner: `ForkHooks.startOnboarding(...)` where `LauncherActivity` starts the first-run wizard (marked `Fork`), so the language screen goes before it. Keep it on a sync.
- One more delegate call, approved by the owner: `FrequentMenu.addRows(...)` in `LauncherActivity.showDrawerItemMenu`, right after `itemMenuHook`, which adds «Не показувати в Частих» to an app's menu while the Frequent category's screen is open (marked `Fork`). Keep it on a sync.
- Optional hooks, only when a feature cannot work otherwise: a single delegate call as the first line of `dispatchTouchEvent`, and a single delegate call as the first line of `onNewIntent` after `super`.
- Add fork UI in code. Do not edit `app/src/main/res/layout/activity_launcher.xml` for fork UI.
- Do not change `namespace` (`com.supernova.launcher`) or the icon-pack intent action and extra strings listed in FORK.md.
- Do not restructure `LauncherActivity.kt` (no extractions, no reordered functions) on a branch that must keep syncing with upstream. The do-not-touch list is in FORK.md.
- Keep pull requests small, one topic each.
- Before opening a PR, run `./gradlew testDebugUnitTest detekt lintDebug`. Report the real result of each command; do not claim one passed unless you ran it.
- Sync from upstream in its own pull request, one topic: a branch `sync-vX.Y.Z` cut from `main`, then `git merge vX.Y.Z` (the upstream tag) as a merge commit. A fast-forward is no longer possible, because `main` has commits of its own. Resolve conflicts keeping upstream's logic and the fork's hooks (marked `Fork`), translate new strings (`tools/uk.txt`, `tools/ru.txt`, then `tools/gen_locale.py`, see FORK.md). Do not rebase or force-push `main` or the sync branch, and never push anything to upstream. Last synced: v0.2.9 (`de54a7f`), in PR sync-v0.2.9.
- Do not add `.github/workflows/ci.yml`, `.github/dependabot.yml`, or `distributionSha256Sum`. Those are open PR #2 (`ci-dependabot-gradle-checksum`), CI green, not merged.
- Do not invent branch purposes. The verified map is in FORK.md. `dev` and the name `ccr-*` have no written purpose in the repo.
