(ns ralphie.browser
  (:require
   [babashka.process :as p]
   [defthing.defcom :refer [defcom] :as defcom]
   [clojure.string :as string]
   [ralphie.rofi :as rofi]
   [ralphie.notify :as notify]
   [ralphie.config :as config]
   [ralphie.clipboard :as clipboard]))

(defn line->tab [s]
  (->>
    s
    (#(string/split % #"\t"))
    ((fn [[tab-id title url]]
       ;; TODO parse browser from tab-id + another (cached) bt command
       {:tab/id    tab-id
        :tab/title title
        :tab/url   url}))))

(defn tabs-osx []
  (->
    ;; TODO refactor this - this is what keeps opening safari on osx
    ^{:out :string}
    (p/$
      osascript -e "tell application \"Safari\" to return URL of every tab of every window")
    p/check
    :out
    (string/replace "missing value" "")
    (string/split #",")
    (->> (map string/trim)
         (remove empty?)
         (map (fn [url]
                {:tab/url url})))))

(defn tabs
  "List browser tabs - depends on https://github.com/balta2ar/brotab."
  ([] (tabs nil))
  ([opts]
   (let [query         (:query opts nil)
         default-query "-pinned"]
     (try
       (->> (p/$ bt query ~(if query query default-query ))
            p/check
            :out
            slurp
            (#(string/split % #"\n"))
            (map line->tab))
       (catch Exception err
         (notify/notify "Error building tabs list")
         (println err)
         [])))))

(comment
  (tabs)
  (tabs {:query "+active"})
  (tabs {:query "+highlighted"})
  (let [query "+active"]
    (p/$ bt query -pinned ~(when query query)))

  (->
    ^{:out :string
      :err :string}
    (p/$
      obb -e '(-> (js/Application "Safari")
                  (.-windows)
                  (aget 0) ;; just for first window rn
                  (.tabs)
                  (->> (map (fn [t]
                              (-> (t) (.-url)))))))
    p/check)

  (->
    ^{:out :string}
    (p/$
      osascript -e "tell application \"Safari\" to return URL of every tab of every window")
    p/check
    :out
    (string/replace "missing value" "")
    (string/split #",")
    (->> (map string/trim)
         (remove empty?))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; List open tabs
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defcom list-open-tabs
  "Lists whatever tabs you have open.
Depends on `brotab`."
  {:doctor/depends-on ["bt"]}
  (->> (tabs)
       (map #(assoc % :rofi/label
                    ;; TODO pango markup
                    (str (:tab/url %) ": " (:tab/title %))))
       (rofi/rofi {:msg "Open tabs"})
       ((fn [{:tab/keys [url] :as t}]
          (if url
            (rofi/rofi {:msg (str "Selected: " url)}
                       [ ;; TODO filter options based on url (is this a git url?)
                        {:rofi/label     "Clone repo"
                         :rofi/on-select (fn [_]
                                           (notify/notify "Clone repo not implemented" t))}
                        {:rofi/label     "Copy to clipboard"
                         :rofi/on-select (fn [_]
                                           (println "selected!" url)
                                           (clipboard/set-clip url)
                                           (println "set!" url)
                                           (notify/notify "Copied url to clipboard: " url))}])
            (println "no url in " t))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Copy all open tabs to clipboard
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn copy-tabs [ts]
  (->> ts
       (map :tab/url)
       (string/join "\n")
       clipboard/set-clip)
  (notify/notify (str "Copied " (count ts) " tabs to the clipboard")))

(defcom copy-all-tabs
  "Copies all (non-pinned) urls to the clipboard, seperated by newlines."
  (copy-tabs (tabs {:query "-pinned"})))

(defcom copy-highlighted-tabs
  "Copies only highlighted tab urls to the clipboard, seperated by newlines."
  "Note that this includes the current tab, if no others are highlighted."
  (copy-tabs (tabs {:query "+highlighted"})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Open
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def osx-default-browser-app "/Applications/Firefox.app")

;; TODO support passing in a :tab/url as well
(defn open
  "Opens the passed url"
  ([] (open nil))
  ([opts]
   (let [url (when opts
               (if (string? opts) opts
                   (some opts [:url :browser.open/url])))]
     (if (config/osx?)
       (if url
         (->
           ^{:out :string}
           (p/$ open ~url)
           p/check :out)
         (->
           ^{:out :string}
           (p/$ open -a ~osx-default-browser-app)
           p/check :out))

       (if url
         (->
           ^{:out :string}
           (p/$ xdg-open ~url)
           p/check :out)

         (->
           (p/$ "/usr/bin/gtk-launch" "firefox")
           p/check :out))))))

(comment
  (open {:url "https://github.com"})

  (->
    ^{:out :string}
    (p/$ open -na "/Applications/Safari.app")
    p/check :out)

  (open))

(defn open-dev
  "Opens a dev browser"
  ([] (open-dev nil))
  ([{:keys [url _dev-console?] :as opts}]
   (let [url (cond (string? opts) opts (string? url) url)]
     (if url
       ;; TODO support --dev-console, opt-in from clawe.edn
       (-> (p/$ firefox-developer-edition --new-tab ~url)
           p/check :out)
       (if (config/osx?)
         (-> ^{:out :string}
             (p/$ open -na "/Applications/Firefox Developer Edition.app")
             p/check :out)
         (-> (p/$ "/usr/bin/gtk-launch" "firefoxdeveloperedition.desktop")
             p/check :out))))))

(comment
  (open-dev)
  (open-dev {:url "http://localhost:9999"}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; reload
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn reload-tab [tab]
  (let [tab-id (:tab/id tab)
        url    (:tab/url tab)]
    (->
      ^{:out :string}
      (p/$ bt navigate ~tab-id ~url)
      p/check :out)))

(comment
  (->> (tabs) first))

(defn reload-tabs [opts]
  (let [to-reload
        (->> (tabs)
             (filter (fn [tab]
                       (cond
                         (:url-match opts) (string/includes? (:tab/url tab) (:url-match opts))
                         :else             false))))]
    (doseq [tab to-reload] (reload-tab tab))))

(comment
  (reload-tabs {:url-match "localhost:9999"}))
