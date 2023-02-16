(ns elfeed-cljsrn.android.scenes.configure-server
  (:require [reagent.core :as r]
            [reagent.react-native :as rn]
            [reagent.react-native-paper :as paper]
            [re-frame.core :refer [subscribe dispatch]]
            [elfeed-cljsrn.events]
            [elfeed-cljsrn.subs]))

(defn header [^js theme]
  (let [title "Configure your Elfeed server"]
    [rn/view {:style {:flex 2.5
                      :background-color (.-primary ^js (.-colors theme))
                      :justify-content "flex-end"
                      :padding-left 28
                      :padding-bottom 10}}
     [paper/text {:variant "headlineSmall"
                  :style {:color "#FFF"}} title]]))

(defn footer [{:keys [on-press button-disabled?]}]
  [rn/view {:style {:flex 0.5
                    :align-items "flex-end"}}
   [paper/button {:on-press on-press
                  :mode "contained-tonal"
                  :disabled button-disabled?} "NEXT"]])

(defn body [server url]
  (let [has-error? (boolean (seq (:error-message server)))]
    [rn/view {:style {:flex 3.5}}
     [paper/text {:variant "bodyMedium"} "Please, check that your Elfeed server is running and accessible and enter the url."]
     [paper/text-input {:style {:margin-top 20}
                        :placeholder "http://"
                        :label "Elfeed url:"
                        :error has-error?
                        :keyboard-type "url"
                        :on-change-text (fn [text]
                                          (reset! url text)
                                          (r/flush))
                        :value @url}]
     [paper/helper-text {:type "error" :visible has-error?} (:error-message server)]]))

(defn scene [server-info input-url]
  (let [theme ^js (paper/use-theme-hook)]
    [rn/view {:style {:background-color (.-background (.-colors theme))
                      :flex 1}}
     [header theme]
     [rn/view {:style {:flex 4
                       :padding-horizontal 28
                       :padding-vertical 20}}
      [body server-info input-url]
      [footer {:on-press (fn [_e] (dispatch [:save-server @input-url]))
               :button-disabled? (empty? @input-url)}]]]))

(defn configure-server-scene [^js _props]
  (let [server-info (subscribe [:server])
        input-url (r/atom "http://")]
    (fn [_props]
      [:f> scene @server-info input-url])))
