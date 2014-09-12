(ns sync-data-json.t-entries
  (:use midje.sweet)
  (:require [sync-data-json.entries :as entries]
            [environ.core :refer [env]]
            [korma.db :refer :all]
            [korma.core :refer :all]))

(def example-entry
  {:description "The LSIS Land Description Area layer (LADESC) is the smallest area unit of the GCDB. \n<p><span style='line-height: normal; text-indent: -0.25in; font-family:; font-size: 7pt;'> </span><font size='3'><span style='line-height: 115%; background-color: white;'>This map service contains Public Land Survey System (PLSS) data for most of the USA, which includes both rectangular and non-rectangular surveys.</span><br /></font></p><p></p><ul><li><span style='line-height: 13px;'><span style='line-height: 115%;'><font size='3'>PLSS data is a useful representation of the geometry and topology of parcels contained within the areas covered by this dataset.</font></span></span></li><li><span style='line-height: 13px;'><span style='line-height: 115%;'><font size='3'>Land Survey data coverage only covers states that participate in the PLSS program.</font></span></span></li><li><font size='3'><span style='line-height: 13px;'><span style='line-height: 115%;'>The map shows </span></span><span style='line-height: 115%;'>meridians and meridian lines at smaller scales and township boundary lines and labels and quarter-quarter boundaries at larger scales. Pop-ups contain specific information about each boundary area, such as label names and identification numbers.</span></font></li><li><span style='line-height: 115%;'><font size='3'>The data was obtained and managed by the <a href='http://www.geocommunicator.gov/GeoComm/services.htm' target='_self'>Bureau of Land Management</a>.</font></span></li></ul><p></p><p></p>", :accessLevel "public", :publisher "none", :license "<br />", :mbox "Federal_User_Community", :spatial "-179.2304 18.9248 179.894 71.4683", :contactPoint "Federal_User_Community", :modified "2014-07-24T15:09:53+00:00", :title "Quarter-Quarters", :keyword ["PLSS" "Public Land Survey System" "PLSS Townships" "PLSS Sections" "PLSS Second Divisions" "Planning Cadastre" "BLM Cadastral Survey" "A-16" "A16" "USDOI" "US Department of the Interior" "BLM" "Bureau of Land Management" "USA" "first divisions" "planning cadastre" "federal" "nationa" "esrimapservice" "vector"], :identifier "599472ca35ab462ba7cb7fc793d97376_6", :distribution [{:accessURL "http://geoportal.congeo.opendata.arcgis.com/datasets/599472ca35ab462ba7cb7fc793d97376_6.csv", :format "csv"} {:accessURL "http://geoportal.congeo.opendata.arcgis.com/datasets/599472ca35ab462ba7cb7fc793d97376_6.kml", :format "kml"} {:accessURL "http://geoportal.congeo.opendata.arcgis.com/datasets/599472ca35ab462ba7cb7fc793d97376_6.geojson", :format "geojson"} {:accessURL "http://geoportal.congeo.opendata.arcgis.com/datasets/599472ca35ab462ba7cb7fc793d97376_6.zip", :format "zip"}], :webService "http://www.geocommunicator.gov/ArcGIS/rest/services/PLSS/MapServer/6"})


(def example-entry-updated
  {:description "Theeeeeeeee LSIS Land Description Area layer (LADESC) is the smallest area unit of the GCDB. \n<p><span style='line-height: normal; text-indent: -0.25in; font-family:; font-size: 7pt;'> </span><font size='3'><span style='line-height: 115%; background-color: white;'>This map service contains Public Land Survey System (PLSS) data for most of the USA, which includes both rectangular and non-rectangular surveys.</span><br /></font></p><p></p><ul><li><span style='line-height: 13px;'><span style='line-height: 115%;'><font size='3'>PLSS data is a useful representation of the geometry and topology of parcels contained within the areas covered by this dataset.</font></span></span></li><li><span style='line-height: 13px;'><span style='line-height: 115%;'><font size='3'>Land Survey data coverage only covers states that participate in the PLSS program.</font></span></span></li><li><font size='3'><span style='line-height: 13px;'><span style='line-height: 115%;'>The map shows </span></span><span style='line-height: 115%;'>meridians and meridian lines at smaller scales and township boundary lines and labels and quarter-quarter boundaries at larger scales. Pop-ups contain specific information about each boundary area, such as label names and identification numbers.</span></font></li><li><span style='line-height: 115%;'><font size='3'>The data was obtained and managed by the <a href='http://www.geocommunicator.gov/GeoComm/services.htm' target='_self'>Bureau of Land Management</a>.</font></span></li></ul><p></p><p></p>", :accessLevel "public", :publisher "none", :license "<br />", :mbox "Federal_User_Community", :spatial "-179.2304 18.9248 179.894 71.4683", :contactPoint "Federal_User_Community", :modified "2014-07-24T15:09:53+00:00", :title "Quarter-Quarterssssssss", :keyword ["PLSS" "Public Land Survey System" "PLSS Townships" "PLSS Sections" "PLSS Second Divisions" "Planning Cadastre" "BLM Cadastral Survey" "A-16" "A16" "USDOI" "US Department of the Interior" "BLM" "Bureau of Land Management" "USA" "first divisions" "planning cadastre" "federal" "nationa" "esrimapservice" "vector"], :identifier "599472ca35ab462ba7cb7fc793d97376_6", :distribution [{:accessURL "http://geoportal.congeo.opendata.arcgis.com/datasets/599472ca35ab462ba7cb7fc793d97376_6.csv", :format "csv"} {:accessURL "http://geoportal.congeo.opendata.arcgis.com/datasets/599472ca35ab462ba7cb7fc793d97376_6.kml", :format "kml"} {:accessURL "http://geoportal.congeo.opendata.arcgis.com/datasets/599472ca35ab462ba7cb7fc793d97376_6.geojson", :format "geojson"}], :webService "http://www.geocommunicator.gov/ArcGIS/rest/services/PLSS/MapServer/6"})

