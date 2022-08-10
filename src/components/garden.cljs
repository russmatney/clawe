(ns components.garden
  (:require
   [components.debug :as components.debug]
   [uix.core.alpha :as uix]
   [doctor.ui.handlers :as handlers]
   [clojure.string :as string]
   [components.floating :as floating]
   [plasma.uix :as plasma.uix :refer [with-rpc]]))


(defn use-full-garden-note [item]
  (let [note (plasma.uix/state nil)]
    (with-rpc []
      (when item
        (handlers/full-garden-item item))
      #(reset! note %))
    {:note @note}))

(declare org-file)
(defn full-note-popover [note]
  (let [{:keys [note]} (use-full-garden-note note)]
    [:div
     {:class ["bg-city-blue-900"]}
     [org-file note]]))

(defn link-text-with-popover [{:keys [link-id link-text]}]
  [floating/popover
   {:hover        true :click true
    :anchor-comp  [:div
                   [:span
                    {:class ["text-city-pink-400"
                             "cursor-pointer"]}
                    link-text]]
    :popover-comp [full-note-popover {:org/id link-id}]}])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; name/line
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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
                   ^{:key comp-or-str} [:span comp-or-str]
                   comp-or-str)))
          (into [])))))
(comment
  (text->comps
    "[[id:3a89063f-ef16-4156-9858-fc941b448057][sudo]] and proper [[id:3a89063f-ef16-4156-9858-fc941b448057][vim]] config?")
  (re-pattern "([hi])")
  )

(defn text-with-links
  ([text] (text-with-links nil text))
  ([opts text]
   (when (seq text)
     [:span
      {:class ["flex" "flex-row" "space-x-2"]}
      (for [[i comp] (->> (text->comps opts text) (map-indexed vector))]
        ^{:key i} comp)])))




;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; body
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn org-body
  "Renders an org body.

  Recursively renders the items nested content, if items are found
  as :org/items on the passed org node."
  ([item] (org-body nil item))
  ([{:keys [nested?] :as opts} {:org/keys [body body-string items status] :as item}]
   [:div
    {:class (concat ["font-mono"])}

    (when status
      [:div
       [:div
        {:class ["text-sm"]}
        "todo: "
        status]])

    (when (or (seq body) (seq body-string))
      [:div
       {:class ["text-city-blue-400"
                "flex" "flex-col" "p-2"
                "bg-yo-blue-500"]}
       [:div
        #_[:div
           {:class ["text-sm"]}
           word-count " words"]]

       (when (seq body)
         (for [[i line] (map-indexed vector body)]
           (let [{:keys [text]} line]
             (cond
               (= "" text)
               ^{:key i} [:span {:class ["py-1"]} " "]

               :else
               ^{:key i} [text-with-links opts text]))))

       (when (and (not (seq body)) body-string)
         [:pre body-string])])

    (when true
      [components.debug/raw-metadata
       {:label "Raw org item"}
       (dissoc item :org/items)])

    [:div
     (for [[i item] (map-indexed vector items)]
       ^{:key i}
       [:div
        {:class
         (concat
           [(when-not nested? "pb-4")]
           (when (and (:org/level item) (= (:org/level item) 1))
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
            ["text-lg" "font-mono"]}
           [text-with-links opts (:org/name item)]])

        ;; recurse
        [org-body (merge {:nested? true} opts) item]])]]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; org link components
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn source-file-link
  "A 'link' source-file that fires open-in-emacs on click."
  [{:org/keys [source-file short-path]
    :as       item}]

  [:span
   {:class    ["font-mono" "text-xl" "text-city-green-200" "p-2"
               "hover:text-city-pink-400"
               "cursor-pointer"]
    :on-click (fn [_] (handlers/open-in-journal item))}
   (or short-path source-file)])

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
  ([opts
    {:org/keys      [name tags urls]
     :org.prop/keys [filetags created-at title]
     :as            item}]
   [:div
    {:class ["text-white"]}

    [:div
     {:class ["text-lg"]}
     created-at]

    [source-file-link item]

    [:div
     {:class ["text-xl"]}
     (or title name)]

    [:div
     {:class ["text-lg"]}
     ;; TODO parse filetags into tags?
     filetags
     (when (seq tags) tags)
     (when (seq urls) urls)]

    [org-body opts item]]))
