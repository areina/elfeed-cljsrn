(ns elfeed-cljsrn.events
  (:require [ajax.core :as ajax]
            [day8.re-frame.async-flow-fx]
            [day8.re-frame.http-fx]
            [re-frame.core :refer [reg-event-db after debug dispatch reg-event-fx reg-fx]]
            [cljs.spec :as s]
            [elfeed-cljsrn.local-storage :as ls]
            [elfeed-cljsrn.rn :as rn]
            [elfeed-cljsrn.db :as db :refer [app-db]]))

;; -- Helpers ------------------------------------------------------------------

(defn dec-to-zero
  "Same as dec if not zero"
  [arg]
  (if (< 0 arg)
    (dec arg)
    arg))

;; -- Interceptors -------------------------------------------------------------

(defn check-and-throw
  "Throw an exception if db doesn't have a valid spec."
  [spec db]
  (when-not (s/valid? spec db)
    (let [explain-data (s/explain-data spec db)]
      (throw (ex-info (str "Spec check failed: " explain-data) explain-data)))))

;;TODO Use this interceptor
(def check-spec-interceptor
  (if goog.DEBUG
    (after (partial check-and-throw ::db/app-db))
    []))

(def ->ls (after (fn [db] (ls/save db))))

;; -- Effect Handlers ----------------------------------------------------------

(reg-fx
 :get-localstore
 (fn [localstore-fx]
   (ls/load #(dispatch (conj (:on-success localstore-fx) %)))))

(reg-fx
 :open-url
 (fn [url]
   (.openURL (.-Linking rn/ReactNative) url)))

(reg-fx
 :open-drawer
 (fn [drawer-ref]
   (.openDrawer drawer-ref)))

(reg-fx
 :close-drawer
 (fn [drawer-ref]
   (.closeDrawer drawer-ref)))

;; -- Event Handlers -----------------------------------------------------------

(defn boot-flow []
  {:first-dispatch [:load-localstore]
   :rules [{:when :seen?
            :events :success-load-localstore
            :dispatch-n (list [:fetch-entries] [:fetch-update-time])}
           {:when :seen-both?
            :events [:success-fetch-entries :success-fetch-update-time]
            :dispatch [:success-boot] :halt? true}]})

(reg-event-fx
 :boot
 [debug]
 (fn [_ _]
   {:db (assoc app-db :booting? true)
    :async-flow (boot-flow)}))

(reg-event-db
 :success-boot
 (fn [db [_ _]]
   (assoc db :booting? false)))

(reg-event-fx
 :load-localstore
 debug
 (fn [{db :db} _]
   {:get-localstore {:on-success [:success-load-localstore]}
    :db (assoc db :loading-ls? true)}))

(reg-event-db
 :success-load-localstore
 (fn [db [_ value]]
   (-> db
       (merge value)
       (assoc :loading-ls? false))))

(reg-event-fx
 :fetch-entries
 (fn [{db :db} _]
   {:http-xhrio {:method :post
                 :uri (str (:server db) "/elfeed/search?q=" "@15-days-old %2bunread")
                 :format :text
                 :response-format (ajax/json-response-format {:keywords? true})
                 :keywords? true
                 :on-success [:success-fetch-entries]
                 :on-failure [:failure-fetch-entries]}
    :db (assoc db
               :error-entries false
               :fetching-entries? true)}))

(reg-event-db
 :success-fetch-entries
 ->ls
 (fn [db [_ response]]
   (-> db
       (assoc :fetching-entries? false)
       ;; TODO check if we can do this in a different way. SwipeableListView
       ;; needs a collection of elements with id attribute
       (assoc :entries (map (fn [x] (merge {:id (:webid x)} x)) response)))))

(reg-event-db
 :failure-fetch-entries
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
                 :on-success [:success-mark-entry-as-read]
                 :on-failure [:failure-mark-entry-as-read]}
    :db (update db :recent-reads (fn [coll]
                                   (if coll
                                     (conj coll (:webid entry))
                                     #{(:webid entry)})))}))

(reg-event-db
 :success-mark-entry-as-read
 (fn [db [_ response]]
   db))

(reg-event-db
 :failure-mark-entry-as-read
 (fn [db [_ error]]
   db))

(reg-event-fx
 :fetch-entry-content
 (fn [{db :db} [_ entry]]
   {:http-xhrio {:method :post
                 :uri (str (:server db) "/elfeed/content/" (:content entry))
                 :format :json
                 :response-format (ajax/text-response-format)
                 :on-success [:success-fetch-entry-content entry]
                 :on-failure [:failure-fetch-entry-content]}
    :db (-> db
            (assoc :error-entry false
                   :current-entry (:webid entry)
                   :fetching-entry? true)
            (assoc-in [:entries-m (:webid entry)] entry))}))

(reg-event-db
 :success-fetch-entry-content
 ->ls
 (fn [db [_ entry response]]
   (dispatch [:mark-entry-as-read entry])
   (-> db
       (assoc :fetching-entry? false)
       (assoc-in [:entries-m (:webid entry) :content-body] response))))

(reg-event-db
 :failure-fetch-entry-content
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
                 :on-success [:success-fetch-update-time]
                 :on-failure [:failure-fetch-update-time]}
    :db (assoc db :fetching-update-time true)}))

(reg-event-db
 :success-fetch-update-time
 ->ls
 (fn [db [_ response]]
   (-> db
       (assoc :fetching-update-time false)
       (assoc :update-time response))))

(reg-event-db
 :failure-fetch-update-time
 (fn [db [_ error]]
   (-> db
       (assoc :fetching-update-time? false)
       (assoc :error-update-time error))))

(reg-event-db
 :update-server-value
 ->ls
 (fn [db [_ value]]
   (assoc db :server value)))

(reg-event-fx
 :open-entry-in-browser
 (fn [{db :db} _]
   (let [url (:link (get (:entries-m db) (:current-entry db)))]
     {:open-url url})))

(reg-event-db
 :drawer/set
 (fn [db [_ ref]]
   (assoc-in db [:drawer :ref] ref)))

(reg-event-fx
 :drawer/open
 (fn [{db :db} _]
   {:open-drawer (:ref (:drawer db))
    :db (assoc-in db [:drawer :open?] true)}))

(reg-event-fx
 :drawer/close
 (fn [{db :db} _]
   {:close-drawer (:ref (:drawer db))
    :db (assoc-in db [:drawer :open?] false)}))

(reg-event-db
 :nav/push
 (fn [db [_ value]]
   (-> db
       (update-in [:nav :index] inc)
       (update-in [:nav :routes] #(conj % value)))))

(reg-event-db
 :nav/pop
 (fn [db [_ _]]
   (-> db
       (update-in [:nav :index] dec-to-zero)
       (update-in [:nav :routes] pop))))
