(ns clawe.install
  (:require
   [babashka.process :refer [$ check]]
   [ralphie.util :as r.util]
   [ralphie.notify :as r.notify]
   [ralphie.sh :as r.sh]
   [ralphie.install :as r.install]
   [defthing.defcom :as defcom :refer [defcom]]))

(defn install-zsh-tab-completion []
  (r.install/install-zsh-completion "clawe"))

(defcom install-clawe-zsh-tab-completion
  (install-zsh-tab-completion))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Build Clawe Uberjar
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn build-uberjar
  "Rebuilds the clawe uberjar. Equivalent to:

bb -cp $(clojure -Spath) --uberjar clawe.jar -m clawe.core # rebuild clawe

  on the command line."
  []
  (let [notif (fn [s] (r.notify/notify
                        {:subject s :replaces-process "rebuilding-clawe-uberjar"}))
        dir   (r.sh/expand "~/russmatney/clawe")]
    (notif "Clawe Uberjar: Rebuilding")
    (let [cp (r.util/get-cp dir)]
      (->
        ^{:dir dir}
        ($ bb -cp ~cp --uberjar clawe.jar -m clawe.core )
        check)
      (notif "Clawe Uberjar: Rebuild Complete"))))

(defcom rebuild-clawe build-uberjar)
