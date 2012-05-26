(ns jspha.bert.decode-test
  (:use expectations
        jspha.bert.decode
        [clj-time.core :only [date-time epoch]]))

(given [obj dec] (expect dec (to-clojure obj))
       1 1
       1.5 1.5
       :a :a
       (list :a :B :c) (list :a :B :c)
       [:a :b] [:a :b]
       [:bert :dict (list [:a 1] [:b 2])] {:a 1 :b 2}
       [:bert :nil] nil
       [:bert :true] true
       [:bert :false] false
       [:bert :time 0 0 0] (epoch))

;;; Check regex, not the flags yet though
(expect (str #"foo")
        (-> [:bert :regex (.getBytes "foo") (list)]
            to-clojure
            str))