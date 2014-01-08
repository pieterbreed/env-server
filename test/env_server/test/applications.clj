(ns env-server.test.applications
    (:use clojure.test
          env-server.applications))

(deftest addition
    (testing "When adding apps to the db"
        (testing "if the db is nil"
            (let [path "path/to/file"
                  data {:some :data}
                  db (post-app nil path data)]
                (is (not (nil? db)))))))

(run-tests)