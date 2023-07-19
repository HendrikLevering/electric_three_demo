(ns app.orbit-controls
  (:require [hyperfiddle.electric :as e]
            [missionary.core :as m]
            #?(:cljs ["three" :as three])
            [contrib.missionary-contrib :as mx]
            [hyperfiddle.electric-dom2 :as dom]))



(e/defn Down? "down flag with pointer capture on down event"
  []
  (->> (mx/mix
        (e/listen> dom/node "pointerdown" (fn [e]
                                            (.setPointerCapture dom/node (.-pointerId e))
                                            true))
        (e/listen> dom/node "pointercancel"  (constantly false))
        (e/listen> dom/node "pointerup" (constantly false))
        )
       (m/reductions {} false)
       (m/relieve {})
       new))


(e/defn RelMove  "delta movement since last move"
  []
  (->> (e/listen> dom/node "pointermove" (fn [e] e))
       (m/reductions ((fn []
                        (let [l (volatile! nil)]
                          (fn
                            ([] [0 0])
                            ([r e]
                             (if-let [[x0 y0] (deref l)]
                               (let [x1 (.-pageX e)
                                     y1 (.-pageY e)]
                                 (vreset! l [x1 y1])
                                 [(- x1 x0) (- y1 y0)])
                               (let [x (.-pageX e)
                                     y (.-pageY e)]
                                 (vreset! l [x y])
                                 [0 0]))))))))
       (m/relieve {})
       new))


#?(:cljs (defn -orbit-controls [camera dx dy opts]
            (let [{rotateSpeed :rotateSpeed,
                   :or {rotateSpeed 1}} opts
                  nx (* 2 Math/PI rotateSpeed dx  0.001)
                  ny (* 2 Math/PI rotateSpeed dy  0.001)
                  q (doto (three/Quaternion.)
                      (.setFromUnitVectors (.-up camera) (three/Vector3. 0 1 0)))
                  iq (doto (.clone q)
                       (.invert))
                  target (three/Vector3.)
                  v (doto (three/Vector3.)
                      (.copy (.-position camera))
                      (.sub target)
                      (.applyQuaternion q))
                  s (doto (three/Spherical.)
                      (.setFromVector3 v))
                  theta (.-theta s)
                  phi (.-phi s)]
              (set! (.-theta s) (- theta nx))
              (set! (.-phi s) (- phi ny))
              (.makeSafe s)

              (doto v
                (.setFromSpherical s)
                (.applyQuaternion iq)
                (.add target))
              {:x (.-x v) :y (.-y v)  :z (.-z v)})))


(e/defn orbit-controls [camera opts]
  (let [down? (Down?.)
        [dx dy] (if down? (RelMove.) [0 0])]
    (-orbit-controls camera dx dy opts)))