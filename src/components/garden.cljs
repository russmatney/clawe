(ns components.garden
  (:require
   [clojure.string :as string]
   ;; [hiccup-icons.fa :as fa]
   [uix.core :as uix :refer [defui $]]

   [components.debug :as components.debug]
   [components.floating :as floating]
   [components.actions :as components.actions]
   [components.colors :as colors]
   [doctor.ui.handlers :as handlers]
   [doctor.ui.db :as ui.db]))

(defui status-icon [todo]
  (case (:org/status todo)
    ;; :status/done        fa/check-circle
    ;; :status/not-started fa/sticky-note
    ;; :status/in-progress fa/pencil-alt-solid
    ;; :status/cancelled   fa/ban-solid
    ;; :status/skipped     fa/eject-solid
    (when (:org.prop/archive-time todo)
      ($ :div.text-sm.font-mono "Archived"))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; urls
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn all-nested-urls [item]
  (->>
    (tree-seq (comp seq :org/items) :org/items item)
    (mapcat :org/urls)
    (into #{})))

(defui urls-list [{:keys [urls]}]
  (when (seq urls)
    ($ :div
       {:class ["grid"]}
       (for [u urls]
         ($ :a {:href  u
                :class ["inline-block"
                        "text-city-pink-200"
                        "hover:text-city-pink-400"
                        "p-2"]
                :key   u}
            (string/replace u #"https?://w?w?w?\.?" ""))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; tags
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn all-nested-tags [item]
  (->>
    (tree-seq (comp seq :org/items) :org/items item)
    (mapcat :org/tags)
    (into #{})))

(defui tags-list [tags]
  (when (seq tags)
    ($ :div
       {:class ["flex flex-row flex-wrap" "justify-center"]}
       (for [[i t] (->> tags (map-indexed vector))]
         ($ :span {:key t
                   :class
                   (concat ["font-mono"]
                           (colors/color-wheel-classes {:type :line :i i}))}
            (str ":" t))))))

(defui tags-comp [item]
  ($ tags-list (-> item :org/tags)))

(defui all-nested-tags-comp [item]
  (let [all-tags (all-nested-tags item)]
    ($ tags-list all-tags)))

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
  (let [_note (uix/use-state nil)]
    ;; (with-rpc []
    ;;   (when item
    ;;     (handlers/full-garden-item (dissoc item :actions/inferred)))
    ;;   #(reset! note %))
    {:note item}))

(declare org-file)
(defui full-note [item]
  (let [{:keys [note]} (use-full-garden-note item)]
    ($ :div
       {:class ["bg-city-blue-900" "p-4"]}
       ($ org-file {:item note}))))

(defui link-text-with-popover
  [{:keys [link-id link-text text-classes]}]
  ($ floating/popover
     {:hover        true :click true
      :anchor-comp  ($ :div
                       ($ :span
                          {:class
                           (concat ["inline-flex"
                                    "text-city-pink-400"
                                    "cursor-pointer"]
                                   text-classes)}
                          link-text))
      :popover-comp ($ full-note {:org/id link-id})}))

(def id-regex
  "The second group is anything except a closing \\]."
  #"\[\[id:([A-Za-z0-9-]+)\]\[([^\]]+)\]\]")

(defui text->comps
  [{:keys [text] :as opts}]
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
                                          [link-text-with-popover
                                           (merge {:link-id link-id :link-text link-text
                                                   :key     (str match)}
                                                  opts)])))))))))) ;; component here
             ) [text])
         (remove empty?)
         (map-indexed (fn [i comp-or-str]
                        (if (string? comp-or-str)
                          ;; wrap strings in :spans (better spacing control)
                          ($ :span
                             {:class ["inline-flex"] :key i}
                             comp-or-str)
                          ($ comp-or-str {:key i}))))
         (into []))))

(comment
  (text->comps
    "[[id:3a89063f-ef16-4156-9858-fc941b448057][sudo]] and proper [[id:3a89063f-ef16-4156-9858-fc941b448057][vim]] config?")
  (re-pattern "([hi])"))

(defui text-with-links
  [{:keys [text] :as opts}]
  (when (seq text)
    ($ :span
       {:class
        (concat
          ["inline-flex" "space-x-1"]
          (:text-classes opts))}
       (for [[i comp] (->> (assoc opts :text text) text->comps (map-indexed vector))]
         ($ comp {:key i})))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; body
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defui org-body-text [{:keys [item] :as opts}]
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
      ($ :div
         {:class ["flex" "flex-col"]}
         (for [[i line] (->> item :org/body (map-indexed vector))]
           (let [{:keys [text]} line]
             (cond
               (= "" text)
               ($ :span {:class [(when-not (zero? i) "py-1")]
                         :key   i} " ")

               :else ($ text-with-links
                        (assoc opts :key i :text text)))))))))

