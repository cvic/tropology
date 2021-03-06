(ns tropology.api
  (:require [tropology.db :as db]
            [com.numergent.url-tools :as ut]
            [taoensso.timbre.profiling :as prof]
            [tropology.base :as b]
            [clojure.string :as s]))


(defn node-size [rel-count]
  (cond
    (nil? rel-count) 0.5
    (< rel-count 10) 2
    (< rel-count 100) 4
    (< rel-count 500) 8
    (< rel-count 1000) 16
    :else 32
    ))

(defn transform-node
  "Transforms a node into the expected map values adds the coordinates"
  [node x y]
  (-> node
      (select-keys [:code :url :title])
      (assoc :id (:code node))                              ; We could just send a hash as the id, which would be more succinct, but this allows for quicker debugging.
      (assoc :x x :y y :size (node-size (:incoming node)) :label (:display node))))

(defn edge
  "Returns an edge map. Does not return an index, since those are disposable
  and can be assigned later."
  [from to color]
  {:source from
   :target to
   :color  color
   :type   "arrow"})                                        ; Could be line, curve, arrow or curvedArrow

(defn edge-collection
  [{code       :code
    links-from :links-from
    links-to   :links-to
    color-from :color-from
    color-to   :color-to}]
  (->>
    (let [edges-from (map #(edge code %1 (ut/if-nil color-from "#ff3300")) links-from)
          edges-to   (map #(edge %1 code (ut/if-nil color-to "#0066ff")) links-to)]
      (concat edges-from edges-to))
    (prof/p :edge-collection)))


(defn rand-range [n]
  (- (rand n) (/ n 2)))

(defn network-from-node
  [from-code]
  (->>
    (let [code       (s/lower-case from-code)
          node       (-> (db/query-by-code code) (transform-node 0 0))
          nodes-from (db/query-from code :LINKSTO)
          nodes-to   (db/query-to :LINKSTO code)
          node-set   (set (concat nodes-from nodes-to))
          related    (->> (db/query-common-nodes-from code)
                          (b/group-pairs)
                          (pmap #(hash-map :code (key %) :links-from (val %) :color-from "#00ffc7"))
                          )
          with-base  (conj related {:code code :links-from (pmap :code nodes-from) :links-to (pmap :code nodes-to)})
          ]
      {:nodes (conj
                (pmap #(transform-node %1 (rand-range 50) (rand-range 50)) node-set)
                node)
       :edges (->>
                (pmap edge-collection with-base)
                flatten
                (map-indexed #(assoc %2 :id %1)))}
      )
    (prof/profile :trace :API))
  )