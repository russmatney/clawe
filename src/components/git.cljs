(ns components.git
  (:require [clojure.string :as string]))


(defn short-repo [it]
  (some->
    (re-seq #"(\w+/\w+)$" (:git.commit/directory it))
    first
    first))

(defn commit-comp [_opts it]
  (let [{:git.commit/keys
         [body subject short-hash hash
          lines-added lines-removed
          ]}      it
        short-dir (short-repo it)
        body-lines
        (->
          body
          (string/split "\n"))]
    [:div
     {:class ["flex" "flex-col"]}

     [:div
      {:class ["flex" "flex-row"]}
      [:div
       {:class ["text-xl" "text-city-green-400"]}
       subject]

      [:div
       {:class ["flex" "flex-col" "ml-auto"]}
       [:a {:class ["text-city-pink-400"
                    "hover:text-city-pink-200"
                    "hover:cursor-pointer"]
            ;; TODO not all commits have public repos (ignore dropbox)
            ;; TODO include this link in db commits
            :href  (str "https://github.com/" short-dir "/commit/" hash)}
        short-hash]

       [:a {:class ["text-city-pink-400"
                    "hover:text-city-pink-200"
                    "hover:cursor-pointer"]
            :href  (str "https://github.com/" short-dir)}
        short-dir]

       [:div
        {:class ["text-city-green-200"]}
        (str "+" lines-added " added")]

       [:div
        {:class ["text-city-red-200"]}
        (str "-" lines-removed " removed")]]]

     [:div
      {:class [(when (seq body-lines) "pt-8")]}
      (for [[idx line] (->> body-lines
                            (map-indexed vector))]
        ^{:key idx}
        [:p line])]]))
