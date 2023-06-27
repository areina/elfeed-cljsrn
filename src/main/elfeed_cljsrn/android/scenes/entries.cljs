(ns elfeed-cljsrn.android.scenes.entries
  (:require [reagent.core :as r]
            ["react-native" :refer [useWindowDimensions]]
            [reagent.react-native :as rn]
            [reagent.react-native-paper :as paper]
            [re-frame.core :refer [subscribe dispatch]]
            [elfeed-cljsrn.events]
            [elfeed-cljsrn.components :refer [remote-error-message header-icon-button]]
            [elfeed-cljsrn.subs])
  (:import [goog.i18n DateTimeFormat]))

(defn format-update-time [time]
  (let [js-date (js/Date. (* time 1000))]
    (.format (DateTimeFormat. "dd/MM/yyyy h:mm a") js-date)))

(defn format-entry-date [date]
  (let [js-date (js/Date. date)]
    (.format (DateTimeFormat. "dd/MM/yyyy") js-date)))

(defn update-time-info [update-time]
  [paper/list-subheader
   [paper/text {:variant "labelMedium"}
    (str "LAST UPDATE: ")] (format-update-time update-time)])

(defn no-entries-component [_props]
  [rn/view {:style {:flex 1
                    :justify-content "center"
                    :align-items "center"}}
   [paper/icon-button {:color "black" :icon "rss" :size 84}]
   [paper/text {:variant "bodyMedium"} "There are no entries"]])

(defn search-button [{:keys [color]}]
  [header-icon-button {:icon "magnify"
                       :color color
                       :on-press (fn [_]
                                   (dispatch [:search/init]))}])

(defn searchbar [{:keys [default-value on-change-text on-close on-end-editing]}]
  (let [theme ^js (paper/use-theme-hook)
        color (.-onPrimary (.-colors theme))]
    [rn/view {:style {:flex-direction "row"
                      :align-items "center"}
              :theme theme}
     [rn/text-input {:placeholder "Search"
                     :auto-focus true
                     :cursor-color color
                     :placeholder-text-color color
                     :style {:color color
                             :flex 1
                             :padding-left 0
                             :align-self "stretch"
                             :font-size 16
                             :min-height 56}
                     :return-key-type "search"
                     :defaultValue default-value
                     :on-change-text on-change-text
                     :on-end-editing on-end-editing}]
     (when default-value
       [paper/icon-button {:icon "close"
                           :icon-color color
                           :on-press on-close}])]))

(defn entries-search-input [current-term]
  (let [term (r/atom current-term)]
    (fn [_current-term]
      (let [dimensions (useWindowDimensions)]
        [rn/view {:style {:flex 1
                          :min-width (- (.-width dimensions) 72)}}
         [:f> searchbar {:default-value @term
                         :on-change-text (fn [text]
                                           (reset! term text))
                         :on-close (fn [^js _e]
                                     (reset! term nil))
                         :on-end-editing (fn [^js e]
                                           (let [text (.-text (.-nativeEvent e))]
                                             (if (seq text)
                                               (dispatch [:search/execute {:term text}])
                                               (dispatch [:search/abort]))))}]]))))

(defn entries-screen-options-on-selecting [selected-entries]
  (let [ids (map :webid selected-entries)
        icon (if (:unread? (last selected-entries)) "email-open" "email-mark-as-unread")
        next-state (if (:unread? (last selected-entries)) :read :unread)
        right-button [header-icon-button  {:icon icon
                                           :on-press (fn [_]
                                                       (dispatch [:mark-entries-as next-state ids])
                                                       (dispatch [:clear-selected-entries]))}]]

    {:title (str (count selected-entries))
     :headerLeft #(r/as-element [header-icon-button {:icon "arrow-left"
                                                     :on-press (fn [_]
                                                                 (dispatch [:clear-selected-entries]))}])
     :headerRight #(r/as-element right-button)}))

(defn entries-screen-options-on-searching [search-state]
  {:headerTitle #(r/as-element [:f> entries-search-input (:current-term search-state)])
   :headerTitleContainerStyle {:flexGrow 1}
   :headerLeft #(r/as-element [header-icon-button {:icon "arrow-left"
                                                   :on-press (fn [_]
                                                               (dispatch [:search/abort]))}])})

(defn entries-scene-options [search-state selected-entries]
  (let [default-options {:title "All entries"
                         :headerRight (fn [^js props]
                                        (r/as-element [search-button (.-tintColor props)]))}]

    (if (:searching? search-state)
      (entries-screen-options-on-searching search-state)
      (if (seq selected-entries)
        (entries-screen-options-on-selecting selected-entries)
        default-options))))

(defn entry-separator []
  [paper/divider])

(defn entry-date [date unread?]
  [paper/text {:variant "labelSmall"
               :style {:fontWeight (when unread? "bold")}} (format-entry-date date)])

(defn entry-row [{:keys [navigation entry on-press index]}]
  (let [theme ^js (paper/use-theme-hook)
        on-long-press (fn [_event]
                        (dispatch [:toggle-select-entry entry]))
        on-press (fn [_event]
                   (on-press index)
                   (dispatch [:fetch-entry-content {:webid (:webid entry)
                                                    :content (:content entry)}])
                   (.navigate navigation "Entry" #js {:entry-id (:webid entry)
                                                      :index index}))]
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

(defn entries-scene [_props]
  (let [loading (subscribe [:loading?])
        update-time (subscribe [:update-time])
        remote-error-entries (subscribe [:remote-error :entries])
        remote-error-update-time (subscribe [:remote-error :update-time])
        entries (subscribe [:entries])
        on-press (fn [index]
                   (when (< (+ index 1) (count @entries))
                     (dispatch [:fetch-entry-content (select-keys (nth @entries (+ index 1)) [:webid :content])]))
                   (when (> index 0)
                     (dispatch [:fetch-entry-content (select-keys (nth @entries (- index 1)) [:webid :content])])))]

    (fn [{:keys [navigation]}]
      [rn/view {:style {:flex 1}}
       (when (or @remote-error-entries @remote-error-update-time)
         [remote-error-message navigation])
       [rn/flat-list {:data (clj->js @entries)
                      :style {:flex 1}
                      :content-container-style {:flexGrow 1}
                      :refreshing (boolean @loading)
                      :on-refresh (fn [] (dispatch [:fetch-content]))
                      :key-extractor (fn [item] (.toString (.-id item)))
                      :render-item (fn [opts]
                                     (let [current-index (.-index opts)]
                                       (r/as-element [:f> entry-row {:navigation navigation
                                                                     :index current-index
                                                                     :on-press on-press
                                                                     :entry (js->clj (.-item opts) :keywordize-keys true)}])))
                      :ListHeaderComponent (r/as-element [update-time-info @update-time])
                      :ListEmptyComponent (r/as-element [no-entries-component])
                      :ItemSeparatorComponent (r/as-element [entry-separator])}]])))
