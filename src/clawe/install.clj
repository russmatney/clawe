(ns clawe.install
  (:require
   [babashka.process :refer [$ check]]
   [ralphie.util :as r.util]
   [ralphie.notify :as r.notify]
   [ralphie.sh :as r.sh]
   [defthing.defcom :as defcom :refer [defcom]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Build Clawe Uberjar
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn build-uberjar []
  (let [proc "rebuilding-clawe-uberjar"]
    (r.notify/notify {:subject          "Clawe Uberjar: Rebuilding"
                      :replaces-process proc})
    (let [cp (r.util/get-cp (r.sh/expand "~/russmatney/clawe"))]
      (->
        ^{:dir (r.sh/expand "~/russmatney/clawe")}
        ($ bb -cp ~cp --uberjar clawe.jar -m clawe.core )
        check)
      (r.notify/notify {:subject          "Clawe Uberjar: Rebuild Complete"
                        :replaces-process proc}))))

(defcom rebuild-clawe build-uberjar)
