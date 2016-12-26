(ns elfeed-cljsrn.subs
  (:require [re-frame.core :refer [reg-sub]]))

(reg-sub
 :entries
 (fn [db _]
   (map (fn [entry-id] (get (:entry/by-id db) entry-id)) (:entries db))))

(reg-sub
 :recent-reads
 (fn [db _]
   (:recent-reads db)))

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
