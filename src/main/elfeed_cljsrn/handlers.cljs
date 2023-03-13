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
  {:http-xhrio (merge default-http-xhrio-attrs
                      {:uri (str (:url (:server db)) "/elfeed/content/" (:content entry))
                       :response-format (ajax/text-response-format)
                       :on-success [:success-fetch-entry-content (:webid entry)]
                       :on-failure [:failure-fetch-entry-content]})
   :db (-> db
           (assoc :error-entry false
                  :current-entry (:webid entry)
                  :fetching-entry? true))})

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

(defn mark-entries-as-unread [{db :db} [_ ids]]
  {:http-xhrio (merge default-http-xhrio-attrs
                      {:method :put
                       :uri (str (:url (:server db)) "/elfeed/tags")
                       :params {:entries ids :add (list "unread")}
                       :on-success [:success-mark-entries-as-unread ids]
                       :on-failure [:failure-mark-entries-as-unread ids]})
   :db db})

(defn mark-entries-as-read [{db :db} [_ ids]]
  {:http-xhrio (merge default-http-xhrio-attrs
                      {:method :put
                       :uri (str (:url (:server db)) "/elfeed/tags")
                       :params {:entries ids :remove (list "unread")}
                       :on-success [:success-mark-entries-as-read ids]
                       :on-failure [:failure-mark-entries-as-read ids]})
   :db db})

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
  (let [clean-response (str/replace response "\n" " ")]
    {:dispatch [:mark-entries-as-read (list entry-id)]
     :db (-> db
             (assoc :fetching-entry? false)
             (assoc-in [:entry/by-id entry-id :content-body] clean-response))}))

(defn success-mark-entries-as-read [db [_event-id entry-ids _response]]
  (-> db
      (dissoc :error-mark-entries)
      (update :entry/by-id
              (fn [entries]
                (reduce (fn [acc id]
                          (update-in acc [id]
                                     (fn [entry]
                                       (update entry :tags (fn [tags] (remove (fn [tag] (= "unread" tag)) tags)))))) entries entry-ids)))))

(defn success-mark-entries-as-unread [db [_event-id ids _response]]
  (-> db
      (dissoc :error-mark-entries)
      (update :entry/by-id (fn [entries]
                             (reduce
                              (fn [acc id]
                                (update-in acc [id] (fn [entry]
                                                      (update entry :tags conj "unread")))) entries ids)))))

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
