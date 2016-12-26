(ns elfeed-cljsrn.ui
  (:require [reagent.core :as r :refer [atom]]
            [elfeed-cljsrn.rn :as rn]))

(def icon (r/adapt-react-class (js/require "react-native-vector-icons/MaterialIcons")))

(def colors {:white "#FFFFFF"
             :grey50 "#FAFAFA"
             :grey100 "#F5F5F5"
             :grey200 "#EEEEEE"
             :grey300 "#E0E0E0"
             :grey400 "#BDBDBD"
             :grey600 "#757575"
             :grey900 "#212121"
             :red500 "#F44336"
             :teal100 "#B2DFDB"
             :teal500 "#009688"
             :teal700 "#00796B"})
(def palette {:dark-primary (:teal700 colors)
              :primary (:teal500 colors)
              :light-primary (:teal100 colors)
              :accent "#CDDC39"
              :text (:white colors)
              :primary-text (:grey900 colors)
              :secondary-text (:grey600 colors)
              :divider (:grey400 colors)
              :error (:red500 colors)})

(defn button [props label]
  (let [styles (merge-with merge {:wrapper {:margin-top 8
                                            :border-radius 2
                                            :background-color (:secondary-text palette)
                                            :padding 10}
                                  :text {:color (:text palette)}} (:style props))]
    [rn/touchable {:on-press (:on-press props) :disabled (:disabled? props)}
     [rn/view {:style (:wrapper styles)}
      [rn/text {:style (:text styles)} label]]]))

(defn header-icon-button [icon-name props]
  (let [styles (merge-with merge {:button {:margin-vertical 16
                                           :margin-horizontal 12}
                                  :icon {:color (:text palette)}} (:style props))]
    [rn/touchable-opacity {:on-press (:on-press props)}
     [rn/view {:style (:button styles)}
      [icon {:style (:icon styles) :name icon-name :size 24}]]]))
