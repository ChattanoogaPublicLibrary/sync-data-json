(defproject sync-data-json "0.0.0-SNAPSHOT"
  :description "A utility to mirror datasets from other data portals as external datasets in your Socrata Open Data Portal. Uses external data portal's data.json dataset catalog for aggregation."
  :url "https://github.com/ChattanoogaPublicLibrary/sync-data-json"
  :min-lein-version "2.0.0"
  :license {:name "MIT"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [
    [org.clojure/clojure "1.6.0"]
    [log4j "1.2.15" :exclusions [javax.mail/mail
                                 javax.jms/jms
                                 com.sun.jdmk/jmxtools
                                 com.sun.jmx/jmxri]]
    [korma "0.4.0"]
    [com.socrata/soda-api-java "0.9.12"]
    [ragtime "0.3.7"]
    [postgresql/postgresql "9.1-901.jdbc4"]
    [digest "1.4.4"]
    [environ "1.0.0"]
    [org.jsoup/jsoup "1.7.2"]
    [com.cemerick/url "0.1.1"]
    [im.chit/cronj "1.4.2"]
    [cheshire "5.3.1"]]
  :main ^:skip-aot sync-data-json.core
  :target-path "target/%s"
  :javac-options ["-target" "1.7" "-source" "1.7"]
  :profiles {
    :uberjar {:aot :all}
    :dev {:dependencies [[midje "1.6.3"]]}}
  :plugins [[ragtime/ragtime.lein "0.3.7"]]
  :ragtime {:migrations ragtime.sql.files/migrations})
