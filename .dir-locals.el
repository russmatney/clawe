;;; Directory Local Variables
;;; For more information see (info "(emacs) Directory Variables")

((clojure-mode
  (cider-clojure-cli-aliases . "dev")
  (cider-known-endpoints
   . (("expo-clj-server" "localhost" "4442")
      ("expo-cljs-shadow" "localhost" "3335")
      ("doctor-clj-server" "localhost" "3336")
      ("doctor-cljs-shadow" "localhost" "3335")))
  (cider-clojure-cli-global-options . "-A:doctor-deps:dev:test")
  (cider-preferred-build-tool . shadow-cljs)
  (cider-jack-in-default . shadow-cljs)
  (cider-default-cljs-repl . shadow)
  (cider-shadow-default-options . ":doctor-deps")
  (cider-shadow-cljs-global-options . "-A:doctor-deps:dev")
  (company-css-classes-filepath . "~/russmatney/clawe/public/css/main-built.css"))
 (clojurec-mode
  (cider-known-endpoints
   . (("expo-clj-server" "localhost" "4442")
      ("expo-cljs-shadow" "localhost" "3335")
      ("doctor-clj-server" "localhost" "3336")
      ("doctor-cljs-shadow" "localhost" "3335")))
  (cider-clojure-cli-global-options . "-A:doctor-deps:dev:test")
  (cider-preferred-build-tool . shadow-cljs)
  (cider-jack-in-default . shadow-cljs)
  (cider-default-cljs-repl . shadow)
  (cider-shadow-default-options . ":doctor-app")
  (cider-shadow-cljs-global-options . "-A:doctor-deps:dev")
  (company-css-classes-filepath . "~/russmatney/clawe/public/css/main-built.css"))
 (clojurescript-mode
  (cider-known-endpoints
   . (("expo-cljs-shadow" "localhost" "3335")
      ("doctor-cljs-shadow" "localhost" "3335")))
  (cider-clojure-cli-global-options . "-A:doctor-deps:dev:test")
  (cider-preferred-build-tool . shadow-cljs)
  (cider-jack-in-default . shadow-cljs)
  (cider-default-cljs-repl . shadow)
  (cider-shadow-default-options . ":expo-app") ;; ":doctor-app"?
  (cider-shadow-cljs-global-options . "-A:doctor-deps:dev")
  (company-css-classes-filepath . "~/russmatney/clawe/public/css/main-built.css")))
