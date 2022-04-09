(ns defthing.core)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; registry
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; global registry for your 'things'
(defonce registry* (atom {}))

(defn clear-registry
  "Clears the entire registry.
  Could be extended to clear only one type from the registry,
  but no use-case has a-risen yet."
  []
  (reset! registry* {}))

(defn add-thing
  "Adds an x to the registry using the `thing-key`.

  Not expected to be used publicly - consumed by the `defthing` helper fn."
  ([x]
   (if (:type x)
     (add-thing (:type x) x)
     [:error :missing-type]))
  ([thing-key x]
   (swap! registry* assoc-in
          [thing-key (::registry-key x)] x)
   ;; return x
   x))

(defn list-things
  "Helper for building a `list-thing` function."
  [thing-key]
  (vals (get @registry* thing-key)))

(defn get-thing
  "Helper for building a getter function for a specific defthing type.
  Expects to be passed the `thing-key` and a predicate that is run over all
  registered things for that key."
  [thing-key pred]
  (some->> (get @registry* thing-key)
           vals
           (filter pred)
           first))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; defthing macro helper
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- eval-xorf
  "x is the current map of data for the thing being def-ed.

  xorf is one of:
  - a map to be merged into x
  - a function to be called with x and then merged into the original.
  - a string to appended to the thing's `:doc` string.

  Returns the merged/updated x.

  The `seq?` `list?` may need to expand/get smarter at some point.
  For now it seems to work for both anonymous and named functions."
  [x xorf]
  (let [merge-x
        (fn [x v]
          (cond
            (map? v)    (merge x v)
            (string? v) (update x :doc #(str % (when % "\n") v))
            :else       x))]
    (cond
      (or
        (seq? xorf)
        (list? xorf)
        (symbol? xorf)) (merge-x x ((eval xorf) x))
      (or
        (map? xorf)
        (string? xorf)) (merge-x x xorf)
      :else             (do (println "unexpected xorf type!")
                            (println "type" (type xorf))
                            (println "xorf" xorf)
                            (println "x" x)
                            x))))

(comment
  (assert (= {:yo :yo}
             (reduce eval-xorf [{} 'println '#(assoc % :yo :yo) 'println])))
  (assert (= {:meee :bee}
             (eval-xorf {} '#(assoc % :meee :bee))))

  (assert (= {::doc "my\ndocs"
              :and  :keys}
             (reduce eval-xorf [{} '"my" '{:and :keys} "docs"]))))

(defn initial-thing
  "Helper for creating an initial thing.
  Sets some important keys.

  - `::registry-key` is used as the unique key in the defthing registry.
  - `:name` is required to support the current `get-x` helpers.

  The others may not be necessary, but don't cost much, so, meh.
  "
  [thing-key thing-sym]
  {::registry-key (keyword (str *ns*) (-> thing-sym name))
   :name          (-> thing-sym name)
   :type          thing-key
   :ns            (str *ns*)})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public defthing macro helper
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; TODO rename thing-key to `thing-type`, thing-sym to `thing-name`
(defn defthing
  "Creates a map defined as the passed symbol.

  Provides a convenient builder pattern that composes \"xorfs\",
  which is a made up word that refers to a union of a few different things:

  - a map (that will be merged into the map being constructed)
  - a function (that will be called on the built up x, the result of which is
    then merged)
  - a string (that currently appends a newline to a :doc key.)

  The resulting map is automatically assigned a few key-vals:
  - :type - `thing-key`, passed to defthing
  - :name - `thing-sym`, a string version of the def-ed symbol (`title` or `name`)
  - :ns   - the namespace the thing was defed in

  Ex:
  (defmacro defworkspace [name & args]
    (apply defthing/defthing :clawe/workspaces name args))

  (defworkspace my-web-workspace
    \"Definition of my-web-workspace\"
    {:workspace/title \"browser\"}
    (fn [x] (assoc x :some/key \"and value\")))

  See comment below for more example usage, or the defs/workspaces namespace
  for a some real-world examples and helpers.

  TODO consider partitioning xorfs with a runtime/macro-time eval,
  like in emacs use-package.
  "
  ([thing-key thing-sym] (defthing thing-key thing-sym {}))
  ([thing-key thing-sym & xorfs]
   (let [x (->> (concat [(initial-thing thing-key thing-sym)] xorfs)
                (reduce eval-xorf))]
     `(do
        (def ~thing-sym ~x)
        (add-thing ~thing-key ~x)
        ~x))))
