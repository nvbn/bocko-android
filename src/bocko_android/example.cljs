(ns ^:figwheel-always bocko-android.example
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :refer [<! timeout]]
            [bocko.core :as b]
            [bocko-android.core :as a]))

(enable-console-print!)

; Game of life:

(defn neighbours
  [[x y]]
  (for [dx [-1 0 1] dy (if (zero? dx) [-1 1] [-1 0 1])]
    [(+ dx x) (+ dy y)]))

(defn step
  [cells]
  (set (for [[loc n] (frequencies (mapcat neighbours cells))
             :when (or (= n 3) (and (= n 2) (cells loc)))]
         loc)))

(defn game-of-life
  []
  (go-loop [board #{[0 2] [1 0] [1 2] [2 1] [2 2]}]
    (b/clear)
    (run! (partial apply b/plot) board)
    (<! (timeout 100))
    (recur (step board))))

; Flag:

(defn flag
  []
  (doseq [[n c] (take 13
                      (map vector (range) (cycle [:red :white])))]
    (b/color c)
    (let [x1 10
          x2 25
          y (+ 10 n)]
      (b/hlin x1 x2 y)))

  ;; Fill in a dark blue field in the corner

  (b/color :dark-blue)
  (doseq [x (range 10 19)
          y (range 10 17)]
    (b/plot x y))

  ;; Add some stars to the field by skipping by 2

  (b/color :white)
  (doseq [x (range 11 19 2)
          y (range 11 17 2)]
    (b/plot x y)))

; Bouncing ball

(defn ball
  []
  (go-loop [x 5 y 23 vx 1 vy 1]
    ; First determine new location and velocity,
    ; reversing direction if bouncing off edge.
    (let [x' (+ x vx)
          y' (+ y vy)
          vx' (if (< 0 x' 39) vx (- vx))
          vy' (if (< 0 y' 39) vy (- vy))]
      ; Erase drawing at previous location
      (b/color :black)
      (b/plot x y)
      ; Draw ball in new location
      (b/color :dark-blue)
      (b/plot x' y')
      ; Sleep a little and then loop around again
      (<! (timeout 50))
      (recur x' y' vx' vy'))))

; All colors

(defn colors
  []
  (doseq [[c n] (map vector
                     [:black :red :dark-blue :purple
                      :dark-green :dark-gray :medium-blue :light-blue
                      :brown :orange :light-gray :pink
                      :light-green :yellow :aqua :white]
                     (range))]
    (b/color c)
    (let [x' (* 10 (rem n 4))
          y' (* 10 (quot n 4))]
      (doseq [x (range x' (+ 10 x'))
              y (range y' (+ 10 y'))]
        (b/plot x y)))))

; Random

(defn random
  []
  (go-loop []
    (let [c (rand-nth [:black :red :dark-blue :purple
                       :dark-green :dark-gray :medium-blue :light-blue
                       :brown :orange :light-gray :pink
                       :light-green :yellow :aqua :white])
          x (rand-int 40)
          y (rand-int 40)]
      (b/color c)
      (b/plot x y)
      (<! (timeout 1))
      (recur))))

; Defaults
;
;(go (<! (a/init))
;    (flag))
