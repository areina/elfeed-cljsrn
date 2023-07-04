(ns elfeed-cljsrn.effects
  (:require [elfeed-cljsrn.local-storage :as ls]
            ["react-native" :as rn]
            ["@react-native-community/netinfo" :default NetInfo]
            ["react-native-splash-screen" :default SplashScreen]
            [re-frame.core :refer [dispatch reg-fx]]))

(defn add-net-listener [listener]
  (.addEventListener NetInfo listener))

(defn hide-splash-screen []
  (.hide SplashScreen))

(defn open-url [url]
  (.openURL rn/Linking url))

(reg-fx
 :add-netinfo-listener
 (fn [listener]
   (add-net-listener listener)))

(reg-fx
 :get-localstore
 (fn [localstore-fx]
   (ls/load #(dispatch (conj (:on-success localstore-fx) %)))))

(reg-fx
 :hide-splash-screen
 (fn []
   (hide-splash-screen)))

(reg-fx
 :open-url
 (fn [url]
   (open-url url)))
