#!/usr/bin/env obb

(def app (js/Application.currentApplication))

(set! (.-includeStandardAdditions app) true)

(let [response (.displayDialog app
                               "What should we label this space?"
                               #js {:defaultAnswer ""
                                    :withIcon      "note"
                                    :buttons       #js ["Cancel" "Continue"]
                                    :defaultButton "Continue"})]
  (.displayDialog app (str "Fine, we'll label it: " (.-textReturned response) "."))
  (prn (.-textReturned response)))
