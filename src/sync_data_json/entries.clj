(ns sync-data-json.entries
  (:require [korma.db :refer :all]
            [korma.core :refer :all]
            [environ.core :refer [env]]
            [cemerick.url :as cemerickurl]
            [digest :as digest])
  (:import [com.socrata.api SodaImporter HttpLowLevel]
           [com.socrata.model.importer DatasetInfo Metadata]
           [com.socrata.builders ExternalDatasetBuilder]
           [org.jsoup Jsoup]))

(defdb db
  (postgres {
    :db (env :database-name)
    :user (env :database-user)
    :password (env :database-password)
    :host (env :database-host)}))

(defentity entries
  (pk :id)
  (table :entries))

(defn get-host-from-url [u]
  (-> (cemerickurl/url u)
      (get :host)))

(defn strip-html [t]
  (.text (Jsoup/parse t)))

(defn sanitize-entry-attribution [jsonentry]
  (if (nil? (get jsonentry :attribution))
    (assoc-in jsonentry [:attribution] "")
    (update-in jsonentry [:attribution] strip-html)))

(defn sanitize-entry-attribution-url [jsonentry]
  (if (nil? (get jsonentry :attributionURL))
    (assoc-in jsonentry [:attributionURL] "")
    (update-in jsonentry [:attributionURL] strip-html)))

(defn sanitize-entry-description [jsonentry]
  (if (nil? (get jsonentry :description))
    (assoc-in jsonentry [:description] "")
    (update-in jsonentry [:description] strip-html)))

(defn sanitize-entry-keyword [jsonentry]
  (if (nil? (get jsonentry :keyword))
    (assoc-in jsonentry [:keyword] [])
    jsonentry))

(defn sanitize-entry [jsonentry]
  (-> jsonentry
    (sanitize-entry-attribution)
    (sanitize-entry-attribution-url)
    (sanitize-entry-description)
    (sanitize-entry-keyword)))

(defn entry-exists? [source-id host]
  (> (get (first
    (select entries
      (aggregate (count :*) :cnt)
      (where (and (= :host host) (= :source_id source-id)))))
      :cnt) 0))

(defn entry-as-md5 [jsonentry]
  (digest/md5 (pr-str jsonentry)))

(defn entry-changed? [jsonentry host]
  (let [sanitized-entry (sanitize-entry jsonentry)
        id (get sanitized-entry :identifier)
        checksum (entry-as-md5 sanitized-entry)]
    (not (> (get (first
      (select entries
        (aggregate (count :*) :cnt)
        (where (and (= :host host) (= :source_id id) (= :checksum checksum)))))
        :cnt) 0))))

(defn create-entry [jsonentry host]
  (let [sanitized-entry (sanitize-entry jsonentry)
        id (get sanitized-entry :identifier)
        checksum (entry-as-md5 sanitized-entry)]
    (insert entries
      (values {:host host :source_id id :checksum checksum :serialized_data (pr-str sanitized-entry)}))))

(defn update-entry [jsonentry host]
  (let [sanitized-entry (sanitize-entry jsonentry)
        id (get sanitized-entry :identifier)
        checksum (entry-as-md5 sanitized-entry)]
    (update entries
      (set-fields {:changed true :new_entry false :checksum checksum :serialized_data (pr-str sanitized-entry)})
      (where (and (= :host host) (= :source_id id) (not= checksum :checksum))))))

; Combine create and update entry
(defn load-entry [jsonentry host]
  (if (not (entry-exists? (get jsonentry :identifier) host))
    (create-entry jsonentry host)
    (if (entry-changed? jsonentry host)
      (update-entry jsonentry host)
      nil)))

(defn new-soda-importer [url username password token]
  (SodaImporter.
    (HttpLowLevel/instantiateBasic url username password token nil)))

(defn distribution-to-access-points [d]
  (into {} (map
    (fn [a] [(get a :format) (get a :accessURL)]) d)))

(defn build-external-dataset [jsonentry]
  (let [sanitized-entry (sanitize-entry jsonentry)]
    (-> (ExternalDatasetBuilder.)
      (.setMetadata (Metadata.))
      (.setName (get sanitized-entry :title))
      (.setTags (get sanitized-entry :keyword))
      (.setAccessPoints (distribution-to-access-points (get sanitized-entry :distribution)))
      (.setDescription (get sanitized-entry :description))
      (.setAttribution (get sanitized-entry :attribution))
      (.setAttributionLink (get sanitized-entry :attributionURL))
      (.build))))

(defn update-external-dataset-data [jsonentry dataset]
  (let [sanitized-entry (sanitize-entry jsonentry)
        updated-dataset dataset]
    (.setMetadata updated-dataset (Metadata.))
    (.setAccessPoints (.getMetadata updated-dataset) (distribution-to-access-points (get sanitized-entry :distribution)))
    (.setName updated-dataset (get sanitized-entry :title))
    (.setDescription updated-dataset (get sanitized-entry :description))
    (.setTags updated-dataset (get sanitized-entry :keyword))
    (.setAttribution updated-dataset (get sanitized-entry :attribution))
    (.setAttributionLink updated-dataset (get sanitized-entry :attributionURL))
    updated-dataset))


(defn update-external-dataset [destination-id jsonentry url username password token]
  (let [dataset (-> (new-soda-importer url username password token)
         (.loadDatasetInfo destination-id))
        soda-importer (new-soda-importer url username password token)]
    (-> soda-importer (.updateDatasetInfo (update-external-dataset-data jsonentry dataset)))))

(defn update-existing-external-dataset [destination-id jsonentry url username password token]
  (let [importer (new-soda-importer url username password token)
        loaded-view (update-external-dataset-data jsonentry (-> importer (.loadDatasetInfo destination-id)))]
    (-> importer
      (.updateDatasetInfo loaded-view))))


(defn create-external-dataset [jsonentry url username password token]
  (let [soda-importer (new-soda-importer url username password token)
        new-dataset (-> soda-importer (.createDataset (build-external-dataset jsonentry)))]
        ; I don't get it. Tags will only add after dataset is created.
    (update-external-dataset (.getId new-dataset) jsonentry url username password token)
    (-> soda-importer
      (.makePublic (.getId new-dataset)))
    (-> soda-importer
      (.publish (.getId new-dataset)))))

(defn get-new-entries []
  (select entries (where (= :new_entry true))))

(defn get-updated-entries []
  (select entries (where (and (= :new_entry false) (= :changed true)))))

(defn create-external-dataset-from-entry [jsonentry host url username password token]
  (let [new-dataset (create-external-dataset jsonentry url username password token)]
    (update entries
          (set-fields {:changed false :new_entry false :destination_id (.getId new-dataset)})
          (where (and (= :host host) (= :source_id (get jsonentry :identifier)))))))

(defn update-external-dataset-from-entry [destination-id jsonentry host url username password token]
  (let [updated-dataset (update-existing-external-dataset destination-id jsonentry url username password token)]
    (update entries
      (set-fields {:changed false })
      (where (and (= :host host) (= :destination_id destination-id))))))
