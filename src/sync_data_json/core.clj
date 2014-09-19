(ns sync-data-json.core
  (:require [cheshire.core :refer :all]
            [environ.core :refer [env]]
            [cronj.core :as cronj]
            [sync-data-json.entries :as entries])
  (:gen-class))


(defn read-data-json [url]
  (parse-stream (clojure.java.io/reader url) true))

(defn load-entries [url host]
  (doseq [x (filter (fn [i] (= (get i :contactPoint) "geoace67")) (read-data-json url))]
    (entries/load-entry x host)))

(defn add-attributions [jsonentry attribution attribution-url]
  (-> jsonentry
  (assoc-in [:attribution] attribution)
  (assoc-in [:attributionURL] attribution-url)))

(defn load-handler [t opts]
  (load-entries (env :sync-data-json-url) (env :sync-host))
  (println "Entries loaded.")
  (doseq [x (entries/get-new-entries)]
    (let [jsonentry (read-string (get x :serialized_data))]
      (entries/create-external-dataset-from-entry
        (add-attributions jsonentry (env :sync-attribution) (env :sync-attribution-url))
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
        (add-attributions jsonentry (env :sync-attribution) (env :sync-attribution-url))
        (env :sync-host)
        (env :sync-url)
        (env :sync-username)
        (env :sync-password)
        (env :sync-token))))
  (println "Done."))


(def load-task
  {:id "load-task"
   :handler load-handler
   :schedule "0 05 16 * * * *"
   :opts {}})

(def cj (cronj/cronj :entries [load-task]))

(defn -main [& m]
  (println "Starting cron.")
  (cronj/start! cj))
