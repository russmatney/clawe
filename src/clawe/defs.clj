(ns clawe.defs
  (:require
   [ralphie.notify :as notify]
   [ralphie.emacs :as r.emacs]
   [babashka.process :refer [$ check]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; registry
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def base-registry {::workspaces {}
                    ::apps {}})

(defonce registry* (atom base-registry))

(defn clear-registry [] (reset! registry* base-registry))
(comment
  (clear-registry))

(defn add-x [x]
  (swap! registry* assoc-in
         [(::type x) (::registry-key x)] x))

(defn list-xs [type]
  (vals (get @registry* type)))

(defn get-x [type pred]
  (some->> (get @registry* type)
           vals
           (filter pred)
           first))

(defn list-workspaces []
  (list-xs ::workspaces))

(defn get-workspace [wsp]
  (get-x ::workspaces
         (let [name (or (:org/name wsp) (:awesome/tag-name wsp))]
           (fn [w]
             (-> w ::name #{name})))))

(defn list-apps []
  (list-xs ::apps))

(defn get-app [name]
  (get-x ::apps
         (fn [a]
           (-> a ::name #{name}))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; defthing macro
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- eval-xorf
  "x is the current map of data for the thing being def-ed.

  xorf is either a map to be merged into x,
  or a function to be called with x (to update it).

  Returns the merged/updated x.

  The `seq?` `list?` may need to expand/get smarter at some point.
  For now it seems to work for both anonymous and named functions."
  [x xorf]
  (let [merge-x (fn [x v]
                  (cond
                    (map? v) (merge x v)
                    (string? v)
                    (update x ::doc (fn [doc-str]
                                      (str doc-str
                                           (when doc-str "\n")
                                           v)))

                    :else x))]
    (cond
      (map? xorf)     (merge-x x xorf)
      (or
        (seq? xorf)
        (list? xorf)) (merge-x x ((eval xorf) x))
      (symbol? xorf)  (merge-x x ((eval xorf) x))
      (string? xorf)  (merge-x x xorf)
      :else           (do (println "unexpected xorf type!")
                          (println "type" (type xorf))
                          (println "xorf" xorf)
                          x))))

(comment
  (assert (= {:yo :yo}
             (reduce eval-xorf [{} 'println '#(assoc % :yo :yo) 'println])))
  (assert (= {:meee :bee}
             (eval-xorf {} '#(assoc % :meee :bee))))

  (assert (= {::doc "my\ndocs"
              :and  :keys}
             (reduce eval-xorf [{} '"my" '{:and :keys} "docs"])))

  ;; TODO apis like...
  (defn two-arg-fn [x y] {:x x :y y})
  (reduce eval-xorf [{} '(partial (two-arg-fn "my-x"))])
  )

(defn- initial-thing [type thing-sym]
  {::name         (-> thing-sym name)
   ::type         type
   ::registry-key (keyword (str *ns*) (-> thing-sym name))
   :ns                 (str *ns*)})

(defn- defthing
  ;; TODO refactor into fns-or-xs that get run w/ the obj so far, or
  ;; just merge into that object
  ;; try to keep them decoupled
  ;;
  ;; TODO partition xorfs on runtime/macro-time eval
  ;; maybe with a :clawe.eval/runtime keyword
  ([thing-type thing-sym] (defthing thing-type thing-sym {}))
  ([thing-type thing-sym & xorfs]
   (let [x (->> (concat
                  [(initial-thing thing-type thing-sym)]
                  xorfs)
                (reduce eval-xorf))]
     `(do
        (def ~thing-sym ~x)
        (add-x ~x)
        ~x))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; defthing consumers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmacro defworkspace [title & args]
  (apply defthing ::workspaces title args))

(defmacro defapp [title & args]
  (apply defthing ::app title args))

(comment
  ;; TODO convert to test suite?
  ;; all of these should work

  (defworkspace simpleton {:some/other :data})

  (defworkspace my-simple-with-fns
    {:just/a-map :data}
    #(assoc % :some-fn/fn :data) identity)

  (defworkspace my-simple-fn-then-x
    (fn [x] (assoc x :somesecret/fun :data))
    {:funfun/data :data})

  (println "break\t\t\tbreak")
  (defworkspace my-simple-anon-fn-then-x
    #(assoc % :somesecret/fun :data)
    "With a doc string"
    #(assoc % :somesecret/fun :data)
    "or two"
    {:funfun/data :data}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Misc workspace builder helpers
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

(defn awesome-rules [& args]
  (let [{::keys [name] :as thing} (last args)
        args                      (butlast args)
        ]
    (assoc thing :awesome/rules (apply awm-workspace-rules name args))))

(comment
  (awesome-rules {::name "my-thing-name"})
  (awesome-rules "onemore" {::name "my-thing-name"})
  (awesome-rules "more" "andmore" {::name "my-thing-name"})
  )

(defn workspace-title
  "Sets the workspace title using the ::name (defaults to the def-ed symbol)."
  [{::keys [name]}]
  {:workspace/title name})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; usage examples
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(comment
  (defworkspace my-workspace
    workspace-title
    awesome-rules
    )

  (eval workspace-title)

  my-workspace

  ;; TODO get defthing to obey the threading rules
  (defworkspace fancy-example
    workspace-title
    (awesome-rules "extra-match-phrase")
    ))



;; Slack, Spotify


(defapp spotify-app
  {:defcom/handler (fn [_ _] (-> ($ spotify) check))
   :defcom/name    "start-spotify-client"})

(defworkspace spotify
  workspace-title
  {:awesome/rules
   (awm-workspace-rules "spotify"  "spotify" "Pavucontrol" "pavucontrol")}
  {:workspace/color        "#38b98a"
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
                                            "for all your spotifyy needs."))})

(comment
  spotify
  )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Ralphie, Clawe, and other repo-based workspaces
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defworkspace clawe
  workspace-title
  awesome-rules
  {:workspace/color        "#88aadd"
   ;; :workspace/title-pango  "<span>THE CLAWWEEEEEEEEE</span>"
   :workspace/title-pango  "<span size=\"large\">THE CLAWE</span>"
   :workspace/title-hiccup [:h1 "The Cl-(awe)"]
   :git/check-status?      true
   :workspace/on-create    (fn [_wsp]
                             (println "Created clawe workspace")
                             (notify/notify
                               (str "Created clawe workspace...")
                               "hope you're not too deep in the rabbit hole"))})

(defworkspace ralphie
  workspace-title
  awesome-rules
  {:workspace/color       "#aa88ee"
   :workspace/title-pango "<span>The Ralphinator</span>"
   :git/check-status?     true
   :workspace/on-create   (fn [_wsp]
                            (notify/notify "Welcome to Ralphie"
                                           "Don't Wreck it~"))})

(defworkspace dotfiles
  awesome-rules
  workspace-title
  {:git/check-status? true})

(defworkspace clomacs
  awesome-rules
  workspace-title
  {:git/repo "clojure-emacs/clomacs"}
  )

(defworkspace org-roam-server
  awesome-rules
  workspace-title
  {:git/repo "org-roam/org-roam-server"})

(defworkspace emacs
  ;; TODO support strings as doc-builders
  ;; My .doom.d config, where i fix 'emacs'
  awesome-rules
  workspace-title
  {:workspace/directory    "/home/russ/.doom.d"
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
  awesome-rules
  {:workspace/title        "Doom Emacs" ;; just sets the workspace title
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
  awesome-rules
  workspace-title
  {;; TODO point to org manual by default (org-info)
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

(defworkspace org-crud
  awesome-rules
  workspace-title
  {:git/repo "russmatney/org-crud"})

(defworkspace treemacs
  awesome-rules
  workspace-title
  {:git/repo "Alexander-Miller/treemacs"})

(defworkspace git-summary
  awesome-rules
  workspace-title
  {:git/repo "MirkoLedda/git-summary"})
