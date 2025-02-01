# CHANGELOG


## Untagged


## 


### 1 Feb 2025

- ([`22318aa`](https://github.com/russmatney/clawe/commit/22318aa)) wip: narrowing in on some osx hangs - Russell Matney

  > plus some clj-kondo configs


### 27 Jan 2025

- ([`3f9a283`](https://github.com/russmatney/clawe/commit/3f9a283)) wip: hacking on the org-roam db - Russell Matney

### 8 Jan 2025

- ([`7eb8aa9`](https://github.com/russmatney/clawe/commit/7eb8aa9)) chore: disable some down-stream org->blog listeners - Russell Matney

### 3 Jan 2025

- ([`3b7db4e`](https://github.com/russmatney/clawe/commit/3b7db4e)) chore: ignore logs/, support :transparent option - Russell Matney
- ([`1c8051e`](https://github.com/russmatney/clawe/commit/1c8051e)) misc: couple fixes, rename 'rules' to 'ingestors' - Russell Matney

### 2 Jan 2025

- ([`af9699f`](https://github.com/russmatney/clawe/commit/af9699f)) feat: quick repo todo ingestion support - Russell Matney

  > Now supporting non-garden org todo interactions! woo!

- ([`a3c49f2`](https://github.com/russmatney/clawe/commit/a3c49f2)) refactor: drop garden-watcher, add /ingest endpoint - Russell Matney

  > Moving to an on-emacs-save hook, dropping the watch-all-garden-files
  > approach.

- ([`d1cefab`](https://github.com/russmatney/clawe/commit/d1cefab)) tweak: smaller topbar scratchpad icons - Russell Matney

### 31 Dec 2024

- ([`45136ed`](https://github.com/russmatney/clawe/commit/45136ed)) chore: clj-kondo cruft - Russell Matney
- ([`5baa2ef`](https://github.com/russmatney/clawe/commit/5baa2ef)) fix: filter scratchpads when deleting empty workspaces - Russell Matney

  > This has been broken on yabai forever b/c clawe/yabai scratchpads hang on
  > to the workspace they were in, even when hidden. Workspaces now get
  > deleted on the fly effectively.

- ([`895574e`](https://github.com/russmatney/clawe/commit/895574e)) feat: separate 'wsp-cell' for scratchpad clients - Russell Matney

  > Filters scratchpad clients out of workspace cells - towards cleaner repo
  > workspace display in the topbar.


### 21 Dec 2024

- ([`8d963ac`](https://github.com/russmatney/clawe/commit/8d963ac)) misc: attempt to get terminal on wsp-create in correct wsp - Russell Matney

  > Not sure why this one is stubborn - doesn't seem to wait on
  > Thread/sleep, tho i think that's my own fault.


### 19 Dec 2024

- ([`b1e4676`](https://github.com/russmatney/clawe/commit/b1e4676)) fix: fallback garden dir, yarn lock update - Russell Matney
- ([`e76a0fe`](https://github.com/russmatney/clawe/commit/e76a0fe)) chore: race-case note clean up - Russell Matney

### 17 Dec 2024

- ([`5b54ad1`](https://github.com/russmatney/clawe/commit/5b54ad1)) bb: better help/logs for getting symlinks in place - Russell Matney
- ([`d279900`](https://github.com/russmatney/clawe/commit/d279900)) sway: fix bad key - Russell Matney
- ([`dc52755`](https://github.com/russmatney/clawe/commit/dc52755)) feat: tmux step toward configurable terminal - Russell Matney
- ([`865180e`](https://github.com/russmatney/clawe/commit/865180e)) chore: systemic clj-kondo cruft - Russell Matney

  > Not sure why this keeps toggling around

- ([`8dbf891`](https://github.com/russmatney/clawe/commit/8dbf891)) fix: git-status support needs pull AND push - Russell Matney
- ([`a23783c`](https://github.com/russmatney/clawe/commit/a23783c)) fix: sort default clips/screenshots by event/timestamp, not time ingested - Russell Matney
- ([`13ebf54`](https://github.com/russmatney/clawe/commit/13ebf54)) feat: rendering game-clips in the frontend! - Russell Matney
- ([`642b715`](https://github.com/russmatney/clawe/commit/642b715)) fix: clip and screenshot date parsing and other ingestion fixes - Russell Matney
- ([`8d16d1a`](https://github.com/russmatney/clawe/commit/8d16d1a)) feat: screenshot/clip ingest buttons - Russell Matney
- ([`0443678`](https://github.com/russmatney/clawe/commit/0443678)) misc: doctor.config helpers - Russell Matney

  > the nrepl system depends on doctor.config rn, which is a pain b/c it
  > restarts nrepl when editing doctor.config - should maybe remove that
  > dependency?

- ([`d0a6eb1`](https://github.com/russmatney/clawe/commit/d0a6eb1)) fix: restore screenshot page and compoennts - Russell Matney

  > Bit of a cleaner unpacking/renaming from :data to 'screenshot' - ought
  > to go for this style more.

- ([`1598a40`](https://github.com/russmatney/clawe/commit/1598a40)) feat: screenshots/clips ingestion watchers - Russell Matney

  > Refactors garden/watcher into doctor/watchers with support for
  > screenshots and clips. Includes some db ingestion fixes (date parsing
  > and converting File to str).


### 16 Dec 2024

- ([`f80b862`](https://github.com/russmatney/clawe/commit/f80b862)) fix: wallpapers rendering - Russell Matney
- ([`2b9be3f`](https://github.com/russmatney/clawe/commit/2b9be3f)) fix: restore set-wallpaper - Russell Matney
- ([`d7d69b1`](https://github.com/russmatney/clawe/commit/d7d69b1)) refactor: move db usage to use-query hook - Russell Matney
- ([`0fa4cb3`](https://github.com/russmatney/clawe/commit/0fa4cb3)) chore: misc stream toying - Russell Matney

  > No real change, just cleaner logging for the moment.

- ([`0246505`](https://github.com/russmatney/clawe/commit/0246505)) fix: restore last-fetch-timestamp, wait to update status - Russell Matney

  > There's a race-case in the git-status update - we fire a fetch via
  > tmux/fire, which waits for no one to return. waiting a couple seconds
  > here should mostly fix it for now, and hopefully the
  > last-fetch-timestamp comes in handy as well.

- ([`d818f50`](https://github.com/russmatney/clawe/commit/d818f50)) fix: don't crash and exit just b/c doctor server is down - Russell Matney
- ([`47af5b1`](https://github.com/russmatney/clawe/commit/47af5b1)) emacs: use doom commands, not russ/* specific - Russell Matney

### 15 Dec 2024

- ([`4c04f25`](https://github.com/russmatney/clawe/commit/4c04f25)) fix: specify directory in fetch-via-tmux - Russell Matney

  > tmux sessions aren't guaranteed to be in the expected directory.
  > 
  > Also improves the git-status layout.

- ([`52df37a`](https://github.com/russmatney/clawe/commit/52df37a)) fix: couple regex blunders - Russell Matney
- ([`8188d88`](https://github.com/russmatney/clawe/commit/8188d88)) wip: still some key problems on this debug component - Russell Matney

  > It doesn't seem to support lists of items anymore...

- ([`1ac14b2`](https://github.com/russmatney/clawe/commit/1ac14b2)) feat: filter out repos already in open workspaces - Russell Matney
- ([`4f9137b`](https://github.com/russmatney/clawe/commit/4f9137b)) fix: specify :id for db-listening reactions - Russell Matney

  > Ought to add :ids to the other queries as well

- ([`71a8007`](https://github.com/russmatney/clawe/commit/71a8007)) fix: restore debug/raw-data component - Russell Matney

  > Also renames from metadata to just data.

- ([`9eef6c6`](https://github.com/russmatney/clawe/commit/9eef6c6)) fix: more todo/filter-grouper comp fixes - Russell Matney
- ([`eb17b63`](https://github.com/russmatney/clawe/commit/eb17b63)) fix: update pomodoro views/actions to react to db data - Russell Matney
- ([`b928d6d`](https://github.com/russmatney/clawe/commit/b928d6d)) fix: restore dashboard/page icons - Russell Matney
- ([`142bde4`](https://github.com/russmatney/clawe/commit/142bde4)) fix: restore views/focus and comp/todo bits - Russell Matney

  > More component porting that was missed, and surely not the last of it.

- ([`f203b40`](https://github.com/russmatney/clawe/commit/f203b40)) feat: ensure emacs/tmux workspaces whenever a wsp is created - Russell Matney

  > Treating clawe.wm as the dispatcher here - maybe eventually want an
  > event system, or one on top of signals/subscribes.

- ([`7af0214`](https://github.com/russmatney/clawe/commit/7af0214)) fix: focus listening on db changes - Russell Matney

  > Also fixes a long time index-not-changing button after tasks are
  > completed.

- ([`438492f`](https://github.com/russmatney/clawe/commit/438492f)) chore: `shelll` all bb tasks - Russell Matney

  > These logs are helpful for confirming whatever tf the command
  > does (which varies across os)


### 14 Dec 2024

- ([`c6a4927`](https://github.com/russmatney/clawe/commit/c6a4927)) feat: add icons for repo/workspace actions - Russell Matney
- ([`a41f9b5`](https://github.com/russmatney/clawe/commit/a41f9b5)) feat: create/close workspace from repos on wsp widget - Russell Matney
- ([`b3e7a26`](https://github.com/russmatney/clawe/commit/b3e7a26)) feat: text color based on git status - Russell Matney
- ([`0ed8fd9`](https://github.com/russmatney/clawe/commit/0ed8fd9)) feat: basic git dirty/pull/push text on repo comps - Russell Matney
- ([`16d58ab`](https://github.com/russmatney/clawe/commit/16d58ab)) chore: drop some unused topbar code - Russell Matney
- ([`196f372`](https://github.com/russmatney/clawe/commit/196f372)) feat: add client icons to workspaces widget - Russell Matney

  > Also pulls client-icon-list out of topbar, and adds repos to the
  > workspaces widget - should be able to add remove workspaces based on
  > repo next.

- ([`acb9039`](https://github.com/russmatney/clawe/commit/acb9039)) feat: restore journal page - Russell Matney

  > updating frontend db usage, cleaning up some still broken components.

- ([`ea49896`](https://github.com/russmatney/clawe/commit/ea49896)) refactor: pull db atom into hooks/use-db, write use-query - Russell Matney

  > Todos now causing the page to re-render on the first load, so that's something.

- ([`3c53b06`](https://github.com/russmatney/clawe/commit/3c53b06)) fix: misc logging clean up - Russell Matney
- ([`4dcaef6`](https://github.com/russmatney/clawe/commit/4dcaef6)) fix: convert lazy seqs to vectors when ingesting - Russell Matney
- ([`7c8c27e`](https://github.com/russmatney/clawe/commit/7c8c27e)) chore: drop `clawe-*` doctor commands, move focus -> dashboard - Russell Matney
- ([`0dae9ac`](https://github.com/russmatney/clawe/commit/0dae9ac)) fix: drop dead namespace import - Russell Matney

### 13 Dec 2024

- ([`6f9b8c5`](https://github.com/russmatney/clawe/commit/6f9b8c5)) fix: restore workspaces, wip on chess components - Russell Matney
- ([`f12a5d6`](https://github.com/russmatney/clawe/commit/f12a5d6)) feat: restore some tables, more subtle hiccup vs uix bugs - Russell Matney
- ([`c339096`](https://github.com/russmatney/clawe/commit/c339096)) refactor: consolidate git.core/api.repos - Russell Matney
- ([`5aa436d`](https://github.com/russmatney/clawe/commit/5aa436d)) refactor: move hooks.* into doctor.ui.hooks* - Russell Matney
- ([`00a3829`](https://github.com/russmatney/clawe/commit/00a3829)) refactor: move clips/screenshots/wallpapers to api., ralphie. - Russell Matney

  > Pulling config vars and moving db-dependent things into api.*

- ([`04092b0`](https://github.com/russmatney/clawe/commit/04092b0)) refactor: more wallpaper clean up - Russell Matney

  > Add a wallpaper dir to ralphie.config, drop ensure-wallpaper at server
  > startup.

- ([`1d5220c`](https://github.com/russmatney/clawe/commit/1d5220c)) refactor: move all is-mac?/osx? usage to ralphie.config - Russell Matney

  > This was technically cached in clawe.config before, but w/e we don't
  > want to import clawe.config just to check this

- ([`3b69cac`](https://github.com/russmatney/clawe/commit/3b69cac)) refactor: pull os detail into ralphie.wallpaper - Russell Matney

  > Kind of a mess of namespaces here - what lives in ralphie, what gets a
  > top-level namespace? wallpapers.core? clawe/ralphie/doctor? Not sure
  > what makes sense here.

- ([`3310605`](https://github.com/russmatney/clawe/commit/3310605)) misc: some crufty todos - Russell Matney
- ([`61fbe2d`](https://github.com/russmatney/clawe/commit/61fbe2d)) fix: rewrite main to hang around after server restarts - Russell Matney

  > Much thanks to @rschmukler for this - the jvm was shutting down ~30s
  > after restarting the undertow server, b/c the main func here was quite
  > naive. This rewrite it to do a graceful systemic shutdown, and hang
  > around regardless of system/server restarts.

- ([`370f0f6`](https://github.com/russmatney/clawe/commit/370f0f6)) fix: better (zsh-less) env check - Russell Matney

  > Need to confirm this on osx as well, but it should be fine

- ([`b765cdd`](https://github.com/russmatney/clawe/commit/b765cdd)) fix: drop telemere usage in bb file - Russell Matney
- ([`77e0fae`](https://github.com/russmatney/clawe/commit/77e0fae)) wip: debugging server exit after system restart - Russell Matney
- ([`4b29a94`](https://github.com/russmatney/clawe/commit/4b29a94)) feat: cleaner server log prefix - Russell Matney

  > no need for this to be so wide

- ([`e19ceb0`](https://github.com/russmatney/clawe/commit/e19ceb0)) fix: slower pomodoro intervals, push topbar data on pomo start/stop - Russell Matney
- ([`c3d2ef6`](https://github.com/russmatney/clawe/commit/c3d2ef6)) fix: misc icon rendering, always show side bar fix - Russell Matney
- ([`f14a6d5`](https://github.com/russmatney/clawe/commit/f14a6d5)) feat: frontend custom log format - Russell Matney
- ([`d58151f`](https://github.com/russmatney/clawe/commit/d58151f)) refactor: drop timbre, move to telemere - Russell Matney

  > Adds a file logger to the backend, starts to work with a formatted
  > logger on the frontend.
  > 
  > Unfortunately telemere doesn't support babashka yet, so we just println
  > in a few places.


### 12 Dec 2024

- ([`f67daf0`](https://github.com/russmatney/clawe/commit/f67daf0)) feat: impl clawe wm protocol for sway - Russell Matney

### 11 Dec 2024

- ([`837e9ab`](https://github.com/russmatney/clawe/commit/837e9ab)) wip: towards a sway wm protocol impl - Russell Matney

  > Pulls clawe's i3 impl into a sway-named protocol. it roughly works (with
  > some uncommitable local changes). now to use this i3 version while
  > impling the sway bits.


### 10 Dec 2024

- ([`db46fb0`](https://github.com/russmatney/clawe/commit/db46fb0)) chore: drop yabai - Russell Matney
- ([`18fe2fc`](https://github.com/russmatney/clawe/commit/18fe2fc)) yabai: rework topbar updates to use signals - Russell Matney
- ([`97c4d69`](https://github.com/russmatney/clawe/commit/97c4d69)) fix: update clj-kondo config - Russell Matney
- ([`d9495a8`](https://github.com/russmatney/clawe/commit/d9495a8)) perf: for whatever reason, this import order is twice as fast - Russell Matney

  > Not exactly sure why...

- ([`abdaed7`](https://github.com/russmatney/clawe/commit/abdaed7)) fix: mac bb stop-doctor fixes - Russell Matney
- ([`4fd02a1`](https://github.com/russmatney/clawe/commit/4fd02a1)) yabai: move to bb-based clawe commands - Russell Matney

  > Rather than going through the server, we just use the clawe functions
  > directly.

- ([`8211e00`](https://github.com/russmatney/clawe/commit/8211e00)) misc: some junk while hunting down more toggle-perf - Russell Matney
- ([`7415e0c`](https://github.com/russmatney/clawe/commit/7415e0c)) fix: drop unused clawe.restart/reload funcs - Russell Matney

  > No more bindings/sxhkd/etc support - write your own wm config and use
  > clawebb where relevant.

- ([`da6838f`](https://github.com/russmatney/clawe/commit/da6838f)) fix: support :key or :client/key in logs - Russell Matney
- ([`2973c06`](https://github.com/russmatney/clawe/commit/2973c06)) perf: don't shell out to check if osx per rofi label - Russell Matney

  > Holy crap this is what has made rofi/rofi feel slow for a year

- ([`a306b55`](https://github.com/russmatney/clawe/commit/a306b55)) chore: drop i3 config - Russell Matney

  > moving this back into my dotfiles repo

- ([`79f9776`](https://github.com/russmatney/clawe/commit/79f9776)) fix: mac-aware bb.edn - Russell Matney

  > cleaning up some cruft


### 9 Dec 2024

- ([`ed86d9e`](https://github.com/russmatney/clawe/commit/ed86d9e)) feat: some sway workspace hacking - Russell Matney

### 5 Dec 2024

- ([`99392a2`](https://github.com/russmatney/clawe/commit/99392a2)) fix: restore pushing topbar data updates - Russell Matney
- ([`fd82077`](https://github.com/russmatney/clawe/commit/fd82077)) wip: drop time/interval update from topbar state - Russell Matney

  > this is causing a big redraw/rerender that we don't need just yet

- ([`6ef01ca`](https://github.com/russmatney/clawe/commit/6ef01ca)) fix: drop noisey logs, fix client-icon rendering - Russell Matney

### 4 Dec 2024

- ([`bba7009`](https://github.com/russmatney/clawe/commit/bba7009)) wip: topbar at least rendering, mac restart/tail bb helpers - Russell Matney
- ([`22a6710`](https://github.com/russmatney/clawe/commit/22a6710)) feat: better behaved icons - Russell Matney

  > nearly back!

- ([`3891f3b`](https://github.com/russmatney/clawe/commit/3891f3b)) fix: misc item->comp, group->comp fixes - Russell Matney

  > ->comp now means we return a rendered (uix.core/$) comp, which doesn't
  > need to be rendered again.

- ([`75f4f29`](https://github.com/russmatney/clawe/commit/75f4f29)) deps: update plasma reference - Russell Matney
- ([`90a1188`](https://github.com/russmatney/clawe/commit/90a1188)) feat: restore fe db ingestion and queries - Russell Matney

  > I'm not 100% this datascript bootstrapping makes sense - some murky
  > regent reaction vs uix/react use-effect hooks on top of datascript's
  > conn vs db / transact! situation.
  > 
  > Anyway, it seems to be working well enough for now!

- ([`6b3a500`](https://github.com/russmatney/clawe/commit/6b3a500)) feat: drop/port remaining hiccup - Russell Matney

  > There's a bunch of hiccup in src/blog/*, but that should stay as-is.

- ([`9aee96f`](https://github.com/russmatney/clawe/commit/9aee96f)) fix: guard on null string - Russell Matney
- ([`cd92b86`](https://github.com/russmatney/clawe/commit/cd92b86)) feat: restore i3 :workspace/focused flag - Russell Matney

  > Seems like i3's get_tree no longer (did it ever?) supports the :focused
  > flag on workspaces - we instead merge it from i3 get_workspaces

- ([`5750aeb`](https://github.com/russmatney/clawe/commit/5750aeb)) fix: urls need fragments now - Russell Matney

  > like it's 2013!

- ([`9c0e863`](https://github.com/russmatney/clawe/commit/9c0e863)) feat: pulling data again! - Russell Matney

  > Also adds a favicon and icon for ff dev edition's new name.
  > 
  > Reimpls with-rpc, with-stream in terms of new uix hook impls.
  > 
  > Disables pie-charts that are crashing b/c of reuse.

- ([`7cb1a41`](https://github.com/russmatney/clawe/commit/7cb1a41)) feat: finish first pass porting to new uix style - Russell Matney

  > All pages and views rendering!
  > 
  > No data anywhere yet.

- ([`adbae29`](https://github.com/russmatney/clawe/commit/adbae29)) wip: port rest of pages - Russell Matney

  > just a few views left

- ([`79d0fae`](https://github.com/russmatney/clawe/commit/79d0fae)) wip: supporting a few more views/pages - Russell Matney
- ([`c1ee4ce`](https://github.com/russmatney/clawe/commit/c1ee4ce)) fix: disabling more icons/comps - Russell Matney

  > Plus a lingering bit of hiccup

- ([`a1fe237`](https://github.com/russmatney/clawe/commit/a1fe237)) chore: add telemere, small icon fixes - Russell Matney
- ([`0b7fd12`](https://github.com/russmatney/clawe/commit/0b7fd12)) wip: fix hooks/other react warnings, update wing - Russell Matney

  > Also adds `--all --no-hostname` to journalctl tails to colorized and
  > narrow them.

- ([`107690c`](https://github.com/russmatney/clawe/commit/107690c)) wip: clj-kondo configs, defui arity fix - Russell Matney

### 3 Dec 2024

- ([`038d63e`](https://github.com/russmatney/clawe/commit/038d63e)) wip: porting.... more.... components. - Russell Matney
- ([`9f9f289`](https://github.com/russmatney/clawe/commit/9f9f289)) fix: js-joda versions set by shadow - Russell Matney
- ([`ade238b`](https://github.com/russmatney/clawe/commit/ade238b)) wip: fix routing, clear some errors, drop plasma hooks - Russell Matney
- ([`043802a`](https://github.com/russmatney/clawe/commit/043802a)) wip: more component porting - Russell Matney

  > Maybe halfway there?

- ([`7b0f355`](https://github.com/russmatney/clawe/commit/7b0f355)) wip: bunch more component refactors - Russell Matney
- ([`b71a35b`](https://github.com/russmatney/clawe/commit/b71a35b)) wip: break out use-interval, support fast refresh - Russell Matney
- ([`3e80b33`](https://github.com/russmatney/clawe/commit/3e80b33)) wip: component uix refactor well underway - Russell Matney
- ([`9095991`](https://github.com/russmatney/clawe/commit/9095991)) wip: uix-upgraded frontend building - Russell Matney

  > Lots of commented out widgets, pages, components, etc.

- ([`1e06bb5`](https://github.com/russmatney/clawe/commit/1e06bb5)) deps: update npm modules - Russell Matney

  > Apparently i use npm and yarn in this project

- ([`dd7a6ad`](https://github.com/russmatney/clawe/commit/dd7a6ad)) deps: update outdated - Russell Matney

### 1 Dec 2024

- ([`b8115e3`](https://github.com/russmatney/clawe/commit/b8115e3)) wip: not sound logic, but pomodoros a step better - Russell Matney

### 27 Nov 2024

- ([`21a130c`](https://github.com/russmatney/clawe/commit/21a130c)) wip: attempt to use fetch-via-tmux - Russell Matney

  > but apparently not working yet?

- ([`b701d29`](https://github.com/russmatney/clawe/commit/b701d29)) feat: add async git fetch via tmux helper - Russell Matney

  > A helper for firing git-fetch in a tmux session named after the repo
  > name. using tmux for something like this frees up the calling process from
  > waiting for a long fetch, and also sets up the user to jump to the
  > session/more easily see what has been updated. kind of like using tmux
  > to grab the logs?


### 26 Nov 2024

- ([`e8cedb5`](https://github.com/russmatney/clawe/commit/e8cedb5)) fix: also disabling sxhkd for now - Russell Matney

### 22 Nov 2024

- ([`fe21dc1`](https://github.com/russmatney/clawe/commit/fe21dc1)) fix: better error handling on some rofi suggestions - Russell Matney

### 14 Nov 2024

- ([`a0210cf`](https://github.com/russmatney/clawe/commit/a0210cf)) fix: unity toggle, better i3 floating-emacs, restore move-to-space - Russell Matney

### 10 Nov 2024

- ([`c66e779`](https://github.com/russmatney/clawe/commit/c66e779)) fix: don't show 1password (in every workspace?) - Russell Matney

### 9 Nov 2024

- ([`60538bc`](https://github.com/russmatney/clawe/commit/60538bc)) fix: smarter fetch-current on frontend and backend - Russell Matney

  > I can't believe this code exists in both places :/
  > 
  > We no longer assume there's only one :current true pomodoro in the
  > database. might be easier to think of them as open vs closed, and
  > include some metadata/process in 'closing' them out
  > (e.g. stamping a doctor's report with commit and repos-pushed counts)

- ([`dc132c4`](https://github.com/russmatney/clawe/commit/dc132c4)) fix: disable send-client-to-numbered-workspace for 1-5 - Russell Matney

  > This had to be disabled on OSX in favor of their screenshotting
  > keybindings, so i got out of the habit. And really, the first 5
  > workspaces should have enough automation to not need this.

- ([`c670a8c`](https://github.com/russmatney/clawe/commit/c670a8c)) feat: refresh git status for some repos on reload - Russell Matney

  > This could use some wisdom around debouncing/throttling. I think firing
  > only once per pomodoro might be dwim - i'd like to know the status at
  > the beginning of each work sesh. Though, the end of a pomodoro is
  > actually when you'd ideally 'push', i also don't want to create more
  > work before walking away. Maybe there's a 'clean-up' pomodoro where you
  > get off the computer, capture, and prioritize?

- ([`a825b9d`](https://github.com/russmatney/clawe/commit/a825b9d)) feat: start a new pomodoro every time we clawe-reload - Russell Matney

  > I don't reload often, so this might be a nice boon. I suspect we'll have
  > to be careful about the sometimes-end-pomodoro datas, but i think going
  > loosey goosey on the data will free up more opportunities than holding
  > back on the data at this point.
  > 
  > Really this should be a smart-pomo-toggle helper that can get called
  > whenever. One step closer to automagic-pomodoros and 'modes' (in this
  > case, break-vs-working ui modes).


### 3 Nov 2024

- ([`599c9e9`](https://github.com/russmatney/clawe/commit/599c9e9)) fix: restart i3 even if sxhkd isn't running - Russell Matney

  > my laptop doesn't run sxhkd, so this restores the i3 restart binding

- ([`2081419`](https://github.com/russmatney/clawe/commit/2081419)) feat: rewrite local clips path with bb/fs commands - Russell Matney

  > Much nicer than shelling out to `ls` like a heathen

- ([`d986ea8`](https://github.com/russmatney/clawe/commit/d986ea8)) fix: moving more bindings to mod+shift - Russell Matney

  > the mod+blah tend to conflict on osx - this is an effort to keep the
  > bindings more or less the same.

- ([`e2460eb`](https://github.com/russmatney/clawe/commit/e2460eb)) chore: disable sxhkd app bindings - Russell Matney

  > These are all hard-coded via i3/yabai now

- ([`7084cb4`](https://github.com/russmatney/clawe/commit/7084cb4)) feat: watched-repos filtering - Russell Matney

  > Showing fewer repos, getting back into the flow of the dashboard a bit.

- ([`b375db5`](https://github.com/russmatney/clawe/commit/b375db5)) fix: pass opts into git/ingestor widgets - Russell Matney
- ([`581fd46`](https://github.com/russmatney/clawe/commit/581fd46)) chore: osx obsidian and devweb toggles - Russell Matney

### 22 Sep 2024

- ([`4adafa1`](https://github.com/russmatney/clawe/commit/4adafa1)) i3: obsidian toggle - Russell Matney

### 15 Sep 2024

- ([`2979141`](https://github.com/russmatney/clawe/commit/2979141)) i3: adjust some bindings to match osx - Russell Matney

  > moving more app toggles to mod+alt b/c osx is more crowded on mod+
  > bindings.
  > 
  > A trend:
  > - mod+shift - for internal clawe-meta tasks
  > - mod+alt - for external 'app' toggles


### 5 Aug 2024

- ([`f59f9ff`](https://github.com/russmatney/clawe/commit/f59f9ff)) feat: saner osx bindings - Russell Matney

### 20 Jun 2024

- ([`19f7511`](https://github.com/russmatney/clawe/commit/19f7511)) fix: readme discord link - Russell Matney

### 18 Jun 2024

- ([`f839c19`](https://github.com/russmatney/clawe/commit/f839c19)) fix: remove grid from godot scratchpad rules - Russell Matney

  > finally not resizing popups/subwindows! yay!

- ([`69c4a8d`](https://github.com/russmatney/clawe/commit/69c4a8d)) osx: toggleable godot rules - Russell Matney

  > Adds more matching rules for godot - it seems to come in capitalized,
  > lower-cased, and otherwise. here we match on app name and title.
  > probably there's a better yabai syntax for this.
  > 
  > The windows are still getting resized, i think b/c of the scratchpad
  > rules.

- ([`76cd85d`](https://github.com/russmatney/clawe/commit/76cd85d)) osx: move off of typical mac bindings - Russell Matney

  > Finally folding on osx's cut/copy/paste/save/select-all bindings -
  > there's no real way to globally move those to the control key. it ends
  > up being broken/custom per app (and still doesn't always work), and
  > changing the modifier keys themselves breaks typical terminal
  > usage (e.g. ctrl+c to kill the running script).


### 15 Jun 2024

- ([`d930d03`](https://github.com/russmatney/clawe/commit/d930d03)) i3: misc tweaks on a read-through - Russell Matney

  > There's a startup bug at the moment - sxhkd grabs keybindings before i3
  > does. I've been stopping sxhkd, restarting i3 (and clawe), then restarting sxhkd -
  > on this read i realized clawe's reload already ensures sxhkd (which
  > might be how it's grabbing the bindings so early), so i don't need
  > to restart sxhkd an extra time afterwards.
  > 
  > then i had a better idea:
  > 
  > bindsym $mod+Shift+r exec 'systemctl --user stop sxhkd && i3-msg restart'


### 5 Jun 2024

- ([`dc3d285`](https://github.com/russmatney/clawe/commit/dc3d285)) wip: toying with some trello json - Russell Matney

### 4 Jun 2024

- ([`34b8342`](https://github.com/russmatney/clawe/commit/34b8342)) chore: add ko-fi funding link - Russell Matney

### 29 Apr 2024

- ([`af274dc`](https://github.com/russmatney/clawe/commit/af274dc)) fix: open terminal along with emacs in new mx-workspaces - Russell Matney

  > been relying more on the floating emacs window lately, and i'm wondering
  > if creating the terminal for these workspaces will help them stick
  > around more...


### 17 Apr 2024

- ([`2027c47`](https://github.com/russmatney/clawe/commit/2027c47)) i3: mod shift grave to move to scratchpad - Russell Matney
- ([`d8db307`](https://github.com/russmatney/clawe/commit/d8db307)) fix: float and center sushi preview - Russell Matney
- ([`48c902c`](https://github.com/russmatney/clawe/commit/48c902c)) fix: restore clawe toggle - Russell Matney

  > Seems like the babashka/cli :alias behavior broke, or changed? Not sure
  > why this doesn't work anymore.
  > 
  > For now, just support both :key and :client/key


### 13 Apr 2024

- ([`e4ca1b0`](https://github.com/russmatney/clawe/commit/e4ca1b0)) fix: support tmux/fire for workspaces with dots - Russell Matney

### 7 Apr 2024

- ([`0b11444`](https://github.com/russmatney/clawe/commit/0b11444)) fix: dedupe client icons - Russell Matney
- ([`3f96b10`](https://github.com/russmatney/clawe/commit/3f96b10)) feat: move to yabai scratchpads - Russell Matney

  > finally toggling is fast! Cuts off clawe-toggle via yabai's new
  > scratchpads feature. Could do the same within clawe-toggle for toggling
  > per-workspace/project/repo things - an optimistic scratchpad toggle
  > attempt followed by the expected open-app command. wonder how much of
  > that time is querying vs parsing the clawe.edn vs processing.


### 6 Apr 2024

- ([`7e1b668`](https://github.com/russmatney/clawe/commit/7e1b668)) fix: finally fix yabai focus ordering - Russell Matney

  > went way too long with the proper fix here, very happy to have my
  > workflow back!


### 4 Apr 2024

- ([`65a3b7e`](https://github.com/russmatney/clawe/commit/65a3b7e)) i3: hide gdunit windows, don't hide firefox popups - Russell Matney

### 26 Mar 2024

- ([`c666403`](https://github.com/russmatney/clawe/commit/c666403)) feat: quick mod+e toggle for an extra emacs frame - Russell Matney

### 24 Mar 2024

- ([`055c1ba`](https://github.com/russmatney/clawe/commit/055c1ba)) wip: basic impl for a default workspaces/clients feature - Russell Matney

  > Adds support for :client/open, :workspace/open on client and workspace
  > defs, then adds hooks to filter and fire this on :clawe/restart.
  > 
  > Doesn't quite work as implemented - we'll want to create clients in
  > those workspaces to trick the wms to keep them open for now.
  > 
  > Alternatively, we reconfig i3 to keep empty workspaces alive, and get
  > into manually deleting workspaces.


### 17 Mar 2024

- ([`36701d6`](https://github.com/russmatney/clawe/commit/36701d6)) feat: split :wm config key into .local config - Russell Matney

  > Moves clawe.edn config file to a dropbox location, and refactors to
  > support a .local/ based config for the :wm key. A bit messier than
  > necessary, but w/e it'll work fine for another year or two.


### 20 Feb 2024

- ([`3987390`](https://github.com/russmatney/clawe/commit/3987390)) wip: disable git status bar - Russell Matney

  > Takes up too much space on the laptop, and i don't use it anyway


### 9 Feb 2024

- ([`cfadeb4`](https://github.com/russmatney/clawe/commit/cfadeb4)) fix: stop 'managing' godot popups into the background - Russell Matney

### 8 Feb 2024

- ([`2571ea8`](https://github.com/russmatney/clawe/commit/2571ea8)) feat: binding to set to reasonable recording size - Russell Matney

### 20 Jan 2024

- ([`e7ada51`](https://github.com/russmatney/clawe/commit/e7ada51)) feat: publish devlog .html files to the blog - Russell Matney

  > Cool to see this working. Could probably use an index.html (devlogs
  > 'home') at ~russmatney.com/devlogs~.

- ([`7ba9e07`](https://github.com/russmatney/clawe/commit/7ba9e07)) refactor: split out header/footer into blog.components - Russell Matney
- ([`2f4291e`](https://github.com/russmatney/clawe/commit/2f4291e)) feat: add notify to toggle create-client - Russell Matney

### 19 Jan 2024

- ([`32a70b9`](https://github.com/russmatney/clawe/commit/32a70b9)) blog: add youtube link to header - Russell Matney

### 18 Jan 2024

- ([`eed2dbd`](https://github.com/russmatney/clawe/commit/eed2dbd)) i3: dot hop godot toggle, garden-reveal-slides clove app - Russell Matney

### 23 Nov 2023

- ([`c83f552`](https://github.com/russmatney/clawe/commit/c83f552)) blog: homepage clean up, link color shift - Russell Matney
- ([`890127a`](https://github.com/russmatney/clawe/commit/890127a)) fix: safe app name handling - Russell Matney

  > Started seeing some null pointers here when odd windows show up in
  > i3 (like screen-sharing).


### 22 Nov 2023

- ([`78547c5`](https://github.com/russmatney/clawe/commit/78547c5)) fix: set remote plasma dep rather than local - Russell Matney
- ([`7ee581f`](https://github.com/russmatney/clawe/commit/7ee581f)) chore: add 'test' bb task for running all tests - Russell Matney
- ([`5ed2f27`](https://github.com/russmatney/clawe/commit/5ed2f27)) feat: api.repos/get-commits fetching db commits between before/after timestamps - Russell Matney

  > Impls a new api.repos function called get-commits for fetching db
  > commits between the passed before/after timestamps. Defaults to fetching
  > commits for the current day (since 12 am this morning).
  > 
  > Intended to support per-day and per-pomodoro commit fetching. Could use
  > some tests.

- ([`10a5f05`](https://github.com/russmatney/clawe/commit/10a5f05)) i3: wrap focus - Russell Matney
- ([`ee00eb6`](https://github.com/russmatney/clawe/commit/ee00eb6)) fix: misc linter ignores - Russell Matney
- ([`f96fca5`](https://github.com/russmatney/clawe/commit/f96fca5)) fix: revert zprint, which crashes in babashka - Russell Matney
- ([`296a4c6`](https://github.com/russmatney/clawe/commit/296a4c6)) chore: update deps - Russell Matney

### 12 Nov 2023

- ([`d2fdcfd`](https://github.com/russmatney/clawe/commit/d2fdcfd)) yabai: default to layer 'normal' - Russell Matney

  > yabai 6.0 puts all tiled windows in a 'below' layer, which means
  > focusing them does not bring them above other floating windows, which is
  > super annoying for my usage.
  > 
  > This sets all windows in yabai to the 'normal' layer, which fixes most
  > of my workflows.
  > 
  > Much thanks to https://github.com/koekeishiya/yabai/issues/1912 for the solution.


### 9 Nov 2023

- ([`cacb5e9`](https://github.com/russmatney/clawe/commit/cacb5e9)) readme: add badges - Russell Matney

### 30 Oct 2023

- ([`ce1baa7`](https://github.com/russmatney/clawe/commit/ce1baa7)) chore: disable mx invokation on new workspace open - Russell Matney

  > This hasn't been used much since the mod-o and mod-w have come into play.


### 17 Oct 2023

- ([`b695d96`](https://github.com/russmatney/clawe/commit/b695d96)) fix: specify tauri/nvidia env var fix - Russell Matney

  > Nvidia vs webkit2gtk is broken, but is also opt-out via this env var.


### 4 Oct 2023

- ([`00b730c`](https://github.com/russmatney/clawe/commit/00b730c)) fix: dissoc clients from wsps in clawe-mx - Russell Matney

  > Clawe-mx does not depend on the workspace's clients for suggestions (tho
  > maybe it could).
  > 
  > Removing the clients to prevent cache-busting (and slow mx commands).


### 27 Sep 2023

- ([`545f5ff`](https://github.com/russmatney/clawe/commit/545f5ff)) fix: larger 'centered' default size - Russell Matney
- ([`0c383ed`](https://github.com/russmatney/clawe/commit/0c383ed)) fix: remove more invalid anchor text - Russell Matney

  > Should switch to just alphanumeric for this at some point, not sure what
  > i'm waiting for. learning about more ascii bugs?


### 17 Sep 2023

- ([`2c5dedd`](https://github.com/russmatney/clawe/commit/2c5dedd)) chore: fix lint errors - Russell Matney
- ([`d6262bb`](https://github.com/russmatney/clawe/commit/d6262bb)) topbar: combine pomodoro with topbar actions list - Russell Matney

### 15 Sep 2023

- ([`ec1a91e`](https://github.com/russmatney/clawe/commit/ec1a91e)) chore: lint fix, note re: moving to undefined workspace - Russell Matney

### 13 Sep 2023

- ([`e833e0e`](https://github.com/russmatney/clawe/commit/e833e0e)) fix: float godot games by default - Russell Matney

### 10 Sep 2023

- ([`2b32630`](https://github.com/russmatney/clawe/commit/2b32630)) docs: update moved asset dir - Russell Matney

  > quite old, this one


### 7 Sep 2023

- ([`faf3b00`](https://github.com/russmatney/clawe/commit/faf3b00)) feat: faster open workspace - Russell Matney

  > New mx-open command that ONLY opens new workspaces. The mx and
  > mx-suggest versions run very slow :/, so this is a bandaid until those
  > get into shape.


### 5 Sep 2023

- ([`571f0ef`](https://github.com/russmatney/clawe/commit/571f0ef)) fix: strip wsp passed into memoized mx-commands - Russell Matney

  > Stripping here should be more stable and hopefully not miss the cache as
  > much. Still, this is quite slow the first time. Moving to a lazy/async
  > rofi might improve it?


### 4 Sep 2023

- ([`50a5e32`](https://github.com/russmatney/clawe/commit/50a5e32)) feat: i3 mod-play to target spotify - Russell Matney

### 29 Aug 2023

- ([`55d85e0`](https://github.com/russmatney/clawe/commit/55d85e0)) wip: towards auto-formatting the clawe.edn file - Russell Matney

  > The formatting toggling between zprint and cljfmt/aggressive-indent is a
  > headache - this is a rough version of how to avoid it. ideally the file
  > is both machine- and by-hand- editable.


### 21 Aug 2023

- ([`20b6d85`](https://github.com/russmatney/clawe/commit/20b6d85)) fix: skip 0 when picking next-wsp-number - Russell Matney

### 15 Aug 2023

- ([`dc4996b`](https://github.com/russmatney/clawe/commit/dc4996b)) wip: attempt to cut off aseprite/godot with fast-scratchpad - Russell Matney
- ([`2961802`](https://github.com/russmatney/clawe/commit/2961802)) feat: better rofi command - Russell Matney

  > Including drun to support launching most applications.


### 11 Aug 2023

- ([`2c5854f`](https://github.com/russmatney/clawe/commit/2c5854f)) fix: blog anchor links and misc clean up - Russell Matney

  > Updates a handful of string-cleanup resolving tidy errors. Note that
  > some of these were solved by removing bad characters from org titles -
  > should probably switch to only pulling valid chars instead of trying to
  > filter out known bad chars.


### 9 Aug 2023

- ([`dcce72e`](https://github.com/russmatney/clawe/commit/dcce72e)) fix: yabai window bindings shift - Russell Matney

  > More aligned with i3/awm bindings


### 27 Jul 2023

- ([`2c76e43`](https://github.com/russmatney/clawe/commit/2c76e43)) fix: some reload/cleanup attention - Russell Matney

  > Toying with how to restore label names across yabai restarts.

- ([`b819774`](https://github.com/russmatney/clawe/commit/b819774)) fix: more common tags, fix add-tag bug - Russell Matney
- ([`5178554`](https://github.com/russmatney/clawe/commit/5178554)) feat: remove spaces from selections - Russell Matney

  > support selecting 'game assets' as a 'gameassets' tag

- ([`c675262`](https://github.com/russmatney/clawe/commit/c675262)) fix: try/catch topbar metadata funcs - Russell Matney
- ([`8562354`](https://github.com/russmatney/clawe/commit/8562354)) feat: impl hide-scratchpad for yabai - Russell Matney

  > Same impl, but DRYed up the client->workspace-title bit

- ([`7d15ed4`](https://github.com/russmatney/clawe/commit/7d15ed4)) feat: ensure :wm is set when yabai reloads - Russell Matney
- ([`fba33ae`](https://github.com/russmatney/clawe/commit/fba33ae)) wip: toying with quick clawe.toggle on osx - Russell Matney
- ([`ce98946`](https://github.com/russmatney/clawe/commit/ce98946)) feat: show decorations (titlebar) on clove dashboard/focus - Russell Matney

  > The titlebar seems to make a difference for yabai recognizing/managing
  > the created app. If this could be toggled at runtime, it'd be more
  > ideal, so the topbar could be positioned and then the titlebar dropped.


### 26 Jul 2023

- ([`37cdf5a`](https://github.com/russmatney/clawe/commit/37cdf5a)) wip: re-enable some topbar metadata, wip i3 subscribe - Russell Matney
- ([`f165c1a`](https://github.com/russmatney/clawe/commit/f165c1a)) fix: use new --transparent flag - Russell Matney

### 25 Jul 2023

- ([`80cf1bb`](https://github.com/russmatney/clawe/commit/80cf1bb)) fix: reload clawe via backend instead of frontend - Russell Matney

  > Reloads clawe when i3 starts, which might be problemmatic when first
  > starting up.
  > 
  > Also supports a 1password scratchpad.

- ([`f2f5413`](https://github.com/russmatney/clawe/commit/f2f5413)) wip: rough drag-wsp impl - Russell Matney
- ([`921345a`](https://github.com/russmatney/clawe/commit/921345a)) feat: impl hide-scratchpad for i3 wm protocol - Russell Matney

  > clawe-toggle now fast! clients that get moved into the view get sent
  > back to the scratchpad via :hide, so they are very fast to recall. woo!

- ([`b4d1bb7`](https://github.com/russmatney/clawe/commit/b4d1bb7)) fix: clawe-toggle hide fixed - Russell Matney

  > Just need to use the better wm/ version of this func instead of calling
  > the protocol immediately.

- ([`b2ac425`](https://github.com/russmatney/clawe/commit/b2ac425)) i3: window resizing clean up - Russell Matney

  > Also noting that right now bindings are supported in sxkhd and i3 -
  > reloading one or the other can grab whatever keybindings are configured,
  > and they don't seem to dupe calls if both are registered.

- ([`69cd689`](https://github.com/russmatney/clawe/commit/69cd689)) fix: i3 wsps are zero-based, so we don't inc i - Russell Matney

  > Hopefully we resolve this pattern. I'm feeling like a computed index at
  > config-read-time would help in a few places, b/c then we can build a
  > proper/up-to-date i3 wsp name on the fly.

- ([`29c1ec4`](https://github.com/russmatney/clawe/commit/29c1ec4)) fix: trim workspace names (i3 adds padding?!), impl swap-wsp-by-ix - Russell Matney

  > Finally auto-sorting workspaces! we're getting there!

- ([`b0b1466`](https://github.com/russmatney/clawe/commit/b0b1466)) fix: don't reference wsp names in wsp kbds - Russell Matney

  > The names come from clawe workspace creation now, so we don't need to
  > enforce/couple names and indexes

- ([`9ee1455`](https://github.com/russmatney/clawe/commit/9ee1455)) fix: clean up workspaces (move-c-to-wsp) finding existing wsps - Russell Matney
- ([`2a28e93`](https://github.com/russmatney/clawe/commit/2a28e93)) feat: handle move-client-to-wsp when wsp doesn't exist - Russell Matney

  > For now, creates a dupe workspace - need to check if a matching one
  > exists first, maybe via an i3 find-or-create wsp helper.

- ([`543d5b8`](https://github.com/russmatney/clawe/commit/543d5b8)) feat: now creating workspaces in i3 via mx - Russell Matney

  > Bunch of nil-punning to prevent crashes when, e.g. a workspace doesn't
  > exist.

- ([`c7eef1d`](https://github.com/russmatney/clawe/commit/c7eef1d)) fix: include timeliterals on data-readers - Russell Matney

  > May have hit this eventually.


### 24 Jul 2023

- ([`2b97123`](https://github.com/russmatney/clawe/commit/2b97123)) feat: fast scratchpad show with clawe.toggle fallback - Russell Matney
- ([`d7072e6`](https://github.com/russmatney/clawe/commit/d7072e6)) feat: fallback move to scratchpad - Russell Matney
- ([`ae15c69`](https://github.com/russmatney/clawe/commit/ae15c69)) fix: support including --no-auto-back-and-forth in i3 cmds - Russell Matney
- ([`d6b3e34`](https://github.com/russmatney/clawe/commit/d6b3e34)) feat: clawe/toggle roughly working in i3 - Russell Matney

  > hiding not working yet, and showing moves to a different workspace for
  > some reason.

- ([`7ac190b`](https://github.com/russmatney/clawe/commit/7ac190b)) feat: rough wm client impl for i3 containers - Russell Matney
- ([`e0bbdc7`](https://github.com/russmatney/clawe/commit/e0bbdc7)) feat: listing i3 clients, plus more i3 impl cleanup - Russell Matney
- ([`f4354c7`](https://github.com/russmatney/clawe/commit/f4354c7)) wip: i3 swap/delete wips/notes - Russell Matney
- ([`c5353f3`](https://github.com/russmatney/clawe/commit/c5353f3)) feat: term/emacs starting with respect to i3 workspace - Russell Matney

  > Starting to see i3 workspaces in the topbar as well!

- ([`009e125`](https://github.com/russmatney/clawe/commit/009e125)) feat: handle i3 as clawe wm, clean up i3-msg! impl - Russell Matney
- ([`326b5fc`](https://github.com/russmatney/clawe/commit/326b5fc)) feat: quick set-wm/get-wm abstraction - Russell Matney
- ([`7cff847`](https://github.com/russmatney/clawe/commit/7cff847)) fix: floating rofi in i3 - Russell Matney
- ([`88e9816`](https://github.com/russmatney/clawe/commit/88e9816)) feat: display git dirty/pull/push status in table and topbar - Russell Matney
- ([`8f5ae1e`](https://github.com/russmatney/clawe/commit/8f5ae1e)) feat: some date comparison helpers - Russell Matney

  > Could dry up against sort, but need to handle/think-through the
  > null-input case properly.

- ([`5a112fc`](https://github.com/russmatney/clawe/commit/5a112fc)) misc: i3 moving containers to wsps, more sticky windows - Russell Matney
- ([`b706fd0`](https://github.com/russmatney/clawe/commit/b706fd0)) feat: use components.debug as data popup - Russell Matney

  > Very helpful while developing!

- ([`75d595e`](https://github.com/russmatney/clawe/commit/75d595e)) fix: narrower dashboard bars - Russell Matney

  > Allows drawer-menu to open fully.

- ([`15ea35b`](https://github.com/russmatney/clawe/commit/15ea35b)) feat: git status full-stack handler and introduce api.repos - Russell Matney
- ([`b3f3787`](https://github.com/russmatney/clawe/commit/b3f3787)) feat: rough repos table - Russell Matney
- ([`5354048`](https://github.com/russmatney/clawe/commit/5354048)) chore: boilerplate for git-status dashboard and topbar widget - Russell Matney

### 23 Jul 2023

- ([`43f2733`](https://github.com/russmatney/clawe/commit/43f2733)) i3: no focus for topbar - Russell Matney
- ([`b3bae12`](https://github.com/russmatney/clawe/commit/b3bae12)) chore: move topbar to top - Russell Matney

  > Keeping the i3 bar above it for now b/c i don't have battery/wifi/volume
  > in clawe yet.

- ([`61649e9`](https://github.com/russmatney/clawe/commit/61649e9)) feat: disable variety (clawe sets bgs) - Russell Matney

  > fix bar gap when one client (no smart_gaps).
  > 
  > restart the doctor-topbar (clove app) via systemctl on restart. (same as
  > in awesomewm).


### 22 Jul 2023

- ([`0085c99`](https://github.com/russmatney/clawe/commit/0085c99)) server: add muuntaja to step up ring edges - Russell Matney

  > Now handling :body <edn> conversions from/to json on the way in/out of
  > the api. Not too shabby!

- ([`eacb97a`](https://github.com/russmatney/clawe/commit/eacb97a)) feat: handle clove topbar in new i3 setup - Russell Matney

  > Quick helper and experimenting with i3 gap/bar stuff. won't work on
  > the laptop yet b/c some heights are hard-coded. i wonder if relative px
  > vals are supported so i don't have to hardcode the resolution
  > dimensions. maybe ppt/percentages make more sense.

- ([`42a428a`](https://github.com/russmatney/clawe/commit/42a428a)) feat: handle error in bb check-i3-config - Russell Matney

  > Calling this mostly via fabb now - wonder if i could get that to run on
  > i3/config save, via a watcher.

- ([`15ba1fc`](https://github.com/russmatney/clawe/commit/15ba1fc)) fix: add awesome/lain to the gitignore - Russell Matney

  > I've been ignoring this for months, might as well .gitignore it as well.

- ([`06d7cd3`](https://github.com/russmatney/clawe/commit/06d7cd3)) feat: basic clawe cleanup and smarter scratchpad toggles - Russell Matney

  > Most of the way there for cleaning up scratchpads, plus it's freaking
  > barebones and fast.

- ([`70014f3`](https://github.com/russmatney/clawe/commit/70014f3)) feat: name some i3 workspaces - Russell Matney
- ([`ac2419f`](https://github.com/russmatney/clawe/commit/ac2419f)) feat: i3 config trim and clean up - Russell Matney

### 21 Jul 2023

- ([`91c28db`](https://github.com/russmatney/clawe/commit/91c28db)) misc: i3 namespaces getting some work - Russell Matney
- ([`26f2c88`](https://github.com/russmatney/clawe/commit/26f2c88)) wip: import i3 config - Russell Matney

  > Prepping to symlink and edit this directly from clawe.
  > 
  > Kind of funny to move this into clawe after finally getting the
  > clawe.edn OUT of here (and back into dotfiles). But, i think it makes
  > sense if the awesome and yabai configs are in here - really should be
  > working out a way to get those expressed in the clawe.edn and exported
  > into whatever the contents of these dirs are, and maybe gitignoring
  > them. Alternatively, clawe could just make it easier to write those, and
  > the dirs could provide a base config for other folks to extend on - e.g.
  > norms for fast-impls of clawe toggle or mx.


### 20 Jul 2023

- ([`e505e42`](https://github.com/russmatney/clawe/commit/e505e42)) chore: drop clerk - Russell Matney

  > I'm not using this so much anymore, so, bombs away.

- ([`12d2cce`](https://github.com/russmatney/clawe/commit/12d2cce)) feat: cycle apps in current space - Russell Matney

### 19 Jul 2023

- ([`fb92155`](https://github.com/russmatney/clawe/commit/fb92155)) wip: begin impling clawe.wm for i3 - Russell Matney
- ([`06ef2a2`](https://github.com/russmatney/clawe/commit/06ef2a2)) fix: crash when starting up with no database - Russell Matney

### 14 Jul 2023

- ([`b2d7d09`](https://github.com/russmatney/clawe/commit/b2d7d09)) wip: handling more link context - Russell Matney
- ([`aee793f`](https://github.com/russmatney/clawe/commit/aee793f)) feat: subheader anchor links - Russell Matney

  > Not looking great, but more useful.

- ([`e9afce3`](https://github.com/russmatney/clawe/commit/e9afce3)) feat: proper todo sorting/filtering, adds ^top link to tags index - Russell Matney
- ([`113f47f`](https://github.com/russmatney/clawe/commit/113f47f)) feat: date_index page for building notes by any date index - Russell Matney

  > Adds (ugly) links to the home page for notes by last-modified,
  > created-at, published-at.

- ([`3ac3093`](https://github.com/russmatney/clawe/commit/3ac3093)) feat: last-modified index using similar TOC - Russell Matney

  > Adds pill/hrefs to the last-modified page, and some anchor tags for
  > navigating it.

- ([`e8e7fac`](https://github.com/russmatney/clawe/commit/e8e7fac)) feat: href-pill-list pulls out a generic grouped-toc - Russell Matney

  > I'm hopeful this can be immediately reused for date index pages

- ([`fa49bad`](https://github.com/russmatney/clawe/commit/fa49bad)) feat: tag index table of contents (tags grouped by first letter) - Russell Matney

  > These also show their note-count too now - could maybe get to a
  > word-cloud at some point.

- ([`4a2eb41`](https://github.com/russmatney/clawe/commit/4a2eb41)) wip: browser dev-console open by default - Russell Matney
- ([`1fc5b24`](https://github.com/russmatney/clawe/commit/1fc5b24)) chore: update outdated deps - Russell Matney

### 13 Jul 2023

- ([`3080689`](https://github.com/russmatney/clawe/commit/3080689)) wip: big old tag pool as a quick table of contents - Russell Matney
- ([`4c0d675`](https://github.com/russmatney/clawe/commit/4c0d675)) feat: blog table colors based on daily/note counts - Russell Matney

  > We don't care about tags-on-items in notes, only dailies, so we vary the
  > colors and numbers based on what kind of note it is.

- ([`ea00dae`](https://github.com/russmatney/clawe/commit/ea00dae)) feat: add word-count with div-factor for colorization - Russell Matney
- ([`f1d051c`](https://github.com/russmatney/clawe/commit/f1d051c)) feat: improve blog note table: item/todo/link/backlink counts and colors - Russell Matney
- ([`477081b`](https://github.com/russmatney/clawe/commit/477081b)) feat: add item, todos vs tagged counts to blog tables - Russell Matney
- ([`5d24d61`](https://github.com/russmatney/clawe/commit/5d24d61)) feat: clawe-mx open common urls in ff, dev-ff - Russell Matney
- ([`0dcd1b3`](https://github.com/russmatney/clawe/commit/0dcd1b3)) feat: blog lemmy link, separating todos from notes - Russell Matney
- ([`68ff2fd`](https://github.com/russmatney/clawe/commit/68ff2fd)) fix: don't ensure wallpaper on osx - Russell Matney

  > Needs some attention to handle 'uptime's osx differences, plus the
  > wallpaper handling is different anyway (wp per space instead of one for
  > everything.)


### 12 Jul 2023

- ([`380dcbf`](https://github.com/russmatney/clawe/commit/380dcbf)) feat: ensure wallpaper when server starts with < 5 mins of uptime - Russell Matney
- ([`371737c`](https://github.com/russmatney/clawe/commit/371737c)) feat: log uptime, new workspace creation - Russell Matney

  > Uptime to get me to fix the wallpaper on-startup sooner than later.
  > 
  > New workspace notif b/c it's nice to know it worked when git clones
  > fail. (git clones should also detect already-cloned situations as well,
  > and log nicely instead of crash w/ error).


### 11 Jul 2023

- ([`04b6d1f`](https://github.com/russmatney/clawe/commit/04b6d1f)) feat: read clawe.edn from ~/.config/clawe/clawe.edn - Russell Matney

  > Removes clawe.edn, but leaves a clawe-template.edn as a guide/starter.

- ([`21a35c7`](https://github.com/russmatney/clawe/commit/21a35c7)) fix: proper status check - Russell Matney

  > fixes todo action sorting logic

- ([`3abc931`](https://github.com/russmatney/clawe/commit/3abc931)) feat: add fetch latest - Russell Matney

  > Pull fails when there are local changes, which there usually are.


### 10 Jul 2023

- ([`e7aac24`](https://github.com/russmatney/clawe/commit/e7aac24)) feat: always show add-tag button, prioritize 'done' from 'not_started' - Russell Matney
- ([`ddaad4a`](https://github.com/russmatney/clawe/commit/ddaad4a)) feat: add tags from recent selections - Russell Matney

  > Kind of a quirky api, and not really tied to the component being
  > selected upon, but makes it possible to add new tags by selecting words
  > from item names

- ([`fd6577f`](https://github.com/russmatney/clawe/commit/fd6577f)) feat: set priority directly instead of inc/dec - Russell Matney
- ([`46c8b0c`](https://github.com/russmatney/clawe/commit/46c8b0c)) feat: pill for inbox processing: unprioritized-by-tag - Russell Matney
- ([`c5641f8`](https://github.com/russmatney/clawe/commit/c5641f8)) feat: drag wsp left/right in clawe-mx - Russell Matney

  > So much low-hanging fruit here

- ([`cd8d7b8`](https://github.com/russmatney/clawe/commit/cd8d7b8)) feat: pull latest for wsp as first suggestion in clawe-mx - Russell Matney
- ([`5155ca6`](https://github.com/russmatney/clawe/commit/5155ca6)) feat: add open-wsp as direct clawe-mx action - Russell Matney

  > to skip the wsp-actions option step with the most common use-case.

- ([`5211eea`](https://github.com/russmatney/clawe/commit/5211eea)) fix: move wallpaper actions next to image, don't use actions-cell - Russell Matney

  > Lifting these actions out of their 'popup' style. plz just let me click
  > a button right next to the image! maybe just add an on-click to the
  > image itself? I don't know what these other actions even are.

- ([`300854a`](https://github.com/russmatney/clawe/commit/300854a)) feat: add defcom for reload-last-wallpaper - Russell Matney

  > OSX default wallpaper won't consistently persist, so here's a workaround
  > to make it easy to reload the last-used wallpaper via clawe-mx, which is
  > called when creating a new workspace.

- ([`e647e1d`](https://github.com/russmatney/clawe/commit/e647e1d)) chore: commenting out erroring/unused yabai rules - Russell Matney

### 1 Jul 2023

- ([`f591358`](https://github.com/russmatney/clawe/commit/f591358)) tweak: use play icon rather than stopwatch - Russell Matney

### 22 Jun 2023

- ([`b233779`](https://github.com/russmatney/clawe/commit/b233779)) chore: misc workspace updates - Russell Matney

### 20 Jun 2023

- ([`3eb06fc`](https://github.com/russmatney/clawe/commit/3eb06fc)) fix: ensure dirs for db file - Russell Matney

### 18 Jun 2023

- ([`5d404e2`](https://github.com/russmatney/clawe/commit/5d404e2)) feat: move osx bindings to backend clawe-toggle - Russell Matney

### 17 Jun 2023

- ([`f291bc8`](https://github.com/russmatney/clawe/commit/f291bc8)) feat: clawe-toggle via server, with fallback for journal - Russell Matney

  > Moves clawe-toggle on awm to use the api and server, which is quicker
  > already, but could impl some workspace/client caching to be even faster.
  > 
  > Note that a journal-bb, terminal-bb fallback is left in place so we can
  > still toggle an emacs/terminal instance before the server starts
  > up (which will make it possible to debugging if the server is
  > down/crashing).


### 16 Jun 2023

- ([`79e1a29`](https://github.com/russmatney/clawe/commit/79e1a29)) feat: drop dead rerender notebooks button - Russell Matney
- ([`62fd23d`](https://github.com/russmatney/clawe/commit/62fd23d)) wip: towards server-supported clawe-toggle - Russell Matney
- ([`be1bd03`](https://github.com/russmatney/clawe/commit/be1bd03)) fix: update clawe.mx, .mx-suggestions osx bindings - Russell Matney

  > Really gotta get this incorporated into clawe.bindings

- ([`300b5b5`](https://github.com/russmatney/clawe/commit/300b5b5)) fix: remove cyclic dep (clawe.mx <> clawe.workspace.open) - Russell Matney

  > Kind of annoying to deal with, maybe need to restructure things a bit.
  > 
  > For now this maintains the same behavior.


### 15 Jun 2023

- ([`116a408`](https://github.com/russmatney/clawe/commit/116a408)) feat: mod+x/w for server-mx, mod+shift+x/w for bb-mx - Russell Matney

  > Some fixes and clean up to the mx commands, which are now running well.
  > 
  > Adds a clawe-mx call to open-new-workspace, which warms up the cache
  > when the workspace is first opened. This might also be a good time to
  > clear the cache, and maybe 'restart-workspace' ends up becoming a
  > cache-clearing tactic.

- ([`bc3b16d`](https://github.com/russmatney/clawe/commit/bc3b16d)) feat: fast clawe-mx - Russell Matney

  > Finally gets getting some nice performance out of clawe-mx.
  > 
  > This breaks rofi's MRU sorting b/c the rofi input string build is now
  > memoized. The performance win is worth it for now.
  > 
  > Moves 'dynamic' parts of clawe-mx to a mx-suggestions command on a
  > different keybinding. These can't be memoized (b/c their inputs change)
  > and break the performance gain, so a separate keybinding resolves some
  > of that.
  > 
  > The suggestions could have a server-supported version as well at some
  > point, which should be faster out of the box.
  > 
  > Cache-busting and cache-warming follow ups can come later on.


### 13 Jun 2023

- ([`b2181f2`](https://github.com/russmatney/clawe/commit/b2181f2)) chore: restore zprint - Russell Matney
- ([`67c46e6`](https://github.com/russmatney/clawe/commit/67c46e6)) fix: walk back clawe-mx-fast hacks - Russell Matney

  > Just needs something working today.


### 12 Jun 2023

- ([`f795cee`](https://github.com/russmatney/clawe/commit/f795cee)) wip: nearly worked rofi cache to pass around an input file - Russell Matney
- ([`7de826a`](https://github.com/russmatney/clawe/commit/7de826a)) wip: disable deduping mru results - Russell Matney

  > duplicate results aren't really a problem, and rofi might just filter
  > them anyway - here we remove some logic in the hopes of speeding this up
  > a bit.
  > 
  > Most likely the cache we write should be the direct input to
  > rofi, rather than re-building and escaping all the labels.

- ([`720f436`](https://github.com/russmatney/clawe/commit/720f436)) perf: cache mx-commands, read cache first in mx-fast - Russell Matney

  > Getting kind of crazy, but closer to a very performant mx command,
  > via caching. Here we write the mx-command labels to a cache, and read
  > them instead of building them up when invoking clawe-mx. This lets us
  > have a slim-dependency mx_fast namespace that will load very quickly,
  > and once a command is selected, we create a new process for selecting
  > the command and then firing the 'on-select' for it.
  > 
  > This doesn't yet handle workspace-specific or context-specific
  > commands (like those based on current browser tabs). I have a hope we
  > can handle those asynchronously, at least on the rofi side.
  > 
  > This is still a bit slow - still takes a full second for rofi to render
  > 175 items. digging there next.

- ([`97ef195`](https://github.com/russmatney/clawe/commit/97ef195)) perf: logging more ns load times - Russell Matney

  > zprint seems to be ~250 ms to require! Gotta work-around that one.

- ([`484ea83`](https://github.com/russmatney/clawe/commit/484ea83)) chore: move some defs to defns, ns-load perf fixes - Russell Matney
- ([`5e5364b`](https://github.com/russmatney/clawe/commit/5e5364b)) chore: clean up timer ns usage - Russell Matney

### 9 Jun 2023

- ([`dc8d8bc`](https://github.com/russmatney/clawe/commit/dc8d8bc)) wip: some perf experiments - Russell Matney
- ([`4d34ea7`](https://github.com/russmatney/clawe/commit/4d34ea7)) wip: logs showing time for each step of clawe.mx call - Russell Matney

  > clawebb_mx
  > 
  > | nil   | 346   | clawe.mx      Namespace (and deps) Loaded
  > | 3     | 349   | clawe.mx/mx   start
  > | 51    | 400   | clawe.mx/mx   fetched current workspace (or it's lazy?)
  > | 239   | 639   | clawe.mx/mx   commands
  > | 0     | 639   | rofi/rofi
  > | 573   | 1212  | rofi labels filtered and sorted
  > | 2571  | 3783  | clawe.mx      end
  > 
  > clawebb_mx_fast
  > 
  > | nil   | 351   | clawe.mx      Namespace (and deps) Loaded
  > | 2     | 353   | clawe.mx/mx-fast      start
  > | 229   | 582   | clawe.mx/mx-fast      commands fast
  > | 0     | 582   | rofi/rofi
  > | 1     | 583   | rofi labels filtered and sorted
  > | 747   | 1330  | clawe.mx-fast end

- ([`db91fce5`](https://github.com/russmatney/clawe/commit/db91fce5)) wip: debugging clawe-mx speed - Russell Matney
- ([`8c25ddb`](https://github.com/russmatney/clawe/commit/8c25ddb)) fix: ralphie.notify misc cleanup, doc string, naming - Russell Matney
- ([`25d2f2a`](https://github.com/russmatney/clawe/commit/25d2f2a)) feat: :replaces-process is now :notify/id, notify/is-mac? now config/osx? - Russell Matney

  > Updates old naming per ralphie.notify refactor.

- ([`691706f`](https://github.com/russmatney/clawe/commit/691706f)) feat: notify replacing notifs via process-cache - Russell Matney

  > Removes notify-send.py dependency in favor of calling notify-send
  > directly, with support for replacing notifications with arbitry
  > :notify/id strings.
  > 
  > Maintains the --replaces-process feature of notify-send.py, which can
  > now be deleted.
  > 
  > Also pulls some osx predicates and cache helpers out of rofi's mru-cache
  > feat and into ralphie.config.

- ([`ef4fef8`](https://github.com/russmatney/clawe/commit/ef4fef8)) wip: refactoring notifs to drop notify-send.py usage - Russell Matney

  > Notify-send.py has a bug (fixed but no new release:
  > https://github.com/phuhl/notify-send.py/issues/21), but really I don't
  > want to thrust a python dependency on clawe consumers anyway. Here we
  > start a refactor of ralphie.notify into a cleaner impl - the key feature
  > to maintain is replacing previous notifs using a :notify/id to reduce
  > noise.

- ([`a7c2384`](https://github.com/russmatney/clawe/commit/a7c2384)) fix: add missing systemic dep to server - Russell Matney
- ([`ddd4534`](https://github.com/russmatney/clawe/commit/ddd4534)) fix: remove mru-cache items not in labels - Russell Matney

  > Prevents cached items that aren't in the passed labels from being shown
  > in the list. Does not yet clean up the cache, but maybe that's fine.

- ([`916c0cb`](https://github.com/russmatney/clawe/commit/916c0cb)) feat: clawe-mx MRU sorting - Russell Matney

  > Pretty clean! Should have done this ages ago!

- ([`7d47246`](https://github.com/russmatney/clawe/commit/7d47246)) feat: rofi supports mru cache via :cache-id opt - Russell Matney

  > All rofi usage now supports an MRU cache - just pass a :cache-id string
  > from the consumer. Note this id gets used to write the cache file, so
  > don't go too crazy.

- ([`d98065c`](https://github.com/russmatney/clawe/commit/d98065c)) feat: basic mru cache impl - Russell Matney
- ([`f45aeb5`](https://github.com/russmatney/clawe/commit/f45aeb5)) fix: include all defkbds in clawe-mx-fast - Russell Matney
- ([`abc0d9c`](https://github.com/russmatney/clawe/commit/abc0d9c)) fix: stack org/body-strings - Russell Matney

  > Not quite great yet - links are getting their own line with these
  > settings, so this might not last. may need to split out chunks by empty
  > lines or something.

- ([`d0c71f5`](https://github.com/russmatney/clawe/commit/d0c71f5)) fix: safer ralphie/notify - Russell Matney

  > try/catches the process exec, logs a helpful message, continues
  > execution. No longer crashes when the attempted notification program is
  > missing, so we don't get broken keybindings for optionals like
  > notifications.

- ([`d5aca06`](https://github.com/russmatney/clawe/commit/d5aca06)) refactor: clean up doctor/server - Russell Matney

  > Pull plasma-undertow and doctor-page impls into helper funcs.

- ([`1dacc80`](https://github.com/russmatney/clawe/commit/1dacc80)) refactor: remove clerk stuff from server - Russell Matney
- ([`857c6c5`](https://github.com/russmatney/clawe/commit/857c6c5)) feat: logs, notifs on backend socket conn/disconn - Russell Matney

  > Also removes a noisy toggle-floating notif


### 8 Jun 2023

- ([`fb0c57b`](https://github.com/russmatney/clawe/commit/fb0c57b)) fix: use basic name-string for topbar todo name - Russell Matney

  > Rather than going for the fancy text-with-links to nowhere


### 7 Jun 2023

- ([`d502d16`](https://github.com/russmatney/clawe/commit/d502d16)) fix: drop :ws-channel usage, pass :channel into connect/disconnect - Russell Matney

  > This has apparently been wonky for ages.

- ([`de493e0`](https://github.com/russmatney/clawe/commit/de493e0)) chore: logging on socket reconnect - Russell Matney
- ([`5987b0c`](https://github.com/russmatney/clawe/commit/5987b0c)) feat: quick bb task for stopping the backend - Russell Matney
- ([`8fb0cbf`](https://github.com/russmatney/clawe/commit/8fb0cbf)) chore: add plasma workspace - Russell Matney
- ([`280a83d`](https://github.com/russmatney/clawe/commit/280a83d)) fix: don't call init in index.html - Russell Matney

  > This is already handled by shadow-cljs via [:modules :main :init-fn]


### 1 Jun 2023

- ([`fe2e093`](https://github.com/russmatney/clawe/commit/fe2e093)) feat: todos filter: last-30-days - Russell Matney

### 22 May 2023

- ([`ba83a21`](https://github.com/russmatney/clawe/commit/ba83a21)) chore: ensure explicit aseprite workspace - Russell Matney

  > This was already being created, but attaching a git repo (workspace
  > directory) affects the sort order, which is what I really wanted here.


### 21 May 2023

- ([`9c7650a`](https://github.com/russmatney/clawe/commit/9c7650a)) fix: use brotab on osx - Russell Matney
- ([`ff36824`](https://github.com/russmatney/clawe/commit/ff36824)) fix: add alt chrome name to devweb workspace - Russell Matney

### 11 May 2023

- ([`29e21df`](https://github.com/russmatney/clawe/commit/29e21df)) refactor: replace todo status button icons - Russell Matney

  > These look worse but provide a better UI - using the desired next status
  > as the button icon. Could do a bit of work to make these render better,
  > and come up with icon version for skip/cancel. One step at a time.
  > 
  > Unfortunately couldn't depend on components/todos here, b/c it already
  > depends on handlers... circular deps, always an interesting problem.

- ([`f7b12f0`](https://github.com/russmatney/clawe/commit/f7b12f0)) refactor: drop 'current' usage everywhere - Russell Matney

  > Moves away from using a 'current' tag for :status/in-progress. It's
  > redundant, and shouldn't be necessary.
  > 
  > Leaving some things in place for a bit, in case there's a reason to
  > revert - I think at one point I liked supporting both so that 'current'
  > could be used on non-todo items, but at this point todos and notes have
  > a strong line between them (no longer mixed).


### 10 May 2023

- ([`18b9fb3`](https://github.com/russmatney/clawe/commit/18b9fb3)) feat: re-use status, priority, tags on topbar focus-widget - Russell Matney

  > Love that this was simple! The components are working.

- ([`0ec60d8`](https://github.com/russmatney/clawe/commit/0ec60d8)) feat: give focus topbar widget a strong background - Russell Matney

  > Some much needed readability.

- ([`55b418f`](https://github.com/russmatney/clawe/commit/55b418f)) feat: list pomodoros in pomdodoro frontend widget - Russell Matney

  > Next we can get a look at other points on the timeline, like git
  > commits, to help automatically correct/close pomodoros (when they're left
  > on overnight, for ex).


### 8 May 2023

- ([`fe1a0b3`](https://github.com/russmatney/clawe/commit/fe1a0b3)) fix: correct :org.prop key, include reset-last-modified - Russell Matney

  > Updates garden notes to ensure a created-at in most.


### 6 May 2023

- ([`8e6eba3`](https://github.com/russmatney/clawe/commit/8e6eba3)) fix: move topbar actions into right-most widget - Russell Matney

  > Better use of space, especially on a laptop.


### 4 May 2023

- ([`22aee8e`](https://github.com/russmatney/clawe/commit/22aee8e)) chore: add two godot workspaces - Russell Matney
- ([`572cfc2`](https://github.com/russmatney/clawe/commit/572cfc2)) feat: lines to paragraphs clipboard converter - Russell Matney

  > Copy-pasting from org, where I use a fixed-width character limit, to
  > something like github/slack/discord is really annoying, so here's a
  > quick clipboard-based defcom to re-join the paragraphs into single lines
  > again.


### 2 May 2023

- ([`05f1dd8`](https://github.com/russmatney/clawe/commit/05f1dd8)) feat: clear current todos impl - Russell Matney
- ([`f89c7e8`](https://github.com/russmatney/clawe/commit/f89c7e8)) feat: workspace-aware godot launch command - Russell Matney

  > This was nice.


### 1 May 2023

- ([`2cd6277`](https://github.com/russmatney/clawe/commit/2cd6277)) chore: update readme danger.russmatney.com links - Russell Matney
- ([`b6d4f91`](https://github.com/russmatney/clawe/commit/b6d4f91)) feat: add notifications to pomodoro start/end - Russell Matney
- ([`780883b`](https://github.com/russmatney/clawe/commit/780883b)) chore: removing more garden/workspaces references - Russell Matney

### 29 Apr 2023

- ([`36278a1`](https://github.com/russmatney/clawe/commit/36278a1)) misc: retracting attributes - Russell Matney
- ([`3a689b8`](https://github.com/russmatney/clawe/commit/3a689b8)) misc: some db exploration comment helpers - Russell Matney
- ([`cb90d21`](https://github.com/russmatney/clawe/commit/cb90d21)) readme: link to twitch overview, while it's relevant - Russell Matney

### 28 Apr 2023

- ([`70dfbcd`](https://github.com/russmatney/clawe/commit/70dfbcd)) fix: share more space in topbar, hide/show pomodoro actions - Russell Matney
- ([`57a5ce3`](https://github.com/russmatney/clawe/commit/57a5ce3)) feat: much larger workspace name - Russell Matney

  > fix: shorterner only shortens when it's going to make something shorter

- ([`c65fc0c`](https://github.com/russmatney/clawe/commit/c65fc0c)) fix: cleaner tick bar layout - Russell Matney

  > Shortens the human-time-since format, adds more padding.

- ([`4b0c2c0`](https://github.com/russmatney/clawe/commit/4b0c2c0)) chore: remove unused deps - Russell Matney
- ([`9dfe497`](https://github.com/russmatney/clawe/commit/9dfe497)) misc: comments/todos - Russell Matney
- ([`674a357`](https://github.com/russmatney/clawe/commit/674a357)) fix: restore blog note actions - Russell Matney

  > this :doctor/type is still required!

- ([`3ee8f06`](https://github.com/russmatney/clawe/commit/3ee8f06)) feat: workspace-focus action - Russell Matney
- ([`54169c4`](https://github.com/russmatney/clawe/commit/54169c4)) feat: pomodoro in widget bar, topbar, and as page - Russell Matney
- ([`0e87975`](https://github.com/russmatney/clawe/commit/0e87975)) feat: pomodoro widget consuming db data - Russell Matney
- ([`1ee4b0a`](https://github.com/russmatney/clawe/commit/1ee4b0a)) feat: pomodoro handlers and queries - Russell Matney
- ([`7004f3a`](https://github.com/russmatney/clawe/commit/7004f3a)) feat: backend pomodoro crud - Russell Matney
- ([`7feece0`](https://github.com/russmatney/clawe/commit/7feece0)) fix: dates.tick durations nil-punning + fallback parsing - Russell Matney
- ([`8b5d34f`](https://github.com/russmatney/clawe/commit/8b5d34f)) fix: first-commit-dt, reset-created-at-via-git fixes - Russell Matney

  > These should move into ralphie.git and get unit tested.

- ([`baccfe8`](https://github.com/russmatney/clawe/commit/baccfe8)) lint: remove unused import - Russell Matney
- ([`2972326`](https://github.com/russmatney/clawe/commit/2972326)) fix: nil-pun `dir` in ralphie.git - Russell Matney

  > Should really get some more unit tests on this!

- ([`7ae6a88`](https://github.com/russmatney/clawe/commit/7ae6a88)) docs: add link to clawe overview in readme - Russell Matney
- ([`329778e`](https://github.com/russmatney/clawe/commit/329778e)) fix: don't disable :queue-todo when missing :org/id - Russell Matney

### 26 Apr 2023

- ([`c1833bb`](https://github.com/russmatney/clawe/commit/c1833bb)) wip: towards optimistic updates - Russell Matney

  > This is really a backend db optimistic update. It seems like the
  > handlers could cut off the defhandlers to perform a frontend update
  > first, to get things moving faster.

- ([`9fe8afb`](https://github.com/russmatney/clawe/commit/9fe8afb)) fix: dedupe tag suggestions - Russell Matney
- ([`cf183f0`](https://github.com/russmatney/clawe/commit/cf183f0)) feat: add suggested tags based on name/parent-name - Russell Matney
- ([`f3486de`](https://github.com/russmatney/clawe/commit/f3486de)) feat: infer presets in filter-grouper - Russell Matney

  > The filter-grouper now infers some presets for tags, showing the 20 most
  > common in the whole collection as toggleable filters.

- ([`f036886`](https://github.com/russmatney/clawe/commit/f036886)) feat: add inferred todo actions to focus cards, topbar - Russell Matney
- ([`b12c584`](https://github.com/russmatney/clawe/commit/b12c584)) feat: adding (and dissoc-ing) inferred actions on todos - Russell Matney

  > Adds an initial inferred action for the views/todos widget: quick add
  > for tags on other items in the group.
  > 
  > Introduces an :action/popup-comp that pops up a div when you click the
  > action.
  > 
  > Unfortunately :actions/inferred cannot be passed into defhandlers ->
  > they crash because transit chokes on the :action/on-click function, or
  > some other nonsense that it can't deal with.

- ([`f8ae108`](https://github.com/russmatney/clawe/commit/f8ae108)) fix: grab less initial data for more speed - Russell Matney
- ([`f06ae25`](https://github.com/russmatney/clawe/commit/f06ae25)) fix: log less in frontend console - Russell Matney
- ([`6e56073`](https://github.com/russmatney/clawe/commit/6e56073)) feat: concat inferred actions in (->actions item) - Russell Matney
- ([`e72bd49`](https://github.com/russmatney/clawe/commit/e72bd49)) feat: transform items in a group context - Russell Matney
- ([`d4a93eb`](https://github.com/russmatney/clawe/commit/d4a93eb)) fix: hover colors should not match non-hover - Russell Matney

  > Also fixes 'border' vs 'text'

- ([`c1ee7c5`](https://github.com/russmatney/clawe/commit/c1ee7c5)) comment: helper to remove archive/ todos - Russell Matney
- ([`4538ef0`](https://github.com/russmatney/clawe/commit/4538ef0)) fix: ralphie.git/commits fix - Russell Matney

  > Moves repo-todo-paths from garden.core to ralphie.git.
  > 
  > Removes crappy '-for-dir' naming. Ugh!

- ([`d5fc940`](https://github.com/russmatney/clawe/commit/d5fc940)) feat: quick todos helper - Russell Matney
- ([`59e9a28`](https://github.com/russmatney/clawe/commit/59e9a28)) fix: safer debug/raw-metadata usage - Russell Matney

  > Filtering some data before passing in, plus some tweaks to prevent
  > seqing uuids, etc.

- ([`af0ffea`](https://github.com/russmatney/clawe/commit/af0ffea)) fix: allow dates to pass through parser - Russell Matney

  > Maybe dates.tick gets a single conditional for this?

- ([`f559ddb`](https://github.com/russmatney/clawe/commit/f559ddb)) fix: nil-punning in commit last-modified reset - Russell Matney
- ([`0f6a2be`](https://github.com/russmatney/clawe/commit/0f6a2be)) refactor: misc initial-fe-db improvements/clarity - Russell Matney
- ([`1141561`](https://github.com/russmatney/clawe/commit/1141561)) fix: support insts passed to parse-time-string - Russell Matney
- ([`aefefd1`](https://github.com/russmatney/clawe/commit/aefefd1)) feat: garden.db clean up helpers - Russell Matney
- ([`d995c06`](https://github.com/russmatney/clawe/commit/d995c06)) feat: ralphie.git/commits supporting :oldest-first - Russell Matney

  > Untested!

- ([`5e6e47a`](https://github.com/russmatney/clawe/commit/5e6e47a)) feat: garden created-at, last-modified via git helpers - Russell Matney
- ([`c5b2c95`](https://github.com/russmatney/clawe/commit/c5b2c95)) wip: towards dropping use-blog, pulling notes from fe db - Russell Matney
- ([`10aa176`](https://github.com/russmatney/clawe/commit/10aa176)) feat: root-notes fe db query - Russell Matney
- ([`e67609f`](https://github.com/russmatney/clawe/commit/e67609f)) fix: nil-punned last-modified comp - Russell Matney

### 25 Apr 2023

- ([`3aad51c`](https://github.com/russmatney/clawe/commit/3aad51c)) feat: ralphie.git/commit supporting dir and path - Russell Matney
- ([`7909ca9`](https://github.com/russmatney/clawe/commit/7909ca9)) feat: refresh blog.db button - Russell Matney
- ([`a554b86`](https://github.com/russmatney/clawe/commit/a554b86)) feat: hide-current toggle on todos widget - Russell Matney
- ([`c825017`](https://github.com/russmatney/clawe/commit/c825017)) feat: toggle groups open/close, better nowrap vs table-width handling - Russell Matney
- ([`02e2192`](https://github.com/russmatney/clawe/commit/02e2192)) feat: impl todos widget table-def, show raw on :level - Russell Matney
- ([`7e7230b`](https://github.com/russmatney/clawe/commit/7e7230b)) fix: check for children after filtering, not before - Russell Matney
- ([`4a18f69`](https://github.com/russmatney/clawe/commit/4a18f69)) fix: space between debug/metadata items - Russell Matney

  > Also adds metadata popups to the filter grouper comps for opts and items
  > themselves.

- ([`bb3011b`](https://github.com/russmatney/clawe/commit/bb3011b)) fix: topbar current task action list layout - Russell Matney
- ([`9f40f8c`](https://github.com/russmatney/clawe/commit/9f40f8c)) chore: fix linter error - Russell Matney
- ([`c625125`](https://github.com/russmatney/clawe/commit/c625125)) refactor: no need to pass these handlers all over - Russell Matney
- ([`e584398`](https://github.com/russmatney/clawe/commit/e584398)) fix: delete priority from db - Russell Matney

  > Removing values from the db is not well handled yet - datascript needs
  > explicit retracts. Here we remove it explicitly from the db along with updating the org file.


### 24 Apr 2023

- ([`f79b35d`](https://github.com/russmatney/clawe/commit/f79b35d)) fix: include chess games in initial load - Russell Matney
- ([`f51ab59`](https://github.com/russmatney/clawe/commit/f51ab59)) chore: drop unused namespace - Russell Matney
- ([`c218438`](https://github.com/russmatney/clawe/commit/c218438)) feat: include resources page in publish - Russell Matney

  > Also, refresh the blog.db before publishing to be sure we have the
  > latest.

- ([`2e94d04`](https://github.com/russmatney/clawe/commit/2e94d04)) feat: v1 blog resources page rendering - Russell Matney
- ([`be86393`](https://github.com/russmatney/clawe/commit/be86393)) refactor: filter-grouper preset persistence to localstorage - Russell Matney

  > Now supporting preset-selection caching per-filter-grouper (rather than
  > via the query, which only allows one per page). Falls back on an id to
  > get the preset by, which is the old (shared) behavior - pass an :id into
  > the filter-grouper to get unique caching on that id.
  > 
  > These ids might come in handy, really - wonder if i can pull + edit them
  > with it, via some other filter-drawer popover - perhaps the filters +
  > presets should be managed in the frontend datascript db...

- ([`c8c4b88`](https://github.com/russmatney/clawe/commit/c8c4b88)) tweak: limit topbar current-focus width - Russell Matney
- ([`a21112c`](https://github.com/russmatney/clawe/commit/a21112c)) feat: send all prioritized todos to frontend - Russell Matney

  > More for the frontend's initial page load

- ([`ae4b3ee`](https://github.com/russmatney/clawe/commit/ae4b3ee)) fix: send 14 days of dailies + current todos to fe - Russell Matney

  > The growing set of frontend data and helpers to build it up. These are
  > starting to look like their own ingestor functions, and would probably
  > benefit from some pagination impls soon (i.e. give me the next page of
  > dailies/todos/whatever).

- ([`97e5ac4`](https://github.com/russmatney/clawe/commit/97e5ac4)) refactor: cosmetic sort impl dry up - Russell Matney

### 23 Apr 2023

- ([`918dc0d`](https://github.com/russmatney/clawe/commit/918dc0d)) fix: nil-punning, fallback parsing in time sorters - Russell Matney

  > Also guards string/trim with a `string?`

- ([`fe3b421`](https://github.com/russmatney/clawe/commit/fe3b421)) feat: print a few entities when they hit the fe - Russell Matney

  > Also, frontend log formatting.

- ([`a84f30f`](https://github.com/russmatney/clawe/commit/a84f30f)) wip: logging more datoms as they come in - Russell Matney
- ([`2c1ec46`](https://github.com/russmatney/clawe/commit/2c1ec46)) wip: rough org-body relayout - Russell Matney
- ([`af6953e`](https://github.com/russmatney/clawe/commit/af6953e)) feat: widget-bar caching open/close via localstorage - Russell Matney

  > Feels like a nice pattern here - could probably wrap and combine the uix
  > state to make this cleaner. I think we'll want a similar impl for the
  > filter-grouper's preset-query usage.

- ([`4b49c15`](https://github.com/russmatney/clawe/commit/4b49c15)) feat: displaying clips on the frontend - Russell Matney

  > Still needs a manual ingestion, but they're playing!
  > 
  > Could probably dry up screenshots vs clips a bit - they're very similar,
  > don't need specific naming one way vs the other, and might clear the way
  > for adding more source dirs/media types (audio? game assets?).

- ([`9b481d1`](https://github.com/russmatney/clawe/commit/9b481d1)) feat: ingesting clips from ~/gifs dir - Russell Matney
- ([`3b61368`](https://github.com/russmatney/clawe/commit/3b61368)) fix: remove huge timestamp and hostname from logs - Russell Matney

  > Probably some correct way to do this, but at least i can read the logs
  > in a narrow buffer now.

- ([`f52cac8`](https://github.com/russmatney/clawe/commit/f52cac8)) lint: unused opts var - Russell Matney
- ([`8d9323d`](https://github.com/russmatney/clawe/commit/8d9323d)) fix: correct some retract errors - Russell Matney

  > Some gross txs happening in here!

- ([`665c66f`](https://github.com/russmatney/clawe/commit/665c66f)) feat: initial commit widget - Russell Matney
- ([`9519c57`](https://github.com/russmatney/clawe/commit/9519c57)) feat: misc log clean up, commit ingestion fixes - Russell Matney
- ([`0800484`](https://github.com/russmatney/clawe/commit/0800484)) fix: prefer log over println - Russell Matney
- ([`59e450c`](https://github.com/russmatney/clawe/commit/59e450c)) fix: push repos to fe, fix screenshot name - Russell Matney
- ([`719ac06`](https://github.com/russmatney/clawe/commit/719ac06)) wip: initial today page/widget - Russell Matney
- ([`723f122`](https://github.com/russmatney/clawe/commit/723f122)) fix: popup, sticky z-index fixes - Russell Matney
- ([`b05e770`](https://github.com/russmatney/clawe/commit/b05e770)) feat: displayed todo/queued-at, sort by it - Russell Matney

  > Now sorting todos by todo/queued-at, which is a db-only (not persisted
  > to org!) property.

- ([`3f28ef2`](https://github.com/russmatney/clawe/commit/3f28ef2)) fix: use :org/parent-name, which preserves order - Russell Matney

  > the move to the db means :org/parent-names come back in whatever order,
  > so we use the string version for now.

- ([`5a1e7da`](https://github.com/russmatney/clawe/commit/5a1e7da)) fix: filter-fn passing bugs - Russell Matney

  > This filter-grouper has all the feats, but needs a rewrite

- ([`153567d`](https://github.com/russmatney/clawe/commit/153567d)) chore: reduce debug log noise - Russell Matney
- ([`f5af489`](https://github.com/russmatney/clawe/commit/f5af489)) feat: include parent relations for all org items - Russell Matney

  > Not just those with uuids/parent-ids. This is done via other-db-updates,
  > which can be extended to include other note datoms that don't work as a
  > map in the note. (like datoms belonging to other entities, e.g.
  > children).
  > 
  > Also updates the filter-grouper to pass the active-filter-fn and passed
  > filter-items fn into the item->comp as :filter-by and :filter-fn. Not in
  > love with the naming, but i need these funcs passed to apply to children
  > when rendering todo/card-or-card-group.

- ([`2f7b112`](https://github.com/russmatney/clawe/commit/2f7b112)) feat: optionally skip-subtasks - Russell Matney

  > Deduplicates sub-tasks in the todos widget.
  > 
  > NOTE sub-tasks are only relevant relative to a parent with an
  > :org/id (uuid). Without a uuid, children don't get a parent-id
  > reference, and 'parents' never gets set in the db. We could fix this
  > with a reduce while ingesting.

- ([`a4dee0c`](https://github.com/russmatney/clawe/commit/a4dee0c)) fix: don't retract! empty tx lists - Russell Matney
- ([`73fc60b`](https://github.com/russmatney/clawe/commit/73fc60b)) feat: remove deleted tags when purging org files - Russell Matney

  > Extends garden.db's sync-and-purge to remove deleted tags. Similar
  > functionality needs to be applied to all lists.
  > 
  > An alternative is diffing the parsed org file with the current db to
  > figure out what to add/update/remove, then transacting that difference.
  > I was kind of hoping datascript could do that for me, but perhaps not.

- ([`9a4017c`](https://github.com/russmatney/clawe/commit/9a4017c)) fix: include 'current' tagged todos - Russell Matney

  > Not perfect yet - tags removed from edited org files are not yet deleted
  > from the db.

- ([`24bb9eb`](https://github.com/russmatney/clawe/commit/24bb9eb)) fix: current-stack joining children, fix filtering - Russell Matney

  > The focus widget was filtering on 'current' tags - that filter has been
  > fixed to include 'in-progress' status, but also removed - the
  > db.ui/current-todos should just get the right data.
  > 
  > Also breaks out the join-children helper from yesterday. Probably not
  > performant, but we can come back to that.


### 22 Apr 2023

- ([`203beae`](https://github.com/russmatney/clawe/commit/203beae)) feat: restore nested :org/items on todos page - Russell Matney
- ([`cd48d23`](https://github.com/russmatney/clawe/commit/cd48d23)) feat: working backend todos-with-children - Russell Matney

  > Not perfect, but working.

- ([`3d5b916`](https://github.com/russmatney/clawe/commit/3d5b916)) wip: garden.db/todos-with-children - Russell Matney

  > Not quite done, but most of the way there.

- ([`09f6576`](https://github.com/russmatney/clawe/commit/09f6576)) feat: db.core/datoms, datascript-playground wsp - Russell Matney
- ([`e7bf23b`](https://github.com/russmatney/clawe/commit/e7bf23b)) chore: clean up dashboard layout - Russell Matney

  > Close all the auto-opens, fix the widget widths.

- ([`f7dc4fe`](https://github.com/russmatney/clawe/commit/f7dc4fe)) feat: add screenshots widget, support width per widget-bar - Russell Matney
- ([`92ff474`](https://github.com/russmatney/clawe/commit/92ff474)) feat: basic chess games page/widget with list - Russell Matney
- ([`bb5c6e4`](https://github.com/russmatney/clawe/commit/bb5c6e4)) feat: add event-timestamp preset and group/sort/filter - Russell Matney
- ([`191a927`](https://github.com/russmatney/clawe/commit/191a927)) feat: some events widget clean up - Russell Matney
- ([`0f96149`](https://github.com/russmatney/clawe/commit/0f96149)) fix: cleaner todos widget divs - Russell Matney
- ([`8ffdbf3`](https://github.com/russmatney/clawe/commit/8ffdbf3)) wip: started a pomodoro re-lay-out - Russell Matney
- ([`19f5f71`](https://github.com/russmatney/clawe/commit/19f5f71)) feat: nicer new-datoms dev log - Russell Matney
- ([`39ca8dc`](https://github.com/russmatney/clawe/commit/39ca8dc)) feat: current-focus empty state - Russell Matney

### 21 Apr 2023

- ([`ceb8844`](https://github.com/russmatney/clawe/commit/ceb8844)) feat: toggle not-started/in-progress - Russell Matney

  > Rather than just hide completed, this also hides skipped/cancelled.

- ([`25418cc`](https://github.com/russmatney/clawe/commit/25418cc)) fix: clear org/priority when removing - Russell Matney
- ([`f562cb0`](https://github.com/russmatney/clawe/commit/f562cb0)) wip: popover with todo content - Russell Matney
- ([`cc3b81e`](https://github.com/russmatney/clawe/commit/cc3b81e)) wip: towards blog feats using the db - Russell Matney
- ([`8eaa3a0`](https://github.com/russmatney/clawe/commit/8eaa3a0)) feat: garden ingestion, safer date comparators - Russell Matney

  > Starting to move the blog work into the db

- ([`15ff6be`](https://github.com/russmatney/clawe/commit/15ff6be)) style: improved filter-grouper layout/presentation - Russell Matney
- ([`513ab05`](https://github.com/russmatney/clawe/commit/513ab05)) style: better colors - Russell Matney
- ([`57f2a30`](https://github.com/russmatney/clawe/commit/57f2a30)) fix: file/last-modified already parsed - Russell Matney
- ([`d86a6d6`](https://github.com/russmatney/clawe/commit/d86a6d6)) refactor: drop use-todos, add reingest-todos handler - Russell Matney
- ([`24335c9`](https://github.com/russmatney/clawe/commit/24335c9)) wip: todos pulling from db, not files - Russell Matney

  > fine as long as the relevant data is in the db! feels like an
  > ingestion/data-pull/push feature is next.

- ([`d3ea10f`](https://github.com/russmatney/clawe/commit/d3ea10f)) chore: misc rearranging - Russell Matney

  > debugging some startup slowness, only to find it doesn't exist if the
  > console is closed.

- ([`f15bcb3`](https://github.com/russmatney/clawe/commit/f15bcb3)) refactor: use-todos/use-current-todos handler/stream - Russell Matney

  > Plus misc other clean up. Not happy with this yet, should probably use
  > handlers to fire transactions and just read from the updated frontend
  > database instead.

- ([`a7ac0b3`](https://github.com/russmatney/clawe/commit/a7ac0b3)) feat: support mark-not-started on todos - Russell Matney
- ([`bc64aab`](https://github.com/russmatney/clawe/commit/bc64aab)) refactor: pull current-task bar into views/focus - Russell Matney
- ([`53a21e9`](https://github.com/russmatney/clawe/commit/53a21e9)) refactor: drop api.focus, use-focus - Russell Matney

  > Now using api.todos, use-todos, tho we might move away from that as
  > well.

- ([`7001451`](https://github.com/russmatney/clawe/commit/7001451)) wip: api.focus cleanup, add repo-watcher todo - Russell Matney
- ([`7aeadb4`](https://github.com/russmatney/clawe/commit/7aeadb4)) feat: support proper todos page - Russell Matney
- ([`b3d3db6`](https://github.com/russmatney/clawe/commit/b3d3db6)) feat: include wallpapers and events in initial data - Russell Matney
- ([`f7267a4`](https://github.com/russmatney/clawe/commit/f7267a4)) wip: auto-push recent data instead of all data - Russell Matney
- ([`7bc27f0`](https://github.com/russmatney/clawe/commit/7bc27f0)) deps: update org-crud for :file/last-modified - Russell Matney
- ([`f3ea487`](https://github.com/russmatney/clawe/commit/f3ea487)) fix: more space for current-task in topbar - Russell Matney
- ([`bc76f47`](https://github.com/russmatney/clawe/commit/bc76f47)) fix: more safe sorting - Russell Matney

  > Should have done this ages ago.

- ([`b7659e3`](https://github.com/russmatney/clawe/commit/b7659e3)) wip: restore events/todos on dashboard - Russell Matney

  > Gotta pass in the opts to support the connection (for now).
  > 
  > Also restores the lazy db data pushing, which is a pain while it runs,
  > but gets all the data into the frontend.

- ([`bc71805`](https://github.com/russmatney/clawe/commit/bc71805)) wip: towards restoring ye old todos - Russell Matney
- ([`0ac867e`](https://github.com/russmatney/clawe/commit/0ac867e)) fix: use safe datetime sort - Russell Matney

  > wraps ticks' comparators to prevent crashes when a datetime dares to be
  > nil.

- ([`0059e4c`](https://github.com/russmatney/clawe/commit/0059e4c)) fix: sort using tick - Russell Matney

  > Hopefully doesn't break a bunch of things.

- ([`55f8634`](https://github.com/russmatney/clawe/commit/55f8634)) refactor: current-task in topbar, some :todo keys addressed - Russell Matney
- ([`3669634`](https://github.com/russmatney/clawe/commit/3669634)) refactor: :type/garden -> :type/note, api.todos clean up - Russell Matney

  > Moves all :type/garden usage to :type/note, and updates the garden.db
  > ingestor to create :type/todo items when an org item has a status.
  > 
  > Cleans up api.todos somewhat, prepping to port/DRY up api.focus logic.

- ([`c387ce0`](https://github.com/russmatney/clawe/commit/c387ce0)) feat: parse dates in garden helpers - Russell Matney

  > Wraps org-crud's ->nested-item, ->flattened-items helpers and applies
  > date parsing, to clean up that logic elsewhere.

- ([`0cd03de`](https://github.com/russmatney/clawe/commit/0cd03de)) refactor: split focus and todos feats - Russell Matney
- ([`080456e`](https://github.com/russmatney/clawe/commit/080456e)) refactor: pull todo/card-or-card-group from focus - Russell Matney

  > Now ready to split focus/todos feats

- ([`ef36052`](https://github.com/russmatney/clawe/commit/ef36052)) refactor: move use-focus handlers to handlers - Russell Matney
- ([`90a15df`](https://github.com/russmatney/clawe/commit/90a15df)) refactor: pull todo/card component from focus - Russell Matney
- ([`30cdade`](https://github.com/russmatney/clawe/commit/30cdade)) refactor: pull id-hash, db-id out of views/focus - Russell Matney
- ([`e98a677`](https://github.com/russmatney/clawe/commit/e98a677)) feat: move status, priority, level to comps/todo - Russell Matney

  > Dismantling views/focus to share the todo components.

- ([`e2cc41f`](https://github.com/russmatney/clawe/commit/e2cc41f)) fix: restore pomodoro crud with time-literals - Russell Matney

  > edn/read-string requires time-literals to be passed as readers manually.

- ([`cae5398`](https://github.com/russmatney/clawe/commit/cae5398)) refactor: pull pomodoro bar from focus to new widget - Russell Matney

  > And adds it to the dashboard. no functionality restored yet, but it's a
  > start.


### 20 Apr 2023

- ([`d3b02ca`](https://github.com/russmatney/clawe/commit/d3b02ca)) feat: set rest of dashboard icons, cleaner labels - Russell Matney
- ([`2642831`](https://github.com/russmatney/clawe/commit/2642831)) feat: add icons to widget-bar - Russell Matney
- ([`1697703`](https://github.com/russmatney/clawe/commit/1697703)) feat: more dashboard actions, improved widget-bar layout - Russell Matney
- ([`42714aa`](https://github.com/russmatney/clawe/commit/42714aa)) feat: add actions to widget-bar, clean-up wsps action - Russell Matney
- ([`09d631d`](https://github.com/russmatney/clawe/commit/09d631d)) feat: pull workspaces into widget, add to dashboard - Russell Matney
- ([`508a7ad`](https://github.com/russmatney/clawe/commit/508a7ad)) refactor: pull dashboard todos into widget - Russell Matney
- ([`b0204cd`](https://github.com/russmatney/clawe/commit/b0204cd)) feat: topbar showing workspace actions on hover - Russell Matney
- ([`f4c4618`](https://github.com/russmatney/clawe/commit/f4c4618)) refactor: active-workspace layout - Russell Matney
- ([`9494818`](https://github.com/russmatney/clawe/commit/9494818)) feat: show topbar metadata in workspaces widget - Russell Matney

  > Not really relevant, but topbar/workspaces/clients have so much
  > overlap...

- ([`2ea57c3`](https://github.com/russmatney/clawe/commit/2ea57c3)) fix: support ->actions for workspaces/clients - Russell Matney
- ([`a2f99bd`](https://github.com/russmatney/clawe/commit/a2f99bd)) fix: display 'false' in debug-metadata - Russell Matney
- ([`e6c6d21`](https://github.com/russmatney/clawe/commit/e6c6d21)) feat: workspace and client action handlers - Russell Matney
- ([`46eeb8c`](https://github.com/russmatney/clawe/commit/46eeb8c)) feat: workspace action list and close handler - Russell Matney
- ([`393ebbc`](https://github.com/russmatney/clawe/commit/393ebbc)) wip: toward workspaces page face lift - Russell Matney
- ([`f3230c9`](https://github.com/russmatney/clawe/commit/f3230c9)) refactor: restore workspaces widget/page - Russell Matney

  > This was impled in terms of awesomewm - this refactor updates the ui to
  > use :workspace/ and :client/ namespaced fields.

- ([`aa1f86d`](https://github.com/russmatney/clawe/commit/aa1f86d)) fix: todo cards: tags line, better breadcrumb wrapping - Russell Matney
- ([`d42353b`](https://github.com/russmatney/clawe/commit/d42353b)) chore: drop unused dep - Russell Matney

### 19 Apr 2023

- ([`41808f0`](https://github.com/russmatney/clawe/commit/41808f0)) wip: dashboard events cleanup - Russell Matney

  > cleans up the table styles and moves the dashboard events into the
  > widget-bar style.

- ([`72d07e9`](https://github.com/russmatney/clawe/commit/72d07e9)) feat: add focus, blog widgets to dashboard - Russell Matney

  > Starting to treat these widgets more like widgets. Should probably break
  > 'focus' up - maybe 'focus' shows all things 'current', and the rest
  > moves to 'todos' for todo processing. Need to break out some blog
  > use-cases as well.

- ([`84c291a`](https://github.com/russmatney/clawe/commit/84c291a)) chore: drop expo - Russell Matney

  > Everything via doctor is fine, no need for another runtime

- ([`88b19f4`](https://github.com/russmatney/clawe/commit/88b19f4)) fix: error boundary readability, fix event query filter - Russell Matney
- ([`b6d36ea`](https://github.com/russmatney/clawe/commit/b6d36ea)) fix: saw this crash, but apparently it went away? - Russell Matney

  > not sure what that's about.

- ([`02b2bb5`](https://github.com/russmatney/clawe/commit/02b2bb5)) fix: side-bar menu now fixed, so scrolling doesn't affect it - Russell Matney
- ([`cfec11c`](https://github.com/russmatney/clawe/commit/cfec11c)) feat: restore parent-names on focus todo cards - Russell Matney

  > Also cleans up the grouped children layout.

- ([`674480e`](https://github.com/russmatney/clawe/commit/674480e)) fix: bump org-crud - Russell Matney
- ([`bf17e25`](https://github.com/russmatney/clawe/commit/bf17e25)) feat: org watcher purges edited/removed items - Russell Matney

  > Supports basic sync-and-purge for a file. When an org item is edited,
  > the watcher syncs the latest to the database, and retracts db items
  > that belonged to the edited file but are no longer found in the latest
  > parsed version of that file.
  > 
  > We may need to push these retractions to the frontend if we aren't
  > already.

- ([`5fc1d28`](https://github.com/russmatney/clawe/commit/5fc1d28)) feat: display db id in focus todos card - Russell Matney
- ([`9e3d013`](https://github.com/russmatney/clawe/commit/9e3d013)) feat: focus widget merging matching db items - Russell Matney

  > The focus widget todos now merge in items found in the db. This also
  > updates the garden.db/fallback-id func to use name-string and fallback
  > to source-path when short-path isn't there. this means we're creating
  > many-many more org items in the db again, which introduce some issues.
  > 
  > I hope to update the watcher to clear edited/removed org items next -
  > right now, just editing an items name can lead to a duplicated item,
  > which shows up in db queries.

- ([`284e31a`](https://github.com/russmatney/clawe/commit/284e31a)) feat: ensure-uuid on focus todo card - Russell Matney

  > Plus misc other cleanup/fixups.

- ([`71af6a3`](https://github.com/russmatney/clawe/commit/71af6a3)) fix: move menu-expand-hover to the whole menu - Russell Matney
- ([`6d52801`](https://github.com/russmatney/clawe/commit/6d52801)) feat: menu icons, expand on hover, improved layout - Russell Matney
- ([`6303065`](https://github.com/russmatney/clawe/commit/6303065)) feat: persist selected preset via route params - Russell Matney

  > This may need more work if we want to namespace the preset keys.

- ([`6a062cd`](https://github.com/russmatney/clawe/commit/6a062cd)) feat: pass params into pages - Russell Matney

  > Also refactors the page component to something less 3-arity-y.

- ([`9d99aa6`](https://github.com/russmatney/clawe/commit/9d99aa6)) refactor: comp-only reverted, use hide-header instead - Russell Matney

  > filters :comp-only routes from menu.


### 18 Apr 2023

- ([`26ecd0c`](https://github.com/russmatney/clawe/commit/26ecd0c)) refactor: include side-menu on comp-only pages - Russell Matney

  > Ought to rename this :comp-only flag :/

- ([`a89a362`](https://github.com/russmatney/clawe/commit/a89a362)) feat: refactor menu into sliding/expanding one - Russell Matney

  > Menu now slides in from the side, and is hopefully ready to be re-used
  > in widgets.

- ([`c634642`](https://github.com/russmatney/clawe/commit/c634642)) feat: move menu in-line rather than use popover - Russell Matney
- ([`a60bbf0`](https://github.com/russmatney/clawe/commit/a60bbf0)) fix: don't overwrite 'class', merge it - Russell Matney
- ([`cf23cee`](https://github.com/russmatney/clawe/commit/cf23cee)) fix: fallback ->value function - Russell Matney

  > can't call nil as a function.


### 5 Apr 2023

- ([`923dbf5`](https://github.com/russmatney/clawe/commit/923dbf5)) fix: filter out 'published' when building tags-list - Russell Matney
- ([`fa3807b`](https://github.com/russmatney/clawe/commit/fa3807b)) blog: mobile friendly layout fixes - Russell Matney

  > At least gets things to readable, navigable.

- ([`ae95c40`](https://github.com/russmatney/clawe/commit/ae95c40)) blog: Danger [:br] Russ for mobile-friendly header - Russell Matney
- ([`9e2996c`](https://github.com/russmatney/clawe/commit/9e2996c)) blog preset: unpublished notes with a 'published' tag - Russell Matney

  > A useful preset for a workflow where you set 'published' while
  > mind-gardening in emacs, then come here to manually kick those notes
  > over the line.
  > 
  > Maybe a single button to publish all of these (while scanning over them)
  > is even better.

- ([`7e389b5`](https://github.com/russmatney/clawe/commit/7e389b5)) feat: add 'published' tag when publishing notes - Russell Matney
- ([`9146677`](https://github.com/russmatney/clawe/commit/9146677)) chore: update org-crud dep, add published tag to all notes - Russell Matney

  > Using org-crud's latest feature to update published notes with a
  > 'published' tag without updating the last-modified date.
  > 
  > This tag will be ignored by the blog, but is very helpful when
  > mind-gardening, so we know if the note we're in is 'live' or not.
  > Especially useful for walking and publishing old dailies.


### 4 Apr 2023

- ([`8b92642`](https://github.com/russmatney/clawe/commit/8b92642)) feat: images show :image/date-string, if known - Russell Matney
- ([`875a31a`](https://github.com/russmatney/clawe/commit/875a31a)) lint: remove unused require - Russell Matney
- ([`9e0497d`](https://github.com/russmatney/clawe/commit/9e0497d)) fix: add :comment to extra-partition-by/split-bys - Russell Matney

  > Not the best solution. lists/images preceded by multiple lines of text
  > won't render correctly yet... this partition by needs to get a little
  > smarter.
  > 
  > Unfortunately it's very easy to fix on a case-by-case basis, by just
  > reworking the source to not have this issue.

- ([`306a825`](https://github.com/russmatney/clawe/commit/306a825)) fix: ensure filter actually runs with seq - Russell Matney

  > Fixes dupe images being rendered in notes with multiple images.

- ([`dcb88bf`](https://github.com/russmatney/clawe/commit/dcb88bf)) blog: better render when linked note can't be found - Russell Matney

  > This shouldn't really happen, but this way it'll look a bit better when
  > it does.

- ([`18226ba`](https://github.com/russmatney/clawe/commit/18226ba)) deps: update org-crud version - Russell Matney
- ([`893dea9`](https://github.com/russmatney/clawe/commit/893dea9)) fix: :line-type vs :type in org/body maps - Russell Matney

  > 'clean up' from yesterday accidentally removed src blocks - there's
  > apparently a :line-type and a :type param in the :org/body maps.

- ([`4a2972d`](https://github.com/russmatney/clawe/commit/4a2972d)) fix: display images if they exist in public/images - Russell Matney

  > Also displays image name and caption, if they exist.
  > 
  > A few more fixes required here - don't copy over daily images for items
  > that aren't published, multiple images in an item rendering the first
  > image multiple times, and a handful of org-crud parse fixes.

- ([`8c9cfc9`](https://github.com/russmatney/clawe/commit/8c9cfc9)) blog: now supporting images and video - Russell Matney

  > Copies images into blog-content/public/images, and builds :img and
  > :video components based on the :image/extension parsed from org blog.
  > Huzzah!


### 3 Apr 2023

- ([`ef4ddee`](https://github.com/russmatney/clawe/commit/ef4ddee)) wip: setup for displaying images in posts - Russell Matney
- ([`250163a`](https://github.com/russmatney/clawe/commit/250163a)) feat: image count per note in note-comps - Russell Matney
- ([`4a88a9c`](https://github.com/russmatney/clawe/commit/4a88a9c)) feat: update org-crud version - Russell Matney

### 1 Apr 2023

- ([`de36fcc`](https://github.com/russmatney/clawe/commit/de36fcc)) feat: include styles in rebuild-indexes - Russell Matney
- ([`597cc89`](https://github.com/russmatney/clawe/commit/597cc89)) wip: rebuild open blog pages notes - Russell Matney
- ([`e7b228a`](https://github.com/russmatney/clawe/commit/e7b228a)) feat: boilerplate for more blog api/rofi/cli cmds - Russell Matney
- ([`b832196`](https://github.com/russmatney/clawe/commit/b832196)) feat: /blog/rebuild endpoint and rofi entry - Russell Matney

  > can now rebuild the blog via clawe-mx (S-x anywhere) and
  > `clawebb -x clawe.doctor/rebuild-blog` on the command line.
  > 
  > Note this expects clawe-server to be running - the actual build isn't
  > bb-compatible yet - some tick stuff to still dig into.

- ([`717edb2`](https://github.com/russmatney/clawe/commit/717edb2)) script: merge org files in websites dir into single file - Russell Matney

### 31 Mar 2023

- ([`6e75575`](https://github.com/russmatney/clawe/commit/6e75575)) feat: drop 'Home' title, use font-nes on home headers - Russell Matney
- ([`7a3bcc7`](https://github.com/russmatney/clawe/commit/7a3bcc7)) feat: presentable home page - Russell Matney

  > Pulls content from blog_home.org, then impls a few custom content
  > filters for dino games and clojure projects.
  > 
  > Also adds mastodon/github links to the header.

- ([`c79d2a1`](https://github.com/russmatney/clawe/commit/c79d2a1)) blog: favicon and logo - Russell Matney
- ([`669e523`](https://github.com/russmatney/clawe/commit/669e523)) feat: remove annoying space following links - Russell Matney

  > When the next text is punctuation, remove space added around links.

- ([`8305914`](https://github.com/russmatney/clawe/commit/8305914)) feat: blog.db/find-note(s) helper - Russell Matney
- ([`d096fcc`](https://github.com/russmatney/clawe/commit/d096fcc)) blog: different link color for published/unpublished note, external link - Russell Matney

  > Also wraps the header/logo as a link to home.

- ([`3cca6f2`](https://github.com/russmatney/clawe/commit/3cca6f2)) feat: add link-count filter and dead-end presets - Russell Matney

  > Very useful for showing notes with no links, which are a dead-end from
  > the rabbit hole flow in the blog.

- ([`867e152`](https://github.com/russmatney/clawe/commit/867e152)) blog: remove 'dark' references/usage, add no-prose to colorized tags - Russell Matney
- ([`0412e35`](https://github.com/russmatney/clawe/commit/0412e35)) fix: kick pomodoro actions into action form - Russell Matney

  > Tho the non-action prefixed may also be supported.

- ([`5e44028`](https://github.com/russmatney/clawe/commit/5e44028)) feat: re-use priority-label comp, add actions-list, border to parent todos - Russell Matney

  > Fixes an issue where parent todos were annoyingly non-interactable.
  > adds an actions list and a border to make it more obvious what todos
  > hare parents/children. The children still duplicate in the list - could
  > clean that up as well.

- ([`9c5a62d`](https://github.com/russmatney/clawe/commit/9c5a62d)) fix: drop viewer-notebook naming - Russell Matney
- ([`18b0cbc`](https://github.com/russmatney/clawe/commit/18b0cbc)) blog: add nes font - Russell Matney
- ([`8517be2`](https://github.com/russmatney/clawe/commit/8517be2)) blog: tags list using color wheel - Russell Matney

### 30 Mar 2023

- ([`54bc310`](https://github.com/russmatney/clawe/commit/54bc310)) lint: remove unused require - Russell Matney
- ([`3371b26`](https://github.com/russmatney/clawe/commit/3371b26)) fix: safer nil-punning in render-paragraph - Russell Matney
- ([`963a667`](https://github.com/russmatney/clawe/commit/963a667)) feat: better paragraph line-breaks - Russell Matney

  > A rule i've liked for a while - join paragraph newlines when there is a
  > line ending with punctuation. If not: keep the lines in place, for
  > things like random thoughts and poetry that shouldn't be joined into a
  > single paragraph line.

- ([`9fe2122`](https://github.com/russmatney/clawe/commit/9fe2122)) feat: push nested lists with margin - Russell Matney

  > These aren't properly recursive/nested, but this implementation is much
  > simpler. we can come back to something more copy/paste friendly later
  > on.
  > 
  > Fixes a bunch of poor list rendering cases, including lists being
  > ignored when they aren't preceded by an empty line, and nested bullets
  > getting flattened.

- ([`ca7bcaa`](https://github.com/russmatney/clawe/commit/ca7bcaa)) wip: working through better nested list rendering - Russell Matney

  > Making a bit of a mess, but we'll get there.

- ([`65310c8`](https://github.com/russmatney/clawe/commit/65310c8)) fix: tweak to org-note action list display - Russell Matney
- ([`6d43aea`](https://github.com/russmatney/clawe/commit/6d43aea)) feat: more blog presets, more note stats - Russell Matney

  > Adds internal link, daily item counts. Dupes more blog/item logic in
  > components/note.cljc.

- ([`d5d576b`](https://github.com/russmatney/clawe/commit/d5d576b)) feat: :blog/published-at set in api.blog/publish - Russell Matney

  > Now tracking the date a particular post was added to the
  > org-blog-content/blog.edn's :notes list. Ideally it will get rebuilt and
  > pushed live around the same time, but i'm ok calling it 'published' at
  > this point.

- ([`9a9c1c2`](https://github.com/russmatney/clawe/commit/9a9c1c2)) hack: one-off script for assigning existing published-at - Russell Matney

  > Some code for parsing the output files of `git_all_versions_of` and
  > assigning :blog/published-at dates on all existing posts, based on the
  > first time they were committed to blog.edn's :notes map.

- ([`ba4372a`](https://github.com/russmatney/clawe/commit/ba4372a)) fix: disable this debug-mode guard for now - Russell Matney
- ([`755a476`](https://github.com/russmatney/clawe/commit/755a476)) feat: flex grow note comp, add (index/total) - Russell Matney
- ([`2b63dad`](https://github.com/russmatney/clawe/commit/2b63dad)) blog: components.note/metadata consumed in views/blog - Russell Matney

  > Adds a components/note.cljc, with the hope that views/blog and
  > blog/pages/* can both consume from it. Still need to figure out how to
  > share them against a database - maybe the answer is just datascript? Or,
  > pass everything in?

- ([`e5e444d`](https://github.com/russmatney/clawe/commit/e5e444d)) feat: improve note-comp, extend color-wheel helper - Russell Matney

  > larger org-names in font-nes with colorful backgrounds and borders.
  > Colorize all the things!


### 29 Mar 2023

- ([`905bd90`](https://github.com/russmatney/clawe/commit/905bd90)) fix: floating popups no longer scroll the page - Russell Matney

  > Fixed by limiting the size of the popup and adding an :overflow 'scroll'

- ([`1e2730c`](https://github.com/russmatney/clawe/commit/1e2730c)) feat: blog.config/!debug-mode atom and toggle - Russell Matney

  > Also removes a bunch of dead comments.

- ([`ae5357a`](https://github.com/russmatney/clawe/commit/ae5357a)) fix: start blog/config when starting blog/db - Russell Matney

  > I can't remember the syntax for the manual systemic deps trick

- ([`ae97077`](https://github.com/russmatney/clawe/commit/ae97077)) refactor: render/write-page conveniences - Russell Matney
- ([`360df92`](https://github.com/russmatney/clawe/commit/360df92)) blog: cleaner headline impl - Russell Matney

  > Using >> to indicate the level of nesting, and TODO vs [ ].

- ([`be7fb62`](https://github.com/russmatney/clawe/commit/be7fb62)) blog: show tags per headline - Russell Matney

  > This helps the daily notes quite a bit.

- ([`1e1f222`](https://github.com/russmatney/clawe/commit/1e1f222)) feat: blog metadata per note - Russell Matney

  > Still need to set blog/published-date, but it'll work once it's in
  > there.


### 28 Mar 2023

- ([`e8fe5dd`](https://github.com/russmatney/clawe/commit/e8fe5dd)) feat: republish whole blog via dashboard button - Russell Matney
- ([`26973b8`](https://github.com/russmatney/clawe/commit/26973b8)) fix: notes by id instead of a list, for incremental db updates - Russell Matney

  > Much faster update time from the frontend now!

- ([`53b929d`](https://github.com/russmatney/clawe/commit/53b929d)) feat: live-re-rendering notes on org file save - Russell Matney

  > Well, hot-dog! This rerenders a changed org-file to org-blog-content's
  > /public dir, where the livejs bit re-renders things automagically.
  > 
  > The indexes don't rebuild automatically yet, but maybe they could?
  > 
  > This could also use some debouncing, but nothing is slow yet, so, seems
  > fine.

- ([`d5484ac`](https://github.com/russmatney/clawe/commit/d5484ac)) feat: restore backlinks - Russell Matney

  > How did this ever work? Probably explains why they were lost at one
  > point.

- ([`7289ec7`](https://github.com/russmatney/clawe/commit/7289ec7)) feat: pull in rest of org-blog, publishing blog again - Russell Matney

  > I don't have numbers on how long the publish-all command used to take...
  > but now it takes about 3.5 seconds, which I'm quite pleased with. My
  > memory is feels-like 15s, but :shrug:. It's fine now. Pretty good for
  > 700 files and 3 index pages!
  > 
  > This pulls in everything from org-blog except it's 'export' namespace,
  > which was mostly clerk-ui helpers for toggling notes as published and
  > displaying linked notes. That work was already replaced by the
  > doctor.ui.views.blog and api.blog stuff.
  > 
  > I have some decent ideas for reducing the slow re-render after
  > publishing a new note - most likely with a partial db update instead of
  > a full rebuild. Then we'll want to push only incremental updates over
  > the wire, rather than the whole db. Then, we should prevent full page
  > rerenders.

- ([`ac46c92`](https://github.com/russmatney/clawe/commit/ac46c92)) feat: show note total count, filter out modified more than 3 weeks ago - Russell Matney
- ([`0caefa3`](https://github.com/russmatney/clawe/commit/0caefa3)) feat: more blog presets, tags-list in blog table - Russell Matney
- ([`9dae6a7`](https://github.com/russmatney/clawe/commit/9dae6a7)) feat: support toggling published on notes - Russell Matney

  > Adds/removes the note from org-blog-content/blog.edn, refreshes the
  > notes, and pushes the update back down to the UI.

- ([`db14e4d`](https://github.com/russmatney/clawe/commit/db14e4d)) feat: pill/filter facelift, hide-all-tables/groups - Russell Matney

  > Not quite the right table/group toggle interaction yet, but useful
  > enough for now.

- ([`223796c`](https://github.com/russmatney/clawe/commit/223796c)) feat: sort published first/last, toggle dailies - Russell Matney

  > Cleans up the blog presets, and adds a few extra-pill impls for toggling
  > published-first/last and hiding/showing dailies completely.

- ([`8783528`](https://github.com/russmatney/clawe/commit/8783528)) feat: passing table-def into filter group comp - Russell Matney
- ([`483661a`](https://github.com/russmatney/clawe/commit/483661a)) feat: blog filters modified today/yesterday/last-7-days - Russell Matney
- ([`abdb448`](https://github.com/russmatney/clawe/commit/abdb448)) feat: pagination controls in filter-group->comp - Russell Matney

  > A quick actions-list for showing more/less of the notes in a group.
  > 
  > Not really pagination, really changing a page-size to walk up to all the
  > items in the list.


### 27 Mar 2023

- ([`db4099c`](https://github.com/russmatney/clawe/commit/db4099c)) feat: impl a few last-modified filter options - Russell Matney
- ([`5808365`](https://github.com/russmatney/clawe/commit/5808365)) feat: impl some published, posts blog presets - Russell Matney
- ([`789890f`](https://github.com/russmatney/clawe/commit/789890f)) fix: use correct key, nil-pun group-by-key - Russell Matney

  > Correctly showing which pill is active, and not crashing when there is
  > no group-by-key set.

- ([`a451a6e`](https://github.com/russmatney/clawe/commit/a451a6e)) feat: mix blog/config data into :blog/db all-notes - Russell Matney

  > Now including a :blog/published flag on notes

- ([`d00d1f0`](https://github.com/russmatney/clawe/commit/d00d1f0)) feat: better filter-grouper naming, support passed filter/sorter - Russell Matney

  > Also use tick to group on and sort by last-modified-date (instead of
  > down to the second, it's per date).

- ([`f30d599`](https://github.com/russmatney/clawe/commit/f30d599)) feat: dry up group-by-labelling, fixup sorting in focus presets - Russell Matney
- ([`f9f04e0`](https://github.com/russmatney/clawe/commit/f9f04e0)) fix: don't filter when group-by returns nil/empty-coll - Russell Matney

  > Updates the expand-coll-group-bys to make sure group-bys returning nil
  > or empty collections don't get lost. This lets us see all the untagged
  > items, for example.

- ([`ba05f75`](https://github.com/russmatney/clawe/commit/ba05f75)) feat: filter-grouper supports sort-groups via tags impl - Russell Matney

  > Also supports passing in filter-items/sort-items for per-page filtering
  > of items in groups. Maybe these filters should apply/be passed into
  > use-filter ?

- ([`a53991c`](https://github.com/russmatney/clawe/commit/a53991c)) refactor: views/focus consume DRYed up group->comp impl - Russell Matney

### 26 Mar 2023

- ([`14ca74d`](https://github.com/russmatney/clawe/commit/14ca74d)) feat: filter-grouper items-by-group component - Russell Matney

  > DRYing up filter-grouper's group-by component, passing in the
  > item->component for rendering items within groups.

- ([`e15652d`](https://github.com/russmatney/clawe/commit/e15652d)) refactor: namespace filter-defs keys, for more clarity - Russell Matney

  > This filter-grouper(-sorter?) api is weird, hopefully some namespacing
  > will help clear it up.


### 24 Mar 2023

- ([`ecc5a3b`](https://github.com/russmatney/clawe/commit/ecc5a3b)) feat: add preset filters to /blog, clean up in /focus - Russell Matney

  > Should probably DRY this up a bit more, but they're not _precisely_ the
  > same...

- ([`cb3d3b8`](https://github.com/russmatney/clawe/commit/cb3d3b8)) wip: /blog page suddenly has data! so here's a commit. - Russell Matney
- ([`5651222`](https://github.com/russmatney/clawe/commit/5651222)) feat: new /blog widget - Russell Matney

  > Not too bad, boilerplate-wise

- ([`1df8c49`](https://github.com/russmatney/clawe/commit/1df8c49)) wip: add blog/{config,db} directly from russmatney/org-blog - Russell Matney

  > Pulling in most of org-blog, or moving it to org-blog-content. Towards a
  > clawe/focus -like interface for the blog.

- ([`d7b828d`](https://github.com/russmatney/clawe/commit/d7b828d)) fix: linting errors - Russell Matney
- ([`3f4cb7a`](https://github.com/russmatney/clawe/commit/3f4cb7a)) feat: include item count per group-by - Russell Matney
- ([`b623bfb`](https://github.com/russmatney/clawe/commit/b623bfb)) refactor: pass extra-preset-pills into filter-grouper - Russell Matney

  > A nicer api, just data. Maybe something to these toggle-pills, feels
  > like the actions api but interaction focused.


### 23 Mar 2023

- ([`5785fdf`](https://github.com/russmatney/clawe/commit/5785fdf)) refactor: introducing components/pill - Russell Matney

  > Pulls pill component out of filter-grouper, re-uses with focus
  > completed/current toggles.

- ([`45ee4d1`](https://github.com/russmatney/clawe/commit/45ee4d1)) chore: sort clawe.edn - Russell Matney
- ([`2a0c276`](https://github.com/russmatney/clawe/commit/2a0c276)) chore: clj-kondo helpers - Russell Matney
- ([`515b0bc`](https://github.com/russmatney/clawe/commit/515b0bc)) feat: yabai discord toggle keybd - Russell Matney
- ([`237f517`](https://github.com/russmatney/clawe/commit/237f517)) fix: remove missing dep import - Russell Matney
- ([`4554347`](https://github.com/russmatney/clawe/commit/4554347)) chore: more workspaces - Russell Matney

  > Really gotta break this out into a ~/.config/clawe.edn

- ([`70df8fb`](https://github.com/russmatney/clawe/commit/70df8fb)) fix: ignore Kap in yabai - Russell Matney
- ([`96a107e`](https://github.com/russmatney/clawe/commit/96a107e)) chore: new workspace - Russell Matney
- ([`a6edab9`](https://github.com/russmatney/clawe/commit/a6edab9)) todo: adds log whining about missing reconnect logic - Russell Matney

  > I'll impl it one of these days...

- ([`1221646`](https://github.com/russmatney/clawe/commit/1221646)) feat: toggle item groups open/close - Russell Matney

  > Had to pull this out into a component to get around a tHe RuLeS oF rEaCT
  > crash.

- ([`02f6b2f`](https://github.com/russmatney/clawe/commit/02f6b2f)) feat: show active filter data - Russell Matney

  > Rather than hide this, expose the presets definition. just some edn in
  > your UI, nbd.

- ([`8c195f4`](https://github.com/russmatney/clawe/commit/8c195f4)) filter-grouper: quick toggle filter details - Russell Matney

  > Looks like a big diff, but this was actually pretty simple.

- ([`76ce787`](https://github.com/russmatney/clawe/commit/76ce787)) chore: update outdated dependencies - Russell Matney

  > Some manual tick updates to keep things working. had to disable the
  > pomodoros for now, not sure why the data_readers aren't available when
  > it reads the current state from localstorage...


### 22 Mar 2023

- ([`996af8f`](https://github.com/russmatney/clawe/commit/996af8f)) chore: new workspace - Russell Matney
- ([`45781fc`](https://github.com/russmatney/clawe/commit/45781fc)) fix: :workspace/directory is optional - Russell Matney

### 21 Mar 2023

- ([`0ec69b1`](https://github.com/russmatney/clawe/commit/0ec69b1)) chore: bunch of workspaces - Russell Matney

### 19 Mar 2023

- ([`c7e22bb`](https://github.com/russmatney/clawe/commit/c7e22bb)) script: user comment block for renaming files - Russell Matney

  > Some code to quickly renaming photos in a dir.

- ([`c87aec0`](https://github.com/russmatney/clawe/commit/c87aec0)) feat: add logger macro to bb.edn usage - Russell Matney

### 17 Mar 2023

- ([`59913b6`](https://github.com/russmatney/clawe/commit/59913b6)) feat: remove id links when converting org->markdown - Russell Matney

### 6 Mar 2023

- ([`d9c9021`](https://github.com/russmatney/clawe/commit/d9c9021)) workspaces: update preferred index sorting - Russell Matney

### 22 Feb 2023

- ([`854905c`](https://github.com/russmatney/clawe/commit/854905c)) feat: two more util libs - Russell Matney
- ([`de73343`](https://github.com/russmatney/clawe/commit/de73343)) fix: restore center-window-large keybinding - Russell Matney
- ([`1f26821`](https://github.com/russmatney/clawe/commit/1f26821)) feat: run godot games as non-master - Russell Matney

  > Sets godot game instances as slaves in awesomewm, so they don't replace
  > the godot editor (which can steal focus, get crunched too small, etc).
  > This also ends up putting the game in a more realistic size.


### 12 Feb 2023

- ([`d8245d4`](https://github.com/russmatney/clawe/commit/d8245d4)) fix: is-mac? on ventura - Russell Matney

### 5 Feb 2023

- ([`65a5ef7`](https://github.com/russmatney/clawe/commit/65a5ef7)) feat: update org-crud version (stop dropping blocks) - Russell Matney

### 3 Feb 2023

- ([`4dfa388`](https://github.com/russmatney/clawe/commit/4dfa388)) idea: an .edn first focus-toggle api - Russell Matney

  > Speced out a data structure for expressing focus-toggle options.
  > 
  > Depends on some smart widget placement, but i think i want that anyway
  > for easier window management.

- ([`fe96edf`](https://github.com/russmatney/clawe/commit/fe96edf)) feat: nicer filter grouper component layout - Russell Matney
- ([`e939f20`](https://github.com/russmatney/clawe/commit/e939f20)) fix: use proper `dd` in tick format string - Russell Matney

### 1 Feb 2023

- ([`0ebfb96`](https://github.com/russmatney/clawe/commit/0ebfb96)) fix: ensure tags as a set to prevent page crash - Russell Matney
- ([`bd51c92`](https://github.com/russmatney/clawe/commit/bd51c92)) feat: support wallpapers in dir root, not just nested - Russell Matney
- ([`307a1d8`](https://github.com/russmatney/clawe/commit/307a1d8)) feat: godot text effects repo - Russell Matney
- ([`e414a9e`](https://github.com/russmatney/clawe/commit/e414a9e)) feat: remove priority on-click - Russell Matney
- ([`e181def`](https://github.com/russmatney/clawe/commit/e181def)) feat: add some repo org files and repo presets - Russell Matney

  > Quick and useful features when it's all data-driven!

- ([`075ce67`](https://github.com/russmatney/clawe/commit/075ce67)) feat: focus pulling todos from last 30 modified org files - Russell Matney
- ([`9015f02`](https://github.com/russmatney/clawe/commit/9015f02)) feat: opt-in to inline filters, counts use filtered-items - Russell Matney

  > The inline filter counts now use filtered-items, not all items, when
  > showing counts. The interaction is getting pretty nice in here.


### 31 Jan 2023

- ([`4122968`](https://github.com/russmatney/clawe/commit/4122968)) feat: expose buried filter-grouper available filters - Russell Matney

  > Shows all matches for each filter type (short-path, tag, etc.). Also
  > shows them inline rather than via popover - this will need to be
  > configurable or dealt with in other contexts, as some views end up with
  > HUGE (way too tall) lists this way.

- ([`90f55b4`](https://github.com/russmatney/clawe/commit/90f55b4)) feat: add prioritized/unprioritized presets - Russell Matney
- ([`0fa7473`](https://github.com/russmatney/clawe/commit/0fa7473)) feat: show presets all the time - Russell Matney

  > Easier to use when they don't require a hover - and now we can see what
  > the values are.

- ([`7e992e3`](https://github.com/russmatney/clawe/commit/7e992e3)) feat: add :match-str-includes-any, fix default preset - Russell Matney

  > Extends the filter-grouper's match api with a new match type,
  > ~:match-str-includes-any~. the match api supports a :match-fn predicate
  > already, which is flexible, but not completely data (b/c functions show
  > up as Object[blah] when you stringify them in cljs). Supporting them as
  > data (edn) as much as possible is a win and keeps things really simpler.
  > 
  > Also fixes the default preset usage, which should really only be a
  > fallback for when local storage didn't find a last-set preset for a
  > given filter-grouper-id.

- ([`f11f1fc`](https://github.com/russmatney/clawe/commit/f11f1fc)) feat: break filter-defs out of pages.todos - Russell Matney

  > Updates consumers to use filter-defs, which makes way more sense than
  > those consumers importing the whole todos page.

- ([`77f857b`](https://github.com/russmatney/clawe/commit/77f857b)) refactor: move to cleaner args in filter-grouper - Russell Matney

  > buying into simpler keywords (`:presets`) and `config` as a var name.

- ([`3b8751c`](https://github.com/russmatney/clawe/commit/3b8751c)) feat: supporting 'today' preset filter - Russell Matney

  > some quick tick date formatting work

- ([`12465ce`](https://github.com/russmatney/clawe/commit/12465ce)) feat: preset-filter-groups supported - Russell Matney

  > Not too bad, really.

- ([`269c67a`](https://github.com/russmatney/clawe/commit/269c67a)) chore: adds a few more repo/workspaces - Russell Matney
- ([`a94799f`](https://github.com/russmatney/clawe/commit/a94799f)) refactor: clean up filter-grouper component break down - Russell Matney

  > Breaks the filter-grouper into smaller pieces to hopefully make it
  > easier to work with.

- ([`ddd6ca6`](https://github.com/russmatney/clawe/commit/ddd6ca6)) chore: misc spacing and parens - Russell Matney
- ([`d22ec37`](https://github.com/russmatney/clawe/commit/d22ec37)) fix: allow all todos through - Russell Matney

  > Removes opt-in style in favor of showing all todos in the daily files.
  > Knowing what tags or what level of org item gets included wasn't worth
  > it - simpler is easier to work with.


### 15 Jan 2023

- ([`fd5fba9`](https://github.com/russmatney/clawe/commit/fd5fba9)) fix: restore some hidden todo data - Russell Matney

  > A fix that brings more todos into view - they were hidden when they had
  > no children but were grouping by parent-name.


### 13 Jan 2023

- ([`603c287`](https://github.com/russmatney/clawe/commit/603c287)) bind: toggle sticky on windows - Russell Matney

  > I don't use bury-all anymore, and toggling sticky is more relevant with
  > for using widgets now


### 12 Jan 2023

- ([`3fe1979`](https://github.com/russmatney/clawe/commit/3fe1979)) fix: don't bury p-i-p video clients - Russell Matney

### 11 Jan 2023

- ([`1430c16`](https://github.com/russmatney/clawe/commit/1430c16)) feat: todo cards groups by parent-names by default - Russell Matney

  > Also consumes todo-cards in all-todos list.


### 6 Jan 2023

- ([`5bc002e`](https://github.com/russmatney/clawe/commit/5bc002e)) feat: lift all nested todo cards - Russell Matney

  > Not just the first child's.

- ([`ce6fa01`](https://github.com/russmatney/clawe/commit/ce6fa01)) feat: item-card show uuid hash, raw metadata popover - Russell Matney
- ([`8d8c52a`](https://github.com/russmatney/clawe/commit/8d8c52a)) refactor: todo status change handlers - Russell Matney

  > Collecting a few data changes - feels like we want rules or events for
  > some of these.

- ([`efb2c47`](https://github.com/russmatney/clawe/commit/efb2c47)) fix: hide completed sub-todo cards in current - Russell Matney

  > the focus-stack is all about getting rid of things - no need to show
  > completed or skipped tasks.
  > 
  > ... it might actually be better to opt-in rather than out. glad the arg
  > is ~filter-by~.

- ([`55cc698`](https://github.com/russmatney/clawe/commit/55cc698)) fix: remove missing key warning - Russell Matney
- ([`3d2de83`](https://github.com/russmatney/clawe/commit/3d2de83)) feat: cljs zero pad via gstring/format - Russell Matney

  > https://clojurescript.org/reference/google-closure-library
  > 
  > https://stackoverflow.com/questions/34667532/clojure-clojurescript-e-g-the-format-function

- ([`a9b0c03`](https://github.com/russmatney/clawe/commit/a9b0c03)) feat: show subtodos as cards in current-stack items - Russell Matney

  > Also fixes when condition in item-body (with actions list) that was
  > hiding based on :org/body-string. This now also shows the item-body if
  > there are any :org/items.
  > 
  > Includes a sketch of a :presets idea for the filter-grouper, which
  > should lead to some immediate gains, e.g. a 'today' preset that restores
  > an approximation of the previous focus widget behavior (showing todos
  > just from the current daily).

- ([`de6cc73`](https://github.com/russmatney/clawe/commit/de6cc73)) fix: move no-current note to top - Russell Matney
- ([`2a17a90`](https://github.com/russmatney/clawe/commit/2a17a90)) feat: focus todos filter-grouper integration - Russell Matney

  > Todos filtered, grouped, and better sorted via filter-grouper and sorting.
  > 
  > This component could use a face-lift, but basically works.

- ([`88acbbd`](https://github.com/russmatney/clawe/commit/88acbbd)) feat: current todo using existing garden/org-body comp - Russell Matney
- ([`f6338b2`](https://github.com/russmatney/clawe/commit/f6338b2)) fix: action-list on current todo items - Russell Matney
- ([`0c6b83d`](https://github.com/russmatney/clawe/commit/0c6b83d)) feat: focus item-card consistent action list - Russell Matney
- ([`d98496d`](https://github.com/russmatney/clawe/commit/d98496d)) fix: fixed-square action icons - Russell Matney
- ([`aebe0e4`](https://github.com/russmatney/clawe/commit/aebe0e4)) feat: interactive todo status, fine-grained todo components - Russell Matney

  > todo-status component now supports cycling on click.
  > 
  > Breaks out levels, status, tags, priority, parent-names components for
  > re-use in item-header, item-body and item-card components.

- ([`830b612`](https://github.com/russmatney/clawe/commit/830b612)) feat: todo action handler sorting and misc improvements - Russell Matney

  > The action sorting by priority now moves relevant actions to the front
  > based on todo priority and tags. Ex: mark-current is the top action for
  > an incomplete, high-priority item.

- ([`a062811`](https://github.com/russmatney/clawe/commit/a062811)) feat: focus todo card better colors and layout - Russell Matney
- ([`9fc6b9c`](https://github.com/russmatney/clawe/commit/9fc6b9c)) fix: zero pad mins/secs - Russell Matney

### 5 Jan 2023

- ([`bf5c95f`](https://github.com/russmatney/clawe/commit/bf5c95f)) fix: stackable tag layout - Russell Matney
- ([`e58839d`](https://github.com/russmatney/clawe/commit/e58839d)) fix: push focus updates on more file changes - Russell Matney

  > This isn't all of them yet, but fine for now.

- ([`6837b03`](https://github.com/russmatney/clawe/commit/6837b03)) feat: focus only shows todos - Russell Matney

  > Parent names are now available inline.

- ([`6d75be4`](https://github.com/russmatney/clawe/commit/6d75be4)) feat: impl todo item card - Russell Matney

  > A bit cleaner, handles actions better, and shows the parent and source
  > file metadata.

- ([`b3ed2f1`](https://github.com/russmatney/clawe/commit/b3ed2f1)) fix: when org/priority, org/status - Russell Matney

  > Some quick ui improvements via when wrappers

- ([`7cee611`](https://github.com/russmatney/clawe/commit/7cee611)) chore: fishgame-godot workspace - Russell Matney
- ([`e041e47`](https://github.com/russmatney/clawe/commit/e041e47)) fix: no need to quote in dir-locals - Russell Matney
- ([`a23a5c9`](https://github.com/russmatney/clawe/commit/a23a5c9)) fix: update org-crud, expose and sort by priority - Russell Matney
- ([`0750542`](https://github.com/russmatney/clawe/commit/0750542)) feat: todo handlers for non db items, priority inc/dec - Russell Matney

  > Priority inc/dec is not yet impled in org-crud.
  > 
  > This consumes the handlers/todo->actions helper for focus todos - nice
  > functionality for not much lift, tho the popover can misbehave in some
  > cases.

- ([`059d0eb`](https://github.com/russmatney/clawe/commit/059d0eb)) feat: pull focus todos from more files - Russell Matney

  > Last 7 dailies, projects and journal .orgs.
  > 
  > Shrinks text to make more room for long headers.

- ([`380d5ea`](https://github.com/russmatney/clawe/commit/380d5ea)) feat: rough add-tag on frontend via js/prompt - Russell Matney
- ([`a9bb91b`](https://github.com/russmatney/clawe/commit/a9bb91b)) feat: click org item tag to remove - Russell Matney

  > Backend support for adding/removing tags from org items, plus frontend
  > support for removal.


### 4 Jan 2023

- ([`bd32bb7`](https://github.com/russmatney/clawe/commit/bd32bb7)) feat: add topgrade workspace - Russell Matney
- ([`72ac75f`](https://github.com/russmatney/clawe/commit/72ac75f)) feat: support :focus: as alternate focus component tag - Russell Matney

### 8 Dec 2022

- ([`076bf8b`](https://github.com/russmatney/clawe/commit/076bf8b)) feat: focus widget does not recenter when toggled - Russell Matney

  > Well this was nice, another behavior already available in the current
  > structure. Data-driven wm-behaviors ftw!


### 5 Dec 2022

- ([`55b9e2a`](https://github.com/russmatney/clawe/commit/55b9e2a)) fix: more useful time formats - Russell Matney

### 4 Dec 2022

- ([`6e84b2f`](https://github.com/russmatney/clawe/commit/6e84b2f)) focus: show the tag label for the 'love' tag group - Russell Matney

  > A few considerations for a tag group component layout. Feels like i
  > want a list of components more than a single div, but maybe it's fine to
  > wrap elems arbitrarily. mostly i worry about css width inheritance, and
  > would prefer to not introduce a heriarchy just for a set of
  > components... but maybe it should be multiple components, not just one.

- ([`9abc906`](https://github.com/russmatney/clawe/commit/9abc906)) wip: quick and dirty lifting by tag - Russell Matney

  > Simple if hard-codable, but this should just be dynamic and support user-input.

- ([`afe9c9f`](https://github.com/russmatney/clawe/commit/afe9c9f)) fix: show multiple current parent headers - Russell Matney

  > Apparently we can work on multiple things at once

- ([`52a3808`](https://github.com/russmatney/clawe/commit/52a3808)) feat: if current, default to only-current - Russell Matney

  > Show current parent-names together at the top

- ([`f94b397`](https://github.com/russmatney/clawe/commit/f94b397)) wsp: advent-of-bb-template - Russell Matney
- ([`d7e1722`](https://github.com/russmatney/clawe/commit/d7e1722)) feat: advent-of-godot workspace - Russell Matney

### 3 Dec 2022

- ([`b3b5703`](https://github.com/russmatney/clawe/commit/b3b5703)) feat: initial focus widget todo filters - Russell Matney

### 2 Dec 2022

- ([`dc79746`](https://github.com/russmatney/clawe/commit/dc79746)) fix: remove unused manifold require - Russell Matney
- ([`3b4c4c7`](https://github.com/russmatney/clawe/commit/3b4c4c7)) fix: restore garden watcher - Russell Matney

  > Was creating a bad path here.

- ([`93a889b`](https://github.com/russmatney/clawe/commit/93a889b)) feat: clove, advent-of-code icons - Russell Matney

### 1 Dec 2022

- ([`5b1e186`](https://github.com/russmatney/clawe/commit/5b1e186)) feat: use :org/name-string in focus component - Russell Matney

  > Updates the org-crud dep to use the new simplified name key.


### 22 Nov 2022

- ([`8415a53`](https://github.com/russmatney/clawe/commit/8415a53)) feat: restore frontend db, disable streaming all datoms - Russell Matney

  > Restores most functionality. We're not streaming all datoms into the
  > frontend any more because it's slow. Instead we want to pull whatever
  > data each page/feature needs.

- ([`b0e37b2`](https://github.com/russmatney/clawe/commit/b0e37b2)) fix: move comp.debug back to cljs - Russell Matney

  > The cljc usage here was an attempt at reusing components in clerk views.
  > it kind of worked... but not really.
  > 
  > Clerk component re-use is better with jvm hiccup structures, and which
  > unfortunately makes interacting with npm-based component libs (like the
  > floating popover) complicated.

- ([`01b8254`](https://github.com/russmatney/clawe/commit/01b8254)) fix: ensure dbs dir exists - Russell Matney
- ([`87707e1`](https://github.com/russmatney/clawe/commit/87707e1)) fix: handle tz-less int -> zdt case - Russell Matney

  > I think this could break in certain timezones? Not really sure.

- ([`2949ecd`](https://github.com/russmatney/clawe/commit/2949ecd)) feat: bb test tasks, update wing, fix dir-locals - Russell Matney
- ([`12ce6ef`](https://github.com/russmatney/clawe/commit/12ce6ef)) fix: refactor and fix broken cljs timeline tests - Russell Matney
- ([`cbae7ad`](https://github.com/russmatney/clawe/commit/cbae7ad)) fix: handle str-ifying unixPaths before transit - Russell Matney

  > Transit errors in plasma are a real pain to debug - i wonder if there's
  > a tap/portal/logging integration that could make it easier to figure out
  > what's going wrong when this blows up in the socket on-message.
  > 
  > This could be expensive (we're checking every outgoing val to see if
  > it's an instance of Path :/) - a few (fs/home)s were wrapped in strings,
  > which is probably enough to fix the issue on its own. A UnixPath transit
  > read/write handler is probably the better way. Really i just wish
  > transit would nil pun unsupported types.

- ([`febd1d7`](https://github.com/russmatney/clawe/commit/febd1d7)) wip: remove clerk completely - can't import offline - Russell Matney

  > Clerk cannot be loaded/evaled in anyway offline, which in this case
  > means I can't run cljs tests with any clerk imports. What a shame!
  > 
  > We could break clawe's clerk usage into a dependent repo, if it's needed
  > at all going forward.

- ([`59f1ff2`](https://github.com/russmatney/clawe/commit/59f1ff2)) fix: server tz tests use local, not hard-coded, tz - Russell Matney
- ([`1abe905`](https://github.com/russmatney/clawe/commit/1abe905)) fix: flag ralphie/git tests as integration - Russell Matney

  > These require external deps (git) to run. Marking them as integration
  > tests for now.

- ([`946085e`](https://github.com/russmatney/clawe/commit/946085e)) chore: resolve all clj-kondo warnings - Russell Matney
- ([`59bfd88`](https://github.com/russmatney/clawe/commit/59bfd88)) fix: ralphie.browser/open default to firefox on osx - Russell Matney

### 21 Nov 2022

- ([`bd38c0a`](https://github.com/russmatney/clawe/commit/bd38c0a)) fix: add yarn.lock to project - Russell Matney
- ([`1325b76`](https://github.com/russmatney/clawe/commit/1325b76)) ci: install yarn deps - Russell Matney
- ([`a7661b6`](https://github.com/russmatney/clawe/commit/a7661b6)) fix: bump ci java version - Russell Matney
- ([`8e9b13f`](https://github.com/russmatney/clawe/commit/8e9b13f)) ci: add basic cljs test running - Russell Matney
- ([`abf58fa`](https://github.com/russmatney/clawe/commit/abf58fa)) fix: don't use zsh before running tests - Russell Matney
- ([`27cab93`](https://github.com/russmatney/clawe/commit/27cab93)) refactor: remove a handful of zsh usages - Russell Matney

  > In favor of babashka.fs/home. Still a few more to reimpl, but the zsh
  > impl doesn't fly in ci.

- ([`65ed955`](https://github.com/russmatney/clawe/commit/65ed955)) deps: update some deps, remove local/roots - Russell Matney
- ([`9e361d3`](https://github.com/russmatney/clawe/commit/9e361d3)) feat: configure github actions for tests/linting - Russell Matney
- ([`f766bb8`](https://github.com/russmatney/clawe/commit/f766bb8)) fix: reduce org logging noise - Russell Matney
- ([`df83f7a`](https://github.com/russmatney/clawe/commit/df83f7a)) wip: adds multiple cljs testing approaches - Russell Matney

  > Tests on the jvm via kaocha should be fine.
  > 
  > For cljs, kaocha has two cljs libs to consider - included is code for
  > the first one, which is not working, nor is it shadow-cljs compatible.
  > 
  > shadow-cljs provides a test build and runner of sorts via karma, which
  > is working here, and olical/cljs-test-runner worked with the least
  > amount of config (tho it might need more work to run in a browser).
  > 
  > these tests are not yet passing - i'm capturing notes on these
  > approaches to be shared. ideally they can all run via kaocha, which
  > looks like it requires another side process... but that's not too
  > different from karma.
  > 
  > it'd also be a boon to run integration tests via etaoin - in the future.

- ([`20abf92`](https://github.com/russmatney/clawe/commit/20abf92)) feat: highlight working beyond 40 minutes in a pomodoro - Russell Matney

  > Also fallback to 'now' to let break time increment (rather than waiting
  > for the next pomodoro to start).


### 20 Nov 2022

- ([`9a5ff20`](https://github.com/russmatney/clawe/commit/9a5ff20)) feat: mindnode client with osx toggle binding - Russell Matney
- ([`4cfc021`](https://github.com/russmatney/clawe/commit/4cfc021)) feat: show last pomo duration, break duration - Russell Matney
- ([`6e1aa96`](https://github.com/russmatney/clawe/commit/6e1aa96)) refactor: pull pomodoro logic into namespace - Russell Matney

  > pulls 'bar' out of focus/widget. cleans up layout/styling.


### 17 Nov 2022

- ([`2a10db7`](https://github.com/russmatney/clawe/commit/2a10db7)) feat: some stubs for burying clients on osx - Russell Matney
- ([`613ef8f`](https://github.com/russmatney/clawe/commit/613ef8f)) feat: binding for focus toggle on osx - Russell Matney

  > still need to pull yabai/skhd configs into clawe's feature set.


### 16 Nov 2022

- ([`e226cd8`](https://github.com/russmatney/clawe/commit/e226cd8)) fix: misc styling improvements - Russell Matney

### 15 Nov 2022

- ([`c84bd72`](https://github.com/russmatney/clawe/commit/c84bd72)) chore: drop some expo related infra. - Russell Matney

  > Will get around to deleting expo stuff later.

- ([`3b7c5d6`](https://github.com/russmatney/clawe/commit/3b7c5d6)) wip: current, last pomodoro - Russell Matney

  > TODO move to a list of maps instead of named :current/:last keys.

- ([`7860f01`](https://github.com/russmatney/clawe/commit/7860f01)) refactor: consolidate client/workspace defs - Russell Matney

  > Clients don't need to be 1:1 with workspaces - this groups some related
  > client defs that don't need to be on unique workspaces (eg. browsers,
  > clove widgets).

- ([`74b1463`](https://github.com/russmatney/clawe/commit/74b1463)) feat: focus supporting 'skipped' items - Russell Matney
- ([`aca50cb`](https://github.com/russmatney/clawe/commit/aca50cb)) fix: restart config if already running in reload-config - Russell Matney

  > Includes a write-config to reformat the file whenever reload-config is
  > called.


### 13 Nov 2022

- ([`dc28424`](https://github.com/russmatney/clawe/commit/dc28424)) feat: message when no :current: item - Russell Matney
- ([`b7c32c3`](https://github.com/russmatney/clawe/commit/b7c32c3)) feat: focus empty state - Russell Matney

### 12 Nov 2022

- ([`8c1283b`](https://github.com/russmatney/clawe/commit/8c1283b)) perf: more performant focus via bury-clients - Russell Matney

  > Rather than multiple awm calls to bury clients, this adds a bury-clients
  > that only runs once.

- ([`b2c2000`](https://github.com/russmatney/clawe/commit/b2c2000)) fix: don't bury everythign when invoking rofi - Russell Matney

  > This was a fix to an old behavior, but as far as I can tell, this ends
  > up ontop as expected in my current usage, so i'm dropping it. It's been
  > burying widgets, so would need to ignore specific :bury/ignore clients
  > if it needs to be added back.

- ([`1fdabdf`](https://github.com/russmatney/clawe/commit/1fdabdf)) feat: better todo layout, include body on :current: items - Russell Matney
- ([`674b5a6`](https://github.com/russmatney/clawe/commit/674b5a6)) feat: quick time-ago helper, showing when closed items were closed - Russell Matney
- ([`23c4c77`](https://github.com/russmatney/clawe/commit/23c4c77)) feat: pushing updates to focus widget on org-file save - Russell Matney

  > Not too bad! Bit more boilerplate for the systems for supporting basic
  > widgets, to be included in the widget template.

- ([`bdbb01b`](https://github.com/russmatney/clawe/commit/bdbb01b)) feat: fix bg height, misc todo header styling - Russell Matney

### 11 Nov 2022

- ([`2b9c3dc`](https://github.com/russmatney/clawe/commit/2b9c3dc)) feat: initial focus widget working - Russell Matney

  > A widget that displays the 'goals' tagged items from the current daily
  > file.

- ([`da0c6f7`](https://github.com/russmatney/clawe/commit/da0c6f7)) wip: focus widget infrastructure complete - Russell Matney

  > Adds `bb focus`, focus clawe client def, and focus keybinding.
  > 
  > Maybe this plus the last commit are a nice example for creating a new
  > widget, maybe for code gen/templates.

- ([`ebb5087`](https://github.com/russmatney/clawe/commit/ebb5087)) wip: initial focus component - Russell Matney

  > Adds route and basic widget hiccup component.
  > 
  > Disables frontend database fetching!


### 3 Nov 2022

- ([`eb0e604`](https://github.com/russmatney/clawe/commit/eb0e604)) feat: update a few deps - Russell Matney

### 30 Oct 2022

- ([`33a9ebb`](https://github.com/russmatney/clawe/commit/33a9ebb)) fix: update app-names in todo/topbar client defs - Russell Matney
- ([`dff52cb`](https://github.com/russmatney/clawe/commit/dff52cb)) fix: restore toggleable tauri clerk clients - Russell Matney
- ([`740aa36`](https://github.com/russmatney/clawe/commit/740aa36)) fix: break pavucontrol out of spotify client def, more twitch-clove clients - Russell Matney
- ([`d9bb109`](https://github.com/russmatney/clawe/commit/d9bb109)) fix: can't pass nil to some - Russell Matney

### 29 Oct 2022

- ([`0814f33`](https://github.com/russmatney/clawe/commit/0814f33)) feat: move yabai -x and -w to mx-fast, mx - Russell Matney

  > mx tends to calc more in-context items, but is a bit slower.

- ([`d20cf4f`](https://github.com/russmatney/clawe/commit/d20cf4f)) feat: mx suggests opening known dep :git/urls - Russell Matney

  > Could reach for urls on non :git/url deps as well - parsing libs into
  > fallbacks... but they wouldn't all work.

- ([`1bfd5b2`](https://github.com/russmatney/clawe/commit/1bfd5b2)) feat: add deps-from for pulling sets of [lib coords] - Russell Matney

  > Pulls from bb.edn and deps.edn. Does not yet support aliases. May
  > actually belong in another namespace. (ralphie.deps?)

- ([`c50ef7f`](https://github.com/russmatney/clawe/commit/c50ef7f)) feat: support browser/open 'url' - Russell Matney

  > Cuz why not?

- ([`480b121`](https://github.com/russmatney/clawe/commit/480b121)) feat: zprint clawe.edn config - Russell Matney

  > Alphabetizes the map keys and formats the the clawe config file.
  > 
  > Had to add zprint to both deps.edn and bb.edn here, there's probably
  > some way to dry that up.

- ([`79d8a3e`](https://github.com/russmatney/clawe/commit/79d8a3e)) fix: update clerk sci env usage - Russell Matney

  > Updating the clerk dep, and the sci usage when creating html pages.
  > 
  > Restores the clerk notebooks in clawe! Except for the org_daily, which
  > seems to be having an issue.

- ([`ae59e31`](https://github.com/russmatney/clawe/commit/ae59e31)) fix: notebooks working at startup, better fallback route handling - Russell Matney

  > Adds a basic list of links for `/`, and an error page when notebook eval
  > crashes.
  > 
  > Also restores notebooks/notebooks. This was working after being evaled
  > in the repl - it turns out requiring it sets *file* to
  > notebooks/core.clj - i was using it for the file prefix, but it was
  > crashing in fs/as-file without it. Will have to find a better way to do
  > that - probably by collecting the namespaces, but we don't necessarily
  > have them loaded yet :/

- ([`bbeefc2`](https://github.com/russmatney/clawe/commit/bbeefc2)) deps: neil deps upgrade - Russell Matney

  > Still skipping the tick and time-literals upgrades, as they failed for
  > me last time.

- ([`a061af7`](https://github.com/russmatney/clawe/commit/a061af7)) feat: fallback icon for clove apps - Russell Matney
- ([`30e6394`](https://github.com/russmatney/clawe/commit/30e6394)) fix: actually prevent deleting the last workspace - Russell Matney

  > some bad logic here - this was actually preventing the command from
  > deleting only one workspace.

- ([`b252c16`](https://github.com/russmatney/clawe/commit/b252c16)) feat: move bury-clients to focus-client - Russell Matney

  > Moves the bury-clients logic to focus-client. focusing a client now also
  > buries all clients, except for those with :bury/ignore flags.

- ([`4276a02`](https://github.com/russmatney/clawe/commit/4276a02)) chore: add twitch-chat client, bury/ignore for that and obs clients - Russell Matney
- ([`2bb6596`](https://github.com/russmatney/clawe/commit/2bb6596)) feat: bury-clients ignores :bury/ignore client defs - Russell Matney

  > Also updates show-client to fetch active workspace clients when not
  > passed a current-workspace.

- ([`2df7df4`](https://github.com/russmatney/clawe/commit/2df7df4)) refactor: drop bury-all-clients from wm protocol - Russell Matney

  > We'll just use bury-clients and add opts going forward.

- ([`a09b9a6`](https://github.com/russmatney/clawe/commit/a09b9a6)) fix: restore working clerk notebook - Russell Matney
- ([`61462b3`](https://github.com/russmatney/clawe/commit/61462b3)) break: r.awm/focus-client no longer buries clients - Russell Matney

  > Burying clients should now be done explicitly or in the clawe.wm
  > namespace.

- ([`1191301`](https://github.com/russmatney/clawe/commit/1191301)) fix: client match passing opts properly - Russell Matney

  > This was not passing the client def as opts to the client/match?
  > function. Maybe not the best api here.


### 28 Oct 2022

- ([`d80fd7e`](https://github.com/russmatney/clawe/commit/d80fd7e)) feat: ralphie.browser/reload-tabs - Russell Matney

  > Requires brotab 1.4 or greater


### 27 Oct 2022

- ([`3083ad9`](https://github.com/russmatney/clawe/commit/3083ad9)) fix: yabai/fix-topbar on window_create - Russell Matney
- ([`1d9bdcd`](https://github.com/russmatney/clawe/commit/1d9bdcd)) feat: suggest neil dep add if open github tab - Russell Matney

  > Not too bad! I wish tmux/fire had current-workspace defaults, but that's
  > also not a dependency I want to force on tmux/fire... maybe there could
  > be a wm/tmux-fire wrapped version? feels odd.

- ([`69858b7`](https://github.com/russmatney/clawe/commit/69858b7)) tweak: use fs/home instead of hard-coded '~' - Russell Matney
- ([`98176bc`](https://github.com/russmatney/clawe/commit/98176bc)) fix: mx - drop the pesky dash in m-x - Russell Matney

### 25 Oct 2022

- ([`c6549b4`](https://github.com/russmatney/clawe/commit/c6549b4)) chore: add funding.yml - Russell Matney

### 20 Oct 2022

- ([`a180997`](https://github.com/russmatney/clawe/commit/a180997)) doc: link to workspace file in readme - Russell Matney

### 19 Oct 2022

- ([`e9933ed`](https://github.com/russmatney/clawe/commit/e9933ed)) feat: remove home from topbar directory - Russell Matney

### 18 Oct 2022

- ([`63f6abe`](https://github.com/russmatney/clawe/commit/63f6abe)) fix: remove / from tauri window titles - Russell Matney
- ([`8b8eef6`](https://github.com/russmatney/clawe/commit/8b8eef6)) chore: remove tauri in favor of `clove` - Russell Matney

  > Removing all the tauri from this repo - all we need is the `clove`
  > executable managed in russmatney/clove.

- ([`8937f94`](https://github.com/russmatney/clawe/commit/8937f94)) refactor: move to `clove` as tauri binary - Russell Matney

### 17 Oct 2022

- ([`68da91a`](https://github.com/russmatney/clawe/commit/68da91a)) feat: convert clipboard json to edn - Russell Matney

  > A leading string will cause the whole blob to be wrapped in square
  > brackets, which isn't always right, but will be right for this use-case,
  > which is copy-pasting json-based schemas for things like vega-lite into
  > edn/clj based charts.


### 16 Oct 2022

- ([`ac97d4a`](https://github.com/russmatney/clawe/commit/ac97d4a)) misc: watch workspace, emacs/open doc string - Russell Matney
- ([`a02647c`](https://github.com/russmatney/clawe/commit/a02647c)) feat: open workspace on github - Russell Matney

  > Adds a quick workspace option - all wsps can now be used as quick github
  > bookmarks.


### 13 Oct 2022

- ([`6ad07da`](https://github.com/russmatney/clawe/commit/6ad07da)) misc: org-blog icon, more repo workspaces - Russell Matney
- ([`f03e529`](https://github.com/russmatney/clawe/commit/f03e529)) feat: defcom converting clipboard-org to markdown - Russell Matney

### 11 Oct 2022

- ([`7d05e86`](https://github.com/russmatney/clawe/commit/7d05e86)) chore: couple spicetify repos - Russell Matney
- ([`17ae07a`](https://github.com/russmatney/clawe/commit/17ae07a)) fix: ff dev edition desktop entry changed it's name - Russell Matney

### 10 Oct 2022

- ([`d36f171`](https://github.com/russmatney/clawe/commit/d36f171)) fix: garden paths filter out non-existent paths - Russell Matney

### 6 Oct 2022

- ([`a1a8d11`](https://github.com/russmatney/clawe/commit/a1a8d11)) feat: cleaner garden, consume latest org-crud - Russell Matney
- ([`a4ab280`](https://github.com/russmatney/clawe/commit/a4ab280)) chore: misc clean up - Russell Matney

  > The blog_daily is removed as it now belongs to russmatney/org-blog

- ([`e76def8`](https://github.com/russmatney/clawe/commit/e76def8)) fix: restore tmux tables - Russell Matney

  > This one was not easy to spot... seems odd that the markdown comments
  > show up when {:result :hide} is set.


### 5 Oct 2022

- ([`f402de3`](https://github.com/russmatney/clawe/commit/f402de3)) feat: initial lichess clerk notebook - Russell Matney

  > Just listing lichess games in a table for now

- ([`cde0743`](https://github.com/russmatney/clawe/commit/cde0743)) wsps: bbin, org-blog, blorg - Russell Matney

### 4 Oct 2022

- ([`bb2095a`](https://github.com/russmatney/clawe/commit/bb2095a)) feat: poc for writing out static blog dailies via clerk - Russell Matney
- ([`1819ecb`](https://github.com/russmatney/clawe/commit/1819ecb)) feat: garden/daily-path api supporting n days-ago - Russell Matney
- ([`8721fc7`](https://github.com/russmatney/clawe/commit/8721fc7)) fix: remove 'core' from notebooks list - Russell Matney

  > Important to not depend on any clerk here so the clawe bb (clawe-m-x
  > command) side doesn't need to import clerk.

- ([`b49cd7b`](https://github.com/russmatney/clawe/commit/b49cd7b)) feat: drop notebooks.nav, consume my-notebooks viewer - Russell Matney

  > Now navigating between notebooks... tho we've lost some important
  > features, like :width control.

- ([`4e043cc`](https://github.com/russmatney/clawe/commit/4e043cc)) feat: preferred journal index, add nj/viewers repo - Russell Matney
- ([`74a66f7`](https://github.com/russmatney/clawe/commit/74a66f7)) feat: overwrite notebook viewer, include links to other notebooks - Russell Matney

  > Writes a new notebook-level viewer.
  > Strips out the toc and dark mode toggles.
  > Adds a notebook nav component for jumping between other notebooks in the
  > dir.
  > 
  > Note super pretty, and collides with notebook content in small screen
  > widths - more work to do here, but at least now we're exposing and
  > easily moving between notebooks.

- ([`0114368`](https://github.com/russmatney/clawe/commit/0114368)) chore: update to latest clerk sha - Russell Matney

### 3 Oct 2022

- ([`0945f4d`](https://github.com/russmatney/clawe/commit/0945f4d)) feat: initial daily blog builder notebook - Russell Matney
- ([`8a0d589`](https://github.com/russmatney/clawe/commit/8a0d589)) fix: open wps and emacs as top wsp rofi option - Russell Matney
- ([`9eb539d`](https://github.com/russmatney/clawe/commit/9eb539d)) fix: move tmux from clawe notebook to tmux notebook - Russell Matney
- ([`dd85d50`](https://github.com/russmatney/clawe/commit/dd85d50)) feat: picture-in-picture icon - Russell Matney
- ([`f6e2f04`](https://github.com/russmatney/clawe/commit/f6e2f04)) feat: cleaner notebook impl - :result :show boundary - Russell Matney
- ([`46b2fcd`](https://github.com/russmatney/clawe/commit/46b2fcd)) fix: cycle-focus-clients come ontop - Russell Matney

  > Rather than leave focused clients underneath, this brings them ontop.


### 2 Oct 2022

- ([`a4e6500`](https://github.com/russmatney/clawe/commit/a4e6500)) feat: initial todos notebook - Russell Matney

### 1 Oct 2022

- ([`894b31c`](https://github.com/russmatney/clawe/commit/894b31c)) feat: org daily, recent clerk notebooks - Russell Matney

  > Rendering today's daily org and the last ten touched org files.

- ([`ee5ffde`](https://github.com/russmatney/clawe/commit/ee5ffde)) refactor: move components to cljc - Russell Matney

  > Trying to get some existing components consumable in clerk

- ([`66ba59f`](https://github.com/russmatney/clawe/commit/66ba59f)) refactor: cleaner channel->notebook state datastructure - Russell Matney

  > Much easier working with a ch->notebook map, and grouping by notebook as
  > needed.
  > 
  > Cleans up the clerk state vis to help debugging.


### 30 Sep 2022

- ([`0e9951c`](https://github.com/russmatney/clawe/commit/0e9951c)) wip: nav component attempt - Russell Matney

  > Note sure what components should/could look like in clerk - i'd love to
  > hook into the table of contents or some other bread-crumbs/history
  > pattern.

- ([`9bf30fb`](https://github.com/russmatney/clawe/commit/9bf30fb)) fix: add TOC to wallpapers - Russell Matney
- ([`1e792e7`](https://github.com/russmatney/clawe/commit/1e792e7)) feat: quick navigation hack using headers - Russell Matney

  > For now I'm not sure how to inject complex template-y details into clerk
  > - a quick read leads me to thinking i might need to write a sci-viewere,
  > but it seems likely that there'd be some support for passing in wrappers
  > without reimpling too much.
  > 
  > A first-class navigation bar/history tool would be a nice win for people
  > traversing and working in multiple notebooks.

- ([`11f4c7e`](https://github.com/russmatney/clawe/commit/11f4c7e)) git status notebook: fan out types of repo status - Russell Matney
- ([`c9af537`](https://github.com/russmatney/clawe/commit/c9af537)) feat: consume clerk html wrapper, send current path - Russell Matney

  > Cleans up the log-state clerk status helper, which helps debugging quite
  > a bit.
  > 
  > When a client connects, it now sends a lone {:path doc.pathname} after
  > from it's 'onopen' - this lets us manage which channel is connected to
  > which notebook, so that we can update the currently watching clients
  > when that notebook is re-evaled.
  > 
  > Should probably just use a simple ch->notebook map as the datastructure,
  > and group-by notebook as needed - that'd clear up the hairy 'update'
  > logic.

- ([`b4aa7d2`](https://github.com/russmatney/clawe/commit/b4aa7d2)) feat: cleaner directory display, misc todos - Russell Matney
- ([`ea8038d`](https://github.com/russmatney/clawe/commit/ea8038d)) feat: recent commit notebook, helpers for ingesting more - Russell Matney
- ([`10703c1`](https://github.com/russmatney/clawe/commit/10703c1)) feat: initial git-status clerk notebook - Russell Matney
- ([`73556ad`](https://github.com/russmatney/clawe/commit/73556ad)) feat: clerk clawe notebook clean up - Russell Matney

  > Using ns-level flags is much cleaner.

- ([`d0e47c6`](https://github.com/russmatney/clawe/commit/d0e47c6)) feat: enable clerk toc in a few notebooks - Russell Matney
- ([`ed99523`](https://github.com/russmatney/clawe/commit/ed99523)) feat: open wsp + emacs or term - Russell Matney

  > A simple solution to sometimes wanting to open a client in the new wsp.
  > 
  > Benefits from rofi-wizard flow

- ([`425b573`](https://github.com/russmatney/clawe/commit/425b573)) feat: tables for tmux sessions, panes in clawe notebook - Russell Matney

  > Easy peasy!

- ([`186d160`](https://github.com/russmatney/clawe/commit/186d160)) feat: wm/bury-(all-)client(s) added and impled for awesome - Russell Matney

  > Wrapped in a try-catch, so osx should be fine with this.

- ([`e1b579e`](https://github.com/russmatney/clawe/commit/e1b579e)) ralphie: restored, improved awm client focus, bury, bury-all - Russell Matney
- ([`093ce2a`](https://github.com/russmatney/clawe/commit/093ce2a)) feat: notebook rofi: eval+broadcast, open-in-emacs, open-in-browser - Russell Matney

  > Implements a few convenient notebook rofi commands.

- ([`e923e97`](https://github.com/russmatney/clawe/commit/e923e97)) feat: ralphie.browser/open-dev supports :url - Russell Matney

  > Now opening new dev-browser tabs.

- ([`09804ac`](https://github.com/russmatney/clawe/commit/09804ac)) wip: rofi notebook actions in clawe/m-x - Russell Matney

  > Removes clerk dep from notebooks.core so it can be called without a
  > clerk dep.
  > 
  > Adds `notebook: <nb-name>` to clawe/m-x with some todo actions.
  > 
  > Improves ralphie.rofi/rofi command to support a list of xs as the first
  > arg.

- ([`541f5b9`](https://github.com/russmatney/clawe/commit/541f5b9)) fix: use change function names in doctor-server - Russell Matney

### 29 Sep 2022

- ([`e8cbce0`](https://github.com/russmatney/clawe/commit/e8cbce0)) fix: misc clean up - Russell Matney
- ([`5b627d5`](https://github.com/russmatney/clawe/commit/5b627d5)) feat: clerk notebook clean up, current wsp and health report - Russell Matney
- ([`9126253`](https://github.com/russmatney/clawe/commit/9126253)) feat: maintain notebook->channels map in notebooks/clerk.clj - Russell Matney

  > Includes an update-open-notebooks helper for evaling and pushing the
  > current notebook to any open clients.

- ([`1dafd7d`](https://github.com/russmatney/clawe/commit/1dafd7d)) feat: working clerk notebook socket updates - Russell Matney
- ([`23afbfe`](https://github.com/russmatney/clawe/commit/23afbfe)) feat: sort indexes after opening new wsp - Russell Matney
- ([`ac4c104`](https://github.com/russmatney/clawe/commit/ac4c104)) fix: rofi select shortest match - Russell Matney

  > ralphie/rofi has had this bug forever! When rofi labels start with the
  > same prefix, it would select the first starts-with? match, which could
  > end up as a different item than the selected. Especially common when
  > selecting a workspace that starts with the same label (e.g. clerk and
  > clerk-demo).

- ([`610caf2`](https://github.com/russmatney/clawe/commit/610caf2)) feat: support+set preferred workspace indexes - Russell Matney
- ([`95b25f0`](https://github.com/russmatney/clawe/commit/95b25f0)) wip: clerk update via broadcast attempt - Russell Matney

### 28 Sep 2022

- ([`5ca41a3`](https://github.com/russmatney/clawe/commit/5ca41a3)) feat: update clerk-tauri apps to use new paths - Russell Matney

  > Toggleable clerk notebooks! well hey!

- ([`737f740`](https://github.com/russmatney/clawe/commit/737f740)) feat: wallpaper notebook setting os wallpaper - Russell Matney

  > Pretty cool!

- ([`51cc12b`](https://github.com/russmatney/clawe/commit/51cc12b)) feat: navigating notebooks in src/notebooks/* - Russell Matney

  > No live-updating yet, but navigation is working. Also drops the
  > clerk/serve! systemic setup.

- ([`23796c4`](https://github.com/russmatney/clawe/commit/23796c4)) feat: ignore picture-in-picture from web client - Russell Matney
- ([`3c512be`](https://github.com/russmatney/clawe/commit/3c512be)) feat: serving some clerk notebooks - Russell Matney

  > Serves clerk notebooks when navigating to
  > <server-url>/notebooks/<nb-name>. Supports connecting to websockets and
  > re-evaluates on refresh. Even evals funcs passed from `v/clerk-eval`!

- ([`855f817`](https://github.com/russmatney/clawe/commit/855f817)) fix: drop some annoying logs - Russell Matney
- ([`ef66d8e`](https://github.com/russmatney/clawe/commit/ef66d8e)) fix: flip bury-all? default, don't bury clients when focusing - Russell Matney

  > bury-all is too big of a gun when focusing - flipping the default and
  > the behavior when focusing clients to see what happens.

- ([`5851d11`](https://github.com/russmatney/clawe/commit/5851d11)) feat: support :match/ignore-names in client defs - Russell Matney

  > Prevents toggling 'web' from grabbing the picture-in-picture window.

- ([`0a1859a`](https://github.com/russmatney/clawe/commit/0a1859a)) wip: towards a new clerk-ws endpoint - Russell Matney
- ([`b1aa263`](https://github.com/russmatney/clawe/commit/b1aa263)) chore: move plasma ws to `/plasma-ws` route - Russell Matney

  > Creating space for multiple websocket handlers.

- ([`a8efeb3`](https://github.com/russmatney/clawe/commit/a8efeb3)) chore: add re-db workspace - Russell Matney

  > Have got to prevent this .edn diff overhead somehow or other.

- ([`ecd97eb`](https://github.com/russmatney/clawe/commit/ecd97eb)) fix: rename notebooks.system to notebooks.server - Russell Matney
- ([`35c801d`](https://github.com/russmatney/clawe/commit/35c801d)) feat: clerk-workspace app toggle - Russell Matney

  > No server for this 9999 port yet. This commit shows the overhead for
  > adding a toggleable tauri app - ideally we'd remove the bb.edn
  > middleman, and reduce the clawe.edn diff. Known problems!

- ([`177173d`](https://github.com/russmatney/clawe/commit/177173d)) feat: create workspace when there is none - Russell Matney

  > A quick fallback workspace - creates 'journal' if there is no current
  > workspace.
  > 
  > assumption: wm/current-workspace returns nil when there are no
  > workspaces at all.

- ([`9d17675`](https://github.com/russmatney/clawe/commit/9d17675)) fix: don't delete the last workspace - Russell Matney

  > Deleting the last workspace/tag reveals some issues/assumptions,
  > particularly when trying to create an emacs/terminal client. Those
  > should be fixed to create workspace if there is none as well (i.e.
  > wm/toggle-client should work when there are no workspaces), but that case
  > is quite rare. This at least prevents the last workspace from being
  > auto-deleted during clean up - a no workspace situation could still be
  > achieved via the wm directly, if the wm allows it (the way awesome
  > does).

- ([`f832bd0`](https://github.com/russmatney/clawe/commit/f832bd0)) fix: prevent delete workspaces loop - Russell Matney

  > The code was already implemented to solve this, but was not passing
  > the wsp def into the recursive `clean-workspaces` call, so it just
  > looped forever.

- ([`1f9467c`](https://github.com/russmatney/clawe/commit/1f9467c)) feat: basic navigation 'component' for walking notebooks - Russell Matney
- ([`e94aacb`](https://github.com/russmatney/clawe/commit/e94aacb)) feat: toggleable clerk instance via tauri - Russell Matney
- ([`fc5f90e`](https://github.com/russmatney/clawe/commit/fc5f90e)) feat: some clerk infra, buttons for navigating/rerendering notebooks - Russell Matney

  > Creates a clerk system and execs a handful of ideas/toys


### 27 Sep 2022

- ([`a02a45d`](https://github.com/russmatney/clawe/commit/a02a45d)) icons: org-crud genie lamp - Russell Matney
- ([`869967f`](https://github.com/russmatney/clawe/commit/869967f)) wip: new clerk notebook - Russell Matney

  > Trying to create a scratchpad with some relevant info... hopefully tie
  > this into a tauri native app that can be navigated.


### 26 Sep 2022

- ([`123864b`](https://github.com/russmatney/clawe/commit/123864b)) deps: include clerk markdown js deps - Russell Matney

  > Auto-added during install/init.

- ([`df14b18`](https://github.com/russmatney/clawe/commit/df14b18)) fix: better first-option in wsp/client def m-x - Russell Matney
- ([`690aa7c`](https://github.com/russmatney/clawe/commit/690aa7c)) docs: tweak, drop initial header - Russell Matney
- ([`f4d4f31`](https://github.com/russmatney/clawe/commit/f4d4f31)) chore: misc clerk notebook clean up - Russell Matney

  > Trims some values, hides some results. Dashboards coming along!

- ([`2132acb`](https://github.com/russmatney/clawe/commit/2132acb)) fix: add clerk dep back - Russell Matney

  > I was able to get clerk building fine on openjdk-18 - there were some
  > issues running with openjdk-graalvm, but i don't remember why I was
  > using that - may have to switch back to it for some projects.

- ([`5816636`](https://github.com/russmatney/clawe/commit/5816636)) fix: update to non 'deprecated' :on-close-message - Russell Matney

  > Not sure why the ring undertow handler decides to assert (i.e. crash) on
  > a 'deprecated' key - crashing usually indicates that it's been fully
  > removed, so maybe there's some dead code in there?
  > 
  > Either way, we're using the new key now.

- ([`d3deb14`](https://github.com/russmatney/clawe/commit/d3deb14)) fix: restore sxhkd restart - Russell Matney

  > This was disabled to prevent apps created by bindings from being dropped, but
  > we should really learn to start things properly instead.

- ([`f49f1f0`](https://github.com/russmatney/clawe/commit/f49f1f0)) feat: m-x-fast - Russell Matney
- ([`c79a74d`](https://github.com/russmatney/clawe/commit/c79a74d)) feat: update deps per antq - Russell Matney
- ([`2f4594a`](https://github.com/russmatney/clawe/commit/2f4594a)) fix: remove duplicated wsp config - Russell Matney
- ([`9c001f3`](https://github.com/russmatney/clawe/commit/9c001f3)) fix: remove nonsense file - Russell Matney
- ([`78fb071`](https://github.com/russmatney/clawe/commit/78fb071)) chore: better garden ingest logs - Russell Matney
- ([`446293a`](https://github.com/russmatney/clawe/commit/446293a)) fix: remove redundant wsp index - Russell Matney
- ([`6bde1df`](https://github.com/russmatney/clawe/commit/6bde1df)) feat: drop tmux sesh fallback - Russell Matney

  > Proper tmux startup makes this redundant.

- ([`9336ad2`](https://github.com/russmatney/clawe/commit/9336ad2)) conf: support xcode client - Russell Matney

### 25 Sep 2022

- ([`21b1216`](https://github.com/russmatney/clawe/commit/21b1216)) fix: restore center window - Russell Matney
- ([`d48b2b5`](https://github.com/russmatney/clawe/commit/d48b2b5)) wip: clerk wallpaper setting demo - Russell Matney

  > Begins this feat, but blocked by no wifi


### 23 Sep 2022

- ([`233bb53`](https://github.com/russmatney/clawe/commit/233bb53)) feat: clerk notebook with rerender topbar button - Russell Matney

### 20 Sep 2022

- ([`cee0287`](https://github.com/russmatney/clawe/commit/cee0287)) docs: readme tweaks - Russell Matney
- ([`393a649`](https://github.com/russmatney/clawe/commit/393a649)) fix: jump to 0th wsp properly - Russell Matney
- ([`8c26b36`](https://github.com/russmatney/clawe/commit/8c26b36)) docs: readme rewrite - Russell Matney

### 19 Sep 2022

- ([`db7042b`](https://github.com/russmatney/clawe/commit/db7042b)) misc: noisey stuff - Russell Matney

  > Really need to get clawe.edn into a consistent format.

- ([`8287eff`](https://github.com/russmatney/clawe/commit/8287eff)) fix: better page size for full-garden-sync - Russell Matney
- ([`74139d8`](https://github.com/russmatney/clawe/commit/74139d8)) fix: remove circular dep - Russell Matney

  > Will need to approach this some other way.

- ([`08ab777`](https://github.com/russmatney/clawe/commit/08ab777)) feat: clerk and clerk-demo workspaces - Russell Matney
- ([`bd201ee`](https://github.com/russmatney/clawe/commit/bd201ee)) feat: icon for xcode - Russell Matney

### 17 Sep 2022

- ([`6031fa0`](https://github.com/russmatney/clawe/commit/6031fa0)) feat: including basic linked items - Russell Matney
- ([`a519c67`](https://github.com/russmatney/clawe/commit/a519c67)) fix: require :org/source-file in fetch-with-org-id - Russell Matney

  > Also passing remove-item, including 'til's.

- ([`deb986b`](https://github.com/russmatney/clawe/commit/deb986b)) fix: latest workspace updates - Russell Matney
- ([`ab583e9`](https://github.com/russmatney/clawe/commit/ab583e9)) wip: handful of blog-supporting pieces - Russell Matney

  > Removes tx->fe-db as a server dep, mostly as a reaction to the way
  > systemic systems seem to shutdown their parents when they are stopped -
  > not sure if that's a sign my system design is crap.
  > 
  > blog.core passing some new opts into quickblog, that mostly end up being
  > called/used in org-crud anyway - there's likely a few quickblog funcs
  > that could be passed to support a much smaller change to that lib.
  > 
  > Adds a rerender-the-blog garden->blog db listener.
  > 
  > Sending less data the frontend by default - ideally the backend would
  > have everything, and the frontend some less portion - this would enable
  > the frontend to fetch by id when missing items were referenced.

- ([`06898fb`](https://github.com/russmatney/clawe/commit/06898fb)) feat: rerender blog on org-file-save - Russell Matney

### 7 Sep 2022

- ([`21715d9`](https://github.com/russmatney/clawe/commit/21715d9)) feat: thunar, finder icons - Russell Matney
- ([`dbce148`](https://github.com/russmatney/clawe/commit/dbce148)) fix: ensure window-title exists before comparing - Russell Matney

  > Resolves null pointer exception in client/match?
  > 
  > Couldn't this be nil-punned?

- ([`1cdac7d`](https://github.com/russmatney/clawe/commit/1cdac7d)) feat: custom emacs icons per workspace - Russell Matney

### 6 Sep 2022

- ([`864f41e`](https://github.com/russmatney/clawe/commit/864f41e)) fix: delete workspaces by index instead of title - Russell Matney

  > Ended up with a work-around in clean-workspaces that only deletes one
  > wsp per round, which avoids deleting two indexes in a row (which can
  > result in the indexes shifting before the second one is executed.)
  > 
  > The real problem was some bad tag names getting created, and then
  > awesome not finding a tag name with a `.` in it.

- ([`ab0b27a`](https://github.com/russmatney/clawe/commit/ab0b27a)) feat: update godot def to use :merge/skip-title - Russell Matney
- ([`da5b2d9`](https://github.com/russmatney/clawe/commit/da5b2d9)) feat: support setting :match/skip-title when merging via :merge/skip-title - Russell Matney

  > Probably confusing - this lets a client-def opt-in to :match/skip-title
  > at client-def merge-time. We want to avoid setting :match/skip-title for
  > _every_ client/match?, so `:merge` is used as a prefix to reference
  > def-merging-time.

- ([`d147d10`](https://github.com/russmatney/clawe/commit/d147d10)) chore: lower-case app-names, window-titles - Russell Matney
- ([`17bec65`](https://github.com/russmatney/clawe/commit/17bec65)) defs: pirates, tilePipe workspaces - Russell Matney

### 4 Sep 2022

- ([`8632ac8`](https://github.com/russmatney/clawe/commit/8632ac8)) fix: topbar osx layout - Russell Matney

### 2 Sep 2022

- ([`7605cff`](https://github.com/russmatney/clawe/commit/7605cff)) wip: show :current: org items along with queued-todos - Russell Matney
- ([`0f15266`](https://github.com/russmatney/clawe/commit/0f15266)) feat: support use-workspace-title and soft-title - Russell Matney

  > Supporting these :match/* feats together makes toggling a workspace's
  > correct godot client possible. The logic in client-match? could be
  > cleaned up a bit at some point.


### 1 Sep 2022

- ([`344e794`](https://github.com/russmatney/clawe/commit/344e794)) feat: obs client toggle - Russell Matney

### 30 Aug 2022

- ([`ed218e9`](https://github.com/russmatney/clawe/commit/ed218e9)) feat: aseprite, godot toggle bindings and workspaces - Russell Matney
- ([`b0ac68c`](https://github.com/russmatney/clawe/commit/b0ac68c)) feat: add show-all buttons to table/list pagination - Russell Matney
- ([`97409e4`](https://github.com/russmatney/clawe/commit/97409e4)) fix: actions-list show-less button only when it makes sense - Russell Matney
- ([`b9f2fb5`](https://github.com/russmatney/clawe/commit/b9f2fb5)) topbar: move left/right back to flex - Russell Matney
- ([`3646687`](https://github.com/russmatney/clawe/commit/3646687)) docs: misc todos - Russell Matney
- ([`4f73684`](https://github.com/russmatney/clawe/commit/4f73684)) fix: disable sxhkd restart for now - Russell Matney

  > This kills some running apps if they've been started before a proper
  > clawe startup/reload - disabling for now. It should be run manually to
  > figure out if it's needed/what problems it causes (i.e. what depends on
  > this sxhkd session anyway? shouldn't procs be handed off somewhere?).

- ([`a98b82c`](https://github.com/russmatney/clawe/commit/a98b82c)) feat: show todos/events counts on dashboard, fetch more todos - Russell Matney

  > Fetches 200 incomplete todos, instead of 100 todos and then filtering
  > out the complete ones.
  > 
  > Removes the topbar feats, and pulls out the initial filters.

- ([`ff5976a`](https://github.com/russmatney/clawe/commit/ff5976a)) feat: support pre-pagination filter on todos - Russell Matney

  > So we can filter for incomplete todos before taking `n`.

- ([`1c65c38`](https://github.com/russmatney/clawe/commit/1c65c38)) feat: better queue/unqueue todo action text - Russell Matney
- ([`5490f06`](https://github.com/russmatney/clawe/commit/5490f06)) feat: ingest buttons, improved default filters on todos page - Russell Matney
- ([`d50a70b`](https://github.com/russmatney/clawe/commit/d50a70b)) chore: drop notifies from toggle-client - Russell Matney

  > This was useful for debugging, but runs fine now and is a bit noisey.

- ([`24cceda`](https://github.com/russmatney/clawe/commit/24cceda)) wip: more clj-kondo, workspaces - Russell Matney
- ([`3fb1dfc`](https://github.com/russmatney/clawe/commit/3fb1dfc)) chore: reduce println, rename fns, add docstrings to clawe reload - Russell Matney

### 27 Aug 2022

- ([`754c407`](https://github.com/russmatney/clawe/commit/754c407)) wip: building ns deps visualization - Russell Matney
- ([`f16745e`](https://github.com/russmatney/clawe/commit/f16745e)) feat: better dashboard filter defaults - Russell Matney
- ([`d3ff29d`](https://github.com/russmatney/clawe/commit/d3ff29d)) feat: basic per-repo commit ingestion buttons - Russell Matney
- ([`6a8e293`](https://github.com/russmatney/clawe/commit/6a8e293)) feat: wallpaper actions (set, ingest) - Russell Matney

  > Plus fixes popup icon background.

- ([`50d84cd`](https://github.com/russmatney/clawe/commit/50d84cd)) misc: topbar height tweaks - Russell Matney
- ([`08d543e`](https://github.com/russmatney/clawe/commit/08d543e)) chore: drop dead awesome config, awesome rule handling - Russell Matney
- ([`8d64c69`](https://github.com/russmatney/clawe/commit/8d64c69)) chore: drop uberjar building code - Russell Matney
- ([`1fae7eb`](https://github.com/russmatney/clawe/commit/1fae7eb)) feat: drop clawe uberjar, use -x clawe.core/fire - Russell Matney

  > Updates the fallback defkbd cli command to use `-x clawe.core/fire`
  > rather than rely on a built clawe uberjar. Commands are a touch slower,
  > but can be optimized on a per-keybinding basis going forward.

- ([`b884391`](https://github.com/russmatney/clawe/commit/b884391)) feat: improved icons on various actions - Russell Matney
- ([`cb4b034`](https://github.com/russmatney/clawe/commit/cb4b034)) refactor: include all actions, move conds to :action/disabled - Russell Matney

  > This leads to more consistent coloring when map-indexing and using the
  > color-wheel.
  > 
  > Also sets some new icons via heroicons.

- ([`5f8ba3b`](https://github.com/russmatney/clawe/commit/5f8ba3b)) fix: current-task conditional fix - Russell Matney
- ([`6f5b5ed`](https://github.com/russmatney/clawe/commit/6f5b5ed)) feat: actions list - disabled sorted last and hiding colors - Russell Matney

### 26 Aug 2022

- ([`58ad401`](https://github.com/russmatney/clawe/commit/58ad401)) fix: yabai config - smaller topbar (bottombar?) - Russell Matney
- ([`39093bd`](https://github.com/russmatney/clawe/commit/39093bd)) feat: collapsible all-todos list on dashboard - Russell Matney
- ([`55f4b0e`](https://github.com/russmatney/clawe/commit/55f4b0e)) fix: requeue action hide/show logic - Russell Matney
- ([`cf8bf37`](https://github.com/russmatney/clawe/commit/cf8bf37)) defs: add clojure docs site workspace - Russell Matney
- ([`fb2da84`](https://github.com/russmatney/clawe/commit/fb2da84)) feat: un-projectile-ignore some files - Russell Matney
- ([`18932a2`](https://github.com/russmatney/clawe/commit/18932a2)) fix: yabai - use fallback wsp titles - Russell Matney
- ([`3a46edb`](https://github.com/russmatney/clawe/commit/3a46edb)) refactor: consume new group-by helper - Russell Matney
- ([`8c66d9f`](https://github.com/russmatney/clawe/commit/8c66d9f)) fix: util now cljc, adds explode-group-by - Russell Matney
- ([`85f477f`](https://github.com/russmatney/clawe/commit/85f477f)) yabai: add config - Russell Matney

  > Pulling this over from my dotfiles for now - too annoying to have to
  > change projects to find it.

- ([`ffe7d2b`](https://github.com/russmatney/clawe/commit/ffe7d2b)) refactor: drop uberjar rebuild - Russell Matney

  > Need to do an audit for where the uberjar is still used (sxhkd
  > bindings?), but otherwise we're not building it any more


### 25 Aug 2022

- ([`888f301`](https://github.com/russmatney/clawe/commit/888f301)) misc: rough topbar, filter, events improvements - Russell Matney
- ([`f4506f0`](https://github.com/russmatney/clawe/commit/f4506f0)) wip: initial dashboard as home page - Russell Matney

  > Some issues, but the data is there, so the rest is the fun part.

- ([`3241b24`](https://github.com/russmatney/clawe/commit/3241b24)) feat: add event-cluster to todo-list component - Russell Matney
- ([`3ca5ec4`](https://github.com/russmatney/clawe/commit/3ca5ec4)) feat: quick re-queue action - Russell Matney
- ([`7975a95`](https://github.com/russmatney/clawe/commit/7975a95)) feat: topbar metadata clean up - Russell Matney
- ([`cf97a6c`](https://github.com/russmatney/clawe/commit/cf97a6c)) wip: attempt to show all-tags in todo rows - Russell Matney
- ([`5cef3e4`](https://github.com/russmatney/clawe/commit/5cef3e4)) misc: clean up, new workspace - Russell Matney

### 24 Aug 2022

- ([`b2ea1dd`](https://github.com/russmatney/clawe/commit/b2ea1dd)) feat: consistent action icon colors - Russell Matney

  > Supports setting action icon colors from :action/class, and uses indexes
  > to find a 'consistent' color for actions as they leave the
  > handlers/->actions helper. Colors for specific actions can be set on the
  > action definition itself.

- ([`1ab177b`](https://github.com/russmatney/clawe/commit/1ab177b)) feat: todo-row layout - cleaner than todo cell look - Russell Matney
- ([`4a9d9cb`](https://github.com/russmatney/clawe/commit/4a9d9cb)) tweak: todo action priority adjustments - Russell Matney
- ([`bcfaadd`](https://github.com/russmatney/clawe/commit/bcfaadd)) fix: don't crash on null duration - Russell Matney

  > nil-punning not yet pervasive in tick funcs.

- ([`a3eb1dc`](https://github.com/russmatney/clawe/commit/a3eb1dc)) clawe.deps: more workspaces (portal, cider, lila/lichess) - Russell Matney
- ([`230070f`](https://github.com/russmatney/clawe/commit/230070f)) feat: action improvements - Russell Matney

  > - Action colors via color-wheel
  > - Show more/less actions in list
  > - Sort actions by priority
  > - open-in-emacs uses emacs icon
  > - improved todo actions and topbar actions layouts


### 23 Aug 2022

- ([`cf4a9e9`](https://github.com/russmatney/clawe/commit/cf4a9e9)) fix: swap in single quotes in commit content - Russell Matney

  > ralphie.git was crashing when double quotes were used in commit
  > messages. A better option for dealing with this might be
  > encoding/decoding the content across the edn/read-string.

- ([`313486a`](https://github.com/russmatney/clawe/commit/313486a)) fix: restore initial file/directory in emacs/terminal toggle - Russell Matney

  > Another replace-val-with-current-workspace case - not sure i love this
  > pattern, but it works for now.

- ([`f1be509`](https://github.com/russmatney/clawe/commit/f1be509)) chore: misc ralphie clean up - Russell Matney

  > Drops unused nses and defs, some function renaming.

- ([`45d5450`](https://github.com/russmatney/clawe/commit/45d5450)) clawe.deps: instaparse workspace - Russell Matney
- ([`fa84076`](https://github.com/russmatney/clawe/commit/fa84076)) fix: awesome tag focus bugfix - Russell Matney

  > Was using the wrong key here.


### 21 Aug 2022

- ([`19f249e`](https://github.com/russmatney/clawe/commit/19f249e)) feat: add ingest buttons to events page - Russell Matney
- ([`6360716`](https://github.com/russmatney/clawe/commit/6360716)) fix: ensure timezone on timeline dates - Russell Matney
- ([`815723f`](https://github.com/russmatney/clawe/commit/815723f)) refactor: cleaner org-body impl - Russell Matney

  > prepping for handling more complex org bodies (src blocks)

- ([`5a2d021`](https://github.com/russmatney/clawe/commit/5a2d021)) feat: support no-sort in debug metadata comp - Russell Matney

### 17 Aug 2022

- ([`81fbfb1`](https://github.com/russmatney/clawe/commit/81fbfb1)) feat: org-file component improvements - Russell Matney

  > line-through on completed items, toggleable show-raw, fewer borders,
  > detecting empty bodies. pulls out some util.

- ([`8ec5c0b`](https://github.com/russmatney/clawe/commit/8ec5c0b)) refactor: clean up posts page layout - Russell Matney
- ([`f04f0ff`](https://github.com/russmatney/clawe/commit/f04f0ff)) fix: restore posts page - Russell Matney

  > Not yet showing full-items, but maybe we don't want to?

- ([`318c1fe`](https://github.com/russmatney/clawe/commit/318c1fe)) misc: tighter topbar components - Russell Matney
- ([`b1297b9`](https://github.com/russmatney/clawe/commit/b1297b9)) refactor: pull `dialog` comp impl out of screenshot - Russell Matney
- ([`e2f6b7f`](https://github.com/russmatney/clawe/commit/e2f6b7f)) feat: bar current-task walking queued items - Russell Matney
- ([`2bae160`](https://github.com/russmatney/clawe/commit/2bae160)) refactor: repo actions include commit ingestion - Russell Matney
- ([`39e17a5`](https://github.com/russmatney/clawe/commit/39e17a5)) feat: general handlers/->actions, tables actions column - Russell Matney

  > Adds a general handlers/->actions impl and an actions column to relevant
  > db tables. Every item in those tables can be 'deleted' this way, and
  > more actions per each item will be added as functionality grows.

- ([`1bd7416`](https://github.com/russmatney/clawe/commit/1bd7416)) fix: better inline handling for garden text-with-links - Russell Matney
- ([`9f22861`](https://github.com/russmatney/clawe/commit/9f22861)) chore: remove unused vars, requires - Russell Matney
- ([`24f0b37`](https://github.com/russmatney/clawe/commit/24f0b37)) feat: show more/less on tables - Russell Matney
- ([`4e7ab13`](https://github.com/russmatney/clawe/commit/4e7ab13)) misc: drop bad icon - text fallback is preferred - Russell Matney
- ([`7029acb`](https://github.com/russmatney/clawe/commit/7029acb)) fix: use lesser of n/count for initial page size - Russell Matney
- ([`68e684a`](https://github.com/russmatney/clawe/commit/68e684a)) feat: stickier floating/popups - Russell Matney

  > Not perfect, but makes more interactions possible for now.

- ([`115ad0b`](https://github.com/russmatney/clawe/commit/115ad0b)) feat: ingest garden button - Russell Matney
- ([`f626289`](https://github.com/russmatney/clawe/commit/f626289)) feat: show more/less in todo-list comp - Russell Matney
- ([`c1ffd68`](https://github.com/russmatney/clawe/commit/c1ffd68)) misc: todo layout, handlers rearrange, offset 0 - Russell Matney

  > Still an issue when interacting with nested popovers - can't seem to
  > fire the command before the popover disappears.

- ([`cff40ad`](https://github.com/russmatney/clawe/commit/cff40ad)) fix: tags correctly 'many', don't render a vector - Russell Matney

  > Confusing error when attempting to render a vector here - reported as a
  > crash when reset! was called, which is true. it was a crash while
  > rendering the group-by-tags table.


### 16 Aug 2022

- ([`cffdcbf`](https://github.com/russmatney/clawe/commit/cffdcbf)) feat: adding uuids when missing via actions - Russell Matney

  > Helps with name-changing workflow - add a uuid, now it'll lock in
  > whenever updates are made in org or the app.

- ([`2abe9e8`](https://github.com/russmatney/clawe/commit/2abe9e8)) feat: full-note-popover instead of todo-popover - Russell Matney

  > Re-using these comps all over! Can now purge files from the todos page
  > by way of the full-note popover.

- ([`3795af7`](https://github.com/russmatney/clawe/commit/3795af7)) feat: garden-file actions, purging whole org/source-files - Russell Matney
- ([`473ab99`](https://github.com/russmatney/clawe/commit/473ab99)) feat: pull out components/filter - Russell Matney

  > Pulls the todos filtering logic into a reusable use-filter hook, which
  > returns filtered items, filtered+grouped items, and a component for
  > manipulating the filters themselves.
  > 
  > This expects to be passed a set of filters and some defaults, which
  > should be malli-speced to provide warnings/make this more consumable.

- ([`f48cdda`](https://github.com/russmatney/clawe/commit/f48cdda)) feat: add support for tags to filters - Russell Matney

  > Tags 'explode' the values b/c their group-by returns a collection, not
  > just a single val - this adds support to the filters/group-bys to
  > support coll? based group-bys.

- ([`17eb4d6`](https://github.com/russmatney/clawe/commit/17eb4d6)) misc: ensure tags as a list - Russell Matney

  > Probably not necessary, but here we ensure tags are a list similarly to
  > urls.

- ([`4f2102c`](https://github.com/russmatney/clawe/commit/4f2102c)) misc: tmux/fire clients can be restarted via create-client - Russell Matney

  > kind of a nice thing - here the create-client rofi label is tweaked to
  > hint at that.

- ([`c3742c1`](https://github.com/russmatney/clawe/commit/c3742c1)) feat: display org tags - Russell Matney
- ([`90dc78e`](https://github.com/russmatney/clawe/commit/90dc78e)) feat: todo crud working via org-crud - Russell Matney

  > Also adding tags to items.

- ([`9eea51d`](https://github.com/russmatney/clawe/commit/9eea51d)) fix: consume schema on frontend as well - Russell Matney

  > Explains a few multi cardinality things i've seen lately.

- ([`64d6125`](https://github.com/russmatney/clawe/commit/64d6125)) feat: workspace def actions - Russell Matney
- ([`f7665c2`](https://github.com/russmatney/clawe/commit/f7665c2)) fix: wrap on-select in function - Russell Matney

  > Could just name the func here, but :shrug:


### 15 Aug 2022

- ([`ad22b5f`](https://github.com/russmatney/clawe/commit/ad22b5f)) feat: display queued tasks in bar, support retracting queued-at - Russell Matney
- ([`9d93696`](https://github.com/russmatney/clawe/commit/9d93696)) feat: queue todo, display in-progress todos - Russell Matney
- ([`f8a282f`](https://github.com/russmatney/clawe/commit/f8a282f)) wip: cancelling todos - Russell Matney

  > Org-crud needs some work before org-updates work, as well as
  > garden-ingestion. at the moment, updating items can have some
  > undesirable org content updates, like dropping unrecognized
  > statuses (DONE, for example), and duplicating content.
  > 
  > This is necessary to support resonable tagging/linking from the doctor
  > ui.

- ([`a78248e`](https://github.com/russmatney/clawe/commit/a78248e)) feat: delete todos from db - Russell Matney

  > org items get duped in the current setup, so deleting items is a useful
  > action right now.

- ([`ab1bda4`](https://github.com/russmatney/clawe/commit/ab1bda4)) fix: move popover comp to small text hover - Russell Matney
- ([`5f6d54a`](https://github.com/russmatney/clawe/commit/5f6d54a)) relayout: todos as cells instead of lines - Russell Matney

  > Also more gap- instead of space-

- ([`c833f2e`](https://github.com/russmatney/clawe/commit/c833f2e)) feat: re-layout todos pages - Russell Matney

  > Mostly moving from flex to grid, but adding debugging and improving
  > small details.

- ([`0c1e1eb`](https://github.com/russmatney/clawe/commit/0c1e1eb)) feat: use garden/link-text to unwrap roam ids - Russell Matney
- ([`88699cc`](https://github.com/russmatney/clawe/commit/88699cc)) wip: todos.hooks refactor, misc other touches - Russell Matney
- ([`d6e6aab`](https://github.com/russmatney/clawe/commit/d6e6aab)) feat: client-def->actions via rofi - Russell Matney
- ([`e682a41`](https://github.com/russmatney/clawe/commit/e682a41)) feat: doctor-be/fe client create configs - Russell Matney

  > Tmux fire already supports choosing a window/pane :woo:

- ([`abb8fa5`](https://github.com/russmatney/clawe/commit/abb8fa5)) feat: debug/ls supporting sort by key - Russell Matney
- ([`4a32abf`](https://github.com/russmatney/clawe/commit/4a32abf)) feat: client/workspace defs via rofi - Russell Matney
- ([`f745197`](https://github.com/russmatney/clawe/commit/f745197)) feat: clawe.debug/ls command - Russell Matney

### 12 Aug 2022

- ([`8da2268`](https://github.com/russmatney/clawe/commit/8da2268)) feat: client/workspaces defs via rofi - Russell Matney
- ([`ebdd8ee`](https://github.com/russmatney/clawe/commit/ebdd8ee)) feat: support setting wallpaper on osx - Russell Matney

### 11 Aug 2022

- ([`aa4665e`](https://github.com/russmatney/clawe/commit/aa4665e)) fix: ignore bar app when cleaning up workspaces - Russell Matney
- ([`9c8a160`](https://github.com/russmatney/clawe/commit/9c8a160)) fix: remove extra div wrappers - Russell Matney
- ([`8d46b69`](https://github.com/russmatney/clawe/commit/8d46b69)) conf: clawe.edn - topbar, messages, more repos - Russell Matney
- ([`4b5650c`](https://github.com/russmatney/clawe/commit/4b5650c)) wip: disable some non-osx topbar metadata - Russell Matney

  > These are impled via linux-y namespaces for now
  > 
  > coming soon: a ralphie osx reckoning

- ([`1fcc7fd`](https://github.com/russmatney/clawe/commit/1fcc7fd)) refactor: topbar refactored for osx - Russell Matney

  > Removes a bunch of hover handling and other things that are no longer
  > relevant. Moves flexbox usage to grid.

- ([`3e6fb74`](https://github.com/russmatney/clawe/commit/3e6fb74)) feat: support more topbar icons - Russell Matney
- ([`6b1e2c1`](https://github.com/russmatney/clawe/commit/6b1e2c1)) feat: allow updating doctor topbar on mac, from -x - Russell Matney

  > bb-cli can call any func! as long as it expects args.

- ([`5d1f355`](https://github.com/russmatney/clawe/commit/5d1f355)) feat: support toggle/find-client 'client-key' - Russell Matney
- ([`ecd7f6d`](https://github.com/russmatney/clawe/commit/ecd7f6d)) chore: misc logs/comments in debug prints - Russell Matney
- ([`4a59562`](https://github.com/russmatney/clawe/commit/4a59562)) feat: :workspace/focused attribute - Russell Matney
- ([`b6a26c6`](https://github.com/russmatney/clawe/commit/b6a26c6)) fix: misc tauri osx fixes/updates - Russell Matney
- ([`3342858`](https://github.com/russmatney/clawe/commit/3342858)) feat: move client+workspace to cljc - Russell Matney

  > So that we can use strip in cljs files

- ([`d31387c`](https://github.com/russmatney/clawe/commit/d31387c)) feat: debug helpers printing clients and wsps - Russell Matney

  > clojure.pprint/print-table is great!


### 10 Aug 2022

- ([`ade20ed`](https://github.com/russmatney/clawe/commit/ade20ed)) fix: resolve all clj-kondo warnings - Russell Matney
- ([`6fea0b6`](https://github.com/russmatney/clawe/commit/6fea0b6)) fix: make sure bindings are loaded when we reload - Russell Matney

  > Otherwise we write empty bindings to awesome + sxhkd and lose all control.

- ([`1cc6519`](https://github.com/russmatney/clawe/commit/1cc6519)) chore: clean up a bunch of todos - Russell Matney
- ([`0751e58`](https://github.com/russmatney/clawe/commit/0751e58)) fix: defkbd clj-kondo rules - Russell Matney
- ([`1bd041f`](https://github.com/russmatney/clawe/commit/1bd041f)) feat: org-roam linked content displaying via popovers - Russell Matney
- ([`8a355f4`](https://github.com/russmatney/clawe/commit/8a355f4)) feat: open-in-journal, move handlers to ui.handlers - Russell Matney

  > Now toggling the journal and opening arbitrary files on click.

- ([`ab63219`](https://github.com/russmatney/clawe/commit/ab63219)) feat: ralphie.emacs/open-in-emacs supporting a specified frame - Russell Matney

  > Can now open files in arbitrary frames!

- ([`69cf654`](https://github.com/russmatney/clawe/commit/69cf654)) feat: move show-client to wm/ ns, support client-key in some wm funcs - Russell Matney
- ([`8e3577a`](https://github.com/russmatney/clawe/commit/8e3577a)) refactor: break table-defs out of table comp - Russell Matney

  > We can now opt-in to multiple types of tables per :doctor/type more
  > easily, tho at the cost of the current fallback-table feat... which
  > could be restored and may run over all the 'other' entities.

- ([`5c37747`](https://github.com/russmatney/clawe/commit/5c37747)) refactor: move journal feats to pulling from db - Russell Matney

  > Drops journal hook, combines frontend db with handler for fetching full
  > org-items.

- ([`f459b3d`](https://github.com/russmatney/clawe/commit/f459b3d)) misc: reingestion comment code, other fixes/helpers - Russell Matney
- ([`1c7ad5f`](https://github.com/russmatney/clawe/commit/1c7ad5f)) feat: use tables in event clusters - Russell Matney

  > Somewhat more readable for not much effort.

- ([`94f1d73`](https://github.com/russmatney/clawe/commit/94f1d73)) feat: pre-calced :event/timestamp - Russell Matney

  > and misc date add-tz hacks to make the page work again.
  > 
  > Feels like the clustering algo needs to be tweakable in the UI - there's
  > a 9am -> 1am session right now that i just don't want to buy.
  > 
  > maybe it's the journal inputs via drafts that have held it over my down
  > time this evening?

- ([`dfa8834`](https://github.com/russmatney/clawe/commit/dfa8834)) feat: expand txs with :event/timestamp - Russell Matney

  > Adds a db listener that runs item->event-timestamp on transacted
  > entities, transacting an :event/timestamp attr if one can be determined.


### 9 Aug 2022

- ([`e8ba4d1`](https://github.com/russmatney/clawe/commit/e8ba4d1)) feat: distinct key counts on db records - Russell Matney
- ([`b7380cf`](https://github.com/russmatney/clawe/commit/b7380cf)) chore: drop hooks.garden, pull notes from db - Russell Matney
- ([`1ffa89c`](https://github.com/russmatney/clawe/commit/1ffa89c)) chore: misc deadcode cleanup - Russell Matney
- ([`db1850e`](https://github.com/russmatney/clawe/commit/db1850e)) feat: wallpaper ingestion, wallpapers via ui.db - Russell Matney

  > Drops wallpapers hooks, api nspcs

- ([`e56e7ad`](https://github.com/russmatney/clawe/commit/e56e7ad)) feat: db.core basic retract func - Russell Matney
- ([`fe71272`](https://github.com/russmatney/clawe/commit/fe71272)) refactor: screenshots page consuming from db - Russell Matney

  > Drops api.screenshots and hooks.screenshots.

- ([`962ca59`](https://github.com/russmatney/clawe/commit/962ca59)) feat: introduce doctor.ui.db ns - Russell Matney

  > Need somewhere to dry up frontend db usage.

- ([`f38e637`](https://github.com/russmatney/clawe/commit/f38e637)) chore: drop events, repos, commits hooks/apis - Russell Matney

  > All dead now that we just pull from the frontend datastore.

- ([`3b9b0a0`](https://github.com/russmatney/clawe/commit/3b9b0a0)) feat: move events page to consuming from db - Russell Matney

  > Sort-of painless transition. Sort of.

- ([`dd713e4`](https://github.com/russmatney/clawe/commit/dd713e4)) fix: padding in db page table/button layout - Russell Matney
- ([`cb263c6`](https://github.com/russmatney/clawe/commit/cb263c6)) feat: ingest commits per repo, commits table columns - Russell Matney

  > Repos merging in commits, commits mergin in repos, via shared datascript
  > db.
  > 
  > Breaks some pieces of components.git out for re-use in the tables.

- ([`096b719`](https://github.com/russmatney/clawe/commit/096b719)) feat: support dates.tick in cljs - Russell Matney

  > Nice little cljc try catch trick here.

- ([`9e9fd0a`](https://github.com/russmatney/clawe/commit/9e9fd0a)) feat: frontend sharing db from root view component - Russell Matney
- ([`fc1c22b`](https://github.com/russmatney/clawe/commit/fc1c22b)) refactor: remove table defs from table component - Russell Matney

  > Table component now has no dependencies! woo!

- ([`ce88c26`](https://github.com/russmatney/clawe/commit/ce88c26)) refactor: move defthing.db to db.core - Russell Matney

  > Moves the db-handling in defthing into a new db namespace.

- ([`df1ab52`](https://github.com/russmatney/clawe/commit/df1ab52)) refactor: move clawe.config/*config* to systemic system - Russell Matney

  > I'd moved to an atom here when i misread systemic's ability to have an
  > atom as a systemic value. It was nice to not need the systemic/start! in
  > every function, but w/e, it's nice to have systems too.

- ([`24e6b2b`](https://github.com/russmatney/clawe/commit/24e6b2b)) fix: include `:include-clients` in fetch-workspace - Russell Matney

  > wm tests caught this one. Huzzah!

- ([`b182320`](https://github.com/russmatney/clawe/commit/b182320)) feat: basic commit table - Russell Matney
- ([`0eb87bd`](https://github.com/russmatney/clawe/commit/0eb87bd)) fix: some dead func names - Russell Matney

### 8 Aug 2022

- ([`7edce0a`](https://github.com/russmatney/clawe/commit/7edce0a)) feat: basic commit ingestion per repo - Russell Matney

  > Some of the backend plus the fallback frontend table.
  > No button yet.

- ([`64caa31`](https://github.com/russmatney/clawe/commit/64caa31)) feat: ingest and table for clawe repos - Russell Matney
- ([`d0519bd`](https://github.com/russmatney/clawe/commit/d0519bd)) feat: button to clear lichess cache - Russell Matney
- ([`1dfb79c`](https://github.com/russmatney/clawe/commit/1dfb79c)) feat: lichess games table layout - Russell Matney
- ([`f44c0b1`](https://github.com/russmatney/clawe/commit/f44c0b1)) feat: button and ingestion for lichess games - Russell Matney
- ([`9d3f831`](https://github.com/russmatney/clawe/commit/9d3f831)) feat: helpful fallback table impl - Russell Matney
- ([`8874e39`](https://github.com/russmatney/clawe/commit/8874e39)) feat: ingesting screenshots via button on frontend - Russell Matney

  > Routes screenshots into the db, then displays them in a table on the db page.

- ([`9c74943`](https://github.com/russmatney/clawe/commit/9c74943)) feat: update schema on *conn* startup - Russell Matney
- ([`3921b9d`](https://github.com/russmatney/clawe/commit/3921b9d)) feat: latest org items in a table, org-body in a popover - Russell Matney
- ([`368e811`](https://github.com/russmatney/clawe/commit/368e811)) feat: wallpapers displaying, popover for setting a new one - Russell Matney
- ([`208bff4`](https://github.com/russmatney/clawe/commit/208bff4)) feat: table component, exposing doctor entity type counts - Russell Matney
- ([`9c683d8`](https://github.com/russmatney/clawe/commit/9c683d8)) conf: add steam workspace - Russell Matney
- ([`65cef24`](https://github.com/russmatney/clawe/commit/65cef24)) chore: drop wm awesome.rules dep - Russell Matney
- ([`3e9d47a`](https://github.com/russmatney/clawe/commit/3e9d47a)) feat: streaming data on page-load - Russell Matney

  > Rather than loading all of org at once, we stream it at some per-datom
  > batch sizes.


### 7 Aug 2022

- ([`45f0317`](https://github.com/russmatney/clawe/commit/45f0317)) feat: live-updating org content on pages/db - Russell Matney
- ([`e35b429`](https://github.com/russmatney/clawe/commit/e35b429)) wip: db page queries - Russell Matney
- ([`fedf669`](https://github.com/russmatney/clawe/commit/fedf669)) feat: datascript-transit handlers, displaying db data on the fe - Russell Matney
- ([`48e0318`](https://github.com/russmatney/clawe/commit/48e0318)) feat: datascript db hook and backend listener - Russell Matney

  > Pushing txs on the backend to the frontend

- ([`c6f08c7`](https://github.com/russmatney/clawe/commit/c6f08c7)) chore: drop defworkspace usage - Russell Matney
- ([`f589570`](https://github.com/russmatney/clawe/commit/f589570)) refactor: drop datalevin, use datascript - Russell Matney

### 6 Aug 2022

- ([`fb9f857`](https://github.com/russmatney/clawe/commit/fb9f857)) clawe.edn: more repo wsps - Russell Matney
- ([`c814c76`](https://github.com/russmatney/clawe/commit/c814c76)) fix: restart callable via -x - Russell Matney
- ([`1da9a35`](https://github.com/russmatney/clawe/commit/1da9a35)) defs: godot and dev web better wsp matching - Russell Matney
- ([`e94bd86`](https://github.com/russmatney/clawe/commit/e94bd86)) fix: consistent osx workspace titles - Russell Matney

  > osx emacs adds gibberish to the title bar, including a fancy emdash.


### 5 Aug 2022

- ([`e2a9c97`](https://github.com/russmatney/clawe/commit/e2a9c97)) refactor: general clean up - Russell Matney

  > The awm rules and removing the uberjar logic are next

- ([`8dc3c4f`](https://github.com/russmatney/clawe/commit/8dc3c4f)) fix: always ensure-workspace when moving to it - Russell Matney

  > No reason to no-op here, let's just drop the option and always create
  > the workspace when we move a client to it.

- ([`e6bca7b`](https://github.com/russmatney/clawe/commit/e6bca7b)) fix: upddate :client/create forms - Russell Matney

  > strings get exec-ed directly now.

- ([`89437f5`](https://github.com/russmatney/clawe/commit/89437f5)) fix: ralphie.browser/open with no url on linux - Russell Matney
- ([`0af885d`](https://github.com/russmatney/clawe/commit/0af885d)) fix: restore clients to topbar - Russell Matney
- ([`5186a6f`](https://github.com/russmatney/clawe/commit/5186a6f)) refactor: basic rules rewrite - Russell Matney

  > Clients are moved to the workspace dictated by
  > wm/client->workspace-title, which check for :client/workspace-title,
  > then :client/window-title.

- ([`4109c71`](https://github.com/russmatney/clawe/commit/4109c71)) feat: ralphie.awesome/fetch-tags supporting :only-current, :include-clients - Russell Matney

  > Rather than fetch all the awesome tags and clients every time,
  > awm/fetch-tags now supports :include-clients and :only-current to limit
  > the data fetched.
  > 
  > Includes some direction toward malli schemas and some attach-clients
  > helpers in the clawe.awesome implementation.

- ([`65889cd`](https://github.com/russmatney/clawe/commit/65889cd)) refactor: move awesome.fnl tests - Russell Matney
- ([`b62f1b7`](https://github.com/russmatney/clawe/commit/b62f1b7)) refactor: move `fnl` macro magic into awesome.fnl - Russell Matney

  > Tho, maybe it should live outside of awesome? eh, it's pretty dependent
  > at the moment.

- ([`b7a827b`](https://github.com/russmatney/clawe/commit/b7a827b)) perf: dry up client and wsp fetches on toggle path - Russell Matney

  > Well that was easy!

- ([`b56a485`](https://github.com/russmatney/clawe/commit/b56a485)) test: coverage for clawe.client.create - Russell Matney
- ([`c27a7ec`](https://github.com/russmatney/clawe/commit/c27a7ec)) refactor: rename open to create-client, move to namespace - Russell Matney

  > Also refactors toggle a bit to make its process explicit.

- ([`d85b605`](https://github.com/russmatney/clawe/commit/d85b605)) test: schema tests for config client/workspace defs - Russell Matney
- ([`3085c6d`](https://github.com/russmatney/clawe/commit/3085c6d)) workspace/defs: couple more repos - Russell Matney
- ([`5df3ed1`](https://github.com/russmatney/clawe/commit/5df3ed1)) fix: bury clients when focusing one - Russell Matney
- ([`ed0f6f7`](https://github.com/russmatney/clawe/commit/ed0f6f7)) refactor: drop defs/workspaces completely - Russell Matney

  > now supporting all clients in resources/clawe.edn - data-driven ftw!

- ([`900f156`](https://github.com/russmatney/clawe/commit/900f156)) feat: client/open supporting basic exec/cmd - Russell Matney
- ([`2876638`](https://github.com/russmatney/clawe/commit/2876638)) fix: ralphie.browser/open-dev on linux - Russell Matney
- ([`a82acd1`](https://github.com/russmatney/clawe/commit/a82acd1)) feat: toggle term/emacs working again, and data-driven - Russell Matney

  > A bit complicated - here we extend client/match to support a
  > use-workspace-title style match, so that clients can be toggled per
  > workspace (if they opt-in). This match requires passing a workspace
  > title into client/match - i.e. what is the in-context workspace? A
  > question raise is what workspace to pass in wm/merge-client-defs - do
  > clients know their workspace name already? if they did, it might be
  > simple to use at that client/match call-site. For now we merge it in
  > toggle/find-client.

- ([`014f546`](https://github.com/russmatney/clawe/commit/014f546)) feat: kbds using bb -x for toggles, m-x - Russell Matney

  > Also a clawe.edn update. Note that adding workspaces interactively
  > rewrites this file, which drops comments and does not maintain order in
  > the defs maps.

- ([`afaec29`](https://github.com/russmatney/clawe/commit/afaec29)) feat: git-clone suggestion also creates workspace - Russell Matney

  > Have wanted this feature forever! Should probably find a good way to
  > test it (and create/open workspace in general).

- ([`b962cea`](https://github.com/russmatney/clawe/commit/b962cea)) feat: lower-case app-name comparisons - Russell Matney

  > No more of this exact-case matching on whatever the app dev set the
  > class name to.

- ([`4cdec1f`](https://github.com/russmatney/clawe/commit/4cdec1f)) fix: silly safari bug. - Russell Matney

  > i ought to read more --help text, i think this was originally a quick
  > copy-pasta. Have been creating extra safari instances forever, not sure
  > what that was about, but this appears to be why. hopefully my
  > url-reading will now get more consistent too.

- ([`a0a5a82`](https://github.com/russmatney/clawe/commit/a0a5a82)) fix: auto-resolve in both forms - Russell Matney
- ([`2909fed`](https://github.com/russmatney/clawe/commit/2909fed)) feat: remove dead toggle code, auto-require :open/cmd - Russell Matney
- ([`6ea8423`](https://github.com/russmatney/clawe/commit/6ea8423)) feat: impl open-client, add to client-defs - Russell Matney

  > Well that was easy.

- ([`9da1948`](https://github.com/russmatney/clawe/commit/9da1948)) fix: better logging, scratchpads working, dupe key fix - Russell Matney

  > Could maybe use a dupe key clj-kondo/malli warning for the silly :map
  > key dupe i just ran into. But, the tests weren't passing either :shrug:

- ([`0b90413`](https://github.com/russmatney/clawe/commit/0b90413)) feat: toggle journal works! and others too! - Russell Matney

  > Not too bad, debugging wise either. Especially fun is testing bindings
  > while editing clawe.edn.


### 4 Aug 2022

- ([`8dca7fb`](https://github.com/russmatney/clawe/commit/8dca7fb)) wip: stub for `toggle` function - Russell Matney
- ([`ddc060f`](https://github.com/russmatney/clawe/commit/ddc060f)) feat: determine-toggle-action and tests - Russell Matney

  > Returning an event-y signal instead of performing side-effects. What a world!

- ([`15e6482`](https://github.com/russmatney/clawe/commit/15e6482)) feat: toggle/client-in-current-workspace? and tests - Russell Matney
- ([`2b08558`](https://github.com/russmatney/clawe/commit/2b08558)) refactor: toggle-app test now using specmonstah - Russell Matney

  > Not quite sure this is better - not really using relations, so not as
  > much of specmonstah to take advantage of in this case.

- ([`c88b40e`](https://github.com/russmatney/clawe/commit/c88b40e)) wip: begin toggle testing - Russell Matney

  > Basic test for now - next is refactoring to use specmonstah and re-use
  > the malli schemas.

- ([`73a954a`](https://github.com/russmatney/clawe/commit/73a954a)) feat: wm/workspace fetchers merging client defs - Russell Matney
- ([`874ded2`](https://github.com/russmatney/clawe/commit/874ded2)) fix: client/match? bug, more testing, misc other clean up - Russell Matney

  > workspace/find-matching-client, and other helpers to clean up the test
  > impl/provide some nicer apis.

- ([`6ec1103`](https://github.com/russmatney/clawe/commit/6ec1103)) feat: client/match? helper and tests - Russell Matney

### 3 Aug 2022

- ([`cb06eea`](https://github.com/russmatney/clawe/commit/cb06eea)) misc: println and test clean up - Russell Matney
- ([`4fc8b8a`](https://github.com/russmatney/clawe/commit/4fc8b8a)) feat: couple more assertions for :include-clients cases - Russell Matney
- ([`1fb0244`](https://github.com/russmatney/clawe/commit/1fb0244)) fixes: wm-test passing in yabai! - Russell Matney

  > Handful of little issues and missing yabai claweWM impl.
  > 
  > Includes a wait-for helper that re-runs an expression until it returns
  > truthy, which helps deal with yabai returning before osx is finished
  > switching workspaces or moving clients around.
  > 
  > The integration tests can take a bit on osx, something like 10 seconds
  > or so.

- ([`b77afd7`](https://github.com/russmatney/clawe/commit/b77afd7)) test: move client to workspace - Russell Matney

  > Getting some nice re-usable functions out of this - funny how tests
  > demand a nice api to stay clean.

- ([`270709d`](https://github.com/russmatney/clawe/commit/270709d)) test: focus-wsp, focus-client, drag-workspace tests - Russell Matney

  > Finding more bugs, getting more stable

- ([`c4205d5`](https://github.com/russmatney/clawe/commit/c4205d5)) test: create-and-delete-workspace test - Russell Matney

  > Pretty happy with where this is landing!
  > 
  > Integration tests at the wm-protocol level kick-ass.
  > 
  > Fixes fetch-workspace returning a weird-default-map when no wsp exists.

- ([`6d57956`](https://github.com/russmatney/clawe/commit/6d57956)) feat: wm/active-clients test - Russell Matney
- ([`5ba465d`](https://github.com/russmatney/clawe/commit/5ba465d)) feat: restore wm/workspace-defs test - Russell Matney
- ([`b5ccfa8`](https://github.com/russmatney/clawe/commit/b5ccfa8)) refactor: moves the workspace logic into wm itself - Russell Matney

  > Rather than use `clawe.workspace` as an extra ns layer, this moves the
  > `clawe.workspace` helpers and public funcs into `clawe.wm`, and moves to
  > consuming `clawe.wm` everywhere.
  > 
  > The `clawe.wm` functions were really easy to reach for but as impled
  > were a weird extra layer. This is much nicer to consume.
  > 
  > This also introduces tests for the `wm` layer itself - these are kind of
  > fun b/c they run with whatever the current window-manager is, so it'll
  > simple to use them to debug/automate testing for whatever silly things
  > we hit.
  > 
  > Includes some malli test helpers.

- ([`e3d894d`](https://github.com/russmatney/clawe/commit/e3d894d)) feat: support inlined sxhkd commands - Russell Matney

  > Similar to the inlined-awm bindings, these are sxhkd bindings that get
  > executed directly (rather than run the whole clawe uberjar just to do
  > some shell command).
  > 
  > Moves a few bindings to using `bb -x` to fire the commands, which is
  > slightly quicker, execution wise. Note that this style uses the live
  > version of the code, whereas the uberjar is somewhat 'stable' or
  > 'outdated', depending on your point of view. This means bindings can
  > fail if clawe is currently being refactored and in a broken state, which
  > might be annoying. It also means 'fixes' work right away, which is a
  > nice feedback loop. Maybe eventually we'll reach for some way to toggle
  > between these types of interactions - the stable and edge versions.

- ([`418b59b`](https://github.com/russmatney/clawe/commit/418b59b)) fix: try-catch ralphie.emacs/open - Russell Matney

  > Nicer to get notified about a file not existing than silent background
  > failures.

- ([`394c8e1`](https://github.com/russmatney/clawe/commit/394c8e1)) feat: pretty-print clawe.edn - Russell Matney

  > Makes it much easier to work with this file by hand.
  > 
  > Now, to remove those pesky commas!


### 2 Aug 2022

- ([`c7fe117`](https://github.com/russmatney/clawe/commit/c7fe117)) fix: clawe clean up rules working on osx - Russell Matney

  > No longer smashing the bottom client.

- ([`cb49400`](https://github.com/russmatney/clawe/commit/cb49400)) refactor: remove clawe.workspaces, clean up *config* atom shift - Russell Matney

  > clawe.config/*config* is now just an atom - the system was overkill and
  > didn't support a restart without updating dependent systems/killing the repl.

- ([`1aa0ec8`](https://github.com/russmatney/clawe/commit/1aa0ec8)) feat: open workspace implicitly installs - Russell Matney

  > Open workspace now scans for repos based on your root-repo config, and
  > fallsback to selecting git users and repos arbitrarily. Once selected,
  > the repo is 'installed', i.e. merged into the local :workspace/defs in
  > resources/clawe.edn.
  > 
  > Quite nice! Now to include the clone-from-open-tabs flow...

- ([`3427fe8`](https://github.com/russmatney/clawe/commit/3427fe8)) refactor: pull workspaces rules funcs into rules - Russell Matney

  > Workspaces no longer yabai/awm dependent

- ([`3ac1e72`](https://github.com/russmatney/clawe/commit/3ac1e72)) feat: drop merge-awm-tags, wm agnostic wsp sort - Russell Matney
- ([`462333e`](https://github.com/russmatney/clawe/commit/462333e)) fix: restore and clean up topbar - Russell Matney
- ([`b764266`](https://github.com/russmatney/clawe/commit/b764266)) fix: expand initial file in emacs/open - Russell Matney
- ([`d30b291`](https://github.com/russmatney/clawe/commit/d30b291)) refactor: move off clawe.scratchpad completely - Russell Matney
- ([`9654bf5`](https://github.com/russmatney/clawe/commit/9654bf5)) refactor: clawe.toggle no longer awm/yabai dependent - Russell Matney

  > Moves the rest of the toggle logic behind the wm.protocol

- ([`ff69ff7`](https://github.com/russmatney/clawe/commit/ff69ff7)) feat: awesome -all-clients impl - Russell Matney
- ([`07654fb`](https://github.com/russmatney/clawe/commit/07654fb)) feat: impl yabai side of ClaweWM protocol - Russell Matney

  > ports clawe/toggle towards using the protocol funcs, drops some newly
  > dead code.


### 1 Aug 2022

- ([`9dd5bb2`](https://github.com/russmatney/clawe/commit/9dd5bb2)) chore: drop merge-awm-tags - Russell Matney

  > This is equivalent to wsp/all-active

- ([`dbfbe39`](https://github.com/russmatney/clawe/commit/dbfbe39)) refactor: pull protocol, awm, yabai impls out - Russell Matney
- ([`77b1f02`](https://github.com/russmatney/clawe/commit/77b1f02)) refactor: remove workspaces/all-workspaces, misc rules cleanup - Russell Matney
- ([`a010bed`](https://github.com/russmatney/clawe/commit/a010bed)) feat: consuming new workspace/current func - Russell Matney
- ([`e1d383c`](https://github.com/russmatney/clawe/commit/e1d383c)) feat: initial workspace/current impl and tests - Russell Matney
- ([`0a55a31`](https://github.com/russmatney/clawe/commit/0a55a31)) wip: new workspace ns, begin ClaweWM protocol - Russell Matney
- ([`d9e07c7`](https://github.com/russmatney/clawe/commit/d9e07c7)) misc: clean up - Russell Matney
- ([`d2a3418`](https://github.com/russmatney/clawe/commit/d2a3418)) fix: restore toggle emacs, terminal kbd - Russell Matney
- ([`ff10c98`](https://github.com/russmatney/clawe/commit/ff10c98)) refactor: move to `dbs` dir in repo - Russell Matney

### 31 Jul 2022

- ([`a7a0796`](https://github.com/russmatney/clawe/commit/a7a0796)) feat: completed clawe.toggle refactor - Russell Matney

  > Now supporting emacs/terminal workspace-dependent use-cases.

- ([`fbacaff`](https://github.com/russmatney/clawe/commit/fbacaff)) feat: dry up clawe.toggle on osx - Russell Matney

  > Consumed as just a bb -x clawe.toggle/toggle-app now:
  > 
  > cmd - u : bb --config ~/russmatney/clawe/bb.edn -x clawe.toggle/toggle-app --wsp journal --title journal --app Emacs
  > cmd - t : bb --config ~/russmatney/clawe/bb.edn -x clawe.toggle/toggle-app --wsp web --app Safari
  > cmd - b : bb --config ~/russmatney/clawe/bb.edn -x clawe.toggle/toggle-app --wsp devweb --app 'Firefox Developer Edition'
  > cmd - s : bb --config ~/russmatney/clawe/bb.edn -x clawe.toggle/toggle-app --wsp spotify --app Spotify
  > cmd - e : bb --config ~/russmatney/clawe/bb.edn -x clawe.toggle/toggle-app --wsp aseprite --app Aseprite
  > cmd - a : bb --config ~/russmatney/clawe/bb.edn -x clawe.toggle/toggle-app --wsp slack --app Slack
  > cmd - m : bb --config ~/russmatney/clawe/bb.edn -x clawe.toggle/toggle-app --wsp messages --app Messages
  > cmd - g : bb --config ~/russmatney/clawe/bb.edn -x clawe.toggle/toggle-app --wsp godot --app Godot

- ([`e0ae6a3`](https://github.com/russmatney/clawe/commit/e0ae6a3)) wip: defclient, maybe? - Russell Matney
- ([`7d00eb3`](https://github.com/russmatney/clawe/commit/7d00eb3)) refactor: consolidate on clawe.client - Russell Matney

  > And remove even more dead funcs/workspaces

- ([`91b4f3c`](https://github.com/russmatney/clawe/commit/91b4f3c)) feat: consolidate clawe.restart and ralphie.util - Russell Matney
- ([`42893c2`](https://github.com/russmatney/clawe/commit/42893c2)) refactor: move wallpapers, screenshots out, kill more dead nses - Russell Matney
- ([`9bb0662`](https://github.com/russmatney/clawe/commit/9bb0662)) refactor: pull clawe.git to git.core - Russell Matney

  > Also drop unused clawe.dwim ns

- ([`6c377ae`](https://github.com/russmatney/clawe/commit/6c377ae)) refactor: clean up doctor api pings - Russell Matney
- ([`843f741`](https://github.com/russmatney/clawe/commit/843f741)) chore: remove workrave - Russell Matney

  > Haven't used this for a while!

- ([`987b51b`](https://github.com/russmatney/clawe/commit/987b51b)) refactor: clean up update-topbar in clawe - Russell Matney

  > Intro clawe config, use clawe.doctor as doctor api interface.

- ([`5e79399`](https://github.com/russmatney/clawe/commit/5e79399)) fix: use #zsh/expand in resource, drop hack - Russell Matney
- ([`666dd86`](https://github.com/russmatney/clawe/commit/666dd86)) feat: zsh/expand reader macro - Russell Matney

  > Kind of cool! Can eval at read-time, which saves a function call, and
  > might be fun in a resource/*.edn. Not sure if this forces more eval than
  > desired when running `clawe` - maybe things are faster as bb.edn tasks
  > or clojure-cli commands after all.

- ([`338d60f`](https://github.com/russmatney/clawe/commit/338d60f)) fix: break the listener<>server dependency - Russell Matney

  > The listener wants to start when the server does, but we don't want the
  > server to restart when we sys/restart! the listener


### 30 Jul 2022

- ([`63e8289`](https://github.com/russmatney/clawe/commit/63e8289)) wip: repos sync - Russell Matney
- ([`6a503ff`](https://github.com/russmatney/clawe/commit/6a503ff)) wip: a mess of db connection debugging/fixing - Russell Matney
- ([`25eedf7`](https://github.com/russmatney/clawe/commit/25eedf7)) fixes: db test isolation, scratchpad/wallpaper db cleanup - Russell Matney

  > DB items were clobbering eachother... hopefully just a weird state

- ([`86e82f6`](https://github.com/russmatney/clawe/commit/86e82f6)) feat: expo db schema and clear helpers - Russell Matney
- ([`ac82e67`](https://github.com/russmatney/clawe/commit/ac82e67)) wip: querying org notes with filename regex - Russell Matney
- ([`8fe7056`](https://github.com/russmatney/clawe/commit/8fe7056)) feat: server starting watchers/listeners - Russell Matney
- ([`781f065`](https://github.com/russmatney/clawe/commit/781f065)) misc: remove some db scratchpad usage - Russell Matney

  > This seems pretty busted.

- ([`115739a`](https://github.com/russmatney/clawe/commit/115739a)) feat: garden file watcher via juxt/dirwatch and systemic - Russell Matney
- ([`148e624`](https://github.com/russmatney/clawe/commit/148e624)) feat: expo-db writing posts on garden-note transaction - Russell Matney

  > Nearly all wired up!

- ([`658edcd`](https://github.com/russmatney/clawe/commit/658edcd)) refactor: misc expo, defthing db clean up - Russell Matney

  > Now using datalevin.core instead of the datalevin pod when in :clj, so
  > we can share the connection and make listeners work.


### 29 Jul 2022

- ([`e7bd304`](https://github.com/russmatney/clawe/commit/e7bd304)) expo: misc query toying - Russell Matney
- ([`9ef5c70`](https://github.com/russmatney/clawe/commit/9ef5c70)) garden: misc fixes/tweaks, ingesting new org fields - Russell Matney
- ([`7ef755a`](https://github.com/russmatney/clawe/commit/7ef755a)) fix: restore unit tests - Russell Matney

  > More deps.edn vs bb.edn overlap

- ([`9e2fb39`](https://github.com/russmatney/clawe/commit/9e2fb39)) fix: create missing sxhkd session if it doesn't exist - Russell Matney

  > Not perfect - i'd hoped to avoid this to keep keybindings closer to the
  > metal, speed-wise. It could probably be sped up, but this isn't the slow
  > part for now, and 'fast' keybindings can be done via awm/fnl anyway.
  > 
  > The main win is keybindings should now work after login, without
  > requiring a `clawe reload`. imagine that!

- ([`722163d`](https://github.com/russmatney/clawe/commit/722163d)) fix: more forgiving clawe restart behavior - Russell Matney

  > Fire `clawe reload` even if unit tests fail. This is sort of fine, b/c
  > the old uberjar will be used, which was presumably built with passing
  > unit tests...
  > 
  > The real issue is before sxhkd and tmux are running ok, i have no
  > keybindings - usually cmd+r restores all that, but if tests are failing
  > b/c of a bad db config or some other thing, getting to a working wm
  > requires tty-hopping to disable unit tests and eventually get a
  > successful clawe-reload to fire :/


### 28 Jul 2022

- ([`50d366f`](https://github.com/russmatney/clawe/commit/50d366f)) wip: ingesting new org fields - Russell Matney

  > links and parents, new fallback-ids.
  > 
  > apparently datalevin is a no-go on osx except through docker, so here we
  > switch to that.

- ([`77b849d`](https://github.com/russmatney/clawe/commit/77b849d)) feat: include promesa clj-kondo config - Russell Matney

### 27 Jul 2022

- ([`80e43b6`](https://github.com/russmatney/clawe/commit/80e43b6)) wip: not sure why these aren't running - Russell Matney

  > but here's a doc string.

- ([`2ee3201`](https://github.com/russmatney/clawe/commit/2ee3201)) feat: first posts hitting expo's homepage! - Russell Matney

  > Pulls org-items from the defthing.db (ingested via garden.db), writes
  > them to the datascript db in expo.core, writes that db to disk, reads
  > and renders that data in expo's frontend. The most overengineering blog
  > in the world! Two databases! No backend! WHY?

- ([`9a2b674`](https://github.com/russmatney/clawe/commit/9a2b674)) feat: ingesting all org notes into datalevin - Russell Matney

  > Couple of parse errors throw some annoying errors, but there's a
  > try/catch for it at least - malformed notes should be better dealt with
  > in org-crud. (extra spaces before :PROPERTIES: buckets, etc.)

- ([`bcd1340`](https://github.com/russmatney/clawe/commit/bcd1340)) feat: db supporting sets (and org-tags) - Russell Matney
- ([`779ac73`](https://github.com/russmatney/clawe/commit/779ac73)) feat: pull db path from config system - Russell Matney
- ([`dfc16d3`](https://github.com/russmatney/clawe/commit/dfc16d3)) fix: convert string ids to uuids - Russell Matney

  > Really should fix this upstream in org-crud.

- ([`a3e19cd`](https://github.com/russmatney/clawe/commit/a3e19cd)) refactor: datalevin conn as a systemic system, datalevin pod via bb.edn :pods - Russell Matney

  > Moves to the newer bb.edn `:pods {}` map for including pods (rather than
  > the inline: `load-pods` fn.)
  > 
  > Refactors the defthing.db datalevin usage to create and re-use a
  > connection rather than opening/closing on every query and transaction.
  > Should have done this ages ago!
  > 
  > Adds systemic and wing to the bb.edn deps, so that the bb test runner
  > can use them.

- ([`a80b79d`](https://github.com/russmatney/clawe/commit/a80b79d)) wip: should be transacting org-items - Russell Matney

  > Transacting seems to be locking up somehow... :/

- ([`add764c`](https://github.com/russmatney/clawe/commit/add764c)) feat: garden.db with ->org-db-item helper - Russell Matney
- ([`4be364c`](https://github.com/russmatney/clawe/commit/4be364c)) refactor: all-garden-notes helpers - Russell Matney

  > Landing on some solid org-item fetchers. Here we list only a few
  > ~/todo/<files>, as I don't care for the rest entering into clawe's
  > notion of a garden.
  > 
  > Some perf opportunity here for speeding up the zsh expansion, but
  > ultimately this is just ingesting into the db - the perf for this will
  > likely never be felt.

- ([`1da5764`](https://github.com/russmatney/clawe/commit/1da5764)) refactor: api/todos org items derived from garden-notes - Russell Matney
- ([`a6037ac`](https://github.com/russmatney/clawe/commit/a6037ac)) feat: building up to useful paths -> garden-note functions - Russell Matney
- ([`bfa6a30`](https://github.com/russmatney/clawe/commit/bfa6a30)) refactor: date, org-path helpers much more reusable - Russell Matney

  > Pulls the date helpers out of api/todos and into dates/tick, refactors
  > and moves the org-path helpers into garden.core.

- ([`a2e2390`](https://github.com/russmatney/clawe/commit/a2e2390)) refactor: pull time-ago-ms into dates.tick helper - Russell Matney
- ([`1b5ac04`](https://github.com/russmatney/clawe/commit/1b5ac04)) fix: restore unit tests, fix some aliases - Russell Matney

  > moving away from non-aliased, even when the actual namespace is used
  > verbatim.

- ([`65b4524`](https://github.com/russmatney/clawe/commit/65b4524)) fix: add `systemic/start!` back to auto-require hook - Russell Matney

  > Need to make sure the systemic systems are alive before using them,
  > otherwise we get obscure websocket `nil`-does-not-impl-async bugs.


### 26 Jul 2022

- ([`5f445a6`](https://github.com/russmatney/clawe/commit/5f445a6)) feat: doc/task for expo deploy - Russell Matney
- ([`237cd5b`](https://github.com/russmatney/clawe/commit/237cd5b)) feat: pull and query examples working - Russell Matney

  > huzzah! the fun begins

- ([`3f08f4c`](https://github.com/russmatney/clawe/commit/3f08f4c)) wip: entity, query attempt - Russell Matney

  > Not quiet working query example

- ([`be99559`](https://github.com/russmatney/clawe/commit/be99559)) feat: returning 'working' db/conns - Russell Matney

  > Not sure why the full entity isn't showing up here... maybe it needs a
  > schema?

- ([`e908ca6`](https://github.com/russmatney/clawe/commit/e908ca6)) feat: expo reading from 'local' static db - Russell Matney
- ([`d99ebff`](https://github.com/russmatney/clawe/commit/d99ebff)) expo: delete backend - moving to static blog setup - Russell Matney
- ([`bd38736`](https://github.com/russmatney/clawe/commit/bd38736)) fix: quieter server logs - Russell Matney
- ([`a878302`](https://github.com/russmatney/clawe/commit/a878302)) feat: basic journal page - Russell Matney

  > Some infra for creating a new full stack page.

- ([`9473268`](https://github.com/russmatney/clawe/commit/9473268)) fix: tick latest crashing on shadow latest - Russell Matney

  > b/c of course it is - will try to update later on


### 25 Jul 2022

- ([`08e0e23`](https://github.com/russmatney/clawe/commit/08e0e23)) wip: remove date handling - not bb compatible yet - Russell Matney
- ([`94e228e`](https://github.com/russmatney/clawe/commit/94e228e)) feat: db round-tripping dates, converting zoned-date-times to insts - Russell Matney
- ([`2992ec5`](https://github.com/russmatney/clawe/commit/2992ec5)) wip: towards a topbar timer - Russell Matney
- ([`f485e8e`](https://github.com/russmatney/clawe/commit/f485e8e)) fix: remove systemic/start in plasma autorequire - Russell Matney

  > Also includes the commits stream as a server dep.

- ([`02f99ed`](https://github.com/russmatney/clawe/commit/02f99ed)) chore: update deps - Russell Matney
- ([`0fb8d1d`](https://github.com/russmatney/clawe/commit/0fb8d1d)) misc: comments re: restart/db clean up - Russell Matney
- ([`62ec207`](https://github.com/russmatney/clawe/commit/62ec207)) feat: discord and audacity support - Russell Matney
- ([`4177203`](https://github.com/russmatney/clawe/commit/4177203)) feat: shadow build hook notifies when complete - Russell Matney
- ([`9255b30`](https://github.com/russmatney/clawe/commit/9255b30)) chore: move to newdb - Russell Matney
- ([`024bc07`](https://github.com/russmatney/clawe/commit/024bc07)) chore: ignore commands.json - Russell Matney

  > These were consumed by alfred on osx as an experiment (before i found `choose`)


### 8 Jul 2022

- ([`4c68635`](https://github.com/russmatney/clawe/commit/4c68635)) misc: more logging - Russell Matney
- ([`d66124e`](https://github.com/russmatney/clawe/commit/d66124e)) wip: catching uberjar build errors - Russell Matney

  > The uberjar build has started throwing errors, but the build still
  > 'works'. Need to look into this soon.

- ([`71da0fb`](https://github.com/russmatney/clawe/commit/71da0fb)) wip: comments for retracting from the db - Russell Matney

### 5 Jul 2022

- ([`b2c4908`](https://github.com/russmatney/clawe/commit/b2c4908)) misc: support discord icon - Russell Matney

### 3 Jul 2022

- ([`06800c1`](https://github.com/russmatney/clawe/commit/06800c1)) misc: clean up - Russell Matney

  > Remove extra imports. One flaw with the full-namespace usage is that
  > tools don't detect that they are not used, b/c there's an assumption
  > that we're using effectful imports.

- ([`db94627`](https://github.com/russmatney/clawe/commit/db94627)) fix: restore expo build - Russell Matney

  > The change in page airty broke errything.
  > 
  > Improves some bb.edn helpers.

- ([`93b9bb4`](https://github.com/russmatney/clawe/commit/93b9bb4)) feat: move to shadow-cljs-tailwind-jit - Russell Matney

  > Adds dep, updates shadow-cljs.edn for doctor, expo.
  > Removes manual tailwind style build commands.


### 1 Jul 2022

- ([`06e96bb`](https://github.com/russmatney/clawe/commit/06e96bb)) deps: update antq, now with progress bar. - Russell Matney
- ([`fb0f28d`](https://github.com/russmatney/clawe/commit/fb0f28d)) wip: building up gen-data and some test schemas - Russell Matney

  > And move to a more descriptive model - returning 'event-y' vectors
  > instead of performing ops right away in toggle-scratchpad.

- ([`1ac6c16`](https://github.com/russmatney/clawe/commit/1ac6c16)) fix: move back to non-local specmonstah-malli - Russell Matney
- ([`3bdab51`](https://github.com/russmatney/clawe/commit/3bdab51)) feat: gen-data specmonstah helper - Russell Matney

  > For getting your ent data by type and by name.

- ([`fbc6832`](https://github.com/russmatney/clawe/commit/fbc6832)) test: initial data generation assertions - Russell Matney
- ([`7cf4898`](https://github.com/russmatney/clawe/commit/7cf4898)) wip: building up helpers for specmonstah usage - Russell Matney
- ([`498f090`](https://github.com/russmatney/clawe/commit/498f090)) feat: specmonstah-malli tests working via babashka - Russell Matney

  > Pulled some specmonstah bb.edn deps, but seems to work well enough

- ([`914af05`](https://github.com/russmatney/clawe/commit/914af05)) demo: successful malli decode in clawe.main! - Russell Matney

  > This is very exciting.

- ([`f2ab661`](https://github.com/russmatney/clawe/commit/f2ab661)) chore: update deps - Russell Matney

  > Updating malli for bb compat!
  > 
  > Note that tick RC6 causes shadow-cljs to crash at startup.


### 27 Jun 2022

- ([`2515b4a`](https://github.com/russmatney/clawe/commit/2515b4a)) fix: zoom app name change - Russell Matney

### 25 Jun 2022

- ([`d38185b`](https://github.com/russmatney/clawe/commit/d38185b)) feat: nested commits/repo popovers - Russell Matney
- ([`170fbad`](https://github.com/russmatney/clawe/commit/170fbad)) chore: delete dead component - Russell Matney
- ([`cd204b7`](https://github.com/russmatney/clawe/commit/cd204b7)) feat: better repo-name layout on commits page - Russell Matney
- ([`8096a52`](https://github.com/russmatney/clawe/commit/8096a52)) feat: repo popover showing commit list - Russell Matney
- ([`dd3edd8`](https://github.com/russmatney/clawe/commit/dd3edd8)) refactor: pull events-cluster out of events page - Russell Matney

  > The whole events page needs to be portable!
  > I wonder what else will end up over there

- ([`7671c43`](https://github.com/russmatney/clawe/commit/7671c43)) feat: click repo name to ingest latest commits - Russell Matney

  > Well that was easy!


### 24 Jun 2022

- ([`46cb2e4`](https://github.com/russmatney/clawe/commit/46cb2e4)) misc: post layout tweaks, todo status, debug border - Russell Matney
- ([`4b34b29`](https://github.com/russmatney/clawe/commit/4b34b29)) feat: set floating anchor-comp max-w - Russell Matney
- ([`7165539`](https://github.com/russmatney/clawe/commit/7165539)) feat: rendering nested org bodies - Russell Matney
- ([`506c443`](https://github.com/russmatney/clawe/commit/506c443)) feat: floating/popover supporting popover-comp-props - Russell Matney
- ([`dbbb79f`](https://github.com/russmatney/clawe/commit/dbbb79f)) feat: debug/raw-data component using popover and shortener - Russell Matney
- ([`95f6194`](https://github.com/russmatney/clawe/commit/95f6194)) feat: commits page, api, hooks, repos api - Russell Matney
- ([`a7f7ebc`](https://github.com/russmatney/clawe/commit/a7f7ebc)) feat: dry up pages/events, add pages/commits - Russell Matney
- ([`44990f7`](https://github.com/russmatney/clawe/commit/44990f7)) feat: open-in-emacs on counts page - Russell Matney

  > Misc clean up, removes unused ref from chess.cljs


### 19 Jun 2022

- ([`dd6456a`](https://github.com/russmatney/clawe/commit/dd6456a)) misc: .gitignore update - Russell Matney

  > Ignoring a bunch of manual/generated files.

- ([`14341bc`](https://github.com/russmatney/clawe/commit/14341bc)) misc: remove printlns - Russell Matney

### 18 Jun 2022

- ([`c98d620`](https://github.com/russmatney/clawe/commit/c98d620)) docs: ubersicht/widgets dir detail - Russell Matney
- ([`b747381`](https://github.com/russmatney/clawe/commit/b747381)) fix: remove wsp-name clawe-test from tests - Russell Matney

### 12 Jun 2022

- ([`26774a9`](https://github.com/russmatney/clawe/commit/26774a9)) feat: fetching full org item, preserving selected item - Russell Matney

### 10 Jun 2022

- ([`4f4435c`](https://github.com/russmatney/clawe/commit/4f4435c)) feat: open-in-emacs - Russell Matney

  > Supports open-in-emacs as a garden hook. Opens in the most recently used
  > emacs frame via emacsclient.

- ([`aa79ca5`](https://github.com/russmatney/clawe/commit/aa79ca5)) ralphie.emacs: basic open-in-emacs function - Russell Matney
- ([`805f35e`](https://github.com/russmatney/clawe/commit/805f35e)) feat: some posts clean up - Russell Matney

  > - No longer overwriting org/source-file
  >   (so we can use it in open-in-emacs)
  > - string-shortener dynamic length
  > - improved posts display/layout


### 15 May 2022

- ([`7b3303b`](https://github.com/russmatney/clawe/commit/7b3303b)) feat: add text animations to big eval-diffs - Russell Matney
- ([`82383f8`](https://github.com/russmatney/clawe/commit/82383f8)) feat: calc eval-diff, colorize eval, eval-diff, improve layout - Russell Matney

  > Jumping into css grid was quite simple.

- ([`e02cc52`](https://github.com/russmatney/clawe/commit/e02cc52)) feat: toggle-all-mistakes button - Russell Matney
- ([`272f40c`](https://github.com/russmatney/clawe/commit/272f40c)) feat: scroll on chessground to move through game states - Russell Matney
- ([`e044368`](https://github.com/russmatney/clawe/commit/e044368)) refactor: break out game-states filter and list - Russell Matney

  > Prepping for an all-mistakes feature.

- ([`aa2976e`](https://github.com/russmatney/clawe/commit/aa2976e)) feat: display judgement and eval per game state - Russell Matney
- ([`d2e7b42`](https://github.com/russmatney/clawe/commit/d2e7b42)) feat: working cljs chessground wrapper - Russell Matney

  > Couple uix/with-effects. Modeled after the react-chess/chessground impl.

- ([`1915faf`](https://github.com/russmatney/clawe/commit/1915faf)) wip: error-boundary attempt, unproven - Russell Matney
- ([`3626364`](https://github.com/russmatney/clawe/commit/3626364)) misc: chess path clean up - Russell Matney
- ([`fd2e476`](https://github.com/russmatney/clawe/commit/fd2e476)) fix: update to latest chessground direct assets - Russell Matney
- ([`b08a9b5`](https://github.com/russmatney/clawe/commit/b08a9b5)) fix: include wing via local/root on test-runner classpath - Russell Matney

  > Restores unit tests.

- ([`98cb31c`](https://github.com/russmatney/clawe/commit/98cb31c)) fix: larger float-and-center dimensions - Russell Matney
- ([`fb9af6d`](https://github.com/russmatney/clawe/commit/fb9af6d)) wip: moving to consuming chessground directly - Russell Matney

### 14 May 2022

- ([`9819356`](https://github.com/russmatney/clawe/commit/9819356)) wip: trying to get shapes to draw... without success - Russell Matney
- ([`b328585`](https://github.com/russmatney/clawe/commit/b328585)) feat: rendering game highlights via chess.js, react-chessground - Russell Matney

  > Will need to copy the styles/images into the public dir on other
  > clients, but otherwise we're off to the races.
  > 
  > Wips the analysis - when it exists, it's hitting the game-highlights component.

- ([`b4d905e`](https://github.com/russmatney/clawe/commit/b4d905e)) feat: initial chess thumbnail and popover - Russell Matney

  > Coming together!

- ([`1188c55`](https://github.com/russmatney/clawe/commit/1188c55)) fix: support java.lang.Integer db types - Russell Matney

  > Kind of a headache to have more than one kind of number type...

- ([`7d84eb2`](https://github.com/russmatney/clawe/commit/7d84eb2)) feat: support inst-ms as time format - Russell Matney

  > Yet another date-time format!

- ([`267cd6f`](https://github.com/russmatney/clawe/commit/267cd6f)) lichess: flatten and parse-game clean up - Russell Matney

  > Also sets the timestamp fields.

- ([`39c960b`](https://github.com/russmatney/clawe/commit/39c960b)) feat: fetching and storing lichess games - Russell Matney

  > Adds a unique id to make syncing to datalevin simple, and some handling
  > for namespacing lichess games on :lichess.game/ keywords.
  > 
  > Pretty smooth!

- ([`234dddc`](https://github.com/russmatney/clawe/commit/234dddc)) db: use unique key for commits, workspaces - Russell Matney

  > Saves the fetch-per-object lookup to get the :db/id when syncing with
  > the db.

- ([`055a09d`](https://github.com/russmatney/clawe/commit/055a09d)) feat: repos page, hooks, and backend boilerplate - Russell Matney

### 13 May 2022

- ([`c82e8df`](https://github.com/russmatney/clawe/commit/c82e8df)) feat: group events by commits and screenshots - Russell Matney
- ([`e32f404`](https://github.com/russmatney/clawe/commit/e32f404)) git: filter some repos, fix some error handling - Russell Matney
- ([`0ae8ced`](https://github.com/russmatney/clawe/commit/0ae8ced)) refactor: pass opts into pages to support live-reload - Russell Matney

  > If we get too many deps/files away, uix/react are 'smart' enough to
  > avoid re-rendering our components, which makes immediate tailwind
  > changes require a reload or re-saving a different file. Here we pass
  > updated props into the pages to extend the live-reload love.
  > 
  > Also renames some page public funcs to align better.

- ([`6b4635b`](https://github.com/russmatney/clawe/commit/6b4635b)) refactor: much cleaner router and core component via route-defs - Russell Matney
- ([`b7114c5`](https://github.com/russmatney/clawe/commit/b7114c5)) refactor: move all {doctor,expo}.ui.views/* to pages/* - Russell Matney

  > Doctor now includes all pages (including expo's garden, counts, posts).
  > 
  > The `counter` widget in doctor.ui.core was pulled out as a page and
  > query-param crud example.

- ([`00ff62a`](https://github.com/russmatney/clawe/commit/00ff62a)) feat: better hover delay, support offset prop - Russell Matney
- ([`e19908c`](https://github.com/russmatney/clawe/commit/e19908c)) feat: extract todo-list into components.todo - Russell Matney

  > Also sets better default todos filters.

- ([`12046e1`](https://github.com/russmatney/clawe/commit/12046e1)) page: extract page component from doctor.ui.core - Russell Matney

  > Starting a new pages namespace to combine the views in the doctor/expo.

- ([`d3e2a92`](https://github.com/russmatney/clawe/commit/d3e2a92)) debug: support label as false to hide the label - Russell Matney
- ([`150cb96`](https://github.com/russmatney/clawe/commit/150cb96)) refactor: use new todos components - Russell Matney
- ([`f0c63e5`](https://github.com/russmatney/clawe/commit/f0c63e5)) todos: cleaner filter popovers - Russell Matney
- ([`bb22eea`](https://github.com/russmatney/clawe/commit/bb22eea)) feat: move filters into popover - Russell Matney
- ([`2bec177`](https://github.com/russmatney/clawe/commit/2bec177)) feat: click to open nav menu - Russell Matney

  > The floating/popover is now opt-in for click and hover usage, and
  > supports passing props for the anchor component.
  > 
  > Rather than an always-open menu, it's now a popover that opens on click.
  > We could later reach for closing the popover when navigation occurs,
  > but, meh.

- ([`a3114d9`](https://github.com/russmatney/clawe/commit/a3114d9)) refactor: home comp now page, pulled out menu - Russell Matney
- ([`267f6da`](https://github.com/russmatney/clawe/commit/267f6da)) feat: extend todo filtering groups - Russell Matney

  > Now including match-fn support for custom filtering options, and
  > sub-groups by filter type for splitting out daily/workspace/todo file
  > sources.

- ([`a8077c6`](https://github.com/russmatney/clawe/commit/a8077c6)) feat: include workspaces in todos - Russell Matney

  > Plus yet another date format.


### 12 May 2022

- ([`9432c2d`](https://github.com/russmatney/clawe/commit/9432c2d)) feat: commit subject/body in font-mono - Russell Matney

  > Somehow just looks a bit better, ya know?

- ([`f1042ff`](https://github.com/russmatney/clawe/commit/f1042ff)) feat: honor newlines in commit bodies - Russell Matney

  > TIL: whitespace: pre-line. like `pre`, but trims the outside, and honors
  > the newlines. no more fill-paragraph! yay!

- ([`fc4f48e`](https://github.com/russmatney/clawe/commit/fc4f48e)) feat: basic commit 'thumbnail' - Russell Matney

  > really more of a one-liner


### 11 May 2022

- ([`1ef2c5f`](https://github.com/russmatney/clawe/commit/1ef2c5f)) feat: initial commit popover layout - Russell Matney
- ([`ac47059`](https://github.com/russmatney/clawe/commit/ac47059)) feat: sorting events, full popup screenshots from thumbnails - Russell Matney
- ([`34f6692`](https://github.com/russmatney/clawe/commit/34f6692)) feat: ralphie.git parsing binaries in stat-lines - Russell Matney

### 8 May 2022

- ([`ea6895d`](https://github.com/russmatney/clawe/commit/ea6895d)) feat: components.screenshot/thumbnail - Russell Matney

  > displaying screenshots in clusters! Getting there :D

- ([`86c059c`](https://github.com/russmatney/clawe/commit/86c059c)) feat: cluster events by intervals between - Russell Matney

  > Kind of a crazy reduce, but maybe it'll be re-usable in some way later
  > on.

- ([`df84b6e`](https://github.com/russmatney/clawe/commit/df84b6e)) topbar: persist bg-mode to datalevin via db singleton - Russell Matney

  > Adds a topbar id and db singleton support, plus a round-trip from the
  > component to the db and back via plasma defhandler and defstream.

- ([`87ca322`](https://github.com/russmatney/clawe/commit/87ca322)) deps: update all yarn deps - Russell Matney
- ([`01834f1`](https://github.com/russmatney/clawe/commit/01834f1)) deps: update to tailwind 3 - Russell Matney
- ([`be1237d`](https://github.com/russmatney/clawe/commit/be1237d)) deps: update clojure deps - Russell Matney
- ([`6fc68c5`](https://github.com/russmatney/clawe/commit/6fc68c5)) fix: misc floating-ui middlewares - Russell Matney
- ([`78aa917`](https://github.com/russmatney/clawe/commit/78aa917)) feat: cleaner timeline api and layout - Russell Matney
- ([`784bfeb`](https://github.com/russmatney/clawe/commit/784bfeb)) feat: components.floating/popover component - Russell Matney

  > A reasonable api for a floating popover, via floating-ui

- ([`a8b2bf4`](https://github.com/russmatney/clawe/commit/a8b2bf4)) wip: floating-ui sort of working - Russell Matney

  > ended up bailing on the headlessui popover integration... their docs
  > suggest popper-js, which has been abandoned for floating-ui... spreading
  > these js props into cljs is tricky/weird.


### 7 May 2022

- ([`7dddace`](https://github.com/russmatney/clawe/commit/7dddace)) wip: integrating floating-ui - Russell Matney
- ([`22f9316`](https://github.com/russmatney/clawe/commit/22f9316)) refactor: events page - pull v1 event-list into component - Russell Matney
- ([`3503ceb`](https://github.com/russmatney/clawe/commit/3503ceb)) chore: date format clean up, first cljs test - Russell Matney
- ([`44f0439`](https://github.com/russmatney/clawe/commit/44f0439)) feat: list counts by type for displayed list - Russell Matney
- ([`5124112`](https://github.com/russmatney/clawe/commit/5124112)) feat: support selecting multiple dates - Russell Matney
- ([`03a0f51`](https://github.com/russmatney/clawe/commit/03a0f51)) feat: date popover showing event counts by type - Russell Matney
- ([`ca2338b`](https://github.com/russmatney/clawe/commit/ca2338b)) feat: improve timeline layout and filtering by days-ago - Russell Matney
- ([`dfb4746`](https://github.com/russmatney/clawe/commit/dfb4746)) chore: force root re-render on file-save - Russell Matney

  > Not all files (namely, components) trigger a full re-render, b/c the
  > root element technically hasn't been updated. If the app is huge, this
  > might be expensive, but for now we want live-reload, b/c duh.

- ([`ed037dc`](https://github.com/russmatney/clawe/commit/ed037dc)) feat: fade dates without data, show dates for timestamps - Russell Matney
- ([`01db3c9`](https://github.com/russmatney/clawe/commit/01db3c9)) feat: introduce timeline component and event-by-date filtering - Russell Matney
- ([`9a63265`](https://github.com/russmatney/clawe/commit/9a63265)) chore: extract use-keyboard-cursor - Russell Matney
- ([`f725231`](https://github.com/russmatney/clawe/commit/f725231)) chore: extract commit component - Russell Matney
- ([`4a56854`](https://github.com/russmatney/clawe/commit/4a56854)) fix: don't reference non-existent namespace - Russell Matney
- ([`00e75b1`](https://github.com/russmatney/clawe/commit/00e75b1)) chore: better logging of plasma errors - Russell Matney
- ([`1bdd884`](https://github.com/russmatney/clawe/commit/1bdd884)) chore: remove println, redundant do - Russell Matney
- ([`95ead75`](https://github.com/russmatney/clawe/commit/95ead75)) feat: pull daily files from the last two weeks - Russell Matney
- ([`36155f6`](https://github.com/russmatney/clawe/commit/36155f6)) feat: yet another date time format - Russell Matney
- ([`597a390`](https://github.com/russmatney/clawe/commit/597a390)) chore: remove dead m-x suggestions - Russell Matney
- ([`fd74f6f`](https://github.com/russmatney/clawe/commit/fd74f6f)) feat: aseprite toggle - Russell Matney

### 6 May 2022

- ([`e71b6c0`](https://github.com/russmatney/clawe/commit/e71b6c0)) refactor: more components pulled out - Russell Matney
- ([`6a3d138`](https://github.com/russmatney/clawe/commit/6a3d138)) refactor: move doctor.api nses to toplevel - Russell Matney

  > Perhaps these will start eating some of the clawe namespaces/logic now.
  > top level `api` is intended to be used by the server, but be mostly .clj
  > implemented. Perhaps api.garden should replace top-level garden? These
  > could probably talk to the clawe binary use-cases, unless the systemic
  > startup times factor into the bb startup performance.

- ([`f4727c4`](https://github.com/russmatney/clawe/commit/f4727c4)) refactor: break components into shared namespace - Russell Matney
- ([`7f4715c`](https://github.com/russmatney/clawe/commit/7f4715c)) refactor: shared screenshots/todos components - Russell Matney
- ([`5d2872b`](https://github.com/russmatney/clawe/commit/5d2872b)) feat: click screenshots to open/close modal - Russell Matney
- ([`815260b`](https://github.com/russmatney/clawe/commit/815260b)) feat: display commit lines added/removed - Russell Matney

  > Adds try/catches to the ralphie.git funcs, extends clawe.git to get
  > commit-stats, displays the added/removed in the doctor event frontend.

- ([`38ede16`](https://github.com/russmatney/clawe/commit/38ede16)) ralphie.git: parsing commit stat-lines - Russell Matney
- ([`3c69868`](https://github.com/russmatney/clawe/commit/3c69868)) ralphie.git: parsing commit stats headers - Russell Matney
- ([`7c29309`](https://github.com/russmatney/clawe/commit/7c29309)) fix: update server deps - Russell Matney

  > Could use some testing on these servers too!
  > 
  > Testing a body with multiple lines.
  > 
  > .... parse this!

- ([`b7b1e0f`](https://github.com/russmatney/clawe/commit/b7b1e0f)) wip: starting towards parsed commit stats - Russell Matney
- ([`bbde9f3`](https://github.com/russmatney/clawe/commit/bbde9f3)) chore: basic ralphie.git/commits-for-dir tests - Russell Matney

### 5 May 2022

- ([`c4db012`](https://github.com/russmatney/clawe/commit/c4db012)) fix: ensure [?e :type :clawe/workspaces] on get-db-workspace - Russell Matney

### 1 May 2022

- ([`e488b11`](https://github.com/russmatney/clawe/commit/e488b11)) refactor: move topbar hooks - Russell Matney
- ([`3ef7bba`](https://github.com/russmatney/clawe/commit/3ef7bba)) refactor: move events to shared hooks file - Russell Matney
- ([`bff02ad`](https://github.com/russmatney/clawe/commit/bff02ad)) refactor: move doctor ui.namespaces to hooks - Russell Matney
- ([`620264b`](https://github.com/russmatney/clawe/commit/620264b)) fix: update frontends for new shared transit time handlers - Russell Matney
- ([`6d21774`](https://github.com/russmatney/clawe/commit/6d21774)) refactor: expo counts hook - Russell Matney
- ([`8e611d6`](https://github.com/russmatney/clawe/commit/8e611d6)) refactor: clean up garden hooks - Russell Matney
- ([`d9a199d`](https://github.com/russmatney/clawe/commit/d9a199d)) feat: yet another date format - Russell Matney
- ([`e57e7d0`](https://github.com/russmatney/clawe/commit/e57e7d0)) chore: util clean up - Russell Matney
- ([`34906cc`](https://github.com/russmatney/clawe/commit/34906cc)) feat: date parsing unit tests - Russell Matney
- ([`bb10aa7`](https://github.com/russmatney/clawe/commit/bb10aa7)) refactor: pull date parsing, item func out of api.events - Russell Matney
- ([`f250f56`](https://github.com/russmatney/clawe/commit/f250f56)) chore: delete expo.db, dry up transit time literals - Russell Matney
- ([`b9c736d`](https://github.com/russmatney/clawe/commit/b9c736d)) feat: introduce garden.core namespace - Russell Matney

  > Might move alot more domain objects into top-level namespaces going
  > forward. `git` feels like a good contender.

- ([`8cde8cf`](https://github.com/russmatney/clawe/commit/8cde8cf)) refactor: pull posts hooks out into hooks/garden - Russell Matney

  > Going with a top-level hooks namespace for these use-blahs and plasma
  > handlers.

- ([`51fbead`](https://github.com/russmatney/clawe/commit/51fbead)) fix: more expo infra fixes - Russell Matney

  > Moves expo to it's own expo/public/index.html, copies in css.

- ([`a6afd2c`](https://github.com/russmatney/clawe/commit/a6afd2c)) fix: include key when source-file is missing - Russell Matney
- ([`a41c282`](https://github.com/russmatney/clawe/commit/a41c282)) feat: add expo as known endpoints, sets default shadow opts - Russell Matney
- ([`45e46f7`](https://github.com/russmatney/clawe/commit/45e46f7)) feat: expo fe and be config - Russell Matney

  > Copies doctor helpers for expo. This is missing a .dir-locals change
  > that updates the default shadow opts to use :expo-app.

- ([`21b0402`](https://github.com/russmatney/clawe/commit/21b0402)) feat: screenshot full-screen modal via headlessui - Russell Matney
- ([`11f7800`](https://github.com/russmatney/clawe/commit/11f7800)) fix: filter empty timestrings - Russell Matney
- ([`4ccc53a`](https://github.com/russmatney/clawe/commit/4ccc53a)) feat: more specific bb restarts and loggers - Russell Matney
- ([`f9c483c`](https://github.com/russmatney/clawe/commit/f9c483c)) fix: org item status showing archived, skipped - Russell Matney

### 30 Apr 2022

- ([`052ec74`](https://github.com/russmatney/clawe/commit/052ec74)) feat: including archived items in events - Russell Matney
- ([`f812ebb`](https://github.com/russmatney/clawe/commit/f812ebb)) feat: use todo component, filter out future events - Russell Matney

  > Remove 'scheduled/deadline' org todos from events view

- ([`18a9284`](https://github.com/russmatney/clawe/commit/18a9284)) fix: colorized-metadata passing opts to recursive calls - Russell Matney
- ([`606ee15`](https://github.com/russmatney/clawe/commit/606ee15)) feat: parsing org-item dates, more screenshot parse fixes - Russell Matney
- ([`603a197`](https://github.com/russmatney/clawe/commit/603a197)) feat: smooth scrolling to selected event - Russell Matney
- ([`f13215f`](https://github.com/russmatney/clawe/commit/f13215f)) feat: initial j/k keybindings for navigating events - Russell Matney
- ([`d0a7ddf`](https://github.com/russmatney/clawe/commit/d0a7ddf)) feat: osx dev browser toggle - Russell Matney

### 29 Apr 2022

- ([`aa9b85f`](https://github.com/russmatney/clawe/commit/aa9b85f)) feat: more events style, urls to commits/repo - Russell Matney
- ([`675359e`](https://github.com/russmatney/clawe/commit/675359e)) feat: rendering commits in the ui - Russell Matney

  > Commit events pulling and rendering.

- ([`d471615`](https://github.com/russmatney/clawe/commit/d471615)) misc: remove old git-log format helper - Russell Matney
- ([`2dd419d`](https://github.com/russmatney/clawe/commit/2dd419d)) feat: parsing git commits from workspace-git repos - Russell Matney

  > Pulls git commits per repo, syncs them to the db.
  > Pulls commits from the db, parses timestamps, pushes them as events to
  > the frontend.

- ([`92ec107`](https://github.com/russmatney/clawe/commit/92ec107)) feat: parsing arbitrary git commits - Russell Matney

  > Uses git log format to build an edn-readable git log, then reads it.

- ([`9d52ec6`](https://github.com/russmatney/clawe/commit/9d52ec6)) wip: toying with benchmarking - Russell Matney
- ([`22029bb`](https://github.com/russmatney/clawe/commit/22029bb)) feat: events supporting screenshots on osx - Russell Matney
- ([`0c34661`](https://github.com/russmatney/clawe/commit/0c34661)) wip: disable uuid support - Russell Matney
- ([`51629f1`](https://github.com/russmatney/clawe/commit/51629f1)) fix: restore initial file in workspace create - Russell Matney
- ([`fbc454a`](https://github.com/russmatney/clawe/commit/fbc454a)) feat: layout screenshot-events on events page - Russell Matney
- ([`b9c1bc1`](https://github.com/russmatney/clawe/commit/b9c1bc1)) feat: refactor wallpapers - Russell Matney

  > Move some db interactions to defthing namespace, pull mark-wp-set out of
  > set-wallpaper, which let me force the onedark ones to the top of the
  > list, so they land in the ui.


### 23 Apr 2022

- ([`3e57889`](https://github.com/russmatney/clawe/commit/3e57889)) refactor: consolidate yabai usage in workspaces ns - Russell Matney

  > Refactors yabai usage into workspaces funcs. Ideally this wouldn't leak
  > from clawe.workspaces at all.

- ([`aafc9ff`](https://github.com/russmatney/clawe/commit/aafc9ff)) fix: find readmes in directories for emacs/open initial-file - Russell Matney
- ([`db03d00`](https://github.com/russmatney/clawe/commit/db03d00)) refactor: more clawe.workspaces clean up - Russell Matney

  > Moves a few more awm-cli stringy lua commands into ralphie.awesome/fnl
  > functions.
  > 
  > Re-uses workspace-defaults in ->pseudo-workspace. Perhaps that whole
  > func should move over, along with the basic workflow data helpers.

- ([`f720d37`](https://github.com/russmatney/clawe/commit/f720d37)) refactor: clear dead code, move more to defworkspace fns - Russell Matney
- ([`2b07a97`](https://github.com/russmatney/clawe/commit/2b07a97)) feat: defthing supporting post-ops - Russell Matney

  > Adds a way to pass x-or-f operations that will run after a defthing's
  > xorfs have been evaluated. In this case, we support moving
  > :workspace/directory from a relative to an absolute path by default.

- ([`790f086`](https://github.com/russmatney/clawe/commit/790f086)) feat: refactor install workspaces to use defworkspace - Russell Matney
- ([`3c1306f`](https://github.com/russmatney/clawe/commit/3c1306f)) fix: continuing to work out laziness in db/transact - Russell Matney

  > Very happy to have this caught in testing - it'd be very confusing
  > otherwise.

- ([`c0dce65`](https://github.com/russmatney/clawe/commit/c0dce65)) misc: clean up defworkspace, include new unit tests - Russell Matney
- ([`e95f4f9`](https://github.com/russmatney/clawe/commit/e95f4f9)) refactor: cleaner install repo workspaces - Russell Matney

  > Also `seq`s transactions before creating the connection, so that we
  > don't nest connections when the seq itself needed to connect as well. At
  > some point we could re-use the connection for these things...

- ([`9478716`](https://github.com/russmatney/clawe/commit/9478716)) feat: seq on sync-workspaces-to-db, initial install-repo-wsps port - Russell Matney

  > This required seq perplexes me a bit.
  > 
  > Ports the repos and gets tests passing before impl clean up.

- ([`05b35de`](https://github.com/russmatney/clawe/commit/05b35de)) icon: pavucontrol - Russell Matney
- ([`3fc8b9b`](https://github.com/russmatney/clawe/commit/3fc8b9b)) wip: refactoring/cleaning defworkspace tests - Russell Matney
- ([`0e04af8`](https://github.com/russmatney/clawe/commit/0e04af8)) refactor: clean up defworkspace tests - Russell Matney

### 22 Apr 2022

- ([`b28828a`](https://github.com/russmatney/clawe/commit/b28828a)) feat: support defworkspace db read/write - Russell Matney

  > Pulls helpers from clawe.workspaces into defthing.defworkspace, with
  > some tests.

- ([`e3dd69e`](https://github.com/russmatney/clawe/commit/e3dd69e)) feat: update db consumers to use defthing.db - Russell Matney
- ([`af2111d`](https://github.com/russmatney/clawe/commit/af2111d)) feat: create defthing.db, write a test - Russell Matney

  > Pulls clawe.db.core over.


### 16 Apr 2022

- ([`0ffd395`](https://github.com/russmatney/clawe/commit/0ffd395)) fix: sorting workspaces by scratchpads on osx - Russell Matney
- ([`d8d043f`](https://github.com/russmatney/clawe/commit/d8d043f)) feat: support godot app toggle - Russell Matney

### 15 Apr 2022

- ([`26088ad`](https://github.com/russmatney/clawe/commit/26088ad)) fix: is-mac? conditional in test runner - Russell Matney
- ([`6bdbde2`](https://github.com/russmatney/clawe/commit/6bdbde2)) fix: fetch full-wsp before opening term/emacs - Russell Matney
- ([`ad1cbbe`](https://github.com/russmatney/clawe/commit/ad1cbbe)) fix: try/catch a few yabai funcs - Russell Matney
- ([`bcdef91`](https://github.com/russmatney/clawe/commit/bcdef91)) fix: restore tests on non-mac - Russell Matney
- ([`4dd9265`](https://github.com/russmatney/clawe/commit/4dd9265)) fix: restore clawe reload on linux - Russell Matney
- ([`25cb196`](https://github.com/russmatney/clawe/commit/25cb196)) fix: portable bb.edn home-dir lookup - Russell Matney

### 14 Apr 2022

- ([`9dccb5f`](https://github.com/russmatney/clawe/commit/9dccb5f)) feat: unfloat scratchpads when we're on their own space - Russell Matney

### 10 Apr 2022

- ([`8b8bc3d`](https://github.com/russmatney/clawe/commit/8b8bc3d)) yabai: misc clean up and fixes - Russell Matney
- ([`8f7f360`](https://github.com/russmatney/clawe/commit/8f7f360)) fix: opt-in to label overwriting - Russell Matney

  > unlabelled spaces will be preferred to creating new ones.

- ([`34fb9f1`](https://github.com/russmatney/clawe/commit/34fb9f1)) fix: create-and-label fixed to ensure and support overwriting - Russell Matney
- ([`746da97`](https://github.com/russmatney/clawe/commit/746da97)) feat: create toggle spaces when missing - Russell Matney

  > before sending clients 'away' to nothing, create the tag and label that
  > they'll eventually land on.

- ([`63b80fd`](https://github.com/russmatney/clawe/commit/63b80fd)) wip: hacky workspace clean up - Russell Matney
- ([`788d8d0`](https://github.com/russmatney/clawe/commit/788d8d0)) wip: may never need this, but a focus-in-space fn - Russell Matney

  > Osx should do this automatically - a yabai rule can do this as well:
  > 
  > yabai -m signal --add event=window_created active=yes action='yabai -m window --focus $YABAI_WINDOW_ID'

- ([`99fb596`](https://github.com/russmatney/clawe/commit/99fb596)) feat: support open/create workspace via yabai - Russell Matney
- ([`e04311e`](https://github.com/russmatney/clawe/commit/e04311e)) misc: clean up, fixes, use local wing - Russell Matney

  > wing's latest release is not up to date

- ([`fafa909`](https://github.com/russmatney/clawe/commit/fafa909)) fix: markup-less label/desc - Russell Matney

  > The markup doesn't work with `choose`, so we move the overloaded label
  > to the description, which ralphie/rofi will markup itself

- ([`d576a53`](https://github.com/russmatney/clawe/commit/d576a53)) fix: highlight background - Russell Matney

  > b/c the default font highlight is illegible.


### 9 Apr 2022

- ([`20fa275`](https://github.com/russmatney/clawe/commit/20fa275)) fix: clean up, remove printlns - Russell Matney

  > Things working pretty great! Much quicker toggles.

- ([`db71c87`](https://github.com/russmatney/clawe/commit/db71c87)) fix: browser toggle shouldn't open a browser every time - Russell Matney
- ([`10b8e8d`](https://github.com/russmatney/clawe/commit/10b8e8d)) perf: much faster journal toggle - Russell Matney

  > Writes a current-workspace-fast that skips the db, tmux, etc.
  > Extends a few functions to recieve 'prefetched' data, to prevent
  > redundant queries.

- ([`a5d7f05`](https://github.com/russmatney/clawe/commit/a5d7f05)) fix: disable all awm when on mac - Russell Matney

### 8 Apr 2022

- ([`9c192fc`](https://github.com/russmatney/clawe/commit/9c192fc)) refactor: break out toggle namespace, cover slack/spotify - Russell Matney
- ([`bf5bb4a`](https://github.com/russmatney/clawe/commit/bf5bb4a)) feat: working clone suggestions on osx - Russell Matney
- ([`a0ba3e2`](https://github.com/russmatney/clawe/commit/a0ba3e2)) feat: osx support for browser/tabs, ralphie.clipboard - Russell Matney
- ([`ea9892e`](https://github.com/russmatney/clawe/commit/ea9892e)) feat: use choose as osx rofi, restore clawe m-x on osx - Russell Matney
- ([`6966bd4`](https://github.com/russmatney/clawe/commit/6966bd4)) feat: write commands to json to support alfred workflow - Russell Matney
- ([`9f9ce2e`](https://github.com/russmatney/clawe/commit/9f9ce2e)) feat: labelling spaces after prompting for user input - Russell Matney

  > Consuming a lil obb script!

- ([`900c1fc`](https://github.com/russmatney/clawe/commit/900c1fc)) feat: toggle-web-2 - Russell Matney
- ([`ba2ad62`](https://github.com/russmatney/clawe/commit/ba2ad62)) feat: ralphie.browser/open on osx - Russell Matney

### 6 Apr 2022

- ([`7019591`](https://github.com/russmatney/clawe/commit/7019591)) fix: prefer yabai index to workspace title - Russell Matney

  > Could probably pass the whole wsp into yabai's function - might be a
  > cleaner pattern.

- ([`2c0aac7`](https://github.com/russmatney/clawe/commit/2c0aac7)) feat: journal toggle working! - Russell Matney
- ([`4e4952b`](https://github.com/russmatney/clawe/commit/4e4952b)) fix: `-1` names break command line opts. like creating new terms - Russell Matney

### 4 Apr 2022

- ([`03ddc7d`](https://github.com/russmatney/clawe/commit/03ddc7d)) feat: more bb.edn aliases - Russell Matney
- ([`e66fce0`](https://github.com/russmatney/clawe/commit/e66fce0)) fix: move back to local wing - Russell Matney

  > wing hasn't published the latest yet - i need uix.router for doctor's
  > ui.


### 3 Apr 2022

- ([`8a8ca2b`](https://github.com/russmatney/clawe/commit/8a8ca2b)) fix: don't crash when tmux isn't running - Russell Matney
- ([`d649126`](https://github.com/russmatney/clawe/commit/d649126)) feat: yabai sandbox, couple label-space helpers - Russell Matney
- ([`3615295`](https://github.com/russmatney/clawe/commit/3615295)) feat: toggling emacs and alacritty successfully - Russell Matney

  > Integrates yabai spaces and windows into clawe.workspaces all-workspaces
  > and current-workspace fns.
  > 
  > Manually installed workspaces via comments, and now labelling spaces via
  > yabai means emacs/term open in well named emacs workspaces and tmux
  > sessions (as long as the label has a matching db-workspace).

- ([`b48ac6a`](https://github.com/russmatney/clawe/commit/b48ac6a)) misc: fixes and misc non osx comments - Russell Matney

### 2 Apr 2022

- ([`4001e19`](https://github.com/russmatney/clawe/commit/4001e19)) feat: initial yabai namespace - Russell Matney

  > querying namespaced maps for displays, spaces, windows.

- ([`c8dcfe8`](https://github.com/russmatney/clawe/commit/c8dcfe8)) fix: don't check a created term so we don't block skhd - Russell Matney
- ([`d82ab0a`](https://github.com/russmatney/clawe/commit/d82ab0a)) feat: notify supporting apple notifications - Russell Matney
- ([`f1de88b`](https://github.com/russmatney/clawe/commit/f1de88b)) feat: add install-clawe alias (for clawe-install) - Russell Matney
- ([`88ce497`](https://github.com/russmatney/clawe/commit/88ce497)) fix: remove most hard-coded home dirs - Russell Matney

### 1 Apr 2022

- ([`7172f51`](https://github.com/russmatney/clawe/commit/7172f51)) feat: add malli, initial schema toying - Russell Matney
- ([`278543a`](https://github.com/russmatney/clawe/commit/278543a)) wip: towards listing user studies - Russell Matney

  > Going to need to get some oauth going to even list studies - creating
  > them doesn't seem to exist yet.

- ([`fb87f68`](https://github.com/russmatney/clawe/commit/fb87f68)) feat: move chess commands, add cache-clearing - Russell Matney

  > Also moves the cache to storying per-inputs.

- ([`4c222f8`](https://github.com/russmatney/clawe/commit/4c222f8)) feat: copy-highlighted-tabs - Russell Matney

  > Quicker than deleting the others from all-open tabs. And a nice way to
  > grab the current url without switching to the browser.


### 31 Mar 2022

- ([`26a078e`](https://github.com/russmatney/clawe/commit/26a078e)) fix: broken unit test - Russell Matney

  > makes me wonder if this thing is worth anything

- ([`a822403`](https://github.com/russmatney/clawe/commit/a822403)) wip: towards toggle-scratchpad-2 - Russell Matney

### 27 Mar 2022

- ([`5f23a90`](https://github.com/russmatney/clawe/commit/5f23a90)) topbar: display battery time/charge remaining - Russell Matney
- ([`88e798f`](https://github.com/russmatney/clawe/commit/88e798f)) feat: support awm-tag merging on current-workspaces - Russell Matney
- ([`24a4549`](https://github.com/russmatney/clawe/commit/24a4549)) feat: ralphie.awesome: support tag-names on clients - Russell Matney
- ([`64788de`](https://github.com/russmatney/clawe/commit/64788de)) fix: doctor.util for filtering fns before plasma - Russell Matney

  > Ideally we'd bake something like this into the server elsewhere - there
  > are probably transit handlers to catch/filter this kind of thing. Just
  > want to throw the data to the client...

- ([`354276f`](https://github.com/russmatney/clawe/commit/354276f)) feat: workspace/current-workspaces - Russell Matney

  > For getting all the current workspaces (not just one).


### 24 Mar 2022

- ([`9bad49c`](https://github.com/russmatney/clawe/commit/9bad49c)) refactor: rewrite focus-client with awm/fnl macro - Russell Matney
- ([`462f503`](https://github.com/russmatney/clawe/commit/462f503)) fix: current-workspace for tags not in db - Russell Matney

  > Falls back to locally defined workspace def when a tag name does not
  > exist in the db. Necessary to support e.g. the journal workspace, which
  > is not 'installable'. Note that creating workspaces does not yet imply
  > that they exist in the db.

- ([`7d9e934`](https://github.com/russmatney/clawe/commit/7d9e934)) fix: notify on fallback creation, nil-pun chartjs load - Russell Matney

  > No need to crash everything.


### 23 Mar 2022

- ([`41f5d68`](https://github.com/russmatney/clawe/commit/41f5d68)) fix: use proper tmux.fire/cmd, add display name to workspace desc - Russell Matney

### 20 Mar 2022

- ([`66f1e83`](https://github.com/russmatney/clawe/commit/66f1e83)) feat: surface restart/stop for systemd user services - Russell Matney
- ([`623d2ed`](https://github.com/russmatney/clawe/commit/623d2ed)) feat: misc workspace layout improvements - Russell Matney

  > Now showing tmux session data.

- ([`bd0b02a`](https://github.com/russmatney/clawe/commit/bd0b02a)) feat: merge tmux sessions into all-workspaces/current-workspace - Russell Matney
- ([`2913aa6`](https://github.com/russmatney/clawe/commit/2913aa6)) feat: metadata-debug, show collections after plain vals - Russell Matney
- ([`a882d2b`](https://github.com/russmatney/clawe/commit/a882d2b)) feat: colorized and recursive metadata - Russell Matney
- ([`ba68b84`](https://github.com/russmatney/clawe/commit/ba68b84)) fix: duplicate require - Russell Matney
- ([`10ace09`](https://github.com/russmatney/clawe/commit/10ace09)) fix: wrap bb tasks in a try/catch - Russell Matney

  > prevent m-x crashes when not in a bb.edn supported workspace.

- ([`264cf9f`](https://github.com/russmatney/clawe/commit/264cf9f)) feat: support :tmux.fire/directory - Russell Matney

  > Prefixes the tmux.fire command with `cd <directory> &&`, for much
  > convenience.

- ([`3e4ac8d`](https://github.com/russmatney/clawe/commit/3e4ac8d)) fix: reduce event logging noise - Russell Matney
- ([`b71f098`](https://github.com/russmatney/clawe/commit/b71f098)) feat: basic keyboard-based control in doctor core ui - Russell Matney

  > keybind is pretty good! Got caught for a long time on some router hook
  > rule breakage tho :/


### 19 Mar 2022

- ([`d18405c`](https://github.com/russmatney/clawe/commit/d18405c)) feat: m-x now supports killing clients and awm tags - Russell Matney

  > Kill all the things!
  > 
  > Note that awm blocks tag deletion if the tag still has clients.

- ([`78adbc5`](https://github.com/russmatney/clawe/commit/78adbc5)) fix: support rofi labels with `|` - Russell Matney

  > For now it replaces them with `-` as a quickfix.

- ([`d8bf757`](https://github.com/russmatney/clawe/commit/d8bf757)) feat: tmux key namespacing, tmux/rofi-kill-opts - Russell Matney

  > m-x now supports killing any open tmux session, window, or pane.

- ([`fdd470d`](https://github.com/russmatney/clawe/commit/fdd470d)) fix: use namespaced keys in list-panes arg - Russell Matney
- ([`f27820f`](https://github.com/russmatney/clawe/commit/f27820f)) tmux: update misc tmux/fire usage - Russell Matney
- ([`e607c5b`](https://github.com/russmatney/clawe/commit/e607c5b)) feat: ralphie.tmux overhaul - Russell Matney

  > tmux/fire now creates a session if it doesn't exist, and optionally
  > interrupts a running process or creates a new pane for the desired
  > command.
  > 
  > Also, introspection has been rewritten to work with tmux's FORMATS
  > approach - (list-panes) now gets a slew of details from tmux, which is
  > read in via edn/read-string. Next we'll have to reduce these panes into
  > windows and sessions, then attach them to workspaces.

- ([`077517e`](https://github.com/russmatney/clawe/commit/077517e)) fix: include drun in main rofi - Russell Matney

  > Also adds an icon for devhub

- ([`efdfa44`](https://github.com/russmatney/clawe/commit/efdfa44)) sxhkd: remove rc file, which is generated and ignored - Russell Matney
- ([`f8641d2`](https://github.com/russmatney/clawe/commit/f8641d2)) bb: aliases for tail/log commands - Russell Matney

  > Helps the tab completion when you can type it any way you want.

- ([`85c0b7a`](https://github.com/russmatney/clawe/commit/85c0b7a)) wip: godot-repo-actions shell - Russell Matney

### 18 Mar 2022

- ([`1542981`](https://github.com/russmatney/clawe/commit/1542981)) fix: git clone - Russell Matney
- ([`16ac5ec`](https://github.com/russmatney/clawe/commit/16ac5ec)) chore: delete repo workspaces - Russell Matney

  > Unless these are modifying/customizing a workspace repo, there's no need
  > for them any more - they can be installed via `install-workspace` via
  > rofi.

- ([`b4a7d7f`](https://github.com/russmatney/clawe/commit/b4a7d7f)) wip: misc ink toying - Russell Matney

  > also pulls in the advent of code utils for reading local files

- ([`7cb5a7c`](https://github.com/russmatney/clawe/commit/7cb5a7c)) fix: nil-punning for workspaces without dirs - Russell Matney
- ([`7affc42`](https://github.com/russmatney/clawe/commit/7affc42)) fix: remove dead funcs - Russell Matney
- ([`caa717c`](https://github.com/russmatney/clawe/commit/caa717c)) fix: update current-workspace to read from the db - Russell Matney

  > Also fixes the 'install-all' option when installing workspaces.

- ([`90c2030`](https://github.com/russmatney/clawe/commit/90c2030)) feat: support dynamic workspace creation - Russell Matney

  > install-workspaces now adds workspaces to the datalevin db, based on
  > selection via rofi.

- ([`19dacc8`](https://github.com/russmatney/clawe/commit/19dacc8)) m-x: add bb tasks for the current workspace to m-x - Russell Matney

  > clawe m-x now includes bb tasks for the current workspace - if selected
  > they are fired via tmux.

- ([`d2a0b7e`](https://github.com/russmatney/clawe/commit/d2a0b7e)) feat: parse task cmd and description for `bb tasks` output - Russell Matney

  > A helper for collecting available bb tasks in a dir.

- ([`eabda5e`](https://github.com/russmatney/clawe/commit/eabda5e)) rofi: support markup for :rofi/description - Russell Matney

  > If a :rofi/label and :rofi/description are included as strings, rofi
  > will now convert them to better looking rofi markup.

- ([`b4d7334`](https://github.com/russmatney/clawe/commit/b4d7334)) feat: include open-workspace in m-x command - Russell Matney

  > M-x is still performing well! nice to pull these rofi-s up a level - one
  > place to search for most everything.

- ([`43481f5`](https://github.com/russmatney/clawe/commit/43481f5)) feat: db-based workspace support - Russell Matney

  > Generates awesome rules at awm-config-write time rather than requiring
  > it when defining a workspace.
  > 
  > Improves support for dumping workspace data into datalevin by for keys
  > with basic (storable) types - could improve more, but this is enough for
  > now.

- ([`514bc9c`](https://github.com/russmatney/clawe/commit/514bc9c)) feat: awm print date, restart topbar, restore picom - Russell Matney

  > Some convenient helpers for startup time.

- ([`68802a5`](https://github.com/russmatney/clawe/commit/68802a5)) workspaces: more hacking on workspace gen - Russell Matney

  > Not quite there - not writing awm rules from database workspaces yet.

- ([`56d03d2`](https://github.com/russmatney/clawe/commit/56d03d2)) feat: include clone suggestions in m_x - Russell Matney

  > Adds clone suggestions from open tabs and the clipboard to clawe m-x.

- ([`b1c6533`](https://github.com/russmatney/clawe/commit/b1c6533)) workspaces: hacking on generating workspace defs - Russell Matney
- ([`4aeab53`](https://github.com/russmatney/clawe/commit/4aeab53)) zsh: expand-many helper - Russell Matney

  > makes an assumption about no spaces in filenames. so, ya know, plz.


### 17 Mar 2022

- ([`0275863`](https://github.com/russmatney/clawe/commit/0275863)) noise: disable awm garbage notif - Russell Matney

  > Also cuts to 30 wallpapers - scrolling still slow.
  > 
  > Plus some ralphie.git comment toying


### 12 Mar 2022

- ([`124c0b0`](https://github.com/russmatney/clawe/commit/124c0b0)) feat: quick lock impl via i3lock-slick - Russell Matney

  > https://github.com/timvisee/i3lock-slick

- ([`6905a38`](https://github.com/russmatney/clawe/commit/6905a38)) feat: add org-todos to events - Russell Matney
- ([`87a2ca9`](https://github.com/russmatney/clawe/commit/87a2ca9)) feat: events with tick timestamps - Russell Matney

  > Somehow missed the write handlers when i added the read ones. :/

- ([`7f7a0e9`](https://github.com/russmatney/clawe/commit/7f7a0e9)) feat: screenshots leaving via events stream - Russell Matney

  > Adding the tick timestamps causes defhandler/defstream to silently
  > fail... or maybe the error just doesn't show up in the cider repl.

- ([`ce9e79d`](https://github.com/russmatney/clawe/commit/ce9e79d)) wip: trying to get events to hit the frontend - Russell Matney

  > Not sure where things are getting lost :/

- ([`d7bfef7`](https://github.com/russmatney/clawe/commit/d7bfef7)) feat: parsing screenshot timestrings - Russell Matney

  > Updates a bunch of deps. Went through hell trying to figure out the tick
  > format string, as usual.

- ([`f3c0c6d`](https://github.com/russmatney/clawe/commit/f3c0c6d)) refactor: wallpapers views, ui, api cleanup - Russell Matney

  > Same as the screenshots clean up - it's more ergonomic to work in clj
  > and cljs files.

- ([`8d8dd95`](https://github.com/russmatney/clawe/commit/8d8dd95)) refactor: screenshots view split into ui, api - Russell Matney

  > doctor.ui namespaces set up streams, handlers, and use-x helpers for
  > doctor frontend components, and mostly cljc files. doctor.api is
  > backend (clj) files that support the ui cljc files. doctor.ui.views are
  > cljs files that consume the streams/handlers/use-x funcs from doctor.ui.

- ([`a867381`](https://github.com/russmatney/clawe/commit/a867381)) feat: copy-pasta doctor-popup to doctor-todo - Russell Matney

  > This is how much it takes to add a custom tauri scratchpad right now.
  > Note that the sxhkd bit is generated when clawe is restarted, should
  > probably be gitignored.

- ([`3abd1c6`](https://github.com/russmatney/clawe/commit/3abd1c6)) feat: one-click reload in the topbar - Russell Matney

  > simples.

- ([`3cec25e`](https://github.com/russmatney/clawe/commit/3cec25e)) refactor: clean up doctor tauri app tasks - Russell Matney
- ([`4aa614f`](https://github.com/russmatney/clawe/commit/4aa614f)) feat: doctor-popup as scratchpad workspace - Russell Matney

  > Consuming the expanded tmux and scratchpad feats, this execs the bb.edn
  > task in the workspace's tmux session. Could probably assume the
  > workspace tmux session is the desired one in workspace/create at some
  > point. This might end up being a better method than the bb/process and
  > gtk launch one - the tmux sessions could serve as simple logging for the
  > scratchpad apps, and make it easy to kill and restart them by hand.

- ([`b7defd7`](https://github.com/russmatney/clawe/commit/b7defd7)) feat: notify when backend server restarts - Russell Matney

  > A notif to save the trouble of following logs until startup is complete.

- ([`da5dff3`](https://github.com/russmatney/clawe/commit/da5dff3)) feat: scratchpad client-lookup now checks names too - Russell Matney

  > Scratchpads have used 'class'es to check for existing apps - now we can
  > match on a specific name as well, which works better for tauri apps.

- ([`9c0773c`](https://github.com/russmatney/clawe/commit/9c0773c)) fix: support :workspace/exec as :tmux/fire description - Russell Matney

  > :workspace/exec is fired upon creation/opening of a workspace - this
  > expands the 'string' support to handle a tmux-fire as well, so that the
  > tmux session and window can be specified as well.

- ([`adda4fd`](https://github.com/russmatney/clawe/commit/adda4fd)) fix: remove some hard-coded doctor-popup - Russell Matney

  > Also remove logging from awesome/tags

- ([`afca135`](https://github.com/russmatney/clawe/commit/afca135)) feat: tmux/fire now supporting {:tmux/fire 'echo 'hi''} - Russell Matney

  > A data-driven description of the tmux/fire command - makes for simpler
  > usage in defworkspace/defapp, b/c we don't have to store a function
  > itself, only the description of it.

- ([`c1237ed`](https://github.com/russmatney/clawe/commit/c1237ed)) feat: expand the bb.edn tauri helpers, add docs - Russell Matney
- ([`c579c7f`](https://github.com/russmatney/clawe/commit/c579c7f)) fix: bb topbar command updated - Russell Matney

  > Does require a `cargo tauri dev` to build it the first time.


### 11 Mar 2022

- ([`384f330`](https://github.com/russmatney/clawe/commit/384f330)) wip: tauri creating windows via cli - Russell Matney

### 10 Mar 2022

- ([`eb0c0d4`](https://github.com/russmatney/clawe/commit/eb0c0d4)) fix: better defcom linting, misc linter errors - Russell Matney
- ([`7db1974`](https://github.com/russmatney/clawe/commit/7db1974)) feat: ported rest of ralphie.awesome to `fnl` macro - Russell Matney

### 7 Mar 2022

- ([`a6bf14c`](https://github.com/russmatney/clawe/commit/a6bf14c)) refactor: porting more to `fnl`, misc clean up - Russell Matney
- ([`a640ee2`](https://github.com/russmatney/clawe/commit/a640ee2)) refactor: ported more awm fetchers, unit-test defcom - Russell Matney
- ([`40f55cf`](https://github.com/russmatney/clawe/commit/40f55cf)) refactor: port fetchers to fnl, parse booleans - Russell Matney

  > Includes more unit tests for ralphie.awesome


### 4 Mar 2022

- ([`cbddadc`](https://github.com/russmatney/clawe/commit/cbddadc)) wip: extending fnl macro to merge do blocks - Russell Matney
- ([`a35243d`](https://github.com/russmatney/clawe/commit/a35243d)) fix: add no-ops to easy_async funcs - Russell Matney

  > The nils or omitted callback functions here were throwing errors in the
  > awm logs, and sometimes notifying with ugly exceptions - adding no-ops
  > silences those for now.


### 27 Feb 2022

- ([`476bd48`](https://github.com/russmatney/clawe/commit/476bd48)) misc: more awesome tests, bb clj-kondo, etc - Russell Matney
- ([`dc06cb8`](https://github.com/russmatney/clawe/commit/dc06cb8)) feat: run unit tests before rebuilding clawe - Russell Matney

  > Finally getting some basic protection in place - this throws an error
  > before rebuilding the clawe uberjar if the unit tests are failing.
  > 
  > Also adds a bb test-unit command and a basic test_runner.clj, from the
  > bb book recipe.

- ([`c708282`](https://github.com/russmatney/clawe/commit/c708282)) chore: ralphie awesome docs and general clean up - Russell Matney
- ([`2a306e7`](https://github.com/russmatney/clawe/commit/2a306e7)) feat: reasonable `fnl` macro - Russell Matney

  > Allows you to write raw-fnl with unquotes for local values - wraps your
  > fnl expressions in a do blocks and pipes it into awesome-cli.


### 26 Feb 2022

- ([`c0db236`](https://github.com/russmatney/clawe/commit/c0db236)) wip: backtick impl - Russell Matney
- ([`5b7e1f4`](https://github.com/russmatney/clawe/commit/5b7e1f4)) fix: unqualify awm symbols - Russell Matney

  > A tricky bug! This is just the kind of nonsense i hope to avoid with the
  > new `fnl` macro.


### 25 Feb 2022

- ([`7d404b5`](https://github.com/russmatney/clawe/commit/7d404b5)) wip: towards an awm-$ fennel macro - Russell Matney
- ([`74fa115`](https://github.com/russmatney/clawe/commit/74fa115)) bb.edn: log-awesome helper - Russell Matney
- ([`4caac2b`](https://github.com/russmatney/clawe/commit/4caac2b)) wip: improved quoting for awm-fnl usage - Russell Matney

  > Starting to work through better awm-fnl - syntax quoting works in some
  > cases, but breaks when symbols get namespace-qualified. Trying to find
  > some nice way to support simple fennel usage.

- ([`0d4fa3a`](https://github.com/russmatney/clawe/commit/0d4fa3a)) fix: move tests off of defworkspace key - Russell Matney

  > The defthings tests, when evaluated, were impacting the web workspace
  > definition b/c of a name collision. So that's weird.
  > 
  > Also removes a circular dep.

- ([`8a1b7c4`](https://github.com/russmatney/clawe/commit/8a1b7c4)) fix: handle expo issues - Russell Matney

  > Missing and outdated deps (datahike, tick) was causing issues with the
  > expo namespaces, which impact full-project scans for fallbacks like
  > +lookup/references. it's likely these expo namespaces now have errors
  > b/c of tick's api updates.

- ([`6b16302`](https://github.com/russmatney/clawe/commit/6b16302)) feat: toggle darker topbar background - Russell Matney

  > Makes lighter wallpapers work better


### 20 Feb 2022

- ([`bf6a7b9`](https://github.com/russmatney/clawe/commit/bf6a7b9)) feat: misc handling for streaming - Russell Matney
- ([`255e0d5`](https://github.com/russmatney/clawe/commit/255e0d5)) feat: better defkbd rofi label - Russell Matney

  > Extending defthing/defkbd with a few more helpers and a ->rofi function.
  > Moves dwim to m-x, which is a better namespace, b/c what i had was not
  > dwim, it was just a list of functions. dwim should be suggesting/picking
  > one based on the context.


### 4 Feb 2022

- ([`f23ce4c`](https://github.com/russmatney/clawe/commit/f23ce4c)) fix: obs pulls it's own client - Russell Matney
- ([`e1d94a6`](https://github.com/russmatney/clawe/commit/e1d94a6)) fix: handle streaming chat client - Russell Matney

  > Creates tauri window, adds awm rules, adjusts some sizing/float toggle
  > bindings.


### 15 Jan 2022

- ([`a4ea14d`](https://github.com/russmatney/clawe/commit/a4ea14d)) feat: including today, yesterday in doctor todos list - Russell Matney

  > Now parsing todos from today/yesterday daily garden files.
  > 
  > Includes some deps updates and parsing of two created-at formats, which
  > then failed to flow through transit... not sure why.

- ([`0ea8e21`](https://github.com/russmatney/clawe/commit/0ea8e21)) deps: remove react-chrono - Russell Matney

  > Hit some strange vips/sharp build fails, this was just a dep to delete.
  > Eventually things just worked? Not sure exactly what happened.

- ([`e5ac3b2`](https://github.com/russmatney/clawe/commit/e5ac3b2)) feat: bb stop-doctor command - Russell Matney

  > For stopping all doctor services

- ([`f96fe2f`](https://github.com/russmatney/clawe/commit/f96fe2f)) feat: break out ui.todos helpers - Russell Matney

  > Moving views to pure .cljs, purely preferential.


### 14 Jan 2022

- ([`1533ec0`](https://github.com/russmatney/clawe/commit/1533ec0)) feat: workspace db crud and :display-name support - Russell Matney

  > Now supporting updating workspace names on the fly. Click in the topbar
  > workspace cell to edit in-place - this will create a workspace entry in
  > the datalevin db on-blur. db workspaces are merged into existing
  > workspaces or on-the-fly awm tags.
  > 
  > Still plenty of issues - the on-the-fly awm workspaces move around b/c
  > their names change when their index changes.... need a better way to mix
  > this data.


### 9 Jan 2022

- ([`f4841ca`](https://github.com/russmatney/clawe/commit/f4841ca)) feat: restart systemctl units via rofi - Russell Matney
- ([`b935ae5`](https://github.com/russmatney/clawe/commit/b935ae5)) zsh: toying with history command here - Russell Matney

  > The zsh history parser breaks when some nonsense ends up in the zsh
  > history - now it's try/catch wrapped.
  > 
  > Also moves to a better default handling.

- ([`4cf6be4`](https://github.com/russmatney/clawe/commit/4cf6be4)) feat: reload doom env in clawe restart - Russell Matney

  > Also adds a `fire` alias to ralphie.emacs/eval-form

- ([`ba2f45a`](https://github.com/russmatney/clawe/commit/ba2f45a)) keys: centering a bit slower, but it buries the rest now - Russell Matney

### 8 Jan 2022

- ([`1991ce0`](https://github.com/russmatney/clawe/commit/1991ce0)) wip: attempts to get rust to work - Russell Matney

  > futile.

- ([`20ad1e3`](https://github.com/russmatney/clawe/commit/20ad1e3)) fix: move popup workspace details to doctor workspaces view - Russell Matney
- ([`2e45e98`](https://github.com/russmatney/clawe/commit/2e45e98)) fix: restore workspaces doctor app - Russell Matney

  > moves to relying on the shared doctor.ui.workspaces helpers.

- ([`0901766`](https://github.com/russmatney/clawe/commit/0901766)) feat: include unmatched tags in all-workspaces - Russell Matney

  > Tags are created on the fly in a few cases, as a fallback to allow
  > clients to be usable. One method is to launch a terminal when not
  > attached to any workspace (move to a scratchpad and then toggle it off).
  > 
  > This updates the clawe.workspaces/all-workspaces function to create
  > pseudo-workspaces for tags that didn't match a passed workspace. The
  > result is seeing the temp tags in topbar, and supporting deleting those
  > extra tags in clean-up commands.
  > 
  > These pseudo-workspaces are pretty handy - probably a good starting
  > place for on-the-fly workspace creation. Perhaps the doctor workspaces
  > widget should support changing these workspace names and setting other
  > details, like a workspace directory/repo, initial file, todo sources,
  > etc. All that requires reading/merging workspaces from the clawe.db.

- ([`a897b29`](https://github.com/russmatney/clawe/commit/a897b29)) tasks: expanded bb.edn with helpers and logging - Russell Matney

### 7 Jan 2022

- ([`7b08bef`](https://github.com/russmatney/clawe/commit/7b08bef)) fix: bb dev-deps, note about required symlinks - Russell Matney
- ([`11befd6`](https://github.com/russmatney/clawe/commit/11befd6)) refactor: remove 'dock' usage everywhere - Russell Matney
- ([`e3a6b2d`](https://github.com/russmatney/clawe/commit/e3a6b2d)) fix: journal workspace grabbing tauri client - Russell Matney
- ([`3b7cf82`](https://github.com/russmatney/clawe/commit/3b7cf82)) fix: restore web usage - Russell Matney

  > Can't run tauri from web :/

- ([`1a77c62`](https://github.com/russmatney/clawe/commit/1a77c62)) chore: clj-kondo hooks for plasma - Russell Matney
- ([`ec60c21`](https://github.com/russmatney/clawe/commit/ec60c21)) feat: popup toggling via tauri - Russell Matney

  > A prototype of a togglable tauri popup, via buttons added to the topbar
  > and home page. Some shared-state issues, but tons of progress toward
  > things behaving reasonably well.


### 6 Jan 2022

- ([`83ce85c`](https://github.com/russmatney/clawe/commit/83ce85c)) wip: tauri api toying - Russell Matney
- ([`005177a`](https://github.com/russmatney/clawe/commit/005177a)) feat: topbar running via tauri - Russell Matney

  > Finally successfully working with the tauri js api. Key error was using
  > absolute urls in the window config.

- ([`7141fda`](https://github.com/russmatney/clawe/commit/7141fda)) refactor: move start-*.sh to bb tasks - Russell Matney

  > These scripts seem to get the zsh env for free, which was an advantage.
  > might move back to them at some point.

- ([`1b19528`](https://github.com/russmatney/clawe/commit/1b19528)) wip: copy in expo - Russell Matney

  > This should get refactored/minimized/absorbed by doctor, or vice versa.

- ([`fe09e7b`](https://github.com/russmatney/clawe/commit/fe09e7b)) fix: update all deps, fix tick breaking changes - Russell Matney

  > Also had to adjust the aliases for the doctor/shadow builds.

- ([`3076ced`](https://github.com/russmatney/clawe/commit/3076ced)) feat: pull in all of doctor - Russell Matney

  > Mixing with a full-stack app now...

- ([`7541566`](https://github.com/russmatney/clawe/commit/7541566)) feat: completely pull in ralphie - Russell Matney
- ([`07fe29d`](https://github.com/russmatney/clawe/commit/07fe29d)) feat: port chess ns - Russell Matney
- ([`d6e899a`](https://github.com/russmatney/clawe/commit/d6e899a)) feat: include bb/fs and rewrite-clj kondo helpers - Russell Matney
- ([`658154a`](https://github.com/russmatney/clawe/commit/658154a)) fix: clawe-install command now working - Russell Matney

  > Was unnecessarily slurping after setting the :out to a :string.

- ([`043604d`](https://github.com/russmatney/clawe/commit/043604d)) feat: pull defthing into clawe - Russell Matney

  > Moving toward a monorepo style to cut out the overhead and practice what
  > i preach.


### 30 Dec 2021

- ([`f27920b`](https://github.com/russmatney/clawe/commit/f27920b)) fix: handle wallpaper filenames with spaces - Russell Matney

### 22 Dec 2021

- ([`ff129c3`](https://github.com/russmatney/clawe/commit/ff129c3)) kbds: simpler focus movement - Russell Matney

### 21 Dec 2021

- ([`4406fd2`](https://github.com/russmatney/clawe/commit/4406fd2)) fix: misc rules, workspaces, fixes - Russell Matney

### 17 Dec 2021

- ([`4424d68`](https://github.com/russmatney/clawe/commit/4424d68)) feat: reload wallpaper in restart, handle :skip-count in set-wallpaper - Russell Matney

### 14 Dec 2021

- ([`d2e6e94`](https://github.com/russmatney/clawe/commit/d2e6e94)) fix: support chrome as an independent dev-browser - Russell Matney

  > Rather than depend on a ff-dev edition in the flow, the dev-browser
  > workspace will now use either or both, depending on what is open.


### 31 Oct 2021

- ([`ad5d3ae`](https://github.com/russmatney/clawe/commit/ad5d3ae)) fix: misc godot tweaks - Russell Matney

  > Godot's editor misbehaves - for some reason it's a struggle to get it to
  > tile. This fix sort of works on my laptop, but requires restarting
  > awesome after starting up godot....
  > 
  > This hasn't been an issue on my desktop at all :/


### 3 Oct 2021

- ([`4673738`](https://github.com/russmatney/clawe/commit/4673738)) feat: closer to full-height topbar - Russell Matney

### 17 Sep 2021

- ([`77fa88a`](https://github.com/russmatney/clawe/commit/77fa88a)) fix: local repo upstream label fix - Russell Matney

### 6 Sep 2021

- ([`fc215bd`](https://github.com/russmatney/clawe/commit/fc215bd)) fix: fix workspace indexes in clawe rules - Russell Matney

### 5 Sep 2021

- ([`7ebd3e9`](https://github.com/russmatney/clawe/commit/7ebd3e9)) chore: misc debugging in user.clj - Russell Matney

### 4 Sep 2021

- ([`d4dae6e`](https://github.com/russmatney/clawe/commit/d4dae6e)) refactor: workspace indexes working as expected again - Russell Matney

  > These had relied on the awm workspaces widget - now they update via
  > clawe.

- ([`44b9e5e`](https://github.com/russmatney/clawe/commit/44b9e5e)) feat: misc resizing - huge topbar, smaller gaps - Russell Matney
- ([`714b009`](https://github.com/russmatney/clawe/commit/714b009)) chore: remove more dead awm config - Russell Matney

  > Good bye to the widgets, the dashboard, some other helpers.

- ([`fba4fb5`](https://github.com/russmatney/clawe/commit/fba4fb5)) feat: slightly larger top bar - Russell Matney

### 3 Sep 2021

- ([`55a077d`](https://github.com/russmatney/clawe/commit/55a077d)) docs: clears old todos - Russell Matney
- ([`e49fac3`](https://github.com/russmatney/clawe/commit/e49fac3)) feat: deleting most of bar.fnl - Russell Matney

  > Down to just a systray and some top/bottom margins in here

- ([`a8fb77e`](https://github.com/russmatney/clawe/commit/a8fb77e)) feat: update to support new doctor/topbar - Russell Matney
- ([`c2e4e40`](https://github.com/russmatney/clawe/commit/c2e4e40)) feat: db/transact removes null vals before - Russell Matney

  > This crashes if you try to set some attr to nil - here we just remove
  > keys from maps if the passed val was nil.

- ([`b4e408b`](https://github.com/russmatney/clawe/commit/b4e408b)) fix: no longer using awesome for wallpapers - Russell Matney
- ([`50bf905`](https://github.com/russmatney/clawe/commit/50bf905)) feat: set wallpaper via feh, store in clawe db - Russell Matney

  > Sets the time used, the used-count.

- ([`1e52bd1`](https://github.com/russmatney/clawe/commit/1e52bd1)) fix: better key name for screenshot :file - Russell Matney
- ([`fe4599d`](https://github.com/russmatney/clawe/commit/fe4599d)) feat: initial wallpapers namespace - Russell Matney

### 29 Aug 2021

- ([`09bfa1d`](https://github.com/russmatney/clawe/commit/09bfa1d)) fix: per garden workspaces avoid 'garden' rules match - Russell Matney
- ([`d4d3af9`](https://github.com/russmatney/clawe/commit/d4d3af9)) refactor: dry up toggle-client code - Russell Matney

  > This has been irking me for a long time - yet another rendition of this
  > logic, but at least a bit drier for these three consumers now.

- ([`b259374`](https://github.com/russmatney/clawe/commit/b259374)) fix: fallback readme path for :workspace/initial-file - Russell Matney
- ([`7b47e41`](https://github.com/russmatney/clawe/commit/7b47e41)) fix: revert binding switch - Russell Matney

  > this change exhausted me

- ([`0e42313`](https://github.com/russmatney/clawe/commit/0e42313)) feat: clears dead bottom bar code - Russell Matney

### 28 Aug 2021

- ([`9efbecd`](https://github.com/russmatney/clawe/commit/9efbecd)) fix: adds apparently crucial workspace-repo to journal scratchpad - Russell Matney

  > refactors workspaces/create a bit. Should probably go away, but it looks
  > like it has 3 deps, so maybe it's isolated on purpose.

- ([`1c4a419`](https://github.com/russmatney/clawe/commit/1c4a419)) refactor: drop local-repo, use workspace-repo everywhere - Russell Matney
- ([`ad7345e`](https://github.com/russmatney/clawe/commit/ad7345e)) feat: pull defkbd, defworkspace, defapp out - Russell Matney

  > Rips the macros out and moves them to defthing.

- ([`6ea32f1`](https://github.com/russmatney/clawe/commit/6ea32f1)) feat: new wallpaper, cleaner top bar - Russell Matney
- ([`85a2bc2`](https://github.com/russmatney/clawe/commit/85a2bc2)) feat: mod+v for a tall centered client - Russell Matney
- ([`cff2a29`](https://github.com/russmatney/clawe/commit/cff2a29)) fix: remove garden scratchpad treatment - Russell Matney

### 21 Aug 2021

- ([`2e9d08c`](https://github.com/russmatney/clawe/commit/2e9d08c)) refactor: rearrange cycle tag/client/layout keybindings - Russell Matney
- ([`e796dc7`](https://github.com/russmatney/clawe/commit/e796dc7)) feat: use volume labels, update dock on change - Russell Matney

### 15 Aug 2021

- ([`b779868`](https://github.com/russmatney/clawe/commit/b779868)) feat: misc godot toying - open-godot-file, attempted reload - Russell Matney

  > The reload doesn't quite work - trying to get a binding to fake another
  > one within godot's editor.


### 8 Aug 2021

- ([`21fa0b9`](https://github.com/russmatney/clawe/commit/21fa0b9)) defs: new workspace - Russell Matney

### 7 Aug 2021

- ([`9abd83f`](https://github.com/russmatney/clawe/commit/9abd83f)) feat: require defs.local.workspaces in workspaces.clj - Russell Matney

  > Moves the require for defs.local.workspaces to workspaces.clj, so that
  > consuming libraries don't need to require clawe.core or all workspace
  > namespaces independently.

- ([`195e09e`](https://github.com/russmatney/clawe/commit/195e09e)) feat: correct all clients - Russell Matney

  > A different approach to getting clients and tags to behave. The
  > :rules/apply stuff can probably be deleted - committing here as its
  > behaving pretty well.


### 31 Jul 2021

- ([`8795b48`](https://github.com/russmatney/clawe/commit/8795b48)) refactor: consume awm-fnl and new client/tag keys - Russell Matney

  > Updates the client and tag key consumers, ports clawe.awesome to
  > ralphie.awesome, with a few remaining exceptions.

- ([`634537b`](https://github.com/russmatney/clawe/commit/634537b)) feat: notify visible clients when cycling focus - Russell Matney

  > Improving debugging for some toggle issues.
  > This is precisely where an api offering :after-hooks would maintain
  > zero-overhead through commands like these.

- ([`de845f9`](https://github.com/russmatney/clawe/commit/de845f9)) feat: more git-status repos, update dock on mute toggle - Russell Matney

### 25 Jul 2021

- ([`6ae47bf`](https://github.com/russmatney/clawe/commit/6ae47bf)) feat: remove screenshot limit - Russell Matney
- ([`3b5f3c1`](https://github.com/russmatney/clawe/commit/3b5f3c1)) misc: bg change, misc cleanup - Russell Matney

### 24 Jul 2021

- ([`65f6c8f`](https://github.com/russmatney/clawe/commit/65f6c8f)) fix: cycle focus, update dock on client focus signal - Russell Matney

  > misc rules cleanup.

- ([`424d7ea`](https://github.com/russmatney/clawe/commit/424d7ea)) feat: better toggles for pixels, 1pass - Russell Matney
- ([`3c5c9ce`](https://github.com/russmatney/clawe/commit/3c5c9ce)) feat: toggle scratchpad - apply rules if client found elsewhere - Russell Matney

  > Often a client ends up on the wrong tag, and the current fix is a
  > nuclear cmd-r, which happens to reapply the :rules/apply fns. This adds
  > a bit of logic to the toggle-scratchpad function to apply the function
  > for that workspace if the client is found in another workspace. This
  > happens most often for firefoxdeveloperedition, which ends up on the web
  > tag b/c the 'firefox' rule grabs it.


### 20 Jul 2021

- ([`de225e4`](https://github.com/russmatney/clawe/commit/de225e4)) fix: add space to sxhkd commands - Russell Matney

  > To keep bindings out of zsh history.


### 18 Jul 2021

- ([`5ecc296`](https://github.com/russmatney/clawe/commit/5ecc296)) feat: ensure dock update on tag deletion - Russell Matney

  > via property::screen.

- ([`58e58b5`](https://github.com/russmatney/clawe/commit/58e58b5)) feat: attach tag/client signals to dock updates - Russell Matney
- ([`53530c1`](https://github.com/russmatney/clawe/commit/53530c1)) feat: reloading notifications and doctor dock - Russell Matney
- ([`93c2b26`](https://github.com/russmatney/clawe/commit/93c2b26)) feat: ensure sxhkd tmux session - Russell Matney

  > Quick improvement to start up. Could also wrap something like this into
  > the sxhkd systemctl startup, which might be more resilient in general,
  > but this was easy.

- ([`53dd85e`](https://github.com/russmatney/clawe/commit/53dd85e)) fix: restore apparently vital firefox rule - Russell Matney

### 17 Jul 2021

- ([`89e4722`](https://github.com/russmatney/clawe/commit/89e4722)) feat: sort latest first, update after taking screenshots - Russell Matney
- ([`222b9c9`](https://github.com/russmatney/clawe/commit/222b9c9)) feat: gitops workspace - Russell Matney
- ([`0b2599f`](https://github.com/russmatney/clawe/commit/0b2599f)) feat: screenshots namespace - Russell Matney

  > just listing from ~/Screenshots for now


### 16 Jul 2021

- ([`1a00ff0`](https://github.com/russmatney/clawe/commit/1a00ff0)) feat: handle doctor-dock on clawe - Russell Matney

### 6 Jul 2021

- ([`c5875ce`](https://github.com/russmatney/clawe/commit/c5875ce)) fix: hide bottom bar - Russell Matney
- ([`971aca1`](https://github.com/russmatney/clawe/commit/971aca1)) feat: dock update hooks on kbds, rules for dock - Russell Matney

### 4 Jul 2021

- ([`41b3406`](https://github.com/russmatney/clawe/commit/41b3406)) feat: move volume control to sxhkd - Russell Matney
- ([`c1c03b0`](https://github.com/russmatney/clawe/commit/c1c03b0)) fix: support sxhkd period, comma keynames - Russell Matney
- ([`b9420e6`](https://github.com/russmatney/clawe/commit/b9420e6)) feat: toggle-terminal via sxhkd again - Russell Matney
- ([`7f109b4`](https://github.com/russmatney/clawe/commit/7f109b4)) feat: run sxhkd in tmux/bg - Russell Matney

  > bg_shell should be set to SXHKD_SHELL somewhere for your sxhkd daemon to
  > consume - this allows commands to belong to tmux rather than sxhkd,
  > which allows restarting sxhkd without killing running processes (like
  > alacritty instances). some rough notes:
  > 
  > largely pulled from 'examples/background_shell' in baskerville/sxhkd git repo
  > 
  > systemd unit in ~/.config/systemd/user/sxhkd.service:
  > note environment variable set!
  > 
  > /#+begin_src
  > [Unit]
  > Description=Simple X Hotkey Daemon
  > Documentation=man:sxhkd(1)
  > 
  > [Service]
  > ExecStart=/usr/bin/sxhkd
  > ExecReload=/usr/bin/kill -SIGUSR1 $MAINPID
  > Environment=SXHKD_SHELL=/home/russ/.local/bin/bg_shell
  > 
  > [Install]
  > WantedBy=graphical.target
  > /#+end_src
  > 
  > xinitrc or something similar:
  > /#+begin_src
  > if ! tmux has-session -t sxhkd 2> /dev/null ; then
  > 	tmux new-session -s sxhkd -d
  > 	printf '%s
  > ' 'echo 'hello world'' | tmux load-buffer -
  > 	tmux paste-buffer -t sxhkd
  > fi
  > /#+end_src
  > 
  > bg_shell script itself:
  > /#+begin_src
  > /#! /bin/sh
  > 
  > printf '(%s)&!
  > ' '$2' | tmux load-buffer -
  > tmux paste-buffer -t sxhkd
  > /#+end_src


### 3 Jul 2021

- ([`c4745c3`](https://github.com/russmatney/clawe/commit/c4745c3)) fix: move toggle terminal back to awm for now - Russell Matney

  > Now it survives restarts but drops calls (b/c of existing awm call
  > dropping - should maybe just remove that.)

- ([`3de676d`](https://github.com/russmatney/clawe/commit/3de676d)) wip: sxhkd nohup attempt - Russell Matney

  > Trying to get alacritty to survive clawe restarts.

- ([`75310fd`](https://github.com/russmatney/clawe/commit/75310fd)) feat: pull non-awm bindings into sxhkd config - Russell Matney

  > Moves defkbd impl into clawe.bindings.
  > 
  > Adds clawe/sxhkd/bindings.clj for writing sxhkdrc and restarting sxhkd
  > via system-ctl. Some problems: sxhkd is now the parent process for
  > non-client server apps like alacritty. Awm can be opted back into via
  > {:binding/awm true} or by impling :binding/raw-fnl (calling awm/awm-fnl).
  > Otherwise sxhkd is now the default keybinding for execing a
  > clawe/bindings/defkbd command.


### 27 Jun 2021

- ([`e44e821`](https://github.com/russmatney/clawe/commit/e44e821)) feat: optimize workspace update function - Russell Matney

  > This thing was much slower than needed - this reduces the loops and
  > commands to a minimum.

- ([`42ea3cb`](https://github.com/russmatney/clawe/commit/42ea3cb)) fix: support dwim when on tags without workspaces - Russell Matney
- ([`997cd4e`](https://github.com/russmatney/clawe/commit/997cd4e)) feat: remove dead item ns - Russell Matney

### 26 Jun 2021

- ([`eb84674`](https://github.com/russmatney/clawe/commit/eb84674)) feat: awm rules/apply now creates workspaces when clients are found - Russell Matney

  > Might not always want this, but it's occurred to be numerous times as a
  > cheaper way to fix things. Right now i spend too much time creating a
  > target workspace after a client has been created.

- ([`18b67c0`](https://github.com/russmatney/clawe/commit/18b67c0)) fix: update-widgets after clearing dead wsps - Russell Matney
- ([`17efba0`](https://github.com/russmatney/clawe/commit/17efba0)) perf: shell functions with no clawe overhead - Russell Matney

  > moves several commands from $/bb-process usage to a pure fennel
  > implementation via awm-fnl and awful.spawn.easy_async. keybindings that
  > invoke (awm/awm-fnl exp) directly don't pay clawe's or awesome-client's
  > startup cost, as the exp is pasted directly into the awm-keybinding defn.

- ([`c8c301e`](https://github.com/russmatney/clawe/commit/c8c301e)) feat: defkbds with quoted-fennel write pure awm kbds - Russell Matney

  > When a defkbd's final xorf is a call to `(awm/awm-fnl
  > 'some-fennel-string'),
  > the string is printed as the keybinding in
  > awesome/clawe-bindings.fnl. Normally kbds print as spawn-fn calls that
  > execute clawe itself, but now we're priting pure awesome functions as
  > well. This makes a performance difference for commands that could be to
  > the metal (or at least skipping the overhead of running clawe's
  > defcom/run command).

- ([`13b59dc`](https://github.com/russmatney/clawe/commit/13b59dc)) refactor: transcribe more bindings from awm into clawe - Russell Matney

  > Only a few ui functions remaining - the movement ones especially will
  > feel the overhead of going through clawe just to move, and are probably
  > better left in awesome, defkbd expands to inject fnl directly.


### 20 Jun 2021

- ([`30aadbc`](https://github.com/russmatney/clawe/commit/30aadbc)) feat: quick zsh completion command - Russell Matney
- ([`d8c522f`](https://github.com/russmatney/clawe/commit/d8c522f)) refactor: drop clawe.defthing, use defthing.core - Russell Matney
- ([`6a3cdab`](https://github.com/russmatney/clawe/commit/6a3cdab)) refactor: move off of ralph.defcom completely - Russell Matney

  > Refactors/deletes existing defcoms to the new defcom.

- ([`ca87296`](https://github.com/russmatney/clawe/commit/ca87296)) feat: refactor bindings to use `defkbd` - Russell Matney

  > `defkbd` is the latest helper for writing key-bindings. It maintains
  > the same features as before, but is backed by the new defcom, and so
  > does not require perfect arity - you can now just pass a form as the
  > last xorf.


### 19 Jun 2021

- ([`895932d`](https://github.com/russmatney/clawe/commit/895932d)) fix: update ralphie namespace usage - Russell Matney
- ([`75f9588`](https://github.com/russmatney/clawe/commit/75f9588)) fix: remove dead microscript code - Russell Matney
- ([`25d4aa9`](https://github.com/russmatney/clawe/commit/25d4aa9)) feat: misc new project workspaces - Russell Matney
- ([`82179d6`](https://github.com/russmatney/clawe/commit/82179d6)) wip: awesome rules window-callback impl - Russell Matney

  > This works, but doesn't do more than log yet. It doesn't seem to fire
  > for all new windows, just some subset, which is annoying. Might look
  > into a non-awesome listener, but this way would be nice to use, as it
  > allows for pretty good control over the input metadata.


### 31 May 2021

- ([`bb8f4a1`](https://github.com/russmatney/clawe/commit/bb8f4a1)) feat: port cycle workspace commands into defs.bindings - Russell Matney
- ([`0af1bd5`](https://github.com/russmatney/clawe/commit/0af1bd5)) docs: notes how viewing logs - Russell Matney

### 30 May 2021

- ([`1380f38`](https://github.com/russmatney/clawe/commit/1380f38)) feat: ensure git status checks for defthing, beu2 - Russell Matney
- ([`4399ba5`](https://github.com/russmatney/clawe/commit/4399ba5)) docs: readme dev note - Russell Matney

### 29 May 2021

- ([`4741f9c`](https://github.com/russmatney/clawe/commit/4741f9c)) feat: change wallpaper, ignore wallpapers dir - Russell Matney
- ([`6895bd5`](https://github.com/russmatney/clawe/commit/6895bd5)) feat: more workspaces - Russell Matney

  > defthing, grid+1, some camsbury

- ([`8ab628f`](https://github.com/russmatney/clawe/commit/8ab628f)) wip: cycle-focus attempt, toggle-workspace cleanup - Russell Matney

### 23 May 2021

- ([`c7922bb`](https://github.com/russmatney/clawe/commit/c7922bb)) feat: initial global rules callback - Russell Matney

  > A step toward moving all rules into clawe workspace definitions....
  > where they already exist, but for now they're expressed as static awm
  > rules. This will give clawe a chance to introspect on client of various
  > types, such as emacs or browsers that might be dedicated to some
  > specific purpose.

- ([`04e9ff5`](https://github.com/russmatney/clawe/commit/04e9ff5)) misc: clean up logging, quiet a few more awm-fnls - Russell Matney
- ([`a28d457`](https://github.com/russmatney/clawe/commit/a28d457)) feat: clean up awesome init, apply clawe rules as last step - Russell Matney

  > Prints the garbage collection counts at init.
  > Skips init_remote for now. This thing always logs an error anyway,
  > so I'm not sure it's actually required... but i'm leaving it for now
  > just in case.

- ([`8320672`](https://github.com/russmatney/clawe/commit/8320672)) fix: remove more dead widgets - Russell Matney
- ([`68a048d`](https://github.com/russmatney/clawe/commit/68a048d)) fix: remove org-clock component - Russell Matney
- ([`b058658`](https://github.com/russmatney/clawe/commit/b058658)) fix: use a proper gears.timer to clear the spawn-fn-cache - Russell Matney

  > This cache prevents keybindings from spamming too aggressively - but
  > when commands don't return properly (e.g. b/c they crash), they don't
  > clear the cache nicely. Here we reduce the timeout and use a proper
  > start_new. The code in place created timer objects that were never
  > started.


### 15 May 2021

- ([`7c8cb36`](https://github.com/russmatney/clawe/commit/7c8cb36)) wip: attempt to ignore some clients - Russell Matney
- ([`2170be2`](https://github.com/russmatney/clawe/commit/2170be2)) feat: bb tasks install command - Russell Matney

### 14 May 2021

- ([`93e0ec2`](https://github.com/russmatney/clawe/commit/93e0ec2)) feat: move from chrome to ff-developer as dev-browser - Russell Matney
- ([`4f34a1f`](https://github.com/russmatney/clawe/commit/4f34a1f)) feat: support finding clients by class - Russell Matney
- ([`f77daca`](https://github.com/russmatney/clawe/commit/f77daca)) fix: disable titlebars by default - Russell Matney

### 9 May 2021

- ([`34fcd94`](https://github.com/russmatney/clawe/commit/34fcd94)) feat: new workspaces, better defworkspace defaults - Russell Matney

  > Now calling `workspace-title` for every defworkspace, and introducing
  > `workspace-repo` that operates on an already set `:workspace/directory`.


### 2 May 2021

- ([`35e0699`](https://github.com/russmatney/clawe/commit/35e0699)) feat: move aseprite to pixels workspace - Russell Matney
- ([`f90cc39`](https://github.com/russmatney/clawe/commit/f90cc39)) feat: mute, volume notifs - Russell Matney

  > Also adds client bindings as comments to def/bindings.

- ([`8b23562`](https://github.com/russmatney/clawe/commit/8b23562)) misc: clean up, removing old comments - Russell Matney
- ([`e300439`](https://github.com/russmatney/clawe/commit/e300439)) feat: 1password wsp rules, godot workspace - Russell Matney
- ([`f61f36f`](https://github.com/russmatney/clawe/commit/f61f36f)) wip: trying to avoid sending focus when restoring - Russell Matney

  > Some scratchpad clients seem to steal focus after toggling, so this was
  > a shot at improving that.


### 25 Apr 2021

- ([`6a99226`](https://github.com/russmatney/clawe/commit/6a99226)) feat: open latest lichess games via rofi - Russell Matney

  > Connected the dots on this wip open-chess-game command - now consuming
  > commands from russmatney/chess, which uses systemic and a git-ignored
  > resources/config.edn file to get the lichess data.

- ([`4e7cdb4`](https://github.com/russmatney/clawe/commit/4e7cdb4)) feat: move xf86 keybindings into clawe - Russell Matney

  > Rewrites and adds notifications for these, as well as introducing the
  > handling for 'pause'. Now seeing the resulting volume on each inc/dec.

- ([`2b8a23f`](https://github.com/russmatney/clawe/commit/2b8a23f)) fix: add timeout and remove blood from error notifs - Russell Matney

  > Reduces the noise of awm errors.

- ([`74c099c`](https://github.com/russmatney/clawe/commit/74c099c)) misc: some clean up, missing deps, emacs/open api - Russell Matney

  > Opts into the preferred emacs/open api, fixes misc formatting.


### 23 Apr 2021

- ([`2a3a69a`](https://github.com/russmatney/clawe/commit/2a3a69a)) feat: handful of new workspaces - Russell Matney

  > These added mostly to support creating garden files for them.


### 22 Apr 2021

- ([`2aa26e9`](https://github.com/russmatney/clawe/commit/2aa26e9)) fix: remove shell from readme src block - Russell Matney

  > Because i keep accidentally kicking off the jar build from this file.

- ([`7a4f50e`](https://github.com/russmatney/clawe/commit/7a4f50e)) feat: move slack call clients to the zoom tag - Russell Matney

  > This is an example of an awesome-wm pain point - rules can be hacked to
  > get a hold of clients with changing titles and tough to capture process
  > ids, but now i'm doing it like this so it can be debugged at repl-time.

- ([`5ad1bfa`](https://github.com/russmatney/clawe/commit/5ad1bfa)) feat: apply workspace rules on clawe-restart - Russell Matney

  > Adds clawe-rule application to the ever growing mod+r reload button.

- ([`d74dbd2`](https://github.com/russmatney/clawe/commit/d74dbd2)) feat: awesome client/tag helpers - Russell Matney

  > Some basic functions for operating on clients and moving them between
  > tags.


### 21 Apr 2021

- ([`fb6ac13`](https://github.com/russmatney/clawe/commit/fb6ac13)) fix: extend def-like clj-kondo to defworkspace, defbinding - Russell Matney

  > Should have done this ages ago, didn't realize it'd be so easy.

- ([`d80c38c`](https://github.com/russmatney/clawe/commit/d80c38c)) feat: repops buried scratchpads - v1 impl - Russell Matney

  > This works pretty well already! I'm sure there are quirks that will come
  > up in regular usage, but i'm pretty excited that the uberjar maintains
  > the same performance and clawe now has a datalevin db supporting it
  > across executions. At some point I'd like to cap/handle the db size - as
  > impled it'll grow forever, but for this use-case the data itself doesn't
  > need to last more than a few minutes.

- ([`446ce1a`](https://github.com/russmatney/clawe/commit/446ce1a)) refactor: switch from uberscript to uberjar - Russell Matney

  > Uberscripts have caveats - they don't support dynamic requires, for one,
  > which rules out pods. This moves clawe's executable to an uberjar - i'd
  > long thought these were too slow, but that was a result of a bloated
  > jar. After resolving some issues where the entire directory (including
  > caches and git history) were being added to the jar, this now runs at
  > equivalent speed to the uberscript, and can work with pods as expected.

- ([`77c8cb7`](https://github.com/russmatney/clawe/commit/77c8cb7)) wip: more clawe db work - Russell Matney
- ([`ea35411`](https://github.com/russmatney/clawe/commit/ea35411)) wip: db/scratchpads wip-impl - Russell Matney

  > Not quite working yet - my db/query doesn't handle clojure core and/or,
  > which is forcing some more experimentation.

- ([`7766fd2`](https://github.com/russmatney/clawe/commit/7766fd2)) feat: add datalevin workspace - Russell Matney
- ([`7d40ac9`](https://github.com/russmatney/clawe/commit/7d40ac9)) feat: initial datalevin dtlv transact and query fns - Russell Matney

  > Kind of gross having to go across the shell layer, but it saves having
  > to work with a server or a pod and might be enough for a handful of
  > features. I'm interested to see how fast it can be.


### 15 Apr 2021

- ([`fdfb2db`](https://github.com/russmatney/clawe/commit/fdfb2db)) feat: toggle global mute button - Russell Matney

  > Quick impl that relies on `amixer set Capture toggle`


### 10 Apr 2021

- ([`6022708`](https://github.com/russmatney/clawe/commit/6022708)) wip: toggling doctor-dock via tmux - Russell Matney
- ([`47e74bd`](https://github.com/russmatney/clawe/commit/47e74bd)) misc: uuid binding, defs clean up, rules wip - Russell Matney

  > Adds a binding adding a uuid to the clipboard.
  > 
  > Rearranges some bindings.
  > 
  > Wip for applying clawe rules, as the beginning of replacing awesome
  > rules completely.
  > 
  > Also adds a center-small binding.


### 3 Apr 2021

- ([`9113969`](https://github.com/russmatney/clawe/commit/9113969)) fix: ignore generated files - Russell Matney

  > These are generated, and don't need to be tracked at all.

- ([`d2b70fe`](https://github.com/russmatney/clawe/commit/d2b70fe)) feat: chrome, 1password workspace, rename per-wsp-garden - Russell Matney

  > First case of colliding bindings, and binding names. Ought to get some
  > notifs/warnings for that kind of thing.

- ([`f9f6ed5`](https://github.com/russmatney/clawe/commit/f9f6ed5)) fix: floating, center bindings set :ontop - Russell Matney

  > This has long been an issue - now getting :ontop handled correctly when
  > moving into floating mode.


### 28 Mar 2021

- ([`ec24373`](https://github.com/russmatney/clawe/commit/ec24373)) feat: remove old bindings, set zoom icon - Russell Matney
- ([`bda8817`](https://github.com/russmatney/clawe/commit/bda8817)) feat: port toggle-scratchpad into clawe bindings - Russell Matney

  > toggle-scratchpad now completely consumed as clojure, and also sending
  > centerfocussed scratchpads away rather than just promoting them to
  > floating + centered.

- ([`0d918ea`](https://github.com/russmatney/clawe/commit/0d918ea)) feat: toggle-terminal working as expected - Russell Matney

  > cmd+ret to open, focus, or close the workspace's terminal instance

- ([`509df14`](https://github.com/russmatney/clawe/commit/509df14)) feat: toggle-emacs working as expected - Russell Matney

  > Rather than just opening emacs when called, this now opens it if it's
  > closed, focuses it if it's open and not focused, and closes it if it's
  > focused.

- ([`c4db4e3`](https://github.com/russmatney/clawe/commit/c4db4e3)) feat: open-term,emacs working when there's no selected tag - Russell Matney
- ([`45d9c26`](https://github.com/russmatney/clawe/commit/45d9c26)) feat: doctor workspace - Russell Matney

### 25 Mar 2021

- ([`7c65de4`](https://github.com/russmatney/clawe/commit/7c65de4)) feat: bindings, workspace workspaces; emacs,term,garden toggles - Russell Matney

  > Writes defs for bindings and workspace workspaces. These are for
  > managing bindings and workspaces, of course. Hopefully they'll one day
  > come with dashboards, for now they come with garden notes.
  > 
  > Writes clawe versions of open-emacs and open-term functions, named as
  > toggle-blah for now, but not yet implemented to actually toggle.
  > 
  > Writes toggle-workspace-garden for opening a garden note names after the
  > current workspace. Intended to serve as a hub for the workspace, a quick
  > step into the current context's garden.
  > 
  > Refactors scratchpad helper into awm/set-focused. writes
  > awm/close-client.
  > 
  > For whatever reason, toggle-workspace-garden isn't centering its client.
  > Will have to debug later.
  > 
  > Writing these out as clojure as been oh so repl-smooth.


### 21 Mar 2021

- ([`d205db3`](https://github.com/russmatney/clawe/commit/d205db3)) misc: clean up and raw data updates - Russell Matney

  > Some of these should probably be git-ignored, as they're generated or
  > could be private workspaces that we don't want to share config for.

- ([`fbda790`](https://github.com/russmatney/clawe/commit/fbda790)) feat: use notify {:replace-process NAME} feat - Russell Matney

  > The same notification can be updated as a process runs - the clawe
  > rebuild notifications now update themselves, rather than send a new
  > notification.

- ([`51e1536`](https://github.com/russmatney/clawe/commit/51e1536)) feat: toggle deadd-notification-center open/closed - Russell Matney

### 20 Mar 2021

- ([`fc714b9`](https://github.com/russmatney/clawe/commit/fc714b9)) feat: tile-all-windows - Russell Matney

  > wip: open-chess-game
  > 
  > refactor: move bindings into def.bindings


### 17 Mar 2021

- ([`dbf5998`](https://github.com/russmatney/clawe/commit/dbf5998)) feat: impl toggle-all-titlebars - Russell Matney

  > This was delightful. Learned that because I've already created and built
  > the defbinding, I don't need to re-write clawe-bindings.fnl or reload
  > awesome to update the effect of the same keybinding.
  > 
  > Feels like writing out to clawe-bindings.fnl could happen on save - it'd
  > be great if the 'restart' of awesomewm to rebuild the bindings was more
  > transparent. more reason to find something else to manage the keybindings.


### 14 Mar 2021

- ([`d0b1dba`](https://github.com/russmatney/clawe/commit/d0b1dba)) feate: key bindings defined in clj - Russell Matney

  > Hacks some stuff together to support defbinding being consumed by
  > defcom, and writing out awesome style bindings. For now, these bindings
  > spawn a clawe command that fires the attached function via defcom.
  > 
  > There's some oddities and probably some subtle bugs. Right now the
  > :binding/command needs to be arity 2 to match defcom.
  > 
  > It would probably be worth expanding the api to support writing
  > fennel/awesome directly as well.
  > 
  > Resetting these right now requires running cmd+r twice, then cmd+shift+r
  > - that's because we rebuild the clawe uberscript with the latest code,
  > then running again writes that latest in-memory binding data to the
  > clawe-bindings.fnl file, then an awesome restart updates keybindings
  > based on the (now updated) static config. This is clearly not ideal - it
  > might worth moving to a totally independent keybinding tool, as it's
  > annoying to reboot awesome just to update keybindings - the awesome
  > internal apis seem like they support removing bindings, but my
  > impression is that it must be the same awful.key that was created, - you
  > can't seem to be able to remove/upsert by keybinding (it just adds
  > multiple listeners to that same binding...)

- ([`d285759`](https://github.com/russmatney/clawe/commit/d285759)) fix: some-usage with string/identity was no good - Russell Matney

  > Moves to a quick if, or keyword-fn-fallback style where relevant.


### 13 Mar 2021

- ([`2c1dc77`](https://github.com/russmatney/clawe/commit/2c1dc77)) wip: refactor bindings to use new awm append_keybds fn - Russell Matney
- ([`fedf031`](https://github.com/russmatney/clawe/commit/fedf031)) wip: nearly impled awesome bindings - Russell Matney

  > Ended up down a rabbit hole, and now i'm on awesome.git.
  > The awful.keyboard.append_global_keybindings appears to wipe the current
  > keybindings... but I'm in a strange half-state right now, so I'm not
  > sure if that's a real problem

- ([`4c9c97f`](https://github.com/russmatney/clawe/commit/4c9c97f)) fix: sort-order getting out of whack - Russell Matney

  > hitting 10 workspaces reveals an issue that zero-padding resolves.

- ([`a945839`](https://github.com/russmatney/clawe/commit/a945839)) feat: refactor defthing, defs/workspaces, defs/apps - Russell Matney

  > Also, begins defs/bindings.
  > 
  > Doc strings for defthing functions.

- ([`9eccaf9`](https://github.com/russmatney/clawe/commit/9eccaf9)) refactor: all-in on derived workspace-rules - Russell Matney
- ([`18fb253`](https://github.com/russmatney/clawe/commit/18fb253)) fix: open workspace fixes, add todo workspace - Russell Matney

### 7 Mar 2021

- ([`8019293`](https://github.com/russmatney/clawe/commit/8019293)) feat: move to clawe toggle-scratchpad - Russell Matney

  > Moves scratchpad keybindings off of the ralphie micro to a normal clawe
  > command. Speed seems to be fine on the laptop, will have to test the
  > desktop at some point.
  > 
  > Disables carve in the micro install. for some reason it's never
  > finishing, i'll have to find a smaller reproduction.

- ([`c5f7919`](https://github.com/russmatney/clawe/commit/c5f7919)) refactor: workspaces, scratchpad, install - Russell Matney

  > workspaces and scratchpad circular dep clean up, moves create-client to
  > workspaces.create namespace.
  > 
  > Writes but does not test micro install port from ralphie.

- ([`9abfa36`](https://github.com/russmatney/clawe/commit/9abfa36)) feat: adds workspace usage from ralphie - Russell Matney

  > Clawe is consuming all workspace handling from ralphie.
  > Also moves away from org-based workspace definitions.


### 28 Feb 2021

- ([`464ca2e`](https://github.com/russmatney/clawe/commit/464ca2e)) fix: disable clean wksps in open-wksp command - Russell Matney
- ([`fb28169`](https://github.com/russmatney/clawe/commit/fb28169)) fix: disable garbage collection message - Russell Matney
- ([`5784cc8`](https://github.com/russmatney/clawe/commit/5784cc8)) wip: middle click to set ontop/above - Russell Matney

  > Doesn't seem to do anything, not sure why

- ([`cb5ee5a`](https://github.com/russmatney/clawe/commit/cb5ee5a)) feat: bindings for spotify volume control - Russell Matney

### 21 Feb 2021

- ([`41499b4`](https://github.com/russmatney/clawe/commit/41499b4)) feat: cleaner wsp defs, merge in awm tags - Russell Matney

  > The awm tags were only merging into org-based workspaces - this provides
  > the merge for the clawe-only workspaces.


### 20 Feb 2021

- ([`5eba519`](https://github.com/russmatney/clawe/commit/5eba519)) feat: right click workspace to toggle visibility - Russell Matney

  > left click still does the usual toggle-only/go-back-to-last.

- ([`4772b31`](https://github.com/russmatney/clawe/commit/4772b31)) wip: refactoring workspace defs - Russell Matney

  > getting a bit more minimal here, experimenting with some macro styles.

- ([`98f6ae9`](https://github.com/russmatney/clawe/commit/98f6ae9)) feat: write out awesome rules on reload - Russell Matney

  > Breaks out awesome reload function into clawe.awesome/restart namespace,
  > and impls clawe.awesome/write-awesome-rules function, which pulls in the
  > workspace defs and stringifies any attached :awesome/rules,
  > then pushes it into my awesome config. Updates the config to consume the
  > newly generated file.

- ([`c4d8e87`](https://github.com/russmatney/clawe/commit/c4d8e87)) feat: quick org-crud workspace - Russell Matney
- ([`ab42562`](https://github.com/russmatney/clawe/commit/ab42562)) misc: namespace some keys according to the defcom linter - Russell Matney

  > Be nice if clj-kondo hooks could make these changes for
  > me/automatically. Flycheck that just fixes the code?


### 10 Feb 2021

- ([`5eed0ef`](https://github.com/russmatney/clawe/commit/5eed0ef)) wip: breaking workspaces out of defthing - Russell Matney
- ([`2982f38`](https://github.com/russmatney/clawe/commit/2982f38)) wip: more defworkspace refactoring - Russell Matney

  > Now supporting accepting xorfs, which is named b/c it's either x (some
  > map of data) or a f (a unary function x->x).
  > 
  > One thing we'll likely need is an ability to push some of these
  > functions to be evaluated at run-time, right now these are evaluated at
  > macro time, which will not be desired at some point.
  > If the partitioning that is easy, we should have some state of
  > workspaces that can be recalculated cheaply/measurably.


### 9 Feb 2021

- ([`7a19c52`](https://github.com/russmatney/clawe/commit/7a19c52)) wip: refactored defworkspace to support arbitrary fns - Russell Matney

  > A step toward defworkspace macro glory! Reduces a list of arbitrary
  > functions for building up the workspace definition.


### 7 Feb 2021

- ([`7b50ab3`](https://github.com/russmatney/clawe/commit/7b50ab3)) feat: doctor firing git-status notifications - Russell Matney

  > Pulls all workspaces, applies git-status to those opting in via
  > :git/check-status?, and notifies for all detected
  > dirty/needs-pull/needs-push.
  > 
  > Adds a dose of color by reusing the renamed wsp->rofi-label. Notify
  > handles pango just as well - now to get these pushing into widgets as
  > well.

- ([`676abd6`](https://github.com/russmatney/clawe/commit/676abd6)) fix: remove wing dep, which apparently fails in bb/my environment - Russell Matney

  > Symptom is lack of `dissoc!`.

- ([`943e3e0`](https://github.com/russmatney/clawe/commit/943e3e0)) wip: doctor command shell - Russell Matney

  > Had to rename some existing commands, and refactored a bit of the dwim
  > command in core. Could pull that into it's own namespace soon.

- ([`aa5b24a`](https://github.com/russmatney/clawe/commit/aa5b24a)) def: quick org-roam-server wsp - Russell Matney

  > Note: :git/repo, :awesome/rules not yet supported, just playing with api
  > ideas.


### 6 Feb 2021

- ([`c909afe`](https://github.com/russmatney/clawe/commit/c909afe)) feat: tile-all-clients command - Russell Matney

  > Quick helper that tells awm to push all clients on the current screen
  > into tiled mode (i.e. not-floating). This is a bandaid, but useful in a
  > bind - there are cases rn where i open a window that grabs focus, but
  > another is floating and completely obscuring it.

- ([`21921fb`](https://github.com/russmatney/clawe/commit/21921fb)) feat: show current app instance name - Russell Matney

  > Also, some more wip towards awm-fnl helpers.

- ([`34f1618`](https://github.com/russmatney/clawe/commit/34f1618)) wip: toggle-current-workspace-name outline - Russell Matney
- ([`3fc98f0`](https://github.com/russmatney/clawe/commit/3fc98f0)) feat: dotfiles workspace opt-in to git check-status - Russell Matney
- ([`6259122`](https://github.com/russmatney/clawe/commit/6259122)) feat: clj-kondo defworkspace hook presents def - Russell Matney

  > Gives the benefits of clj-kondo thinking defworkspace is a normal def,
  > like smarter warnings via flycheck.

- ([`c2e71d3`](https://github.com/russmatney/clawe/commit/c2e71d3)) wip: defworkspace pre-hook attempt, rules.clj idea - Russell Matney

### 31 Jan 2021

- ([`ee35b33`](https://github.com/russmatney/clawe/commit/ee35b33)) feat: first crack at defworkspace clj-kondo hook - Russell Matney

  > Writes a clj-kondo hook that lints the defworkspace/defcom maps,
  > alerting on missing keys.

- ([`ec17966`](https://github.com/russmatney/clawe/commit/ec17966)) fix: rofi :sticky rule, notify on dropped binding calls - Russell Matney
- ([`22f2f2b`](https://github.com/russmatney/clawe/commit/22f2f2b)) fix: opt-in required to check git status - Russell Matney

  > Running git status on every workspace repo is a bit too slow, so here we
  > make it opt-in.

- ([`a24ea0e`](https://github.com/russmatney/clawe/commit/a24ea0e)) wip: defapps macro and a few defcom defs - Russell Matney

  > Toying with this idea for now

- ([`bf3320d`](https://github.com/russmatney/clawe/commit/bf3320d)) fix: lessen garbage collection notifs - Russell Matney

### 30 Jan 2021

- ([`a96479a`](https://github.com/russmatney/clawe/commit/a96479a)) refactor: misc reload cleanup and refactor - Russell Matney

  > also supports an optional top_bar cut-off variable. Going to try to port
  > the fennel directly into clojure and clawe-defs.

- ([`ca67edd`](https://github.com/russmatney/clawe/commit/ca67edd)) feat: write fennel as quoted-clojure, run it in awesome - Russell Matney

  > Finally, fennel and the clojure repl to run it, by totally smashing the
  > awesome-client api. Huzzah!

- ([`25b9807`](https://github.com/russmatney/clawe/commit/25b9807)) refactor: generalize defs registry for clawe - Russell Matney

  > Should support another type with a minimum of boilerplate.

- ([`6ff7d7f`](https://github.com/russmatney/clawe/commit/6ff7d7f)) feat: dwim prints notifies git status, offers fetch - Russell Matney
- ([`30d6bb2`](https://github.com/russmatney/clawe/commit/30d6bb2)) refactor: use flattened awesome fields - Russell Matney
- ([`4f104c2`](https://github.com/russmatney/clawe/commit/4f104c2)) feat: create first client after creating workspace - Russell Matney

  > A bit tentative about this, but i think it makes sense to auto-create a
  > client in every case where the workspace is empty (no clients) and it is
  > opened. This is roughly the same behavior as the scratchpad toggling,
  > and will reveal cases where workspaces don't have the right initial-file
  > or exec set.

- ([`a89c8bb`](https://github.com/russmatney/clawe/commit/a89c8bb)) fix: move apply-git-status to specific locations - Russell Matney

  > Too slow to call every time.

- ([`436c213`](https://github.com/russmatney/clawe/commit/436c213)) feat: show git status in open-workspace - Russell Matney

  > - Refactors to use git/status to prevent calling 3 git statuses per repo.
  > - Refactors open-workspace into more reusable pieces.
  > - Writes v1 wsp->rofi-label

- ([`cb8c34f`](https://github.com/russmatney/clawe/commit/cb8c34f)) feat: all-workspaces apply some git status flags - Russell Matney

  > Passes :dirty, :needs_pull, :needs_push into the workspace list

- ([`ade7b90`](https://github.com/russmatney/clawe/commit/ade7b90)) feat: zoom rules, scratchpad binding, show date - Russell Matney
- ([`a673d95`](https://github.com/russmatney/clawe/commit/a673d95)) misc: workspace defs cleanup - Russell Matney
- ([`1851c9d`](https://github.com/russmatney/clawe/commit/1851c9d)) feat: toggle-scratchpad-names as defcom - Russell Matney
- ([`69b02b5`](https://github.com/russmatney/clawe/commit/69b02b5)) feat: escape strings passed to awm-fnl - Russell Matney
- ([`0c740c8`](https://github.com/russmatney/clawe/commit/0c740c8)) feat: scratchpad names now togglable - Russell Matney

  > ```sh
  > clawe awm-cli '_G.toggle_show_scratchpad_names();'
  > ```

- ([`418ac12`](https://github.com/russmatney/clawe/commit/418ac12)) feat: wires org-clock through defcom - Russell Matney
- ([`9a425fc`](https://github.com/russmatney/clawe/commit/9a425fc)) fix: move clear timer to run after dropped calls - Russell Matney

  > rather than for every successful one


### 28 Jan 2021

- ([`ae8877a`](https://github.com/russmatney/clawe/commit/ae8877a)) feat: icon for emacs, fix wsp-meta widget - Russell Matney
- ([`3153165`](https://github.com/russmatney/clawe/commit/3153165)) feat: emacs workspace def - Russell Matney
- ([`e1f1204`](https://github.com/russmatney/clawe/commit/e1f1204)) feat: show all active tags by name in workspace meta - Russell Matney

### 25 Jan 2021

- ([`d74dab5`](https://github.com/russmatney/clawe/commit/d74dab5)) feat: rearrange topbar to show larger clock in center - Russell Matney

### 24 Jan 2021

- ([`a79049f`](https://github.com/russmatney/clawe/commit/a79049f)) fix: add cache-clearing timer so failing bindings aren't blocked forever - Russell Matney
- ([`33aa9f7`](https://github.com/russmatney/clawe/commit/33aa9f7)) feat: open-workspace deletes empty workspaces when called - Russell Matney
- ([`1bc8dcb`](https://github.com/russmatney/clawe/commit/1bc8dcb)) fix: rofi window above and ontop - Russell Matney
- ([`136b77c`](https://github.com/russmatney/clawe/commit/136b77c)) feat: ralphie workspace definition - Russell Matney
- ([`dc36e1d`](https://github.com/russmatney/clawe/commit/dc36e1d)) fix: make sure rofi starts centered - Russell Matney
- ([`690440c`](https://github.com/russmatney/clawe/commit/690440c)) feat: add create-workspace-client, get-workspace, current-workspace - Russell Matney

  > Quick wins here, including pushing for this just-create-my-client
  > laziness.

- ([`7152c0e`](https://github.com/russmatney/clawe/commit/7152c0e)) feat: rofi -normal-window rules - Russell Matney

  > Floating and no titlebar, and we're good!

- ([`cd9399f`](https://github.com/russmatney/clawe/commit/cd9399f)) feat: use rofi combi mode - Russell Matney

  > Suddenly I can jump around all my windows right away. Who needs
  > launchpad?


### 23 Jan 2021

- ([`26c5015`](https://github.com/russmatney/clawe/commit/26c5015)) feat: list background, larger text, show all names - Russell Matney
- ([`4baffc8`](https://github.com/russmatney/clawe/commit/4baffc8)) feat: connected first workspaces.clj data to workspace widget - Russell Matney

  > Color and text. For some reason the pango doesn't pass along well -
  > maybe it can't be nested, could be some other weird escaping happening.
  > (or not happening).

- ([`823abd7`](https://github.com/russmatney/clawe/commit/823abd7)) feat: first defworkspace steps, already integrating into active-workspaces - Russell Matney
- ([`c028253`](https://github.com/russmatney/clawe/commit/c028253)) feat: org-clock-menu via ralphie-emacs-cli keybdg - Russell Matney
- ([`20c4fbc`](https://github.com/russmatney/clawe/commit/20c4fbc)) feat: clawe dwim command - Russell Matney

  > Setting up some structure for manual suggestions/actions on top of all
  > other clawe commands via rofi.

- ([`b02e16c`](https://github.com/russmatney/clawe/commit/b02e16c)) feat: sort workspaces before scratchpads - Russell Matney
- ([`3e4abb2`](https://github.com/russmatney/clawe/commit/3e4abb2)) feat: delete all but 1 empty workspace on-update - Russell Matney
- ([`ce894a8`](https://github.com/russmatney/clawe/commit/ce894a8)) feat: show current workspace name - Russell Matney

  > These widgets are huge pain to work with. Likely abandoning these
  > widgets completely in favor of something electron based soon.

- ([`19db7d8`](https://github.com/russmatney/clawe/commit/19db7d8)) fix: remove current workspace from workspaces widget - Russell Matney
- ([`a2d4bdd`](https://github.com/russmatney/clawe/commit/a2d4bdd)) feat: improve bindings speed with async callbacks - Russell Matney

  > And add a quick cache to prevent calling again faster than execution
  > time.

- ([`6827f36`](https://github.com/russmatney/clawe/commit/6827f36)) refactor: clean up tag restoration/startup - Russell Matney

  > Removes client restoration, as it wasn't doing anything anyway. For now
  > we rely on rules for restoring tags, and need to get per-workspace rules
  > in there so that it works for all, not just the hard-coded ones.

- ([`d07fa62`](https://github.com/russmatney/clawe/commit/d07fa62)) feat: log request duration - Russell Matney
- ([`5ab63e6`](https://github.com/russmatney/clawe/commit/5ab63e6)) feat: timer for garbage collection - Russell Matney
- ([`62d0322`](https://github.com/russmatney/clawe/commit/62d0322)) feat: larger current-task text - Russell Matney
- ([`8a593c1`](https://github.com/russmatney/clawe/commit/8a593c1)) feat: notifications on garbage collection - Russell Matney
- ([`91c340e`](https://github.com/russmatney/clawe/commit/91c340e)) wip: a bindings.clj workspace to toy with - Russell Matney

### 20 Jan 2021

- ([`62255c0`](https://github.com/russmatney/clawe/commit/62255c0)) feat: improved garbage collector output - Russell Matney
- ([`18f4284`](https://github.com/russmatney/clawe/commit/18f4284)) refactor: run-init updated to consume modules - Russell Matney

  > Rather than relying on global fns to be set previously, a few more are
  > consumed as normal modules.

- ([`18baacb`](https://github.com/russmatney/clawe/commit/18baacb)) feat: restore fullscreen feature - Russell Matney

  > Cleans up a handful of old signals and rules - these were causing issues
  > and are not really helpful anyway. In particular, this arrange signal
  > was causing a completely block on the main thread whenever apps tried to
  > go fullscreen.

- ([`106c574`](https://github.com/russmatney/clawe/commit/106c574)) feat: set wallpaper - Russell Matney

  > This is in bar for now, as that's the only thing dependent on this
  > whacky screen callback.

- ([`97e019d`](https://github.com/russmatney/clawe/commit/97e019d)) refactor: moving away from global functions - Russell Matney

  > Creates actual returns from the spawns and titlebars modules.
  > Also updates the .gitignore and symlinks a background image.

- ([`9708fe5`](https://github.com/russmatney/clawe/commit/9708fe5)) Create LICENSE - Russell Matney
- ([`890e7b7`](https://github.com/russmatney/clawe/commit/890e7b7)) feat: color and wiring for workspace urgency - Russell Matney

### 18 Jan 2021

- ([`daba9af`](https://github.com/russmatney/clawe/commit/daba9af)) wip: nearly luachecking config before restarts - Russell Matney
- ([`a677cc0`](https://github.com/russmatney/clawe/commit/a677cc0)) fix: awesome config completely luacheck-approved - Russell Matney
- ([`61dcc2a`](https://github.com/russmatney/clawe/commit/61dcc2a)) feat: clawe rofi helper - Russell Matney

  > As implemented, this includes whatever ralphie namespaces have been
  > loaded as well.

- ([`5d25820`](https://github.com/russmatney/clawe/commit/5d25820)) feat: show indexes on non-scratchpad workspaces - Russell Matney
- ([`bdf0867`](https://github.com/russmatney/clawe/commit/bdf0867)) feat: workrave widget exposing time until micro/rest breaks - Russell Matney
- ([`0aa9742`](https://github.com/russmatney/clawe/commit/0aa9742)) feat: org-clock displaying a clocked-in item - Russell Matney

### 17 Jan 2021

- ([`f442b4c`](https://github.com/russmatney/clawe/commit/f442b4c)) feat: garbage collection helper - Russell Matney
- ([`f4a4f23`](https://github.com/russmatney/clawe/commit/f4a4f23)) fix: clean-workspaces - Russell Matney

  > Found this finally - defcom can overwrite commands with same name but
  > diff namespaces. need to make them unique - ralphie's open-workspaces
  > was overwriting clawe's. pretty big bug!

- ([`2784c32`](https://github.com/russmatney/clawe/commit/2784c32)) misc: print awm-cli output, add util to preamble. - Russell Matney
- ([`f153f40`](https://github.com/russmatney/clawe/commit/f153f40)) feat: highlight all selected tags - Russell Matney

  > Impled this via awesome signals, which don't seem to be removable. Not
  > sure how to handle that. Right now it stacks callbacks for every
  > refresh.


### 16 Jan 2021

- ([`4448be6`](https://github.com/russmatney/clawe/commit/4448be6)) misc: ignore unused vars - Russell Matney
- ([`a040856`](https://github.com/russmatney/clawe/commit/a040856)) misc: todo.org cleanup - Russell Matney
- ([`5059a74`](https://github.com/russmatney/clawe/commit/5059a74)) refactor: break bar into top/bottom - Russell Matney
- ([`c1ea840`](https://github.com/russmatney/clawe/commit/c1ea840)) fix: remove index/key, make bigger icons - Russell Matney
- ([`e33a7c3`](https://github.com/russmatney/clawe/commit/e33a7c3)) feat: make-icon supporting margins - Russell Matney
- ([`a48e35f`](https://github.com/russmatney/clawe/commit/a48e35f)) feat: include icons when reloading - Russell Matney
- ([`e453be8`](https://github.com/russmatney/clawe/commit/e453be8)) fix: remove merge, impl via lume - Russell Matney
- ([`a6a3231`](https://github.com/russmatney/clawe/commit/a6a3231)) fix: update workspaces after workspace-related bindings - Russell Matney

  > This is begging for a hooks/advice abstraction

- ([`cf97ab9`](https://github.com/russmatney/clawe/commit/cf97ab9)) log: Awm completion log line - Russell Matney
- ([`b33a090`](https://github.com/russmatney/clawe/commit/b33a090)) fix: util syntax error - Russell Matney
- ([`1c4aa63`](https://github.com/russmatney/clawe/commit/1c4aa63)) wip: passing and setting index via new_index - Russell Matney

  > Seems swap is now a bit broken tho...

- ([`52fb612`](https://github.com/russmatney/clawe/commit/52fb612)) feat: adds some margins back - Russell Matney
- ([`4804b9d`](https://github.com/russmatney/clawe/commit/4804b9d)) fix: much better reload and clawe-cli command logging - Russell Matney

  > Finally catching and logging errors without devestating nonsense.

- ([`8f8168b`](https://github.com/russmatney/clawe/commit/8f8168b)) feat: get_tag now expects {:name 'blah'} or {:index 1} - Russell Matney
- ([`90e440a`](https://github.com/russmatney/clawe/commit/90e440a)) feat: center the workspaces (finally) - Russell Matney

  > This took a bit, but fortunately found the trivia:
  > layout.container.place
  > https://github.com/awesomeWM/awesome/issues/2426
  > 
  > Starting to wrap my head around this awm layout system.

- ([`ca68c62`](https://github.com/russmatney/clawe/commit/ca68c62)) misc: helpers added to util namespace - Russell Matney

  > try-catch wrapper and log_if_error helpers are a nice win for error
  > handling.
  > 
  > these util functions are underscored to be sure they can be called from
  > lua ergonomically. kind of annoying, wonder if there's a way to smooth
  > that out, maybe by exporting both snake and camel-cased function names.
  > Or a macro for fennel modules that just turns a file of functions into a
  > proper namespace.

- ([`016345a`](https://github.com/russmatney/clawe/commit/016345a)) feat: ports workspace movement/open/close to clawe - Russell Matney

  > Updates the awesome bindings to use the clawe command.

- ([`16c6c69`](https://github.com/russmatney/clawe/commit/16c6c69)) feat: clean up util, bar - Russell Matney

  > Completely drops the awesome taglist component in favor of the newly
  > minted one.

- ([`cd1367a`](https://github.com/russmatney/clawe/commit/cd1367a)) feat: click to select workspace - Russell Matney

  > Re-requesting workspaces from clawe on every click - seems reasonable...?

- ([`48b31b7`](https://github.com/russmatney/clawe/commit/48b31b7)) feat: refactors workspace widget to stay DRY, support hover - Russell Matney
- ([`41aedad`](https://github.com/russmatney/clawe/commit/41aedad)) feat: transparent bg, selected, empty, scratchpad flags - Russell Matney

  > Also sets some colors based on tag state.

- ([`c0aaebd`](https://github.com/russmatney/clawe/commit/c0aaebd)) feat: handle_garbage function - Russell Matney

  > Starting to drown in awm garbage...

- ([`eb3bf47`](https://github.com/russmatney/clawe/commit/eb3bf47)) feat: support passing workspace icon-code from org - Russell Matney

### 15 Jan 2021

- ([`5b71c25`](https://github.com/russmatney/clawe/commit/5b71c25)) feat: global keybindings now also update with mod+r - Russell Matney
- ([`6cced88`](https://github.com/russmatney/clawe/commit/6cced88)) feat: awesome check-for-errors - Russell Matney

  > Not nearly complete, but a v1 for asserting on files before
  > hot-reloading them. Runs .fnl files through the `fennel --compile`
  > command, checking the output for a leading `Parse Error` message.


### 10 Jan 2021

- ([`413cff5`](https://github.com/russmatney/clawe/commit/413cff5)) rules: chat box rules, first clover widget handling - Russell Matney

### 9 Jan 2021

- ([`28df25c`](https://github.com/russmatney/clawe/commit/28df25c)) todo: misc todo cleanup (cont) - Russell Matney
- ([`0a24f3b`](https://github.com/russmatney/clawe/commit/0a24f3b)) misc: clean up core ns - Russell Matney

  > Adds comment blocks, removes unused install functions.


### 8 Jan 2021

- ([`99c12f4`](https://github.com/russmatney/clawe/commit/99c12f4)) todo: misc todo cleanup - Russell Matney
- ([`fdff998`](https://github.com/russmatney/clawe/commit/fdff998)) fix: ignore clawe-script.clj - Russell Matney

  > Plus some misc other comments and cleanup.

- ([`a8c8c57`](https://github.com/russmatney/clawe/commit/a8c8c57)) feat: move garden binding to g, add r for rebuild/reload - Russell Matney

  > Mod+r now rebuilds-clawe and reloads all of the awesome widgets.
  > 
  > This currently has a race-case - the clawe rebuild likely won't finish
  > before the widgets have reloaded and been restarted, so their initial
  > state _can_ come from the previous version of clawe.
  > 
  > I don't want to block on the widget reload yet though, i think i'd
  > prefer the speed, especially b/c the clawe code might not have even
  > changed yet.

- ([`1b610e4`](https://github.com/russmatney/clawe/commit/1b610e4)) feat: update-workspaces passing non-scratchpad tags - Russell Matney

  > Pulls workspaces from ralphie, which come with awesome tags attached,
  > then filters for the tags and then removes those that have opt-in to the
  > scratchpad feature set, as this generally excludes them from persisted
  > workspace treatment. (scratchpads don't need to be shown, as they
  > startup automatically via another keybinding).

- ([`73227db`](https://github.com/russmatney/clawe/commit/73227db)) feat: building clawe as an uberscript - Russell Matney

  > Creates a bin/clawe that executes the /clawe-script.clj with whatever
  > arguments. This is expected to be symlinked to your path somewhere.
  > 
  > Updates clawe.core to support building and installing the binary, so
  > that rebuilds are a viable clawe command. This should be refactored into
  > a useful tool somewhere, as it's the same logic/style that ralphie and
  > vapor also use.
  > 
  > Once installed, clawe can now be rebuilt via `clawe rebuild-clawe`.

- ([`8e88b74`](https://github.com/russmatney/clawe/commit/8e88b74)) widget: workspaces updates, showing tag index - Russell Matney
- ([`7f2c7bf`](https://github.com/russmatney/clawe/commit/7f2c7bf)) misc: remove nested awesome symlink, update todos.org, .gitignore - Russell Matney
- ([`03922fb`](https://github.com/russmatney/clawe/commit/03922fb)) docs: readme update and quote added - Russell Matney
- ([`c9f07cc`](https://github.com/russmatney/clawe/commit/c9f07cc)) wip: workspaces widget updating with passed objs. - Russell Matney
- ([`becea1d`](https://github.com/russmatney/clawe/commit/becea1d)) wip: work on workspaces widget - Russell Matney

### 7 Jan 2021

- ([`aa3ca7a`](https://github.com/russmatney/clawe/commit/aa3ca7a)) todo: update todo.org - Russell Matney
- ([`32508cd`](https://github.com/russmatney/clawe/commit/32508cd)) wip: workspaces widget updated to consume clawe.update-workspaces - Russell Matney
- ([`eaf40f9`](https://github.com/russmatney/clawe/commit/eaf40f9)) feat: clawe.fnl created - will support the fnl -> clawe-clojure api - Russell Matney

  > Supports calling a passed callback function name.

- ([`0bc0fe6`](https://github.com/russmatney/clawe/commit/0bc0fe6)) feat: clawe.workspaces ns, clawe.awesome/awm-fn impl - Russell Matney

  > awm-fn supports converting clojure datastructures to lua syntax. This
  > makes it really convenient to pass data from clojure to a lua function.
  > 
  > clawe.workspaces gets a handler that will support updating the
  > workspaces widget. This handler supports calling a passed function name
  > with the latest workspace data, which is just hard-coded for now.

- ([`3b0acda`](https://github.com/russmatney/clawe/commit/3b0acda)) feat: quick defcom reload-widgets command - Russell Matney
- ([`71154ad`](https://github.com/russmatney/clawe/commit/71154ad)) feat: reload-widgets function - Russell Matney

  > Impls a function for hotswapping the widget modules, then calling
  > init_screen. Immediately recreates the status bar.

- ([`e6d9aa8`](https://github.com/russmatney/clawe/commit/e6d9aa8)) feat: bar.init_screen updated to teardown and rebuild the wibar - Russell Matney

  > Now init_screen can be called repeatedly!

- ([`d40afae`](https://github.com/russmatney/clawe/commit/d40afae)) wip: workspaces widget - Russell Matney

  > Starts writing and using the user.clj to do module hotswapping and basic
  > updates. Re-initing the screen doubles up on the wibar, so we'll need to
  > do a proper teardown before rebuilding it.

- ([`7491a05`](https://github.com/russmatney/clawe/commit/7491a05)) feat: more awm-cli preamble, clojure-like parsing - Russell Matney
- ([`0b68ec5`](https://github.com/russmatney/clawe/commit/0b68ec5)) fix: disable duplicate workspace creation - Russell Matney
- ([`b1dd239`](https://github.com/russmatney/clawe/commit/b1dd239)) todo: ideas for exposing keybindings, workspace config - Russell Matney

### 6 Jan 2021

- ([`1a42074`](https://github.com/russmatney/clawe/commit/1a42074)) feat: writes awm-cli command - Russell Matney

  > Expects a string arugment that is lua text. Same api as 'awesome-client'.
  > 
  > More preamble to come here, for sure.

- ([`ef4356c`](https://github.com/russmatney/clawe/commit/ef4356c)) feat: writes clawe 'install' command - Russell Matney

  > Theoretically, you can call this on the command line. That is, if you
  > have clawe installed - for that I recommend looking at the
  > clawe.core/install command.

- ([`eb7ab9a`](https://github.com/russmatney/clawe/commit/eb7ab9a)) copy: pulls awesome config in from dotfiles - Russell Matney

  > May want to go back and get the git history for this... or maybe even
  > use it as a submodule. I hope to hack it down to size shortly.

- ([`f1774a2`](https://github.com/russmatney/clawe/commit/f1774a2)) feat: initial defcom app - Russell Matney

  > - adds wing, ralphie dependencies
  > - adds clj-kondo/config for defcom clj-kondo config
  > - adds user.clj with wing.repl/sync-libs!
  > - adds src/core with small defcom and -main function
  > - adds todo.org
