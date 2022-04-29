(ns clawe.workspaces-bench
  (:require
   [criterium.core :as crit]

   [clawe.workspaces :as workspaces]
   [defthing.defworkspace :as defworkspace]

   ))

(comment
  (crit/bench (Thread/sleep 1000))


  (count
    (defworkspace/list-workspaces))
  (count
    (defworkspace/latest-db-workspaces))

  (crit/bench (defworkspace/list-workspaces))
  ;; Evaluation count : 3269891160 in 60 samples of 54498186 calls.
  ;; Execution time mean : 16.731557 ns
  ;; Execution time std-deviation : 0.213694 ns
  ;; Execution time lower quantile : 16.540880 ns ( 2.5%)
  ;; Execution time upper quantile : 17.371014 ns (97.5%)
  ;; Overhead used : 1.859236 ns
  ;; Found 12 outliers in 60 samples (20.0000 %)
  ;; low-severe	 5 (8.3333 %)
  ;; low-mild	 1 (1.6667 %)
  ;; high-mild	 6 (10.0000 %)
  ;; Variance from outliers : 1.6389 % Variance is slightly inflated by outliers


  (crit/bench (defworkspace/latest-db-workspaces))
  ;; Evaluation count : 12960 in 60 samples of 216 calls.
  ;; Execution time mean : 9.216540 ms
  ;; Execution time std-deviation : 9.052453 ms
  ;; Execution time lower quantile : 4.978772 ms ( 2.5%)
  ;; Execution time upper quantile : 33.767704 ms (97.5%)
  ;; Overhead used : 1.859236 ns
  ;; Found 5 outliers in 60 samples (8.3333 %)
  ;; low-severe	 5 (8.3333 %)
  ;; Variance from outliers : 98.3072 % Variance is severely inflated by outliers


  )
