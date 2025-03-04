(ns ralphie.yabai
  (:require
   [babashka.process :as process]
   [cheshire.core :as json]
   [wing.core :as w]
   [babashka.fs :as fs]
   [defthing.defcom :refer [defcom]]
   [ralphie.notify :as notify]
   [clojure.string :as string]
   [clojure.edn :as edn]
   [ralphie.rofi :as rofi]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; prompt and label space

(defn get-input
  "Prompts for user input in a native dialog via obb."
  []
  (->
    ^{:dir (str (fs/home) "/russmatney/clawe")
      :out :string
      :err :string}
    ;; TODO pass in dialog messages
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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; yabai message
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


;; TODO refactor into something like this
;; (defn yabai-m [cmd]
;;   (->
;;     ^{:out :string}
;;     (process/$ yabai -m ~@cmd)
;;     process/check
;;     :out))

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
  (try
    (->
      ^{:out :string}
      (process/$ yabai -m query --spaces)
      process/check
      :out
      (json/parse-string (fn [k] (keyword "yabai.space" k))))
    (catch Exception _e
      nil)))

(defn spaces-by-idx []
  (->> (query-spaces) (w/index-by :yabai.space/index)))

(defn spaces-by-label []
  (->> (query-spaces)
       (remove (comp empty? :yabai.space/label))
       (w/index-by :yabai.space/label)))

(defn spaces-unlabeled []
  (->> (query-spaces)
       (filter (comp empty? :yabai.space/label))))


(comment
  (query-spaces)
  (spaces-by-idx)
  (spaces-by-label)
  )

(defn swap-spaces-by-index [index new-index]
  (when (and index new-index)
    (->
      ^{:out :string}
      (process/$ yabai -m space ~index --swap ~new-index)
      process/check
      :out)))

(defn drag-workspace [dir]
  (let [dir (case dir :dir/up "next" :dir/down "prev")]
    (->
      ^{:out :string}
      (process/$ yabai -m space --move ~dir)
      process/check
      :out)))

(comment
  (drag-workspace :dir/up)
  (swap-spaces-by-index 4 1)
  (->>
    (spaces-by-idx)
    (map (fn [[idx wsp]]
           [idx (-> wsp :yabai.space/label )]))
    (sort-by first)))

(defn query-current-space []
  (try
    (->
      ^{:out :string}
      (process/$ yabai -m query --spaces --space)
      process/check
      :out
      (json/parse-string (fn [k] (keyword "yabai.space" k))))
    (catch Exception _e nil)))

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

(def minimal-wm-fields
  (string/join
    " " ["id"
         "app"
         "has-focus"
         "space"
         "is-sticky"
         "title"
         "is-floating"]))

;; TODO does this have a format-string/read-edn style?
;; TODO refactor to use these malli schemas
(defn query-windows
  ([] (query-windows nil))
  ([{:keys [fields]}]
   (println "yabai query-windows start")
   (let [fields (if (nil? fields) nil minimal-wm-fields)
         windows
         (try
           (->
             ^{:out :string}
             (process/$ yabai -m query --windows ~fields)
             process/check
             :out
             (json/parse-string (fn [k] (keyword "yabai.window" k))))
           (catch Exception e
             (println "yabai query-windows exception" e)
             nil))]
     (println "yabai query-windows found dows" windows)
     windows
     )))

(comment
  (query-windows)
  )

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
  (let [window-id (or (:yabai.window/id w)
                      (:window-id w))]
    (when window-id
      (println "focusing window with window-id" window-id)
      (->
        ^{:out :string}
        (process/$ yabai -m window --focus ~window-id)
        process/check
        :out))))

(comment
  (->> (query-windows)
       (filter (comp #{"Spotify"} :yabai.window/app))
       first
       focus-window))

(defn focus-window-in-current-space
  "Typically unnecessary (osx tries to focus windows for you).
  Perhaps better handled via yabai signals."
  []
  (let [current-space (query-current-space)
        window-ids    (:yabai.space/windows current-space)
        window-id     (some-> window-ids first)]
    (println "focus window attempt" current-space)
    (when window-id
      (println "focusing window" window-id)
      (focus-window {:window-id window-id}))))

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
      (filter
        (fn [w]
          (and
            (-> w :yabai.window/app
                ((fn [app-str]
                   (if (string? app-name)
                     (#{app-name} app-str)
                     (app-name app-str)))))
            (if-not title-match
              true
              (-> w :yabai.window/title
                  ((fn [app-str]
                     (if (string? title-match)
                       (#{title-match} app-str)
                       (title-match app-str))))))))))))

(defn window-for-app-desc [app-desc]
  (->>
    (windows-for-app-desc app-desc)
    first))

(def ->window window-for-app-desc)


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
       (w/index-by :yabai.space/index)))

(comment
  (-> {:yabai.window/app "Spotify"
       ;; :yabai.window/title #(re-seq #"journal" %)
       } spaces-for-app-desc))

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
  (let [spaces-by-idx (spaces-for-app-desc app-desc)
        str-app-name  (if (string? app-desc) app-desc (:yabai.window/app app-desc))
        ;; could be a function/obj, which notify on osx can't deal with yet
        str-app-name  (when (string? str-app-name) str-app-name)]
    (cond
      (#{1} (count spaces-by-idx))
      (do
        (notify/notify "Found single space for window(s)" (str (first (keys spaces-by-idx)) " - " str-app-name))
        (->> spaces-by-idx vals first (label-space label)))

      (zero? (count spaces-by-idx))
      (notify/notify "No spaces for app name, no space to label" str-app-name)

      (> (count spaces-by-idx) 1)
      (notify/notify "Multiple spaces for app desc.... could not label space" str-app-name))))

(defn toggle-floating [{:yabai.window/keys [id] :as _window}]
  (->
    ^{:out :string}
    (process/$ yabai -m window ~id --toggle float)
    process/check
    :out))

(comment
  (toggle-floating
    (->window
      {:yabai.window/app   "Emacs"
       :yabai.window/title #(re-seq #"clawe" %)})))

(defn center-window
  "

  From yabai manual:
  --grid <rows>:<cols>:<start-x>:<start-y>:<width>:<height>
    Set the frame of the selected window based on a
    self-defined grid.

  "
  [window]
  (->
    ^{:out :string}
    (process/$ yabai -m window ~(:yabai.window/id window) --grid "50:50:1:1:48:48")
    process/check
    :out))


(defn is-in-space-for-label?
  "Does the window's space match the passed label?"
  [window label]
  (->>
    (query-spaces)
    (filter (fn [spc]
              (and
                (-> spc :yabai.space/index #{(:yabai.window/space window)})
                (-> spc :yabai.space/label #{label}))))
    seq))

(defn move-window-to-space
  "If the window is already in the space, its float will be toggled
  and the window will be centered.
  "
  [window space-label-or-idx]
  ;; TODO could probably be more performant than this
  (if (is-in-space-for-label? window space-label-or-idx)
    (do
      (notify/notify "i'm in my space, cozy as can be")
      (toggle-floating window)
      (center-window window))
    (if (and (:yabai.window/id window)
             space-label-or-idx)
      (->
        ^{:out :string}
        #_(process/$ yabai -m window ~(:yabai.window/id window) --space ~space-label-or-idx || yabai -m space --focus recent)
        (process/$ yabai -m window ~(:yabai.window/id window) --space ~space-label-or-idx)
        process/check
        :out)
      (throw (Exception. (str "Missing required move-window-to-space data"
                              window
                              space-label-or-idx))))))

(comment

  (move-window-to-space
    (window-for-app-desc "Safari")
    "web")

  (query-windows)


  (->>
    (query-spaces)
    (filter (comp #{(->
                      {:yabai.window/app   "Emacs"
                       :yabai.window/title #(re-seq #"clawe" %)}
                      ->window
                      :yabai.window/space
                      )} :yabai.space/index)))
  (spaces-by-idx)

  (move-window-to-space
    (->window {:yabai.window/app "Emacs" :yabai.window/title #(re-seq #"clawe" %)})
    "clawe")
  )

(defn float-and-center-window
  "Toggles floating if the passed window is not already."
  [{:yabai.window/keys [is-floating] :as window}]
  ;; ensure floating
  (when-not is-floating
    (toggle-floating window))

  (center-window window))

(comment
  "
--move abs|rel:<dx>:<dy>
If type is rel the selected window is moved by dx pixels
horizontally and dy pixels vertically, otherwise dx and
dy will become its new position. "

  )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; focus space
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn focus-space
  "Focus the space with the passed label."
  [wsp-or-label]
  (let [label (:workspace/title wsp-or-label wsp-or-label)]
    (if (or (not label) (empty? label))
      (throw (Exception. (str "Could not find label with passed wsp-or-label" wsp-or-label)))
      (try
        (->
          ^{:out :string}
          (process/$ yabai -m space --focus ~label)
          process/check
          :out)
        (catch Exception e
          (if (re-seq #"already focused" (str e))
            (println "Ignoring space already focused err")
            (throw e)))))))

(comment
  (focus-space "clawe")
  )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; create space

(defn ensure-labeled-space
  "If a space with the passed label does not exist,
  (and overwrite-unlabeled is truthy) an unlabeled one is
  selected and labeled. If there are no unlabeled, a new one is created.
  "
  [{:keys [space-label overwrite-unlabeled]}]
  (let [spcs    (spaces-by-label)
        ;; naive, just checks if a space already has this label
        ;; could one day determine if an existing space should be labeled
        exists? (spcs space-label)]
    (when-not exists?
      (let [unlabeled-space (when overwrite-unlabeled
                              (->> (spaces-unlabeled) first))]
        (when-not unlabeled-space
          (->
            ^{:out :string}
            (process/$ yabai -m space --create)
            process/check
            :out))
        (let [space (or unlabeled-space
                        (->> (query-spaces)
                             (remove :yabai.space/is-native-fullscreen)
                             (sort-by :yabai.space/index)
                             reverse
                             first))]
          (label-space space-label space))))))

(comment
  (ensure-labeled-space {:space-label "new-space"
                         :focus       true})
  (->>
    (spaces-by-idx)
    (sort-by first)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; clean and destroy
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn destroy-space [{:keys             [space-label]
                      :yabai.space/keys [label index]
                      :as               _input}]
  (let [label (or label space-label index)]
    (when label
      (notify/notify "Destroying space" label)
      (->
        ^{:out :string}
        (process/$ yabai -m space --destroy ~label)
        process/check
        :out))))

(defcom destroy-current-space
  (destroy-space (query-current-space)))

(defcom select-and-destroy-space
  (let [selected (rofi/rofi (->> (query-spaces)
                                 (map (fn [spc]
                                        (assoc spc :rofi/label (or (:yabai.space/label spc)
                                                                   (:yabai.space/index spc)))))))]
    (when selected
      (destroy-space (:yabai.space/index selected)))))

(comment
  (destroy-space {:space-label "2"})
  (spaces-by-idx))

(defn destroy-unlabelled-empty-spaces []
  (doall
    (->> (query-spaces)
         (filter (comp empty? :yabai.space/label))
         (remove (comp seq :yabai.space/windows))
         (map destroy-space))))

(comment
  (->> (query-spaces)
       (remove (comp not empty? :yabai.space/label))
       (remove (comp seq :yabai.space/windows))))

(defn label-space-with-user-input []
  (let [new-label     (get-input)
        current-space (query-current-space)]
    (when new-label
      (label-space new-label current-space))))

(defcom set-new-label-for-space
  (label-space-with-user-input))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; fix topbar
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


;; yabai -m window
;;  $(yabai -m query --windows | jq '[.[] | select(.title == "tauri-doctor-topbar")][0].id')
;; --move abs:0:1134
;; && yabai -m window $(yabai -m query --windows | jq '[.[] | select(.title == "tauri-doctor-topbar")][0].id')
;; --resize abs:1800:36

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn fix-topbar
  ([] (fix-topbar nil))
  ([_]
   (println "fixing topbar")
   (let [topbar-window (some->> (query-windows)
                                (filter :yabai.window/title)
                                (filter (comp #(string/includes? % "tauri-doctor-topbar") :yabai.window/title))
                                first)]
     (when-not topbar-window
       (println "no topbar window found"))
     (when topbar-window
       (->
         ^{:out :string}
         (process/$ yabai -m window ~(:yabai.window/id topbar-window) --move "abs:0:1134")
         process/check
         :out)
       (->
         ^{:out :string}
         (process/$ yabai -m window ~(:yabai.window/id topbar-window) --resize "abs:1800:36")
         process/check
         :out)))))
