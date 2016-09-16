(ns elfeed-cljsrn.android.core
  (:require [reagent.core :as r :refer [atom]]
            [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [elfeed-cljsrn.rn :as rn]
            [elfeed-cljsrn.navigation :refer [navigate-back navigate-to]]
            [elfeed-cljsrn.events]
            [elfeed-cljsrn.subs])
  (:import [goog.i18n DateTimeFormat]))

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

(def MaterialIcons (js/require "react-native-vector-icons/MaterialIcons"))
(def icon (r/adapt-react-class MaterialIcons))

(defn go-back! [navigation-state]
  (if (zero? (:index navigation-state))
    false
    (do
      (navigate-back)
      true)))

(defn listen-back-button! [navigation-state]
  (.addEventListener (.-BackAndroid rn/ReactNative) "hardwareBackPress" #(go-back! @navigation-state)))

(defn unlisten-back-button! [navigation-state]
  (.removeEventListener (.-BackAndroid rn/ReactNative) "hardwareBackPress" #(go-back! @navigation-state)))

(defn format-entry-date [date]
  (let [js-date (js/Date. date)]
    (.format (goog.i18n.DateTimeFormat. "dd/MM/yyyy") js-date)))

(defn format-update-time [time]
  (let [js-date (js/Date. (* time 1000))]
    (.format (goog.i18n.DateTimeFormat. "dd/MM/yyyy hh:mm") js-date)))

(defn button [props label]
  (let [styles (merge-with merge {:wrapper {:margin-top 8
                                            :border-radius 2
                                            :background-color (:secondary-text palette)
                                            :padding 10}
                                  :text {:color (:text palette)}} (:style props))]
    [rn/touchable {:on-press (:on-press props) :disabled (:disabled? props)}
     [rn/view {:style (:wrapper styles)}
      [rn/text {:style (:text styles)} label]]]))

(defn remote-error-message []
  [rn/view {:style {:padding 10
                    :background-color "#fff9c4"}}
   [rn/text "Network error. Check your wifi or your elfeed server."]])

(defn configure-server-scene []
  (let [server (subscribe [:server])
        server-url (r/atom nil)
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
                :text-input {:margin-top 15}
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
          [rn/text-input {:style (:text-input styles)
                          :placeholder "Enter your Elfeed URL:"
                          :underline-color-android (when (false? (:valid? @server)) (:error palette))
                          :keyboard-type "url"
                          :on-change-text (fn [text]
                                            (reset! server-url text)
                                            (r/flush))
                          :value @server-url}]
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

(defn entry-quick-actions [entry]
  (let [styles {:wrapper {:flex 1
                          :flex-direction "row"
                          :justify-content "flex-end"
                          :align-items "center"
                          :padding-right 14}
                :icon {:color (:dark-primary palette)}}]
    [rn/view {:style (:wrapper styles)}
     [rn/touchable {:on-press #(dispatch [:mark-entry-as-read entry])}
      [rn/view {}
       [icon {:style (:icon styles) :name "archive" :size 22}]]]]))

(defn entry-scene [entry]
  (let [loading? (subscribe [:fetching-entry?])
        remote-error (subscribe [:remote-error :entry])
        entry-content (subscribe [:current-entry])
        styles {:wrapper {:flex 1}
                :header {:margin-bottom 10
                         :padding-vertical 10
                         :padding-horizontal 10
                         :border-bottom-color (:divider palette)
                         :border-bottom-width 1}
                :loading-content {:flex 1
                                  :padding-left 10
                                  :justify-content "center"
                                  :align-items "center"}
                :content {}
                :web-view {:height 600}}
        ;; content-height (r/atom 200)
        ]
    (fn [entry]
      [rn/view {:style (:wrapper styles)}
       (when @remote-error
         [remote-error-message])
       [rn/view {:style (:header styles)}
        [rn/text (:title @entry-content)]
        [rn/text (str "» " (:title (:feed @entry-content)))]]
       [rn/view {:style (if @loading? (:loading-content styles) (:content styles))}
        (if @loading?
          [rn/activity-indicator]
          [rn/web-view {:style (:web-view styles)
                        ;; :injectedJavaScript "document.body.scrollHeight;"
                        ;; :onNavigationStateChange (fn [event]
                        ;;                            (println (str "HOLA: " (.-jsEvaluationValue event)))
                        ;;                            (reset! content-height (.-jsEvaluationValue event))
                        ;;                            )
                        :javaScriptEnabled true
                        :scrollEnabled false
                        :automaticallyAdjustContentInsets true
                        :source {:html (:content-body @entry-content)}}])]])))

(defn entry-row [entry]
  (let [styles {:list-wrapper {:flex-direction "row"
                               :background-color (if (:unread? entry) (:white colors) (:grey100 colors))
                               :padding-left 16
                               :padding-right 16
                               :height 72
                               :align-items "center"}
                :first-line {:flex-direction "row"}
                :primary-text-wrapper {:flex 1
                                       :padding-right 16}
                :primary-text {:font-size 16
                               :font-weight "400"
                               :line-height 24}
                :caption-text-wrapper {:align-self "flex-start"
                                       :align-items "flex-start"}
                :caption-text {:font-size 12
                               :font-weight "400"
                               :line-height 20}
                :secondary-text-wrapper {}
                :secondary-text {:line-height 22
                                 :font-size 14
                                 :color "rgba(0,0,0,.54)"}}]
    [rn/touchable {:key (:webid entry)
                   :underlay-color (:grey-100 colors)
                   :on-press (fn [_]
                               (dispatch [:fetch-entry-content entry])
                               (navigate-to :entry))}
     [rn/view {:style (:list-wrapper styles)}
      [rn/view {:style {:flex 1
                        :justify-content "center"}}
       [rn/view {:style (:first-line styles)}
        [rn/view {:style (:primary-text-wrapper styles)}
         [rn/text {:number-of-lines 1
                   :style (:primary-text styles)}
          (:title entry)]]
        [rn/view {:style (:caption-text-wrapper styles)}
         [rn/text {:style (:caption-text styles)} (format-entry-date (:date entry))]]]
       [rn/view
        [rn/text {:style (:secondary-text styles)} (str "»" (:title (:feed entry)))]]]]]))

(defn update-time-info [update-time]
  (let [styles {:wrapper {:background-color (:grey300 colors)
                          :padding-vertical 6
                          :padding-left 16}
                :text {:font-size 12
                       :font-weight "500"
                       :color (:secondary-text palette)}}]
    [rn/view {:style (:wrapper styles)}
     [rn/text {:style (:text styles)}
      (str "LAST UPDATE: ") (format-update-time update-time)]]))

(defn loading-component []
  (let [styles {:wrapper {:height 34
                          :align-items "center"
                          :padding-left 20
                          :flex-direction "row"
                          :background-color (:secondary-text palette)}
                :text {:margin-left 20 :color "white"}}]
    [rn/view {:style (:wrapper styles)}
     [rn/activity-indicator {:color "white"}]
     [rn/text {:style (:text styles)} "Loading entries..."]]))

(defn no-entries-component []
  (let [styles {:wrapper {:height 400
                          :justify-content "center"
                          :align-items "center"}
                :button {:margin-top 10
                         :border-radius 2
                         :background-color (:secondary-text palette)
                         :padding 10}
                :button-text {:color (:white colors)}}]
    [rn/view {:style (:wrapper styles)}
     [rn/text "There are no entries"]
     [button {:on-press #(dispatch [:fetch-content])} "REFRESH"]]))

(defn entries-scene []
  (let [loading (subscribe [:loading?])
        update-time (subscribe [:update-time])
        remote-error (subscribe [:remote-error :entries])
        entries (subscribe [:entries])
        recent-reads (subscribe [:recent-reads])
        styles {:wrapper {:flex 1}
                :list {:margin-top 0
                       :padding-bottom 0}
                :separator {:height 1
                            :background-color (:grey300 colors)}}]
    (fn []
      (let [datasource (.cloneWithRowsAndSections
                        (rn/ReactNative.SwipeableListView.getNewDataSource.)
                        (clj->js {:s1 (or @entries '())})
                        (clj->js '("s1")))
            hack @recent-reads]
        [rn/view {:style (:wrapper styles)}
         (when @loading
           [loading-component])
         [rn/view {:style {:flex 1}}
          (when @remote-error
            [remote-error-message])
          (when @entries
            (if-not (empty? @entries)
              [rn/swipeable-list-view {:dataSource datasource
                                       :max-swipe-distance 50
                                       :bounceFirstRowOnMount false
                                       :refresh-control (r/as-element [rn/refresh-control {:refreshing false
                                                                                           :on-refresh #(dispatch [:fetch-content])}])
                                       :style (:list styles)
                                       :enableEmptySections true
                                       :render-header (fn [_ _]
                                                        (when (> @update-time 0)
                                                          (r/as-element [update-time-info @update-time])))
                                       :render-quick-actions (fn [row-data section-id row-id]
                                                               (r/as-element [entry-quick-actions (js->clj row-data :keywordize-keys true)]))
                                       :render-row (fn [data section-id row-id]
                                                     (let [entry-data (js->clj data :keywordize-keys true)
                                                           unread? (and (boolean (some #{"unread"} (:tags entry-data)))
                                                                        (not (boolean (some #{(:webid entry-data)} @recent-reads))))]
                                                       (r/as-element [entry-row (merge  entry-data {:unread? unread?})])))
                                       :render-separator (fn [section-id row-id _]
                                                           (r/as-element [rn/view {:key (str section-id "-" row-id)
                                                                                   :style (:separator styles)}]))}]
              [no-entries-component]))]]))))

(defn drawer-component []
  (let [styles {:section {}
                :header {:background-color (:primary palette)
                         :height 120
                         :margin-bottom 8 }
                :item {:flex 1
                       :flex-direction "row"
                       :align-items "center"
                       :height 48
                       :padding-left 16}
                :icon {:padding 1}
                :value {:font-size 14
                        :color (:primary-text palette)
                        :margin-left 28}}]
    [rn/view {:style {:flex 1
                      :background-color (:white colors)}}
     [rn/view {:style (:header styles)}]
     [rn/view {:style {:margin-top 8}}
      [rn/touchable {:on-press (fn []
                                 (dispatch [:drawer/close nil]))}
       [rn/view {:style (:item styles)}
        [icon {:style (:icon styles) :name "rss-feed" :size 22}]
        [rn/text {:style (:value styles)} "All entries" ]]]
      [rn/touchable {:on-press (fn []
                                 (dispatch [:drawer/close nil])
                                 (navigate-to :settings))}
       [rn/view {:style (:item styles)}
        [icon {:style (:icon styles) :name "settings" :size 20}]
        [rn/text {:style (:value styles)} "Settings" ]]]]]))

(defn entry-actions-button []
  (let [ref-icon (r/atom nil)
        styles {:button {:padding-vertical 16
                         :padding-horizontal 8}
                :icon {:color (:text palette)}}
        actions [{:label "Open in browser"
                  :action (fn [] (dispatch [:open-entry-in-browser]))}]]
    (fn []
      [rn/view
       [rn/touchable {:on-press (fn [e]
                                  (.showPopupMenu rn/ui-manager (.findNodeHandle rn/ReactNative @ref-icon)
                                                  (clj->js (map :label actions))
                                                  (fn [] )
                                                  (fn [e i]
                                                    (when i
                                                      ((:action (nth actions i)))))))}
        [rn/view {:style (:button styles)}
         [icon {:style (:icon styles)
                :name "more-vert"
                :size 24
                :ref (fn [ref] (reset! ref-icon ref))}]]]])))

(defmulti scene #(keyword (aget % "scene" "route" "key")))

(defmethod scene :configure-server [scene-props] [configure-server-scene])
(defmethod scene :entries [scene-props] [entries-scene])
(defmethod scene :entry [scene-props] [entry-scene])
(defmethod scene :settings [scene-props] [settings-scene])

(defn nav-title [scene-props]
  (let [title (aget scene-props "scene" "route" "title")]
    [rn/navigation-header-title {:text-style {:color (:text palette)}} title]))

(defn nav-left-button [scene-props]
  (let [scene-key (aget scene-props "scene" "route" "key")
        styles {:button-container {:flex 1
                                   :flex-direction "row"
                                   :align-items "center"
                                   :justify-content "center"}
                :button {:color (:text palette)
                         :height 24
                         :width 24
                         :margin 16}}
        root? (= scene-key "entries")
        icon-name (if root? "menu" "arrow-back")
        on-press (if root? #(dispatch [:drawer/open nil]) #(navigate-back))]
    [rn/touchable-opacity {:style (:button-container styles) :on-press on-press}
     [icon {:style (:button styles) :name icon-name :size 24}]]))

(defn nav-right-button [scene-props]
  (when (= (aget scene-props "scene" "route" "key") "entry")
    [entry-actions-button]))

(defn header [scene-props]
  (when-not (or (= (:key (:route (:scene scene-props))) "configure-server")
                (= (:key (:route (:scene scene-props))) "success-configuration"))
    [rn/navigation-header (assoc
                           scene-props
                           :style {:background-color (:primary palette)}
                           :render-title-component #(r/as-element [nav-title %])
                           :render-left-component #(r/as-element [nav-left-button %])
                           :render-right-component #(r/as-element [nav-right-button %]))]))

(defn app-root []
  (let [nav (subscribe [:nav/state])]
    (r/create-class
     {:component-did-mount #(listen-back-button! nav)
      :component-will-unmount #(unlisten-back-button! nav)
      :display-name "AppRoot"
      :reagent-render
      (fn []
        [rn/drawer-layout {:drawer-width 300
                           :drawer-position (.. rn/ReactNative -DrawerLayoutAndroid -positions -Left)
                           :render-navigation-view #(r/as-element [drawer-component])
                           :ref (fn [ref-drawer]
                                  (dispatch [:drawer/set ref-drawer]))}
         [rn/status-bar {:background-color (:dark-primary palette)}]
         (when @nav
           [rn/navigation-card-stack {:on-navigate-back #(navigate-back)
                                      :render-header #(r/as-element [header (js->clj % :keywordize-keys true)])
                                      :navigation-state @nav
                                      :style {:flex 1}
                                      :card-style {:background-color (:white colors)}
                                      :render-scene #(r/as-element [scene %])}])])})))

(defn init []
  (dispatch-sync [:boot])
  (.registerComponent rn/app-registry "ElfeedCljsrn" #(r/reactify-component app-root)))
