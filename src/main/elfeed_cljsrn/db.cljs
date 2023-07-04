(ns elfeed-cljsrn.db)

;; initial state of app-db
(def app-db {:update-time 0
             :connected? false
             :search {:searching? false
                      :default-term "@15-days-old +unread"}})

(def s-server [:map
               [:url string?]
               [:valid? :boolean]
               [:checking? :boolean]
               [:error-message {:optional true} string?]])

(def s-search [:map
               [:searching? :boolean]
               [:term {:optional true} string?]
               [:default-term :string]
               ;; TODO
               ;; [:feed-title {:optional true} string?]
               ;; [:feed-url {:optional true} string?]
               ])

(def db-schema [:map
                [:current-entry {:optional true} string?]
                [:update-time :int]
                [:connected? :boolean]
                [:search s-search]
                [:entries {:optional true} [:sequential :string]]
                [:server {:optional true} s-server]])
