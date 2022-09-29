^{:nextjournal.clerk/visibility {:code :hide :result :hide}}
(ns notebooks.nav
  (:require
   [nextjournal.clerk :as clerk]
   [notebooks.server :as server]
   [babashka.fs :as fs]))

(defn nav-clicked [f]
  (println "nav-clicked with " f)
  (let [{:keys [path]} f]
    ;; maybe eval?
    (server/set-file f)))

(def nav-viewer
  {:transform-fn clerk/mark-presented
   :render-fn
   '(fn [files]
      (v/html
        [:div
         [:h3 "Notebooks"]
         (for [{:keys [path name current] :as f} files]
           [:div
            [:span (str (when current "*") ": " name)]

            [:button
             {:class    ["bg-blue-700" "hover:bg-blue-700"
                         "text-slate-300" "font-bold"
                         "py-2" "px-4" "m-1"
                         "rounded"]
              :on-click (fn [_]
                          (js/console.log "nav-clicked with " (clj->js f))
                          (v/clerk-eval `(nav-clicked ~f))
                          (js/location.replace (str "/notebooks/" name)))}

             "Set as current file"]])]))})

^{::clerk/visibility {:code :hide}
  ::clerk/no-cache   true
  ::clerk/viewer     nav-viewer}
(def nav-options
  (->>
    (server/clerk-files)
    (map (fn [f] (assoc f :current
                        (= (:path f) (:path @server/*current-clerk-file*)))))
    (into [])))

^{::clerk/visibility {:code :hide}
  ::clerk/no-cache   true}
(clerk/md (str "### -> notebooks/" (fs/file-name (:path @server/*current-clerk-file*))))
