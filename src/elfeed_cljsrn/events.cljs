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

(defn compose-query-term [search-params]
  (let [term (if (empty? (:term search-params))
               (:default-term search-params)
               (:term search-params))]
    (js/encodeURIComponent
     (clojure.string/trim (str term " " (:feed-title search-params))))))

(def default-http-xhrio-attrs
  {:method :post
   :timeout 5000
   :format (ajax/json-request-format)
   :response-format (ajax/json-response-format {:keywords? true})})

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

(def ->ls
  (after
   (fn [db]
     (let [keys-to-store '(:entry/by-id :entries :nav :server :update-time)]
       (ls/save (select-keys db keys-to-store))))))

;; -- Effect Handlers ----------------------------------------------------------

(reg-fx
 :get-localstore
 (fn [localstore-fx]
   (ls/load #(dispatch (conj (:on-success localstore-fx) %)))))

(reg-fx
 :open-url
 (fn [url]
   (.openURL rn/linking url)))

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
   {:http-xhrio (merge default-http-xhrio-attrs
                       {:uri (str url "/elfeed/update")
                        :on-success [:success-update-server]
                        :on-failure [:failure-update-server]})
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
       (merge events {:http-xhrio (merge default-http-xhrio-attrs
                                         {:uri (str url "/elfeed/update")
                                          :on-success [:success-save-server]
                                          :on-failure [:failure-save-server]})})
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
     {:dispatch-n (list [:fetch-entries (:search db)]
                        [:fetch-update-time])
      :db db}
     {:db db})))

(defn fetch-entries [{db :db} [_event-id search-params]]
  (let [query-term (compose-query-term (dissoc search-params :feed-title))
        uri (str (:url (:server db)) "/elfeed/search")]
    {:http-xhrio (merge default-http-xhrio-attrs
                        {:uri uri
                         :params {:q query-term}
                         :format (ajax/url-request-format)
                         :on-success [:success-fetch-entries search-params]
                         :on-failure [:failure-fetch-entries]})
     :db (assoc db :fetching-feeds? true :fetching-entries? true)}))

(reg-event-fx
 :fetch-entries
 fetch-entries)

(reg-event-db
 :process-entries
 (fn [db [_ response]]
   ;; RN/SwipeableListView needs a collection of elements with id attribute
   (let [by-id (reduce #(merge %1 {(:webid %2) (assoc %2 :id (:webid %2))})
                       {} response)]
     (assoc db
            :fetching-entries? false
            :error-entries false
            :entry/by-id by-id
            :entries (map :webid response)))))

(reg-event-db
 :process-feeds
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
       (assoc :feeds (map (fn [item] (:webid (:feed item))) response)))))

(reg-event-db
 :process-total-entries
 (fn [db [_ response]]
   (assoc db :total-entries (count response))))

(defn success-fetch-entries [{db :db} [_event-id search-params response]]
  (if (:feed-title search-params)
    (let [query-term (compose-query-term search-params)
          uri (str (:url (:server db)) "/elfeed/search")]
      {:http-xhrio (merge default-http-xhrio-attrs
                          {:uri uri
                           :params {:q query-term}
                           :format (ajax/url-request-format)
                           :on-success [:process-entries]
                           :on-failure [:failure-fetch-entries]})
       :dispatch-n (list [:process-feeds response] [:process-total-entries response])})
    {:dispatch-n (list [:process-feeds response]
                       [:process-entries response]
                       [:process-total-entries response])}))

(reg-event-fx
 :success-fetch-entries
 ->ls
 success-fetch-entries)

(reg-event-db
 :failure-fetch-entries
 (fn [db [_ error]]
   (-> db
       (assoc :fetching-entries? false)
       (assoc :error-entries error))))

