(ns elfeed-cljsrn.subs
  (:require [re-frame.core :refer [reg-sub]]))

(defn get-entry [db entry-id]
  (let [entry (get (:entry/by-id db) entry-id)
        unread? (boolean (some #{"unread"} (:tags entry)))
        selected? (boolean (some #{entry-id} (:selected-entries db)))]
    (merge {:selected? selected? :unread? unread?} entry)))

(reg-sub
 :entries
 (fn [db _]
   (map (fn [entry-id] (get-entry db entry-id)) (:entries db))))

(reg-sub
 :loading?
 (fn [db]
   (:fetching-entries? db)))

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
 :nav/state
 (fn [db]
   (:nav db)))

(reg-sub
 :search/state
 (fn [db]
   (:search db)))
