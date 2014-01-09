(ns env-server.test.applications
    (:use clojure.test
          env-server.applications
          [slingshot.slingshot :only [throw+ try+]]))

(testing "testing"
    (is true))

(deftest addition
    (testing "When the first app to the db"
        (let [path "path/to/file"
              data {:some :data}
              db (post-app nil path data)]
            (testing "that the db is created"
                (is (not (nil? db))))
            (testing "that getting the default version returns an error with information to the current version"
                (is "36047353" 
                    (try+ (get-app db path)
                        (catch [:type     :env-server.applications/bad-request 
                                :sub-type :env-server.applications/version-not-specified] 
                                {:keys [current-version]}
                            current-version)))))))
