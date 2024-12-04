(ns components.dialog
  (:require
   ["@headlessui/react" :as Headless]
   [uix.core :as uix :refer [defui $]]))

(defui dialog [{:keys [open on-close title description content]}]
  ($ Headless/Dialog
     {:class ["relative"
              "z-50"]
      :open  open :on-close on-close}

     ($ :div
        {:class       ["fixed"
                       "inset-0"
                       "bg-city-black-700"
                       "bg-opacity-60"]
         :aria-hidden "true"})

     ($ :div {:class ["fixed inset-0 flex items-center justify-center"
                      "m-24" "p-12"
                      "bg-city-black-200"]}

        ($ Headless/Dialog.Panel
           ($ Headless/Dialog.Title title)
           ($ Headless/Dialog.Description description)
           (when content content)))))
