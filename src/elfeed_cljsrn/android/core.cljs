(ns elfeed-cljsrn.android.core
  (:require [reagent.core :as r :refer [atom]]
            [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [elfeed-cljsrn.rn :as rn]
            [elfeed-cljsrn.events]
            [elfeed-cljsrn.subs])
  (:import [goog.i18n DateTimeFormat]))

(def MaterialIcons (js/require "react-native-vector-icons/MaterialIcons"))
(def icon (r/adapt-react-class MaterialIcons))

(def codePush (js/require "react-native-code-push"))

(defn go-back! [navigation-state]
  (if (zero? (:index navigation-state))
    false
    (do
      (dispatch [:nav/pop nil])
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

(defn remote-error-message []
  [rn/view {:style {:padding 10
                    :background-color "#fff9c4"}}
   [rn/text "Network error. Check your wifi or your elfeed server."]])

(defn settings-panel []
  (let [server (subscribe [:server])
        styles {:wrapper {:margin-top 72
                          :padding-left 16}}]
    (fn []
      [rn/view {:style (:wrapper styles)}
       [rn/text "Elfeed web url: "]
       [rn/text-input {:style {}
                       :keyboard-type "url"
                       :on-change-text (fn [text]
                                         (dispatch-sync [:update-server-value text])
                                         (r/flush))
                       :value @server}]])))

(defn entry-quick-actions [entry]
  (let [styles {:wrapper {:flex 1
                          :flex-direction "row"
                          :justify-content "flex-end"
                          :align-items "center"
                          :padding-right 14}
                :icon {:color "#00796b"}}]
    [rn/view {:style (:wrapper styles)}
     [rn/touchable {:on-press #(dispatch [:mark-entry-as-read entry])}
      [rn/view {}
       [icon {:style (:icon styles) :name "archive" :size 22}]]]]))

(defn entry-panel [entry]
  (let [loading? (subscribe [:fetching-entry?])
        remote-error (subscribe [:remote-error :entry])
        entry-content (subscribe [:current-entry])
        styles {:wrapper {:flex 1
                          :background-color "#FFFFFF"
                          :margin-top 56}
                :header {:margin-bottom 10
                         :padding-top 10
                         :padding-left 10
                         :padding-bottom 10
                         :border-bottom-color "#CCCCCC"
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
                               :background-color (if (:unread? entry) "#FFFFFF" "#F5F5F5")
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
                   :underlay-color "#f5f5f5"
                   :on-press (fn [_]
                               (dispatch [:fetch-entry-content entry])
                               (dispatch [:nav/push {:key :entry :title ""}]))}
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
  (let [styles {:wrapper {:padding-left 16}
                :value {:font-weight "500"}}]
    [rn/view {:style (:wrapper styles)}
     [rn/text {:style (:value styles)}
      (str "Last update at: ") (format-update-time update-time)]]))

(defn entries-panel []
  (let [loading (subscribe [:loading?])
        update-time (subscribe [:update-time])
        remote-error (subscribe [:remote-error :entries])
        entries (subscribe [:entries])
        recent-reads (subscribe [:recent-reads])
        styles {:wrapper {:flex 1
                          :flex-direction "row"
                          :margin-top 56
                          :background-color "#FFFFFF"}
                :loading-info {:margin 10}
                :list {:margin-top 8
                       :padding-bottom 8}
                :separator {:height 1
                            :background-color "#E0E0E0"}}]
    (fn []
      (let [datasource (.cloneWithRowsAndSections
                        (rn/ReactNative.SwipeableListView.getNewDataSource.)
                        (clj->js {:s1 (or @entries '())})
                        (clj->js '("s1")))
            hack @recent-reads]
        [rn/view {:style (:wrapper styles)}
         (if @loading
           [rn/view {:style (:loading-info styles)}
            [rn/text "Loading entries"]
            [rn/activity-indicator]]
           [rn/view {:style {:flex 1}}
            (when @remote-error
              [remote-error-message])
            [rn/swipeable-list-view {:dataSource datasource
                                     :max-swipe-distance 50
                                     :bounceFirstRowOnMount false
                                     :refresh-control (r/as-element [rn/refresh-control {:refreshing false
                                                                                         :on-refresh (fn []
                                                                                                       (dispatch [:fetch-update-time])
                                                                                                       (dispatch [:fetch-entries]))}])
                                     :style (:list styles)
                                     :enableEmptySections true
                                     :render-section-header (fn [_ _]
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
                                                                                 :style (:separator styles)}]))}]])]))))

(defn drawer-component []
  (let [styles {:section {}
                :header {:background-color "#004d40"
                         :height 120
                         :margin-bottom 8 }
                :item {:flex 1
                       :flex-direction "row"
                       :align-items "center"
                       :height 48
                       :padding-left 16}
                :icon {:padding 1}
                :value {:font-size 14
                        :color "#000000"
                        :margin-left 28}}]
    [rn/view {:style {:flex 1
                      :background-color "#FFFFFF"}}
     [rn/view {:style (:header styles)}]
     [rn/view {:style {:margin-top 8}}
      [rn/touchable {:on-press (fn []
                                 (dispatch [:drawer/close nil]))}
       [rn/view {:style (:item styles)}
        [icon {:style (:icon styles) :name "rss-feed" :size 22}]
        [rn/text {:style (:value styles)} "All entries" ]]]
      [rn/touchable {:on-press (fn []
                                 (dispatch [:drawer/close nil])
                                 (dispatch [:nav/push {:key :settings :title "Settings"}]))}
       [rn/view {:style (:item styles)}
        [icon {:style (:icon styles) :name "settings" :size 20}]
        [rn/text {:style (:value styles)} "Settings" ]]]]]))

(defn navbar-title [title]
  (let [styles {:title {:width 200
                        :color "#e0f2f1"
                        :font-size 20
                        :font-weight "500"
                        :margin-vertical 15}}]
    [rn/text {:style (:title styles)} title]))

(defn entry-actions-button []
  (let [ref-icon (r/atom nil)
        styles {:button {:padding-vertical 16
                         :padding-horizontal 8}
                :icon {:color "#e0f2f1"}}
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

(defmethod scene :entries [scene-props] [entries-panel])
(defmethod scene :entry [scene-props] [entry-panel])
(defmethod scene :settings [scene-props] [settings-panel])

(defn nav-title [scene-props]
  (let [title (aget scene-props "scene" "route" "title")]
    [rn/navigation-header-title {:text-style {:color "#e0f2f1"}} title]))

(defn nav-left-button [scene-props]
  (let [index (aget scene-props "scene" "index")
        styles {:button-container {:flex 1
                                   :flex-direction "row"
                                   :align-items "center"
                                   :justify-content "center"}
                :button {:color "#e0f2f1"
                         :height 24
                         :width 24
                         :margin 16}}
        root? (= index 0)
        icon-name (if root? "menu" "arrow-back")
        on-press (if root? #(dispatch [:drawer/open nil]) #(dispatch [:nav/pop nil]))]
    [rn/touchable-opacity {:style (:button-container styles) :on-press on-press}
     [icon {:style (:button styles) :name icon-name :size 24}]]))

(defn nav-right-button [scene-props]
  (when (= (aget scene-props "scene" "route" "key") "entry")
    [entry-actions-button]))

(defn header [scene-props]
  [rn/navigation-header (assoc
                         scene-props
                         :style {:background-color "#00796b"}
                         :render-title-component #(r/as-element [nav-title %])
                         :render-left-component #(r/as-element [nav-left-button %])
                         :render-right-component #(r/as-element [nav-right-button %]))])

(defn app-root []
  (let [nav (subscribe [:nav/state])]
    (r/create-class
     {:component-did-mount (fn []
                             (listen-back-button! nav)
                             (.sync codePush))
      :component-will-unmount #(unlisten-back-button! nav)
      :display-name "AppRoot"
      :reagent-render
      (fn []
        [rn/drawer-layout {:drawer-width 300
                           :drawer-position (.. rn/ReactNative -DrawerLayoutAndroid -positions -Left)
                           :render-navigation-view #(r/as-element [drawer-component])
                           :ref (fn [ref-drawer]
                                  (dispatch [:drawer/set ref-drawer]))}
         [rn/navigation-card-stack {:on-navigate-back #(dispatch [:nav/pop nil])
                                    :render-overlay #(r/as-element [header (js->clj % :keywordize-keys true)])
                                    :navigation-state @nav
                                    :style {:flex 1}
                                    :render-scene #(r/as-element [scene %])}]])})))

(defn init []
  (dispatch-sync [:initialize-db])
  (.registerComponent rn/app-registry "ElfeedCljsrn" #(r/reactify-component app-root)))
