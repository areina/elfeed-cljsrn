(ns elfeed-cljsrn.events-test
  (:require [cljs.test :refer [deftest is testing]]
            [elfeed-cljsrn.events :as events]))

(deftest search-execute-handler-test
  (testing
    "when the search term is empty"
    (let [term ""
          expected-default-term "@15-days-old +unread"
          db {:search {:default-term expected-default-term }}
          expected-db {:search {:term expected-default-term
                                :default-term expected-default-term
                                :searching? false}}]
      (is (= (events/search-execute-handler {:db db} [:search/execute term])
             {:dispatch [:fetch-entries]
              :db expected-db}))))
  (testing
    "when the search term is not empty"
    (let [term "@95-days-old"
          expected-default-term "@15-days-old +unread"
          db {:search {:default-term expected-default-term }}
          expected-db {:search {:term term
                                :default-term expected-default-term
                                :searching? false}}]
      (is (= (events/search-execute-handler {:db db} [:search/execute term])
             {:dispatch [:fetch-entries]
              :db expected-db})))))
