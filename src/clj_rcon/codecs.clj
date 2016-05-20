(ns clj-rcon.codecs
  (:require [org.clojars.smee.binary.core :as b]
            [clojure.string :as str]
            [clojure.set :refer [map-invert]]))

(defn- str-length [as-codec & {:keys [offset] :or {:offset 0}}]
  (reify b/BinaryIO
    (read-data  [_ big-in little-in]
      (b/read-data as-codec big-in little-in))
    (write-data [_ big-out little-out value]
      (b/write-data as-codec big-out little-out (+ offset (count value))))))

(def rcon-packet-codec
  (b/ordered-map
   :body (str-length :int-le :offset 10)
   :id :int-le
   :type :int-le
   :body (b/c-string "ASCII")
   :null (b/constant :byte 0)))

(def serverdata-auth 3)
(def serverdata-auth-response 2)
(def serverdata-exec 2)
(def serverdata-response-value 0)

(defn auth [id password] {:type serverdata-auth :id id :body password})
(defn exec [id command] {:type serverdata-exec :id id :body command})
