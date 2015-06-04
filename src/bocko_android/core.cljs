(ns ^:figwheel-always bocko-android.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :refer [<! timeout]]
            [cognitect.transit :as t]
            [bocko.core :refer [set-create-canvas clear-screen]]))

(defonce stable-timeout 5)
(defonce max-timeout 20)
(defonce last-drawed (atom clear-screen))
(defonce last-attempt (atom []))
(defonce timeouted (atom 0))

(defn indexed-zip
  [& colls]
  (->> colls
       (apply map vector)
       (map-indexed vector)))

(defn get-draw-commands
  [color-map pixel-width pixel-height new]
  (flatten (for [[x [old-col new-col]] (indexed-zip @last-drawed new)
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
  [color-map pixel-width pixel-height new android]
  (let [draw-commands (get-draw-commands color-map pixel-width
                                         pixel-height new)
        writer (t/writer :json)
        serialised (t/write writer draw-commands)]
    (.flush android serialised)
    (reset! last-drawed new)))

(defn is-stable?
  [new]
  (reset! last-attempt new)
  (go (<! (timeout stable-timeout))
      (reset! timeouted + stable-timeout)
      (when-let [stable (or (= @last-attempt new)
                            (>= @timeouted max-timeout))]
        (reset! timeouted 0)
        stable)))

(defn init-canvas!
  [android]
  (set-create-canvas
    (fn [color-map raster width _ pixel-width pixel-height]
      (add-watch raster :monitor
        (fn [_ _ _ new]
          (go (when (<! (is-stable? new))
                (let [new-width (/ (.width android) width)]
                  (draw! color-map
                         new-width
                         (* (/ new-width pixel-width) pixel-height)
                         new android)))))))))

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
