(ns elfeed-cljsrn.components
  (:require [reagent.react-native-paper :as paper]))

(defn button [props & children]
  (let [default-props {:content-style {:height 48}}
        merged-props (merge default-props props)]
    [paper/button merged-props children]))

(defn connection-error []
  [paper/snackbar {:visible true} "No connection"])

(defn header-icon-button [{:keys [icon color on-press]}]
  [paper/icon-button {:icon icon
                      :icon-color color
                      :on-press on-press}])

(defn remote-error-message [{:keys [navigation]}]
  [paper/banner
   {:visible true
    :actions [{:label "Go to Settings"
               :onPress (fn [_e] (.navigate navigation "Settings"))}
              {:label "Retry"
               :onPress (fn [_e])}]}
   "There seem to be a problem connecting to your Elfeed server."])
