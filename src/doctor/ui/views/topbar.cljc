(ns doctor.ui.views.topbar
  (:require
   [plasma.core :refer [defhandler defstream]]
   #?@(:clj [[clawe.workspaces :as clawe.workspaces]
             [clawe.scratchpad :as scratchpad]
             [ralphie.awesome :as awm]
             [doctor.api.topbar :as d.topbar]
             [doctor.api.workspaces :as d.workspaces]]
       :cljs [[wing.core :as w]
              [clojure.string :as string]
              [uix.core.alpha :as uix]
              [plasma.uix :refer [with-rpc with-stream]]
              [hiccup-icons.fa :as fa]
              [hiccup-icons.mdi :as mdi]
              [tick.alpha.api :as t]
              [tick.format :as t.format]
              [doctor.ui.components.icons :as icons]
              [doctor.ui.components.charts :as charts]
              [doctor.ui.components.todos :as todos]
              [doctor.ui.views.popover :as popover]
              ["@tauri-apps/api" :as tauri]
              ["@tauri-apps/api/window" :as twindow :refer (appWindow getAll)]
              ])))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Workspaces
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defhandler get-workspaces [] (d.workspaces/active-workspaces))
(defstream workspaces-stream [] d.workspaces/*workspaces-stream*)

(defn skip-bar-app? [client]
  (and
    (-> client :awesome.client/focused not)
    (-> client :awesome.client/name #{"clover/doctor-dock"
                                      "clover/doctor-topbar"})))

#?(:cljs
   (defn use-workspaces []
     (let [workspaces  (plasma.uix/state [])
           handle-resp (fn [new-wsps]
                         (swap! workspaces
                                (fn [_wsps]
                                  (->> new-wsps
                                       (w/distinct-by :workspace/title)
                                       (sort-by :awesome.tag/index)))))]

       (with-rpc [] (get-workspaces) handle-resp)
       (with-stream [] (workspaces-stream) handle-resp)

       {:workspaces        @workspaces
        :active-clients    (->> @workspaces
                                (filter :awesome.tag/selected)
                                (mapcat :awesome.tag/clients)
                                (remove skip-bar-app?)
                                (w/distinct-by :awesome.client/window))
        :active-workspaces (->> @workspaces (filter :awesome.tag/selected))})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Topbar metadata
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defhandler get-topbar-metadata [] (d.topbar/build-topbar-metadata))
(defstream topbar-metadata-stream [] d.topbar/*topbar-metadata-stream*)

#?(:cljs
   (defn use-topbar-metadata []
     (let [topbar-metadata (plasma.uix/state [])
           handle-resp     #(reset! topbar-metadata %)]

       (with-rpc [] (get-topbar-metadata) handle-resp)
       (with-stream [] (topbar-metadata-stream) handle-resp)

       @topbar-metadata)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Workspace commands
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defhandler hide-workspace [item]
  (->
    ;; TODO support non scratchpad workspaces - could be a quick awm-fnl show-only
    item
    ;; :name
    ;; clawe.workspaces/for-name
    clawe.workspaces/merge-awm-tags
    scratchpad/toggle-scratchpad)
  (d.workspaces/update-workspaces))

(defhandler show-workspace [item]
  (->
    item
    ;; :name
    ;; clawe.workspaces/for-name
    clawe.workspaces/merge-awm-tags
    scratchpad/toggle-scratchpad)
  (d.workspaces/update-workspaces))

(defhandler toggle-topbar-above [above?]
  (if above?
    ;; awm-fnl does not yet support passed arguments
    (awm/awm-fnl
      '(->
         (client.get)
         (lume.filter (fn [c] (= c.name "clover/doctor-topbar")))
         (lume.first)
         ((fn [c]
            (tset c :above true)
            (tset c :below false)
            (tset c :ontop true)))))
    (awm/awm-fnl
      '(do
         ;; bury everything else
         (-> (client.get)
             (lume.filter (fn [c] c.ontop))
             (lume.map (fn [c] (tset c :ontop false))))

         ;; keep topbar ontop
         (->
           (client.get)
           (lume.filter (fn [c] (= c.name "clover/doctor-topbar")))
           (lume.first)
           ((fn [c]
              (tset c :above false)
              (tset c :below true)
              (tset c :ontop false)))))))
  above?)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Actions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

#?(:cljs
   (defn hours [n] (* 1000 60 60 n)))

#?(:cljs
   (defn ms-ago [ms-timestamp]
     (- (js/Date.now) ms-timestamp)))

#?(:cljs
   (defn ->actions
     ([wsp] (->actions nil wsp))
     ([{:keys [hovering?]} wsp]
      (let [{:keys [awesome.tag/selected
                    git/dirty?
                    git/needs-push?
                    git/needs-pull?
                    ;; git/last-fetch-timestamp
                    ]} wsp]
        (->>
          [(when needs-push? {:action/icon {:icon    mdi/github-face
                                            :color   "text-city-red-400"
                                            :tooltip "Needs Push"}})
           (when needs-pull? {:action/icon {:icon    mdi/github-face
                                            :color   "text-city-blue-500"
                                            :tooltip "Needs Pull"}})
           (when dirty? {:action/icon {:icon    mdi/github-face
                                       :color   "text-city-green-500"
                                       :tooltip "Dirty"}})
           ;; (when (and last-fetch-timestamp (> (ms-ago (* last-fetch-timestamp 1000)) (hours 3)))
           ;;   {:action/icon {:icon    mdi/github-face
           ;;                  :color   "text-city-yellow-500"
           ;;                  :tooltip "Last Fetch over 3 hours ago"}})
           (when (and selected hovering?)
             {:action/label    "hide"
              :action/on-click #(hide-workspace wsp)
              :action/icon     {:icon fa/eye-slash}})
           (when (and (not selected) hovering?)
             {:action/label    "show"
              :action/on-click #(show-workspace wsp)
              :action/icon     {:icon fa/eye}})]
          (remove nil?))))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Icons
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

#?(:cljs
   (defn bar-icon
     [{:keys [color icon src
              on-mouse-over
              on-mouse-out
              fallback-text
              classes
              border?]}]
     [:div
      {:on-mouse-over on-mouse-over
       :on-mouse-out  on-mouse-out
       :class         ["flex" "flex-row" "items-center"]}
      [:div
       {:class (concat (when border? ["border"]) [color] classes)}
       (cond
         src   [:img {:class ["w-10"] :src src}]
         icon  [:div {:class ["text-3xl"]} icon]
         :else fallback-text)]]))

#?(:cljs
   (defn client-icon-list
     [{:keys [on-hover-client on-unhover-client workspace
              ->show-name?
              ]} clients]
     (when (seq clients)
       [:div
        {:class ["flex" "flex-row" "flex-wrap"]}
        (for [c (->> clients (remove skip-bar-app?))]
          (let [c-name                                         (->> c :awesome.client/name (take 15) (apply str))
                {:awesome.client/keys [window urgent focused]} c
                {:keys [color] :as icon-def}                   (icons/client->icon c workspace)]
            ^{:key (or window c-name)}
            [:div
             {:class         ["flex" "flex-row" "items-center" "justify-center"]
              :on-mouse-over #(on-hover-client c)
              :on-mouse-out  #(on-unhover-client c)}
             (when (and ->show-name? (->show-name? c))
               [:span
                {:class ["px-2"]}
                c-name])

             [bar-icon (-> icon-def
                           (assoc
                             :fallback-text c-name
                             :color color
                             :classes ["border-opacity-0"
                                       (cond
                                         focused "text-city-orange-400"
                                         urgent  "text-city-red-400"
                                         color   color
                                         :else   "text-city-blue-400")]
                             :border? true))]]))])))

(def cell-classes
  ["flex" "flex-row" "justify-center"
   "max-h-16" "px-2"
   "border" "border-city-blue-600" "rounded" "border-opacity-50"
   "bg-yo-blue-800"
   "bg-opacity-10"
   "text-white"])

#?(:cljs
   (defn clients-cell
     [topbar-state clients]
     (let [hovering? (uix/state false)]
       [:div
        {:class          cell-classes
         :on-mouse-enter #(reset! hovering? true)
         :on-mouse-leave #(reset! hovering? false)}
        [:div {:class ["flex" "flex-row" "items-center" "justify-center"]}
         ;; icons
         [client-icon-list (assoc topbar-state
                                  :->show-name? (fn [{:keys [awesome.client/focused]}] focused))
          clients]]])))

#?(:cljs
   (defn workspace-cell
     [{:as   topbar-state
       :keys [hovered-workspace on-hover-workspace on-unhover-workspace]}
      {:as               wsp
       :workspace/keys   [title scratchpad]
       :awesome.tag/keys [index clients selected urgent]}]
     (let [hovering? (= hovered-workspace wsp)]
       [:div
        {:class          (conj cell-classes (cond selected "bg-opacity-60" :else "bg-opacity-10"))
         :on-mouse-enter #(on-hover-workspace wsp)
         :on-mouse-leave #(on-unhover-workspace wsp)}
        (let [show-name (or hovering? (not scratchpad) urgent selected (#{0} (count clients)))]
          [:div {:class ["flex" "flex-row" "items-center" "justify-center"]}
           ;; name/number
           [:div {:class [(when show-name "px-2")
                          (when-not show-name "w-0")
                          "transition-all"
                          (cond urgent   "text-city-red-400"
                                selected "text-city-orange-400"
                                :else    "text-yo-blue-300")]}
            [:div {:class ["font-nes" "text-lg"]}
             ;; number/index
             (let [show (and show-name (or hovering? (#{0} (count clients))))]
               [:span {:class [(when show "pr-2")]}
                (when show
                  (str "(" index ")"))])
             ;; name/title
             (when show-name title)]]

           ;; clients
           [client-icon-list (assoc topbar-state :workspace wsp) clients]

           ;; actions
           [:div
            {:class ["flex" "flex-wrap" "flex-row" "text-yo-blue-300"]}
            (for [[i ax] (map-indexed vector (->actions {:hovering? hovering?} wsp))]
              ^{:key i}
              [:div {:class    ["cursor-pointer" "hover:text-yo-blue-300"]
                     :on-click (:action/on-click ax)}
               (if (seq (:action/icon ax))
                 [bar-icon (:action/icon ax)]
                 (:action/label ax))])]])])))

#?(:cljs
   (defn workspace-list [topbar-state wspcs]
     [:div
      {:class ["flex" "flex-row" "justify-center"]}
      (for [[i it] (->> wspcs (map-indexed vector))]
        ^{:key i}
        [workspace-cell topbar-state it])]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Clock/host/metadata
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

#?(:cljs (defn sep [] [:span.px-2.font-mono "|"]))

#?(:cljs
   (defn clock-host-metadata [{:keys [time topbar-above toggle-above-below]} metadata]
     [:div
      {:class ["flex" "flex-row" "justify-center" "items-center"]}

      [:div.font-mono
       (some->> time (t.format/format (t.format/formatter "MM/dd HH:mm")))]

      [sep]
      [:div
       {:class ["font-nes"]}
       (:hostname metadata)]

      [sep]
      [:div
       (if (:microphone/muted metadata) fa/microphone-slash-solid fa/microphone-solid)]

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
      ;; current todos
      (let [ct (-> metadata :todos/in-progress count)]
        [:div.font-mono
         (if (zero? ct)
           "No in-progress todos"
           (str ct " in-progress todo" (when (> ct 1) "s")))])
      [sep]
      [:div.font-mono
       {:on-click toggle-above-below}
       (if topbar-above "above" "below")]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Current task
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

#?(:cljs
   (defn current-task [metadata]
     (when-let [todo (:todos/latest metadata)]
       (let [{:todo/keys [name]} todo]
         [:div
          {:class ["flex" "flex-row" "justify-center" "items-center"]}
          [:div.font-mono.pr-3 name]

          [todos/action-list todo]]))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Topbar widget and state
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

#?(:cljs
   (defn use-topbar-state []
     (let [hovered-client         (uix/state nil)
           hovered-workspace      (uix/state nil)
           last-hovered-client    (uix/state nil)
           last-hovered-workspace (uix/state nil)
           topbar-above           (uix/state true)
           pull-above             (fn []
                                    (-> (toggle-topbar-above true)
                                        (.then (fn [v] (reset! topbar-above v)))))
           push-below             (fn []
                                    (-> (toggle-topbar-above false)
                                        (.then (fn [v] (reset! topbar-above v)))))
           toggle-above-below     (fn []
                                    (-> (toggle-topbar-above (not @topbar-above))
                                        (.then (fn [v] (reset! topbar-above v)))))
           time                   (uix/state (t/zoned-date-time))
           interval               (atom nil)]
       (uix/with-effect [@interval]
         (reset! interval (js/setInterval #(reset! time (t/zoned-date-time)) 1000))
         (fn [] (js/clearInterval @interval)))

       {:hovered-client         @hovered-client
        :hovered-workspace      @hovered-workspace
        :last-hovered-workspace @last-hovered-workspace
        :last-hovered-client    @last-hovered-client
        :on-hover-workspace     (fn [w]
                                  (reset! last-hovered-workspace w)
                                  (reset! hovered-workspace w)
                                  (pull-above))
        :on-unhover-workspace   (fn [_] (reset! hovered-workspace nil))
        :on-hover-client        (fn [c]
                                  (reset! last-hovered-client c)
                                  (reset! hovered-client c)
                                  (pull-above))
        :on-unhover-client      (fn [_] (reset! hovered-client nil))
        :topbar-above           @topbar-above
        :pull-above             pull-above
        :push-below             push-below
        :toggle-above-below     toggle-above-below
        :time                   @time})))

#?(:cljs
   (defn widget []
     (let [metadata                                              (use-topbar-metadata)
           {:keys [workspaces active-clients active-workspaces]} (use-workspaces)
           topbar-state                                          (use-topbar-state)]
       (println
         (->> workspaces (remove :workspace/scratchpad)
              (filter :awesome.tag/selected)
              (map :name)))

       [:div
        {:class ["h-screen" "overflow-hidden" "text-city-pink-200"]}
        [:div
         {:class ["flex" "flex-row" "justify-between" "pr-3"]}

         ;; repo workspaces
         [workspace-list topbar-state (->> workspaces (remove :workspace/scratchpad))]
         ;; scratchpads
         [workspace-list topbar-state (->> workspaces (filter :workspace/scratchpad))]
         ;; active-clients
         (when (seq active-clients) [clients-cell topbar-state active-clients])
         ;; clock/host/metadata
         [clock-host-metadata topbar-state metadata]
         ;; current task
         [current-task metadata]]

        ;; below bar
        ;; (when (:topbar-above topbar-state)
        ;;   [popover/detail-window
        ;;    (assoc topbar-state :active-workspaces active-workspaces) metadata])
        ])))

#?(:cljs
   (comment
     (let [w tauri/window.appWindow]
       (js/Object.keys w)
       )

     appWindow
     (getAll)

     (println "hi")
     (-> appWindow
         (.hide)
         (.then println)
         (.catch println)
         (.finally println)
         )
     (-> appWindow
         (.innerSize)
         (.then println)
         (.catch println)
         (.finally println)
         )

     (-> tauri/window.appWindow
         js/Object.keys)

     (.. tauri/window.appWindow
         -label)

     (.. tauri/window.appWindow
         -listeners)

     (twindow/getAll)
     (twindow/getCurrent)

     (->> (tauri/window.getAll))


     (.. js/window
         -__TAURI__)))
