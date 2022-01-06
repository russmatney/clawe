(ns ralphie.re)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; url parsing
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn re-get
  "Returns the first match for a passed regex and string.
  Shouldn't this function exist?
  "
  [pat s]
  (when s
    (let [parts (re-find pat s)]
      (when (> (count parts) 1)
        (second parts)))))

(def url-regex
  "https://stackoverflow.com/questions/3809401/what-is-a-good-regular-expression-to-match-a-url"
  #"(https?:[/][/](?:www\.|(?!www))[a-zA-Z0-9][a-zA-Z0-9-]+[a-zA-Z0-9]\.[^\s]{2,}|www\.[a-zA-Z0-9][a-zA-Z0-9-]+[a-zA-Z0-9]\.[^\s]{2,}|https?:[/][/](?:www\.|(?!www))[a-zA-Z0-9]+\.[^\s]{2,}|www\.[a-zA-Z0-9]+\.[^\s]{2,})")

(defn ->url [s]
  (re-get url-regex s))

(def repo-id-regex
  #"github.com/([a-zA-Z0-9_-[.]]+/[a-zA-Z0-9_-[.]]+)")

(defn url->repo-id [s]
  (re-get repo-id-regex s))

(defn ->repo-id
  "Attempts to a return a repo-id from the passed string.
  repo-ids have the shape `group-name/repo-name`, and can presumably be
  git-cloned.

  Maybe parse a list of potential candidates, and expose all of them?
  ;; TODO consume in repos.core?
  "
  [s]
  (when s
    (let [s (if  (coll? s) (apply str s) s)]
      (if-not (string? s)
        (do
          (println "not-string passed to ->repo-id:" s)
          (println "type: " (type s)))
        (let [url     (->url s)
              repo-id (when url (url->repo-id url))
              repo-id (or repo-id
                          (re-get #"(?!com)?([a-zA-Z0-9_-[.]]+/[a-zA-Z0-9_-[.]]+)" s))]
          repo-id)))))
(comment
  (->repo-id "https://github.com/kikito/tween.lua")
  (->repo-id "https://github.com/kikito.user/tween.lua")
  (->repo-id "kikito/tween.lua")
  (->repo-id "kikito.blah/tween.lua")
  )
