(defproject jspha/bert "0.1.0-SNAPSHOT"
  :description "BERT, BERP, and BERT-RPC for Clojure."
  :url "http://github.com/sdbo/bert"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [clj-time "0.4.2"]
                 [jspha/otp "1.5.3"]
                 [potemkin "0.1.3"]]
  :profiles {:dev {:dependencies [[expectations "1.3.7"]]}})
