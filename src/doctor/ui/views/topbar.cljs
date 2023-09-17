(ns doctor.ui.views.topbar
  (:require
   [clojure.string :as string]
   [hiccup-icons.fa :as fa]
   [tick.core :as t]
   [uix.core.alpha :as uix]

   [components.icons :as icons]
   [components.charts :as charts]
   [components.actions :as components.actions]
   [components.colors :as colors]
   [components.format :as format]

   [hooks.topbar :as hooks.topbar]
   [hooks.workspaces :as hooks.workspaces]

   [doctor.ui.handlers :as handlers]
   [doctor.ui.hooks.use-topbar :as use-topbar]

   ["@heroicons/react/20/solid" :as HIMini]

   [doctor.ui.views.pomodoro :as pomodoro]
   [doctor.ui.views.git-status :as git-status]
   [doctor.ui.views.focus :as focus]))

(defn skip-bar-app? [client]
  (-> client :client/window-title #{"tauri-doctor-topbar"}))

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
   {:as wsp :workspace/keys [index clients focused]}
   {:keys [is-last]}]
  (let [urgent      false
        clients     (->> clients (remove skip-bar-app?))
        show-number (or urgent focused (zero? (count clients)) is-last)
        hovering?   (uix/state nil)]
    [:div
     {:class ["grid" "grid-flow-col" "place-items-center"
              "space-x-2" "px-1"
              "h-full"
              "bg-yo-blue-800"
              "border" "border-city-blue-600"
              "rounded" "border-opacity-50"
              "text-white"
              (cond focused "bg-opacity-60" :else "bg-opacity-30")]

      :on-mouse-enter #(reset! hovering? true)
      :on-mouse-leave #(reset! hovering? false)}

     [client-icon-list (assoc topbar-state :workspace wsp) clients]

     ;; number/index
     (when show-number
       [:div {:class ["transition-all"
                      (cond urgent  "text-city-red-400"
                            focused "text-city-orange-400"
                            :else   "text-yo-blue-300")
                      "font-nes" "text-lg"]}
        [:span (str "[" index "]")]])

     (when @hovering?
       [:div
        {:class ["transition-all"
                 "overflow-hidden"
                 #_(if @hovering? "w-32" "w-0")]}
        [components.actions/actions-list {:actions (handlers/->actions wsp) :n 2}]])]))

(defn workspace-list [topbar-state wspcs]
  [:div
   {:class ["flex" "flex-row" "h-full" "place-self-start"]}
   (for [[i it] (->> wspcs (map-indexed vector))]
     ^{:key i}
     [workspace-cell topbar-state it {:is-last (= i (- (count wspcs) 1))}])])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Clock/host/metadata
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn sep []
  [:span
   {:class ["font-nes text-3xl text-slate-600"]}
   "|"])

(defn toggle-background-mode [{:keys [topbar/background-mode] :as _metadata}]
  (fn [_]
    (hooks.topbar/set-background-mode
      (if (#{:bg/dark} background-mode)
        :bg/light :bg/dark))))

(defn topbar-actions-list [{:keys [conn]} metadata]
  [components.actions/actions-list
   {:actions
    (concat
      (handlers/pomodoro-actions conn)
      [{:action/on-click (fn [_]
                           ;; TODO toggle mute
                           )
        :action/icon     (if (:microphone/muted metadata)
                           fa/microphone-slash-solid fa/microphone-solid)}

       {:action/on-click (toggle-background-mode metadata)
        :action/label    "toggle"
        :action/icon

        (if (#{:bg/dark} (:topbar/background-mode metadata))
          [:> HIMini/SunIcon {:class ["w-6" "h-6"]}]
          [:> HIMini/MoonIcon {:class ["w-6" "h-6"]}])}
       ;; reload

       {:action/on-click (fn [_] (js/location.reload))
        :action/label    "reload"
        :action/icon     [:> HIMini/ArrowPathIcon {:class ["w-6" "h-6"]}]}])}])

(defn clock-host-metadata [{:keys [time] :as opts} metadata]
  [:div
   {:class ["flex" "flex-row" "justify-end" "items-center"]}

   [:div
    {:class ["overflow-scroll"]}
    [pomodoro/bar opts]]

   [topbar-actions-list opts metadata]

   [sep]

   [:div
    {:class
     (concat ["font-nes"]
             (colors/color-wheel-classes {:type :line :i 2}))}
    (:hostname metadata)]

   (when (:battery/status metadata)
     [sep])
   (when (:battery/status metadata)
     [:div
      {:class (colors/color-wheel-classes {:type :line :i 4})}
      [:span
       (:battery/remaining-time metadata)]
      [:span
       {:class ["pl-2"]}
       (:battery/remaining-charge metadata)]])

   (when-let [pcts
              (->>
                metadata
                (filter (fn [[_ v]] (when (and v (string? v)) (string/includes? v "%")))))]
     [sep]
     [:div
      {:class (concat ["flex" "flex-row"]
                      (colors/color-wheel-classes {:type :line :i 5}))}
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
                    "rgb(255, 99, 132)")}]])])

   [sep]
   [:div.font-nes {:class ["flex" "flex-row" "space-x-2"]}
    [:span
     {:class (colors/color-wheel-classes {:type :line :i 6})}
     (some->> time (t/format (t/formatter "M/d")))]
    [:span
     {:class (colors/color-wheel-classes {:type :line :i 7})}
     (some->> time (t/format (t/formatter "h:mma")))]]])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; topbar
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn widget [opts]
  (let [metadata                                      (hooks.topbar/use-topbar-metadata)
        {:keys [topbar/background-mode] :as metadata} @metadata
        {:keys [active-workspaces]}                   (hooks.workspaces/use-workspaces)
        topbar-state                                  (use-topbar/use-topbar-state)]
    [:div
     {:class ["h-screen" "overflow-hidden" "text-city-pink-200"
              (when (#{:bg/dark} background-mode) "bg-gray-700")
              (when (#{:bg/dark} background-mode) "bg-opacity-50")]}
     [:div
      {:class ["flex" "flex-row" "h-full"
               "justify-between" "items-center"]}

      [workspace-list (merge opts topbar-state) active-workspaces]

      ;; workspace title
      (let [current-workspace
            (some->> active-workspaces
                     (filter :workspace/focused) first)]
        (when current-workspace
          [:span
           {:class ["font-nes" "text-city-blue-500" "whitespace-nowrap"
                    "text-4xl" "pl-3"]}
           (components.format/s-shortener
             {:length 6 :break "|"}
             (string/replace (:workspace/title current-workspace) "-" ""))]))

      [:div
       {:class ["overflow-scroll"]}
       [focus/current-task opts]]

      [:div
       {:class ["overflow-scroll"]}
       [git-status/bar opts]]

      [clock-host-metadata (merge opts topbar-state) metadata]]]))
