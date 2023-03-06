(ns reagent.react-native
  (:require ["react-native" :as rn]
            [reagent.core :as r]))

(def activity-indicator (r/adapt-react-class rn/ActivityIndicator))
(def flat-list (r/adapt-react-class rn/FlatList))
(def status-bar (r/adapt-react-class rn/StatusBar))
(def text-input (r/adapt-react-class rn/TextInput))
(def view (r/adapt-react-class rn/View))
