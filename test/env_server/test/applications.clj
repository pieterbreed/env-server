(ns env-server.test.applications
    (:use clojure.test
          env-server.applications
          [slingshot.slingshot :only [throw+ try+]])
    (:import [org.joda.time DateTimeUtils])
    (:require [joda-time :as time]))

(defn do-at* [date-time f]
  (DateTimeUtils/setCurrentMillisFixed (.getMillis date-time))
  (try
    (f)
    (finally (DateTimeUtils/setCurrentMillisSystem))))

(defmacro do-at [date-time & body]
  "like clojure.core.do except evalautes the
  expression at the given time"
  `(do-at* ~date-time
    (fn [] ~@body)))
;; use like this
;; (do-at (DateMidnight. 2000 1 1)
;;  (DateMidnight.)) ;;=> (DateMidnight. 2000 1 1)

(deftest addition
    (let [path "path/to/file"
          data {:some :data}
          db (post-app nil path data)
          version "36047353"]
        (testing "When the first app to the db"
            (testing "that the db is created"
                (is (not (nil? db))))
            (testing "that getting the default version returns an error with information to the current (predicted) version"
                (is version 
                    (try+ (get-app db path)
                        (catch [:type     :env-server.applications/bad-request 
                                :sub-type :env-server.applications/version-not-specified] 
                                {:keys [current-version]}
                            current-version))))
            (testing "that we can get the (predicted) version and it has the original data"
                (is data (-> (get-app db path version) :data)))
            (testing "that the list of versions contains only one (predicted) entry"
                (let [versions (get-app-versions db path)]
                    (is 1 (count versions))
                    (is version (first versions)))))
        (testing "app version are returned in order of creation"
            (do-at (time/date-time)
                (let [[db hashes] (loop [datas [{:a :first} {:a :second} {:a :third}]
                                         db nil
                                         hashes '()]
                                    (if (empty? datas) [db hashes]
                                        (do
                                          (DateTimeUtils/setCurrentMillisOffset 1)
                                          (recur (rest datas)
                                                 (post-app db path (first datas))
                                                 (conj hashes (-create-app-hash (first datas)))))))]
                    (print (get-app-versions db path))
                    (is (= hashes (get-app-versions db path))))))))




