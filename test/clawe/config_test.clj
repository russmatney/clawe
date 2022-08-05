(ns clawe.config-test
  (:require
   [clojure.test :refer [deftest is]]
   test-util
   [clawe.config :as config]
   [clawe.client :as client]
   [clawe.workspace :as workspace]))


(deftest client-defs-schema-test
  (is (valid [:sequential client/schema] (config/client-defs))))

(deftest workspace-defs-schema-test
  (is (valid [:sequential workspace/schema] (vals (config/workspace-defs-with-titles)))))
