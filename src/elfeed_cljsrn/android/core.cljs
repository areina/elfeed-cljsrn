(ns elfeed-cljsrn.android.core
  (:require [reagent.core :as r]
            [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [elfeed-cljsrn.rn :as rn]
            [elfeed-cljsrn.navigation :refer [navigate-back navigate-to]]
            [elfeed-cljsrn.ui :as ui :refer [colors palette icon]]
            [elfeed-cljsrn.android.scenes.configure-server :refer [configure-server-scene]]
            [elfeed-cljsrn.android.scenes.settings :refer [settings-scene]]
            [elfeed-cljsrn.android.scenes.entries :refer [entries-scene]]
            [elfeed-cljsrn.android.scenes.entry :refer [entry-scene]]
            [elfeed-cljsrn.events]
            [elfeed-cljsrn.subs]))

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

(defn entry-actions-button []
  (let [ref-icon (r/atom nil)
        styles {:button {:padding-vertical 16
                         :padding-horizontal 8}
                :icon {:color (:text palette)}}
        actions [{:label "Open in browser"
                  :action (fn [] (dispatch [:open-entry-in-browser]))}]]
    (fn []
      [rn/view {:style {:flex 1
                        :flex-direction "row"}}
       ;; this empty view is a hack for showPopupmenu
       ;; it adds the popup next to the tag, so we need an element before the
       ;; icon.
       [rn/view [rn/text {:ref (fn [ref] (reset! ref-icon ref))} ""]]
       [rn/touchable {:on-press (fn [e]
                                  (.showPopupMenu rn/ui-manager (.findNodeHandle rn/ReactNative @ref-icon)
                                                  (clj->js (map :label actions))
                                                  (fn [] )
                                                  (fn [e i]
                                                    (when i
                                                      ((:action (nth actions i)))))))}
        [rn/view {:style (:button styles)}
         [icon {:style (:icon styles) :name "more-vert" :size 24}]]]])))

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
