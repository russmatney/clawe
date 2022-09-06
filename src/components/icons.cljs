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
      (#{"emacs"} app-name)
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

      (#{"vscodium"} app-name)
      {:color "text-city-green-600"
       :src   "/assets/candy-icons/vscodium.svg"}

      (#{"alacritty"} app-name)
      {:color "text-city-green-600"
       :src   "/assets/candy-icons/Alacritty.svg"}

      (#{"spotify"} app-name)
      {:color "text-city-green-400"
       :src   "/assets/candy-icons/spotify.svg"}

      (#{"audacity"} app-name)
      {:color "text-city-green-400"
       :src   "/assets/candy-icons/audacity.svg"}

      (#{"pavucontrol"} app-name)
      {:color "text-city-green-400"
       :src   "/assets/candy-icons/pavucontrol.svg"}

      (#{"messages"} app-name)
      {:color "text-city-green-400"
       :src   "/assets/candy-icons/messenger.svg"}

      (#{"safari"} app-name)
      {:color "text-city-green-400"
       :src   "/assets/candy-icons/browser.svg"}

      (#{"firefox"} app-name)
      {:color "text-city-green-400"
       :src   "/assets/candy-icons/firefox.svg"}

      (#{"firefoxdeveloperedition"
         "firefox developer edition"} app-name)
      {:color "text-city-green-600"
       :src   "/assets/candy-icons/firefox-nightly.svg"}

      (#{"google-chrome"} app-name)
      {:color "text-city-green-600"
       :src   "/assets/candy-icons/google-chrome.svg"}

      (#{"devhub"} window-title)
      {:color "text-city-green-600"
       :src   "/assets/candy-icons/github-desktop.svg"}

      (string/includes? window-title "slack call")
      {:color "text-city-green-600"
       :src   "/assets/candy-icons/shutter.svg"}

      (#{"slack"} app-name)
      {:color "text-city-green-400"
       :src   "/assets/candy-icons/slack.svg"}

      (#{"discord"} app-name)
      {:color "text-city-green-400"
       :src   "/assets/candy-icons/discord.svg"}

      (#{"rofi"} app-name)
      {:color "text-city-green-400"
       :src   "/assets/candy-icons/kmenuedit.svg"}

      (#{"1password"} app-name)
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

      (string/includes? window-title "developer tools")
      {:color "text-city-blue-600"
       :src   "/assets/candy-icons/firefox-developer-edition.svg"}

      (#{"godot"} app-name)
      {:color "text-city-green-400"
       :src   "/assets/candy-icons/godot.svg"}

      (#{"aseprite"} app-name)
      {:color "text-city-green-400"
       :src   "/assets/candy-icons/winds.svg"}

      (#{"steam"} app-name)
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