(defn reset-database []
  (exec-raw entries/db "DELETE FROM entries;")
  (exec-raw entries/db "ALTER SEQUENCE entries_id_seq RESTART WITH 1;"))

(facts "strip-html"
  (fact "strips html from given text"
    (entries/strip-html "<b>Hi. I am text.</b><a href='#'> I am a link.") => "Hi. I am text. I am a link."))

(facts "sanitize-entry-description"
  (fact "If an entry has no description, it is given an empty description"
    (entries/sanitize-entry-description {:identifier "test"}) => {:description "", :identifier "test"})
  (fact "If an entry has a description with HTML, the HTML is stripped."
    (entries/sanitize-entry-description {:description "<b>Test</b>" :identifier "test"}) => {:description "Test", :identifier "test"})
  (fact "If an entry has a description with no HTML, the plain text stays the same."
    (entries/sanitize-entry-description {:description "Test" :identifier "test"}) => {:description "Test", :identifier "test"}))

(facts "sanitize-entry-keyword"
  (fact "If there is no keyword field in the entry, it is given an empty vector."
    (entries/sanitize-entry-keyword {:identifier "anothertest"}) => {:identifier "anothertest", :keyword []})
  (fact "If there are keywords in the entry, the keywords stay the same."
    (entries/sanitize-entry-keyword {:identifier "anothertest" :keyword ["yeah" "sure"]}) => {:identifier "anothertest", :keyword ["yeah" "sure"]})

    )

(facts "exists-by-source-id"
  (with-state-changes [(before :facts (reset-database))]
    (fact "it returns true if a record exists by the source id."
      (do
        (entries/create-entry {:identifier "thisisatest"})
        (entries/exists-by-source-id "thisisatest")) => true)
    (fact "it returns true if a record exists by the source id."
      (entries/exists-by-source-id "idonotexist") => false)))

(facts "entry-as-md5"
  (fact "it returns the md5 checksum of a stringified clojure data structure"
    (entries/entry-as-md5 {:test "test"}) => "57e29fb58f7eb934bdd53e6b6a6a4a57"))

(facts "entry-not-changed"
  (with-state-changes [(before :facts (reset-database))]
    (fact "It assumes HTML is stripped in create-entry and update-entry."
      (do
        (entries/create-entry {:identifier "thisisatest" :description "<b>Hi</b>"})
        (entries/entry-not-changed {:identifier "thisisatest" :description "Hi"})) => true)
    (fact "it returns true if the entry has not changed"
      (do
        (entries/create-entry {:identifier "thisisatest"})
        (entries/entry-not-changed {:identifier "thisisatest"})) => true)
    (fact "it returns false if the entry has changed"
      (do
        (entries/create-entry {:identifier "thisisatest"})
        (entries/entry-not-changed {:identifier "thisisatest" :something "else"})) => false)))

(facts "create-entry"
  (with-state-changes [(before :facts (reset-database))]
    (fact "It strips HTML from description."
      (do
        (entries/create-entry {:identifier "thisisatest" :description "<b>Hi.</b>"})
        (get (read-string (get
          (first (select entries/entries
            (where (= :source_id "thisisatest")))) :serialized_data)) :description) => "Hi."))
    (fact "it returns true if a record exists by the source id."
      (do
        (entries/create-entry {:identifier "thisisatest"})
        (entries/exists-by-source-id "thisisatest")) => true)
    (fact "it creates a record with the serialized entry data saved in the record."
      (do
        (entries/create-entry {:identifier "atest"})
        (get
          (first
            (select entries/entries
              (where (= :source_id "atest")))) :serialized_data)) => "{:keyword [], :description \"\", :identifier \"atest\"}")))

