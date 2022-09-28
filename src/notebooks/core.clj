^{:nextjournal.clerk/visibility {:code :hide :result :hide}}
(ns notebooks.core
  (:require
   [nextjournal.clerk :as clerk]
   [notebooks.system :as system]
   [notebooks.nav :as nav]))


^{::clerk/visibility {:code :hide}
  ::clerk/no-cache   true
  ::clerk/viewer     nav/nav-viewer}
nav/nav-options

;; Core

^{::clerk/visibility {:code :hide :result :hide}}
(defn rerender []
  (system/rerender))
