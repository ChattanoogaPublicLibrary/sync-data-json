(ns sync-data-json.t-entries
  (:use midje.sweet)
  (:require [sync-data-json.entries :as entries]
            [environ.core :refer [env]]
            [korma.db :refer :all]
            [korma.core :refer :all]))

(def example-entry {:description "Properties of the Tennessee River Gorge Trust", :accessLevel "public", :publisher "none", :license "", :mbox "geoace67", :spatial "-85.5614 34.9938 -85.34 35.1803", :contactPoint "geoace67", :modified "2013-02-06T15:24:40.000+00:00", :title "Prentice Cooper State Forest", :keyword ["Tennessee River Gorge" "Chattanooga" "TN"], :identifier "d0111ac0e16341b48418e27a152b16cb_1", :distribution [{:accessURL "http://geoportal.congeo.opendata.arcgis.com/datasets/d0111ac0e16341b48418e27a152b16cb_1.csv", :format "csv"} {:accessURL "http://geoportal.congeo.opendata.arcgis.com/datasets/d0111ac0e16341b48418e27a152b16cb_1.kml", :format "kml"} {:accessURL "http://geoportal.congeo.opendata.arcgis.com/datasets/d0111ac0e16341b48418e27a152b16cb_1.geojson", :format "geojson"} {:accessURL "http://geoportal.congeo.opendata.arcgis.com/datasets/d0111ac0e16341b48418e27a152b16cb_1.zip", :format "zip"}], :webService "http://geoengine.utc.edu:6080/arcgis/rest/services/TRGT/TRGT_properties/MapServer/1"})

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



;(facts "create-external-dataset"
;  (fact ""
;    (entries/create-external-dataset example-entry (env :test-url) (env :test-username) (env :test-password) (env :test-token)) => ""))
