(ns com.numergent.url-tools
  (:require [clojurewerkz.urly.core :as u]))


(defn host-string-of
  "Returns the host string for a url, including protocol and trailing slash.
  For now assumes that there's no need for a port."
  [url]
  (str (u/protocol-of url) "://" (u/host-of url) "/"))

(defn if-nil
  "Returns a if not nil, b if otherwise"
  [a b]
  (if (nil? a) b a))

(defn if-empty
  "Returns a if neither nil or empty, b if otherwise"
  [a b]
  (if (or (nil? a) (empty? a)) b a))

(defn in-seq? [s x]
  (some? (some #{x} s)))

