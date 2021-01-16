
(local
 send-string-to-client
 (fn [s c]
   (local old_c client.focus)
   (set client.focus c)
   (for [i 1 #s]
     (let [char (s:sub i i)]
       (root.fake_input "key_press" char)
       (root.fake_input "key_release" char))) (set client.focus old_c)
   ))

(local
 hostname
 (fn []
   "ty to https://gist.github.com/h1k3r/089d43771bdf811eefe8 for this."
   (let [f (io.popen "/bin/hostname")
         hostname (or (f:read "*a") "")]
     (f:close)
     (string.gsub hostname "\n$" ""))))

(fn is-vader []
  (= (hostname) "vader"))

{: send-string-to-client
 : hostname
 : is-vader}
