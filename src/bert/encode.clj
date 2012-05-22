(ns bert.encode
  (:import [org.joda.time ReadableInstant]))

;;; BERT -- Binary Erlang Terms
;;;
;;; BERT is a binary serialization library based on Erlang's
;;; erlang:term_to_binary/1 function. For full details on the protocol
;;; itself see
;;;
;;; http://bert-rpc.org/

;;; BERT <-> Clojure Mapping
;;;
;;; BERT begins from a set of Erlang primitives and builds more
;;; powerful structures from them. The primitives are Erlang
;;; primitives and map to Clojure forms as below
;;;
;;; integer: 4                <->                 4 :long
;;;   float: 8.1516           <->            8.1516 :double
;;;    atom: foo              <->              :foo :keyword
;;;   tuple: {coord, 23, 42}  <->    [:coord 23 42] :vector
;;;    list: [a, [1, 2]]      <->       '(:a (1 2)) :seq
;;;  binary: <<"roses\0">>    <->     #<byte[] ...> :byte array
;;;
;;; With some additional Clojure -> Erlang conversions
;;;
;;; string: "foo"  <->  <<"foo">> :binary
;;; symbol: 'foo   <->        foo :atom
;;;
;;; Atop those primitives, BERT builds more complex forms. These are
;;; automatically encoded/decoded from corresponding Clojure objects.
;;;
;;;        nil: {bert, nil}                     <->              nil :nil
;;;    boolean: {bert, true}                    <->             true :boolean
;;; dictionary: {bert, dict, [{k,v}, ...]}      <->        {k v ...} :hashmap
;;;       time: {bert, time, Ms, s, ms}         <->  #<DateTime ...> :datetime
;;;      regex: {bert, regex, Source, Options}  <->           #"..." :compiled regex
;;;
;;; Where the datetimes are backed by JodaTime (via clj-time) and the
;;; regexes are your regular compiled Java regexes.

(defmulti to-erlang
  "Converts a Clojure object to its BERT Erlang primitive
  representation. This function should be idempotent: it reduces
  Clojure objects to their representers which represent themselves."
  class)

;;; Primitives
(defmethod to-erlang java.lang.Long   [n] n)
(defmethod to-erlang java.lang.Float  [n] n)
(defmethod to-erlang java.lang.Double [n] n)
(defmethod to-erlang clojure.lang.Named [s] (keyword (name s)))
(defmethod to-erlang clojure.lang.APersistentVector [v]
  (vec (map to-erlang v)))
(defmethod to-erlang clojure.lang.ISeq [l]
  (map to-erlang l))
(defmethod to-erlang (class (make-array Byte/TYPE 0)) [ba] ba)
(defmethod to-erlang java.lang.String [str] (.getBytes str))

;;; Combined
(defmethod to-erlang nil [_] [:bert nil])
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

(def *erlang-regex-flags*
  {:caseless java.util.regex.Pattern/CASE_INSENSITIVE
   :unicode java.util.regex.Pattern/UNICODE_CASE
   :dotall java.util.regex.Pattern/DOTALL
   :multiline java.util.regex.Pattern/MULTILINE})

(defn- make-flags [flag]
  (filter identity
          (map (fn [[name bitmask]]
                 (when (not= 0 (bit-and flag bitmask)) name))
               *erlang-regex-flags*)))

(defmethod to-erlang java.util.regex.Pattern [pat]
  [:bert :regex (.. pat pattern getBytes) (make-flags (.flags pat))])

;;; 