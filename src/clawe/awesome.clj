(ns clawe.awesome
  (:require
   [clawe.wm.protocol :refer [ClaweWM]]
   [ralphie.awesome :as awm]
   [clawe.workspace :as workspace]))


(defn awesome-client->clawe-client [client]
  (-> client
      (assoc :client/window-title (:awesome.client/name client))
      (assoc :client/app-name (:awesome.client/class client))
      (assoc :client/focused (:awesome.client/focused client))))

(defn tag->wsp [tag]
  (-> tag
      (assoc :workspace/index (:awesome.tag/index tag))
      (assoc :workspace/title (:awesome.tag/name tag))

      ;; NOTE the `tag` may not include clients
      (assoc :workspace/clients
             (->> tag :awesome.tag/clients
                  (map awesome-client->clawe-client)))
      (dissoc :awesome.tag/clients)))

(defn attach-clients [clients tag]
  (cond
    (not clients) ;; no prefetched passed
    tag

    (seq (:workspace/clients tag)) ;; already have clients
    tag ;; return the tag

    :else
    (assoc tag :workspace/clients ;; attach any matching clients
           (->> clients
                (filter (comp #{(:awesome.tag/name tag)} :awesome.client/tag))))))

(defrecord Awesome []
  ClaweWM

  ;; workspaces

  (-current-workspaces [_this opts]
    (->>
      (awm/fetch-tags (merge {:only-current true} opts))
      (map tag->wsp)
      (map (partial attach-clients (:prefetched-clients opts)))))

  (-active-workspaces [_this opts]
    (->>
      (awm/fetch-tags opts)
      (map tag->wsp)
      (map (partial attach-clients (:prefetched-clients opts)))))

  (-create-workspace [_this _opts workspace-title]
    (awm/ensure-tag workspace-title))

  (-focus-workspace [_this _opts workspace]
    (let [workspace-title (if (string? workspace)
                            workspace (:workspace/title workspace))]
      (awm/ensure-tag workspace-title)
      (awm/focus-tag! workspace-title)))

  (-fetch-workspace [_this opts workspace-title]
    (some->>
      (awm/fetch-tags {:tag-names #{workspace-title}})
      (filter (comp #{workspace-title} :awesome.tag/name))
      first
      tag->wsp
      (attach-clients (:prefetched-clients opts))))

  (-swap-workspaces-by-index [_this a b]
    (awm/swap-tags-by-index a b))

  (-drag-workspace [_this dir]
    (awm/drag-workspace dir))

  (-delete-workspace [_this workspace]
    (awm/delete-tag! (:workspace/title workspace)))

  ;; clients

  (-close-client [_this _opts c]
    (awm/close-client c))

  (-active-clients [_this _opts]
    (->> (awm/all-clients)
         (map awesome-client->clawe-client)))

  (-focus-client [_this opts client]
    (awm/focus-client
      {:center?   (:float-and-center opts)
       :float?    (:float-and-center opts)
       :bury-all? true}
      ;; TODO consider :client/window-id client attr
      (:awesome.client/window client)))

  (-move-client-to-workspace [this opts c wsp]
    (let [workspace-title (if (string? wsp) wsp (:workspace/title wsp))]
      (when (:ensure-workspace opts)
        (clawe.wm.protocol/-create-workspace this nil workspace-title))
      ;; TODO consider :client/window-id client attr
      (awm/move-client-to-tag (:awesome.client/window c) workspace-title))))

(comment
  (clawe.wm.protocol/-fetch-workspace
    (Awesome.) nil "dontexist")

  (clawe.wm.protocol/-current-workspaces
    (Awesome.) {:include-clients true})

  (awm/fetch-tags {:include-clients true
                   :only-current    true})

  (->>
    (clawe.wm.protocol/-current-workspaces
      (Awesome.)
      {:prefetched-clients
       (clawe.wm.protocol/-active-clients (Awesome.) nil)})
    (map workspace/strip))

  (clawe.wm.protocol/-fetch-workspace
    (Awesome.) nil "dontexist"))
