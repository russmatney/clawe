(ns doctor.ui.hooks.use-reaction
  (:require ["use-sync-external-store/with-selector" :refer [useSyncExternalStoreWithSelector]]
            [reagent.impl.component :as impl.component]
            [reagent.ratom :as ratom]
            [scheduler]
            [uix.core :as uix]))

;; bunch of noise from uix docs to make ratoms work
;; https://github.com/pitch-io/uix/blob/master/docs/interop-with-reagent.md#syncing-with-ratoms-and-re-frame

(defn- cleanup-ref [ref]
  (when-let [^ratom/Reaction temp-ref (aget ref "__rat")]
    (remove-watch ref temp-ref)
    (when-let [watching (.-watching temp-ref)]
      (set! (.-watching temp-ref)
            (.filter watching #(not (identical? ref %)))))))

(defn- use-batched-subscribe
  "Takes an atom-like ref type and returns a function
  that adds change listeners to the ref"
  [^js ref]
  (uix/use-callback
    (fn [listener]
      ;; Adding an atom holding a set of listeners on a ref
      (let [listeners (or (.-react-listeners ref) (atom #{}))]
        (set! (.-react-listeners ref) listeners)
        (swap! listeners conj listener))
      (fn []
        (let [listeners (.-react-listeners ref)]
          (swap! listeners disj listener)
          ;; When the last listener was removed,
          ;; remove batched updates listener from the ref
          (when (empty? @listeners)
            (cleanup-ref ref)
            (set! (.-react-listeners ref) nil)))))
    [ref]))

(defn- use-sync-external-store [subscribe get-snapshot]
  (useSyncExternalStoreWithSelector
    subscribe
    get-snapshot
    nil ;; getServerSnapshot, only needed for SSR
    identity ;; selector, not using, just returning the value itself
    =)) ;; value equality check

(defn- run-reaction [^js ref]
  (let [key       "__rat"
        ^js rat   (aget ref key)
        on-change (fn [_]
                    ;; When the ref is updated, schedule all listeners in a batch
                    (when-let [listeners (.-react-listeners ref)]
                      (scheduler/unstable_scheduleCallback scheduler/unstable_ImmediatePriority
                                                           #(doseq [listener @listeners]
                                                              (listener)))))]
    (if (nil? rat)
      (ratom/run-in-reaction
        #(-deref ref) ref key on-change {:no-cache true})
      (._run rat false))))

;; Public API

(defn use-reaction
  "Takes Reagent's Reaction,
  subscribes UI component to changes in the reaction
  and returns current state value of the reaction"
  [reaction]
  (if impl.component/*current-component*
    ;; in case when the reaction runs in Reagent component
    ;; just deref it and let Reagent handle everything
    @reaction
    ;; otherwise manage subscription via hooks
    (let [subscribe    (use-batched-subscribe reaction)
          get-snapshot (uix/use-callback #(run-reaction reaction) [reaction])]
      (use-sync-external-store subscribe get-snapshot))))
