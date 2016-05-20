# clj-rcon

[![Clojars Project](http://clojars.org/clj-rcon/latest-version.svg)](http://clojars.org/clj-rcon)

A Clojure library implementing the Source RCON protocol for
administering Source engine powered game servers (TF2, L4D, etc.).

The protocol: https://developer.valvesoftware.com/wiki/Source_RCON_Protocol

## Usage

This library provides a single function: `(connect host port
password)`. `host` can be a string or `InetAddress`. `port` is a
number. `password` is a string.

Read `(doc clj-rcon/connect)` for more.

## Example

```clojure
user>  ;; TODO
```

## License

Copyright © 2016 George Pittarelli

Distributed under the MIT License.
