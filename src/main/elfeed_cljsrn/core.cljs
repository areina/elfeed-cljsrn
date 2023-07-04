(ns elfeed-cljsrn.core
  (:require
   ["react-native-gesture-handler"]
   [elfeed-cljsrn.android.components.drawer :refer [drawer-content]]
   [elfeed-cljsrn.android.scenes.configure-server :refer [configure-server-scene]]
   [elfeed-cljsrn.android.scenes.entry :refer [entry-scene entry-scene-options]]
   [elfeed-cljsrn.android.scenes.entries :refer [entries-scene entries-scene-options]]
   [elfeed-cljsrn.android.scenes.settings :refer [settings-scene]]
   [elfeed-cljsrn.components :refer [connection-error]]
   [elfeed-cljsrn.events]
   [elfeed-cljsrn.navigation :as nav]
   [elfeed-cljsrn.theme :refer [get-app-theme]]
   [elfeed-cljsrn.subs]
   [reagent.core :as r]
   [reagent.react-native :as rn]
   [reagent.react-native-paper :as paper]
   [re-frame.core :refer [subscribe dispatch dispatch-sync]]
   [shadow.react-native :refer (render-root)]))

(defn default-screen-options [^js theme]
  {:headerStyle {:backgroundColor (.-primary (.-colors theme))}
   :headerTintColor (.-onPrimary (.-colors theme))})

(defn screen-opts->clj [^js props]
  {:navigation (.-navigation props)
   :route (.-route props)})

(defn root [theme]
  (let [search-state @(subscribe [:search/state])
        selected-entries @(subscribe [:selected-entries])
        screen-options (default-screen-options theme)
        [stack-nav stack-screen] nav/stack-navigator
        [drawer-nav drawer-screen] nav/drawer-navigator]
    [nav/container {:theme theme}
     [stack-nav {:screen-options screen-options}
      [stack-screen
       {:name "Root"
        :options {:header-shown false}}
       (fn []
         (r/as-element
          [drawer-nav {:drawer-content (fn [opts] (r/as-element [drawer-content opts]))
                       :screen-options screen-options}
           [drawer-screen
            {:name "Entries"
             :options (fn [] (clj->js (entries-scene-options search-state selected-entries)))}
            (fn [props] (r/as-element [:f> entries-scene (screen-opts->clj props)]))]
           [drawer-screen
            {:name "Settings"}
            (fn [props] (r/as-element [settings-scene (screen-opts->clj props)]))]]))]
      [stack-screen
       {:name "Entry"
        :options (fn [opts] (clj->js (entry-scene-options (screen-opts->clj opts))))}
       (fn [opts] (r/as-element [:f> entry-scene (screen-opts->clj opts)]))]]]))

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
;; splash screen and our app component once the app finishes the booting process.
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
