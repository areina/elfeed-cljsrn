(ns elfeed-cljsrn.events
  (:require [clojure.string :as str]
            [day8.re-frame.async-flow-fx]
            [day8.re-frame.http-fx]
            [elfeed-cljsrn.db :as db :refer [app-db db-schema]]
            [elfeed-cljsrn.effects]
            [elfeed-cljsrn.handlers :as handlers]
            [elfeed-cljsrn.local-storage :as ls]
            [malli.core :as m]
            [malli.dev.pretty :as mp]
            [re-frame.core :as re-frame :refer [reg-event-db dispatch reg-event-fx]]))

;; -- Interceptors -------------------------------------------------------------

(defn check-and-throw
  "Throws an exception if `db` doesn't match the schema `schema`."
  [schema db]
  (when-not (m/validate schema db)
    (throw (ex-info (str "schema validation failed: " (mp/explain schema db)) {}))))

(def check-schema-interceptor
  (when ^boolean goog.DEBUG
    (re-frame/after (partial check-and-throw db-schema))))

(def debug-interceptor
  (when ^boolean goog.DEBUG re-frame/debug))

(def ->ls
  (re-frame/after
   (fn [db]
     (let [keys-to-store '(:entry/by-id :feed/by-id :entries :current-entry :feeds :server :update-time)]
       (ls/save (select-keys db keys-to-store))))))

(def interceptors [check-schema-interceptor ->ls])

;; -- Event Handlers -----------------------------------------------------------

(defn boot-flow []
  {:first-dispatch [:load-localstore]
   :rules [{:when :seen?
            :events :success-load-localstore
            :dispatch-n (list [:add-connection-listener] [:fetch-content] [:success-boot])
            :halt? true}]})

(reg-event-fx
 :boot
 [debug-interceptor]
 (fn [_ _]
   {:db (assoc app-db :booting? true)
    :async-flow (boot-flow)}))

(reg-event-fx
 :success-boot
 [debug-interceptor]
 (fn [{db :db} _]
   {:db (assoc db :booting? false)}))

(reg-event-fx
 :hide-splash-screen
 [debug-interceptor]
 (fn [_ _]
   {:hide-splash-screen nil}))

(reg-event-fx
 :load-localstore
 [debug-interceptor]
 (fn [{db :db} _]
   {:get-localstore {:on-success [:success-load-localstore]}
    :db (assoc db :loading-ls? true)}))

(reg-event-db
 :success-load-localstore
 interceptors
 (fn [db [_ value]]
   (-> db
       (merge value)
       (assoc :loading-ls? false))))

(reg-event-fx
 :add-connection-listener
 interceptors
 (fn [{db :db} _]
   {:add-netinfo-listener (fn [connection-info]
                            (dispatch [:connection/set (.-isConnected connection-info)]))
    :db db}))

(reg-event-fx
 :update-server
 interceptors
 handlers/update-server)

(reg-event-fx
 :success-update-server
 interceptors
 (fn [{db :db} _]
   {:db (update db :server merge {:valid? true :checking? false})}))

(reg-event-fx
 :failure-update-server
 interceptors
 (fn [{db :db} [_ error]]
   (let [message (str/join " " (list (:status-text error) (:debug-message error)))]
     {:db (update db :server merge {:valid? false :error-message message :checking? false})})))

(reg-event-fx
 :save-server
 [debug-interceptor]
 handlers/save-server)

(reg-event-fx
 :success-save-server
 interceptors
 (fn [{db :db} _]
   {:dispatch-n (list [:fetch-content])
    :db (update db :server merge {:valid? true :checking? false})}))

(reg-event-fx
 :failure-save-server
 [debug-interceptor]
 (fn [{db :db} [_ error]]
   {:db (assoc db :server {:url nil :valid? false :error-message (:status-text error) :checking? false})}))

(reg-event-fx
 :fetch-content
 interceptors
 (fn [{db :db} _]
   (if (:valid? (:server db))
     {:dispatch-n (list [:fetch-entries (:search db)]
                        [:fetch-update-time])
      :db db}
     {:db db})))

(reg-event-fx
 :fetch-entries
 interceptors
 handlers/fetch-entries)

(reg-event-db
 :process-entries
 interceptors
 (fn [db [_ response]]
   ;; RN/SwipeableListView needs a collection of elements with id attribute
   (let [by-id (reduce #(merge %1 {(:webid %2) (assoc %2 :id (:webid %2))})
                       {} response)]
     (-> db
         (dissoc :error-entries)
         (assoc :fetching-entries? false
                :entry/by-id by-id
                :entries (map :webid response))))))

(reg-event-db
 :process-feeds
 interceptors
 (fn [db [_ response]]
   (-> db
       (assoc :fetching-feeds? false)
       (assoc :feed/by-id (reduce (fn [acc item]
                                    (let [feed (:feed item)
                                          feed-id (:webid feed)]
                                      (if (get acc feed-id)
                                        (update-in acc [feed-id :total] + 1)
                                        (merge acc {feed-id (assoc feed
                                                                   :id feed-id
                                                                   :total 1)}))))
                                  {}
                                  response))
       (assoc :feeds (distinct (map (fn [item] (:webid (:feed item))) response))))))

(reg-event-fx
 :success-fetch-entries
 interceptors
 handlers/success-fetch-entries)

(reg-event-db
 :failure-fetch-entries
 interceptors
 (fn [db [_ error]]
   (-> db
       (assoc :fetching-feeds? false)
       (assoc :fetching-entries? false)
       (assoc :error-entries error))))

(reg-event-db
 :toggle-select-entry
 interceptors
 (fn [db [_ entry]]
   (if (some #{(:webid entry)} (:selected-entries db))
     (update db :selected-entries (fn [ids] (remove (fn [id] (= (:webid entry) id)) ids)))
     (update db :selected-entries conj (:webid entry)))))

(reg-event-db
 :clear-selected-entries
 interceptors
 (fn [db [_ _]]
   (assoc db :selected-entries nil)))

(reg-event-fx
 :press-entry-row
 interceptors
 (fn [{db :db} [_ entry]]
   (if (empty? (:selected-entries db))
     {:dispatch-n (list [:fetch-entry-content entry])}
     {:dispatch [:toggle-select-entry entry]})))

(reg-event-fx
 :mark-entries-as-unread
 interceptors
 handlers/mark-entries-as-unread)

(reg-event-db
 :success-mark-entries-as-unread
 interceptors
 handlers/success-mark-entries-as-unread)

(reg-event-db
 :failure-mark-entries-as-unread
 interceptors
 (fn [db [_ _ids error]]
   (assoc db :error-mark-entries error)))

(reg-event-fx
 :mark-entries-as-read
 interceptors
 handlers/mark-entries-as-read)

(reg-event-db
 :success-mark-entries-as-read
 interceptors
 handlers/success-mark-entries-as-read)

(reg-event-db
 :failure-mark-entries-as-read
 interceptors
 (fn [db [_ _ids error]]
   (assoc db :error-mark-entries error)))

(reg-event-fx
 :fetch-entry-content
 interceptors
 handlers/fetch-entry-content)

(reg-event-fx
 :success-fetch-entry-content
 interceptors
 handlers/success-fetch-entry-content)

(reg-event-db
 :failure-fetch-entry-content
 interceptors
 (fn [db [_ error]]
   (-> db
       (assoc :fetching-entry? false)
       (assoc :error-entry error))))

(reg-event-fx
 :fetch-update-time
 interceptors
 handlers/fetch-update-time)

(reg-event-db
 :success-fetch-update-time
 interceptors
 (fn [db [_ response]]
   (-> db
       (assoc :fetching-update-time? false)
       (assoc :update-time response)
       (dissoc :error-update-time))))

(reg-event-db
 :failure-fetch-update-time
 interceptors
 (fn [db [_ error]]
   (-> db
       (assoc :fetching-update-time? false)
       (assoc :error-update-time error))))

(reg-event-db
 :update-server-value
 interceptors
 (fn [db [_ value]]
   (assoc db :server value)))

(defn open-url-in-browser [url]
  {:open-url url})

(reg-event-fx
 :open-url-in-browser
 interceptors
 (fn [{db :db} [_ url]]
   (open-url-in-browser url)))

(reg-event-fx
 :open-entry-in-browser
 interceptors
 (fn [{db :db} [_ entry-id]]
   (open-url-in-browser (:link (get (:entry/by-id db) entry-id)))))

(reg-event-db
 :process-total-entries
 (fn [db [_ response]]
   (assoc db :total-entries (count response))))

(reg-event-db
 :connection/set
 interceptors
 (fn [db [_ value]]
   (assoc db :connected? value)))

(reg-event-db
 :search/init
 interceptors
 (fn [db [_ _]]
   (assoc-in db [:search :searching?] true)))

(reg-event-db
 :search/clear
 interceptors
 (fn [db [_ _]]
   (-> db
       (update :search dissoc :term))))

(reg-event-fx
 :search/execute
 interceptors
 handlers/search-execute)

(reg-event-db
 :search/abort
 interceptors
 (fn [db [_ _]]
   (assoc-in db [:search :searching?] false)))
