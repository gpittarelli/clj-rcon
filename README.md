# clj-rcon

[![Clojars Project](http://clojars.org/clj-rcon/latest-version.svg)](http://clojars.org/clj-rcon)

A Clojure library implementing the Source RCON protocol for
administering Source engine powered game servers (TF2, L4D, etc.).

The protocol: https://developer.valvesoftware.com/wiki/Source_RCON_Protocol

Warning: almost no optimization effort has been put into this library.

## Usage

This library provides two functions: `connect` and `exec`.

`(connect host port password)`. `host` can be a string or
`InetAddress`. `port` is a number. `password` is a string. Read `(doc
clj-rcon/connect)` for more.

`(exec stream command)`. `connection` is a manifold stream;
specifically: one setup by a successful `connect` (a deref of the
return value of an individual `connect` call). Command is a rcon
command string to send to the server. A manifold deferred is
returned. It may resolve to a `:timeout` keyword, else the response of
the command, as a string.

## Example

```clojure
user> (def c (clj-rcon.core/connect "example.com" 27015 "rc0npassw0rd"))
#'user/c
user> (def q (clj-rcon.core/exec @c "status"))
#'user/q
user> @q
"hostname: ...\nrest of the status message"
```

## License

Copyright © 2016 George Pittarelli

Distributed under the MIT License.
