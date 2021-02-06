(ns clawe.defs
  (:require [ralphie.notify :as notify]
            [ralphie.emacs :as r.emacs]
            [babashka.process :refer [$ check]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; registry
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def base-registry {:clawe/workspace {}})

(defonce registry* (atom base-registry))

(defn clear-registry [] (reset! registry* base-registry))

(defn add-x [x]
  (swap! registry* assoc-in
         [(:clawe/type x) (:clawe.registry/key x)] x))

(defn list-xs [type]
  (vals (get @registry* type)))

(defn get-x [type pred]
  (some->> (get @registry* type)
           vals
           (filter pred)
           first))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; workspaces
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn list-workspaces []
  (list-xs :clawe/workspace))

(defn get-workspace [wsp]
  (get-x :clawe/workspace
         (let [name (or (:org/name wsp) (:awesome/tag-name wsp))]
           (fn [w]
             (-> w :clawe/name #{name})))))

(defn- defworkspace*
  ([title] (defworkspace* title {}))
  ([title x & _fns]
   (let [x    (update x :workspace/title (fn [t] (or t (-> title
                                                           symbol
                                                           name))))
         type :clawe/workspace
         ;; fns  (into [] fns)
         ]
     `(let [ns#           ~(str *ns*)
            name#         ~(-> title symbol name)
            registry-key# (keyword ns# name#)
            x#
            (->
              ~x
              ;; (reduce
              ;;   (fn [x f]
              ;;     (println "x" x)
              ;;     (println "f" f)
              ;;     (f x))
              ;;   ~x
              ;;   ~fns)
              (assoc :clawe/name name#
                     :clawe.registry/key registry-key#
                     :clawe/type ~type
                     :ns ns#))]

        (def ~title x#)
        (add-x x#)
        ;; returns the created command map
        x#))))


(defmacro defworkspace [title & args]
  (apply defworkspace* title args))

(comment
  (println "hi")
  (defworkspace simpleton {:some/other :data})

  (defworkspace my-simple-with-fn {:funfun/data :data}
    ;; identity
    ;; ;; ((fn [x]
    ;; ;;    (println "yooooo")
    ;; ;;    (assoc x :somesecret/fun :data)))
    ;; identity
    )

  (reduce
    (fn [x f]
      (println "x" x)
      (println "f" f)
      (f x))
    {:funfun/data     :data,
     :workspace/title "my-simple-with-fn"}
    [identity identity
     (fn [x] (println "wooo!") x)])

  )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; apps
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn list-apps []
  (list-xs :clawe/app))

(defn get-app [name]
  (get-x :clawe/app
         (fn [a]
           (-> a :clawe/name #{name}))))

(defmacro defapp
  "Merges data into the defs registry*."
  [app-symbol x]
  (let [type       :clawe/app
        the-symbol (symbol app-symbol)]
    `(let [ns#           ~(str *ns*)
           name#         ~(name app-symbol)
           registry-key# (keyword ns# name#)
           x#            (assoc ~x
                                :clawe/name name#
                                :clawe.registry/key registry-key#
                                :clawe/type ~type
                                :ns ns#)]

       (def ~the-symbol x#)
       (add-x x#)
       ;; returns the created command map
       x#)))

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


;; (defworkspace example
;;   {:workspace/title "example"}
;;   (fn [x]
;;     (println x)
;;     (-> x (assoc :awesome/rules (-> x :workspace/title awm-workspace-rules)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Slack, Spotify
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defapp spotify-app
  {:defcom/handler (fn [_ _] (-> ($ spotify) check))
   :defcom/name    "start-spotify-client"})

(defworkspace spotify
  {:workspace/title        "Spotify"
   :workspace/color        "#38b98a"
   :workspace/directory    "/home/russ/"
   :workspace/exec         "spotify"
   :workspace/initial-file "/home/russ/.config/spicetify/config.ini"
   :workspace/scratchpad   true
   :workspace/key          "s"
   :workspace/fa-icon-code "f1bc"

   :workspace/apps [spotify-app]

   :workspace/title-pango  "<span>Spotify</span>"
   :workspace/title-hiccup [:h1 "Spotify"]
   :workspace/on-create    (fn [_wsp]
                             (println "Created spotify workspace")
                             (notify/notify (str "Created spotify workspace...")
                                            "for all your spotifyy needs."))
   :awesome/rules
   (awm-workspace-rules "spotify"  "spotify" "Pavucontrol" "pavucontrol")})

(comment
  spotify
  )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Ralphie, Clawe, and other repo-based workspaces
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defworkspace clawe
  {:awesome/rules          (awm-workspace-rules "clawe")
   :workspace/color        "#88aadd"
   ;; :workspace/title-pango  "<span>THE CLAWWEEEEEEEEE</span>"
   :workspace/title        "clawe"
   :workspace/title-pango  "<span size=\"large\">THE CLAWE</span>"
   :workspace/title-hiccup [:h1 "The Cl-(awe)"]
   :git/check-status?      true
   :workspace/on-create    (fn [_wsp]
                             (println "Created clawe workspace")
                             (notify/notify
                               (str "Created clawe workspace...")
                               "hope you're not too deep in the rabbit hole"))})

(defworkspace ralphie
  {:awesome/rules         (awm-workspace-rules "ralphie")
   :workspace/color       "#aa88ee"
   :workspace/title       "ralphie"
   :workspace/title-pango "<span>The Ralphinator</span>"
   :git/check-status?     true
   :workspace/on-create   (fn [_wsp]
                            (notify/notify "Welcome to Ralphie"
                                           "Don't Wreck it~"))})

(defworkspace dotfiles
  {:awesome/rules   (awm-workspace-rules "dotfiles")
   :workspace/title "dotfiles"
   :git/check-status?      true})

(defworkspace clomacs
  {:awesome/rules   (awm-workspace-rules "clomacs")
   :workspace/title "clomacs"})

(defworkspace emacs
  ;; My .doom.d config, where i fix 'emacs'
  {:awesome/rules          (awm-workspace-rules "emacs")
   :workspace/title        "Emacs"
   :workspace/directory    "/home/russ/.doom.d"
   :workspace/initial-file "/home/russ/.doom.d/init.el"
   :workspace/scratchpad   true
   :workspace/key          "e"
   :workspace/fa-icon-code "f1d1"
   :git/check-status?      true

   :workspace/title-pango  "<span>Emax</span>"
   :workspace/title-hiccup [:h1 "Emacs"]
   :workspace/on-create    (fn [_wsp]
                             (println "Created emacs workspace")
                             (notify/notify (str "Created emacs workspace...")
                                            "for all your emacsy needs."))})

(defworkspace doom-emacs
  {:awesome/rules          (awm-workspace-rules "doom-emacs")
   :workspace/title        "Doom Emacs"
   :workspace/color        "#aaee88"
   :workspace/directory    "/home/russ/.emacs.d"
   :workspace/initial-file "/home/russ/.emacs.d/docs/index.org"
   :workspace/fa-icon-code "f1d1"
   :workspace/title-pango  "<span>Doom Emacs</span>"})

(defapp org-manual
  {:defcom/handler (fn [_app _]
                     (r.emacs/open {:eval-sexp "(org-info)"}))
   :defcom/name    "open-org-manual"})

(defworkspace org
  {:awesome/rules          (awm-workspace-rules "org")
   :workspace/title        "Org"
   ;; TODO point to org manual by default (org-info)
   :workspace/initial-file "/home/russ/.emacs.d/init.el"
   ;; TODO org icon
   :workspace/fa-icon-code "f1d1"

   :workspace/title-pango  "<span>ORG</span>"
   :workspace/title-hiccup [:h1 "Org"]
   :workspace/on-create    (fn [_wsp]
                             (println "Created org workspace")
                             (notify/notify (str "Created org workspace...")
                                            "time to RTFM"))
   :workspace/apps         [org-manual]

   :workspace/actions
   [{:defcom/handler
     (fn [_ _]
       ;; Ex: open emacs, run command
       ;; TODO refactor r.emacs/open to support this
       (r.emacs/open {:eval-sexp "(org-info)"})
       )}]

   })

(comment)
