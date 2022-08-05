(ns ralphie.awesome-test
  (:require
   [test-util]
   [clojure.test :refer [deftest is]]
   [ralphie.awesome :as awm]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; malli schemas
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; these depend on a running awesome instance, and
;; somewhat on the state of the system to run (i.e. tags, clients should exist)

(deftest screen-schema-test
  (is (valid awm/screen-schema (awm/screen))))

(deftest fetch-tags-schema-test
  (is (valid [:sequential awm/tag-schema] (awm/fetch-tags)))
  (is (valid [:sequential awm/tag-schema] (awm/fetch-tags {:include-clients true}))))

(deftest fetch-clients-schema-test
  (is (valid [:sequential awm/client-schema] (awm/all-clients))))
