(ns clj-rcon.core
  (:require [aleph.tcp :as tcp]
            [org.clojars.smee.binary.core :as b]
            [manifold.deferred :as d]
            [manifold.stream :as s]
            [clj-rcon.codecs :as codecs])
  (:import [java.io ByteArrayInputStream ByteArrayOutputStream]))

(defn encode->bytes [codec data]
  (let [bao (java.io.ByteArrayOutputStream.)]
    (b/encode codec bao data)
    (seq (.toByteArray bao))))

(defn bytes->decode [codec data]
  (let [bao (java.io.ByteArrayOutputStream.)]
    (b/encode codec bao data)
    (seq (.toByteArray bao))))

(defn encode-rcon [data]
  (encode->bytes codecs/rcon-packet-codec data))

(defn decode-rcon [data]
  (encode->bytes codecs/rcon-packet-codec data))

(defn wrap-rcon [s]
  (let [out (s/stream)]
    (s/connect (s/map encode-rcon out) s)
    (s/splice out s)))

(defn connect
  "Connects to a Source RCON server at host on port, identified by
  password. May throw an IllegalStateException or ConnectException if
  unable to connect (depending on the cause).

  Returns a manifold stream."
  [host port password]
  (let [send! (fn [data] #(do (s/put! % (encode-rcon data)) %))
        recv! (fn [] #(do (println (s/take! %)) %))]
    (d/chain
     (tcp/client {:host host :port port})
     wrap-rcon
     #(do (s/put! % (codecs/auth 0 password)) %)
     )))
