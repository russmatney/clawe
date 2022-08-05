(ns clawe.client.create-test
  (:require
   [clojure.test :refer [deftest is]]
   [clawe.client.create :as create]))

(deftest create-client-test
  (is (create/create-client)))
