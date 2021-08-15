(ns clawe.godot
  (:require
   [defthing.defcom :refer [defcom] :as defcom]
   [ralphie.notify :as notify]
   [ralphie.emacs :as emacs]))

(defcom open-godot-file
  (fn [com args]
    (if-let [filepath (some-> args first)]
      (do
        (notify/notify "open-godot-file" (str args " - " com))
        ;; for now, this appears to open the file in last-used emacs client
        (emacs/eval-form (str "(find-file \"" filepath "\")"))

        ;; TODO focus emacs client - there's logic for this in defs/bindings
        )
      (notify/notify "[ERROR]" "open-godot-file called with no args"))))

(comment
  (defcom/exec open-godot-file nil)
  )
