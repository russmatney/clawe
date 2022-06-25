(ns components.git
  (:require
   [tick.core :as t]

   [components.debug]
   [components.floating :as floating]
   [hooks.repos]
   [hooks.commits]))


(declare repo-label)
(declare repo-popover-content)

(defn short-repo [it]
  (some->>
    (or (:git.commit/directory it)
        (:repo/path it))
    (re-seq #"([A-Za-z-]+/[A-Za-z-]+)$")
    first
    first))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; commit thumbnail
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn commit-thumbnail
  ([commit] [commit-thumbnail nil commit])
  ([_opts commit]
   [:div
    {:class ["flex" "flex-row"
             "gap-x-4"]}

    [:div
     (short-repo commit)]

    [:div
     (:git.commit/short-hash commit)]

    [:div
     (:git.commit/subject commit)]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; commit-popover
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn commit-popover-content
  ([commit] [commit-popover-content nil commit])
  ([_opts commit]
   (let [repo-name (short-repo commit)
         repo      (:commit/repo commit)]
     [:div
      {:class ["bg-city-blue-900"
               "border"
               "border-city-blue-400"
               "text-city-pink-100"
               "text-opacity-90"]}
      [:div
       {:class ["flex" "flex-col"]}
       [:div
        {:class ["flex" "flex-row"
                 "py-4" "px-4"
                 "gap-x-8"
                 "justify-between"]}
        [:div
         {:class ["text-city-pink-300"]}
         (if repo
           [components.floating/popover
            {:hover true :click true
             :anchor-comp
             [:div repo-name]
             :popover-comp
             [repo-popover-content repo]}]
           repo-name)]

        [:a {:class [(when repo-name "text-city-pink-300")
                     (when repo-name "hover:text-city-pink-200")
                     (when repo-name "hover:cursor-pointer")]
             ;; TODO not all commits have public repos (ignore dropbox)
             ;; TODO include this link in db commits
             :href  (when repo-name (str "https://github.com/"
                                         repo-name "/commit/"
                                         (:git.commit/hash commit)))}
         (:git.commit/short-hash commit)]

        (when (or
                (:git.commit/lines-added commit)
                (:git.commit/lines-removed commit))
          [:div
           {:class ["flex" "flex-row" "gap-x-2"]}
           [:div
            {:class ["text-city-red-400"]}
            (str "+" (:git.commit/lines-added commit 0))]
           [:div
            {:class ["text-city-green-400"]}
            (str "-" (:git.commit/lines-removed commit 0))]])

        [:div
         {:class ["ml-auto"
                  "flex" "flex-row"
                  "gap-x-2"]}
         [:div
          {:class ["text-city-pink-100"]}
          (:git.commit/author-name commit)]

         [:div
          {:class ["text-city-pink-100"]}
          (t/format "h:mma" (:event/timestamp commit))]]]

       [:div
        {:class ["bg-city-blue-800"
                 "flex"
                 "flex-col"
                 "p-4" "font-mono"]}
        [:div
         {:class [""]}
         (:git.commit/subject commit)]

        [:div
         {:class ["pt-2" "whitespace-pre-line" "font-mono"]}
         (:git.commit/body commit)]

        [components.debug/raw-metadata commit]]]])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; commit list
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn commit-list [opts commits]
  [:div
   {:class ["flex" "flex-col"]}

   (for [[i event] (->> commits
                        (sort-by :event/timestamp t/<)
                        (map-indexed vector))]
     [:div
      {:key   i
       :class ["m-2"]}

      (when (:git.commit/hash event)
        [floating/popover
         {:click true :hover true
          :anchor-comp
          [:div
           {:class ["cursor-pointer"]}
           [components.git/commit-thumbnail opts event]]
          :popover-comp
          [:div
           [components.git/commit-popover-content opts event]]}])])])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; repos
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn repo-label [repo]
  [:div
   {:class ["text-white"]}
   (:repo/path repo)])

(defn repo-popover-content
  ([repo] [repo-popover-content nil repo])
  ([_opts repo]
   (let [commits-resp (hooks.commits/use-commits)
         commits      (:items commits-resp)
         commits      (->> commits (filter (comp #{(:repo/path repo)} :git.commit/directory)))]
     [:div
      {:class ["text-white"
               "bg-yo-blue-800"
               "p-6"
               "border"
               "border-city-blue-800"]}
      [repo-label repo]

      [commit-list nil commits]])))

(defn repo-popover [repo]
  [:div
   {:class    ["hover:text-city-blue-800"
               "cursor-pointer"
               ]
    :on-click (fn [_] (hooks.repos/fetch-commits repo))}
   [components.floating/popover
    {:hover true :click true
     :anchor-comp
     [:div
      {:class ["text-white"
               "hover:text-city-pink-400"]}
      (or
        (short-repo repo)
        (:repo/path repo))]
     :popover-comp
     [repo-popover-content repo]}]])
