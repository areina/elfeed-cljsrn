(ns elfeed-cljsrn.core
  (:require
   ["react-native-gesture-handler"]
   ["@react-navigation/native" :refer [NavigationContainer]]
   ["@react-navigation/native-stack" :refer [createNativeStackNavigator]]
   ["@react-navigation/drawer" :refer [createDrawerNavigator]]
   [elfeed-cljsrn.android.components.drawer :refer [drawer]]
   [elfeed-cljsrn.android.scenes.configure-server :refer [configure-server-scene]]
   [elfeed-cljsrn.android.scenes.settings :refer [settings-scene]]
   [elfeed-cljsrn.android.scenes.entries :refer [entries-scene]]
   [elfeed-cljsrn.android.scenes.entry :refer [entry-scene]]
   [elfeed-cljsrn.components :refer [connection-error header-icon-button]]
   [elfeed-cljsrn.events]
   [elfeed-cljsrn.theme :refer [get-app-theme]]
   [elfeed-cljsrn.subs]
   [reagent.core :as r]
   [reagent.react-native :as rn]
   [reagent.react-native-paper :as paper]
   [re-frame.core :refer [subscribe dispatch dispatch-sync]]
   [shadow.react-native :refer (render-root)]))

(defn header-screen-options [^js theme]
  {:headerStyle {:backgroundColor (.-primary (.-colors theme))}
   :headerTintColor (.-onPrimary (.-colors theme))})

(defn entries-search-input [current-term]
  (let [term (r/atom current-term)]
    (fn [_current-term]
      [paper/text-input {:placeholder "Search"
                         :dense true
                         :return-key-type "search"
                         :on-change-text (fn [text]
                                           (reset! term text))
                         :on-blur (fn [^js e] (dispatch [:search/execute {:term (.-text (.-nativeEvent e))}]))
                         :value @term}])))

;; this function needs love (look which args receives)
(defn entry-header-actions [^js navigation entry-id tint-color]
  [rn/view {:style {:margin -16 :flex-direction "row"}}
   [header-icon-button {:icon "email-mark-as-unread"
                        :color tint-color
                        :on-press (fn [_]
                                    (dispatch [:mark-entries-as-unread (list entry-id)])
                                    (.goBack navigation))}]
   [header-icon-button {:icon "open-in-new"
                        :color tint-color
                        :on-press (fn [_]
                                    (dispatch [:open-entry-in-browser entry-id]))}]])

(defn entries-screen-options-on-selecting [selected-entries]
  (let [right-button (if (:unread? (last selected-entries))
                       [header-icon-button  {:icon "email-open"
                                             :on-press (fn [_]
                                                         (dispatch [:mark-entries-as-read (map :webid selected-entries)])
                                                         (dispatch [:clear-selected-entries]))}]
                       [header-icon-button  {:icon "email-mark-as-unread"
                                             :on-press (fn [_]
                                                         (dispatch [:mark-entries-as-unread (map :webid selected-entries)])
                                                         (dispatch [:clear-selected-entries]))}])]

    {:title (str (count selected-entries))
     :headerLeft #(r/as-element [header-icon-button {:icon "arrow-left"
                                                     :on-press (fn [_]
                                                                 (dispatch [:clear-selected-entries]))}])
     :headerRight #(r/as-element right-button)}))

(defn entries-screen-options-on-searching [search-state]
  {:headerTitle #(r/as-element [entries-search-input (:current-term search-state)])
   :headerLeft #(r/as-element [header-icon-button {:icon "arrow-left"
                                                   :on-press (fn [_]
                                                               (dispatch [:search/abort]))}])
   :headerRight #(r/as-element [header-icon-button {:icon "close"
                                                    :on-press (fn [_]
                                                                (dispatch [:search/clear]))}])})

(defn entries-screen-options [search-state selected-entries]
  (let [default-options {:title "All entries"
                         :headerRight (fn [^js props]
                                        (r/as-element
                                         [header-icon-button {:icon "magnify"
                                                              :color (.-tintColor props)
                                                              :on-press (fn [_]
                                                                          (dispatch [:search/init]))}]))}]

    (if (:searching? search-state)
      (entries-screen-options-on-searching search-state)
      (if (seq selected-entries)
        (entries-screen-options-on-selecting selected-entries)
        default-options))))

(defn drawer-navigation [theme]
  (let [search-state @(subscribe [:search/state])
        selected-entries @(subscribe [:selected-entries])
        drawer-navigator (createDrawerNavigator)]
    [:> (.-Navigator drawer-navigator) {:screenOptions (header-screen-options theme)
                                        :drawerContent (fn [opts]
                                                         (r/as-element [drawer opts]))}
     [:> (.-Screen drawer-navigator) {:name "Entries"
                                      :options (fn [_props]
                                                 (clj->js (entries-screen-options search-state selected-entries)))}
      (fn [props]
        (r/as-element [entries-scene props]))]
     [:> (.-Screen drawer-navigator) {:name "Settings"}
      (fn [_props]
        (r/as-element [settings-scene]))]]))

(defn entry-screen-options [^js props]
  (let [navigation (.-navigation props)
        entry-id (aget (.-params (.-route props)) "entry-id")]
    {:title ""
     :animation "fade_from_bottom"
     :headerRight (fn [^js props]
                    (r/as-element [entry-header-actions navigation entry-id (.-tintColor props)]))}))

(defn root [theme]
  (let [stack (createNativeStackNavigator)]
    [:> NavigationContainer {:theme theme}
     [:> (.-Navigator stack) {:screenOptions (header-screen-options theme)}
      [:> (.-Screen stack) {:name "root"
                            :options {:headerShown false}}
       (fn [_props]
         (r/as-element [drawer-navigation theme]))]
      [:> (.-Screen stack) {:name "Entry"
                            :headerStyle {:backgroundColor "white"}
                            :options (fn [opts] (clj->js (entry-screen-options opts)))}
       (fn [props]
         (r/as-element [entry-scene props]))]]]))

(defn app []
  (let [server-configured? (subscribe [:server-configured?])
        connected? (subscribe [:connected?])
        theme ^js (get-app-theme)]
    [paper/provider {:theme theme}
     [rn/status-bar {:background-color (.-primary (.-colors theme))}]
     (if @server-configured?
       [root theme]
       [configure-server-scene])
     (when-not @connected?
       [connection-error])]))

;; With this wrapper component we can have a better transition between the
;; splash screen and our app component after the app finishes booting
(defn booting-app []
  (let [booting? (subscribe [:booting?])]
    (r/create-class
     {:component-did-update (fn [_]
                              (when (not @booting?)
                                (dispatch [:hide-splash-screen])))
      :display-name "BootingApp"
      :reagent-render
      (fn []
        (when (not @booting?)
          [app]))})))

(defn start
  {:dev/after-load true}
  []
  (render-root "ElfeedCljsrn" (r/as-element [booting-app])))

(defn init []
  (dispatch-sync [:boot])
  (start))
