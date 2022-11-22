(ns components.garden
  (:require
   [clojure.string :as string]
   [uix.core.alpha :as uix]
   [hiccup-icons.fa :as fa]
   [plasma.uix :as plasma.uix :refer [with-rpc]]
   [doctor.ui.handlers :as handlers]
   [components.debug :as components.debug]
   [components.floating :as floating]
   [components.actions :as components.actions]
   [components.colors :as colors]
   [doctor.ui.db :as ui.db]))

(defn status-icon [todo]
  (case (:org/status todo)
    :status/done        fa/check-circle
    :status/not-started fa/sticky-note
    :status/in-progress fa/pencil-alt-solid
    :status/cancelled   fa/ban-solid
    :status/skipped     fa/eject-solid
    (when (:org.prop/archive-time todo)
      [:div.text-sm.font-mono "Archived"])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; urls
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn all-nested-urls [item]
  (->>
    (tree-seq (comp seq :org/items) :org/items item)
    (mapcat :org/urls)
    (into #{})))

(defn urls-list [urls]
  (when (seq urls)
    [:div
     {:class ["grid"]}
     (for [u urls]
       ^{:key u}
       [:a {:href  u
            :class ["inline-block"
                    "text-city-pink-200"
                    "hover:text-city-pink-400"
                    "p-2"]}
        (string/replace u #"https?://w?w?w?\.?" "")])]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; tags
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn all-nested-tags [item]
  (->>
    (tree-seq (comp seq :org/items) :org/items item)
    (mapcat :org/tags)
    (into #{})))

(defn tags-list [tags]
  (when (seq tags)
    [:div
     (for [[i t] (->> tags (map-indexed vector))]
       ^{:key t}
       [:span {:class
               (concat ["font-mono"]
                       (colors/color-wheel-classes {:type :line :i i}))}
        (str ":" t)])]))

(defn tags-comp [item]
  [tags-list (-> item :org/tags)])

(defn all-nested-tags-comp [item]
  (let [all-tags (all-nested-tags item)]
    [tags-list all-tags]))

(defn all-tags [{:keys [conn]} _item]
  #_(println "all-tags called")
  #_(println
      #_(ui.db/garden-tags conn))
  (->>
    (ui.db/garden-tags conn)
    (take 3)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; name/line
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn use-full-garden-note [item]
  (let [note (plasma.uix/state nil)]
    (with-rpc []
      (when item
        (handlers/full-garden-item item))
      #(reset! note %))
    {:note @note}))

(declare org-file)
(defn full-note [item]
  (let [{:keys [note]} (use-full-garden-note item)]
    [:div
     {:class ["bg-city-blue-900"]}
     [org-file note]]))

(defn link-text-with-popover
  [{:keys [link-id link-text text-classes]}]
  [floating/popover
   {:hover        true :click true
    :anchor-comp  [:div
                   [:span
                    {:class
                     (concat ["inline-flex"
                              "text-city-pink-400"
                              "cursor-pointer"]
                             text-classes)}
                    link-text]]
    :popover-comp [full-note {:org/id link-id}]}])

(def id-regex
  "The second group is anything except a closing \\]."
  #"\[\[id:([A-Za-z0-9-]+)\]\[([^\]]+)\]\]")

(defn text->comps
  ([text] (text->comps nil text))
  ([opts text]
   (let [matches  (re-seq id-regex text)
         to-regex (fn [s]
                    (re-pattern
                      (str "("
                           (-> s
                               (string/replace "[" "\\[")
                               (string/replace "]" "\\]"))
                           ")")))]
     (->> matches
          (reduce
            (fn [comps [match link-id link-text]]
              (->> comps
                   (mapcat
                     (fn [comp]
                       (if-not (string? comp)
                         [comp]
                         (let [splits (string/split comp (to-regex match))]
                           (if (#{1} (count splits))
                             [(first splits)] ;; return the unmatched, untouched string
                             (->> splits
                                  (map (fn [split]
                                         (if (not (= split match))
                                           split ;; return not-a-match, leave as string so further matches don't have to dig
                                           ^{:key (str match)}
                                           [link-text-with-popover
                                            (merge {:link-id link-id :link-text link-text}
                                                   opts)])))))))))) ;; component here
              ) [text])
          (remove empty?)
          (map (fn [comp-or-str]
                 (if (string? comp-or-str)
                   ;; wrap strings in :spans (better spacing control)
                   ^{:key comp-or-str} [:span
                                        {:class ["inline-flex"]}
                                        comp-or-str]
                   comp-or-str)))
          (into [])))))

(comment
  (text->comps
    "[[id:3a89063f-ef16-4156-9858-fc941b448057][sudo]] and proper [[id:3a89063f-ef16-4156-9858-fc941b448057][vim]] config?")
  (re-pattern "([hi])"))

(defn text-with-links
  ([text] (text-with-links nil text))
  ([opts text]
   (when (seq text)
     [:span
      {:class
       (concat
         ["inline-flex" "space-x-1"]
         (:text-classes opts))}
      (for [[i comp] (->> text (text->comps opts) (map-indexed vector))]
        ^{:key i} comp)])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; body
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn org-body-text [opts item]
  (when (-> item :org/body seq)
    (let [;; TODO what's this calc doing?
          _lines (->> item :org/body
                      (map-indexed vector)
                      (reduce
                        (fn [lines [_i {:keys [text]}]]
                          (cond
                            (#{""} text)
                            (concat lines [])))
                        []))]
      [:div
       {:class ["flex" "flex-col"]}
       (for [[i line] (->> item :org/body (map-indexed vector))]
         (let [{:keys [text]} line]
           (cond
             (= "" text)
             ^{:key i}
             [:span {:class [(when-not (zero? i) "py-1")]} " "]

             :else
             ^{:key i}
             [text-with-links opts text])))])))

(defn org-body
  "Renders an org body.

  Recursively renders the items nested content, if items are found
  as :org/items on the passed org node."
  ([item] (org-body nil item))
  ([{:keys [show-raw] :as opts}
    {:org/keys [body body-string items] :as item}]
   (when (or (seq body-string) (seq items))
     [:div {:class ["font-mono" "max-w-[900px]"]}

      ;; checking for a body-string, which accounts for 'empty' body content blocks
      (when (seq body-string)
        [:div
         {:class ["text-city-blue-400" "bg-yo-blue-500" "p-2"]}

         (when (seq body)
           [:div
            {:class ["flex" "flex-col"]}

            (when show-raw
              [components.debug/raw-metadata
               {:label "Raw body" :no-sort true} body])
            [org-body-text opts item]])

         (when (and (not (seq body)) (seq body-string))
           [:pre
            [text-with-links opts body-string]])])

      (when show-raw
        [components.debug/raw-metadata
         {:label "Raw org item"}
         (dissoc item :org/items)])

      (when (seq items)
        [:div
         (for [[i item] (map-indexed vector items)]
           ^{:key i}
           [:div
            {:class
             (concat
               (when (and (:org/level item) (= (:org/level item) 2))
                 ["border" "border-city-blue-800"])
               (cond
                 (and (:org/level item) (= (:org/level item) 1)) ["pl-2"]
                 (and (:org/level item) (= (:org/level item) 2)) ["pl-4"]
                 (and (:org/level item) (= (:org/level item) 3)) ["pl-8"]
                 (and (:org/level item) (> (:org/level item) 3)) ["pl-16"]))}

            ;; name
            (when (:org/name item)
              [:div
               {:class
                ["text-lg" "font-mono" "flex" "flex-row" "items-center" "space-x-2"]}

               (when (:org/status item)
                 [:div
                  {:class ["text-sm"
                           (when (#{:status/done :status/cancelled :status/skipped}
                                   (:org/status item)) "text-slate-500")]}
                  [status-icon item]])

               [text-with-links
                (assoc opts :text-classes [(when (#{:status/done :status/cancelled :status/skipped}
                                                   (:org/status item)) "line-through")
                                           (when (#{:status/done :status/cancelled :status/skipped}
                                                   (:org/status item)) "text-slate-500")])
                (:org/name item)]])

            ;; recurse
            [org-body (merge {:nested? true} opts) item]])])])))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; org link components
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn source-file-link
  "A 'link' source-file that fires open-in-emacs on click."
  [{:org/keys [source-file short-path] :as item}]
  [components.actions/actions-popup
   {:actions (handlers/garden-file->actions item)
    :comp
    [:span
     {:class ["font-mono" "text-xl" "text-city-green-200" "p-2"
              "hover:text-city-pink-400"
              "cursor-pointer"]}
     (or short-path source-file)]}])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn garden-node
  ([item] (garden-node nil item))
  ([{:keys [on-select]} item]
   (let [{:org.prop/keys [title created-at]}

         item
         hovering? (uix/state false)]
     [:div
      {:class          ["m-1" "p-4"
                        "border" "border-city-blue-600"
                        "bg-yo-blue-700"
                        "text-white"
                        (when @hovering? "cursor-pointer")]
       :on-click       #(on-select)
       :on-mouse-enter #(reset! hovering? true)
       :on-mouse-leave #(reset! hovering? false)}

      title

      (when created-at
        [:div
         {:class ["font-mono"]}
         created-at])

      [source-file-link item]])))

(defn selected-node
  [{:org.prop/keys [title]
    :org/keys      [name]
    :as            item}]

  [:div
   {:class ["flex" "flex-col" "p-2"]}
   [:span
    {:class ["font-nes" "text-xl" "text-city-green-200" "p-2"]}
    (or name title)]

   [source-file-link item]

   [org-body item]])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Org file
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn org-file
  ([item] (org-file nil item))
  ([opts {:org/keys [name] :as item}]
   (let [show-raw (uix/state false)

         all-urls (all-nested-urls item)]
     [:div
      {:class ["text-white"]}

      [:div
       {:class ["grid" "grid-flow-col-dense" "mb-8"]}
       [source-file-link item]

       [:div {:class ["text-xl"]} name]

       [components.actions/actions-list
        {:actions
         [(when (not @show-raw)
            {:action/on-click #(reset! show-raw true)
             :action/label    "show raw"})
          (when @show-raw
            {:action/on-click #(reset! show-raw false)
             :action/label    "hide raw"})]}]]

      [:div
       {:class ["grid" "grid-flow-col"]}

       ;; file content
       [org-body
        (-> opts (assoc :show-raw @show-raw))
        item]

       ;; metadata (tags, urls, links, etc)
       [:div
        {:class ["grid"]}
        [:div
         [all-nested-tags-comp item]]
        [:div
         [urls-list all-urls]]]]])))
