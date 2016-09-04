(ns elfeed-cljsrn.local-storage
  (:require [cljs.reader :as reader]))

(def storage (.-AsyncStorage (js/require "react-native")))
(def storage-key "elfeed-cljs:db")

(defn load
  ([on-success]
   (load on-success nil))
  ([on-success on-error]
   (-> storage
       (.getItem storage-key)
       (.then (fn [data]
                (on-success (cljs.reader/read-string (or data "")))))
       (.catch on-error))))

(defn save
  ([data]
   (save data nil nil))
  ([data on-success on-error]
   (-> storage
       (.setItem key (pr-str data))
       (.then on-success)
       (.catch on-error))))
