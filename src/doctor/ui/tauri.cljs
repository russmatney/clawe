(ns doctor.ui.tauri
  (:require
   [cljs.core.async :refer [go]]
   [cljs.core.async.interop :refer-macros [<p!]]
   [uix.core.alpha :as uix]
   ["@tauri-apps/api" :as tauri]))

;; TODO support tauri backend api, so we don't need a tauri client to do things
;; i.e. it'd be nice to fire show/hide from a browser or keybinding

(defn tauri? [] (.. js/window -__TAURI__))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; popup window
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn show-popup []
  (when (tauri?)
    (let [popup (tauri/window.WebviewWindow.getByLabel "popup")
          popup (or popup
                    ;; quick create if missing.
                    ;; TODO probably a race-case here...
                    (do
                      (tauri/window.WebviewWindow.
                        "popup"
                        (clj->js {"url"   "/popup"
                                  "title" "tauri/doctor-popup"}))
                      (tauri/window.WebviewWindow.getByLabel "popup")))]
      (if popup
        (go
          (let [margin     200
                monitor    (<p! (tauri/window.primaryMonitor))
                max-width  (.. monitor -size -width)
                max-height (.. monitor -size -height)
                width      (- max-width (* 2 margin))
                height     (- max-height (* 2 margin))]
            ;; (js/console.log "showing popup" width height)
            (try
              ;; TODO toggle floating
              (<p! (-> popup (.show)))
              (<p! (-> popup (.setSize (tauri/window.LogicalSize. width height))))
              (<p! (-> popup (.setPosition (tauri/window.LogicalPosition. margin margin))))
              (<p! (-> popup (.setAlwaysOnTop true)))

              ;; (<p!
              ;;   (-> popup
              ;;       (.once "tauri://focus"
              ;;              (fn [data]
              ;;                (def bleh data)
              ;;                (js/alert "sup")
              ;;                (println "focused!")
              ;;                (println "data" data)
              ;;                (let [event   (.-event data)
              ;;                      payload (.-payload data)]
              ;;                  (println "event" event)
              ;;                  (println "payload" payload))))))

              (catch js/Error err (js/console.log (ex-cause err))))))
        (println "show failed! popup not found")))))

(defn hide-popup []
  (when (tauri?)
    (if-let [popup (tauri/window.WebviewWindow.getByLabel "popup")]
      (go
        (try
          (<p! (-> popup (.hide)))
          (catch js/Error err (js/console.log (ex-cause err)))))
      (println "hide-failed! popup not found"))))

(defn popup-open? []
  (when (tauri?)
    (if (tauri/window.WebviewWindow.getByLabel "popup")
      true false)))

(defn use-popup
  ;; This could become problematic - use-popup maintains it's own state, so
  ;; using the public functions won't always update it. maybe should move to a
  ;; systemic or ns-level pattern. Should also get this to be more reactive
  ;; rather than defaulting to nil, which may not be accurate for some
  ;; use-cases.
  []
  (let [open?      (uix/state nil)
        show-popup (fn []
                     (show-popup)
                     (reset! open? true))
        hide-popup (fn []
                     (hide-popup)
                     (reset! open? false))]

    ;; not sure what to depend on here...
    (uix/with-effect []
      ;; (println "use-popup")
      (when (tauri?)
        (when-let [popup (tauri/window.WebviewWindow.getByLabel "popup")]
          (go
            (let [visible (<p! (-> popup (.isVisible)))]
              (reset! open? visible)))))
      ;; clean up?
      (fn []))

    {:show   show-popup
     :hide   hide-popup
     :open?  open?
     :tauri? (tauri?)}))

(comment
  (popup-open?)
  (if (popup-open?) 1 0)
  (show-popup)
  (hide-popup)

  (tauri/window.WebviewWindow. "misc")
  (tauri/window.WebviewWindow.
    "popup"
    (clj->js {"url"   "/popup"
              "title" "tauri/doctor-popup"}))

  (def popup (tauri/window.WebviewWindow.getByLabel "popup"))
  ;; (def topbar (tauri/window.WebviewWindow.getByLabel "topbar"))

  ;; (go
  ;;   (let [m (<p! (primaryMonitor))]
  ;;     (def --m m)))

  (-> popup
      (.listen "tauri://focus"
               (fn [data]
                 (println "focused!")
                 (println "data" data)
                 (let [event   (.-event data)
                       payload (.-payload data)]
                   (println "event" event)
                   (println "payload" payload))))
      (.then println))

  (.. popup -label)
  (.. popup -listeners)
  (-> popup (.hide) (.then println))
  (-> popup (.show) (.then println))
  (-> popup (.close) (.then println))
  (-> popup (.setFocus) (.then println))
  (-> popup (.show) (.then println))
  (-> popup (.setAlwaysOnTop true) (.then println))
  (-> popup (.setAlwaysOnTop false) (.then println))

  (-> popup (.setSize (tauri/window.LogicalSize. 1600 1500)) (.then println))

  (println (js->clj (tauri/window.LogicalSize. 600 500)))

  (.all tauri/window)
  (tauri/window.WebviewWindow.)
  (tauri/window.WebviewWindow.getByLabel "popup")
  (tauri/window.WebviewWindow.getByLabel "popup2")
  (tauri/window.WebviewWindow.getByLabel "main")
  (tauri/window.WebviewWindow.getByLabel "topbar")
  (.. (tauri/window.WebviewWindow.getByLabel "topbar")
      -label))
