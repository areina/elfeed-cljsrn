(ns elfeed-cljsrn.events
  (:require
   [re-frame.core :refer [reg-event after dispatch]]
   [cljs.spec :as s]
   [ajax.core :refer [GET POST]]
   [elfeed-cljsrn.local-storage :as ls]
   [elfeed-cljsrn.rn :as rn]
   [elfeed-cljsrn.db :as db :refer [app-db db->ls! ls-db-key]]))

;; -- Middleware ------------------------------------------------------------
;;
;; See https://github.com/Day8/re-frame/wiki/Using-Handler-Middleware
;;
(defn check-and-throw
  "Throw an exception if db doesn't have a valid spec."
  [spec db]
  (when-not (s/valid? spec db)
    (let [explain-data (s/explain-data spec db)]
      (throw (ex-info (str "Spec check failed: " explain-data) explain-data)))))

(def validate-spec-mw
  (if goog.DEBUG
    (after (partial check-and-throw ::db/app-db))
    []))

(def ->ls (after db->ls!))

;; -- Handlers --------------------------------------------------------------

(reg-event
 :initialize-db
 validate-spec-mw
 (fn [_ _]
   (ls/get-item ls-db-key (fn [error data]
                            (let [ls-db (cljs.reader/read-string (or data ""))]
                              (dispatch [:ls->db ls-db])
                              (dispatch [:fetch-entries])
                              (dispatch [:fetch-update-time]))))
   (assoc app-db :loading-ls? true)))

(reg-event
 :ls->db
 (fn [db [_ ls-db]]
   (-> db
       (merge ls-db)
       (assoc :loading-ls? false))))

(reg-event
 :process-api-error
 (fn [db [_ error]]
   (-> db
       (assoc :loading-remotely? false)
       (assoc :remote-error error))))

(reg-event
 :fetch-entries
 (fn [db _]
   (let [url (str (:server db) "/elfeed/search?q=" "@15-days-old %2bunread")]
     (POST
         url
         {:handler (fn [data]
                     (dispatch [:process-remote-entries data]))
          :error-handler (fn [error]
                           (dispatch [:process-remote-entries-error error]))
          :response-format :json
          :keywords? true}))
   (-> db
       (assoc :error-entries false)
       (assoc :fetching-entries? true))))

(reg-event
 :process-remote-entries
 ->ls
 (fn [db [_ response]]
   (-> db
       (assoc :fetching-entries? false)
       ;; (assoc :entries-m (into {} (map (fn [entry] {(:webid entry) entry}) response)))
       (assoc :entries response))))

(reg-event
 :process-remote-entries-error
 (fn [db [_ error]]
   (-> db
       (assoc :fetching-entries? false)
       (assoc :error-entries error))))

(reg-event
 :mark-entry-as-read
 (fn [db [_ entry]]
   (let [url (str (:server db) "/elfeed/mark-read" "?webid=" (js/encodeURIComponent (:webid entry)))]
     (POST
         url
         {:handler (fn [data]
                     )
          :format :json
          :error-handler (fn [error]
                           
                           )}))
   (-> db
       (update :recent-reads (fn [coll]
                               (if coll
                                 (conj coll (:webid entry))
                                 #{(:webid entry)}))))))

(reg-event
 :fetch-entry-content
 (fn [db [_ entry]]
   (let [url (str (:server db) "/elfeed/content/" (:content entry))]
     (POST url
         {:handler (fn [data]
                     (dispatch [:process-remote-entry entry data])
                     (dispatch [:mark-entry-as-read entry]))
          :error-handler (fn [error]
                           (dispatch [:process-remote-entry-error error]))}))
   (-> db
       (assoc :error-entry false)
       (assoc :current-entry (:webid entry))
       (assoc-in [:entries-m (:webid entry)] entry)
       (assoc :fetching-entry? true))))

(reg-event
 :process-remote-entry
 ->ls
 (fn [db [_ entry response]]
   (-> db
       (assoc :fetching-entry? false)
       (assoc-in [:entries-m (:webid entry) :content-body] response))))

(reg-event
 :process-remote-entry-error
 (fn [db [_ error]]
   (-> db
       (assoc :fetching-entry? false)
       (assoc :error-entry error))))

(reg-event
 :fetch-update-time
 (fn [db [_ _]]
   (let [url (str (:server db) "/elfeed/update")]
     (POST
         url
         {:params {:time (:update-time db)}
          :handler (fn [data]
                     (dispatch [:process-remote-update-time data]))
          :error-handler (fn [error]
                           (dispatch [:process-remote-update-time-error error]))}))
   (assoc db :fetching-update-time true)))

(reg-event
 :process-remote-update-time
 ->ls
 (fn [db [_ response]]
   (-> db
       (assoc :fetching-update-time false)
       (assoc :update-time response))))

(reg-event
 :process-remote-update-time-error
 (fn [db [_ error]]
   (-> db
       (assoc :fetching-update-time? false)
       (assoc :error-update-time error))))

(reg-event
 :update-server-value
 ->ls
 (fn [db [_ value]]
   (assoc db :server value)))

(reg-event
 :open-entry-in-browser
 (fn [db [_ _]]
   (let [url (:link (get (:entries-m db) (:current-entry db)))]
     (.openURL (.-Linking rn/ReactNative) url))
   db))

(reg-event
 :set-active-panel
 (fn [db [_ active-panel]]
   (assoc db :active-panel active-panel)))

(reg-event
 :set-android-drawer
 (fn [db [_ drawer]]
   (assoc db :android-drawer drawer)))
