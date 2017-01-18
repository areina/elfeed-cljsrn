(ns elfeed-cljsrn.events-test
  (:require [cljs.test :refer [deftest is testing]]
            [elfeed-cljsrn.events :as events]))

(deftest search-execute-handler-test
  (testing
    "when the search term is empty"
    (let [term ""
          expected-default-term "@15-days-old +unread"
          db {:search {:default-term expected-default-term}}
          expected-search-params {:term ""
                                  :default-term expected-default-term
                                  :searching? false}
          expected-db {:search expected-search-params}]
      (is (= (events/search-execute-handler {:db db} [:search/execute {:term term}])
             {:dispatch [:fetch-entries expected-search-params]
              :db expected-db}))))
  (testing
    "when the search term is not empty"
    (let [term "@95-days-old"
          expected-default-term "@15-days-old +unread"
          db {:search {:default-term expected-default-term}}
          expected-search-params {:term term
                                  :default-term expected-default-term
                                  :searching? false}
          expected-db {:search expected-search-params}]
      (is (= (events/search-execute-handler {:db db} [:search/execute {:term term}])
             {:dispatch [:fetch-entries expected-search-params]
              :db expected-db})))))
