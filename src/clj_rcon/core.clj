(ns clj-rcon.core
  (:require [aleph.tcp :as tcp]
            [org.clojars.smee.binary.core :as b]
            [manifold.deferred :as d]
            [manifold.stream :as s]
            [clj-rcon.codecs :as codecs])
  (:import [java.io ByteArrayInputStream ByteArrayOutputStream]))

(defn- encode->bytes [codec data]
  (let [bao (java.io.ByteArrayOutputStream.)]
    (b/encode codec bao data)
    (.toByteArray bao)))

(defn- bytes->decode [codec data]
  (b/decode codec (java.io.ByteArrayInputStream. data)))

(defn- encode-rcon [data]
  (encode->bytes codecs/rcon-packet-codec data))

(defn- decode-rcon [data]
  (bytes->decode codecs/rcon-packet-codec data))

(defn- wrap-rcon [s]
  (let [wrapped (s/stream)]
    (s/connect (s/map encode-rcon wrapped) s)
    (s/splice wrapped (s/transform cat (s/map decode-rcon s)))))

(defn connect
  "Connects to a Source RCON server at host on port, identified by
  password. May throw an IllegalStateException or ConnectException if
  unable to connect (depending on the cause).

  Returns a manifold stream."
  [host port password]
  (let [conn (tcp/client {:host host :port port})
        auth-req-id 0
        auth (codecs/auth auth-req-id password)
        send-auth! #(do (s/put! % auth) %)
        recv-auth-response!
        (fn [rcon]
          (let [recv! #(deref (s/try-take! rcon 3000))
                _ (recv!)
                auth-response (recv!)]
            (if (and (= (:type auth-response) codecs/serverdata-auth-response)
                     (= (:id auth-response) auth-req-id))
              rcon
              (throw (Exception. "bad password")))))]
    (d/chain
     (d/catch @(d/chain conn wrap-rcon send-auth!)
         d/error-deferred)
     recv-auth-response!)))

