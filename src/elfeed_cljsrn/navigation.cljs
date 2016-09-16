(ns elfeed-cljsrn.navigation
  (:require [re-frame.core :refer [dispatch]]))

(def routes {:configure-server {:key :configure-server
                                :title "Configure your Elfeed server"}
             :entries {:key :entries
                       :title "All entries"}
             :entry {:key :entry
                     :title ""}
             :settings {:key :settings
                        :title "Settings"}})

(defn navigate-back []
  (dispatch [:nav/pop nil]))

(defn navigate-to [route-key]
  (dispatch [:nav/push (get routes route-key)]))
