;;; Directory Local Variables
;;; For more information see (info "(emacs) Directory Variables")

((clojure-mode
  (cider-clojure-cli-aliases . "dev")
  (cider-known-endpoints
   . (("doctor-clj-server" "localhost" "3336")
      ("doctor-cljs-shadow" "localhost" "3335")))
  (cider-clojure-cli-global-options . "-A:full-stack-deps:dev:test")
  (cider-preferred-build-tool . shadow-cljs)
  (cider-jack-in-default . shadow-cljs)
  (cider-default-cljs-repl . shadow)
  (cider-shadow-default-options . ":full-stack-deps")
  (cider-shadow-cljs-global-options . "-A:full-stack-deps:dev")
  (company-css-classes-filepath . "~/russmatney/clawe/public/css/main-built.css"))
 (clojurec-mode
  (cider-known-endpoints
   . (("doctor-clj-server" "localhost" "3336")
      ("doctor-cljs-shadow" "localhost" "3335")))
  (cider-clojure-cli-global-options . "-A:full-stack-deps:dev:test")
  (cider-preferred-build-tool . shadow-cljs)
  (cider-jack-in-default . shadow-cljs)
  (cider-default-cljs-repl . shadow)
  (cider-shadow-default-options . ":doctor-app")
  (cider-shadow-cljs-global-options . "-A:full-stack-deps:dev")
  (company-css-classes-filepath . "~/russmatney/clawe/public/css/main-built.css"))
 (clojurescript-mode
  (cider-known-endpoints
   . (("doctor-cljs-shadow" "localhost" "3335")))
  (cider-clojure-cli-global-options . "-A:full-stack-deps:dev:test")
  (cider-preferred-build-tool . shadow-cljs)
  (cider-jack-in-default . shadow-cljs)
  (cider-default-cljs-repl . shadow)
  (cider-shadow-watched-builds . (":doctor-app" ":doctor-test"))
  ;; (cider-shadow-default-options . ":doctor-app")
  (cider-shadow-default-options . ":doctor-app")
  (cider-shadow-cljs-global-options . "-A:full-stack-deps:dev")
  (company-css-classes-filepath . "~/russmatney/clawe/public/css/main-built.css")))
