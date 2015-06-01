(ns ^:figwheel-always bocko-android.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :refer [<! timeout]]
            [bocko.core :refer [set-create-canvas]]))

(defn draw!
  [width height color-map pixel-width pixel-height old new android]
  (doseq [x (range width)]
    (let [old-col (nth old x)
          new-col (nth new x)]
      (when-not (= old-col new-col)
        (doseq [y (range height)]
          (let [old-color (nth old-col y)
                new-color (nth new-col y)]
            (when-not (= old-color new-color)
              (let [[r g b] (new-color color-map)
                    left (* x pixel-width)
                    top (* y pixel-height)
                    right (+ pixel-width left)
                    bottom (+ top pixel-height)]
                (.setRGB android r g b)
                (.drawRect android left top right bottom))))))))
  (.flush android))

(defn init-canvas!
  [android]
  (let [last-rendered (atom nil)]
    (set-create-canvas
      (fn [color-map raster width height pixel-width pixel-height]
        (add-watch raster :monitor
          (fn [_ _ _ new]
            (go (let [old @last-rendered]
                  (<! (timeout 5))
                  (when-not (or (= old new) (not= new @raster))
                    (let [new-width (/ (.width android) width)]
                      (draw! width height
                             color-map
                             new-width
                             (* (/ new-width pixel-width) pixel-height)
                             old new
                             android))
                    (reset! last-rendered new))))))))))

(defn wait-android
  []
  (go-loop []
    (when-not (aget js/window "android")
      (<! (timeout 100))
      (recur))))

(defn init
  "Initializes Bocko to render onto the supplied canvas element."
  []
  (go (<! (wait-android))
      (init-canvas! js/android)))
