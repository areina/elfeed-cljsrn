(ns elfeed-cljsrn.db
  (:require [cljs.spec :as s]
            [elfeed-cljsrn.local-storage :as ls]))

;; spec of app-db
(s/def ::server string?)
(s/def ::app-db
  (s/keys :req-un [::server]))

;; initial state of app-db
(def app-db {:update-time 0
             :server "http://localhost:8080"
             :entries '()})

(def ls-db-key "elfeed-cljs")

(defn db->ls! [db]
  (let [whitelist '(:entries :entries-m :server :update-time)
        data (pr-str (select-keys db whitelist))]
    (ls/set-item ls-db-key data #())))
