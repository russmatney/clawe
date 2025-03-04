(ns user
  (:require
   [defthing.defkbd :as defkbd]
   [ralphie.awesome :as awm]
   [ralphie.notify :as notify]
   [wing.repl :as repl]
   [ralphie.zsh :as zsh]
   [clj-kondo.core :as clj-kondo]
   [clj-kondo.main :as clj-kondo.main]
   [loom.graph :refer [digraph]]
   [loom.io :refer [view]]
   [util :as util]
   [clojure.string :as string]
   [babashka.fs :as fs]

   [next.jdbc :as jdbc]


   [org-crud.api :as org-crud.api]))

(comment
  (repl/sync-libs!))

(comment
  (binding
      [*data-readers*
       (->
         *data-readers*
         (assoc `sh/expand #'ralphie.zsh/expand)
         (assoc `sh/expand-many #'ralphie.zsh/expand-many))]
    *data-readers*)

  #zsh/expand "~/todo"
  #zsh/expand-many "~/todo/{projects,journal}.org"
  #to/upper "hi there"
  #to/lower "HI THERE"


  (def test-nses ['ralphie.awesome-test
                  'defthing.core-test
                  'defthing.defcom-test])

  (doall
    (for [t (->> test-nses
                 )]
      (require t)))

  (->>
    test-nses
    (map meta)
    )


  (awm/fetch-tags)



  (defkbd/list-bindings)

  ;; reload widgets (re-runs connect_for_each_screen)
  (awm/awm-cli "require('bar'); return init_bar();")

  (awm/awm-cli "lume.hotswap('theme');")
  (awm/awm-cli "lume.hotswap('clawe');")
  ;; hotswap modules
  (awm/awm-cli "lume.hotswap('theme');")
  (awm/awm-cli "lume.hotswap('bar');")
  (awm/awm-cli "return lume.hotswap('widgets.workspaces');")

  (awm/awm-cli "return view(screen);")
  (awm/awm-cli "return view(screen.count());")


  (awm/awm-cli "return view(screen.count());")

  (notify/notify "basic notification")
  (notify/notify "notification" "with body")
  (notify/notify {:subject "notification"
                  :body    "with body"})
  (awm/awm-fnl
    '(do
       (local naughty (require :naughty))
       (naughty.notify
         {:title "My notif"
          ;; :position "bottom_middle"
          :text  (.. "some sub head: " "with info")})))

  (notify/notify "updated notification")

  )

(defn viz-nses [{:keys [paths]}]
  (when-let [analysis
             (:analysis (clj-kondo/run!
                          {:lint      paths
                           :config    {:analysis {:var-usages      false
                                                  :var-definitions {:shallow true}}}
                           :skip-lint true}))]
    (println "analysis" analysis)
    (let [{:keys [:namespace-definitions :namespace-usages]} analysis
          nodes                                              (map :name namespace-definitions)
          edges                                              (map (juxt :from :to) namespace-usages)
          g                                                  (apply digraph (concat nodes edges))]
      ;; install GraphViz, e.g. with brew install graphviz
      (view g))))

(defn -main [& paths]
  (viz-nses {:paths paths}))

(comment
  (viz-nses {:paths ["~/russmatney/clawe/src"]})
  ;; https://github.com/clj-kondo/clj-kondo/issues/226
  ;; move to using clj-kondo/main and string args?
  (clj-kondo/run!
    {:lint      ["/home/russ/russmatney/clawe/src"]
     :config    {:analysis {:var-usages      false
                            :var-definitions {:shallow true}}}
     :skip-lint true
     :cache     false
     :debug     true})

  (clj-kondo.main/main
    "--lint" "/home/russ/russmatney/clawe/src"
    "--skip-lint")
  )


(comment
  (def dropbox-dir "/Users/Russ/Library/CloudStorage/Dropbox")
  (->>
    (fs/list-dir (str dropbox-dir "/games-notebooks/notebook-1"))
    (map (fn [file]
           {:file      file
            :file-name (fs/file-name file)}))
    (sort-by :file-name)
    (map-indexed (fn [i f]
                   (let [idx (+ i
                                ;;118
                                ;;1
                                105
                                )]
                     (-> f
                         (assoc :idx idx)
                         (assoc :new-file-name
                                (str "pg_" (util/zp idx 3) ".HEIC"))))))
    (take (- 117 105))
    (map (fn [f]
           (let [source (:file f)
                 target (-> f :file str
                            (string/replace (:file-name f)
                                            (:new-file-name f)))]
             (println "renaming" (str source) target)
             (fs/move (str source) target)
             (:idx f)
             )))
    doall))

(defn ->uuid [str-uuid]
  (-> str-uuid
      (string/replace "\"" "")
      util/ensure-uuid)
  )

(defn ->file [str-uuid]
  (-> str-uuid (string/replace "\"" ""))
  )


(def db-spec
  {:dbtype "sqlite"
   :dbname "/home/russ/.config/emacs/.local/cache/org-roam.db"})

 (defn link-data []
   (jdbc/execute! db-spec ["SELECT * FROM links"]))

(def node-data
  (delay
    (->>
      (jdbc/execute! db-spec ["SELECT * FROM nodes"])
      (map (fn [node]
             (-> node
                 (update :nodes/id ->uuid)
                 (update :nodes/file ->file))))
      (map (fn [node]
             [(:nodes/id node) node]))
      (into {}))))

(defn links-by-x-count [link-type]
  (->>
    (jdbc/execute! db-spec ["SELECT * FROM links"])
    (filter (comp #{"\"id\""} :links/type))
    (map (fn [link] (-> link (update link-type ->uuid))))
    (map link-type)
    frequencies
    (sort-by second >)))

(comment
  (->> (link-data)
       (map :links/type)
       (into #{}))

  (->> (link-data)
       (filter (comp #{"\"org\""} :links/type)))

  (defn single-x-data [link-type]
    (->>
      (link-data)
      (filter (comp #{"\"id\""} :links/type))
      (map (fn [link]
             (-> link (update link-type ->uuid))))
      (map link-type)
      frequencies
      (filter (comp #{1} second))
      (map first)
      ))

  (single-x-data :links/source)
  (single-x-data :links/dest)

  (->>
    (single-x-data :links/source)
    (map (fn [id]
           (get @node-data id)))
    ;; (take 4)
    (map (fn [node]
           (println "node" node)
           (->
             (org-crud.api/path->nested-item (:nodes/file node))
             (org-crud.api/update! {:org/tags "singlesource"}))
           ))
    doall)

  (->>
    (single-x-data :links/dest)
    (map (fn [id]
           (get @node-data id)))
    ;; (take 4)
    (map (fn [node]
           (println "node" node)
           (->
             (org-crud.api/path->nested-item (:nodes/file node))
             (org-crud.api/update! {:org/tags "singledest"}))
           ))
    doall)

  (links-by-x-count :links/source)
  (links-by-x-count :links/dest)

  (->>
    (links-by-x-count :links/dest)
    (map (fn [[id ct]] [ct (get @node-data id)]))

    (take 100)
    (map (fn [[_ct node]]
           (->
             (org-crud.api/path->nested-item (:nodes/file node))
             (org-crud.api/update! {:org/tags "topdest"}))
           ))
    doall
    )

  )
