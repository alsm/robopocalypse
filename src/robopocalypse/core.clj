(ns robopocalypse.core
  (:require [clojure.math.numeric-tower :as math])
  (:require [clojure.algo.generic.math-functions :as mathf])
  (:require [clojurewerkz.machine-head.client :as mh])
  (:use clojure.data.priority-map))

(defn deg-to-rad [deg]
  (* deg (/ Math/PI 180)))

(defn rad-to-deg [rad]
  (* rad (/ 180 Math/PI)))

(def me (atom [-0.20101 51.49491]))

(def robots (atom (priority-map)))

(def disabled (atom (set #{})))

(def ignore (set #{"R2D2" "C3PO"}))

(def quotes ["Damn toasters!" "Frak this!" "You shall not pass!" "Make my day!" "Yippee-ki-yay, Mr. Falcon!"])

(def colors ["aqua" "black" "blue" "brown" "cyan" "gold" "green" "gray" "magenta" "orange" "purple" "red" "white" "yellow"])

(def color (rand-nth colors))

(defn calculate-distance-bearing [lat1 lon1 lat2 lon2]
  (let [radius 6371000
        delta-lat (deg-to-rad (- lat2 lat1))
        delta-lon (deg-to-rad (- lon2 lon1))
        lat1r (deg-to-rad lat1)
        lat2r (deg-to-rad lat2)
        a (+ (math/expt (mathf/sin(/ delta-lat 2)) 2)
             (* (* (math/expt (mathf/sin(/ delta-lon 2)) 2)
                   (mathf/cos lat1r))
                (mathf/cos lat2r)))]
    [(* (* (mathf/atan2 (math/sqrt a)
                        (math/sqrt (- 1 a))))
        radius)
     (mod (+ (rad-to-deg (mathf/atan2 (* (mathf/sin delta-lon)
                                         (mathf/cos lat2r))
                                      (- (* (mathf/cos lat1r) (mathf/sin lat2r))
                                         (* (* (mathf/sin lat1r) (mathf/cos lat2r))
                                            (mathf/cos delta-lon)))))
             360)
          360)]))

(defn next-location
  ([location-vec bearing distance]
   (next-location (second location-vec) (first location-vec) bearing distance))
  ([lat1 lon1 bearing distance]
   (let [radius 6371000
         lat1r (deg-to-rad lat1)
         lon1r (deg-to-rad lon1)
         bearingr (deg-to-rad bearing)
         lat2 (mathf/asin (+ (* (* (mathf/cos lat1r)
                                   (mathf/sin (/ distance radius)))
                                (mathf/cos bearingr))
                             (* (mathf/sin lat1r)
                                (mathf/cos (/ distance radius)))))
         lon2 (+ lon1r (mathf/atan2 (* (* (mathf/sin bearingr)
                                          (mathf/sin (/ distance radius)))
                                       (mathf/cos lat1r))
                                    (- (mathf/cos (/ distance radius))
                                       (* (mathf/sin lat1r)
                                          (mathf/sin lat2)))))]
     [(rad-to-deg lon2) (rad-to-deg lat2)])))

(defn transform [topic payload]
  (let [robot (nth (clojure.string/split topic #"/") 2)
        coor (into [] (map #(Float/parseFloat %) (clojure.string/split payload #",")))]
  [robot coor]))

(defn update-atom [topic location]
    (let [[robot [lon lat]] (transform topic location)]
      (if-not (contains? @disabled robot)
        (swap! robots assoc robot (calculate-distance-bearing (second @me) (first @me) lat lon)))))

(def conn (mh/connect "tcp://5.153.17.246:1883" "C3PO"))

(mh/subscribe conn ["hack2/things/+/location"] (fn [^String topic _ ^bytes payload]
                                                 (if-not (contains? ignore ((clojure.string/split topic #"/") 2))
                                                   (update-atom topic (String. payload "UTF-8")))))

(mh/publish conn "hack2/things/C3PO" (str "{\"location\":\"" (first @me) (second @me) "\",\"name\":\"C3PO\",\"description\":\"Beep Boop\",\"type\":\"diamond\",\"state\":1,\"color\":\"red\"}"))

(println "Disabled robots will be turned" color)
(while (not (empty? @robots))
  (do (let [[robo-id robo-info] (first @robots)]
        (if (and (not (nil? robo-id)) (< (first robo-info) 30))
          (do (println "Robot" robo-id "within range, firing lasers, pew pew!")
            (mh/publish conn "hack2/things/C3PO/addOverlay" (str (rand-nth quotes) "|1000|red|white"))
            (swap! disabled conj robo-id)
            (swap! robots dissoc robo-id)
            (mh/publish conn (str "hack2/things/" robo-id "/color") color 1 true)
            (println "Disabled robot" robo-id))
          (do (swap! me next-location (second robo-info) 15.0)
            (mh/publish conn "hack2/things/C3PO/location" (clojure.string/join "," [(first @me) (second @me)]) 1 true)))
        (Thread/sleep 150))))

(println "All robots disabled")
(System/exit 0)
