(ns elfeed-cljsrn.events
  (:require [ajax.core :as ajax]
            [cljs.reader :as reader]
            [day8.re-frame.http-fx]
            [re-frame.core :refer [reg-event-db after dispatch reg-event-fx]]
            [cljs.spec :as s]
            [elfeed-cljsrn.local-storage :as ls]
            [elfeed-cljsrn.rn :as rn]
            [elfeed-cljsrn.db :as db :refer [app-db db->ls! ls-db-key]]))

(defn dec-to-zero
  "Same as dec if not zero"
  [arg]
  (if (< 0 arg)
    (dec arg)
    arg))

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

(reg-event-db
 :initialize-db
 validate-spec-mw
 (fn [_ _]
   (ls/get-item ls-db-key (fn [error data]
                            (let [ls-db (cljs.reader/read-string (or data ""))]
                              (dispatch [:ls->db ls-db])
                              (dispatch [:fetch-entries])
                              (dispatch [:fetch-update-time]))))
   (assoc app-db :loading-ls? true)))

(reg-event-db
 :ls->db
 (fn [db [_ ls-db]]
   (-> db
       (merge ls-db)
       (assoc :loading-ls? false))))

(reg-event-db
 :process-api-error
 (fn [db [_ error]]
   (-> db
       (assoc :loading-remotely? false)
       (assoc :remote-error error))))

(reg-event-fx
 :fetch-entries
 (fn [{db :db} _]
   {:http-xhrio {:method :post
                 :uri (str (:server db) "/elfeed/search?q=" "@15-days-old %2bunread")
                 :format :text
                 :response-format (ajax/json-response-format {:keywords? true})
                 :keywords? true
                 :on-success [:process-remote-entries]
                 :on-failure [:process-remote-entries-error]}
    :db (assoc db
               :error-entries false
               :fetching-entries? true)}))

(reg-event-db
 :process-remote-entries
 ->ls
 (fn [db [_ response]]
   (-> db
       (assoc :fetching-entries? false)
       ;; TODO check if we can do this in a different way. SwipeableListView
       ;; needs a collection of elements with id attribute
       (assoc :entries (map (fn [x] (merge {:id (:webid x)} x)) response)))))

(reg-event-db
 :process-remote-entries-error
 (fn [db [_ error]]
   (-> db
       (assoc :fetching-entries? false)
       (assoc :error-entries error))))

(reg-event-fx
 :mark-entry-as-read
 (fn [{db :db} [_ entry]]
   {:http-xhrio {:method :post
                 :uri (str (:server db) "/elfeed/mark-read" "?webid=" (js/encodeURIComponent (:webid entry)))
                 :format :json
                 :response-format (ajax/json-response-format)
                 :on-success [:process-mark-entry-as-read]
                 :on-failure [:process-mark-entry-as-read-error]}
    :db (update db :recent-reads (fn [coll]
                                   (if coll
                                     (conj coll (:webid entry))
                                     #{(:webid entry)})))}))

(reg-event-db
 :process-mark-entry-as-read
 (fn [db [_ response]]
   db))

(reg-event-db
 :process-mark-entry-as-read-error
 (fn [db [_ error]]
   db))

(reg-event-fx
 :fetch-entry-content
 (fn [{db :db} [_ entry]]
   {:http-xhrio {:method :post
                 :uri (str (:server db) "/elfeed/content/" (:content entry))
                 :format :json
                 :response-format (ajax/text-response-format)
                 :on-success [:process-remote-entry entry]
                 :on-failure [:process-remote-entry-error]}
    :db (-> db
            (assoc :error-entry false
                   :current-entry (:webid entry)
                   :fetching-entry? true)
            (assoc-in [:entries-m (:webid entry)] entry))}))

(reg-event-db
 :process-remote-entry
 ->ls
 (fn [db [_ entry response]]
   (dispatch [:mark-entry-as-read entry])
   (-> db
       (assoc :fetching-entry? false)
       (assoc-in [:entries-m (:webid entry) :content-body] response))))

(reg-event-db
 :process-remote-entry-error
 (fn [db [_ error]]
   (-> db
       (assoc :fetching-entry? false)
       (assoc :error-entry error))))

(reg-event-fx
 :fetch-update-time
 (fn [{db :db} _]
   {:http-xhrio {:method :post
                 :uri (str (:server db) "/elfeed/update")
                 :params {:time (:update-time db)}
                 :format :json
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [:process-remote-update-time]
                 :on-failure [:process-remote-update-time-error]}
    :db (assoc db :fetching-update-time true)}))

(reg-event-db
 :process-remote-update-time
 ->ls
 (fn [db [_ response]]
   (-> db
       (assoc :fetching-update-time false)
       (assoc :update-time response))))

(reg-event-db
 :process-remote-update-time-error
 (fn [db [_ error]]
   (-> db
       (assoc :fetching-update-time? false)
       (assoc :error-update-time error))))

(reg-event-db
 :update-server-value
 ->ls
 (fn [db [_ value]]
   (assoc db :server value)))

(reg-event-db
 :open-entry-in-browser
 (fn [db [_ _]]
   (let [url (:link (get (:entries-m db) (:current-entry db)))]
     (.openURL (.-Linking rn/ReactNative) url))
   db))

(reg-event-db
 :set-active-panel
 (fn [db [_ active-panel]]
   (assoc db :active-panel active-panel)))

(reg-event-db
 :drawer/set
 (fn [db [_ ref]]
   (assoc-in db [:drawer :ref] ref)))

(reg-event-db
 :drawer/open
 (fn [db [_ _]]
   (.openDrawer (:ref (:drawer db)))
   (assoc-in db [:drawer :open?] true)))

(reg-event-db
 :drawer/close
 (fn [db [_ _]]
   (.closeDrawer (:ref (:drawer db)))
   (assoc-in db [:drawer :open?] false)))

(reg-event-db
 :nav/push
 (fn [db [_ value]]
   (-> db
       (update-in [:nav :index] inc)
       (update-in [:nav :routes] #(conj % value)))))

(reg-event-db
 :nav/pop
 validate-spec-mw
 (fn [db [_ _]]
   (-> db
       (update-in [:nav :index] dec-to-zero)
       (update-in [:nav :routes] pop))))
