(ns ralphie.browser
  (:require
   [babashka.process :as p]
   [defthing.defcom :refer [defcom] :as defcom]
   [clojure.string :as string]
   [ralphie.rofi :as rofi]
   [ralphie.notify :as notify]
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

(defn tabs
  "List browser tabs - depends on https://github.com/balta2ar/brotab.

  "
  ([] (tabs nil))
  ([opts]
   (let [query         (:query opts nil)
         default-query "-pinned"
         ]
     (->> (p/$ bt query ~(if query query default-query ))
          p/check
          :out
          slurp
          (#(string/split % #"\n"))
          (map line->tab)))))

(comment
  (tabs)
  (tabs {:query "+active"})
  (tabs {:query "+highlighted"})
  (let [query "+active"]
    (p/$ bt query -pinned ~(when query query)))
  )

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

(defn open
  "Opens the passed url"
  [opts]
  (let [url (some opts [:url :browser.open/url])]
    (->
      (p/$ xdg-open ~url)
      p/check :out slurp)))

(comment
  (open {:url "https://github.com"})
  )
