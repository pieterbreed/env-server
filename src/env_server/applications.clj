(ns env-server.applications
    (:require [joda-time :as time])
    (:use [slingshot.slingshot :only [throw+]]))

;;; the main database (for now)
;;; all in-memory
(def apps-db (atom nil))

(defn create-app-hash
    "creates a value-based hash of the data in the app"
    [data]
    (-> data hash str))

(defn create-meta-info
    "creates some meta information that is not idempotent"
    [path data]
    (let [ver (create-app-hash data)
          now (-> (time/date-time) time/as-map)]
          {:version-nr ver
           :created now}))

(defn post-app
    "Add the application identified by path to the db"
    [db path data]
    (let [app (get-in db [:apps path] {})
          previous-versions (get app :versions {})
          previous-version (get app :current nil)
          meta (assoc (create-meta-info path data)
                      :previous-version previous-version)
          new-version {:meta meta
                       :data data}
          ver (:version-nr meta)
          app (-> app
                  (assoc :current ver 
                         :versions (assoc previous-versions ver new-version)))]
        (assoc-in db [:apps path] app)))

(defn get-app
    "Gets the data from the app"
    ([db path version]
        (let [apps (get db :apps {})]
            (if (not (contains? apps path))
                (throw+ {:type :notfound :sub-type :path :path path :version version})
            (let [app-versions (get-in apps [path :versions] {})]
                (if (not (contains? app-versions version))
                    (throw+ {:type :notfound :sub-type :version :path path :version version})
                (get app-versions version))))))
    ([db path] (get-app path (get-in db [:apps path :current])))) ;; redirect to current version



