(ns elfeed-cljsrn.handlers
  (:require [ajax.core :as ajax]
            [clojure.string :as str]))

(def default-http-xhrio-attrs
  {:method :post
   :timeout 5000
   :format (ajax/json-request-format)
   :response-format (ajax/json-response-format {:keywords? true})})

(defn ^:private compose-query-term [search-params]
  (let [term (if (empty? (:term search-params))
               (:default-term search-params)
               (:term search-params))
        feed (when (:feed-url search-params) (str " =" (:feed-url search-params)))]
    (str/trim (str term feed))))

(defn ^:private valid-url? [url]
  (not (nil? (re-matches #"(https?://)(.*)" url))))

(defn fetch-entry-content [{db :db} [_event-id entry]]
  (if (:content-body (get (:entry/by-id db) (:webid entry)))
    {:db db}
    {:http-xhrio (merge default-http-xhrio-attrs
                        {:uri (str (:url (:server db)) "/elfeed/content/" (:content entry))
                         :response-format (ajax/text-response-format)
                         :on-success [:success-fetch-entry-content (:webid entry)]
                         :on-failure [:failure-fetch-entry-content (:webid entry)]})
     :db (-> db
             ;; (assoc-in [:entry/by-id (:webid entry) :fetching?] true)
             ;; (assoc :error-entry false
             ;;        ;; :current-entry (:webid entry)
             ;;        ;; :fetching-entry? true
             ;;        )
             )}))
(defn fetch-entries [{db :db} [_event-id search-params]]
  (let [query-term (compose-query-term (dissoc search-params :feed-url))
        uri (str (:url (:server db)) "/elfeed/search")]
    {:http-xhrio (merge default-http-xhrio-attrs
                        {:uri uri
                         :method :get
                         :params {:q query-term}
                         :format (ajax/url-request-format)
                         :on-success [:success-fetch-entries search-params]
                         :on-failure [:failure-fetch-entries]})
     :db (assoc db :fetching-feeds? true :fetching-entries? true)}))

(defn fetch-update-time [{db :db} _]
  {:http-xhrio (merge default-http-xhrio-attrs
                      {:uri (str (:url (:server db)) "/elfeed/update")
                       :method :get
                       :on-success [:success-fetch-update-time]
                       :on-failure [:failure-fetch-update-time]})
   :db (assoc db :fetching-update-time? true)})

(defn mark-entries-as [{db :db} [_ state ids]]
  (let [params {:entries ids
                :add (when (= state :unread) (list "unread"))
                :remove (when (= state :read) (list "unread"))}]
    {:http-xhrio (merge default-http-xhrio-attrs
                        {:method :put
                         :uri (str (:url (:server db)) "/elfeed/tags")
                         :params params
                         :on-success [:success-mark-entries-as state ids]
                         :on-failure [:failure-mark-entries-as state ids]})
     :db db}))

(defn search-execute [{db :db} [_event-id search-params]]
  (let [new-search (merge (:search db) search-params {:searching? false})]
    {:dispatch [:fetch-entries new-search]
     :db (assoc db :search new-search)}))

(defn success-fetch-entries [{db :db} [_event-id search-params response]]
  (if (:feed-url search-params)
    (let [query-term (compose-query-term search-params)
          uri (str (:url (:server db)) "/elfeed/search")]
      {:http-xhrio (merge default-http-xhrio-attrs
                          {:uri uri
                           :params {:q query-term}
                           :format (ajax/url-request-format)
                           :on-success [:process-entries]
                           :on-failure [:failure-fetch-entries]})
       :dispatch-n (list [:process-feeds response]
                         [:process-total-entries response])})
    {:dispatch-n (list [:process-feeds response]
                       [:process-entries response]
                       [:process-total-entries response])}))

(defn success-fetch-entry-content [{db :db} [_event-id entry-id response]]
  (let [cleaned-response (str/replace response "\n" " ")]
    {:dispatch [:mark-entries-as :read (list entry-id)]
     :db (-> db
             (assoc-in [:entry/by-id entry-id :content-body] cleaned-response)
             (assoc-in [:entry/by-id entry-id :error-fetching] nil)
             ;; (assoc :fetching-entry? false)
             )}))

(defn success-mark-entries-as [db [_event-id state ids _response]]
  (let [next-state-fn (if (= state :read)
                        (fn [entry]
                          (update entry :tags (fn [tags] (remove (fn [tag] (= "unread" tag)) tags))))
                        (fn [entry]
                          (update entry :tags conj "unread")))]
    (-> db
        (dissoc :error-mark-entries)
        (update :entry/by-id
                (fn [entries]
                  (reduce
                   (fn [acc id]
                     (update-in acc [id] next-state-fn)) entries ids))))))

(defn save-server [{db :db} [_event-id url]]
  (let [events {:db (assoc db :server {:url url :valid? false :checking? true})}]
    (if (valid-url? url)
      (merge events {:http-xhrio (merge default-http-xhrio-attrs
                                        {:uri (str url "/elfeed/update")
                                         :method :get
                                         :on-success [:success-save-server]
                                         :on-failure [:failure-save-server]})})
      (merge events {:dispatch [:failure-save-server {:status-text "Invalid URL (should start with http://)"}]}))))

(defn update-server [{db :db} [_event-id url]]
  {:http-xhrio (merge default-http-xhrio-attrs
                      {:uri (str url "/elfeed/update")
                       :on-success [:success-update-server]
                       :on-failure [:failure-update-server]})
   :db (update db :server merge {:url url :checking? true})})
