(ns components.icons
  (:require
   [clojure.string :as string]
   [hiccup-icons.octicons :as octicons]
   ;; [hiccup-icons.fa :as fa]
   ;; [hiccup-icons.fa4 :as fa4]
   [hiccup-icons.mdi :as mdi]
   [clawe.client :as client]))

(defn client->icon [client _workspace]
  (let [{:client/keys [app-name window-title]} client]
    #_(when (and (#{"Emacs"} app-name)
                 (string/includes? "journal" window-title))
        (println (client/strip client)
                 (workspace/strip workspace)))
    (cond
      (#{"Emacs"} app-name)
      (cond
        (string/includes? "journal" window-title)
        {:color "text-city-blue-400"
         :src   "/assets/candy-icons/todo.svg"}

        (string/includes? "garden" window-title)
        {:color "text-city-blue-400"
         :src   "/assets/candy-icons/cherrytree.svg"}

        :else
        {:color "text-city-blue-400"
         :src   "/assets/candy-icons/emacs.svg"})

      (#{"VSCodium"} app-name)
      {:color "text-city-green-600"
       :src   "/assets/candy-icons/vscodium.svg"}

      (#{"Alacritty"} app-name)
      {:color "text-city-green-600"
       :src   "/assets/candy-icons/Alacritty.svg"}

      (#{"Spotify"} app-name)
      {:color "text-city-green-400"
       :src   "/assets/candy-icons/spotify.svg"}

      (#{"Audacity"} app-name)
      {:color "text-city-green-400"
       :src   "/assets/candy-icons/audacity.svg"}

      (#{"Pavucontrol"} app-name)
      {:color "text-city-green-400"
       :src   "/assets/candy-icons/pavucontrol.svg"}

      (#{"Messages"} app-name)
      {:color "text-city-green-400"
       :src   "/assets/candy-icons/messenger.svg"}

      (#{"Safari"} app-name)
      {:color "text-city-green-400"
       :src   "/assets/candy-icons/browser.svg"}

      (#{"firefox"} app-name)
      {:color "text-city-green-400"
       :src   "/assets/candy-icons/firefox.svg"}

      (#{"firefoxdeveloperedition"
         "Firefox Developer Edition"} app-name)
      {:color "text-city-green-600"
       :src   "/assets/candy-icons/firefox-nightly.svg"}

      (#{"Google-chrome"} app-name)
      {:color "text-city-green-600"
       :src   "/assets/candy-icons/google-chrome.svg"}

      (#{"DevHub"} window-title)
      {:color "text-city-green-600"
       :src   "/assets/candy-icons/github-desktop.svg"}

      (string/includes? window-title "Slack call")
      {:color "text-city-green-600"
       :src   "/assets/candy-icons/shutter.svg"}

      (#{"Slack"} app-name)
      {:color "text-city-green-400"
       :src   "/assets/candy-icons/slack.svg"}

      (#{"discord"} app-name)
      {:color "text-city-green-400"
       :src   "/assets/candy-icons/discord.svg"}

      (#{"Rofi"} app-name)
      {:color "text-city-green-400"
       :src   "/assets/candy-icons/kmenuedit.svg"}

      (#{"1Password"} app-name)
      {:color "text-city-green-400"
       :src   "/assets/candy-icons/1password.svg"}

      (#{"zoom"} app-name)
      {:color "text-city-green-400"
       :src   "/assets/candy-icons/Zoom.svg"}

      (or (#{"tauri/doctor-topbar"
             "tauri/doctor-popup"
             "tauri/doctor-todo"
             "Tauri App"} window-title)
          (#{"doctor"} app-name))
      {:color "text-city-blue-600"
       :icon  mdi/doctor}

      (string/includes? window-title "Developer Tools")
      {:color "text-city-blue-600"
       :src   "/assets/candy-icons/firefox-developer-edition.svg"}

      (#{"Godot"} app-name)
      {:color "text-city-green-400"
       :src   "/assets/candy-icons/godot.svg"}

      (#{"Aseprite"} app-name)
      {:color "text-city-green-400"
       :src   "/assets/candy-icons/winds.svg"}

      (#{"Steam"} app-name)
      {:color "text-city-green-400"
       :src   "/assets/candy-icons/steam.svg"}

      (#{"obs"} app-name)
      {:color "text-city-green-400"
       :src   "/assets/candy-icons/obs.svg"}

      :else
      (do
        (println "missing icon for client" (client/strip client))
        {:icon octicons/question16}))))

(defn icon-comp [{:keys [class src icon text]}]
  (cond
    src   [:img {:class class :src src}]
    icon  [:div {:class class} icon]
    :else text))

(defn action-icon-button [{:action/keys [label icon on-click tooltip]}]
  [:div
   {:class    ["px-2"
               "cursor-pointer" "hover:text-city-blue-300"
               "rounded" "border" "border-city-blue-700"
               "hover:border-city-blue-300"
               "flex" "items-center"
               "tooltip"
               "relative"]
    :on-click (fn [_] (on-click))}
   [:div (if icon icon label)]
   [:div.tooltip.tooltip-text.bottom-10.-left-3
    {:class ["whitespace-nowrap"]}
    (or tooltip label)]])
