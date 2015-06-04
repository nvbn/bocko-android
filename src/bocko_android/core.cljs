(ns ^:figwheel-always bocko-android.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :refer [<! timeout]]
            [cognitect.transit :as t]
            [bocko.core :refer [set-create-canvas]]))

(defn indexed-zip
  [& colls]
  (->> colls
       (apply map vector)
       (map-indexed vector)))

(defn get-draw-commands
  [color-map pixel-width pixel-height old new]
  (flatten (for [[x [old-col new-col]] (indexed-zip old new)
                 :when (not= old-col new-col)]
             (for [[y [old-color new-color]] (indexed-zip old-col new-col)
                   :when (not= old-color new-color)
                   :let [left (* x pixel-width)
                         top (* y pixel-height)
                         right (+ pixel-width left)
                         bottom (+ top pixel-height)]]
               {"color" (map int (new-color color-map))
                "left" left
                "top" top
                "right" right
                "bottom" bottom}))))

(defn draw!
  [color-map pixel-width pixel-height old new android]
  (let [draw-commands (get-draw-commands color-map pixel-width
                                         pixel-height old new)
        writer (t/writer :json)
        serialised (t/write writer draw-commands)]
    (.flush android serialised)))

(defn init-canvas!
  [android]
  (let [last-rendered (atom nil)]
    (set-create-canvas
      (fn [color-map raster width height pixel-width pixel-height]
        (add-watch raster :monitor
          (fn [_ _ _ new]
            (go (let [old @last-rendered]
                  (<! (timeout 10))
                  (when-not (or (= old new) (not= new @raster))
                    (let [new-width (/ (.width android) width)]
                      (draw! color-map
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
