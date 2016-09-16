(ns elfeed-cljsrn.android.scenes.settings
  (:require [reagent.core :as r]
            [re-frame.core :refer [subscribe dispatch]]
            [elfeed-cljsrn.rn :as rn]
            [elfeed-cljsrn.ui :as ui :refer [colors palette button]]
            [elfeed-cljsrn.events]
            [elfeed-cljsrn.subs]))

(defn settings-scene []
  (let [server (subscribe [:server])
        server-url (r/atom (:url @server))
        styles {:wrapper {:flex 1
                          :padding-top 10
                          :padding-horizontal 16}
                :button {:wrapper {:background-color (:white colors)
                                   :margin-top 0
                                   :padding 6
                                   :width 106}
                         :text {:font-weight "bold"
                                :font-size 12
                                :color (:dark-primary palette)}}}]
    (fn []
      [rn/view {:style (:wrapper styles)}
       [rn/text "Elfeed web url: "]
       [rn/text-input {:style {}
                       :keyboard-type "url"
                       :underline-color-android (when (false? (:valid? @server)) (:error palette))
                       :on-change-text (fn [text]
                                         (reset! server-url text)
                                         (r/flush))
                       :value @server-url}]
       [button {:on-press #(dispatch [:update-server @server-url])
                :style (:button styles)} "UPDATE SERVER"]])))
