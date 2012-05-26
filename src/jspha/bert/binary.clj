(ns jspha.bert.binary
  (:import [com.ericsson.otp.erlang
            OtpErlangObject
            OtpErlangAtom
            OtpErlangByte OtpErlangChar OtpErlangInt OtpErlangLong
            OtpErlangShort OtpErlangUInt OtpErlangUShort
            OtpErlangDouble
            OtpErlangBinary OtpErlangTuple OtpErlangList
            OtpOutputStream OtpInputStream]
           [java.nio ByteBuffer]))

(defmulti encode class)
;;; Numeric types
(defmethod encode java.lang.Byte [x] (OtpErlangByte. x))
(defmethod encode java.lang.Integer [x] (OtpErlangInt. x))
(defmethod encode java.lang.Long [x] (OtpErlangLong. x))
(defmethod encode java.lang.Short [x] (OtpErlangShort. x))
(defmethod encode clojure.lang.BigInt [x] (OtpErlangLong. (.toBigInteger x)))

(defn- encode-float
  "Since BERT uses the old style of Erlang float encoding, we need to
  do this manually." [v]
  (let [float (.getBytes (format "%.20e" v))]
    (-> (doto (ByteBuffer/allocate 32)
          (.put (byte 99))
          (.put float))
        .array)))

(defmethod encode java.lang.Double [x]
  (proxy [OtpErlangObject] []
    (encode [this buf] (.write buf (encode-float x)))))
(defmethod encode java.lang.Float [x]
  (proxy [OtpErlangObject] []
    (encode [this buf] (.write buf (encode-float x)))))

;;; Other types
(defmethod encode clojure.lang.Keyword [k] (OtpErlangAtom. (name k)))
(defmethod encode (class (byte-array 0)) [a] (OtpErlangBinary. a))
(defmethod encode clojure.lang.APersistentVector [v]
  (OtpErlangTuple. (into-array OtpErlangObject (map encode v))))
(defmethod encode clojure.lang.ISeq [s]
  (OtpErlangList. (into-array OtpErlangObject (map encode s))))


(defmulti decode class)

(defmethod decode OtpErlangAtom [x]
  (keyword (.atomValue x)))
(defmethod decode OtpErlangLong [x]
  (.longValue x))
(defmethod decode OtpErlangDouble [x]
  (.doubleValue x))
(defmethod decode OtpErlangBinary [x]
  (.binaryValue x))
(defmethod decode OtpErlangTuple [x]
  (vec (map decode (seq (.elements x)))))
(defmethod decode OtpErlangList [x]
  (map decode (seq (.elements x))))


(defn bytes [thing]
  (.toByteArray (OtpOutputStream. thing)))

(defn debytes [bytes]
  (-> bytes
      OtpInputStream.
      .read_any))