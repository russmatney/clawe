(ns ralphie.clipboard
  (:require
   [clojure.string :as string]
   [babashka.process :as p]
   [clojure.java.shell :as sh]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Read the clipboard
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn get-clip [clipboard-name]
  (-> (sh/sh "xclip" "-o" "-selection" clipboard-name)
      :out))

(defn get-all
  "Returns a list of things on available clipboards."
  []
  {:clipboard (get-clip "clipboard")
   :primary   (get-clip "primary")
   ;; :secondary (get-clip "secondary")
   ;; :buffer-cut (get-clip "buffer-cut")
   })

(defn values []
  (->> (get-all)
       vals
       (map string/trim)
       (remove empty?)))

(comment
  (values))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Write to the clipboard
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn set-clip [s]
  (-> (p/process '[xclip -i -selection clipboard]
                 {:in s})
      p/check
      :out
      slurp))

(comment
  (set-clip "hello\ngoodbye")
  (set-clip "siyanora")
  (get-clip "primary")
  (sh/sh "xclip" "-o" "-selection" "primary")
  (get-all)
  )
