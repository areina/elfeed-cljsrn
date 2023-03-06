(ns elfeed-cljsrn.navigation
  (:require [reagent.core :as r]
            ["@react-navigation/drawer" :refer [createDrawerNavigator]]
            ["@react-navigation/native" :refer [NavigationContainer]]
            ["@react-navigation/native-stack" :refer [createNativeStackNavigator]]))

(def container (r/adapt-react-class NavigationContainer))

(defn create-drawer-navigator []
  (let [^js drawer (createDrawerNavigator)]
    [(r/adapt-react-class (.-Navigator drawer))
     (r/adapt-react-class (.-Screen drawer))]))

(defn create-stack-navigator []
  (let [^js stack (createNativeStackNavigator)]
    [(r/adapt-react-class (.-Navigator stack))
     (r/adapt-react-class (.-Screen stack))]))

(def drawer-navigator (create-drawer-navigator))
(def stack-navigator (create-stack-navigator))
