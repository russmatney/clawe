(ns ralphie.yabai
  (:require
   [babashka.process :as process]
   [cheshire.core :as json]
   [wing.core :as w]
   [defthing.defcom :refer [defcom]]
   [ralphie.notify :as notify]
   [ralphie.zsh :as zsh]
   [clojure.string :as string]
   [clojure.edn :as edn]))

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

(defn windows-for-app-desc [app-desc]
  (let [app-name    (if (string? app-desc) app-desc
                        (:yabai.window/app app-desc))
        title-match (if (string? app-desc) app-desc
                        (:yabai.window/title app-desc))]
    (->>
      (query-windows)
      (filter (fn [w]
                (and
                  (-> w :yabai.window/app ((fn [app-str]
                                             (if (string? app-name)
                                               (#{app-name} app-str)
                                               (app-name app-str)))))
                  (if-not title-match
                    true
                    (-> w :yabai.window/title ((fn [app-str]
                                                 (if (string? title-match)
                                                   (#{title-match} app-str)
                                                   (title-match app-str))))))))))))

(defn window-for-app-desc [app-desc]
  (->>
    (windows-for-app-desc app-desc)
    first)
  )

(comment
  (windows-for-app-desc "Spotify")
  (windows-for-app-desc "Emacs"))

(defn space-for-window [window]
  (let [spcs-by-idx (spaces-by-idx)]
    (->
      window
      :yabai.window/space
      spcs-by-idx)))

(defn spaces-for-app-desc
  "Returns spaces indexed by <label>-<idx>, or just <idx> if no <label> is set."
  [app-desc]
  (->> app-desc
       windows-for-app-desc
       (map space-for-window)
       (w/index-by #(str (when-not (empty? (:yabai.space/label %))
                           (:yabai.space/label %) "-")
                         (:yabai.space/index %)))))

(comment

  (->
    {:yabai.window/app "Emacs"
     ;; :yabai.window/title #(re-seq #"journal" %)
     }
    spaces-for-app-desc
    )

  (->
    {:yabai.window/app "Spotify"
     ;; :yabai.window/title #(re-seq #"journal" %)
     }
    spaces-for-app-desc
    )
  )

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

(defn find-and-label-space [app-desc label]
  (let [spaces-map   (spaces-for-app-desc app-desc)
        str-app-name (if (string? app-desc) app-desc (:yabai.window/app app-desc))
        ;; could be a function/obj, which notify on osx can't deal with yet
        str-app-name (when (string? str-app-name) str-app-name)]
    (cond
      (#{1} (count spaces-map))
      (do
        (notify/notify "Found single space for window(s)" (str (first (keys spaces-map)) " - " str-app-name))
        (->> spaces-map vals first (label-space label)))

      (zero? (count spaces-map))
      (notify/notify "No spaces for app name, no space to label" str-app-name)

      (> (count spaces-map) 1)
      (notify/notify "Multiple spaces for app desc.... could not label space" str-app-name))))

(def app-desc->space-label
  {{:yabai.window/app "Spotify"}                "spotify"
   {:yabai.window/app "Slack"}                  "slack"
   {:yabai.window/app "Safari"}                 "web"
   {:yabai.window/app   "Emacs"
    :yabai.window/title #(re-seq #"journal" %)} "journal"
   {:yabai.window/app   "Emacs"
    :yabai.window/title #(re-seq #"clawe" %)}   "clawe"})

(defn set-space-labels []
  (doall
    (->> app-desc->space-label
         (map (fn [[desc label]]
                (find-and-label-space desc label))))))

(comment
  (set-space-labels)
  (->>
    (spaces-by-idx)
    (vals)
    (map :yabai.space/label))

  (->>
    app-desc->space-label
    (map (fn [[desc]]
           desc
           )
         )
    )

  (find-and-label-space "Spotify" "spotify")
  (find-and-label-space "Slack" "slack")
  (find-and-label-space "Safari" "web")
  (find-and-label-space "Emacs" "clawe")
  )

(defcom yabai-set-space-labels
  set-space-labels)


(defn move-window-to-space [window space-label-or-idx]
  (->
    ^{:out :string}
    (process/$ yabai -m window ~(:yabai.window/id window) --space ~space-label-or-idx)
    process/check
    :out))

(comment

  (move-window-to-space
    (window-for-app-desc "Safari")
    "web"
    )
  )



(defn float-and-center-window
  "Toggles floating if the passed window is not already."
  [{:yabai.window/keys [id is-floating] :as w}]
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
;; prompt and label space

(defn get-input
  "Prompts for user input in a native dialog via obb."
  []
  (->
    ^{:dir (zsh/expand "~/russmatney/clawe")
      :out :string
      :err :string}
    (process/$ "./bin/dialog.clj")
    process/check
    ;; TODO for now, obb prints outputs to stderr
    ;; https://github.com/babashka/obb/issues/16
    :err
    string/split-lines
    first
    edn/read-string))

(comment
  (get-input))

(defn label-space-with-user-input []
  (let [new-label     (get-input)
        current-space (query-current-space)]
    (when new-label
      (label-space new-label current-space))))

(defcom set-new-label-for-space
  (label-space-with-user-input))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; sandbox

(comment
  (spaces-by-idx)

  (->>
    (windows-for-app-desc "Emacs")
    first
    space-for-window)

  (let [spcs-by-idx (spaces-by-idx)]
    (->
      (windows-for-app-desc "Safari")
      first
      :yabai.window/space
      spcs-by-idx))
  )
