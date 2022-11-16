(ns doctor.ui.views.topbar
  (:require
   [clojure.string :as string]
   [hiccup-icons.fa :as fa]
   [tick.core :as t]
   [uix.core.alpha :as uix]

   [components.icons :as icons]
   [components.charts :as charts]
   [components.garden :as components.garden]
   [components.actions :as components.actions]
   [components.colors :as colors]

   [hooks.topbar :as hooks.topbar]
   [hooks.workspaces :as hooks.workspaces]

   [doctor.ui.db :as ui.db]
   [doctor.ui.handlers :as handlers]
   [doctor.ui.hooks.use-topbar :as use-topbar]

   ["@heroicons/react/20/solid" :as HIMini]
   [wing.core :as w]))

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
        show-number (or urgent focused (zero? (count clients)) is-last)]
    [:div
     {:class ["grid" "grid-flow-col" "place-items-center"
              "space-x-2" "px-1"
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
        [:span (str "[" index "]")]])]))

(defn workspace-list [topbar-state wspcs]
  [:div
   {:class ["flex" "flex-row" "h-full" "place-self-start"]}
   (for [[i it] (->> wspcs (map-indexed vector))]
     ^{:key i}
     [workspace-cell topbar-state it {:is-last (= i (- (count wspcs) 1))}])])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Clock/host/metadata
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn sep [] [:span
              {:class ["px-3 font-nes text-slate-600"]}
              "|"])

(defn toggle-background-mode [{:keys [topbar/background-mode] :as _metadata}]
  (fn [_]
    (hooks.topbar/set-background-mode
      (if (#{:bg/dark} background-mode)
        :bg/light :bg/dark))))

(defn clock-host-metadata [{:keys [time]} metadata]
  [:div
   {:class ["flex" "flex-row" "justify-end" "items-center"]}

   [:div
    [components.actions/actions-list
     {:actions [{:action/on-click (fn [_] (hooks.topbar/rerender-notebooks))
                 :action/label    "rerender"}

                ;; bg toggle
                {:action/on-click (toggle-background-mode metadata)
                 :action/label    "toggle"
                 :action/icon

                 (if (#{:bg/dark} (:topbar/background-mode metadata))
                   [:> HIMini/SunIcon {:class ["w-6" "h-6"]}]
                   [:> HIMini/MoonIcon {:class ["w-6" "h-6"]}])}

                ;; reload
                {:action/on-click (fn [_] (js/location.reload))
                 :action/label    "reload"
                 :action/icon     [:> HIMini/ArrowPathIcon {:class ["w-6" "h-6"]}]}

                {:action/on-click (fn [_]
                                    ;; TODO toggle mute
                                    )
                 :action/icon     (if (:microphone/muted metadata)
                                    fa/microphone-slash-solid fa/microphone-solid)}]}]]

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
;; Current task
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn current-task [{:keys [conn]}]
  (let [todos         (ui.db/queued-todos conn)
        current-items (ui.db/garden-current-items conn)]
    (when (seq todos)
      (let [queued  (->> todos
                         (concat current-items)
                         (w/dedupe-by :db/id)
                         (sort-by :todo/queued-at >) (into []))
            n       (uix/state 0)
            current (get queued @n)
            ct      (count queued)]
        [:div
         {:class ["flex" "flex-wrap" "place-self-center"
                  "h-full"
                  "items-center"
                  "space-x-4"
                  ;; "w-[1100px]"
                  ]}

         [:span
          {:class ["pl-3" "font-mono"]}
          (str (inc @n) "/" ct)]

         [:div
          {:class ["font-mono pr-3" "whitespace-nowrap"]}
          [components.garden/text-with-links (:org/name current)]]

         [components.actions/actions-list
          {:actions
           (concat
             (when (> ct 0)
               [{:action/label    "next"
                 :action/icon     fa/chevron-up-solid
                 :action/disabled (>= @n (dec ct))
                 :action/on-click (fn [_] (swap! n inc))
                 :action/priority 5}
                {:action/label    "prev"
                 :action/icon     fa/chevron-down-solid
                 :action/disabled (zero? @n)
                 :action/on-click (fn [_] (swap! n dec))
                 :action/priority 5}])
             (handlers/->actions current))}]]))))

(defn widget [_opts]
  (let [;; TODO move metadata/workspaces into use-topbar
        metadata                                      (hooks.topbar/use-topbar-metadata)
        {:keys [topbar/background-mode] :as metadata} @metadata
        {:keys [active-workspaces]}                   (hooks.workspaces/use-workspaces)
        topbar-state                                  (use-topbar/use-topbar-state)]
    [:div
     {:class ["h-screen" "overflow-hidden" "text-city-pink-200"
              (when (#{:bg/dark} background-mode) "bg-gray-700")
              (when (#{:bg/dark} background-mode) "bg-opacity-50")]}
     [:div
      {:class ["flex" "flex-row" "h-full" "justify-between"]}

      ;; workspaces
      [workspace-list topbar-state active-workspaces]

      ;; current task
      (let [current-workspace
            (some->> active-workspaces
                     (filter :workspace/focused)
                     first)]
        (when current-workspace
          [:div
           {:class ["flex" "flex-row" "space-x-4"
                    "items-center"
                    "mx-8"]}
           [:span
            {:class ["font-nes" "text-city-blue-500"]}
            (:workspace/title current-workspace)]
           #_[current-task opts]
           [:span
            {:class ["text-city-blue-500" "font-mono"]}
            (-> current-workspace
                :workspace/directory
                (string/replace "/home/russ" "~")
                (string/replace "/Users/russ" "~"))]]))

      ;; clock/host/metadata
      [clock-host-metadata topbar-state metadata]]]))
