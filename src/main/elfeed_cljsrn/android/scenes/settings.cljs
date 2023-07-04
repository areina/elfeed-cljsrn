(ns elfeed-cljsrn.android.scenes.settings
  (:require
   [reagent.react-native-paper :as paper]
   [reagent.react-native :as rn]
   [reagent.core :as r]
   [re-frame.core :refer [subscribe dispatch]]
   [elfeed-cljsrn.events]
   [elfeed-cljsrn.subs]))

;; TODO: add a snackbar to give feedback when the server is updated

(defn settings [{:keys [url _error-message _on-update]}]
  (let [server-url (r/atom url)
        styles {:wrapper {:flex 1
                          :padding-top 16
                          :padding-horizontal 16}}]
    (fn [{:keys [_url error-message on-update]}]
      [rn/view {:style (:wrapper styles)}
       [paper/text-input {:mode "outlined"
                          :label "Elfeed web url"
                          :keyboard-type "url"
                          :error (boolean (seq error-message))
                          :on-change-text (fn [text]
                                            (reset! server-url text)
                                            (r/flush))
                          :value @server-url}]
       [paper/helper-text {:type "error" :visible (boolean (seq error-message))} error-message]
       [paper/button {:on-press (fn [_e] (on-update @server-url) )} "UPDATE SERVER"]])))

(defn settings-scene [{:keys [^js _navigation ^js _route]}]
  (let [server-info @(subscribe [:server])]
    (when (:url server-info)
      [settings {:url (:url server-info)
                 :error-message (:error-message server-info)
                 :on-update (fn [new-url] (dispatch [:update-server new-url]))}])))
