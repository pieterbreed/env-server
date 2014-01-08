(defproject env-server "0.1.0-SNAPSHOT"
  :description "Environment configuration management"
  :url "http://example.com/FIXME"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [compojure "1.1.6"]
                 [clojure.joda-time "0.1.0"]
                 [slingshot "0.10.3"]]
  :plugins [[lein-ring "0.8.10"]]
  :ring {:handler env-server.handler/app}
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring-mock "0.1.5"]]}})
