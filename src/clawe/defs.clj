(ns clawe.defs
  (:require [ralphie.notify :as notify]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; registry
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; TODO write a ralph.lib called reggie for abstracting this pattern?
;; or just document and unit test it? feels like defcom without
;; the focus on the handler.

(defonce workspace-registry* (atom {}))

(defn clear-workspace-registry [] (reset! workspace-registry* {}))

(defn list-workspaces []
  (vals @workspace-registry*))

(defn get-workspace [wsp]
  (let [name (or (:org/name wsp)
                 (:awesome/tag-name wsp))]
    (some->> (vals @workspace-registry*)
             (filter (comp #{name} :workspace/name))
             first)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; defworkspace
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmacro defworkspace
  "
  Mixes data into the `clawe/db`.

  Supplies configuration workspace-related tooling
  - Awesome/Other WM Layout Rules/Configs
  - WM-level icons
  - Color Themes
  - App related details
  - Keybindings

  Workspace-related depenecies in general.

  "
  [workspace-symbol opts]
  (let [the-symbol (symbol workspace-symbol)]
    `(let [ns#           ~(str *ns*)
           name#         ~(name workspace-symbol)
           registry-key# (keyword ns# name#)
           opts#         (assoc ~opts
                                :workspace/name name#
                                :workspace/registry-key registry-key#
                                :ns ns#)]

       (def ~the-symbol opts#)
       (swap! workspace-registry* assoc registry-key# opts#)
       ;; returns the created command map
       opts#)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; AwesomeWM Data Helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn awm-workspace-rules
  "Returns values intended as literal awesome/rules.
  The first arg is always expected to be the relevant workspace.

  Supports two versions:
  - Single arity returns a simple rule.
  - Multi-arity implies a broader match-any."
  ([name]
   {:rule       {:name name}
    :properties {:tag name}})
  ([name & aliases]
   (let [all (cons name aliases)]
     {:rule_any   {:class all :name all}
      :properties {:tag name :first_tag name}})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Slack, Spotify
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defworkspace spotify
  {:workspace/start        "spotify"
   :workspace/title        "Spotify"
   :workspace/color        "#38b98a"
   :workspace/directory    "/home/russ/"
   :workspace/pinned-apps  "spotify"
   :workspace/exec         "spotify"
   :workspace/initial-file "/home/russ/.config/spicetify/config.ini"
   :workspace/scratchpad   true
   :workspace/key          "s"
   :workspace/fa-icon-code "f1bc"

   :workspace/title-pango  "<span>Spotify</span>"
   :workspace/title-hiccup [:h1 "Spotify"]
   :workspace/on-create    (fn [_wsp]
                             (println "Created spotify workspace")
                             (notify/notify (str "Created spotify workspace...")
                                            "for all your spotifyy needs."))
   :awesome/rules
   (awm-workspace-rules "spotify"  "spotify" "Pavucontrol" "pavucontrol")})

(comment
  spotify)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Ralphie, Clawe, and other repo-based workspaces
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defworkspace clawe
  {:awesome/rules          (awm-workspace-rules "clawe")
   :workspace/color        "#88aadd"
   :workspace/title-pango  "<span>THE CLAWWEE</span>"
   ;; TODO figure out why we can't pass in plain pango like this
   ;; :workspace/title-pango  "<span size=\"large\">THE CLAWE</span>"
   :workspace/title-hiccup [:h1 "The Cl-(awe)"]
   :workspace/on-create    (fn [_wsp]
                             (println "Created clawe workspace")
                             (notify/notify
                               (str "Created clawe workspace...")
                               "hope you're not too deep in the rabbit hole"))})

(comment
  clawe)

(defworkspace ralphie
  {:awesome/rules         (awm-workspace-rules "ralphie")
   :workspace/color       "#aa88ee"
   :workspace/title-pango "<span>The Ralphinator</span>"
   :workspace/on-create   (fn [_wsp]
                            (notify/notify "Welcome to Ralphie"
                                           "Don't Wreck it~")
                            )})

(defworkspace emacs
  {:awesome/rules          (awm-workspace-rules "emacs")
   :workspace/start        "emacs"
   :workspace/title        "Emacs"
   :workspace/color        "#38b98a"
   :workspace/directory    "/home/russ/.doom.d"
   :workspace/pinned-apps  "emacs"
   :workspace/exec         "emacs"
   :workspace/initial-file "/home/russ/.doom.d/init.el"
   :workspace/scratchpad   true
   :workspace/key          "e"
   :workspace/fa-icon-code "f1bc"

   :workspace/title-pango  "<span>Emax</span>"
   :workspace/title-hiccup [:h1 "Emacs"]
   :workspace/on-create    (fn [_wsp]
                             (println "Created emacs workspace")
                             (notify/notify (str "Created emacs workspace...")
                                            "for all your emacsy needs."))})
