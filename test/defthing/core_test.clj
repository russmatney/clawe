(ns defthing.core-test
  (:require
   [defthing.core :as sut]
   [clojure.test :as t]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Example from the doc string
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmacro defworkspace [title & args]
  (apply sut/defthing :clawe/workspaces title args))

(defworkspace my-web-workspace
  "Definition of my-web-workspace"
  {:workspace/title "web"}
  (fn [x] (assoc x :some/key "some value")))

(t/deftest basic-consumer
  ;; :name is set automatically to a string of the first arg
  (t/is (-> my-web-workspace :name (= "my-web-workspace")))
  ;; maps merge automatically
  (t/is (-> my-web-workspace :workspace/title (= "web")))
  ;; anonymous function
  (t/is (-> my-web-workspace :some/key (= "some value"))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Examples from the comment block
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmacro defsomething [title & args]
  (apply sut/defthing ::something title args))

;; testing just a map
(defsomething simpleton
  {:some/other :data})

(t/deftest simpleton-test
  (t/is (= (:some/other simpleton) :data)))

;; a short-hand anonymous function, and identity for kicks
(defsomething simpleton-with-fns
  {:just/a-map :data}
  #(assoc % :some-fn/fn :data)
  identity)

(t/deftest simpleton-with-fns-test
  ;; keeps data
  (t/is (= (:just/a-map simpleton-with-fns) :data))
  ;; calls anon
  (t/is (= (:some-fn/fn simpleton-with-fns) :data)))

;; testing a function coming before a map
(defsomething simpleton-fn-then-x
  (fn [x] (assoc x :somesecret/fun :data))
  {:funfun/data :data})

(t/deftest simpleton-fn-then-x-test
  ;; runs the function
  (t/is (= (:somesecret/fun simpleton-fn-then-x) :data))
  ;; merges the second map
  (t/is (= (:funfun/data simpleton-fn-then-x) :data)))

;; testing doc string concatenation
(defsomething simpleton-with-doc-strings
  #(assoc % :somesecret/fun :data)
  "With a doc string"
  #(assoc % :someother/fun :data/points)
  "or two"
  {:funfun/data :data})

(t/deftest simpleton-with-doc-strings-test
  ;; builds doc strings
  (t/is (= (:doc simpleton-with-doc-strings) "With a doc string\nor two"))
  ;; merges the second map
  (t/is (= (:someother/fun simpleton-with-doc-strings) :data/points)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; list-things, get-thing
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmacro defx [title & args]
  (apply sut/defthing ::x title args))

(defn list-xs [] (sut/list-things ::x))

(defx one)
(defx two {:hi :world})
(defx three)

(t/deftest list-things-test
  (let [xs (list-xs)]
    (t/is (= 3 (count xs)))
    (t/is (= #{"one" "two" "three"} (->> xs (map :name) set)))))

(defn get-x [pred] (sut/get-thing ::x pred))

(t/deftest get-thing-test
  (t/is (= "one" (-> (get-x (comp #{"one"} :name)) :name)))
  (t/is (= "two" (-> (get-x (comp #{:world} :hi)) :name))))
