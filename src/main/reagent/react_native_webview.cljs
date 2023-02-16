(ns reagent.react-native-webview
  (:require ["react-native-webview" :default WebView]
            [reagent.core :as r]))

(def web-view (r/adapt-react-class WebView))
