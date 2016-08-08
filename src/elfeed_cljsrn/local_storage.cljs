(ns elfeed-cljsrn.local-storage)

(def ReactNative (js/require "react-native"))
(def storage (.-AsyncStorage ReactNative))

(defn get-item [key cb]
  (.getItem storage key cb))

(defn set-item [key value cb]
  (.setItem storage key value cb))

(defn remove-item [key cb]
  (.removeItem storage key cb))
