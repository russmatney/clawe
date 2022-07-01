(ns clawe.scratchpad-test
  (:require
   [clojure.test :refer [deftest is]]
   [malli.transform :as mt]
   [malli.core :as m]
   [lambdaisland.specmonstah.malli :as sm-malli]
   [reifyhealth.specmonstah.core :as sm]
   [clawe.scratchpad :as subject]
   [clawe.workspaces :as clawe.workspaces]))

;; TODO generate workspace (scratchpad?) shapes
;; TODO mock/pass in (workspaces/current-workspaces)
;; TODO mock/pass in (awm/all-clients) (or some generic all-clients call)
;; TODO flesh out conditional use cases from doc string

(deftest toggle-scratchpad-2-test
  (is (= nil
         nil
         ;; (subject/toggle-scratchpad-2 nil)
         )))

(def workspace-schema
  "A basic workspace-schema"
  [:map
   ;; [:rules/is-my-client? fn?]
   ;; [:awesome/rules ]
   [:workspace/readme string?]
   [:defthing.core/registry-key keyword?]
   [:ns string?]
   [:name string?]
   [:type keyword?]
   [:git/repo string?]
   [:awesome.tag/selected boolean?]
   [:workspace/initial-file string?]
   [:workspace/updated-at int?] ;; TODO time
   [:workspace/scratchpad boolean?]
   [:workspace/directory string?]
   [:workspace/exec string?] ;; TODO or tmux/fire map?
   [:awesome.tag/index int?]
   [:db/id int?]
   [:workspace/scratchpad-class string?]
   [:awesome.tag/layout string?]
   ;; [:awesome.tag/clients ]
   [:awesome.tag/name string?]
   [:workspace/title string?]
   ;; [:awesome/tag ]
   [:awesome.tag/empty boolean?]
   [:awesome.tag/urgent boolean?]])

(def thing-schema
  [:map
   [:some/string string?]
   [:some/keyword keyword?]
   [:some/int int?]])

(deftest malli-test
  (is (= (m/decode
           thing-schema
           {:some/string    "hi"
            :some/keyword   :yo
            :some/int       5
            :something/else "blah"}
           (mt/strip-extra-keys-transformer))
         {:some/string  "hi"
          :some/keyword :yo
          :some/int     5})))


(def specmonstah-schema
  {:user      {:prefix :u
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

(comment
  (-> (sm-malli/ent-db-spec-gen {:schema specmonstah-schema}
                                {
                                 :user      [[1]]
                                 :procedure [[3]]
                                 :step      [[10]]})
      (sm/attr-map :spec-gen)
      vals))

(deftest specmonstah-malli-test
  (is (=
        (count
          (-> (sm-malli/ent-db-spec-gen {:schema specmonstah-schema}
                                        {
                                         :user      [[1]]
                                         :procedure [[3]]
                                         :step      [[10]]})
              (sm/attr-map :spec-gen)
              vals))
        14)))

(comment
  (def -w
    (->> (clawe.workspaces/all-workspaces)
         (filter :awesome.tag/name)
         first
         ))


  (m/decode
    workspace-schema
    -w
    (mt/strip-extra-keys-transformer))

  )
