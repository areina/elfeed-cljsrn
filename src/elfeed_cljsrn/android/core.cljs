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

(def current-navigator (atom nil))

(defn update-current-navigator! [nav]
  (reset! current-navigator nav))

(defn go-back! []
  (if (and @current-navigator
           (> (count (.getCurrentRoutes @current-navigator)) 1))
    (do
      (.pop @current-navigator)
      true)
    false))

(defn listen-back-button! []
  (.addEventListener (.-BackAndroid rn/ReactNative) "hardwareBackPress" go-back!))

(defn unlisten-back-button! []
  (.removeEventListener (.-BackAndroid rn/ReactNative) "hardwareBackPress" go-back!))

(defn rn-datasource []
  (rn/ReactNative.ListView.DataSource. (clj->js {:rowHasChanged (fn [r1 r2] (not (= r1 r2)))})))

(defn format-entry-date [date]
  (let [js-date (js/Date. date)]
    (.format (goog.i18n.DateTimeFormat. "dd/MM/yyyy") js-date)))

(defn format-update-time [time]
  (let [js-date (js/Date. (* time 1000))]
    (.format (goog.i18n.DateTimeFormat. "dd/MM/yyyy hh:mm") js-date)))

(defn settings-panel [navigator]
  (let [server (subscribe [:server])
        styles {:wrapper {:margin-top 72
                          :padding-left 16}}]
    (fn [navigator]
      [rn/view {:style (:wrapper styles)}
       [rn/text "Elfeed web url: "]
       [rn/text-input {:style {}
                       :keyboard-type "url"
                       :on-change-text (fn [text]
                                         (dispatch-sync [:update-server-value text])
                                         (r/flush))
                       :value @server}]])))

(defn remote-error-message []
  [rn/view {:style {:padding 10
                    :background-color "#fff9c4"}}
   [rn/text "Network error. Check your wifi or your elfeed server."]])

(defn entry-panel [navigator entry]
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
    (fn [navigator entry]
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
                        :source {:html (:content-body @entry-content)}}]
          )]])))