(facts "update-entry"
  (with-state-changes [(before :facts (reset-database))]
    (fact "It strips HTML from description."
      (do
        (entries/create-entry {:identifier "thisisatest" :description "<b>Hi.</b>"})
        (entries/update-entry {:identifier "thisisatest" :something "else" :description "<b>Hi.</b>"})
        (get (read-string (get
          (first (select entries/entries
            (where (= :source_id "thisisatest")))) :serialized_data)) :description) => "Hi."))
    (fact "If the entry has changed, update with the field's new checksum."
      (do
        (entries/create-entry {:identifier "thisisatest"})
        (entries/update-entry {:identifier "thisisatest" :something "else"})
        (get (first (select entries/entries (where (= :source_id "thisisatest")))) :checksum)) => "b9cbcd964dbb8733e51061d3e9af67cf")
    (fact "If the entry has changed, the serialized_data is updated."
      (do
        (entries/create-entry {:identifier "atest"})
        (entries/update-entry {:identifier "atest" :something "else"})
        (get
          (first
            (select entries/entries
              (where (= :source_id "atest")))) :serialized_data)) => "{:keyword [], :description \"\", :identifier \"atest\", :something \"else\"}")
    (fact "If the entry has not changed, the serialized_data stays the same."
      (do
        (entries/create-entry {:identifier "yetanothertest"})
        (entries/update-entry {:identifier "yetanothertest"})
        (get
          (first
            (select entries/entries
              (where (= :source_id "yetanothertest")))) :serialized_data)) => "{:keyword [], :description \"\", :identifier \"yetanothertest\"}")
    (fact "If the entry has not changed, the original checksum for the entry stays the same."
      (do
        (entries/create-entry {:identifier "thisisanothertest"})
        (entries/update-entry {:identifier "thisisanothertest"})
        (get (first (select entries/entries (where (= :source_id "thisisanothertest")))) :checksum)) => "dc5c0c7a0c2ac98d56d142025abc72dc")))

(facts "distribution-to-access-points"
  (fact "It creates a key pair from the URL file type that contains the URL"
    (entries/distribution-to-access-points [{:format "html" :accessURL "http://www.google.com"} {:format "csv" :accessURL "http://www.google.com/something.csv"}]) => {"html" "http://www.google.com", "csv" "http://www.google.com/something.csv"})
  (fact "If the distribution is empty, it returns an empty map."
    (entries/distribution-to-access-points {}) => {}))

(facts "build-external-dataset"
  (fact "Returns new external dataset object with given entry title as the name."
    (.getName (entries/build-external-dataset {:title "Some Name"})) => "Some Name")
  (fact "Returns new external dataset object with given entry description as the description."
    (.getDescription (entries/build-external-dataset {:description "Some description"})) => "Some description")
  (fact "If external dataset has HTML in description, it strips it out."
    (.getDescription (entries/build-external-dataset {:description "<b>Some description</b>"})) => "Some description")
  (fact "Returns new external dataset object with given external dataset access points."
    (.. (entries/build-external-dataset {:distribution [{:format "csv" :accessURL "http://www.example.com/example.csv"} {:format "html" :accessURL "http://www.example.com"}]}) (getMetadata) (getAccessPoints)) => {"html" "http://www.example.com", "csv" "http://www.example.com/example.csv"})
  (fact "Returns new external dataset object with given entry tags as the tags."
    (.. (entries/build-external-dataset {:keyword ["one","two","three"]}) (getTags)) => ["one" "two" "three"]))


(facts "get-new-entries"
  (with-state-changes [(before :facts (reset-database))]
    (fact "It returns new entries."
      (do
        (entries/create-entry {:identifier "yetanothertest"})
        (entries/create-entry {:identifier "test"})
        (entries/create-entry {:identifier "testtest"})
        (entries/create-entry {:identifier "testtesttest"})
        (entries/update-entry {:identifier "testtest" :something "else"})
        (entries/update-entry {:identifier "testtesttest" :something "else"})
        (count (entries/get-new-entries)))
        => 2)))

(facts "get-updated-entries"
  (with-state-changes [(before :facts (reset-database))]
    (fact "It returns updated entries"
      (do
        (entries/create-entry {:identifier "yetanothertest"})
        (entries/create-entry {:identifier "test"})
        (entries/create-entry {:identifier "testtest"})
        (entries/create-entry {:identifier "testtesttest"})
        (entries/update-entry {:identifier "testtest" :something "else"})
        (entries/update-entry {:identifier "testtesttest" :something "else"})
        (entries/update-entry {:identifier "test" :something "else"})
        (count (entries/get-updated-entries)))
        => 3)))
