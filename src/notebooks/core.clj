^{:nextjournal.clerk/visibility {:code :hide :result :hide}}
(ns notebooks.core
  (:require
   [nextjournal.clerk :as clerk]
   [notebooks.system :as system]
   [babashka.fs :as fs]))

;; ## Clerk Files

^{::clerk/visibility {:code :hide}
  ::clerk/no-cache   true
  ::clerk/viewer
  {:transform-fn clerk/mark-presented
   :render-fn
   '(fn [files]
      (v/html
        [:div
         (for [{:keys [f current]} files]
           [:div
            [:span (str (when current "*") "f: " f)]

            [:button
             {:class    ["bg-blue-700" "hover:bg-blue-700"
                         "text-slate-300" "font-bold"
                         "py-2" "px-4"
                         "m-1"
                         "rounded"]
              :on-click (fn [_] (v/clerk-eval `(system/set-file ~f)))}

             "Set as current file"]])]))}}
(->>
  (system/clerk-files)
  (map (fn [f]
         {:f       f
          :current (= f @system/*current-clerk-file*)}))
  (into []))

^{::clerk/visibility {:code :hide}
  ::clerk/no-cache   true}
(clerk/md (str "### -> notebooks/" (fs/file-name @system/*current-clerk-file*)))

^{::clerk/visibility {:code   :hide
                      :result :hide}}
(defn rerender []
  (clerk/show! @system/*current-clerk-file*))
