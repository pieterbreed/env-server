(ns env-server.test.applications
    (:use clojure.test
          env-server.applications))

(deftest addition
    (testing "When adding apps to the db"
        (testing "if the db is nil"
            (let [path "path/to/file"
                  data {:some :data}
                  db (post-app nil path data)]
                (testing "that the result is not nil also"
                    (is (not (nil? db))))
                (testing "that getting the default version will return the old data"
                    (is data (get-app db path)))))))
