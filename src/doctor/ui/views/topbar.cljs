(ns doctor.ui.views.topbar
  (:require
   [clojure.string :as string]
   [hiccup-icons.fa :as fa]
   [tick.core :as t]
   [uix.core.alpha :as uix]

   [components.icons :as icons]
   [components.charts :as charts]
   [components.actions :as components.actions]

   [hooks.todos :as hooks.todos]
   [hooks.topbar :as hooks.topbar]
   [hooks.workspaces :as hooks.workspaces]

   [doctor.ui.tauri :as tauri]))

(defn skip-bar-app? [client]
  (-> client :client/window-title #{"tauri/doctor-topbar"}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Icons
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn bar-icon
  [{:keys [color icon src
           fallback-text
           classes]}]
  [:div
   {:class (concat [color] classes)}
   (cond
     src   [:img {:class ["w-10"] :src src}]
     icon  [:div {:class ["text-3xl"]} icon]
     :else fallback-text)])

(defn client-icon-list
  [{:keys [workspace]} clients]
  (when (seq clients)
    [:div
     {:class ["grid" "grid-flow-col"]}
     (for [[i c] (->> clients (remove skip-bar-app?) (map-indexed vector))]
       (let [c-name
             (some->> c :client/window-title (take 15) (apply str))
             {:client/keys [focused]} c
             {:keys [color] :as icon-def}
             (icons/client->icon c workspace)]
         ^{:key i}
         [:div
          {:class ["w-8"]}
          [bar-icon (-> icon-def
                        (assoc
                          :fallback-text c-name
                          :color color
                          :classes ["border-opacity-0"
                                    (cond
                                      focused "text-city-orange-400"
                                      color   color
                                      :else   "text-city-blue-400")]))]]))]))


(defn workspace-cell
  [topbar-state
   {:as wsp :workspace/keys [index clients focused title]}
   {:keys [is-last]}]
  (let [urgent      false
        clients     (->> clients (remove skip-bar-app?))
        show-name   (or (not (seq title)) urgent focused (zero? (count clients)))
        show-number (or is-last (not (seq title)))]
    [:div
     {:class ["grid" "grid-flow-col-dense" "place-items-center"
              "h-full"
              "bg-yo-blue-800"
              "border" "border-city-blue-600"
              "rounded" "border-opacity-50"
              "text-white"
              (cond focused "bg-opacity-60" :else "bg-opacity-30")]}
     [client-icon-list (assoc topbar-state :workspace wsp) clients]

     ;; number/index
     (when show-number
       [:div {:class ["transition-all"
                      (cond urgent  "text-city-red-400"
                            focused "text-city-orange-400"
                            :else   "text-yo-blue-300")
                      "font-nes" "text-lg"]}
        [:span (str "[" index "]")]])

     ;; name
     (when show-name
       [:div {:class ["transition-all"
                      (cond urgent  "text-city-red-400"
                            focused "text-city-orange-400"
                            :else   "text-yo-blue-300")
                      "font-nes" "text-lg"]}
        (:workspace/title wsp "no title")])]))

(defn workspace-list [topbar-state wspcs]
  [:div
   {:class ["grid" "grid-flow-col" "h-full"]}
   (for [[i it] (->> wspcs (map-indexed vector))]
     ^{:key i}
     [workspace-cell topbar-state it {:is-last (= i (- (count wspcs) 1))}])])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Clock/host/metadata
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn sep [] [:span.px-2.font-mono "|"])

