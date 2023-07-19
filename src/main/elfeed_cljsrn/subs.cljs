(ns elfeed-cljsrn.subs
  (:require [re-frame.core :refer [reg-sub]]
            [clojure.string :as str]))

(defn get-entry [db entry-id]
  (let [entry (get (:entry/by-id db) entry-id)
        unread? (boolean (some #{"unread"} (:tags entry)))
        selected? (boolean (some #{entry-id} (:selected-entries db)))]
    (merge {:selected? selected? :unread? unread?} entry)))

(reg-sub
 :search/state
 (fn [db]
   (let [current-term (or (:term (:search db))
                          (:default-term (:search db)))]
     (assoc (:search db) :current-term current-term))))

(reg-sub
 :feed/by-id
 (fn [db]
   (:feed/by-id db)))

(reg-sub
 :feeds
 :<- [:search/state]
 :<- [:feed/by-id]
 (fn [[search feeds]]
   (sort-by (comp str/lower-case :title)
            (map (fn [[_id feed]]
                   (assoc feed :selected? (= (:title feed) (:feed-title search)))) feeds))))

(reg-sub
 :total-entries
 (fn [db]
   (:total-entries db)))

(reg-sub
 :entries
 (fn [db _]
   (map (fn [entry-id] (get-entry db entry-id)) (:entries db))))

(reg-sub
 :loading?
 (fn [db]
   (or (:fetching-entries? db)
       (:fetching-update-time? db))))

(reg-sub
 :fetching-entry?
 (fn [db]
   (:fetching-entry? db)))

(reg-sub
 :current-entry
 (fn [db]
   (let [id (:current-entry db)]
     (get (:entry/by-id db) id))))

(reg-sub
 :selected-entries
 (fn [db]
   (map (fn [entry-id] (get-entry db entry-id)) (:selected-entries db))))

(reg-sub
 :server
 (fn [db]
   (:server db)))

(reg-sub
 :drawer
 (fn [db]
   (:drawer db)))

(reg-sub
 :remote-error
 (fn [db [_ key]]
   ((keyword (str "error-" (name key))) db)))

(reg-sub
 :update-time
 (fn [db]
   (:update-time db)))

(reg-sub
 :server-configured?
 (fn [db]
   (boolean (seq (:url (:server db))))))

(reg-sub
 :connected?
 (fn [db]
   (:connected? db)))

(reg-sub
 :booting?
 (fn [db]
   (:booting? db)))

(reg-sub
 :ui/feedback
 (fn [db]
   (:feedback (:ui db))))
