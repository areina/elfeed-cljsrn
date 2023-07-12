(ns elfeed-cljsrn.android.scenes.configure-server
  (:require [reagent.core :as r]
            [reagent.react-native :as rn]
            [reagent.react-native-paper :as paper]
            [re-frame.core :refer [subscribe dispatch]]
            [elfeed-cljsrn.components :refer [button]]
            [elfeed-cljsrn.events]
            [elfeed-cljsrn.subs]))

(defn ^:private header [^js theme]
  (let [title "Configure your Elfeed server"]
    [rn/view {:style {:flex 2.5
                      :background-color (.-primary ^js (.-colors theme))
                      :justify-content "flex-end"
                      :padding-left 28
                      :padding-bottom 10}}
     [paper/text {:variant "headlineSmall"
                  :style {:color (.-onPrimary ^js (.-colors theme))}} title]]))

(defn ^:private footer [{:keys [on-press button-disabled?]}]
  [rn/view {:style {:flex 0.5
                    :align-items "flex-end"}}
   [button {:on-press on-press
            :mode "contained-tonal"
            :disabled button-disabled?} "NEXT"]])

(defn ^:private body [{:keys [server-info input-url on-url-change]}]
  (let [has-error? (boolean (seq (:error-message server-info)))]
    [rn/view {:style {:flex 3.5}}
     [paper/text {:variant "bodyMedium"} "Please, check that your Elfeed server is running and accessible and enter the url."]
     [paper/text-input {:style {:margin-top 20}
                        :placeholder "http://"
                        :label "Elfeed url:"
                        :error has-error?
                        :keyboard-type "url"
                        :on-change-text (fn [new-url]
                                          (on-url-change new-url))
                        :value input-url}]
     [paper/helper-text {:type "error" :visible has-error?} (:error-message server-info)]]))

(defn configure-server [{:keys [server-info input-url on-url-change on-save]}]
  (let [theme ^js (paper/use-theme-hook)]
    [rn/view {:style {:background-color (.-background (.-colors theme))
                      :flex 1}}
     [header theme]
     [rn/view {:style {:flex 4
                       :padding-horizontal 28
                       :padding-vertical 20}}
      [body {:server-info server-info
             :input-url input-url
             :on-url-change on-url-change}]
      [footer {:on-press (fn [_e] (on-save input-url))
               :button-disabled? (empty? input-url)}]]]))

(defn configure-server-scene [{:keys [^js _navigation ^js _route]}]
  (let [server-info (subscribe [:server])
        input-url (r/atom "http://")
        on-url-change (fn [new-url]
                        (reset! input-url new-url)
                        (r/flush))
        on-save (fn [new-url]
                  (dispatch [:save-server new-url]))]
    (fn [{:keys [^js _navigation ^js _route]}]
      [:f> configure-server {:server-info @server-info
                             :input-url @input-url
                             :on-url-change on-url-change
                             :on-save on-save}])))