(defn clock-host-metadata [{:keys [time]} metadata]
  [:div
   {:class ["grid" "grid-flow-col"]}

   [:div
    {:class ["font-nes"]}
    (:hostname metadata)]
   [sep]

   [:div
    (if (:microphone/muted metadata) fa/microphone-slash-solid fa/microphone-solid)]
   (when (:battery/status metadata)
     [sep])

   (when (:battery/status metadata)
     [:div
      [:span
       (:battery/remaining-time metadata)]
      [:span
       {:class ["pl-2"]}
       (:battery/remaining-charge metadata)]])

   [sep]

   [:div
    {:class ["flex" "flex-row"]}
    (when-let [pcts
               (->>
                 metadata
                 (filter (fn [[_ v]] (when (and v (string? v)) (string/includes? v "%")))))]
      (for [[k v] pcts]
        ^{:key k}
        [:div
         {:class ["w-10"]}
         [charts/pie-chart
          {:label (str k)
           :value v
           :color (case k
                    :spotify/volume "rgb(255, 205, 86)"
                    :audio/volume   "rgb(54, 162, 235)",
                    "rgb(255, 99, 132)")}]]))]

   [sep]
   [:div.font-mono
    (some->> time
             #_{:clj-kondo/ignore [:unresolved-var]}
             (t/format (t/formatter "MM/dd HH:mm")))][sep]])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Current task
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn current-task [metadata]
  (when-let [todo (:todos/latest metadata)]
    (let [{:todo/keys [name]} todo]
      [:div
       {:class ["grid" "grid-flow-col"]}
       [:div.font-mono.pr-3 name]

       [components.actions/action-list (hooks.todos/->actions todo)]])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Topbar widget and state
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn use-topbar-state []
  (let [hovered-client         (uix/state nil)
        hovered-workspace      (uix/state nil)
        last-hovered-client    (uix/state nil)
        last-hovered-workspace (uix/state nil)
        topbar-above           (uix/state true)
        toggle-above-below     (fn []
                                 (-> (hooks.topbar/toggle-topbar-above (not @topbar-above))
                                     (.then (fn [v] (reset! topbar-above v)))))
        time                   (uix/state
                                 #_{:clj-kondo/ignore [:invalid-arity]}
                                 (t/zoned-date-time))
        interval               (atom nil)]
    (uix/with-effect [@interval]
      (reset! interval (js/setInterval #(reset! time
                                                #_{:clj-kondo/ignore [:invalid-arity]}
                                                (t/zoned-date-time)) 1000))
      (fn [] (js/clearInterval @interval)))

    {:hovered-client         @hovered-client
     :hovered-workspace      @hovered-workspace
     :last-hovered-workspace @last-hovered-workspace
     :last-hovered-client    @last-hovered-client
     :on-hover-workspace     (fn [w]
                               (reset! last-hovered-workspace w)
                               (reset! hovered-workspace w)
                               ;; (pull-above)
                               )
     :on-unhover-workspace   (fn [_] (reset! hovered-workspace nil))
     :on-hover-client        (fn [c]
                               (reset! last-hovered-client c)
                               (reset! hovered-client c)
                               ;; (pull-above)
                               )
     :on-unhover-client      (fn [_] (reset! hovered-client nil))
     :topbar-above           @topbar-above
     :toggle-above-below     toggle-above-below
     :time                   @time}))

(defn widget [_opts]
  (let [metadata                                      (hooks.topbar/use-topbar-metadata)
        {:keys [topbar/background-mode] :as metadata} @metadata
        {:keys [active-workspaces]}                   (hooks.workspaces/use-workspaces)
        topbar-state                                  (use-topbar-state)
        {:keys [tauri? open?] :as popup}              (tauri/use-popup)]
    [:div
     {:class ["h-screen" "overflow-hidden" "text-city-pink-200"
              (when (#{:bg/dark} background-mode) "bg-gray-700")
              (when (#{:bg/dark} background-mode) "bg-opacity-50")]}
     [:div
      {:class ["grid" "grid-flow-col-dense"
               "auto-cols-fr"
               "h-full"]}

      ;; workspaces
      [workspace-list topbar-state active-workspaces]

      [:div {:class ["grid" "grid-flow-col" "text-city-pink-100"]}
       ;; popup toggle
       (when (and tauri? @open?)
         [:button {:on-click (fn [_] ((:hide popup)))} "Hide Popup"])
       (when (and tauri? (not @open?))
         [:button {:on-click (fn [_] ((:show popup)))} "Show Popup"])

       ;; bg toggle
       [:button {:on-click (fn [_]
                             (hooks.topbar/set-background-mode
                               (if (#{:bg/dark} background-mode)
                                 :bg/light :bg/dark)))}
        "BG Toggle"]
       [:button {:on-click (fn [_] (js/location.reload))} "Reload"]]

      ;; clock/host/metadata
      [clock-host-metadata topbar-state metadata]
      ;; current task
      [current-task metadata]]]))
