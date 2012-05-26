(ns jspha.bert.encode
  (:use jspha.bert.util)
  (:import [org.joda.time ReadableInstant]))

(defmulti to-erlang
  "Converts a Clojure object to its BERT Erlang primitive
  representation. This function should be idempotent: it reduces
  Clojure objects to their representers which represent themselves."
  class)

;;; Primitives
(defmethod to-erlang java.lang.Number   [n] n)
(defmethod to-erlang clojure.lang.Named [s] (keyword (name s)))
(defmethod to-erlang clojure.lang.APersistentVector [v]
  (vec (map to-erlang v)))
(defmethod to-erlang clojure.lang.ISeq [l]
  (map to-erlang l))
(defmethod to-erlang (class (make-array Byte/TYPE 0)) [ba] ba)
(defmethod to-erlang java.lang.String [str] (.getBytes str))

;;; Combined
(defmethod to-erlang nil [_] [:bert :nil])
(defmethod to-erlang java.lang.Boolean [b]
  (if b [:bert :true] [:bert :false]))
(defmethod to-erlang clojure.lang.APersistentMap [hashmap]
  [:bert :dict (map (fn [[k v]] [(to-erlang k) (to-erlang v)])
                    hashmap)])
(defmethod to-erlang ReadableInstant [inst]
  (let [instant (.getMillis inst)
        megs    (quot instant 1000000000)
        instant (- instant (* megs 1000000000))
        s       (quot instant 1000)
        instant (- instant (* s 1000))]
    [:bert :time megs s instant]))

;;; Regexes
;;;
;;; Regexes are almost automatic, except only the flags which are
;;; accepted by both Erlang and Java are transmitted. These are
;;;
;;; unicode           <-> UNICODE_CASE
;;; caseless          <-> CASE_INSENSITIVE
;;; unicode, caseless <-> CASE_INSENSITIVE, UNICODE_CASE
;;; dotall            <-> DOTALL
;;; multiline         <-> MULTILINE

(defn- make-flags [flag]
  (filter identity
          (map (fn [[name bitmask]]
                 (when (not= 0 (bit-and flag bitmask)) name))
               *erlang-regex-flags*)))

(defmethod to-erlang java.util.regex.Pattern [pat]
  [:bert :regex (.. pat pattern getBytes) (make-flags (.flags pat))])

;;; 