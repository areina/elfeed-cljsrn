(ns elfeed-cljsrn.android.core
  (:require [reagent.core :as r]
            [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [elfeed-cljsrn.rn :as rn]
            [elfeed-cljsrn.navigation :refer [navigate-back navigate-to]]
            [elfeed-cljsrn.ui :as ui :refer [colors palette icon header-icon-button]]
            [elfeed-cljsrn.android.scenes.configure-server :refer [configure-server-scene]]
            [elfeed-cljsrn.android.scenes.settings :refer [settings-scene]]
            [elfeed-cljsrn.android.scenes.entries :refer [entries-scene]]
            [elfeed-cljsrn.android.scenes.entry :refer [entry-scene]]
            [elfeed-cljsrn.events]
            [elfeed-cljsrn.subs]))

(def splash-screen (js/require "react-native-splash-screen"))

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

(defn handle-connectivity-change [connected?]
  (dispatch [:connection/set connected?]))

(defn listen-connectivity []
  (.addEventListener (.-isConnected rn/net-info) "change" handle-connectivity-change))

(defn unlisten-connectivity []
  (.removeEventListener (.-isConnected rn/net-info) "change" handle-connectivity-change))

(defn check-connection []
  (-> (.-isConnected rn/net-info)
      (.fetch)
      (.then handle-connectivity-change)))

(defn remote-error-message []
  [rn/view {:style {:padding 10
                    :background-color "#fff9c4"}}
   [rn/text "Network error. Check your wifi or your elfeed server."]])

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

(defn entry-actions-button [entry]
  (let [ref-icon (r/atom nil)
        styles {:button {:padding-vertical 16
                         :padding-horizontal 8}
                :icon {:color (:text palette)}}
        actions [{:label "Open in browser"
                  :action (fn [] (dispatch [:open-entry-in-browser entry]))}]]
    (fn []
      (let [show-popup-fn (fn [e]
                            (.showPopupMenu rn/ui-manager (.findNodeHandle rn/ReactNative @ref-icon)
                                            (clj->js (map :label actions))
                                            (fn [] )
                                            (fn [e i]
                                              (when i ((:action (nth actions i)))))))]
        [rn/view {:style {:flex 1
                          :flex-direction "row"}}
         [header-icon-button "markunread" {:on-press
                                           (fn [_]
                                             (dispatch [:mark-entries-as-unread (list (:webid entry))])
                                             (navigate-back))}]
         ;; this empty view is a hack for showPopupmenu
         ;; it adds the popup next to the tag, so we need an element before the
         ;; icon.
         [rn/view [rn/text {:ref (fn [ref] (reset! ref-icon ref))} ""]]
         [header-icon-button "more-vert" {:on-press show-popup-fn}]]))))

(defmulti scene #(keyword (aget % "scene" "route" "key")))

(defmethod scene :configure-server [scene-props] [configure-server-scene])
(defmethod scene :entries [scene-props] [entries-scene])
(defmethod scene :entry [scene-props] [entry-scene])
(defmethod scene :settings [scene-props] [settings-scene])

(defn nav-title [scene-props]
  (let [title (aget scene-props "scene" "route" "title")]
    [rn/navigation-header-title {:text-style {:color (:text palette)}} title]))

(defn header-settings [scene-props]
  (let [left-button [header-icon-button "arrow-back" {:style {:button {:margin-left 16}}
                                                      :on-press #(navigate-back)}]]
    [rn/navigation-header (assoc
                           scene-props
                           :style {:background-color (:primary palette)}
                           :render-title-component #(r/as-element [nav-title %])
                           :render-left-component #(r/as-element left-button))]))

(defn header-entries-searching [search-term scene-props]
  (let [left-button [header-icon-button "arrow-back" {:style {:icon {:color (:grey600 colors)}}
                                                      :on-press #(dispatch [:search/abort nil])}]
        search-input [rn/text-input {:default-value search-term
                                     :placeholder "Search"
                                     :style {:font-size 18
                                             :height 55
                                             :color (:primary-text palette)}
                                     :selection-color "white"
                                     :underline-color-android "transparent"
                                     :return-key-type "search"
                                     :on-submit-editing (fn [e] (dispatch [:search/execute (.-text (.-nativeEvent e))]))
                                     :auto-focus true}]
        right-button [header-icon-button "close" {:style {:button {:margin-right 16}
                                                          :icon {:color (:grey600 colors)}}
                                                  :on-press #(dispatch [:search/clear nil])}]]
    [rn/navigation-header (assoc
                           scene-props
                           :style {:background-color (:grey200 colors)}
                           :render-title-component #(r/as-element search-input)
                           :render-left-component #(r/as-element left-button)
                           :render-right-component #(r/as-element right-button))]))

(defn header-entries-reading [scene-props]
  (let [left-button [header-icon-button "menu" {:style {:button {:margin-left 16}}
                                                :on-press #(dispatch [:drawer/open nil])}]
        right-button [header-icon-button "search" {:style {:button {:margin-right 16}}
                                                   :on-press #(dispatch [:search/init nil])}]]
    [rn/navigation-header (assoc
                           scene-props
                           :style {:background-color (:primary palette)}
                           :render-title-component #(r/as-element [nav-title %])
                           :render-left-component #(r/as-element left-button)
                           :render-right-component #(r/as-element right-button))]))

(defn header-entries-with-actions [scene-props selected-entries]
  (let [left-button [header-icon-button "arrow-back" {:style {:button {:margin-left 16}}
                                                      :on-press #(dispatch [:clear-selected-entries])}]
        title [rn/navigation-header-title
               {:text-style {:color (:text palette)}}
               (str (count selected-entries))]
        right-button (if (:unread? (last selected-entries))
                       [header-icon-button "drafts" {:on-press (fn [_]
                                                                 (dispatch [:mark-entries-as-read (map :webid selected-entries)])
                                                                 (dispatch [:clear-selected-entries]))}]
                       [header-icon-button "markunread" {:on-press (fn [_]
                                                                     (dispatch [:mark-entries-as-unread (map :webid selected-entries)])
                                                                     (dispatch [:clear-selected-entries]))}])]
    [rn/navigation-header (assoc
                           scene-props
                           :style {:background-color (:secondary-text palette)}
                           :render-title-component #(r/as-element title)
                           :render-left-component #(r/as-element left-button)
                           :render-right-component #(r/as-element right-button))]))

(defn header-entries [scene-props]
  (let [search-state (subscribe [:search/state])
        selected-entries (subscribe [:selected-entries])]
    (fn [scene-props]
      (if (:searching? @search-state)
        [header-entries-searching (or (:term @search-state)
                                      (:default-term @search-state)) scene-props]
        (if (empty? @selected-entries)
          [header-entries-reading scene-props]
          [header-entries-with-actions scene-props @selected-entries])))))

(defn header-entry [scene-props]
  (let [current-entry (subscribe [:current-entry])
        left-button [header-icon-button "arrow-back" {:style {:button {:margin-left 16}}
                                                      :on-press #(navigate-back)}]]
    (fn [scene-props]
      [rn/navigation-header
       (assoc
        scene-props
        :style {:background-color (:primary palette)}
        :render-title-component #(r/as-element [nav-title %])
        :render-left-component #(r/as-element left-button)
        :render-right-component #(r/as-element [entry-actions-button @current-entry]))])))

(defmulti header #(keyword (:key (:route (:scene %)))))

(defmethod header :configure-server [scene-props] nil)
(defmethod header :entries          [scene-props] [header-entries scene-props])
(defmethod header :entry            [scene-props] [header-entry scene-props])
(defmethod header :settings         [scene-props] [header-settings scene-props])

(defn app-root []
  (let [nav (subscribe [:nav/state])]
    (r/create-class
     {:component-did-mount (fn []
                             (.hide splash-screen)
                             (listen-back-button! nav)
                             (check-connection)
                             (listen-connectivity))
      :component-will-unmount (fn []
                                (unlisten-back-button! nav)
                                (unlisten-connectivity))
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
