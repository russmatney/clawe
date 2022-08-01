(ns clawe.yabai
  (:require
   [clawe.wm.protocol :refer [ClaweWM]]
   [ralphie.yabai :as yabai]))

(defrecord Yabai [] ClaweWM)
