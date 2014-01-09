(ns env-server.test.applications
    (:use clojure.test
          env-server.applications
          [slingshot.slingshot :only [throw+ try+]]))

(deftest addition
    (testing "When the first app to the db"
        (let [path "path/to/file"
              data {:some :data}
              db (post-app nil path data)
              version "36047353"]
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
                    (is version (first versions)))))))
