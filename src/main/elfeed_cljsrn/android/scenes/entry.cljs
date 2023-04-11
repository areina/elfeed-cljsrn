(ns elfeed-cljsrn.android.scenes.entry
  (:require [reagent.react-native :as rn]
            [reagent.react-native-paper :as paper]
            [reagent.react-native-webview :refer [web-view]]
            [reagent.core :as r]
            [re-frame.core :refer [subscribe dispatch]]
            [elfeed-cljsrn.components :refer [remote-error-message header-icon-button]]
            [elfeed-cljsrn.subs]))

(defn ^:private header-right-actions [{:keys [^js navigation entry-id icon-color]}]
  [rn/view {:style {:margin -16 :flex-direction "row"}}
   [header-icon-button {:icon "email-mark-as-unread"
                        :color icon-color
                        :on-press (fn [_]
                                    (dispatch [:mark-entries-as :unread (list entry-id)])
                                    (.goBack navigation))}]
   [header-icon-button {:icon "open-in-new"
                        :color icon-color
                        :on-press (fn [_]
                                    (dispatch [:open-entry-in-browser entry-id]))}]])

(defn ^:private tag [label]
  [paper/chip {:compact true :style {:margin-right 5}} label])

(defn ^:private header [{:keys [title subtitle tags]}]
  [rn/view {:style {:padding-vertical 10
                    :padding-horizontal 10
                    :flexDirection "column"}}
   [paper/text {:variant "titleLarge"} title]
   [paper/text {:variant "bodyMedium"} subtitle]
   [rn/view {:style {:margin-top 10 :flex-direction "row"}}
    (for [tag-label tags] ^{:key tag-label} [tag tag-label])]])

(defn ^:private wrap-content [{:keys [content dark?]}]
  (str "<!DOCTYPE html><html>"
       "<head><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"></head>"
       (if dark?
         "<body style=\"filter: invert(1)\" >"
         "<body>")
       content
       "</body>"
       "</html>"))

(defn ^:private loading []
  [rn/view {:style {:flex 1
                    :justify-content "center"
                    :align-items "center"}}
   [rn/activity-indicator {:size "large"}]])

(defn ^:private scene [{:keys [entry loading? error]}]
  (let [theme ^js (paper/use-theme-hook)]
    [rn/view {:style {:flex 1}}
     (when error
       [remote-error-message])
     [header {:title (:title entry)
              :subtitle (str "» " (:title (:feed entry)))
              :tags (:tags entry)}]

     [paper/divider]
     (if loading?
       [loading]
       [web-view {:container-style {:padding-horizontal 10
                                    :padding-vertical 10}
                  :style {:backgroundColor (.-background (.-colors theme))}
                  :origin-whitelist ["*"]
                  :on-should-start-load-with-request (fn [req]
                                                       (dispatch [:open-url-in-browser (.-url req)])
                                                       false)
                  :source {:html (wrap-content {:content (:content-body entry) :dark? (.-dark theme)})}}])]))

(defn entry-scene-options [{:keys [^js navigation ^js route]}]
  (let [entry-id (aget (.-params route) "entry-id")]
    {:title ""
     :animation "fade_from_bottom"
     :headerRight (fn [^js opts]
                    (r/as-element [header-right-actions {:navigation navigation
                                                         :entry-id entry-id
                                                         :icon-color (.-tintColor opts)}]))}))

(defn entry-scene [{:keys [_navigation _route]}]
  (let [loading? (subscribe [:fetching-entry?])
        remote-error (subscribe [:remote-error :entry])
        entry-content (subscribe [:current-entry])]
    [:f> scene {:loading? @loading? :entry @entry-content :error @remote-error}]))
