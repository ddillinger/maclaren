(defproject maclaren "1.0.0-SNAPSHOT"
  :description "clojure utils for working with tar archives"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [org.apache.commons/commons-compress "1.4.1"]
                 [org.apache.commons/commons-io "1.3.2"]
                 [org.apache.commons/commons-lang3 "3.1"]
                 [props3t "0.0.3" :exclude [clj-http]]
                 [clj-http "0.6.3"]
                 [sonian/carica "1.0.2"]]
  :min-lein-version "2.0.0"
  :plugins [[lein-bikeshed "0.1.0"]
            [lein-swank "1.4.4"]
            [lein-pprint "1.1.1"]])
