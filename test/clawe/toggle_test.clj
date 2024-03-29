(ns ^:integration clawe.toggle-test
  (:require
   [clojure.test :refer [deftest is testing use-fixtures]]
   [test-util :refer [gen-data]]
   [clawe.wm :as wm]
   [clawe.wm.protocol :as wm.protocol]
   [clawe.client :as client]
   [clawe.toggle :as toggle]
   [systemic.core :as sys]
   [clawe.config :as clawe.config]
   [clawe.workspace :as workspace]))


(defrecord TestWM [workspaces]
  wm.protocol/ClaweWM
  (-current-workspaces [_this _opts]
    ;; the first workspace is the 'current' one
    (->> workspaces (take 1)))
  (-active-workspaces [_this _opts] workspaces)
  (-active-clients [_this _opts]
    (->> workspaces (mapcat :workspace/clients))))

(defn clawe-conf [defs]
  (atom {:client/defs defs}))

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

(def journal-client-focused
  {:client/window-title "journal"
   :client/app-name     "Emacs"
   :client/focused      true})

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
  {;; clients
   :client
   {:prefix   :client :schema client/schema
    :spec-gen {:client/focused nil}}

   :jcli
   {:prefix   :jc :schema client/schema
    :spec-gen journal-client}

   :jcli-focused
   {:prefix   :jc-focused :schema client/schema
    :spec-gen journal-client-focused}

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

   :jwsp
   {:prefix   :jw
    :schema   workspace/schema
    :spec-gen (assoc journal-workspace :workspace/clients [])}

   :jwsp+cli
   {:prefix   :jw-jc
    :schema   workspace/schema
    :spec-gen (assoc journal-workspace :workspace/clients [journal-client])}

   :jwsp+cli-focused
   {:prefix   :jw-jc-focused
    :schema   workspace/schema
    :spec-gen (assoc journal-workspace :workspace/clients
                     [journal-client-focused])}

   :myrepo-wsp
   {:prefix   :mrw
    :schema   workspace/schema
    :spec-gen (assoc myrepo-workspace :workspace/clients [])}

   :myrepo-wsp+cli
   {:prefix   :mrw-mrc
    :schema   workspace/schema
    :spec-gen (assoc myrepo-workspace :workspace/clients [myrepo-emacs-client])}

   :myrepo-wsp+jcli
   {:prefix   :mrw-jc
    :schema   workspace/schema
    :spec-gen (assoc myrepo-workspace :workspace/clients [journal-client])}

   :myrepo-wsp+jcli-focused
   {:prefix   :mrw-jc-focused
    :schema   workspace/schema
    :spec-gen (assoc myrepo-workspace :workspace/clients
                     [journal-client-focused])}})

(comment
  (gen-data schema
            {:journal-client    [[1]]
             :journal-workspace [[1]]
             ;; :workspace-with-clients [[1]]
             :workspace         [[1]]}))

(defn gen-workspaces
  "Takes a list of keys from `schema`, returns a single generated map for
  each. Maintains order.

  Used along with `TestWM` - the first workspace is used as the 'current' one."
  [schema-keys]
  ;; NOTE should only be passed workspace keys
  (let [generated
        (->> schema-keys
             (map (fn [k] [k [[1]]]))
             (into {})
             (gen-data schema))]
    (->> schema-keys
         (mapcat (fn [k] (vals (k generated)))))))

