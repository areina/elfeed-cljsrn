(ns elfeed-cljsrn.local-storage
  (:require
   ["@react-native-async-storage/async-storage" :default async-storage]
   [cljs.reader :as reader]))

(def storage-key "elfeed-cljs:db")

(defn load
  ([on-success]
   (load on-success nil))
  ([on-success on-error]
   (-> async-storage
       (.getItem storage-key)
       (.then (fn [data]
                (on-success (cljs.reader/read-string (or data "")))))
       (.catch on-error))))

(defn save
  ([data]
   (save data nil nil))
  ([data on-success on-error]
   (-> async-storage
       (.setItem storage-key (pr-str data))
       (.then on-success)
       (.catch on-error))))
