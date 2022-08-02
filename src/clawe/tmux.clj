(ns clawe.tmux
  (:require [ralphie.tmux :as tmux]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; tmux session merging
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn merge-tmux-sessions
  "assumes the workspace title and tmux session are the same"
  ([wsps] (merge-tmux-sessions {} wsps))
  ([_opts wsps]
   (when-let [sessions-by-name (try (tmux/list-sessions)
                                    (catch Exception _e
                                      (println "Tmux probably not running!")
                                      nil))]
     (->> wsps
          (map (fn [{:workspace/keys [title] :as wsp}]
                 (if-let [sesh (sessions-by-name title)]
                   (assoc wsp :tmux/session sesh)
                   wsp)))))))

(comment
  (merge-tmux-sessions nil))
