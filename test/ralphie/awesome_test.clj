(ns ralphie.awesome-test
  (:require
   [backtick]
   [clojure.test :as t]
   [ralphie.awesome :as sut]
   [clojure.string :as string]
   [clojure.set :as set]))


(t/deftest awm-lua-parse-output-test
  (t/testing "parses numbers"
    (t/is (= 4.5 (sut/awm-lua "return 4.5")))
    (t/is (= 4 (sut/awm-lua "return 4"))))

  (t/testing "parses strings"
    (t/is (= "hi" (sut/awm-lua "return 'hi'"))))

  (t/testing "parses bools"
    (t/is (= true (sut/awm-lua "return true")))
    (t/is (= false (sut/awm-lua "return false"))) )

  (t/testing "parses maps"
    (t/is (= {:name "hi"} (sut/awm-lua "return view({name='hi'})"))))

  (t/testing "parses lists"
    (t/is (= [{:name "hi"} {:name "bye"}]
             (sut/awm-lua "return view({{name='hi'}, {name='bye'}})"))))

  ;; TODO ways to improve this? a follow up 'view' call?
  (t/testing "returns tables"
    (t/is (string/includes? (sut/awm-lua "return {}") "table:"))))


;; TODO use some tag that opts-out of running before restart
;; concerned about this or similar failing when first starting up (and there are no tags, for ex.)
(t/deftest screen-fetch-test
  (t/testing "returns a map with expected keys"
    (t/is (= (-> (sut/screen) keys set)
             #{:awesome.screen/tags :awesome.screen/geometry :awesome/screen})))

  (t/testing "returns a geometry"
    (let [geo (:awesome.screen/geometry (sut/screen))]
      (t/is (= (set (keys geo)) #{:width :height :x :y}))
      ))

  (t/testing "returns namespaced tags"
    (let [first-tag (-> (sut/screen) :awesome.screen/tags first)]
      (t/is (set/subset? #{:awesome.tag/name :awesome.tag/index} (set (keys first-tag)))))))

  (t/deftest fetch-tags-test
    (t/testing "returns expected keys on the tags"
      (t/is (= (-> (sut/fetch-tags) first keys set)
               #{:awesome.tag/selected :awesome.tag/index :awesome.tag/layout
                 :awesome.tag/clients :awesome.tag/name :awesome/tag
                 :awesome.tag/empty :awesome.tag/urgent})))

    (t/testing "returns namespaced clients"
      (let [client-keys (some-> (sut/fetch-tags) first :awesome.tag/clients first
                                keys set)]
        ;; for now, we don't care if there are no clients
        (when client-keys
          (t/is (= client-keys
                   #{:awesome.client/urgent :awesome.screen/geometry
                     :awesome.client/window :awesome.client/master
                     :awesome.client/class :awesome/client :awesome.client/tag-names
                     :awesome.client/type :awesome.client/name
                     :awesome.client/instance :awesome.client/focused
                     :awesome.client/pid :awesome.client/ontop}))))))

(t/deftest fetch-clients-test
  (t/testing "returns expected keys"
    (t/is (= (-> (sut/all-clients) first keys set)
             #{:awesome.client/urgent :awesome.screen/geometry
               :awesome.client/window :awesome.client/master
               :awesome.client/class :awesome/client :awesome.client/tag-names
               :awesome.client/type :awesome.client/name :awesome.client/instance
               :awesome.client/focused :awesome.client/pid :awesome.client/ontop}))))
