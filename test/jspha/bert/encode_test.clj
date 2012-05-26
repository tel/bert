(ns jspha.bert.encode-test
  (:use expectations
        jspha.bert.encode
        [clj-time.core :only [date-time epoch]]))

;;; to-erlang idempotency
(given [obj] (expect (to-erlang obj) (to-erlang (to-erlang obj)))
       1 1.0 1.5
       :a 'a
       true false nil
       [:a :b 3] (list 1 2 3)
       {:a 1}
       ;; bytestrings can't be compared properly
       ;; #"foo" 
       (date-time 100))

(given [obj enc] (expect enc (to-erlang obj))
       1 1
       1.0 1.0
       :a :a
       'a :a
       [:a 'b] [:a :b]
       (list 1 2 3) (list 1 2 3)
       {:a 1} [:bert :dict (list [:a 1])]
       nil [:bert :nil]
       true [:bert :true]
       false [:bert :false]
       (epoch) [:bert :time 0 0 0])