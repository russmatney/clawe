(local awful (require "awful"))
(local posix (require "posix"))

(require "./table-serialization")
(require "./table-indexof")

;; `layouts` defined in run-init.fnl

;; This save/restore assumes the tags maintain their order.
;; It'd probably be better to match on tag names.

(local tags-state-file (.. (awful.util.get_cache_dir) "/tags-state"))

(local mod [])

(fn mod.save_state []
  (let [screen _G.mouse.screen tags screen.tags tags-to-restore []]
    (each [i t (ipairs tags)]
      (var sel nil)
      (if (= t.selected true)
          (set sel "true")
          (set sel "false"))
      (table.insert tags-to-restore
                    [i
                     t.name
                     (_G.indexof_table layouts t.layout)
                     t.column_count
                     t.master_width_factor
                     t.master_count
                     sel]))

    (when (posix.stat tags-state-file)
      (os.remove tags-state-file))

    (_G.save_table tags-to-restore tags-state-file)))

(fn mod.save_state_and_restart []
  (mod.save_state)
  (awesome.restart))

(fn mod.restore_state []
  (when (posix.stat tags-state-file)
    (local tags-to-restore (_G.load_table tags-state-file))
    (local s (awful.screen.focused))
    (each [_ p (ipairs tags-to-restore)]
      (let [[_ name layout ncol mwfact nmaster selected] p
            selected (= selected "true")]

        (var t (awful.tag.find_by_name s name))
        (when (not t)
          ;; (pp {:creating_cached_tag name})
          (awful.tag.add name {:screen s})
          (set t (awful.tag.find_by_name s name)))

        (if t
            (do
              (set t.layout (. layouts layout))
              (set t.column_count ncol)
              (set t.master_width_factor mwfact)
              (set t.master_count nmaster)
              (when (and selected (= t.selected false))
                (awful.tag.viewtoggle t)))
            (pp {:missed_tag_cache_for name}))))))

mod

