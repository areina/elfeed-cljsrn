(ns elfeed-cljsrn.android.scenes.entry
  (:require [reagent.react-native :as rn]
            [reagent.react-native-paper :as paper]
            [reagent.react-native-webview :refer [web-view]]

            [re-frame.core :refer [subscribe dispatch]]
            [elfeed-cljsrn.components :refer [remote-error-message]]
            [elfeed-cljsrn.subs]))

(defn tag [label]
  [paper/chip {:compact true :style {:margin-right 5}} label])

(defn get-entry-id-from-props [^js props]
  (aget (.-params (.-route props)) "entry-id"))

(defn header [{:keys [title subtitle tags]}]
  [rn/view {:style {:padding-vertical 10
                    :padding-horizontal 10
                    :flexDirection "column"}}
   [paper/text {:variant "titleLarge"} title]
   [paper/text {:variant "bodyMedium"} subtitle]
   [rn/view {:style {:margin-top 10 :flex-direction "row"}}
    (for [tag-label tags] ^{:key tag-label} [tag tag-label])]])

(defn wrap-content [{:keys [content dark?]}]
  (str "<!DOCTYPE html><html>"
       "<head><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"></head>"
       (if dark?
         "<body style=\"filter: invert(1)\" >"
         "<body>")
       content
       "</body>"
       "</html>"))

(defn loading []
  [rn/view {:style {:flex 1
                    :justify-content "center"
                    :align-items "center"}}
   [rn/activity-indicator {:size "large"}]])

(defn scene [{:keys [entry loading? error]}]
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

(defn entry-scene [^js _props]
  (let [loading? (subscribe [:fetching-entry?])
        remote-error (subscribe [:remote-error :entry])
        entry-content (subscribe [:current-entry])]

    [:f> scene {:loading? @loading? :entry @entry-content :error @remote-error}]))
