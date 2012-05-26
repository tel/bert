(ns jspha.bert.core
  (:require (jspha.bert [encode :as e]
                        [decode :as d]
                        [binary :as b]))
  (:import [java.nio ByteBuffer]))

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

(defn bert [thing]
  (-> thing
      e/to-erlang
      b/encode
      b/bytes))

(defn berp [thing]
  (let [bytes (bert thing)
        length (alength bytes)]
    (-> (doto (ByteBuffer/allocate (+ 4 length))
          (.putInt length)
          (.put bytes))
        .array)))

(defn debert [bytes]
  (-> bytes
      b/debytes
      b/decode
      d/to-clojure))