(defui org-body
  "Renders an org body.

  Recursively renders the item s nested content, if items are found
  as :org/items on the passed org node."
  [{:keys [show-raw item] :as opts}]
  (let [{:org/keys [body body-string items]} item]
    (when (or (seq body-string) (seq items))
      ($ :div {:class ["font-mono" "max-w-[900px]"]}
         ;; checking for a body-string, which accounts for 'empty' body content blocks
         (when (seq body-string)
           ($ :div
              {:class ["text-city-blue-400" "bg-yo-blue-500" "p-2"]}

              (when (seq body)
                ($ :div
                   {:class ["flex" "flex-col"]}

                   (when show-raw
                     ($ components.debug/raw-metadata
                        {:label "Raw body" :no-sort true} body))
                   ($ org-body-text (assoc opts :item item))))

              (when (and (not (seq body)) (seq body-string))
                ($ :pre
                   ($ text-with-links
                      (assoc opts :text-classes ["flex" "flex-col"])
                      body-string)))))

         (when show-raw
           ($ components.debug/raw-metadata
              {:label "Raw org item"}
              (dissoc item :org/items)))

         (when (seq items)
           ($ :div
              (for [[i item] (map-indexed vector items)]
                ($ :div
                   {:key i
                    :class
                    (concat
                      ["pt-2"]
                      (when (and (:org/level item) (= (:org/level item) 2))
                        ["border" "border-city-blue-800"])
                      (cond
                        (and (:org/level item) (= (:org/level item) 1)) ["pl-2"]
                        (and (:org/level item) (= (:org/level item) 2)) ["pl-4"]
                        (and (:org/level item) (= (:org/level item) 3)) ["pl-8"]
                        (and (:org/level item) (> (:org/level item) 3)) ["pl-16"]))}

                   ;; name
                   (when (:org/name item)
                     ($ :div
                        {:class
                         ["font-mono"
                          "flex" "flex-row" "items-center"
                          "space-x-2"]}

                        (when (:org/status item)
                          ($ :div
                             {:class [(when (#{:status/done :status/cancelled :status/skipped}
                                              (:org/status item))
                                        "text-slate-500")]}
                             ($ status-icon item)))

                        ($ :span
                           {:class ["pl-2" "text-lg"]}
                           ($ text-with-links
                              (assoc opts :text-classes [(when (#{:status/done :status/cancelled :status/skipped}
                                                                 (:org/status item)) "line-through")
                                                         (when (#{:status/done :status/cancelled :status/skipped}
                                                                 (:org/status item)) "text-slate-500")])
                              (:org/name-string item)))))

                   ;; recurse
                   ($ org-body (merge {:nested? true :item item} opts))))))))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; org link components
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defui source-file-link
  "A 'link' source-file that fires open-in-emacs on click."
  [{:org/keys [source-file short-path] :as item}]
  ($ components.actions/actions-popup
     {:actions (handlers/garden-file->actions item)
      :comp
      ($ :span
         {:class ["font-mono" "text-xl" "text-city-green-200" "p-2"
                  "hover:text-city-pink-400"
                  "cursor-pointer" "whitespace-nowrap"]}
         (or short-path source-file))}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defui garden-node
  [{:keys [on-select item]}]
  (let [{:org.prop/keys [title created-at]} item

        [hovering? set-hovering] (uix/use-state false)]
    ($ :div
       {:class          ["m-1" "p-4"
                         "border" "border-city-blue-600"
                         "bg-yo-blue-700"
                         "text-white"
                         (when hovering? "cursor-pointer")]
        :on-click       #(on-select)
        :on-mouse-enter #(set-hovering true)
        :on-mouse-leave #(set-hovering false)}

       title

       (when created-at
         ($ :div
            {:class ["font-mono"]}
            created-at))

       ($ source-file-link item))))

(defui selected-node
  [{:org.prop/keys [title]
    :org/keys      [name]
    :as            item}]

  ($ :div
     {:class ["flex" "flex-col" "p-2"]}
     ($ :span
        {:class ["font-nes" "text-xl" "text-city-green-200" "p-2"]}
        (or name title))

     ($ source-file-link item)

     ($ org-body item)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Org file
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defui org-file
  [{:keys [item] :as opts}]
  (let [{:org/keys [name] :as item} item
        [show-raw set-show-raw]     (uix/use-state false)
        all-urls                    (all-nested-urls item)]
    ($ :div
       {:class ["text-white"]}

       ($ :div
          {:class ["flex" "flex-row" "justify-between" "items-start" "mb-8"]}

          ($ source-file-link item)

          ($ :div {:class ["text-xl" "whitespace-nowrap"]}
             name)

          ($ :div
             ($ components.actions/actions-list
                {:actions
                 [(when (not show-raw)
                    {:action/on-click #(set-show-raw true)
                     :action/label    "show raw"})
                  (when show-raw
                    {:action/on-click #(set-show-raw false)
                     :action/label    "hide raw"})]})))

       ;; metadata (tags, urls, links, etc)
       ($ :div
          {:class ["flex flex-row" "justify-around" "items-center"]}
          ($ :div
             {:class ["w-1/3"]}
             ($ all-nested-tags-comp item))
          ($ urls-list {:urls all-urls}))

       ;; file content
       ($ org-body (assoc opts :show-raw show-raw :item item)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; priority-label

(defui priority-label
  [{:keys [item] :as opts}]
  (when (:org/priority item)
    ($ :span
       (merge opts
              {:class
               (concat
                 ["whitespace-nowrap" "font-nes"
                  "cursor-pointer"
                  "hover:line-through"]
                 (let [pri (:org/priority item)]
                   (cond
                     (not (:active opts)) ["text-city-blue-dark-400"]
                     (#{"A"} pri)         ["text-city-red-400"]
                     (#{"B"} pri)         ["text-city-pink-400"]
                     (#{"C"} pri)         ["text-city-green-400"])))})
       (str "#" (:org/priority item) " "))))
