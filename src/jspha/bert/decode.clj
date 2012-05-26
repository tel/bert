(ns jspha.bert.decode
  (:use [clj-time.core :only [epoch plus millis secs]]
        jspha.bert.util))

(defmulti to-clojure
  "Converts a Clojure object representing a BERT to its 'expanded'
  Clojure form." class)

;;; Basic forms
(defmethod to-clojure java.lang.Long   [n] n)
(defmethod to-clojure java.lang.Double [n] n)
(defmethod to-clojure clojure.lang.Keyword [k] k)
(defmethod to-clojure clojure.lang.ISeq [s]
  (map to-clojure s))
(defmethod to-clojure (class (make-array Byte/TYPE 0)) [ba] ba)

;;; Decoding tuples
;;;
;;; In BERT (and Erlang) tuples conventionally encode more complex
;;; data types. For BERT these all begin with the term :bert.
(defmethod to-clojure clojure.lang.APersistentVector [v]
  (let [deep (vec (map to-clojure v))
        [head dispatch & rest] deep]
    (if (= head :bert)
      (case dispatch
        :nil   nil
        :true  true
        :false false
        :dict  (persistent!
                (reduce (fn [map [k v]] (assoc! map k v))
                        (transient (hash-map))
                        (first rest)))
        :time (let [[megs s mils & rest] rest]
                (plus (epoch)
                      (millis (+ (* megs 1000000000)
                                 (* s    1000)
                                 (* mils 1)))))
        :regex (let [[patt flags & rest] rest]
                 (java.util.regex.Pattern/compile
                  (String. patt)
                  (apply + (map #(% *erlang-regex-flags* 0) flags)))))
      deep)))

