(ns elfeed-cljsrn.db
  (:require [cljs.spec :as s]
            [elfeed-cljsrn.local-storage :as ls]))

;; spec of app-db
;; TODO Update the spec
(s/def ::server string?)
(s/def ::app-db
  (s/keys :req-un [::server]))

;; initial state of app-db
(def app-db {:nav nil
             :drawer {:open? false
                      :ref nil}
             :update-time 0
             :server {:url nil :valid? nil :checking? nil}
             :entries nil})
