(ns elfeed-cljsrn.android.scenes.entries
  (:require [reagent.core :as r]
            [reagent.react-native :as rn]
            [reagent.react-native-paper :as paper]
            [re-frame.core :refer [subscribe dispatch]]
            [elfeed-cljsrn.events]
            [elfeed-cljsrn.components :refer [remote-error-message]]
            [elfeed-cljsrn.subs])
  (:import [goog.i18n DateTimeFormat]))

(defn format-update-time [time]
  (let [js-date (js/Date. (* time 1000))]
    (.format (DateTimeFormat. "dd/MM/yyyy hh:mm") js-date)))

(defn format-entry-date [date]
  (let [js-date (js/Date. date)]
    (.format (DateTimeFormat. "dd/MM/yyyy") js-date)))

(defn update-time-info [update-time]
  [paper/list-subheader
   [paper/text {:variant "labelmedium"}
    (str "LAST UPDATE: ")] (format-update-time update-time)])

(defn no-entries-component [_props]
  [rn/view {:style {:flex 1
                    :border-width 1
                    :justify-content "center"
                    :align-items "center"}}
   [paper/icon-button {:color "black" :icon "rss" :size 84}]
   [paper/text {:variant "bodyMedium"} "There are no entries"]])

(defn entry-separator []
  [paper/divider])

(defn entry-date [date unread?]
  [paper/text {:variant "labelSmall"
               :style {:fontWeight (when unread? "bold")}} (format-entry-date date)])

(defn entry-row [navigation entry]
  (let [theme ^js (paper/use-theme-hook)
        on-long-press (fn [_event]
                        (dispatch [:toggle-select-entry entry]))
        on-press (fn [_event]
                   (dispatch [:fetch-entry-content entry])
                   (.navigate navigation "Entry" (clj->js {:entry-id (:webid entry)})))]
    [paper/list-item {:title (:title entry)
                      :title-style {:fontWeight (when (:unread? entry) "bold")}
                      :description (str "» " (:title (:feed entry)))
                      :style {:backgroundColor (when (:selected? entry) (.-secondaryContainer ^js (.-colors theme)))}
                      :on-press (fn [event]
                                  (if (:selected? entry)
                                    (on-long-press event)
                                    (on-press event)))
                      :on-long-press on-long-press
                      :right #(r/as-element [entry-date (:date entry) (:unread? entry)])}]))

(defn entry-row-wrapper [navigation entry-id]
  [:f> entry-row navigation entry-id])

(defn entries-scene [props]
  (let [loading (subscribe [:loading?])
        update-time (subscribe [:update-time])
        remote-error (subscribe [:remote-error :entries])
        entries (subscribe [:entries])]
    (fn []
      [rn/view {:style {:flex 1}}
       (when @remote-error
         [remote-error-message (.-navigation props)])
       [rn/flat-list {:data (clj->js @entries)
                      :style {:flex 1}
                      :contentContainerStyle {:flexGrow 1}
                      :refreshing (boolean @loading)
                      :onRefresh (fn [] (dispatch [:fetch-content]))
                      :keyExtractor (fn [item] (.toString (.-id item)))
                      :renderItem (fn [opts]
                                    (r/as-element [entry-row-wrapper (.-navigation props) (js->clj (.-item opts) :keywordize-keys true)]))
                      :ListHeaderComponent (r/as-element [update-time-info @update-time])
                      :ListEmptyComponent (r/as-element [no-entries-component])
                      :ItemSeparatorComponent (r/as-element [entry-separator])}]])))
