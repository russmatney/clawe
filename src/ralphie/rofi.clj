(ns ralphie.rofi
  (:require
   [babashka.process :refer [$ check]]
   [clojure.string :as string]
   [ralphie.config :as config]
   [ralphie.cache :as cache]
   [babashka.fs :as fs]

   [timer :as timer]
   ))

(timer/print-since "ralphie.rofi ns loaded")

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; mru
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn mru-cache-file [cache-id]
  (cache/cache-file (str "mru-" cache-id)))

(defn read-mru-cache [{:keys [cache-id file]}]
  (when (or cache-id file)
    (let [file (or file (mru-cache-file cache-id))]
      (when (fs/exists? file)
        (->>
          (slurp file)
          (string/split-lines)
          distinct
          (into []))))))

(defn update-mru-cache [{:keys [cache-id label]}]
  (when cache-id
    (println "writing to mru cache for id:" cache-id)
    (let [file  (mru-cache-file cache-id)
          cache (or (read-mru-cache {:file file}) #{})]

      (spit file
            (->>
              (concat [label] cache)
              distinct
              (string/join "\n"))))))

(comment
  (read-mru-cache {:cache-id "hi"})
  (update-mru-cache {:cache-id "hi" :label "some command"})
  (update-mru-cache {:cache-id "hi" :label "another command"})
  (update-mru-cache {:cache-id "hi" :label "some other command"}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; rofi-general
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn escape-rofi-label [label]
  (-> label
      (string/escape {\& "&amp;"})
      (string/replace #"\|" "-")))

(defn build-label
  "Builds a better looking label when :rofi/label and :rofi/description are set.

  If no :rofi/label, or if the existing one starts with a `<` (i.e. `<span>`),
  it is left as is."
  [{:rofi/keys [label description]
    :keys      [osx?]
    :as        x}]
  (let [osx? (if (nil? osx?) (config/osx?) osx?)]
    (cond
      osx?                  x
      (not label)           x
      (#{\<} (first label)) x
      :else
      (assoc x :rofi/label
             (str "<span>" label "</span>"
                  (when description (str " <span color='gray'>" description "</span>")))))))

(comment
  (build-label {:rofi/label "hi"})
  (build-label {:rofi/label "hi" :rofi/description "desc"}))

(defn xs->input-string [{:keys [mru-cache-id sep]} xs]
  (let [osx?  (config/osx?)
        maps? (-> xs first map?)
        xs    (if maps? (->> xs (map #(build-label (assoc % :osx? osx?)))) xs)

        labels (if maps? (->> xs
                              (map (some-fn :label :rofi/label))
                              (map escape-rofi-label))
                   xs)

        labels-set (into #{} labels)

        mru-cache     (->> (read-mru-cache {:cache-id mru-cache-id})
                           (filter labels-set))
        mru-cache-set (into #{} mru-cache)

        labels (->> labels
                    (remove mru-cache-set)
                    (concat mru-cache))]
    (string/join sep labels)))

;; TODO restore mru sorting in the wake of this memoization
;; maybe with a write-ahead cache of this function?
(def xs->input-string-memoized (memoize xs->input-string))

;; TODO Tests for this,especially that ensure the result is returned
(defn rofi
  "Expects `xs` to be a coll of maps with a `:label` key.
  `msg` is displayed to the user.

  Upon selection, if the user-input matches a label, that `x`
  is selected and retuned.

  If a no match is found, the user input is returned.
  If on-select is passed, it is called with the selected input.

  Supports :require-match? in `opts`.
  "
  ;; TODO move opts and xs over to :rofi/prefixed keys
  ;; TODO support `:rofi/tag` and `:rofi/tags` for including search terms (like "clone")
  ([opts]
   (cond (map? opts)
         (rofi opts ((some-fn :xs :file) opts))

         (coll? opts)
         (rofi {} opts)))
  ([{:keys [msg message on-select require-match? mru-cache-id label-input->cache] :as opts} xs-or-file]
   ;; (println "Rofi called with" (count xs) "xs.")
   (let [xs         (when (coll? xs-or-file) xs-or-file)
         input-file (when (string? xs-or-file) xs-or-file)]
     (timer/print-since (str "rofi/rofi with " (count xs) "xs or file " input-file))

     (let [msg (or msg message)
           sep (if (config/osx?) "\n" "|")
           rofi-input
           (cond (seq xs)
                 (let [label-input (xs->input-string-memoized (assoc opts :sep sep) xs)]
                   ;; invoke this callback to update calling rofi caches
                   (when label-input->cache
                     (label-input->cache label-input))
                   label-input)

                 (fs/exists? input-file) input-file)

           selected-label
           (some->
             (if (config/osx?)
               ^{:in rofi-input :out :string :err :string}
               ($ choose -u)

               ;; TODO could move to async piping of entries to rofi
               ^{:out :string :err :string :in rofi-input}
               ($ rofi -i
                  ~(if require-match? "-no-custom" "")
                  -sep ~sep
                  ;; ~(when input-file (str "-input " input-file))
                  -markup-rows
                  -normal-window ;; NOTE may want this to be optional
                  ;; -eh 2 ;; row height
                  ;; -dynamic
                  ;; -no-fixed-num-lines
                  -dmenu -mesg ~msg -p *))

             ((fn [proc]
                ;; check for type of error
                (let [res @proc]
                  (cond
                    (zero? (:exit res))
                    (-> res :out string/trim)

                    ;; TODO determine if simple nothing-selected or actual rofi error
                    (= 1 (:exit res))
                    (do
                      (println "\nRofi Nothing Selected (or Error)")
                      nil)

                    :else
                    (do
                      (println res)
                      (check proc)))))))]
       (update-mru-cache {:cache-id mru-cache-id :label selected-label})

       (when (seq selected-label)
         ;; TODO use index-by, or just make a map
         (let [->label    (fn [x]
                            (-> x build-label ((some-fn :label :rofi/label)) escape-rofi-label))
               ;; xs-by-label (->> xs (group-by ->label) (map (fn [[k v]] [k (first v)])) (into {}))
               ;; is this shortest-match actually important?
               selected-x (if (and (seq xs) (map? (first xs)))
                            (let [matches
                                  (->> xs
                                       (filter (fn [x]
                                                 (-> x ->label
                                                     (string/starts-with? selected-label)))))]
                              (some->> matches
                                       ;; select the shortest match
                                       (sort-by (comp count ->label) <)
                                       first))
                            selected-label)]
           (if selected-x
             (if-let [on-select (or ((some-fn :rofi/on-select :on-select)
                                     selected-x) on-select)]
               (do
                 ;; TODO support zero-arity on-select here
                 (println "on-select found" on-select)
                 (println "argslists" (-> on-select meta :arglists))
                 (on-select selected-x))
               selected-x)
             selected-label)))))))


(comment
  ;; TODO convert to tests
  (->
    ^{:in "11  iiii\n22 IIIIII\n33 33333"}
    ($ rofi -i -dmenu -mesg "Select bookmark to open" -sync -p *)
    check
    :out
    slurp)

  (rofi {:msg "test"}
        [{:rofi/label "Kill client: Slack | news-and-articles | clojurians - Slack - slack"}])
  (rofi
    {:msg "message"}
    [{:label "iiiiiii" :url "wut i did not select this"}
     {:label "iii" :url "iii, just like you said"}])

  (rofi
    {:msg "message"}
    [{:label "iii" :url "urlllll"}
     {:label "333" :url "something"}
     {:label "jkjkjkjkjkjkjkjkjkjkjkjkjkjkjk" :url "w/e"}
     {:label "xxxxxxxx" :url "--------------"}])

  (rofi
    {:require-match? true
     :msg            "message"
     :cache-id       "test-cache"}
    [{:label "iii" :url "urlllll"}
     {:label "333" :url "something"}
     {:label "jkjkjkjkjkjkjkjkjkjkjkjkjkjkjk" :url "w/e"}
     {:label "xxxxxxxx" :url "--------------"}])

  (->
    ^{:in  "11  iiii\n22 IIIIII\n33 33333"
      :out :string
      }
    ($ choose)
    check
    :out))