(defn entry-row [entry]
  (let [styles {:list-wrapper {:flex-direction "row"
                               :background-color (when-not (:unread? entry) "#F5F5F5")
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
                               (dispatch [:set-active-panel :entry-panel])
                               (when @current-navigator
                                 (.push @current-navigator (clj->js {:name :entry-panel :index 2}))))}
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

(defn entries-panel [navigator]
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
    (fn [navigator]
      (let [datasource (.cloneWithRows (rn-datasource) (clj->js (or @entries '())))
            hack @recent-reads]
        [rn/view {:style (:wrapper styles)}
         (if @loading
           [rn/view {:style (:loading-info styles)}
            [rn/text "Loading entries"]
            [rn/activity-indicator]]
           [rn/view {:style {:flex 1}}
            (when @remote-error
              [remote-error-message])
            [rn/list-view {:dataSource datasource
                           :refresh-control (r/as-element [rn/refresh-control {:refreshing false
                                                                               :on-refresh (fn []
                                                                                             (dispatch [:fetch-update-time])
                                                                                             (dispatch [:fetch-entries]))}])
                           :style (:list styles)
                           :enableEmptySections true
                           :render-section-header (fn [_ _]
                                                    (when (> @update-time 0)
                                                      (r/as-element [update-time-info @update-time])))
                           :render-row (fn [data]
                                         (let [entry-data (js->clj data :keywordize-keys true)
                                               unread? (and (boolean (some #{"unread"} (:tags entry-data)))
                                                            (not (boolean (some #{(:webid entry-data)} @recent-reads))))]
                                           (r/as-element [entry-row (merge  entry-data {:unread? unread?})])))
                           :render-separator (fn [section-id row-id _]
                                               (r/as-element [rn/view {:key (str section-id "-" row-id)
                                                                       :style (:separator styles)}]))}]])]))))

(defmulti panels identity)
(defmethod panels :entries-panel [_ navigator] [entries-panel])
(defmethod panels :entry-panel [_ navigator] [entry-panel])
(defmethod panels :settings-panel [_ navigator] [settings-panel])

(defn drawer-component [drawer navigator]
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
                                 (.closeDrawer drawer)
                                 (when @current-navigator
                                   (let [current-route (js->clj (first (.getCurrentRoutes @current-navigator)) :keywordize-keys true)]
                                     (when-not (= (keyword (:name current-route)) :entries-panel)
                                       (dispatch [:fetch-entries])
                                       (dispatch [:set-active-panel :entries-panel])
                                       (.push @current-navigator (clj->js {:name :entries-panel :index 1}))))))}
       [rn/view {:style (:item styles)}
        [icon {:style (:icon styles) :name "rss-feed" :size 22}]
        [rn/text {:style (:value styles)} "All entries" ]]]
      [rn/touchable {:on-press (fn []
                                 (.closeDrawer drawer)
                                 (dispatch [:set-active-panel :settings-panel])
                                 (when @current-navigator
                                   (.push @current-navigator (clj->js {:name :settings-panel :index 2}))))}
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

(defn drawer-button [drawer]
  (let [styles {:button {:padding 16}
                :icon {:color "#e0f2f1"}}]
    [rn/touchable {:on-press (fn [e]
                               (.openDrawer drawer))}
     [rn/view {:style (:button styles)}
      [icon {:style (:icon styles) :name "menu" :size 24}]]]))

(defn back-button [navigator]
  (let [styles {:button {:padding 16}
                :icon {:color "#e0f2f1"}}]
    [rn/touchable {:on-press (fn [e]
                               (go-back!))
                   }
     [rn/view {:style (:button styles)}
      [icon {:style (:icon styles) :name "arrow-back" :size 24}]]]))

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

(defn route-mapper-element [element route navigator drawer]
  (let [mapping {:entries-panel {:title [navbar-title "All entries"]
                                 :left-button [drawer-button drawer]
                                 :right-button nil}
                 :settings-panel {:title [navbar-title "Settings"]
                                  :left-button [back-button navigator]
                                  :right-button nil}
                 :entry-panel {:title nil
                               :left-button [back-button navigator]
                               :right-button [entry-actions-button]}}
        route-keyword (keyword (:name (js->clj route :keywordize-keys true)))]
    (get-in mapping [route-keyword element])))

(defn route-mapper [drawer]
  {:LeftButton (fn [route navigator index nav-state]
                 (r/as-element [route-mapper-element :left-button route navigator drawer]))
   :RightButton (fn [route navigator index nav-state]
                  (r/as-element [route-mapper-element :right-button route navigator drawer]))
   :Title (fn [route navigator index nav-state]
            (r/as-element [route-mapper-element :title route navigator drawer]))})

(defn app-root []
  (let [drawer (subscribe [:android-drawer])
        styles {:navigation-bar {:container {:backgroundColor "#00796b"
                                             :elevation 4
                                             :height 56
                                             :margin 0
                                             :padding 0
                                             :flex-direction "row"
                                             :align-items "center"}}}]
    (r/create-class
     {:component-did-mount (fn []
                             (listen-back-button!)
                             (.sync codePush))
      :component-will-unmount #(unlisten-back-button!)
      :display-name "AppRoot"
      :reagent-render
      (fn []
        [rn/drawer-layout {:drawer-width 300
                           :drawer-position (.. rn/ReactNative -DrawerLayoutAndroid -positions -Left)
                           :render-navigation-view #(r/as-element [drawer-component @drawer nil ;; navigator
                                                                   ])
                           :ref (fn [ref-drawer]
                                  (dispatch [:set-android-drawer ref-drawer]))}
         [rn/navigator {:initial-route   {:name :entries-panel :index 1}
                        :configure-scene (fn [route _]
                                           (if (= "settings-panel" (:name (js->clj route :keywordize-keys true)))
                                             (.. rn/ReactNative -Navigator -SceneConfigs -FloatFromBottomAndroid)
                                             (.. rn/ReactNative -Navigator -SceneConfigs -HorizontalSwipeJump)))
                        :scene-style {:background-color "#FFFFFF"}
                        :navigation-bar (r/as-element
                                         [rn/navigation-bar {:style (:container (:navigation-bar styles))
                                                             :route-mapper (clj->js (route-mapper @drawer))}])
                        :render-scene    (fn [route nav]
                                           (update-current-navigator! nav)
                                           (let [clj-route (js->clj route)
                                                 panel (keyword (get clj-route "name"))]
                                             (r/as-element [panels panel nav])))}]])})))

(defn init []
  (dispatch-sync [:initialize-db])
  (.registerComponent rn/app-registry "ElfeedCljsrn" #(r/reactify-component app-root)))
