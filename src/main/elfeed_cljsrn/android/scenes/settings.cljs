(ns elfeed-cljsrn.android.scenes.settings
  (:require
   [reagent.react-native-paper :as paper]
   [reagent.react-native :as rn]
   [reagent.core :as r]
   [re-frame.core :refer [subscribe dispatch]]
   [elfeed-cljsrn.events]
   [elfeed-cljsrn.subs]))

;; TODO: add a snackbar to give feedback when the server is updated

(defn settings-scene-int [url _error]
  (let [server-url (r/atom url)
        styles {:wrapper {:flex 1
                          :padding-top 16
                          :padding-horizontal 16}}]
    (fn [_url error]
      [rn/view {:style (:wrapper styles)}
       [paper/text-input {:mode "outlined"
                          :label "Elfeed web url"
                          :keyboard-type "url"
                          :error (boolean (seq error))
                          :on-change-text (fn [text]
                                            (reset! server-url text)
                                            (r/flush))
                          :value @server-url}]
       [paper/helper-text {:type "error" :visible (boolean (seq error))} error]
       [paper/button {:on-press (fn [_e] (dispatch [:update-server @server-url]))} "UPDATE SERVER"]])))

(defn settings-scene []
  (let [server @(subscribe [:server])]
    (when (:url server)
      [settings-scene-int (:url server) (:error-message server)])))
