(local awful (require "awful"))

(local
 send_string_to_client
 (fn [s c]
   (local old_c client.focus)
   (set client.focus c)
   (for [i 1 #s]
     (let [char (s:sub i i)]
       (root.fake_input "key_press" char)
       (root.fake_input "key_release" char)))
   (set client.focus old_c)))

(local
 hostname
 (fn []
   "ty to https://gist.github.com/h1k3r/089d43771bdf811eefe8 for this."
   (let [f (io.popen "/bin/hostname")
         hostname (or (f:read "*a") "")]
     (f:close)
     (string.gsub hostname "\n$" ""))))

(fn is_vader []
  (= (hostname) "vader"))

(fn get_tag [{: name : index : selected}]
  (if
   name (awful.tag.find_by_name nil name)
   index (-> (awful.screen.focused)
             (. :tags)
             (. index))
   selected
   (-> (awful.screen.focused)
       (. :selected_tag))
   ))

(fn move_tag_to_index [awm-tag i]
  (when (not (= awm-tag.index i))
    (awm-tag:swap (get_tag {:index i}))))

(fn try [f catch-f]
  (let [(status exception) (pcall f)]
    (if (not status)
        (catch-f exception))))

(fn log_if_error [f _opts]
  (try f
       (fn [e]
         ;; (pp (lume.merge {:tripped :error-logger} opts))
         (print e))))

{: send_string_to_client
 : hostname
 : is_vader
 : get_tag
 : move_tag_to_index
 : log_if_error
 : try}
