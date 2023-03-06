(ns elfeed-cljsrn.android.components.drawer
  (:require [reagent.core :as r]
            [re-frame.core :refer [subscribe dispatch]]
            [reagent.react-native-paper :as paper]
            ["@react-navigation/drawer" :refer [DrawerContentScrollView]]
            [elfeed-cljsrn.events]
            [elfeed-cljsrn.subs]))

(def drawer-content-scroll-view (r/adapt-react-class DrawerContentScrollView))

(defn feed-count-entries-badge [{:keys [color count]}]
  [paper/badge {:style {:backgroundColor color}} count])

(defn feed-item [navigation feed]
  [paper/drawer-item {:label (:title feed)
                      :active (:selected? feed)
                      :icon "label-outline"
                      :right (fn [props]
                               (r/as-element [feed-count-entries-badge {:color (.-color props)
                                                                        :count (:total feed)}]))
                      :on-press (fn [_e]
                                  (dispatch [:search/execute {:feed-url (:url feed) :feed-title (:title feed)}])
                                  (.navigate navigation "Entries"))}])

(defn drawer-content [_opts]
  (let [feeds (subscribe [:feeds])
        total-entries (subscribe [:total-entries])]
    (fn [opts]
      (let [filtering-by-feed? (some :selected? @feeds)]
        [drawer-content-scroll-view (js->clj opts)
         [paper/drawer-section {}
          [paper/drawer-item {:label "All entries"
                              :icon "rss"
                              :active (and (= 0 (.-index (.-state opts)))
                                           (not filtering-by-feed?))
                              :right (fn [props]
                                       (r/as-element [feed-count-entries-badge {:color (.-color props)
                                                                                :count @total-entries}]))
                              :on-press (fn [_e]
                                          (dispatch [:search/execute {:feed-url nil :feed-title nil}])
                                          (.navigate (.-navigation opts) "Entries"))}]]

         [paper/drawer-section {:title "Your subscriptions"}
          (for [feed @feeds]
            ^{:key (:webid feed)} [feed-item (.-navigation opts) feed])]
         [paper/drawer-section {:showDivider false}
          [paper/drawer-item {:label "Settings"
                              :icon "cog-outline"
                              :active (and (= 1 (.-index (.-state opts)))
                                           (not filtering-by-feed?))
                              :on-press (fn [_e]
                                          (dispatch [:search/execute {:feed-url nil :feed-title nil}])
                                          (.navigate (.-navigation opts) "Settings"))}]]]))))
