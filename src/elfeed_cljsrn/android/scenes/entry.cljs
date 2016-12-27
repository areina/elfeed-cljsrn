(ns elfeed-cljsrn.android.scenes.entry
  (:require [reagent.core :as r]
            [re-frame.core :refer [subscribe dispatch]]
            [elfeed-cljsrn.rn :as rn]
            [elfeed-cljsrn.ui :as ui :refer [colors palette button icon]]
            [elfeed-cljsrn.events]
            [elfeed-cljsrn.subs]))

(defn remote-error-message []
  [rn/view {:style {:padding 10
                    :background-color "#fff9c4"}}
   [rn/text "Network error. Check your wifi or your elfeed server."]])

(defn tag [label]
  (let [styles {:wrapper {:background-color (:grey300 colors)
                          :margin-left 4
                          :padding-vertical 2
                          :padding-horizontal 4}
                :text {:font-size 12
                       :color (:primary-text colors)}}]
    [rn/view {:style (:wrapper styles)}
     [rn/text {:style (:text styles)} label]]))

(defn entry-scene [entry]
  (let [loading? (subscribe [:fetching-entry?])
        remote-error (subscribe [:remote-error :entry])
        entry-content (subscribe [:current-entry])
        content-height (r/atom nil)
        styles {:wrapper {:flex 1}
                :header {:margin-bottom 10
                         :padding-vertical 10
                         :padding-horizontal 10
                         :border-bottom-color (:divider palette)
                         :border-bottom-width 1}
                :feed-info {:flex-direction "row"}
                :loading-content {:flex 1
                                  :padding-left 10
                                  :justify-content "center"
                                  :align-items "center"}
                :content {:padding-horizontal 10
                          :padding-vertical 0}}
        script "
<script>
;(function() {
var wrapper = document.createElement(\"div\");
wrapper.id = \"height-wrapper\";
while (document.body.firstChild) {
wrapper.appendChild(document.body.firstChild);
}
document.body.appendChild(wrapper);
var i = 0;
function updateHeight() {
document.title = wrapper.clientHeight;
window.location.hash = ++i;
}
updateHeight();
window.addEventListener(\"load\", function() {
updateHeight();
setTimeout(updateHeight, 1000);
});
window.addEventlistener(\"resize\", updateHeight);
}())
</script>"]
    (fn [entry]
      [rn/view {:style (:wrapper styles)}
       (when @remote-error
         [remote-error-message])
       [rn/view {:style (:header styles)}
        [rn/text (:title @entry-content)]
        [rn/view {:style (:feed-info styles)}
         [rn/text (str "» " (:title (:feed @entry-content)))]
         (for [tag-label (:tags @entry-content)] ^{:key tag-label} [tag tag-label])]]
       (if @loading?
         [rn/view {:style (:loading-content styles)} [rn/activity-indicator]]
         [rn/scroll-view {:style (:content styles)}
          [rn/view
           [rn/web-view {:style {:height (+ 100 @content-height)}
                         :onNavigationStateChange (fn [event]
                                                    (reset! content-height (js/parseInt (aget event "title") 10)))
                         :javaScriptEnabled true
                         :scrollEnabled false
                         :automaticallyAdjustContentInsets false
                         :source {:html (str "<body>" (:content-body @entry-content) script "</body>")}}]]])])))
