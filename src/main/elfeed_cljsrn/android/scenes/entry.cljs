(ns elfeed-cljsrn.android.scenes.entry
  (:require [reagent.react-native :as rn]
            [reagent.react-native-paper :as paper]
            [reagent.react-native-webview :refer [web-view]]
            [reagent.core :as r]
            ["react" :refer [useRef]]
            ["react-native" :refer [useWindowDimensions]]
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
                    :flex-direction "column"}}
   [paper/text {:variant "titleLarge"} title]
   [paper/text {:variant "bodyMedium"} subtitle]
   [rn/view {:style {:margin-top 10 :flex-direction "row"}}
    (for [tag-label tags] ^{:key tag-label} [tag tag-label])]])

(defn ^:private wrap-content [{:keys [content dark?]}]
  (str "<!DOCTYPE html><html>"
       "<head><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"></head>"
       "<style>img { display: block; max-width: 100%; height: auto; }</style>"
       "<style>blockquote { margin: 1em 3em; color: #999; border-left: 2px solid #999; padding-left: 1em; }</style>"
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

(defn ^:private entry-item [{:keys [entry on-scroll error width]}]
  (let [theme ^js (paper/use-theme-hook)]
    [rn/view {:style {:flex 1 :width width}}
     (when error
       [remote-error-message])
     [header {:title (:title entry)
              :subtitle (str "» " (:title (:feed entry)))
              :tags (:tags entry)}]
     [paper/divider]
     (if (:content-body entry)
       [web-view {:container-style {:padding-horizontal 10
                                    :padding-vertical 10}
                  :style {:background-color (.-background (.-colors theme))}
                  :origin-whitelist ["*"]
                  :on-scroll on-scroll
                  :on-should-start-load-with-request (fn [req]
                                                       (dispatch [:open-url-in-browser (.-url req)])
                                                       false)
                  :source {:html (wrap-content {:content (:content-body entry)
                                                :dark? (.-dark theme)})}}]
       [loading])]))

(defn entry-scene-options [{:keys [^js navigation ^js route]}]
  (let [entry-id (aget (.-params route) "entry-id")]
    {:title ""
     :animation "fade_from_bottom"
     :headerRight (fn [^js opts]
                    (r/as-element [header-right-actions {:navigation navigation
                                                         :entry-id entry-id
                                                         :icon-color (.-tintColor opts)}]))}))

(defn enable-list-scroll [^js list-ref]
  (.setNativeProps (.-current list-ref) #js {:scrollEnabled true}))

(defn disable-list-scroll [^js list-ref]
  (.setNativeProps (.-current list-ref) #js {:scrollEnabled false}))

(defn entry-scene [{:keys [^js navigation ^js route]}]
  (let [remote-error (subscribe [:remote-error :entry])
        entries (subscribe [:entries])
        initial-index (or (.-index ^js (.-params route)) 0)
        current-index (atom initial-index)
        velocity-y (atom nil)
        viewability-config #js {:itemVisiblePercentThreshold 95}
        last-scroll-timeout (atom nil)
        on-next-page (fn [{:keys [^js _item index]}]
                       (when (< (+ index 1) (count @entries))
                         (dispatch [:fetch-entry-content (select-keys (nth @entries (+ index 1)) [:webid :content])])))
        on-prev-page (fn [{:keys [^js _item index]}]
                       (when (> (- index 1) 0)
                         (dispatch [:fetch-entry-content (select-keys (nth @entries (- index 1)) [:webid :content])])))
        on-change (fn [opts]
                    (when-let [changed (first (filter (fn [^js item]
                                                        (.-isViewable item)) (.-changed ^js opts)))]
                      (let [item ^js (.-item changed)
                            index (.-index changed)
                            opts {:index (.-index changed) :item item}]
                        (when (not (= index @current-index))
                          (.setParams navigation #js {:entry-id (.-webid item)})
                          (if (> index @current-index)
                            (on-next-page opts)
                            (on-prev-page opts))
                          (reset! current-index index)))))]
    (fn [{:keys [_navigation _route]}]
      (let [dimensions (useWindowDimensions)
            flat-list-ref (useRef nil)
            data (clj->js @entries)
            get-item-layout (fn [_data index]
                              #js {:length (.-width dimensions)
                                   :offset (* index (.-width dimensions))
                                   :index index})
            key-extractor-fn (fn [item] (.toString (.-id item)))
            on-scroll (fn [e]
                        (js/clearTimeout @last-scroll-timeout)
                        (reset! velocity-y (.-y (.-velocity (.-nativeEvent ^js e))))
                        (reset! last-scroll-timeout (js/setTimeout (fn []
                                                                     (enable-list-scroll flat-list-ref)) 1000))
                        (disable-list-scroll flat-list-ref))
            render-item-fn (fn [opts]
                             (let [item  (js->clj (.-item opts) :keywordize-keys true)]
                               (r/as-element [:f> entry-item {:width (.-width dimensions)
                                                              :on-scroll on-scroll
                                                              :entry item
                                                              :error (or @remote-error (:error-fetching item))}])))]

        [rn/flat-list {:data data
                       :ref flat-list-ref
                       :viewability-config viewability-config
                       :on-viewable-items-changed on-change
                       :horizontal true
                       :paging-enabled true
                       :initial-scroll-index initial-index
                       :key-extractor key-extractor-fn
                       :scroll-event-throttle 200
                       :get-item-layout get-item-layout
                       :render-item render-item-fn}]))))
