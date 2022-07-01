(ns clawe.scratchpad-test
  (:require
   [clojure.test :refer [deftest is]]
   [malli.transform :as mt]
   [malli.core :as m]
   [lambdaisland.specmonstah.malli :as sm-malli]
   [reifyhealth.specmonstah.core :as sm]
   [clawe.scratchpad :as subject]
   [clawe.workspaces :as clawe.workspaces]
   [loom.io :as lio]))

;; TODO generate workspace (scratchpad?) shapes
;; TODO mock/pass in (workspaces/current-workspaces)
;; TODO mock/pass in (awm/all-clients) (or some generic all-clients call)
;; TODO flesh out conditional use cases from doc string

(def workspace-schema
  "Basic workspace-schema"
  ;; TODO improve these types/generators
  [:map
   ;; [:rules/is-my-client? fn?]
   ;; [:awesome/rules ]
   [:workspace/readme string?] ;; TODO optional filepath
   [:defthing.core/registry-key keyword?]
   [:ns string?]
   [:name string?]
   [:type keyword?] ;; TODO :clawe/workspaces
   [:git/repo string?] ;; TODO directory
   [:awesome.tag/selected boolean?]
   [:workspace/initial-file string?] ;; TODO optional filepath
   [:workspace/updated-at int?] ;; TODO time
   [:workspace/scratchpad boolean?]
   [:workspace/directory string?] ;; TODO directory
   [:workspace/exec string?] ;; TODO string or tmux/fire map
   [:awesome.tag/index int?] ;; TODO relation to awesome clients spec
   [:db/id int?] ;; TODO relation to links in db
   [:workspace/scratchpad-class string?] ;; TODO probably an enum
   [:awesome.tag/layout [:enum "tile" "centerwork"]]
   ;; [:awesome.tag/clients ]
   [:awesome.tag/name string?] ;; TODO from a workspace title enum
   [:workspace/title string?] ;; TODO from a workspace title enum
   ;; [:awesome/tag ]
   [:awesome.tag/empty boolean?]
   [:awesome.tag/urgent boolean?]])

(def client-schema
  [:map
   [:name string?]])

(def specmonstah-schema
  {:clawe/workspace {:prefix :workspace
                     :schema workspace-schema}
   :clawe/client    {:prefix :client
                     :schema client-schema}

   :user      {:prefix :u
               :schema [:map
                        [:foo/id uuid?]
                        [:user/name string?]]}
   :procedure {:prefix      :p
               :schema      [:map
                             [:foo/id uuid?]
                             [:procedure/id uuid?]]
               :relations   {:procflow.procedure/owner [:user :foo/id]
                             :procflow.procedure/steps [:step :foo/id]}
               :constraints {:procflow.procedure/steps #{:coll :uniq}} }
   :step      {:prefix :s
               :schema [:map
                        [:foo/id uuid?]
                        [:step/name string?]]}})

(defn gen-data [opts]
  (let [ent-db    (sm-malli/ent-db-spec-gen {:schema specmonstah-schema} opts)
        by-type   (-> ent-db sm/ents-by-type)
        with-data (-> ent-db (sm/attr-map :spec-gen))]
    (->> by-type
         (map (fn [[type ents]]
                [type (->> ents
                           (map (fn [ent-name]
                                  [ent-name (get with-data ent-name)]))
                           (into {}))]))
         (into {}))))


(comment
  (-> (sm-malli/ent-db-spec-gen
        {:schema specmonstah-schema}
        {:clawe/workspace [[1]]
         :clawe/client    [[1]]})
      (sm/ents-by-type))

  (-> (sm-malli/ent-db-spec-gen
        {:schema specmonstah-schema}
        {:clawe/workspace [[1]]
         :clawe/client    [[1]]})
      (sm/attr-map :spec-gen))

  (lio/view
    (:data (sm/add-ents
             {:schema specmonstah-schema}
             {:clawe/workspace [[2]]
              :clawe/client    [[1]]})))

  (gen-data
    {:clawe/workspace [[1]]
     :clawe/client    [[1]]}))

(deftest toggle-scratchpad-2-test
  (let [wsp-count 6 client-count 5
        {:keys [:clawe/workspace :clawe/client]}
        (gen-data {:clawe/workspace [[wsp-count]]
                   :clawe/client    [[client-count]]})]
    (is (= (count workspace) wsp-count))
    (is (= (count client) client-count))))
