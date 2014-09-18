(ns sync-data-json.core
  (:require [cheshire.core :refer :all]
            [environ.core :refer [env]]
            [cronj.core :as cronj]
            [sync-data-json.entries :as entries])
  (:gen-class))


(defn read-data-json [url]
  (parse-stream (clojure.java.io/reader url) true))

(defn load-entries [url host]
  (doseq [x (read-data-json url)]
    (entries/load-entry x host)))

(defn load-handler [t opts]
  (load-entries (env :sync-data-json-url) (env :sync-host))
  (println "Entries loaded.")
  (doseq [x (entries/get-new-entries)]
    (let [jsonentry (read-string (get x :serialized_data))]
      (entries/create-external-dataset-from-entry
        jsonentry
        (env :sync-host)
        (env :sync-url)
        (env :sync-username)
        (env :sync-password)
        (env :sync-token))))
  (doseq [x (entries/get-updated-entries)]
    (let [jsonentry (read-string (get x :serialized_data))
          destination-id (get x :destination_id)]
      (entries/update-external-dataset-from-entry
        destination-id
        jsonentry
        (env :sync-host)
        (env :sync-url)
        (env :sync-username)
        (env :sync-password)
        (env :sync-token))))
  (println "Done."))


(def load-task
  {:id "load-task"
   :handler load-handler
   :schedule "0 0 0 * * * *"
   :opts {}})

(def cj (cronj/cronj :entries [load-task]))

(defn -main [& m]
  (cronj/start! cj))
