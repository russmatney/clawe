(ns blog.item
  (:require
   [taoensso.timbre :as log]
   [clojure.set :as set]
   [clojure.string :as string]
   [org-crud.core :as org-crud]
   [blog.db :as blog.db]
   [tick.core :as t]
   [dates.tick :as dates]))

(defn item-has-any-tags
  "Returns truthy if the item has at least one matching tag."
  [item]
  (-> item :org/tags seq))

(defn item-has-tags
  "Returns truthy if the item has at least one matching tag."
  [item tags]
  (-> item :org/tags (set/intersection tags) seq))

;; (defn item-has-parent [item parent-names]
;;   (when-let [p-name (:org/parent-name item)]
;;     (->> parent-names
;;          (filter (fn [match] (string/includes? p-name match)))
;;          seq)))

;; (defn items-with-tags [items tags]
;;   (->> items (filter #(item-has-tags % tags))))

;; (defn items-with-parent [items parent-names]
;;   (->> items (filter #(item-has-parent % parent-names))))

(defn item->all-tags [item]
  ;; TODO daily notes should filter untagged items (subitems)
  (->> item org-crud/nested-item->flattened-items
       (mapcat :org/tags) (into #{})))

;; (defn item->all-links [item]
;;   (->> item org-crud/nested-item->flattened-items
;;        (mapcat :org/links-to) (into #{})))

#_(defn item->tag-line
    ([item] (item->tag-line nil item))
    ([opts item]
     (let [tags
           (if (:include-child-tags opts)
             (->> item org-crud/nested-item->flattened-items
                  (mapcat :org/tags) (into #{}))
             (:org/tags item))]
       (when (seq tags)
         (->> tags (map #(str ":" %)) (string/join "\t"))))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; hiccup helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn is-url? [text]
  (boolean (re-seq #"^https?://" text)))

(def org-link-re
  "[[some-link-url][some link text]]"
  #"\[\[([^\]]*)\]\[([^\]]*)\]\]")

(declare interpose-inline-code)

(defn ->hiccup-link [{:keys [text link]}]
  (when link
    (let [id         (some->> (re-seq #"^id:([A-Za-z0-9-]*)" link) first second)
          link-uri   (when id (blog.db/*id->link-uri* id))
          text-elems (->> text interpose-inline-code (map (fn [el] [:span el])))]
      (cond
        (and id link-uri)
        ;; TODO maybe flag intra-blog-note-links as a different color
        (->> text-elems (into [:a {:href link-uri}]))

        (and id (not link-uri))
        ;; TODO tooltip for 'maybe-future-link'
        (->> text-elems (into [:span]))

        :else
        (cond
          (string/starts-with? link "http")
          (->> text-elems (into [:a {:href link}]))

          :else
          (->> text-elems (into [:span])))))))

(defn parse-hiccup-link
  "Returns hiccup representing the next link in the passed string"
  [s]
  (when s
    (let [res  (re-seq org-link-re s)
          link (some-> res first second)
          text (some-> res first last)]
      (->hiccup-link {:text text :link link}))))

(def inline-code-re #"~([^~]*)~")

(defn inline-code
  "Returns a [:code] block wrapping the first match for the inline code regexp"
  [s]
  (when s
    (let [res  (re-seq inline-code-re s)
          text (some-> res first second)]
      (when text
        [:code text]))))

(defn interpose-pattern [s pattern replace-first]
  (->> (loop [s s out []]
         (let [next-replacement (replace-first s)
               parts            (string/split s pattern 2)]
           (if (< (count parts) 2)
             (concat out (if next-replacement (conj parts next-replacement) parts))
             (let [[new-s rest] parts
                   out          (concat out [new-s next-replacement])]
               (recur rest out)))))
       (remove #{""})))

(defn interpose-inline-code [s]
  (interpose-pattern s inline-code-re inline-code))

(comment
  (interpose-inline-code "")

  (re-seq inline-code-re "* maybe ~goals~ :goals:")
  (interpose-inline-code
    "*     maybe ~goals~ :goals:
**    [[id:bfc118eb-23b2-42c8-8379-2b0e249ddb76][~clawe~ note]]
some ~famous blob~ with a list

- fancy ~list with spaces~ and things
- Another part of a list ~ without a tilda wrapper"))

(defn interpose-links [s]
  (interpose-pattern s org-link-re parse-hiccup-link))

(comment
  (def t "[[https://github.com/russmatney/some-repo][leading link]]
    check out [[https://www.youtube.com/watch?v=Z9S_2FmLCm8][this video]] on youtube
    and [[https://github.com/russmatney/org-blog][this repo]]
    and [[https://github.com/russmatney/org-crud][this (other) repo]]")
  (interpose-pattern t org-link-re parse-hiccup-link)
  (interpose-links t))

(defn render-text
  "Converts a passed string into spans.
  Wraps naked urls in [:a {:href url} [:span url]].
  Inlines [:code] via `interpose-inline-code`.
  Returns a seq of hiccup vectors."
  [text]
  (-> text interpose-inline-code
      (->>
        (mapcat
          (fn [chunk]
            (cond
              (vector? chunk)
              ;; wrapping [:code] in [:span]
              [[:span chunk]]

              (string? chunk)
              (-> chunk
                  string/trim
                  (string/split-lines)
                  (->>
                    (mapcat #(string/split % #" "))
                    (partition-by is-url?)
                    (mapcat (fn [strs]
                              (let [f-str (-> strs first string/trim)]
                                (cond
                                  (is-url? f-str)
                                  (->> strs (map #(->hiccup-link {:text % :link %})))
                                  :else [[:span (string/join " " strs)]]))))))))))))

(comment
  (render-text "https://github.com/coleslaw-org/coleslaw looks pretty cool!")
  (render-text "
https://reddit.com/r/gameassets/comments/ydwe3e/retro_game_weapons_sound_effects_sound_effects/
https://happysoulmusic.com/retro-game-weapons-sound-effects/
https://github.com/coleslaw-org/coleslaw looks pretty cool!"))

(defn render-text-and-links [s]
  (when s
    (->> s interpose-links
         (mapcat (fn [chunk]
                   (if (string? chunk)
                     (render-text chunk)
                     [chunk])))
         (interpose [:span " "]))))

(comment
  (render-text-and-links
    "check out [[https://www.youtube.com/watch?v=Z9S_2FmLCm8][this video]] on youtube!")

  (render-text-and-links
    "[[https://github.com/russmatney/some-repo][leading link]]
check out [[https://www.youtube.com/watch?v=Z9S_2FmLCm8][this video]] on youtube
and [[https://github.com/russmatney/org-blog][this repo]]
and [[https://github.com/russmatney/org-crud][this other repo]]"))

(defn item->hiccup-headline
  ([item] (item->hiccup-headline item nil))
  ([item opts]
   (when (:org/name item)
     (let [todo-status (:org/status item)]
       (->> item :org/name render-text-and-links
            ((fn [elems]
               (concat
                 (when todo-status
                   [[:span
                     {:class ["font-mono"]}
                     (case todo-status
                       :status/not-started "TODO"
                       :status/in-progress "IN PROGRESS"
                       :status/done        "DONE"
                       "")]
                    [:span " "]])
                 elems)))
            (into
              (let [[header-elem classes prefix]
                    (case (:org/level item)
                      :level/root [:h1 [] nil]
                      1           [:h1 [] nil]
                      2           [:h2 [""] "> "]
                      3           [:h3 [""] ">> "]
                      4           [:h4 [""] ">>> "]
                      5           [:h5 [""] ">>>> "]
                      6           [:h6 [""] ">>>>> "]
                      [:span [] nil])]
                [(or (:header-override opts) header-elem)
                 {:class classes}
                 prefix])))))))

(defn merge-non-list-lines-up
  "Moves non-li text into the previous list-item."
  [lines]
  (reduce
    (fn [agg line]
      (cond
        (#{:unordered-list :ordered-list} (:line-type line))
        (concat agg [line])

        (#{:table-row} (:line-type line))
        (let [last (last agg)]
          (concat (butlast agg)
                  [(update last :text #(str % "\n" (:text line)))]))))
    [] lines))

(defn list-item-prefix [l]
  (some-> l :text (#(re-seq #"^ *[\+|\-|\d]\d*\.? " %))
          first
          (string/replace #"\d" "d")))

(defn line->list-item [replace-reg {:keys [text] :as line}]
  (let [prefix     (list-item-prefix line)
        prefix-len (count prefix)]
    (-> text (string/replace replace-reg "")
        render-text-and-links
        (->> (into [:li
                    {:style {:margin-left (str (* prefix-len 8) "px")}}])))))

(defn render-nested-lists [lines]
  (let [lines                   (->> lines (merge-non-list-lines-up))
        type                    (->> lines first :line-type)
        [list-elem replace-reg] (cond
                                  (#{:unordered-list} type) [:ul #"^ *[-|\+] "]
                                  (#{:ordered-list} type)   [:ol #"^ *\d+\. "]
                                  :else                     [nil nil])]
    (->> lines
         (map (partial line->list-item replace-reg))
         (into [list-elem]))))

(defn render-block [{:keys [content block-type qualifier] :as _block}]
  (cond
    (#{"src" "SRC"} block-type)
    [:div
     [:pre
      [:code {:class (str "language-" qualifier)}
       (->> content (map :text) (string/join "\n"))]]]

    (#{"quote" "QUOTE"} block-type)
    [:blockquote
     (->> content (map :text)
          (map (fn [t] [:span t]))
          (interpose [:span " "])
          (into [:p]))]

    :else
    (do
      (log/warn "[WARN]: unknown block type, using fallback block markup")
      [:div
       (->> content (map :text)
            (map (fn [t] [:span t]))
            (interpose [:span " "])
            (into [:p]))])))

(defn item->hiccup-body [item]
  (->> item :org/body
       (partition-by (comp #{:blank :metadata} :line-type))

       ;; correct paragraph/lists without blank lines between
       (mapcat
         (fn [group]
           (let [first-elem-type  (-> group first :line-type)
                 second-elem-type (some-> group second :line-type)]
             (cond
               (and (#{:table-row} first-elem-type)
                    second-elem-type
                    (#{:unordered-list
                       :ordered-list} second-elem-type))
               [[(first group)] (rest group)]
               :else [group]))))

       (map (fn [group]
              (let [first-elem           (-> group first)
                    first-elem-line-type (-> first-elem :line-type)]
                (cond
                  (#{:blank} first-elem-line-type) nil #_ [:br]

                  (#{:table-row} first-elem-line-type)
                  (->> group (map :text)
                       ;; join the lines so we can handle multi-line links
                       ;; NOTE here we forego the original line breaks :/
                       (string/join " ")
                       (render-text-and-links)
                       (into [:p]))

                  (#{:unordered-list :ordered-list} first-elem-line-type)
                  (render-nested-lists group)

                  (#{:block} (:type first-elem))
                  (render-block group)))))))

(declare tags-list)

(defn item->hiccup-content
  ([item] (item->hiccup-content item nil))
  ([item opts]
   (let [children
         (when-not (:skip-children opts)
           (->> item :org/items (map item->hiccup-content)))]
     (->>
       (concat
         (when-not (:skip-title opts)
           [(item->hiccup-headline item)])
         [(tags-list item)]
         (when-not (:skip-body opts)
           (item->hiccup-body item))
         children)
       (into [:div])))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; links and backlinks
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn backlink-notes [note]
  (->> note :org/id
       blog.db/id->root-notes-linked-from
       (filter (comp blog.db/*id->link-uri* :org/id))))

(defn backlink-hiccup [note]
  (let [b-notes (backlink-notes note)]
    (when (seq b-notes)
      [:div
       [:br]
       [:h1 "Backlinks"]
       (->> b-notes
            (map (fn [note]
                   (->>
                     (concat
                       [[:a {:href (blog.db/*id->link-uri* (:org/id note))}
                         (item->hiccup-headline note {:header-override :h3})]]
                       (item->hiccup-body note))
                     (into [:div {:class "pb-4"}]))))
            (into [:div]))])))

(defn id->backlink-hiccup [id]
  (backlink-hiccup {:org/id id}))

(comment
  (id->backlink-hiccup
    #uuid "2e856678-0711-48f5-94d6-516432e8e2b7"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; note row
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn tags-list
  ([note] (tags-list note nil))
  ([note tags]
   (let [tags (or tags (:org/tags note))]
     (when (seq tags)
       (->>
         tags
         (map #(str "#" %))
         (map-indexed
           (fn [_i tag]
             [:a {:href  (str "/tags.html" tag)
                  :class ["font-mono"]} tag]))
         (into [:div
                {:class ["space-x-1"
                         "flex flex-row flex-wrap"]}]))))))

(defn note-row
  ([note] (note-row note nil))
  ([note opts]
   (let [is-daily? (re-seq #"/daily/" (:org/source-file note))
         children-with-tags
         (if is-daily?
           (cond->> (:org/items note)
             true
             (filter item-has-any-tags)
             (:tags opts)
             (filter #(item-has-tags % (:tags opts))))
           nil)]
     [:div
      {:class ["flex" "flex-col"]}
      [:div
       {:class ["flex" "flex-row" "justify-between"]}
       [:h3
        {:class ["hover:underline" "whitespace-nowrap"
                 "pr-2"]}
        [:a
         {:class ["cursor-pointer"]
          :href  (blog.db/*id->link-uri* (:org/id note))}
         (:org/name-string note)]]

       ;; [:div
       ;;  {:class ["font-mono"]}
       ;;  (->> note :file/last-modified dates/parse-time-string
       ;;       (t/format (t/formatter "hh:mma")))]

       ;; TODO colorize these tags with
       (tags-list note
                  (->> (item->all-tags note) sort))]

      (->> children-with-tags
           (map (fn [ch]
                  (let [t (:org/name-string ch)]
                    [:div
                     {:class ["pl-4"
                              "flex" "flex-row" "justify-between"]}
                     ;; TODO ideally this is a link to an anchor tag for the daily
                     [:h4 t]
                     (tags-list ch)]))))])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; item metadata
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn date-published [note]
  (when (:blog/published-at note)
    (t/format "MMM dd, YYYY"
              (-> note :blog/published-at))))

(defn last-modified [note]
  (t/format "MMM dd, YYYY"
            (-> note :file/last-modified dates/parse-time-string)))

(defn created-at [note]
  (when (:org.prop/created-at note)
    (t/format "MMM dd, YYYY"
              (-> note :org.prop/created-at dates/parse-time-string))))

(defn word-count [note]
  ;; TODO daily notes should filter untagged items in word count
  (let [items (org-crud/nested-item->flattened-items note)]
    (reduce + 0 (map :org/word-count items))))

(defn metadata [item]
  [:div
   {:class ["flex flex-col"]}

   ;; TODO show more metadata
   (let [c (created-at item)]
     (when c
       [:span
        {:class ["font-mono"]}
        (str "Created: " c)]))
   (let [dp (date-published item)]
     (when dp
       [:span
        {:class ["font-mono"]}
        (str "Published: " dp)]))
   [:span
    {:class ["font-mono"]}
    (str "Last modified: " (last-modified item))]
   [:div
    {:class []}
    (if (seq (item->all-tags item))
      (tags-list item
                 (->> (item->all-tags item) sort))
      [:span {:class ["font-mono"]} "No tags"])]
   [:span
    {:class ["font-mono"]}
    (str "Word count: " (word-count item))]
   (let [backlinks (backlink-notes item)]
     (when (seq backlinks)
       [:span
        {:class ["font-mono"]}
        (str "Backlinks: " (count backlinks))]))
   [:hr]])
