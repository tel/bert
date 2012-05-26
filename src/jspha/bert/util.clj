(ns jspha.bert.util)

(def ^:dynamic *erlang-regex-flags*
  {:caseless java.util.regex.Pattern/CASE_INSENSITIVE
   :unicode java.util.regex.Pattern/UNICODE_CASE
   :dotall java.util.regex.Pattern/DOTALL
   :multiline java.util.regex.Pattern/MULTILINE})