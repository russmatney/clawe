(ns clawe.toggle-test
  (:require
   [clojure.test :refer [deftest is testing use-fixtures]]
   [test-util :refer [gen-data]]
   [clawe.wm :as wm]
   [clawe.wm.protocol :as wm.protocol]
   [clawe.client :as client]
   [clawe.toggle :as toggle]
   [systemic.core :as sys]
   [clawe.config :as clawe.config]
   [wing.core :as w]
   [clawe.workspace :as workspace]))


(defrecord TestWM [workspaces]
  wm.protocol/ClaweWM
  (-current-workspaces [_this _opts] workspaces)
  (-active-workspaces [_this _opts] workspaces)
  (-active-clients [_this _opts]
    (println "active-clients called")
    (println workspaces)
    (->> workspaces (mapcat :workspace/clients))))

(defn set-defs [defs]
  (reset! clawe.config/*config* {:client/defs defs}))

(use-fixtures
  :once
  (fn [f]
    (f)
    ;; reload the config to have mercy on the repl-user
    (clawe.config/reload-config)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; schema and fixtures
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def journal-client
  {:client/window-title "journal"
   :client/app-name     "Emacs"
   :client/focused      nil})

(def myrepo-emacs-client
  {:client/window-title "myrepo"
   :client/app-name     "Emacs"
   :client/focused      nil})

(def journal-workspace
  {:workspace/title     "journal"
   :workspace/directory "~/todo"})

(def myrepo-workspace
  {:workspace/title     "myrepo"
   :workspace/directory "~/myrepo"})

(def schema
  {
   ;; clients
   :client
   {:prefix   :client :schema client/schema
    :spec-gen {:client/focused nil}}

   :j-cli
   {:prefix   :jc :schema client/schema
    :spec-gen journal-client}

   :myrepo-cli
   {:prefix   :mrc
    :schema   client/schema
    :spec-gen myrepo-emacs-client}

   ;; workspaces
   :workspace
   {:prefix   :wsp
    :schema   workspace/schema
    :spec-gen {:workspace/clients []}}

   :wsp+clients
   {:prefix :wsp-cs
    :schema workspace/schema}

   :j-wsp
   {:prefix   :jw
    :schema   workspace/schema
    :spec-gen (assoc journal-workspace :workspace/clients [])}

   :j-wsp+j-cli
   {:prefix   :jw-jc
    :schema   workspace/schema
    :spec-gen (assoc journal-workspace :workspace/clients [journal-client])}

   :myrepo-wsp
   {:prefix   :mrw
    :schema   workspace/schema
    :spec-gen (assoc myrepo-workspace :workspace/clients [])}

   :myrepo-wsp+myrepo-cli
   {:prefix   :mrw-mrc
    :schema   workspace/schema
    :spec-gen (assoc myrepo-workspace :workspace/clients [myrepo-workspace])}})

(comment
  (gen-data schema
            {:journal-client    [[1]]
             :journal-workspace [[1]]
             ;; :workspace-with-clients [[1]]
             :workspace         [[1]]}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; journal toggle
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def journal-def
  {:client/app-names    ["Emacs"]
   :client/window-title "journal"
   ;; adding key just to ease testing, should maybe remove...
   :client/key          "journal"})

(def client-defs
  {"journal" journal-def})

(deftest journal-client-exists-test
  (testing "no clients, not found"
    (sys/with-system
      [wm/*wm* (TestWM. [])]
      (set-defs {"journal" journal-def})
      (is (not (toggle/client-exists? journal-def)))))

  (testing "journal-wsp without journal client, not found"
    (let [{:keys [j-wsp]}
          (gen-data schema {:j-wsp [[1]]})]
      (sys/with-system
        [wm/*wm* (TestWM. (vals j-wsp))]
        (set-defs {"journal" journal-def})
        (is (not (toggle/client-exists? journal-def))))))

  (testing "journal-wsp with journal client, found"
    (let [{:keys [j-wsp+j-cli]}
          (gen-data schema {:j-wsp+j-cli [[1]]})]
      (sys/with-system
        [wm/*wm* (TestWM. (vals j-wsp+j-cli))]
        (set-defs {"journal" journal-def})
        (is (toggle/client-exists? journal-def))
        (is (= (toggle/client-exists? journal-def)
               (merge journal-client journal-def))))))

  (testing "journal-wsp and repo-wsp, correct client found"
    (let [{:keys [j-wsp+j-cli myrepo-wsp+myrepo-cli]}
          (gen-data schema {:j-wsp+j-cli           [[1]]
                            :myrepo-wsp+myrepo-cli [[1]]})]
      (sys/with-system
        [wm/*wm* (TestWM. (mapcat vals [j-wsp+j-cli myrepo-wsp+myrepo-cli]))]
        (set-defs {"journal" journal-def})
        (is (toggle/client-exists? journal-def))
        (is (= (toggle/client-exists? journal-def)
               (merge journal-client journal-def)))))))
