(ns blog.item
  (:require
   [taoensso.telemere :as log]
   [babashka.fs :as fs]
   [clojure.set :as set]
   [clojure.string :as string]
   [org-crud.core :as org-crud]
   [tick.core :as t]

   [blog.db :as blog.db]
   [blog.config :as blog.config]
   [dates.tick :as dates]
   [components.colors :as colors]
   [util]))

(defn is-daily? [note]
  (re-seq #"/daily/" (:org/source-file note)))

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

(defn item->all-todos [item]
  (->> item org-crud/nested-item->flattened-items (filter :org/status)))

(defn item->all-tags [item]
  (->> item org-crud/nested-item->flattened-items
       (mapcat :org/tags) (into #{})
       (#(disj % "published"))))

(defn item->published-tags [item]
  (->> item
       ((fn [it]
          (if (is-daily? it)
            (update it :org/items #(remove item-has-any-tags %))
            it)))
       org-crud/nested-item->flattened-items
       (mapcat :org/tags) (into #{})
       (#(disj % "published"))))

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
          note       (when id (blog.db/id->note id))
          note-uri   (when note (blog.db/note->published-uri note))
          text-elems (some->>
                       (or text link)
                       interpose-inline-code
                       (map (fn [el] [:span el])))]
      [:span {:class ["not-prose"]}
       (cond
         (and id note-uri)
         (->> text-elems (into [:a {:href       note-uri
                                    :class      ["text-city-green-400"
                                                 "font-bold"
                                                 "hover:underline"]
                                    :aria-label (:org/name-string note)}]))

         (and id (not note-uri))
         (->> text-elems
              (into
                [:span {:class      ["text-city-red-400"
                                     "hover:underline"]
                        :aria-label (str "Unpublished note"
                                         (when note
                                           (str ": " (:org/name-string note))))}]))

         (string/starts-with? link "http")
         (->> text-elems (into [:a {:href       link
                                    :class      ["text-city-orange-400"
                                                 "font-bold"
                                                 "hover:underline"]
                                    :aria-label "External Link"}]))

         ;; uh, probably not too many of these?
         :else text-elems)])))

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
         (interpose [:span " "])
         (reduce
           (fn [agg next]
             (let [last (last agg)]
               (if-not (= last [:span " "])
                 (concat agg [next])

                 (if (and next
                          (second next)
                          (string? (second next))
                          (re-seq #"^(,|\.|\?|\!|:|;)" (second next)))
                   ;; drop span with space if next span is punctuation
                   (concat (butlast agg) [next])
                   (concat agg [next])))))
           []))))


(comment
  (render-text-and-links
    "check out [[https://www.youtube.com/watch?v=Z9S_2FmLCm8][this video]] on youtube!")

  (render-text-and-links
    "[[https://github.com/russmatney/some-repo][leading link]]
check out [[https://www.youtube.com/watch?v=Z9S_2FmLCm8][this video]] on youtube
and [[https://github.com/russmatney/org-blog][this repo]]
and [[https://github.com/russmatney/org-crud][this other repo]]")

  (render-text-and-links
    "[[https://github.com/russmatney/some-repo][leading link]],
    check out [[https://www.youtube.com/watch?v=Z9S_2FmLCm8][this video]]. "))

(defn item->anchor-link [item]
  (-> (str
        (apply str (:org/parent-names item))
        (:org/name-string item))
      string/lower-case
      (string/replace #"[%#:\/\\!.,*`_\-—?<>~ |\(\)\\\"\"\'\[\]\{\}]" "")))

(comment
  (item->anchor-link {:org/name-string "somename%80#"})
  (item->anchor-link {:org/name-string "https://www.chiark.greenend.org.uk/%7Esgtatham/puzzles/"})

  )

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
            ((fn [elems]
               (let [[header-elem prefix]
                     (case (:org/level item)
                       :level/root [:h1 nil]
                       1           [:h1 nil]
                       2           [:h2 "> "]
                       3           [:h3 ">> "]
                       4           [:h4 ">>> "]
                       5           [:h5 ">>>> "]
                       6           [:h6 ">>>>> "]
                       [:span nil])]
                 (into
                   (into
                     [(or (:header-override opts) header-elem)
                      (let [default-classes [] #_ ["flex" "items-center" "space-x-2"]
                            classes         (->> opts :header-override-opts :class
                                                 (concat default-classes))]
                        (merge
                          (:header-override-opts opts {})
                          {:class classes}))
                      prefix]
                     elems)
                   ;; TODO share/copy anchor link to clipboard
                   (when (and (item->anchor-link item)
                              (not (#{:level/root} (:org/level item)))
                              (not (:no-anchor opts)))
                     [(when-let [href (item->anchor-link item)]
                        [:span.pl-6.not-prose.w-full.ml-auto
                         [:a
                          {:id    href :href (str "#" href)
                           :class ["text-sm" "font-mono" "ml-auto"]}
                          ":a:"]])]))))))))))

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
      (log/log! :warn "[WARN]: unknown block type, using fallback block markup")
      [:div
       (->> content (map :text)
            (map (fn [t] [:span t]))
            (interpose [:span " "])
            (into [:p]))])))

(defn render-paragraph [lines]
  (let [any-ends-with-punct?
        (some->> lines
                 (filter (fn [l]
                           (some-> l :text string/trimr
                                   (#(re-seq #"[.|\?|!]$" %)))))
                 seq)]
    (if any-ends-with-punct?
      (->> lines (map :text)
           (string/join "\n")
           (render-text-and-links)
           (into [:p]))
      (->> lines
           ;; this won't be able to support links across line breaks
           (map (comp #(into [:p {:style {:margin-top    "0.2rem"
                                          :margin-bottom "0.2rem"}}] %) render-text-and-links :text))
           (into [:div {:style {:margin-top    "1rem"
                                :margin-bottom "1rem"}}])) )))

(defn render-comment-group
  "Supports images for now."
  [item lines]
  (let [img (->> item :org/images
                 (filter (fn [{:keys [image/path]}]
                           (->> lines
                                (filter (fn [line]
                                          (-> line :text
                                              (string/replace "[[" "")
                                              (string/replace "]]" "")
                                              (= path))))
                                seq)))
                 first)]
    (when (some-> img blog.config/image->blog-path fs/exists?)
      (log/log! :info ["Creating img component in note" (:org/source-file item)])
      (let [img-path (blog.config/image->uri img)
            alt      (:image/name img (str (:image/path img)))]
        [:div
         {:class ["flex flex-col"]}
         (case (:image/extension img)
           "mp4" [:video {:controls true}
                  [:source {:src img-path :type "video/mp4"}]]
           [:img {:src img-path :alt alt}])

         (when (:image/name img)
           [:p {:class ["self-center"
                        "font-nes"]
                :style (when (or (:image/date-string img)
                                 (:image/caption img)) {:margin-bottom 0})}
            (:image/name img)])
         (when (:image/caption img)
           [:p {:class ["self-center"
                        "font-mono"]
                :style (when (:image/date-string img) {:margin-bottom 0})}
            (:image/caption img)])
         (when (:image/date-string img)
           [:p {:class ["self-center"
                        "font-mono"]}
            (str "captured: " (:image/date-string img))])]))))

(defn item->hiccup-body
  ([item] (item->hiccup-body item nil))
  ([item _opts]
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
                        :ordered-list
                        :comment} second-elem-type))
                [[(first group)] (rest group)]
                :else [group]))))

        (map (fn [group]
               (let [first-elem           (-> group first)
                     first-elem-line-type (-> first-elem :line-type)
                     first-elem-type      (-> first-elem :type)]
                 (cond
                   (#{:blank} first-elem-line-type) nil #_ [:br]

                   (#{:table-row} first-elem-line-type)
                   (render-paragraph group)

                   (#{:unordered-list :ordered-list} first-elem-line-type)
                   (render-nested-lists group)

                   (#{:block} first-elem-type)
                   (render-block group)

                   ;; TODO should also support images without comments preceding
                   (#{:comment} first-elem-line-type)
                   (render-comment-group item group))))))))

(comment
  (def note
    (->> (blog.db/find-notes "2023-04-04")
         first
         :org/items
         (filter (comp seq #(set/intersection % #{"bug"}) :org/tags))
         first))

  (item->hiccup-body note))


(declare note->tags-list)

(defn item->hiccup-content
  ([item] (item->hiccup-content item nil))
  ([item opts]
   (let [children
         (when-not (:skip-children opts)
           (cond->> item
             true              :org/items
             (:filter-fn opts) (filter (:filter-fn opts))
             true              (map #(item->hiccup-content
                                       %
                                       ;; don't skip children's titles
                                       (dissoc opts :skip-title)))))]
     (->>
       (concat
         (when-not (:skip-title opts)
           [(item->hiccup-headline item opts)])
         [(note->tags-list item)]
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
       (filter (comp blog.db/id->link-uri :org/id))))

(defn backlink-hiccup [note]
  (let [b-notes (backlink-notes note)]
    (when (seq b-notes)
      [:div
       [:br]
       [:h1 "Backlinks"]
       (->> b-notes
            (map (fn [b-note]
                   (->>
                     (concat
                       [[:div
                         {:class ["not-prose"]}
                         [:a {:href       (blog.db/id->link-uri (:org/id b-note))
                              :class      ["text-slate-400"]
                              :aria-label (:org/name-string b-note)}
                          (item->hiccup-headline b-note {:header-override :h3
                                                         :no-anchor       true})]]]
                       ;; not quite useful yet b/c it doesn't get context for links in children
                       ;; i.e. we still see dailies but don't get any context
                       #_[[:span
                           (let [{:link/keys [matching-lines-strs] :as link-to} (some->> b-note :org/links-to
                                                                                         (filter (comp #{(:org/id note)} :link/id))
                                                                                         first
                                                                                         )]
                             (apply str matching-lines-strs)
                             )

                           ]]
                       (item->hiccup-body b-note))
                     (into [:div {:class ["pb-4"]}]))))
            (into [:div]))])))

(defn id->backlink-hiccup [id]
  (backlink-hiccup {:org/id id}))

(comment
  (id->backlink-hiccup
    #uuid "2e856678-0711-48f5-94d6-516432e8e2b7"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; note row
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn href-pill-list
  "Supports a list of colorized xs, where each x is a string or a map like:
  - :href
  - :label

  Intended to support anchor links.
  "
  [xs]
  (when (seq xs)
    (->>
      xs
      (map-indexed
        (fn [i x]
          (let [label (:label x)]
            [:span
             {:class ["not-prose" "px-1"]
              :style {:line-height "2rem"}}
             [:a {:href  (:href x)
                  :class (concat ["font-mono"]
                                 (colors/color-wheel-classes {:i i :type :line}))}
              label]])))
      (into [:div {:class ["flex flex-row flex-wrap" "justify-center" "not-prose"]}]))))


(defn tags-href-list
  "Builds"
  [tags]
  (->> tags
       (map (fn [tag]
              (cond (string? tag) {:href  (str "/tags.html#" tag)
                                   :label (str "#" tag)}
                    (map? tag)    (cond-> tag
                                    (not (:href tag))
                                    (assoc :href (str "/tags.html#" (:tag tag)))
                                    (not (:label tag))
                                    (assoc :label (str "#" (:tag tag)
                                                       (when (:n tag) (str "/" (:n tag))))))
                    :else         tag)))
       href-pill-list))


(defn note->tags-list-terms
  ([note] (note->tags-list-terms note nil))
  ([note tags]
   (let [tags (or tags (->> note :org/tags (#(disj % "published"))))]
     (when (seq tags)
       (->>
         tags
         (map #(str "#" %))
         (map-indexed
           (fn [i tag]
             [:span
              {:class ["not-prose" "px-1"]
               :style {:line-height "2rem"}}
              [:a {:href  (str "/tags.html" tag)
                   :class (concat ["font-mono"]
                                  (colors/color-wheel-classes {:i i :type :line}))} tag]])))))))

(defn note->tags-list
  ([note] (note->tags-list note nil))
  ([note tags]
   (let [terms (note->tags-list-terms note tags)]
     (when (seq terms)
       (->> terms
            (into [:div {:class ["flex flex-row flex-wrap" "not-prose"]}]))))))

(defn note-child-row [{:keys [uri]} item]
  [:div
   {:class ["pl-4" "flex" "flex-row" "justify-between"]}
   (->>
     (note->tags-list-terms item)
     (into
       [:span
        {:class []}
        [:a {:href  (str uri "#" (item->anchor-link item))
             :class (concat ["no-prose" "font-mono"]
                            (case (:org/level item)
                              :level/root []
                              1           []
                              2           ["pl-2"]
                              3           ["pl-4"]
                              4           ["pl-8"]
                              5           ["pl-16"]
                              6           ["pl-24"]
                              []))}
         (:org/name-string item)]]))])

(defn note-row
  ([note] (note-row note nil))
  ([note opts]
   (let [children-with-tags
         (when (is-daily? note)
           (cond->> (:org/items note)
             true         (filter item-has-any-tags)
             (:tags opts) (filter #(item-has-tags % (:tags opts)))))
         uri (blog.db/id->link-uri (:org/id note))]
     [:div
      {:class ["flex" "flex-col" "mb-2"]}

      [:div
       {:class ["flex" "flex-row" "flex-wrap"
                "text-center" "md:text-left"
                "justify-center" "md:justify-normal"]}
       [:h3
        {:class ["hover:underline" "pr-2" "not-prose" "flex" "w-full"]
         :style {:margin-bottom 0}}
        [:a
         {:class      ["cursor-pointer" "w-full"]
          :href       uri
          :aria-label (:org/name-string note)}
         (:org/name-string note)]]

       ;; TODO colorize these tags with
       (note->tags-list-terms note
                              (->> (item->all-tags note) sort))]

      (->> children-with-tags
           (remove :org/status)
           (map #(note-child-row {:uri uri} %)))])))

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
      (note->tags-list item
                       (->> (item->all-tags item) sort))
      [:span {:class ["font-mono"]} "No tags"])]
   [:span
    {:class ["font-mono"]}
    (str "Word count: " (word-count item))]
   (let [backlinks (backlink-notes item)]
     (when (seq backlinks)
       [:span
        {:class ["font-mono"]}
        (str "Backlinks: " (count backlinks))]))])
