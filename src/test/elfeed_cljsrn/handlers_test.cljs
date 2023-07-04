(ns elfeed-cljsrn.handlers-test
  (:require [cljs.test :refer [deftest is testing]]
            [elfeed-cljsrn.handlers :as handlers]))

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
      (is (= (handlers/search-execute {:db db} [:search/execute {:term term}])
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
      (is (= (handlers/search-execute {:db db} [:search/execute {:term term}])
             {:dispatch [:fetch-entries expected-search-params]
              :db expected-db})))))

(deftest fetch-entries-handler-test
  (testing
   "when term in search-params is empty"
    (let [term ""
          expected-default-term "@15-days-old +unread"
          db {}
          search-params {:term term :default-term expected-default-term :feed-title nil}
          subject (handlers/fetch-entries {:db db} [:fetch/entries search-params])]
      (is (= (:params (:http-xhrio subject))
             {:q expected-default-term}))
      (is (= (:on-success (:http-xhrio subject)) [:success-fetch-entries search-params]))
      (is (= (:db subject) {:fetching-feeds? true :fetching-entries? true}))))
  (testing
   "when there is feed-title in search-params"
    (let [search-params {:term "@15-days-old +unread" :feed-title "Foo"}
          term-without-feed-title (:term search-params)
          db {}
          subject (handlers/fetch-entries {:db db} [:fetch/entries search-params])]
      (is (= (:params (:http-xhrio subject))
             {:q term-without-feed-title}))
      (is (= (:on-success (:http-xhrio subject)) [:success-fetch-entries search-params]))
      (is (= (:db subject) {:fetching-feeds? true :fetching-entries? true})))))

(deftest success-fetch-entries-handler-test
  (let [event-id :success-fetch-entries
        db {}
        response ""]
    (testing
     "when there is feed title"
      (let [search-params {:term "@15-days-old +unread" :feed-title "Foo" :feed-url "http://foo.com"}
            term-with-feed-title (str (:term search-params) " =" (:feed-url search-params))
            subject (handlers/success-fetch-entries {:db db} [event-id search-params response])]
        (is (= (:params (:http-xhrio subject))
               {:q term-with-feed-title}))
        (is (= (:on-success (:http-xhrio subject)) [:process-entries]))
        (is (= (:dispatch-n subject) (list [:process-feeds response]
                                           [:process-total-entries response])))))
    (testing
     "when there isn't feed title"
      (let [search-params {}
            subject (handlers/success-fetch-entries {:db db} [event-id search-params response])]
        (is (= subject
               {:dispatch-n (list [:process-feeds response]
                                  [:process-entries response]
                                  [:process-total-entries response])}))))))
