(ns ralphie.battery
  (:require [babashka.process :as process]))

(defn info []
  (let [bat-str (-> ^{:out :string}
                    (process/$ acpi)
                    process/check :out)
        status  (some->> bat-str (re-seq #": (\w+),") first second)
        charge  (some->> bat-str (re-seq #"\d?\d\d%") first)
        time    (some->> bat-str (re-seq #"\d\d:\d\d:\d\d") first)]
    {:battery/status           status
     :battery/remaining-charge charge
     :battery/remaining-time   time}))