(reg-event-db
 :toggle-select-entry
 [check-spec]
 (fn [db [_ entry]]
   (if (some #{(:webid entry)} (:selected-entries db))
     (update db :selected-entries (fn [ids] (remove (fn [id] (= (:webid entry) id)) ids)))
     (update db :selected-entries conj (:webid entry)))))

(reg-event-db
 :clear-selected-entries
 [check-spec]
 (fn [db [_ _]]
   (assoc db :selected-entries nil)))

(reg-event-fx
 :press-entry-row
 (fn [{db :db} [_ entry]]
   (if (empty? (:selected-entries db))
     {:dispatch-n (list [:fetch-entry-content entry] [:nav/push (:entry routes)])}
     {:dispatch [:toggle-select-entry entry]})))

(reg-event-fx
 :mark-entries-as-unread
 [check-spec]
 (fn [{db :db} [_ ids]]
   {:http-xhrio (merge default-http-xhrio-attrs
                       {:method :put
                        :uri (str (:url (:server db)) "/elfeed/tags")
                        :params {:entries ids :add (list "unread")}
                        :on-success [:success-mark-entries-as-unread ids]
                        :on-failure [:failure-mark-entries-as-unread ids]})
    :db db}))

(reg-event-db
 :success-mark-entries-as-unread
 [check-spec]
 (fn [db [_ ids response]]
   (update db :entry/by-id (fn [entries]
                             (reduce
                              (fn [acc id]
                                (update-in acc [id] (fn [entry]
                                                      (update entry :tags conj "unread")))
                                ) entries ids)))))

(reg-event-db
 :failure-mark-entries-as-unread
 [check-spec]
 (fn [db [_ ids error]]
   db))

(reg-event-fx
 :mark-entries-as-read
 [check-spec]
 (fn [{db :db} [_ ids]]
   {:http-xhrio (merge default-http-xhrio-attrs
                       {:method :put
                        :uri (str (:url (:server db)) "/elfeed/tags")
                        :params {:entries ids :remove (list "unread")}
                        :on-success [:success-mark-entries-as-read ids]
                        :on-failure [:failure-mark-entries-as-read ids]})
    :db db}))

(defn success-mark-entries-as-read [db [_event-id entry-ids response]]
  (update db :entry/by-id
          (fn [entries]
            (reduce (fn [acc id]
                      (update-in acc [id]
                                 (fn [entry]
                                   (update entry :tags (fn [tags] (remove (fn [tag] (= "unread" tag)) tags)))))) entries entry-ids))))

(reg-event-db
 :success-mark-entries-as-read
 [check-spec]
 success-mark-entries-as-read)

(reg-event-db
 :failure-mark-entries-as-read
 [check-spec]
 (fn [db [_ ids error]]
   db))

(defn fetch-entry-content [{db :db} [_event-id entry]]
  {:http-xhrio (merge default-http-xhrio-attrs
                      {:uri (str (:url (:server db)) "/elfeed/content/" (:content entry))
                       :response-format (ajax/text-response-format)
                       :on-success [:success-fetch-entry-content (:webid entry)]
                       :on-failure [:failure-fetch-entry-content]})
   :db (-> db
           (assoc :error-entry false
                  :current-entry (:webid entry)
                  :fetching-entry? true))})

(reg-event-fx
 :fetch-entry-content
 [check-spec]
 fetch-entry-content)

(defn success-fetch-entry-content [{db :db} [_event-id entry-id response]]
  (let [clean-response (clojure.string/replace response "\n" " ")]
    {:dispatch [:mark-entries-as-read (list entry-id)]
     :db (-> db
             (assoc :fetching-entry? false)
             (assoc-in [:entry/by-id entry-id :content-body] clean-response))}))

(reg-event-fx
 :success-fetch-entry-content
 [->ls check-spec]
 success-fetch-entry-content)

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
   {:http-xhrio (merge default-http-xhrio-attrs
                       {:uri (str (:url (:server db)) "/elfeed/update")
                        :params {:time (:update-time db)}
                        :on-success [:success-fetch-update-time]
                        :on-failure [:failure-fetch-update-time]})
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

(defn search-execute-handler [{db :db} [_event-id search-params]]
  (let [new-search (merge (:search db) search-params {:searching? false})]
    {:dispatch [:fetch-entries new-search]
     :db (assoc db :search new-search)}))

(reg-event-fx
 :search/execute
 [check-spec]
 search-execute-handler)

(reg-event-db
 :search/abort
 [check-spec]
 (fn [db [_ _]]
   (assoc-in db [:search :searching?] false)))
