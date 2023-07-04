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
       "<style>body { line-height: 1.4rem }</style>"
       "<style>img { display: block; margin-bottom: 5px; max-width: 100%; height: auto; }</style>"
       "<style>blockquote { margin: 1em 1em; color: #999; border-left: 2px solid #999; padding-left: 1em; }</style>"
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

(defn ^:private body [{:keys [entry on-load-end on-scroll]}]
  (let [theme ^js (paper/use-theme-hook)
        html-content (wrap-content {:content (:content-body entry)
                                    :dark? (.-dark theme)})]
    [web-view {:container-style {:padding-horizontal 10
                                 :padding-vertical 10}
               :style {:background-color (.-background (.-colors theme))}
               :origin-whitelist ["*"]
               :on-scroll on-scroll
               :on-load-end (fn [_e]
                              (on-load-end entry))
               :on-should-start-load-with-request (fn [req]
                                                    (dispatch [:open-url-in-browser (.-url req)])
                                                    false)
               :source {:html html-content}}]))

(defn ^:private entry-item [{:keys [entry on-content-loaded on-scroll error width]}]
  [rn/view {:style {:flex 1 :width width}}
   (when error
     [remote-error-message])
   [header {:title (:title entry)
            :subtitle (str "» " (:title (:feed entry)))
            :tags (:tags entry)}]
   [paper/divider]
   (if (:content-body entry)
     [:f> body {:entry entry :on-load-end on-content-loaded :on-scroll on-scroll}]
     [loading])])

(defn entry-scene-options [{:keys [^js navigation ^js route]}]
  (let [entry-id (aget (.-params route) "entry-id")]
    {:title ""
     :animation "fade_from_bottom"
     :headerRight (fn [^js opts]
                    (r/as-element [header-right-actions {:navigation navigation
                                                         :entry-id entry-id
                                                         :icon-color (.-tintColor opts)}]))}))

(defn ^:private enable-list-scroll [^js list-ref]
  (when (.-current list-ref)
    (.setNativeProps (.-current list-ref) #js {:scrollEnabled true})))

(defn ^:private disable-list-scroll [^js list-ref]
  (when (.-current list-ref)
    (.setNativeProps (.-current list-ref) #js {:scrollEnabled false})))

(defn entries-paged-list [{:keys [entries initial-index on-content-loaded on-next-page on-prev-page on-page-change remote-error]}]
  (let [dimensions (useWindowDimensions)
        flat-list-ref (useRef nil)
        current-index (atom initial-index)
        key-extractor-fn (fn [item] (.toString (.-id item)))
        get-item-layout (fn [_data index]
                          #js {:length (.-width dimensions)
                               :offset (* index (.-width dimensions))
                               :index index})
        last-scroll-timeout (atom nil)
        on-change (fn [opts]
                    (when-let [changed (first (filter (fn [^js item]
                                                        (.-isViewable item)) (.-changed ^js opts)))]
                      (let [item ^js (.-item changed)
                            index (.-index changed)]
                        (when (not (= index @current-index))
                          (on-page-change index item)
                          (if (> index @current-index)
                            (on-next-page index item)
                            (on-prev-page index item))
                          (reset! current-index index)))))
        on-scroll (fn [_e]
                    (disable-list-scroll flat-list-ref)
                    (js/clearTimeout @last-scroll-timeout)
                    (reset! last-scroll-timeout (js/setTimeout (fn []
                                                                 (enable-list-scroll flat-list-ref)) 1000)))
        viewability-config-callback-pairs (useRef (clj->js [{:viewabilityConfig {:itemVisiblePercentThreshold 95}
                                                             :onViewableItemsChanged on-change}]))
        render-item-fn (fn [opts]
                         (let [item (js->clj (.-item opts) :keywordize-keys true)]
                           (r/as-element [:f> entry-item {:width (.-width dimensions)
                                                          :on-content-loaded on-content-loaded
                                                          :on-scroll on-scroll
                                                          :entry item
                                                          :error (or remote-error (:error-fetching item))}])))]
    [rn/flat-list {:data (clj->js entries)
                   :ref flat-list-ref
                   :viewability-config-callback-pairs (.-current viewability-config-callback-pairs)
                   :horizontal true
                   :paging-enabled true
                   :initial-num-to-render 1
                   :window-size 1
                   :initial-scroll-index initial-index
                   :key-extractor key-extractor-fn
                   :shows-horizontal-scroll-indicator false
                   :get-item-layout get-item-layout
                   :render-item render-item-fn}]))

(defn entry-scene [{:keys [^js navigation ^js route]}]
  (let [remote-error (subscribe [:remote-error :entry])
        entries (subscribe [:entries])
        initial-index (or (.-index ^js (.-params route)) 0)
        on-content-loaded (fn [_entry])
        on-next-page (fn [index _item]
                       (when (< (+ index 1) (count @entries))
                         (dispatch [:fetch-entry-content (select-keys (nth @entries (+ index 1)) [:webid :content])])))
        on-prev-page (fn [index _item]
                       (when (> (- index 1) 0)
                         (dispatch [:fetch-entry-content (select-keys (nth @entries (- index 1)) [:webid :content])])))
        on-page-change (fn [_index ^js item]
                         (dispatch [:mark-entries-as :read [(.-webid item)]])
                         (.setParams navigation #js {:entry-id (.-webid item)}))]
    (fn [{:keys [_navigation _route]}]
      [:f> entries-paged-list {:entries @entries
                               :remote-error @remote-error
                               :initial-index initial-index
                               :on-content-loaded on-content-loaded
                               :on-page-change on-page-change
                               :on-next-page on-next-page
                               :on-prev-page on-prev-page}])))
