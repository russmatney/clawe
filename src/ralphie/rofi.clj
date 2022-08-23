(ns ralphie.rofi
  (:require
   [babashka.process :refer [$ check]]
   [clojure.string :as string]
   [ralphie.awesome :as awm]
   [ralphie.notify :as notify]))

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
  [{:rofi/keys [label description] :as x}]
  (cond
    notify/is-mac?        x
    (not label)           x
    (#{\<} (first label)) x
    :else
    (assoc x :rofi/label
           (str "<span>" label "</span>"
                (when description (str " <span color='gray'>" description "</span>"))))))

(comment
  (build-label {:rofi/label "hi"})
  (build-label {:rofi/label "hi" :rofi/description "desc"})
  )

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
  ([opts] (rofi opts (:xs opts)))
  ([{:keys [msg message on-select require-match?]} xs]
   (println "Rofi called with" (count xs) "xs.")

   ;; push any floating windows into the view
   (when-not notify/is-mac?
     (awm/awm-cli
       {:quiet? true}
       (str
         ;; set all ontops false
         "for c in awful.client.iterate(function (c) return c.ontop end) do\n"
         "c.ontop = false; "
         "c.floating = false; "
         "end;")))

   (let [maps?  (-> xs first map?)
         xs     (if maps? (->> xs (map build-label)) xs)
         labels (if maps? (->> xs
                               (map (some-fn :label :rofi/label))
                               (map escape-rofi-label))
                    xs)
         msg    (or msg message)

         sep "|"

         selected-label
         (some->

           (if notify/is-mac?
             ^{:in  (string/join "\n" labels)
               :out :string}
             ($ choose -u)

             ^{:in  (string/join sep labels)
               :out :string}
             ($ rofi -i
                ~(if require-match? "-no-custom" "")
                -sep ~sep
                -markup-rows
                -normal-window ;; NOTE may want this to be optional
                ;; -eh 2 ;; row height
                ;; -dynamic
                ;; -no-fixed-num-lines
                -dmenu -mesg ~msg -sync -p *)
             )

           ((fn [proc]
              ;; check for type of error
              (let [res @proc]
                (cond
                  (zero? (:exit res))
                  (-> res :out string/trim)

                  ;; TODO determine if simple nothing-selected or actual rofi error
                  (= 1 (:exit res))
                  (do
                    (println "Rofi Nothing Selected (or Error)")
                    nil)

                  :else
                  (do
                    (println res)
                    (check proc)))))))]
     (when (seq selected-label)
       ;; TODO use index-by, or just make a map
       (let [selected-x (if maps?
                          (->> xs
                               (filter (fn [x]
                                         (-> (or (:rofi/label x) (:label x))
                                             escape-rofi-label
                                             (string/starts-with? selected-label))))
                               first)
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
           selected-label))))))


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
    [{:label "iii" :url "urlllll"}
     {:label "333" :url "something"}
     {:label "jkjkjkjkjkjkjkjkjkjkjkjkjkjkjk" :url "w/e"}
     {:label "xxxxxxxx" :url "--------------"}])

  (rofi
    {:require-match? true
     :msg            "message"}
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
