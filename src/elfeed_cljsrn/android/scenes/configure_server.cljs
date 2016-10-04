(ns elfeed-cljsrn.android.scenes.configure-server
  (:require [reagent.core :as r]
            [re-frame.core :refer [subscribe dispatch]]
            [elfeed-cljsrn.rn :as rn]
            [elfeed-cljsrn.ui :as ui :refer [colors palette button]]
            [elfeed-cljsrn.events]
            [elfeed-cljsrn.subs]))

(defn configure-server-scene []
  (let [server (subscribe [:server])
        server-url (r/atom "http://")
        title "Configure your Elfeed server"
        styles {:wrapper {:background-color (:white colors)
                          :flex 1}
                :header {:height 180
                         :background-color (:primary palette)
                         :justify-content "flex-end"
                         :padding-left 28
                         :padding-bottom 10}
                :title {:font-size 20
                        :color "white"}
                :content  {:padding-horizontal 28
                           :padding-top 20
                           :height 342}
                :message {:flex-direction "row"
                          :flex-wrap "wrap"
                          :color (:primary-text palette)}
                :input {:margin-top 25}
                :text-input {:height 50}
                :button {:background-color (:grey300 colors)
                         :align-items "center"
                         :justify-content "center"
                         :margin-right 30
                         :margin-top 0
                         :width 72
                         :height 46}
                :footer {:height 46
                         :align-items "flex-end"
                         :background-color (:grey300 colors)}}]
    (fn []
      (let [button-disabled? (empty? @server-url)]
        [rn/view {:style (:wrapper styles)}
         [rn/view {:style (:header styles)}
          [rn/text {:style (:title styles)} title]]
         [rn/view {:style (:content styles)}
          [rn/text {:style (:message styles)} "Please, check that your Elfeed server is running and accessible and enter its url."]
          [rn/text {:style {:color (:primary palette)}} "Learn more"]
          [rn/view {:style (:input styles)}
           [rn/text "Enter your Elfeed URL:"]
           [rn/text-input {:style (:text-input styles)
                           :placeholder "http://"
                           :underline-color-android (when (false? (:valid? @server)) (:error palette))
                           :keyboard-type "url"
                           :on-change-text (fn [text]
                                             (reset! server-url text)
                                             (r/flush))
                           :value @server-url}]]
          (when-let [error (:error-message @server)]
            [rn/view
             [rn/text {:style {:font-size 12
                               :color (:error palette)}} error]])]
         [rn/view {:style (:footer styles)}
          [button {:on-press #(dispatch [:save-server @server-url])
                   :disabled? button-disabled?
                   :style {:wrapper (:button styles)
                           :text {:color (if button-disabled?
                                           (:secondary-text palette)
                                           (:primary-text palette))}}} "NEXT >"]]]))))
