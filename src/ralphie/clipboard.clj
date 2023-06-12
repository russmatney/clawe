(ns ralphie.clipboard
  (:require
   [clojure.string :as string]
   [babashka.process :as p]
   [clojure.java.shell :as sh]
   [ralphie.config :as config]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Read the clipboard
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn get-clip
  ([] (get-clip "primary"))
  ([clipboard-name]
   (if (config/osx?)
     (-> ^{:out :string} (p/$ pbpaste) p/check :out)
     (-> (sh/sh "xclip" "-o" "-selection" clipboard-name)
         :out))))

(comment
  (get-clip)
  )

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
  (if (config/osx?)
    (-> ^{:out :string :in s} (p/$ pbcopy) p/check :out)
    (-> (p/process '[xclip -i -selection clipboard]
                   {:in s})
        p/check
        :out
        slurp)))

(comment
  (set-clip "hello\ngoodbye")
  (set-clip "siyanora")
  (get-clip "primary")
  (sh/sh "xclip" "-o" "-selection" "primary")
  (get-all)
  )
