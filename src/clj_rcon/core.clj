(ns clj-rcon.core
  (:require [aleph.tcp :as tcp]
            [org.clojars.smee.binary.core :as b]
            [manifold.deferred :as d]
            [manifold.stream :as s]
            [clj-rcon.codecs :as codecs]))

(defn- encode->bytes [codec data]
  (let [bao (java.io.ByteArrayOutputStream.)]
    (b/encode codec bao data)
    (.toByteArray bao)))

(defn- bytes->decode [codec data]
  (b/decode codec (java.io.ByteArrayInputStream. data)))

(defn- encode-rcon [data]
  (encode->bytes codecs/packet-codec data))

(defn- decode-rcon [data]
  (bytes->decode codecs/packet-codec data))

(defn- split-frames
  "A transducer which accepts packets and concatenates/splits them
  into individual rcon messages."
  [rf]
  (let [reading-len (volatile! true)
        target-len (volatile! -1)
        a (java.util.ArrayList. 4096)]
    (fn
      ([] (rf))
      ([result] (rf result))
      ([result input]
       (.add a input)
       (cond
         (and @reading-len (= 4 (.size a)))
         (let [header (byte-array (.toArray a))
               frame-len (bytes->decode codecs/framing-codec header)]
           (vreset! reading-len false)
           (vreset! target-len frame-len)
           (.clear a)
           result)

         (and (not @reading-len) (= @target-len (.size a)))
         (let [frame (byte-array (.toArray a))]
           (vreset! reading-len true)
           (vreset! target-len -1)
           (.clear a)
           (rf result frame))

         :else result)))))

(defn- wrap-rcon [s]
  (let [wrapped (s/stream)]
    (s/connect (s/map encode-rcon wrapped) s)
    (s/splice wrapped (s/map decode-rcon (s/transform (comp cat split-frames) s)))))

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
              (if (nil? auth-response)
                (throw (Exception. "timeout"))
                (throw (Exception. "bad password"))))))]
    (d/chain
     (d/catch @(d/chain conn wrap-rcon send-auth!)
         d/error-deferred)
     recv-auth-response!)))

(defn exec [connection cmd]
  (let [req-id (rand-int 0x7fffffff)
        response (d/deferred)]
    (s/put! connection (codecs/exec req-id cmd))
    (d/timeout! response 2000 :timeout)
    (future []
            (d/success! response
                        (:body @(s/take! (s/filter #(= (:id %) req-id)
                                                   connection)))))
    response))
