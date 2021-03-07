(ns clawe.scratchpad
  (:require
   [clawe.workspaces :as workspaces]
   [clawe.awesome :as awm]
   [ralph.defcom :refer [defcom]]
   [ralphie.emacs :as emacs]
   [ralphie.awesome :as r.awm]
   [ralphie.notify :as notify]
   [babashka.process :refer [process check]]
   [clojure.string :as string]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Create client
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn create-client
  "Creates clients for a given workspace"
  [wsp]
  (let [wsp       (cond
                    (nil? wsp)    (workspaces/current-workspace)
                    (string? wsp) (workspaces/for-name wsp)
                    :else         wsp)
        exec      (some wsp [:workspace/exec])
        init-file (some wsp [:workspace/initial-file])
        first-client
        (or (:org.prop/first-client wsp)
            (cond
              exec      :create/exec
              init-file :create/emacs
              :else     (do
                          (notify/notify
                            "Could not determine first client for wsp" wsp)
                          :create/none)))]
    (println first-client init-file)

    (case first-client
      :create/emacs (emacs/open wsp)
      :create/exec  (do
                      (notify/notify "Starting new client" exec)
                      (-> exec
                          (string/split #" ")
                          process
                          check)
                      (notify/notify "New client started" exec))
      :create/none
      ;; NOTE maybe detect a readme in directories as well
      (notify/notify "New workspace has no default client."
                     "Try setting :initial-file or :exec"))))

(comment
  (create-client "journal")
  (create-client "org-crud")
  (create-client "yodo-app")
  (create-client "web"))

(defn create-client-handler
  ([] (create-client-handler nil nil))
  ([_config parsed]
   (if-let [arg (some-> parsed :arguments first)]
     (create-client arg)
     (create-client nil))))

(defcom create-client-cmd
  {:name          "create-client"
   :one-line-desc "Creates clients for the passed workspace name"
   :description   [""]
   :handler       create-client-handler})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; toggle scratchpad
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn ontop-and-focused [client]
  ;; set all ontops false
  ;; set all floating false
  ;; set this client ontop true
  ;; set this client floating true
  ;; focus this client

  (awm/awm-cli
    {:parse? false
     :pp?    false}
    (str
      ;; set all ontops false
      "for c in awful.client.iterate(function (c) return c.ontop end) do\n"
      "c.ontop = false; "
      "c.floating = false; "
      "end;"

      ;; set this client ontop true, and focus it
      "for c in awful.client.iterate(function (c) return c.window == "
      (-> client :window awm/->lua-arg)
      " end) do\n"
      "c.ontop = true; "
      "c.floating = true; "

      ;; center and resize
      ;; "local f = (awful.placement.scale + awful.placement.centered);"
      ;; "f(c, {honor_padding=true, honor_workarea=true, to_percent= 0.75})"

      ;; just center
      "local f = awful.placement.centered;"
      "f(c);"
      "_G.client.focus = c;"
      "end; ")))

(defn toggle-scratchpad
  ([] (toggle-scratchpad (workspaces/current-workspace)))
  ([wsp]
   (let [wsp      (cond
                    (nil? wsp)    (workspaces/current-workspace)
                    (string? wsp) (workspaces/for-name wsp)
                    :else         wsp)
         wsp-name (-> wsp :org/name)
         tag      (-> wsp :awesome/tag)
         client   (some-> tag :clients first)]
     (cond
       ;; "found selected tag, client for:" wsp-name
       (and tag client (:selected tag))
       (if (:ontop client)
         ;; TODO also set client ontop false ?
         (r.awm/toggle-tag wsp-name)
         (ontop-and-focused client))

       ;; "found unselected tag, client for:" wsp-name
       (and tag client (not (:selected tag)))
       (do
         (r.awm/toggle-tag wsp-name)
         (ontop-and-focused client))

       ;; tag exists, no client
       (and tag (not client))
       (create-client wsp)

       ;; tag does not exist, presumably no client either
       (not tag)
       (do
         (r.awm/create-tag! wsp-name)
         (r.awm/toggle-tag wsp-name)
         (create-client wsp))))))

(comment
  (println "hi")
  (toggle-scratchpad "journal")

  (toggle-scratchpad "notes")
  (toggle-scratchpad "web")
  (workspaces/for-name "web")
  )

(defn toggle-scratchpad-handler
  ([] (toggle-scratchpad-handler nil nil))
  ([_config parsed]
   (if-let [arg (some-> parsed :arguments first)]
     (toggle-scratchpad arg)
     (toggle-scratchpad nil))))

(defcom toggle-scratchpad-cmd
  {:name          "toggle-scratchpad"
   :one-line-desc "Toggles the passed scratchpad."
   :description   [""]
   :handler       toggle-scratchpad-handler})

(comment)