(comment
  (gen-workspaces [:jwsp :myrepo-wsp])
  (gen-workspaces [:myrepo-wsp :jwsp])
  (gen-workspaces [:jwsp+cli])
  (gen-workspaces [:jwsp+cli :myrepo-wsp]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; journal toggle
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def journal-def
  {:client/app-names    ["Emacs"]
   :client/window-title "journal"
   ;; adding key just to ease testing, should maybe remove...
   :client/key          "journal"})

(deftest journal-find-client-test
  (testing "no clients or workspaces, not found"
    (sys/with-system
      [wm/*wm* (TestWM. [])
       clawe.config/*config* (clawe-conf {"journal" journal-def})]
      (is (not (toggle/find-client journal-def)))

      (is (= [:create-client journal-def]
             (toggle/determine-toggle-action "journal")))))

  (testing "journal-wsp without journal client, not found"
    (sys/with-system
      [wm/*wm* (TestWM. (gen-workspaces [:jwsp]))
       clawe.config/*config* (clawe-conf {"journal" journal-def})]
      (is (not (toggle/find-client journal-def)))

      (is (= [:create-client journal-def]
             (toggle/determine-toggle-action "journal")))))

  (testing "myrepo with myrepo emacs client, not found"
    (sys/with-system
      [wm/*wm* (TestWM. (gen-workspaces [:myrepo-wsp+cli]))
       clawe.config/*config* (clawe-conf {"journal" journal-def})]
      (is (not (toggle/find-client journal-def)))

      (is (= [:create-client journal-def]
             (toggle/determine-toggle-action "journal")))))

  (testing "journal-wsp current and unfocused journal client, found"
    (sys/with-system
      [wm/*wm* (TestWM. (gen-workspaces [:jwsp+cli]))
       clawe.config/*config* (clawe-conf {"journal" journal-def})]

      ;; find-client
      (let [client (toggle/find-client journal-def)]
        (is client)
        (is (= client (merge journal-client journal-def)))

        ;; in-current-workspace?
        (is (= (toggle/client-in-current-workspace? client)
               (merge journal-client journal-def)))

        (is (= [:focus-client client]
               (toggle/determine-toggle-action "journal"))))))

  (testing "journal-wsp current and focused journal client, found"
    (sys/with-system
      [wm/*wm* (TestWM. (gen-workspaces [:jwsp+cli-focused]))
       clawe.config/*config* (clawe-conf {"journal" journal-def})]

      ;; find-client
      (let [client (toggle/find-client journal-def)]
        (is client)
        (is (= client (merge journal-client-focused journal-def)))

        ;; in-current-workspace?
        (is (= (toggle/client-in-current-workspace? client)
               (merge journal-client-focused journal-def)))

        (is (= [:hide-client client]
               (toggle/determine-toggle-action "journal"))))))

  (testing "journal-wsp with jclient is current, repo-wsp with emacs client, found in current"
    (sys/with-system
      [wm/*wm* (TestWM. (gen-workspaces [:jwsp+cli :myrepo-wsp+cli]))
       clawe.config/*config* (clawe-conf {"journal" journal-def})]

      ;; find-client
      (let [client (toggle/find-client journal-def)]
        (is client)
        (is (= client (merge journal-client journal-def)))

        ;; in-current-workspace?
        (is (toggle/client-in-current-workspace? client))
        (is (= (toggle/client-in-current-workspace? client)
               (merge journal-client journal-def)))

        (is (= [:focus-client client]
               (toggle/determine-toggle-action "journal"))))))

  (testing "journal-wsp with focused jclient, repo-wsp with emacs client, found in current"
    (sys/with-system
      [wm/*wm* (TestWM. (gen-workspaces [:jwsp+cli-focused :myrepo-wsp+cli]))
       clawe.config/*config* (clawe-conf {"journal" journal-def})]

      ;; find-client
      (let [client (toggle/find-client journal-def)]
        (is client)
        (is (= client (merge journal-client-focused journal-def)))

        ;; in-current-workspace?
        (is (toggle/client-in-current-workspace? client))
        (is (= (toggle/client-in-current-workspace? client)
               (merge journal-client-focused journal-def)))

        (is (= [:hide-client client]
               (toggle/determine-toggle-action "journal"))))))

  (testing "journal-wsp with jclient, but repo-wsp is current, found, not in current"
    (sys/with-system
      [wm/*wm* (TestWM. (gen-workspaces [:myrepo-wsp+cli :jwsp+cli]))
       clawe.config/*config* (clawe-conf {"journal" journal-def})]

      ;; find-client
      (let [client (toggle/find-client journal-def)]
        (is client)
        (is (= client (merge journal-client journal-def)))

        ;; in-current-workspace?
        (is (not (toggle/client-in-current-workspace? client)))

        (is (= [:show-client client]
               (toggle/determine-toggle-action "journal"))))))

  (testing "empty journal-wsp, myrepo-wsp is current with unfocused journal-client, found in current"
    (sys/with-system
      [wm/*wm* (TestWM. (gen-workspaces [:myrepo-wsp+jcli :jwsp]))
       clawe.config/*config* (clawe-conf {"journal" journal-def})]

      ;; find-client
      (let [client (toggle/find-client journal-def)]
        (is client)
        (is (= client (merge journal-client journal-def)))

        ;; in-current-workspace?
        (is (toggle/client-in-current-workspace? client))
        (is (= (toggle/client-in-current-workspace? client)
               (merge journal-client journal-def)))

        (is (= [:focus-client client]
               (toggle/determine-toggle-action "journal"))))))

  (testing "empty journal-wsp, myrepo-wsp is current with focused journal-client, found in current"
    (sys/with-system
      [wm/*wm* (TestWM. (gen-workspaces [:myrepo-wsp+jcli-focused :jwsp]))
       clawe.config/*config* (clawe-conf {"journal" journal-def})]

      ;; find-client
      (let [client (toggle/find-client journal-def)]
        (is client)
        (is (= client (merge journal-client-focused journal-def)))

        ;; in-current-workspace?
        (is (toggle/client-in-current-workspace? client))
        (is (= (toggle/client-in-current-workspace? client)
               (merge journal-client-focused journal-def)))

        (is (= [:hide-client client]
               (toggle/determine-toggle-action "journal"))))))

  (testing "empty journal-wsp is current, myrepo-wsp with journal-client, found, not in current"
    (sys/with-system
      [wm/*wm* (TestWM. (gen-workspaces [:jwsp :myrepo-wsp+jcli]))
       clawe.config/*config* (clawe-conf {"journal" journal-def})]

      ;; find-client
      (let [client (toggle/find-client journal-def)]
        (is client)
        (is (= client (merge journal-client journal-def)))

        ;; in-current-workspace?
        (is (not (toggle/client-in-current-workspace? client)))

        (is (= [:show-client client]
               (toggle/determine-toggle-action "journal")))))))
