(ns clawe.scratchpad-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [clawe.scratchpad :as subject]
   [clawe.workspaces :as clawe.workspaces]
   [clawe.schema-test :refer [gen-data]]
   [reifyhealth.specmonstah.core :as sm]))

;; TODO generate workspace (scratchpad?) shapes
;; TODO mock/pass in (workspaces/current-workspaces)
;; TODO mock/pass in (awm/all-clients) (or some generic all-clients call)
;; TODO flesh out conditional use cases from doc string

(deftest toggle-scratchpad-test
  (testing "no-ops when there's nothing to do"
    (let [data    (gen-data {:clawe/workspace [[1 {:refs {:scratchpad/client-names ::sm/omit}}]]
                             :clawe/client    [[1]]})
          wsp     (-> data :clawe/workspace vals first)
          wsps    (-> data :clawe/workspace vals)
          clients (-> data :clawe/client vals)]
      (is (:workspace/title wsp))
      (is (:awesome.client/name (first clients)))
      (is (= [:no-op wsp]
             (subject/toggle-scratchpad-2
               wsp {:current-workspaces wsps
                    :all-clients        clients})))))

  ;; (testing "corrects client workspace when client is on the wrong workspace"
  ;;   (let [data    (gen-data {:clawe/workspace [[1]]})
  ;;         wsp     (-> data :clawe/workspace vals first)
  ;;         wsps    (-> data :clawe/workspace vals)
  ;;         clients (-> data :clawe/client vals)]
  ;;     (is (:workspace/title wsp))
  ;;     ;; a client is generated using the specmonstah :scratchpad/names relation
  ;;     (is (:awesome.client/name (first clients)))


  ;;     ;; TODO write smart test
  ;;     (let [events (subject/toggle-scratchpad-2
  ;;                    wsp {:current-workspaces wsps
  ;;                         :all-clients        clients})]
  ;;       ;; These events

  ;;       (is (= [:no-op wsp] events)))))
  )

;; TODO when workspace is not one of the current
;; If it exists anywhere, it will be found and pulled into the current workspace.
;; (this includes 'correcting' the scratchpad's tag if it is wrong)
;; If it is already in the current workspace but not focused, it will be focused.
;; If it is in focus, it will be removed (hidden).
;; Once removed, a previously focused, just-buried scratchpad will have its focus restored.

(comment
  (->> {}
       (map :some-key)
       (apply concat))
  )
