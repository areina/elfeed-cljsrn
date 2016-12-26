(ns elfeed-cljsrn.db
  (:require [cljs.spec :as s]
            [elfeed-cljsrn.local-storage :as ls]))

;; spec of app-db

(s/def :route/key keyword?)
(s/def :route/title string?)
(s/def ::route (s/keys :req-un [:route/key :route/title]))

(s/def :nav/routes (s/+ ::route))
(s/def :nav/index int?)
(s/def ::nav (s/keys :req-un [:nav/index :nav/routes]))

(s/def :drawer/open? boolean?)
(s/def :drawer/ref? string?)
(s/def ::drawer (s/keys :req-un [:drawer/open? :drawer/ref]))

(s/def ::update-time int?)

(s/def :server/url string?)
(s/def :server/valid? boolean?)
(s/def :server/checking? boolean?)
(s/def :server/error-message string?)
(s/def ::server (s/keys :req-un [:server/url :server/valid? :server/checking?]
                        :opt-un [:server/error-message]))

(s/def ::entry map?)
(s/def ::entries (s/* string?))

(s/def ::current-entry string?)

(s/def ::connected? boolean?)

(s/def ::app-db
  (s/keys :req-un [::nav ::drawer ::update-time ::server ::entries ::connected?]
          :opt-un [::current-entry]))

;; initial state of app-db
(def app-db {:nav nil
             :drawer {:open? false
                      :ref nil}
             :update-time 0
             :server {:url nil :valid? nil :checking? nil}
             :search {:searching? false :term nil :default-term "@15-days-old +unread"}
             :entries nil})
