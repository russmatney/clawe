(ns blog.components.header
  (:require
   [blog.config :as blog.config]))

(def about-link-uri "/note/blog_about.html")

(defn header []
  (let [mastodon-href (blog.config/get-mastodon-href)
        _lemmy-href   (blog.config/get-lemmy-href)
        youtube-href  (blog.config/get-youtube-href)]
    [:div
     {:class ["flex" "flex-col" "items-center"
              "text-gray-100" "w-full"]}
     [:div
      {:class ["flex" "flex-row"
               "items-center"
               "max-w-prose" "w-full" "px-8" "py-2"]}
      [:a {:class ["font-mono"
                   "hover:underline"
                   "cursor-pointer"
                   "text-gray-100"]
           :href  "/index.html"}
       [:div
        {:class ["flex" "flex-row" "align-center"]}
        [:img {:class ["object-scale-down"]
               :src   "/images/portrait-nobg-2x.png"
               :alt   "Pixellated Portrait of a ginger with a beard."}]
        [:h3 {:class ["ml-4" "mt-0" "font-nes"]} "Danger" [:br] "Russ" [:br] "Notes"]]]

      [:div
       {:class ["ml-auto" "flex" "flex-col" "md:flex-row"
                "md:space-x-4"]}
       [:div
        [:h4
         [:a {:class ["font-mono"
                      "hover:underline"
                      "cursor-pointer"]
              :href  "/index.html"} "home"]]]
       [:div
        [:h4
         [:a {:class ["font-mono"
                      "hover:underline"
                      "cursor-pointer"]
              :href  about-link-uri} "about"]]]

       (when mastodon-href
         [:div
          [:h4
           [:a {:class ["font-mono"
                        "hover:underline"
                        "cursor-pointer"]
                :href  mastodon-href
                :rel   "me"} "mastodon"]]])

       (when youtube-href
         [:div
          [:h4
           [:a {:class ["font-mono"
                        "hover:underline"
                        "cursor-pointer"]
                :href  youtube-href} "youtube"]]])

       #_(when lemmy-href
           [:div
            [:h4
             [:a {:class ["font-mono"
                          "hover:underline"
                          "cursor-pointer"]
                  :href  lemmy-href} "lemmy"]]])

       [:div
        [:h4
         [:a {:class ["font-mono"
                      "hover:underline"
                      "cursor-pointer"]
              :href  "https://github.com/russmatney"} "github"]]]
       ]]
     [:hr]]))

(defn footer []
  (let [mastodon-href (blog.config/get-mastodon-href)
        _lemmy-href   (blog.config/get-lemmy-href)
        youtube-href  (blog.config/get-youtube-href)]
    [:div
     {:class ["flex" "flex-col" "items-center"
              "text-gray-100" "pb-8"]}

     [:hr]
     [:div
      {:class ["flex" "flex-row" "space-x-4"]}
      [:div
       [:h4
        [:a {:class ["font-mono"
                     "hover:underline"
                     "cursor-pointer"]
             :href  "/index.html"} "home"]]]
      [:div
       [:h4
        [:a {:class ["font-mono"
                     "hover:underline"
                     "cursor-pointer"]
             :href  about-link-uri} "about"]]]

      (when mastodon-href
        [:div
         [:h4
          [:a {:class ["font-mono"
                       "hover:underline"
                       "cursor-pointer"]
               :href  mastodon-href
               :rel   "me"} "mastodon"]]])

      (when youtube-href
        [:div
         [:h4
          [:a {:class ["font-mono"
                       "hover:underline"
                       "cursor-pointer"]
               :href  youtube-href} "youtube"]]])

      #_(when lemmy-href
          [:div
           [:h4
            [:a {:class ["font-mono"
                         "hover:underline"
                         "cursor-pointer"]
                 :href  lemmy-href} "lemmy"]]])

      [:div
       [:h4
        [:a {:class ["font-mono"
                     "hover:underline"
                     "cursor-pointer"]
             :href  "https://github.com/russmatney"} "github"]]]]]))
