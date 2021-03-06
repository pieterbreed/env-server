(ns env-server.applications
    (:require [joda-time :as time])
    (:use [slingshot.slingshot :only [throw+]]))

;;; the main database (for now)
;;; all in-memory
(def apps-db (atom nil))

(defn -create-app-hash
    "creates a value-based hash of the data in the app"
    [data]
    (-> data hash str))

(defn -create-meta-info
    "creates some meta information that is not idempotent"
    [path data]
    (let [ver (-create-app-hash data)
          now (-> (time/date-time) time/as-map)]
          {:version-nr ver
           :created now}))

(defn -get-app-or-error
    "Returns the app indicated by path or an error"
    [db path]
    (let [apps (get db :apps {})]
        (if (not (contains? apps path))
            (throw+ {:type ::notfound :sub-type ::path :path path})
            (get apps path))))

(defn -get-app-version-or-error
    "returns the specific app version record or an error"
    [db path version]
    (let [app (-get-app-or-error db path)
          versions (get app :versions {})]
        (if (not (contains? versions version))
            (throw+ {:type ::notfound :sub-type ::version :path path :version version})
            (get versions version))))

(defn post-app
  "Add the application identified by path to the db.

Optionally: add a based-on vector [path version] which, if specified, will be resolved if it can (else failure) and the data for that app will be the base-data for this app. Ie, the new apps result data will be the result of merging the base-app's data with the newly specified data"
    [db path data & [based-on]]
    (let [base-app (when-let [based-on based-on]
                     (apply -get-app-version-or-error db based-on))
          app (get-in db [:apps path] {})
          previous-versions (get app :versions {})
          previous-version (get app :current nil)
          meta (assoc (-create-meta-info path data)
                 :previous-version previous-version)
          new-version {:meta meta
                       :based-on base-app
                       :data data}
          ver (:version-nr meta)
          app (-> app
                  (assoc :current ver 
                         :versions (assoc previous-versions ver new-version)))]
      (assoc-in db [:apps path] app)))

(defn get-app
    "Gets the data from the app"
    ([db path version]
        (-get-app-version-or-error db path version))
    ([db path] 
        (let [app (-get-app-or-error db path)]
            (throw+ {:type ::bad-request :sub-type ::version-not-specified :current-version (:current app)}))))

(defn get-app-versions
    "gets the list of version from the app, sorted desc by creation timestamp. An app can be based on another app, provided the specification for the other app includes both the path and the version string"
    [db path]
    (let [app (-get-app-or-error db path)
          versions (->> (:versions app)
                        (map #(get % 1))
                        (map #(get % :meta))
                        (map #(hash-map :version-nr (:version-nr %)
                                        :created (time/date-time (:created %))))
                        (sort-by :created time/after?)
                        (map :version-nr))]
        versions))



