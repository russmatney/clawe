(ns clawe.toggle-test
  (:require
   [clojure.test :refer [deftest is testing use-fixtures]]
   [clawe.wm :as wm]
   [clawe.wm.protocol :as wm.protocol]
   [clawe.client :as client]
   [clawe.toggle :as toggle]
   [systemic.core :as sys]
   [clawe.config :as clawe.config]
   [wing.core :as w]))


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
    (clawe.config/reload-config)
    ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; journal toggle
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def journal-client
  {:client/window-title "journal"
   :client/app-name     "Emacs"
   :client/focused      nil})

(def my-repo-emacs-client
  {:client/window-title "my-repo"
   :client/app-name     "Emacs"
   :client/focused      nil})

(def journal-workspace
  {:workspace/title     "journal"
   :workspace/directory "~/todo"
   :workspace/clients   [journal-client]})

(def my-repo-workspace
  {:workspace/title     "my-repo"
   :workspace/directory "~/my-repo"
   :workspace/clients   [my-repo-emacs-client]})

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

  (testing "journal-wsp and journal, found"
    (sys/with-system
      [wm/*wm* (TestWM. [journal-workspace])]
      (set-defs {"journal" journal-def})
      (is (toggle/client-exists? journal-def))
      (is (= (toggle/client-exists? journal-def)
             (merge journal-client journal-def)))))

  (testing "journal-wsp and repo-wsp, correct client found"
    (sys/with-system
      [wm/*wm* (TestWM. [my-repo-workspace journal-workspace])]
      (set-defs {"journal" journal-def})
      (is (toggle/client-exists? journal-def))
      (is (= (toggle/client-exists? journal-def)
             (merge journal-client journal-def))))))
