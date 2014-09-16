(defproject sync-data-json "0.0.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
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
    [cheshire "5.3.1"]]
  :profiles {
    :dev {:dependencies [[midje "1.6.3"]]}}
  :plugins [[ragtime/ragtime.lein "0.3.7"]]
  :ragtime {:migrations ragtime.sql.files/migrations})
