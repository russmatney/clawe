(ns blog.render
  (:require
   [babashka.fs :as fs]
   [babashka.process :as process]
   [taoensso.timbre :as log]
   [hiccup.page :as hiccup.page]
   [hiccup2.core :as hiccup2.core]
   [clojure.string :as string]
   [blog.config :as blog.config]
   [blog.db :as blog.db]))

(defn format-html-file [path]
  (-> ^{:out :string}
      (process/$ tidy -mqi
                 --indent-spaces 1
                 --tidy-mark no
                 --enclose-block-text yes
                 --enclose-text yes
                 --drop-empty-elements no
                 ~path)
      process/check :out))

;; --new-inline-tags fn
(comment
  (format-html-file "public/test.html")
  (format-html-file "public/last-modified.html"))


(def main-title "DangerRuss Notes")
(def about-link-uri "/note/blog_about.html")

(defn header []
  [:div
   {:class ["flex" "flex-col" "items-center"
            "text-gray-900" "dark:text-white" "w-full"]}
   [:div
    {:class ["flex" "flex-row"
             "items-center"
             "max-w-prose" "w-full" "px-8" "py-2"]}
    [:h3 {:class ["font-mono"]} main-title]

    [:div
     {:class ["ml-auto" "flex" "flex-row" "space-x-4"]}
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
            :href  about-link-uri} "about"]]]]]
   [:hr]])

(defn footer []
  (let [mastodon-href (blog.config/get-mastodon-href)]
    [:div
     {:class ["flex" "flex-col" "items-center"
              "text-gray-900"
              "dark:text-white" "pb-8"]}

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
               :rel   "me"} "mastodon"]]])]]))


(defn ensure-path [path]
  (let [parent (fs/parent path)]
    (when-not (fs/exists? parent)
      (log/info "ensuring parent dir exists")
      (fs/create-dirs parent))))


(defn ->html-with-escaping [title content]
  (let [ga-id (blog.config/get-google-analytics-id)]
    (hiccup2.core/html
      {:mode :html}
      (hiccup.page/doctype :html5)
      [:html.dark
       [:head
        [:title title]
        [:meta {:charset "UTF-8"}]
        [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]

        [:link {:type "text/css" :rel "stylesheet" :href "/styles.css"}]
        [:script {:type "text/javascript"
                  :src  "https://storage.googleapis.com/nextjournal-cas-eu/assets/28ktYzexRpt9ZsXvxpxDRnu497pkEeZjEvXB1NMVzfEoPEgsbQXEyM3j5CEucNccte6QGnX1qQxHL2KHfoBRG2FN-viewer.js"}]
        [:link {:type "text/css"
                :href "https://fonts.googleapis.com/css2?family=Fira+Code:wght@400;700&family=Fira+Mono:wght@400;700&family=Fira+Sans+Condensed:ital,wght@0,700;1,700&family=Fira+Sans:ital,wght@0,400;0,500;0,700;1,400;1,500;1,700&family=PT+Serif:ital,wght@0,400;0,700;1,400;1,700&display=swap"
                :rel  "stylesheet"}]
        [:link {:href "https://fonts.googleapis.com/css?family=Press+Start+2P"
                :rel  "stylesheet"}]

        #_(when-not @blog.config/!debug-mode)
        (when true
          (log/info "[BLOG-RENDER]: Debug Build, including live.js")
          [:script {:type "text/javascript"
                    :src  "https://livejs.com/live.js"}])

        (when ga-id
          [:script {:async true :src (str "https://www.googletagmanager.com/gtag/js?id=" ga-id)}])
        (when ga-id
          [:script
           (hiccup2.core/raw
             (str "window.dataLayer = window.dataLayer || [];
function gtag() { dataLayer.push(arguments); }
gtag('js', new Date());
gtag('config', '" ga-id "');"))])]
       [:body
        {:class ["dark:bg-gray-900"]}
        [:div.flex.flex-col.items-center
         (header)
         [:div
          {:class ["w-full" "max-w-prose" "px-8" "viewer-notebook"]}
          content]
         (footer)]]])))

(defn write-page [{:keys [note path title content]}]
  (let [public-path (blog.config/blog-content-public)
        path        (cond
                      note  (str public-path (blog.db/note->uri note))
                      (and path (string/includes? path public-path))
                      path
                      :else (str public-path path))]
    (ensure-path path)
    (log/info "[PUBLISH]: writing path" path)
    (spit path (->html-with-escaping
                 (if title (str title " - " main-title) main-title)
                 content))
    (format-html-file path)))

(defn write-styles []
  (log/info "[PUBLISH]: exporting tailwind styles")
  (let [content-path (str (blog.config/blog-content-public) "/**/*.html")]
    (->
      ^{:out :string :dir (blog.config/blog-content-root)}
      (process/$ npx tailwindcss
                 -c "resources/tailwind.config.js"
                 -i "resources/styles.css"
                 --content ~content-path
                 -o ~(str "public/styles.css"))
      process/check :out)))

(comment
  (write-page
    {:path    "public/test.html"
     :content [:div
               [:h1 "test page"]
               [:h2 "full of content"]]}))
