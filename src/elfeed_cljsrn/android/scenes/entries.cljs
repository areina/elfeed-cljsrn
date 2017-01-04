(ns elfeed-cljsrn.android.scenes.entries
  (:require [reagent.core :as r]
            [re-frame.core :refer [subscribe dispatch]]
            [elfeed-cljsrn.rn :as rn]
            [elfeed-cljsrn.navigation :refer [navigate-to]]
            [elfeed-cljsrn.ui :as ui :refer [colors palette button icon]]
            [elfeed-cljsrn.events]
            [elfeed-cljsrn.subs])
  (:import [goog.i18n DateTimeFormat]))

(defn format-update-time [time]
  (let [js-date (js/Date. (* time 1000))]
    (.format (goog.i18n.DateTimeFormat. "dd/MM/yyyy hh:mm") js-date)))

(defn format-entry-date [date]
  (let [js-date (js/Date. date)]
    (.format (goog.i18n.DateTimeFormat. "dd/MM/yyyy") js-date)))

(defn remote-error-message []
  [rn/view {:style {:padding 10
                    :background-color "#fff9c4"}}
   [rn/text "Network error. Check your wifi or your elfeed server."]])

(defn entry-row [entry]
  (let [styles {:list-wrapper {:flex-direction "row"
                               :background-color (if (:selected? entry) "#d4d4d4"
                                                     (if (:unread? entry)
                                                       (:white colors)
                                                       (:grey100 colors)))
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
                   :on-long-press (fn [_]
                                    (dispatch [:toggle-select-entry entry]))
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

(defn entry-quick-actions [entry]
  (let [styles {:wrapper {:flex 1
                          :flex-direction "row"
                          :justify-content "flex-end"}
                :icon {:color (:dark-primary palette)}}]
    [rn/view {:style (:wrapper styles)}
     [rn/touchable {:on-press #(dispatch [:mark-entries-as-read (list (:webid entry))])}
      [rn/view {}
       [icon {:style (:icon styles) :name "archive" :size 22}]]]]))

(defn no-entries-component []
  (let [styles {:wrapper {:height 300
                          :align-items "center"}}]
    [rn/view {:style (:wrapper styles)}
     [icon {:style {} :name "rss-feed" :size 84}]
     [rn/text "There are no entries"]]))

(defn entries-scene []
  (let [loading (subscribe [:loading?])
        update-time (subscribe [:update-time])
        remote-error (subscribe [:remote-error :entries])
        entries (subscribe [:entries])
        styles {:wrapper {:flex 1}
                :list {:margin-top 0
                       :padding-bottom 0}
                :separator {:height 1
                            :background-color (:grey300 colors)}}]
    (fn []
      (let [datasource (.cloneWithRowsAndSections
                        (rn/ReactNative.SwipeableListView.getNewDataSource.)
                        (clj->js {:s1 (or @entries '())})
                        (clj->js '("s1")))]
        [rn/view {:style (:wrapper styles)}
         (when @remote-error
           [remote-error-message])
         [rn/swipeable-list-view {:dataSource datasource
                                  :max-swipe-distance 50
                                  :bounceFirstRowOnMount false
                                  :refresh-control (r/as-element [rn/refresh-control {:refreshing @loading
                                                                                      :on-refresh #(dispatch [:fetch-content])}])
                                  :style (:list styles)
                                  :enableEmptySections true
                                  :render-header (fn [_ _]
                                                   (when (> @update-time 0)
                                                     (r/as-element [update-time-info @update-time])))
                                  ;; :render-quick-actions (fn [row-data _section-id _row-id]
                                  ;;                         (r/as-element [entry-quick-actions (js->clj row-data :keywordize-keys true)]))
                                  :render-row (fn [data _section-id _row-id]
                                                (r/as-element [entry-row (js->clj data :keywordize-keys true)]))
                                  :render-separator (fn [section-id row-id _]
                                                      (r/as-element [rn/view {:key (str section-id "-" row-id)
                                                                              :style (:separator styles)}]))}]
         (when (empty? @entries)
           [no-entries-component])]))))
