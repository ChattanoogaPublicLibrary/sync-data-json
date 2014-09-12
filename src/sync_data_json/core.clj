(ns sync-data-json.core
  (:require [cheshire.core :refer :all]
            [environ.core :refer [env]]
            [sync-data-json.entries :as entries]))


(defn read-data-json [url]
  (parse-stream (clojure.java.io/reader url) true))

(defn load-entries [url]
  (doseq [x (read-data-json url)]
    (entries/load-entry x)))

(defn -main [& m]
  (load-entries (env :data-json-url))
  (println "Entries loaded."))
