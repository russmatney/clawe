(ns components.garden
  (:require
   [components.debug :as components.debug]
   [uix.core.alpha :as uix]
   [doctor.ui.handlers :as handlers]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; body
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn org-body
  "Renders an org body.

  Recursively renders the items nested content, if items are found
  as :org/items on the passed org node."
  ([item] (org-body nil item))
  ([{:keys [nested?]} {:org/keys [body body-string items status] :as item}]
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
               ^{:key i} [:span text]))))

       (when (and (not (seq body)) body-string)
         [:pre body-string])])

    #_(when @hovering?
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
            (concat
              ["text-lg" "font-mono"])}
           (:org/name item)])

        ;; recurse
        [org-body {:nested? true} item]])]]))


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
  ([_opts
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

    [org-body item]]))
