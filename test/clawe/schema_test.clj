(ns clawe.schema-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [lambdaisland.specmonstah.malli :as sm-malli]
   [reifyhealth.specmonstah.core :as sm]
   [malli.generator :as mg]
   [malli.util :as mu]
   [loom.io :as lio]
   [test-util :refer [gen-data]]))

;; NOTE this is a test-only namespace for working with specmonstah-malli
;; could delete at some point

(def scratchpad-schema
  [:map
   [:workspace/scratchpad boolean?]
   [:workspace/scratchpad-class string?] ;; TODO probably an enum
   [:scratchpad/client-names [:set string?]]])

(def awesome-tag-schema
  [:map
   [:awesome.tag/selected boolean?]
   [:awesome.tag/index int?]      ;; TODO relation to awesome clients spec
   [:awesome.tag/layout [:enum "tile" "centerwork"]]
   ;; [:awesome.tag/clients ]
   [:awesome.tag/name string?] ;; TODO from a workspace title enum
   ;; [:awesome/tag ]
   [:awesome.tag/empty boolean?]
   [:awesome.tag/urgent boolean?]])

(def defworkspace-schema
  [:map
   [:workspace/readme string?] ;; TODO optional filepath
   [:defthing.core/registry-key keyword?]
   [:ns string?]
   [:name string?]
   [:type keyword?]  ;; TODO :clawe/workspaces
   [:workspace/initial-file string?] ;; TODO optional filepath
   [:workspace/updated-at int?]      ;; TODO time
   [:workspace/directory string?] ;; TODO directory
   [:workspace/exec string?]      ;; TODO string or tmux/fire map
   [:db/id int?]                  ;; TODO relation to links in db
   [:workspace/title string?]]) ;; TODO from a workspace title enum

(def workspace-schema
  "Basic workspace-schema"
  ;; TODO improve these types/generators
  (reduce
    mu/merge
    [scratchpad-schema
     awesome-tag-schema
     defworkspace-schema
     [:map [:git/repo string?]]]))

(comment
  (mg/generate workspace-schema))

(def awesome-client-schema
  [:map
   [:awesome.client/name string?]
   [:awesome.client/class string?]
   [:awesome.client/tag-names [:set string?]]])

;; TODO abstract `:awesome` usage, move toward clawe ':client/' prefixed field names
(def client-schema
  awesome-client-schema)

(comment
  (mg/generate client-schema))

(def schema
  {;; TODO this one (with relations to clients) is actually a 'scratchpad'
   :clawe/workspace
   {:prefix      :workspace
    :schema      workspace-schema
    :relations   {:scratchpad/client-names [:clawe/client :awesome.client/name]}
    :constraints {:scratchpad/client-names #{:uniq :coll}}}

   :clawe/workspace-no-client
   {:prefix :workspace-no-client
    :schema workspace-schema}

   :clawe/client
   {:prefix      :client
    :schema      client-schema
    :relations   {:awesome.client/tag-names [:clawe/workspace :workspace/title]}
    :constraints {:awesome.client/tag-names #{:coll :required}}}

   :clawe/client-no-workspace
   {:prefix :client-no-workspace
    :schema client-schema}})

(deftest gen-data-test
  (testing "workspace and client count test"
    (let [tests [{:wsp-count          7 :client-count          5
                  ;; clients are created for workspaces, unless relations are omitted
                  :expected-wsp-count 7 :expected-client-count 7}
                 {:wsp-count          7 :client-count          9
                  :expected-wsp-count 7 :expected-client-count 9}]]

      (doall
        (for [{:keys [wsp-count client-count
                      expected-wsp-count
                      expected-client-count]} tests]
          (let [{:keys [:clawe/workspace :clawe/client]}
                (gen-data schema {:clawe/workspace [[wsp-count]]
                                  :clawe/client    [[client-count]]})]
            (is (= (count workspace) expected-wsp-count))

            ;; clients are created for each workspace
            (is (= (count client) expected-client-count)))))))

  (testing "workspaces imply client creation"
    (let [{:keys [:clawe/workspace :clawe/client]}
          (gen-data schema {:clawe/workspace [[1]]})]
      (is (= (count workspace) 1))
      (is (= (count client) 1))))

  (testing "workspaces can omit clients"
    (let [{:keys [:clawe/workspace :clawe/client]}
          (gen-data schema {:clawe/workspace [[1 {:refs {:scratchpad/client-names ::sm/omit}}]]})]
      (is (= (count workspace) 1))
      (is (= client nil))))

  (testing "workspace-no-client"
    (let [{:keys [:clawe/workspace-no-client :clawe/client]}
          (gen-data schema {:clawe/workspace-no-client [[1]]})]
      (is (= (count workspace-no-client) 1))
      (is (= client nil))))

  (testing "clients imply workspace creation"
    (let [{:keys [:clawe/workspace :clawe/client]}
          (gen-data schema {:clawe/client [[1]]})]
      (is (= (count client) 1))
      (is (= (count workspace) 1))))

  (testing "clients can omit workspaces"
    (let [{:keys [:clawe/workspace :clawe/client]}
          (gen-data schema {:clawe/client [[1 {:refs {:awesome.client/tag-names ::sm/omit}}]]})]
      (is (= (count client) 1))
      (is (= workspace nil))))

  (testing "client-no-workspace"
    (let [{:keys [:clawe/workspace :clawe/client-no-workspace]}
          (gen-data schema {:clawe/client-no-workspace [[1]]})]
      (is (= workspace nil))
      (is (= (count client-no-workspace) 1)))))

(comment
  (-> (sm-malli/ent-db-spec-gen
        {:schema schema}
        {:clawe/workspace [[1]
                           [1 {:refs {:scratchpad/client-names ::sm/omit}}]]
         :clawe/client    [[1]]})
      (sm/attr-map :spec-gen))

  (lio/view
    (:data (sm/add-ents
             {:schema schema}
             {:clawe/workspace [[2]]
              :clawe/client    [[1]]})))

  (lio/view
    (:data (sm/add-ents
             {:schema schema}
             {:clawe/workspace [[6]]
              :clawe/client    [[5]]})))

  (lio/view
    (:data (sm/add-ents
             {:schema schema}
             {:clawe/workspace [[1]
                                [2 {:refs {:scratchpad/client-names ::sm/omit}}]]
              :clawe/client    [[1]]}))))
