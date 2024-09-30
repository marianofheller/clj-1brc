(ns clj-1brc.core
  (:require [clojure.java.io :as io]
            [clojure.core.async :as a :refer [<! >! >!! put!]]
            [clojure.string :as s]))


;; When is channel closed
;; When is parked
;; How to consume all and return only then

(do

  (def acc-atom (atom {}))

  (def nthreads 32)
  (def re-separator #";")
  (defn read-num [s]
    (let [format (java.text.NumberFormat/getNumberInstance java.util.Locale/GERMANY)]
      (.parse format s)))

  (defn proc-line [_n]
    (let [c (a/chan)]
      (a/go-loop []
        (let [line (<! c)
              [city, value-str] (s/split line re-separator)
              value (read-num value-str)
              update-city (fn [old]
                            (if (nil? old)
                              {:max value :min value :sum value :count 1}
                              {:max (max value (:max old))
                               :min (min value (:min old))
                               :sum (+ value (:sum old))
                               :count (inc (:count old))}))]
          (swap! acc-atom update city update-city)
          (recur)))
      c))


  (defn gogogo []
    (let [channels (map proc-line  (range nthreads))]
      (with-open [rdr (io/reader "resources/weather_stations.csv")]
        (doall (map-indexed
                (fn [idx, line] (>!! (nth channels (mod idx nthreads)) line))
                (line-seq rdr)))
        @acc-atom)))


  (time (let [] (gogogo) nil)))




;;   (defn -main []
;;     (let [acc {}
;;           process-line (fn [line]
;;                          (let [[city, value-str] (s/split line ";")
;;                                value (float value-str)
;;                                city-atom (get acc city (atom {}))]
;;                            (swap! city-atom)))]
;;       (with-open [rdr (io/reader "resources/weather_stations.csv")]
;;         (reduce conj [] (line-seq rdr)))))

;;   (def xf-line (comp
;;                 #(s/split % ";")
;;                 (fn [[city, value-str]] [city (float value-str)])))

;;   (defn gogogo []
;;     (let [channels (map proc  (range nthreads))]
;;       (with-open [rdr (io/reader "resources/weather_stations.csv")]
;;         (transduce xf-line println (line-seq rdr)))))