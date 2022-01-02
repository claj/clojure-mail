(defproject io.github.claj/clojure-mail "2.0.0-alpha-1"
  :description "Clojure Email Library"
  :url "https://github.com/claj/clojure-mail"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.3"]
                 [org.jsoup/jsoup "1.14.3"] ;; for cleaning up messy html messages
                 [com.sun.mail/jakarta.mail "2.0.1"]
                 [medley "1.3.0"]]
  :deploy-repositories [["releases" {:url "https://clojars.org/repo"
                                     :creds :gpg}]]
  :plugins [[lein-cljfmt "0.8.0"]]
  :profiles {:dev {:dependencies [[com.icegreen/greenmail "2.0.0-alpha-2"]]}})
