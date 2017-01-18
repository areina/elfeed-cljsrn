(ns elfeed-cljsrn.android.components.drawer
  (:require [reagent.core :as r]
            [re-frame.core :refer [subscribe dispatch]]
            [elfeed-cljsrn.rn :as rn]
            [elfeed-cljsrn.navigation :refer [navigate-to]]
            [elfeed-cljsrn.ui :as ui :refer [colors palette icon]]
            [elfeed-cljsrn.events]
            [elfeed-cljsrn.subs]))

(defn drawer-button [props label]
  (let [selected? (:selected? props)
        styles {:wrapper {:flex-direction "row"
                          :align-items "center"
                          :height 48
                          :padding-horizontal 16
                          :background-color (when selected? (:grey200 colors))}
                :icon {:color (when selected? (:primary palette))}
                :value {:font-size 14
                        :font-family "sans-serif-medium"
                        :flex-grow 2
                        :flex-shrink 1
                        :color (if selected?
                                 (:primary palette)
                                 "rgba(0,0,0,.87)")
                        :margin-left 28}
                :counter {:padding-left 8}}]
    [rn/touchable {:on-press (:on-press props)}
     [rn/view {:style (:wrapper styles)}
      [icon {:style (:icon styles)
             :name (or (:icon-name props) "label")
             :size 20}]
      [rn/text {:number-of-lines 1
                :style (:value styles)} label]
      [rn/text {:style (:counter styles)} (:total props)]]]))

(defn feed-row [feed]
  [drawer-button {:on-press (fn []
                              (dispatch [:search/execute {:feed-title (:title feed)}])
                              (dispatch [:drawer/close nil]))
                  :selected? (:selected? feed)
                  :icon-name "label"
                  :total (:total feed)} (:title feed)])

(defn header [{:keys [selected? total-entries]}]
  (let [styles {:wrapper {}
                :user-info {:background-color (:primary palette)
                            :border-bottom-width 1
                            :border-color (:grey300 colors)
                            :padding-bottom 8
                            :height 136}}]
    [rn/view {:style (:wrapper styles)}
     [rn/view {:style (:user-info styles)}]
     [drawer-button {:on-press (fn []
                                 (dispatch [:search/execute {:feed-title nil}])
                                 (dispatch [:drawer/close nil]))
                     :icon-name "rss-feed"
                     :selected? selected?
                     :total total-entries} "All entries"]]))

(defn footer []
  (let [styles {:wrapper {:border-top-width 1
                          :border-color (:grey300 colors)}}]
    [rn/view {:style (:wrapper styles)}
     [drawer-button {:on-press (fn []
                                 (dispatch [:drawer/close nil])
                                 (navigate-to :settings))
                     :icon-name "settings"} "Settings"]]))

(defn feeds-section-header []
  (let [styles {:wrapper {:flex-direction "row"
                          :align-items "center"
                          :padding-left 16
                          :height 48}
                :text {:font-family "sans-serif-medium"
                       :color "rgba(0,0,0,.54)"}}
        title "Your subscriptions"]
    [rn/view {:style (:wrapper styles)}
     [rn/text {:style (:text styles)} title]]))

(defn navigation-drawer []
  (let [feeds (subscribe [:feeds])
        total-entries (subscribe [:total-entries])
        original-datasource (rn/ReactNative.ListView.DataSource.
                             (clj->js {:rowHasChanged (fn [r1 r2] (not (= r1 r2)))}))]
    (fn []
      (let [filtering-by-feed? (some :selected? @feeds)
            datasource (.cloneWithRows original-datasource
                                       (clj->js (or @feeds '())))]
        [rn/list-view
         {:dataSource datasource
          :enableEmptySections true
          :render-header #(r/as-element [header {:selected? (not filtering-by-feed?)
                                                 :total-entries @total-entries}])
          :render-footer #(r/as-element [footer])
          :render-row (fn [row-data _section-id _row-id _highlight-row]
                        (r/as-element [feed-row (js->clj row-data :keywordize-keys true)]))
          :render-section-header (fn [section-data _section-id]
                                   (when-not (empty? section-data)
                                     (r/as-element [feeds-section-header])))}]))))
