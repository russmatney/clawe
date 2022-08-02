(ns clawe.awesome
  (:require
   [clawe.wm.protocol :refer [ClaweWM]]
   [ralphie.awesome :as awm]))


(defn awesome-client->clawe-client [client]
  (-> client
      (assoc :client/window-title (:awesome.client/name client))
      (assoc :client/app-name (:awesome.client/class client))
      (assoc :client/focused (:awesome.client/focused client))))

(defn tag->wsp [tag]
  (-> tag
      (assoc :workspace/index (:awesome.tag/index tag))
      (assoc :workspace/title (:awesome.tag/name tag))) )

(defrecord Awesome []
  ClaweWM
  (-current-workspaces [_this opts]
    (->>
      ;; TODO re-use pre-fetched clients if passed?
      ;; TODO tags-only (no clients, maybe a bit faster?)
      ;; TODO filter on current tags (less to serialize)
      (awm/fetch-tags (merge {:include-clients false :only-current true} opts))
      (filter :awesome.tag/selected)
      (map (fn [tag]
             (cond-> tag
               (and (or (:include-clients opts)
                        (seq (:prefetched-clients opts)))
                    (seq (:awesome.tag/clients tag)))
               (-> (assoc :workspace/clients
                          (->> tag :awesome.tag/clients
                               (map awesome-client->clawe-client)))
                   (dissoc :awesome.tag/clients))

               true tag->wsp)))))

  (-active-workspaces [_this opts]
    (->>
      (awm/fetch-tags opts)
      (map tag->wsp)))

  (-ensure-workspace [_this _opts workspace-title]
    (awm/ensure-tag workspace-title))

  (-focus-workspace [_this _opts workspace]
    (let [workspace-title (if (string? workspace)
                            workspace (:workspace/title workspace))]
      (awm/ensure-tag workspace-title)
      (awm/focus-tag! workspace-title)))

  (-close-client [_this _opts c]
    (awm/close-client c))

  (-all-clients [_this _opts]
    (->> (awm/all-clients)
         (map awesome-client->clawe-client)))

  (-focus-client [_this opts client]
    (awm/focus-client
      {:center?   (:float-and-center opts)
       :float?    (:float-and-center opts)
       :bury-all? false}
      ;; TODO consider :client/window-id client attr
      (:awesome.client/window client)))

  (-move-client-to-workspace [this opts c wsp]
    (let [workspace-title (if (string? wsp) wsp (:workspace/title wsp))]
      (when (:ensure-workspace opts)
        (clawe.wm.protocol/-ensure-workspace this nil workspace-title))
      ;; TODO consider :client/window-id client attr
      (awm/move-client-to-tag (:awesome.client/window c) workspace-title))))

(comment
  (clawe.wm.protocol/-current-workspaces
    (Awesome.) {:include-clients true})
  )
