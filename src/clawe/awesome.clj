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

(defrecord AwesomeWM []
  ClaweWM
  (-current-workspaces [_this opts]
    (->>
      ;; TODO tags-only (no clients, maybe a bit faster?)
      ;; TODO filter on current tags (less to serialize)
      (awm/fetch-tags (merge {:include-clients false :only-current true} opts))
      (filter :awesome.tag/selected)
      (map (fn [tag]
             (cond-> tag
               (and (:include-clients opts) (:awesome/clients tag))
               (-> (assoc :workspace/clients
                          (->> tag :awesome/clients
                               (map awesome-client->clawe-client ))))

               true tag->wsp)))))

  (-active-workspaces [_this opts]
    (->>
      (awm/fetch-tags opts)
      (map tag->wsp))))
