(ns elfeed-cljsrn.events
  (:require [ajax.core :as ajax]
            [day8.re-frame.async-flow-fx]
            [day8.re-frame.http-fx]
            [re-frame.core :refer [reg-event-db after debug dispatch reg-event-fx reg-fx]]
            [cljs.spec :as s]
            [elfeed-cljsrn.navigation :refer [routes]]
            [elfeed-cljsrn.local-storage :as ls]
            [elfeed-cljsrn.rn :as rn]
            [elfeed-cljsrn.db :as db :refer [app-db]]))

;; -- Helpers ------------------------------------------------------------------

(defn dec-to-zero
  "Same as dec if not zero"
  [arg]
  (if (pos? arg)
    (dec arg)
    arg))

(defn valid-url? [url]
  (not (nil? (re-matches #"(https?://)(.*)" url))))

;; -- Interceptors -------------------------------------------------------------

(defn check-and-throw
  "Throw an exception if db doesn't have a valid spec."
  [spec db]
  (when-not (s/valid? spec db)
    (let [explain-data (s/explain-data spec db)]
      (throw (ex-info (str "Spec check failed: " explain-data) explain-data)))))

(def check-spec
  (if goog.DEBUG
    (after (partial check-and-throw ::db/app-db))
    []))

(def ->ls (after (fn [db] (ls/save (select-keys db '(:entries :nav :server :update-time))))))

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
            :dispatch-n (list [:init-nav] [:fetch-content])}
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
 :update-server
 [check-spec]
 (fn [{db :db} [_ url]]
   {:http-xhrio {:method :post
                 :uri (str url "/elfeed/update")
                 :format :json
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [:success-update-server]
                 :on-failure [:failure-update-server]}
    :db (assoc db :server {:url url :checking? true})}))

(reg-event-fx
 :success-update-server
 [->ls check-spec]
 (fn [{db :db} _]
   {:db (update db :server merge {:valid? true :checking? false})}))

(reg-event-fx
 :failure-update-server
 [check-spec]
 (fn [{db :db} [_ error]]
   {:db (assoc db :server {:url nil :valid? false :error-message (:status-text error) :checking? false})}))

(reg-event-fx
 :save-server
 (fn [{db :db} [_ url ]]
   (let [events {:db (assoc db :server {:url url :checking? true})}]
     (if (valid-url? url)
       (merge events {:http-xhrio {:method :post
                                   :uri (str url "/elfeed/update")
                                   :format :json
                                   :response-format (ajax/json-response-format {:keywords? true})
                                   :on-success [:success-save-server]
                                   :on-failure [:failure-save-server]}})
       (merge events {:dispatch [:failure-save-server {:status-text "Invalid URL (should start with http://)"}]})))))

(reg-event-fx
 :success-save-server
 [->ls check-spec]
 (fn [{db :db} _]
   {:dispatch-n (list [:nav/push (:entries routes)] [:fetch-content])
    :db (update db :server merge {:valid? true :checking? false})}))

(reg-event-fx
 :failure-save-server
 (fn [{db :db} [_ error]]
   {:db (assoc db :server {:url nil :valid? false :error-message (:status-text error) :checking? false})}))

(reg-event-db
 :init-nav
 ->ls
 (fn [db _]
   (let [route (if (:valid? (:server db))
                 (:entries routes)
                 (:configure-server routes))]
     (assoc db :nav {:index 0 :routes [route]}))))

(reg-event-fx
 :fetch-content
 (fn [{db :db} _]
   (if (:valid? (:server db))
     {:dispatch-n (list [:fetch-entries] [:fetch-update-time])
      :db db}
     {:db db})))

(reg-event-fx
 :fetch-entries
 (fn [{db :db} _]
   (let [query-term (js/encodeURIComponent
                     (or (:term (:search db)) (:default-term (:search db))))
         uri (str (:url (:server db)) "/elfeed/search?q=" query-term)]
     {:http-xhrio {:method :post
                   :uri uri
                   :format :text
                   :response-format (ajax/json-response-format {:keywords? true})
                   :keywords? true
                   :on-success [:success-fetch-entries]
                   :on-failure [:failure-fetch-entries]}
      :db (assoc db
                 :error-entries false
                 :fetching-entries? true)})))

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
 [check-spec]
 (fn [{db :db} [_ entry]]
   {:http-xhrio {:method :put
                 :uri (str (:url (:server db)) "/elfeed/tags")
                 :params {:entries (list (:webid entry)) :remove (list "unread")}
                 :format (ajax/json-request-format)
                 :response-format (ajax/json-response-format)
                 :on-success [:success-mark-entry-as-read]
                 :on-failure [:failure-mark-entry-as-read]}
    :db (update db :recent-reads (fn [coll]
                                   (if coll
                                     (conj coll (:webid entry))
                                     #{(:webid entry)})))}))

(reg-event-db
 :success-mark-entry-as-read
 [check-spec]
 (fn [db [_ response]]
   db))

(reg-event-db
 :failure-mark-entry-as-read
 [check-spec]
 (fn [db [_ error]]
   db))

(reg-event-fx
 :fetch-entry-content
 [check-spec]
 (fn [{db :db} [_ entry]]
   {:http-xhrio {:method :post
                 :uri (str (:url (:server db)) "/elfeed/content/" (:content entry))
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
 [->ls check-spec]
 (fn [db [_ entry response]]
   (dispatch [:mark-entry-as-read entry])
   (let [clean-response (clojure.string/replace response "\n" " ")]
     (-> db
         (assoc :fetching-entry? false)
         (assoc-in [:entries-m (:webid entry) :content-body] clean-response)))))

(reg-event-db
 :failure-fetch-entry-content
 [check-spec]
 (fn [db [_ error]]
   (-> db
       (assoc :fetching-entry? false)
       (assoc :error-entry error))))

(reg-event-fx
 :fetch-update-time
 (fn [{db :db} _]
   {:http-xhrio {:method :post
                 :uri (str (:url (:server db)) "/elfeed/update")
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
 (fn [{db :db} [_ entry]] {:open-url (:link entry)}))

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
 [check-spec]
 (fn [db [_ value]]
   (-> db
       (update-in [:nav :index] inc)
       (update-in [:nav :routes] #(conj % value)))))

(reg-event-db
 :nav/pop
 [check-spec]
 (fn [db [_ _]]
   (-> db
       (update-in [:nav :index] dec-to-zero)
       (update-in [:nav :routes] pop))))

(reg-event-db
 :connection/set
 [debug]
 (fn [db [_ value]]
   (assoc db :connected? value)))

(reg-event-db
 :search/init
 [check-spec]
 (fn [db [_ _]]
   (assoc-in db [:search :searching?] true)))

(reg-event-db
 :search/clear
 [check-spec]
 (fn [db [_ _]]
   (assoc-in db [:search :term] "")))

(reg-event-fx
 :search/execute
 [check-spec]
 (fn [{db :db} [_ search-term]]
   (let [term (if (empty? search-term) (:default-term (:search db)) search-term)]
     {:dispatch [:fetch-entries]
      :db (-> db
              (assoc-in [:search :term] term)
              (assoc-in [:search :searching?] false))})))

(reg-event-db
 :search/abort
 [check-spec]
 (fn [db [_ _]]
   (assoc-in db [:search :searching?] false)))
