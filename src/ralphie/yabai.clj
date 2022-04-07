(ns ralphie.yabai
  (:require
   [babashka.process :as process]
   [cheshire.core :as json]
   [wing.core :as w]
   [ralphie.notify :as notify]))

;; TODO get this frame decoding done properly
(def Frame
  [:map
   [:x float?]
   [:y float?]
   [:w float?]
   [:h float?]])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Displays
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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
    (json/parse-string (fn [k] (keyword "yabai.display" k)))))

(comment
  (query-displays)

  (keyword "my" (name :hi)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; spaces
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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

(defn spaces-by-idx []
  (->> (query-spaces) (w/index-by :yabai.space/index)))

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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; windows
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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

(defn query-current-window []
  (->
    ^{:out :string}
    (process/$ yabai -m query --windows --window)
    process/check
    :out
    (json/parse-string (fn [k] (keyword "yabai.window" k)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; yabai commands
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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; finding and labelling spaces
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn windows-for-app-name [app-name]
  ;; TODO maybe extend to match on app and title
  (->>
    (query-windows)
    (filter (comp #{app-name} :yabai.window/app))))

(defn window-for-app-name [app-name]
  (->>
    (windows-for-app-name app-name)
    first)
  )

(comment
  (windows-for-app-name "Spotify")
  (windows-for-app-name "Emacs"))

(defn space-for-window [window]
  (let [spcs-by-idx (spaces-by-idx)]
    (->
      window
      :yabai.window/space
      spcs-by-idx)))

(defn label-space
  [label space]
  (println "label-space" label space)
  (when (and label space)
    (let [space-idx (:yabai.space/index space)]
      (notify/notify (str "Labelling space: " space-idx) label)
      (->
        ^{:out :string}
        (process/$ yabai -m space ~space-idx --label ~label)
        process/check
        :out))))

(defn find-and-label-space [app-name label]
  (let [windows (windows-for-app-name app-name)]
    (cond
      (#{1} (count windows))
      (do
        (notify/notify "Found window" app-name)
        (->>
          windows
          first
          space-for-window
          (label-space label)))

      (zero? (count windows))
      (notify/notify "No windows for app name, could not label space" app-name)

      (> (count windows) 1)
      ;; TODO but if they're in the same space, maybe just do it?
      (notify/notify "Multiple windows for app name, could not label space" app-name))))

(defn move-window-to-space [window space-label-or-idx]
  (->
    ^{:out :string}
    (process/$ yabai -m window ~(:yabai.window/id window) --space ~space-label-or-idx)
    process/check
    :out))

(comment

  (move-window-to-space
    (window-for-app-name "Safari")
    "web"
    )
  )

(defn focus-window [window]
  (->
    ^{:out :string}
    (process/$ yabai -m window ~(:yabai.window/id window) --focus)
    process/check
    :out))

(defn float-and-center-window
  "Toggles floating if the passed window is not already."
  [{:yabai.window/keys [id is-floating] :as w}]
  (println "\nw" w)
  ;; ensure floating
  (when-not is-floating
    (->
      ^{:out :string}
      (process/$ yabai -m window ~id --toggle float)
      process/check
      :out))

  (->
    ^{:out :string}
    (process/$ yabai -m window ~id --grid "10:10:1:1:8:8")
    process/check
    :out))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; sandbox

(comment
  (spaces-by-idx)
  (->>
    (spaces-by-idx)
    (vals)
    (map :yabai.space/label))

  (find-and-label-space "Spotify" "spotify")
  (find-and-label-space "Slack" "slack")
  (find-and-label-space "Safari" "web")
  (find-and-label-space "Emacs" "clawe")

  (->>
    (windows-for-app-name "Emacs")
    first
    space-for-window)

  (let [spcs-by-idx (spaces-by-idx)]
    (->
      (windows-for-app-name "Safari")
      first
      :yabai.window/space
      spcs-by-idx))
  )
