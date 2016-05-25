(ns clj-rcon.codecs
  (:require [org.clojars.smee.binary.core :as b]
            [clojure.string :as str]
            [clojure.set :refer [map-invert]]))

(defn- write-only-str-length [as-codec & {:keys [offset] :or {:offset 0}}]
  (reify b/BinaryIO
    ;; For our specific use case: only ever write the header when
    ;; encoding; for reading/decoding, we'll handle the framing at
    ;; a higher level
    (read-data  [_ big-in little-in])
    (write-data [_ big-out little-out value]
      (b/write-data as-codec big-out little-out (+ offset (count value))))))

(def framing-codec :int-le)

(def packet-codec
  (b/compile-codec
   (b/ordered-map
    :body (write-only-str-length framing-codec :offset 10)
    :id :int-le
    :type :int-le
    :body (b/c-string "ASCII")
    :null (b/constant :byte 0))
   identity
   #(dissoc % :null)))

(def serverdata-auth 3)
(def serverdata-auth-response 2)
(def serverdata-exec 2)
(def serverdata-response-value 0)

(defn auth [id password] {:type serverdata-auth :id id :body password})
(defn exec [id command] {:type serverdata-exec :id id :body command})
