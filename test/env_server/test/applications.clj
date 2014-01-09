(ns env-server.test.applications
  (:use clojure.test
        env-server.applications
        clojure.pprint
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
               (is (= hashes (get-app-versions db path))))))))

(deftest apps-based-on-other-apps
  (testing "can not base an app on another app that is not specified well"
    (is (= :env-server.applications/notfound
           (try+ (post-app nil "path" {:a 1} ["base-app/path" "version"])
                 (catch [:type :env-server.applications/notfound
                         :sub-type :env-server.applications/path]
                     {:keys [type]}
                   type)))))
  (testing "can base an app on another app"
    (let [oldapp {:name "oldapp"
                  :path "path/to/old/app"
                  :data {:a 1
                         :b 2}}
          oldapp (assoc oldapp
                   :version (-create-app-hash (:data oldapp)))
          newapp {:name "newapp"
                  :path "path/to/new/app"
                  :data {:c 3
                         :d 4}}
          newapp (assoc newapp
                  :version (-create-app-hash (:data newapp)))
          db (post-app nil
                       (:path oldapp) (:data oldapp))]
      (testing "when the new app does not exist yet"
        (clojure.pprint/pprint db)
        (post-app db
                  (:path newapp) (:data newapp)
                  [(:path oldapp) (:version oldapp)]))
      (testing "when the new app exists already and was previously based on another app"
        (post-app db
                  (:path newapp) (merge (:data newapp) {:e 5})
                  [(:path oldapp) (:version oldapp)]))
    (testing "when the new app exists already and was not previously based on another app"
      (is false)))
  (testing "when based on another app"
    (testing "retains all new data"
      (is false))
    (testing "retains all data from based-on app, when no data is changed"
      (is false))
    (testing "honours data that is changed in the new app"))))




