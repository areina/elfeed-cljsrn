(ns elfeed-cljsrn.android.scenes.settings
  (:require
   [reagent.react-native-paper :as paper]
   [reagent.react-native :as rn]
   [reagent.core :as r]
   [re-frame.core :refer [subscribe dispatch]]
   [elfeed-cljsrn.components :refer [button]]
   [elfeed-cljsrn.events]
   [elfeed-cljsrn.subs]))

(defn ^:private feedback-message [{:keys [message on-dismiss]}]
  [rn/view {:style {:flex 1
                    :justify-content "space-between"}}
   [paper/snackbar {:visible (boolean message)
                    :on-dismiss on-dismiss} message]])

(defn settings [{:keys [url _updating? _error-message _on-update]}]
  (let [server-url (r/atom url)
        on-change-text (fn [text]
                         (reset! server-url text)
                         (r/flush))
        styles {:wrapper {:flex 1
                          :padding-top 16
                          :padding-horizontal 16}}]
    (fn [{:keys [_url updating? error-message on-update]}]
      (let [has-error? (boolean (seq error-message))]
        [rn/view {:style (:wrapper styles)}
         [paper/text-input {:mode "outlined"
                            :label "Elfeed url"
                            :keyboard-type "url"
                            :error has-error?
                            :on-change-text on-change-text
                            :value @server-url}]
         [paper/helper-text {:type "error" :visible has-error?} error-message]
         [button {:style {:align-self "flex-end"}
                  :mode "contained-tonal"
                  :loading updating?
                  :disabled updating?
                  :on-press (fn [_e] (on-update @server-url))}
          (if updating? "UPDATING" "UPDATE")]]))))

(defn settings-scene [{:keys [^js _navigation ^js _route]}]
  (let [server-info (subscribe [:server])
        feedback (subscribe [:ui/feedback])]
    (fn [{:keys [^js _navigation ^js _route]}]
      (when (:url @server-info)
        [:<>
         [settings {:url (:url @server-info)
                    :updated-at (:updated-at @server-info)
                    :error-message (:error-message @server-info)
                    :updating? (:checking? @server-info)
                    :on-update (fn [new-url] (dispatch [:update-server new-url]))}]
         [feedback-message {:message (:message @feedback)
                            :on-dismiss (fn []
                                          (dispatch [:ui/dismiss-feedback]))}]]))))
