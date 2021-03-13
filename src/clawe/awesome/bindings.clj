(ns clawe.awesome.bindings
  "Manages awesomeWM bindings derived from clawe in-memory data structures."
  (:require
   [clawe.bindings :as bindings]
   ))

(defn write-awesome-bindings []
  (let [wsp-bindings  (->> (bindings/list-bindings))
        ;; TODO magic
        file-contents ""]

    (spit "/home/russ/.config/awesome/clawe-bindings.fnl" file-contents)))

(comment
  (write-awesome-bindings))
