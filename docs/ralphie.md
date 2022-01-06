# Ralphie

A set of commands for the command line, keybindings, and rofi,
expressed via Clojure and [Babashka](https://github.com/borkdude/babashka/).

## Ralphie?

This library was nearly named `bb-gun` as a reference to `babashka`'s `bb`
command. I left it to squat less on `bb`, as this is more of an application
than library. But be warned, this one will shoot your eye out!

> Ralphie as Adult: [narrating] Meanwhile, I struggled for exactly the right BB
> gun hint. It had to be firm, but subtle.
>
> Ralphie: Flick says he saw some grizzly bears near Pulaski's candy store!
>
> [Everyone stares at Ralphie]
>
> Ralphie as Adult: [narrating] They looked at me as if I had lobsters crawling
> out of my ears.

This mostly speaks to Ralphie knowing he wants to write something in Babashka,
whether there are bears near Pulaski's or not.

# Status

Untested on other machines, and not yet configurable. You could clone and modify
it to work, but this is not a plug-n-play tool yet.

If you try to work with it, especially note the paths in `ralphie.config` are
hard-coded - theoretically things should work if you update a few spots in
there. You'll also need whatever tools each command depends on, which is not
documented but should be possible to surmise from the code.

# Background

Ralphie's main goal is to reduce the overhead of automating common dev tasks.
Babashka has made it possible to write clojure where you might otherwise fall
back to Bash - Ralphie is my attempt to rewrite my bash scripts in a way that is
more maintainable (i.e. in clojure).

Ralphie provides a registry-macro `defcom` which can be used to create a
command. It then exposes all of its commands both on the command line and via
[rofi](https://github.com/davatorium/rofi).

My current setup includes firing ralphie commands from awesomeWM, and mapping
`ralphie rofi` to `super+x`, which is a sort of OS-level `M-x` emacs-y metaphor.

I like to think of `ralphie` as a clojure version of the use-case
[Alfred](https://www.alfredapp.com/) filled on OSX.

# Features

TODO: implement feature that writes `docs/features.org` for each command.
TODO: include dependencies per command

Coming soon... `docs/features.org`

### Parses org files for state via org-crud

Tired of moving rewriting configuration when moving between window managers,
Ralphie consumes and applies commands based on parsed org files. This mostly
boils down to workspaces.org and repos.org.

#### workspaces.org

This file supports the ralphie.workspace namespace, as well as
`toggle-scratchpad`, `open-term`, `open-emacs`, probably a few others.

The properties are optional, but help to tell these commands how to create the
initial workspace. `open-term` and `open-emacs` will each create a tmux session
and emacs workspace using the workspace name - the directory and initial-file
help determine where to start when a term/emacs is opened in any workspace.
`exec` is executed is used to start an app in the workspace, if the name is
passed to `toggle-scratchpad`.

- TODO trackdown + document other workspaces.org features
- TODO: support `pinned-apps` for sticky applications
- TODO: derive workspaces from found repos, or repos.org

```org
* spotify
:PROPERTIES:
:directory: /home/russ/
:pinned-apps: spotify
:workspace-key: 1
:initial-file: /home/russ/.config/spicetify/config.ini
:END:
* slack
:PROPERTIES:
:directory: /home/russ/
:pinned-apps: slack
:pinned-apps+: discord
:workspace-key: 2
:END:
* web
:PROPERTIES:
:directory: /home/russ/
:exec: /usr/bin/gtk-launch firefox.desktop
:workspace-key: 3
:END:
* dotfiles
:PROPERTIES:
:directory: /home/russ/dotfiles
:workspace-key: 4
:initial-file: /home/russ/dotfiles/readme.org
:END:
```

#### repos.org

`ralphie.repos` supports a few repos commands, notably a quick way to list
dirty ones via `list-dirty-repos`.

These org items looks like:

```org
* russmatney/scratch
:PROPERTIES:
:id: 326b24a4-72de-4d8e-b822-538c4f6b847f
:seen-at: 2020-07-24T12:26:39-04:00[America/New_York]
:updated-at: 2020-08-18T18:54:59.175-04:00[America/New_York]
:watching: true
:END:
```

Note that mostly this file is generated/consumed by external apps/commands.

- TODO impl watch/stop-watching repos
- TODO append to repos.org from `ralphie clone`
- TODO impl snoozable reminder/notifier for watched, dirty repos

### Carved, single-command Uberscripts for speed

Some of ralphie's features are WM-level functions, and thus necessitate speed -
`toggle-scratchpad` is probably the best example of a feature that I want to be
as fast as possible, others might be `open-term`/`open-emacs` or the old
universal-move-focus command (not yet re-implemented).

Babashka supports building uberscripts/uberjars to aid distribution and speed up
startup time (vs running as a multi-file project). Some brief experiments have
led me to prefer the uberscripts for their speed, though there are some
incompatibilities that are better baked into jars. My experiments were mostly
just building both and wrapping a few calls with `time` - more could be done
here....

[Carve](https://github.com/borkdude/carve) is yet another borkdude project that
can identify and remove unused variables and functions from your project - this
makes for a nice match for trimming uberscripts down to only the required
pieces, which speeds up execution time just by reducing the amount of code that
needs to be processed at startup.

I've put together a pipeline for creating a minimized uberscript for individual
commands in the ralphie.install namespace - it first creates a
`src/ralphie/temp.clj` file with a main function that skips ralphie's routing
completely, calling only the command's handler. This is used as the base for an
uberscript (created in the `uberscripts/` directory) which is then carved to its
minimum. The script is then wrapped in a small bash script that's written
directly to `~/.local/bin/ralphie-<cmd-name>`, and this runs the script in
`uberscripts` via bb.

Scripts can be created, carved, and installed via ralphie's `install-micro`
command.

# Development

Ralphie expects to be run as a babashka process, but it can be developed as a
jvm app as usual (via cider/nrepl or something similar). That being said, not
all libraries will be BB compatible.

You can quickly test if the app/command works on the command line via:

``` sh
bb -cp $(which -Spath) -m ralphie.core <whatever-args>
```

I've had decent results with connecting to a bb process via nrepl, but it's not
as fully featured as working with the usual cider-nrepl. However, that usually
looks like running:

``` sh
rlwrap bb -cp $(clojure -Spath) --nrepl-server 1667
```

Then `cider-connect`ing from emacs.

## Running tests

Tests are usually run fully in cider, but you can also run them on the command
line via kaocha:

```sh
./bin/kaocha --watch
```

# Creating commands

A command can be added at a new namespace with somethine like:

```
(ns ralphie.my-new-ns
 (:require
  [ralph.defcom :refer [defcom]]))

(defn my-new-handler
  ([] (my-new-handler nil nil))
  ([_config _parsed]
    (println "do-the-magic-here")))

(defcom my-new-cmd
  {:name          "my-new-cmd"
   :one-line-desc "Some well written, hopefully cheeky one-liner as a TLDR."
   :description   ["As much context as you feel necessary."]
   :handler       my-new-handler})
```

Note that this new namespace needs to be required somewhere - for now I require
everything in `ralphie.core`.

You can run the command locally via either:

``` sh
bb -cp $(which -Spath) -m ralphie.core my-new-cmd
```

...or by selecting `my-new-cmd` from a 'dev' rofi, similarly:

```
bb -cp $(which -Spath) -m ralphie.core rofi
```

The former is preferred for debugging, as the output is immediately accessible.

If you're confident with your code (because you developed in the clojure repl
anyway), you can also call `ralphie build-and-install-uberscript` (via cli or
global rofi, which is typically key-bound) to update the global uberjar, which
will then make `my-new-cmd` available via your typical `ralphie rofi` as well.

## `defcom` yasnippet

I use a `defcom` snippet to quickly add a new command, which looks something
like:

``` yasnippet
# -*- mode: snippet -*-
# name: defcom
# uuid: defcom
# key: defcom
# condition: t
# --
(:require
   [ralph.defcom :refer [defcom]])

(defn $1-handler
  ([] ($1-handler nil nil))
  ([_config _parsed]
   $4))

(defcom $1-cmd
  {:name          "$1"
   :one-line-desc "$2"
   :description ["$3"]
   :handler       $1-handler})
```

This boilerplate should be shrunk over time, but for now I'm just happy to have
something easy to work with in the repl, and to be encouraging docs at a few
levels - the descriptions are intended to be featured in the help text on teh
command-line and in rofi, which helps find commands/other program interfaces
more easily.

If there are thoughts on a minimal way to make this pluggable, I'm all ears!
(Ralphie as a library, unified keybinding to show ralphie commands across
several apps, etc.)

# License

See `LICENSE`.

TLDR: MIT. Go hog-wild.
