(ns ralphie.yabai
  (:require
   [babashka.process :as process]
   [cheshire.core :as json]
   ;; [malli.core :as m]
   ;; [malli.transform :as mt]
   ;; [malli.provider :as mp]
   ))

;; TODO get this frame decoding done properly
(def Frame
  [:map
   [:x float?]
   [:y float?]
   [:w float?]
   [:h float?]])

(def Display
  [:map
   [:yabai.display/id int?]
   [:yabai.display/uuid :uuid]
   [:yabai.display/index int?]
   [:yabai.display/frame Frame]
   [:yabai.display/spaces [:vector int?]]])

(defn query-displays []
  (->
    ^{:out :string}
    (process/$ yabai -m query --displays)
    process/check
    :out
    (json/parse-string (fn [k] (keyword "yabai.display" k)))
    ;; (->> (map #(m/encode
    ;;              Display %
    ;;              (mt/key-transformer
    ;;                {:encode (fn [k]
    ;;                           (keyword "yabai.display" (name k)))}))))
    ))

(comment
  (query-displays)

  (keyword "my" (name :hi))

  (->>
    (query-displays)
    (first)
    (#(m/decode Display % mt/json-transformer)))

  (mp/provide (query-displays)))

(def Space
  [:map
   [:yabai.space/windows [:vector int?]]
   [:yabai.space/index int?]
   [:yabai.space/is-native-fullscreen boolean?]
   [:yabai.space/type string?]
   [:yabai.space/label string?]
   [:yabai.space/id int?]
   [:yabai.space/is-visible boolean?]
   [:yabai.space/has-focus boolean?]
   [:yabai.space/display int?]
   [:yabai.space/last-window int?]
   [:yabai.space/uuid string?]
   [:yabai.space/first-window int?]])

(defn query-spaces []
  (->
    ^{:out :string}
    (process/$ yabai -m query --spaces)
    process/check
    :out
    (json/parse-string (fn [k] (keyword "yabai.space" k)))))

(comment
  (query-spaces))

(defn query-current-space []
  (->
    ^{:out :string}
    (process/$ yabai -m query --spaces --space)
    process/check
    :out
    (json/parse-string (fn [k] (keyword "yabai.space" k)))))

(comment
  (query-current-space))

(def Window
  [:map
   [:yabai.window/role string?]
   [:yabai.window/has-shadow boolean?]
   [:yabai.window/space int?]
   [:yabai.window/is-minimized boolean?]
   [:yabai.window/frame Frame]
   [:yabai.window/is-native-fullscreen boolean?]
   [:yabai.window/is-sticky boolean?]
   [:yabai.window/has-parent-zoom boolean?]
   [:yabai.window/stack-index int?]
   [:yabai.window/title string?]
   [:yabai.window/level int?]
   [:yabai.window/can-move boolean?]
   [:yabai.window/pid int?]
   [:yabai.window/is-floating boolean?]
   [:yabai.window/split-type string?]
   [:yabai.window/subrole string?]
   [:yabai.window/is-grabbed boolean?]
   [:yabai.window/opacity double?]
   [:yabai.window/id int?]
   [:yabai.window/is-hidden boolean?]
   [:yabai.window/app string?]
   [:yabai.window/is-visible boolean?]
   [:yabai.window/has-focus boolean?]
   [:yabai.window/display int?]
   [:yabai.window/has-fullscreen-zoom boolean?]
   [:yabai.window/is-topmost boolean?]
   [:yabai.window/has-border boolean?]
   [:yabai.window/can-resize boolean?]])

(defn query-windows []
  (->
    ^{:out :string}
    (process/$ yabai -m query --windows)
    process/check
    :out
    (json/parse-string (fn [k] (keyword "yabai.window" k)))))

(comment
  (query-windows))

(defn query-current-window []
  (->
    ^{:out :string}
    (process/$ yabai -m query --windows --window)
    process/check
    :out
    (json/parse-string (fn [k] (keyword "yabai.window" k)))))

(comment
  (query-current-window)

  (def raw
    (->
      ^{:out :string}
      (process/$ yabai -m query --windows --window)
      process/check
      :out))

  (m/decode Window
            (json/parse-string raw true)
            mt/json-transformer))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn focus-window [w]
  (println "focusing yabai window" w)
  (->
    ^{:out :string}
    (process/$ yabai -m window --focus ~(:yabai.window/id w))
    process/check
    :out))

(comment
  (->> (query-windows)
       (filter (comp #{"Spotify"} :yabai.window/app))
       first
       focus-window))

(defn close-window
  "heads up that this requires the application to have a title bar.
  Apparently the implementation simulates a click of the red x.
  could resort to pkill or something else, as we have the pid....
  "
  [w]
  (println "closing yabai window" w)
  (->
    ^{:out :string}
    (process/$ yabai -m window --close ~(:yabai.window/id w))
    process/check
    :out))

;; osascript -e
;;   'tell application "System Events"
;;      to perform action "AXPress" of (first button whose subrole is
;;        "AXCloseButton") of
;;          (first window whose subrole is "AXStandardWindow")
;;                of (first process whose frontmost is true)'

(comment
  (->> (query-windows)
       (filter (comp #{"Safari"} :yabai.window/app))
       first
       close-window))
