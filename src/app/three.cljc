(ns app.three
  (:require
   #?(:cljs ["three" :as three])
   #?(:cljs ["three/examples/jsm/controls/OrbitControls" :refer [OrbitControls]])
   #?(:cljs [goog.object :as gobj])
   [missionary.core :as m]
   [hyperfiddle.electric :as e]
   [hyperfiddle.electric-dom2 :as dom])
   (:import #?(:clj (clojure.lang IDeref))
           (hyperfiddle.electric Pending Failure FailureInfo)
           (missionary Cancelled)))


#?(:cljs def threejs three)


(comment
  (flatten-props {:a {:b {:c 1 :d 2} :e 3} :f 4})

  ( first '([[:a :b :c] 1] [[:a :b :b] 2] [[:a :e] 3] [[:f] 4]))
  )

(e/defn material [m]
  #?(:cljs
     (let [mat (three/MeshBasicMaterial. (clj->js m))]
       (println "mount mat")
       (e/on-unmount #(do
                        (println "unmount material ")
                        (.dispose mat)))
       mat)))

(e/defn meshlambertmaterial [m]
  #?(:cljs
     (let [mat (three/MeshLambertMaterial.  (clj->js m))]
       (e/on-unmount #(do
                        (.dispose mat)))
       mat)))

(e/defn box [[x y z]]
  #?(:cljs
     (let [geometry (three/BoxGeometry. x y z)]
       (println "mount box" x)
       (e/on-unmount #(do
                        (println "unmount box " x)
                        (.dispose geometry)))
       geometry)))



(defn -mesh [opts geometry material]
  #?(:cljs
     (let [{[x y z] :pos key :key } opts
           obj (three/Mesh. geometry material)]
       (gobj/set obj "opts" opts)
       (.translateX obj x)
       (.translateY obj y)
       (.translateZ obj z)
       obj)))

(comment
  ({} false true)
  ({} 1
      )
  )

(e/defn mesh [opts geometry material]
  #?(:cljs
     (let [obj (-mesh opts geometry material)]
       (println "mount mesh")
       (e/on-unmount #(do
                        (println "unmount mesh ")
                        (.removeFromParent obj)
                        (.dispose obj)))
       obj)))


(defn -size [rect] [(.-width rect) (.-height rect)])

(defn size> [node state]
  #?(:cljs (->> (m/observe (fn [!]
                             (! (-> node .getBoundingClientRect))
                             (let [resize-observer (js/ResizeObserver. (fn [[nd] _] (println "resize")(swap! state update :a inc)(! (-> nd .-target .getBoundingClientRect))))]
                               (.observe resize-observer node)
                               #(.disconnect resize-observer))))
                (m/relieve {}))))


(defn -camera [fov aspect near far [x y z]]
  #?(:cljs (let [cam (three/PerspectiveCamera. fov aspect near far)]
             (set! (.. cam -position -x) x)
             (set! (.. cam -position -y) y)
             (set! (.. cam -position -z) z)
             (.updateProjectionMatrix cam)
             cam)))


(e/defn camera [fov near far pos state]
  #?(:cljs
     (let [[width height] (-size (new (size>  dom/node state)))
           cam (-camera fov (/ width height) near far pos)]
       (e/on-unmount #(do
                        (.removeFromParent cam)
                        (.dispose cam)))
       cam)))

(defn -scene [children]
  #?(:cljs (let [s (three/Scene.)]
             (dorun (for [child children]
                      (.add s child)))
             s)))

(e/defn scene [opts children]
  #?(:cljs
     (let [s (-scene children)]
       (e/on-unmount #(do
                        (.removeFromParent s)
                        (.dispose s)))
       s)))

(defn create-render-fn []
  (let [v (volatile! [0 0])]
    (fn [renderer scene camera width height controls c]
      (let [[w h] @v]
        (when (or (not= width w) (not= height h))
          (.setSize renderer width height)
          (vreset! v [width height])))
      (.update controls)
      (.render renderer scene camera))))

(e/defn dom-mousemove
  "mousemove events"
  [node]
  (e/client (new (m/reductions {} nil (e/listen> node "pointermove" (fn [e][(.-pageX e) (.-pageY e)]))))))

(e/defn dom-click
  "mousemove events"
  [node]
  (e/client (new (m/reductions {} nil (e/listen> node "click" (fn [e] [(.-pageX e) (.-pageY e) (rand-int 1000000000)]))))))


(defn -intersected [x y scene camera]
  #?(:cljs
     (let [pointer (three/Vector2.)
           caster (three/Raycaster.)]
       (set! (.-x pointer) x)
       (set! (.-y pointer) y)
       (.setFromCamera caster pointer camera)
       (first (seq (.intersectObjects caster (.-children scene) false))))))


(defn intersected []
  (let [v (volatile! nil)]
    (fn [[x y] scene camera]
      #?(:cljs
         (let [intersection  (when-let [i (-intersected x y scene camera)]
                               (js->clj i))
               i-obj (get intersection "object")
               last-intersection @v
               l-obj (get last-intersection "object")]
           (do
             (vreset! v intersection)

             (if intersection
               (if (== i-obj l-obj)
                 {:on-move intersection}
                 (if last-intersection
                   {:on-move intersection :on-enter intersection :on-leave last-intersection}
                   {:on-move intersection :on-enter intersection}))
               (if last-intersection
                 {:on-leave last-intersection}
                 {}))))))))



(defn -trigger-event [k event]
  #?(:cljs
     (when-let [d (k event)]
       (when-let [fn (-> d (get "object") .-opts k)]
         (fn d)))))

(defn process-event [event]
  #?(:cljs
     (do
       (-trigger-event :on-leave event)
       (-trigger-event :on-enter event)
       (-trigger-event :on-move event))))

(defn -clicked [[x y ] _ scene camera]
  #?(:cljs
     (when-let [i (-intersected x y scene camera)]
       (-trigger-event :on-click {:on-click (js->clj i)}))))


(defn process-events [events]
  #?(:cljs
     (doall (map process-event events))))


(defn -pointer [rect [x y]]
  (let [[width height] (-size rect)
        dx (- (inc x) (.-left rect))
        dy (- (inc y) (.-top rect))
        px (dec (* 2 (/ dx width)))
        py (- (dec (* 2 (/ dy height))))]
    [px py]))

#?(:cljs (defn -create-renderer [node opts]
           (let [renderer (three/WebGLRenderer. (clj->js opts))]
             (.appendChild node
                           (.-domElement renderer))
             renderer)))


(e/defn camera-matrix [controls]
  (if (= "visible" e/dom-visibility-state)
    (new (m/sample #(do
                      (.update controls)
                      [(vec (.. controls -object -position))  (vec (.. controls -object -quaternion))]) e/<clock))
    (throw (Pending.)))) ; tab is hidden, no clock. (This guards NPEs in userland)

(e/defn canvas [state opts scene camera]
  #?(:cljs
     (let [s  e/system-time-ms
           render-opts (:renderer opts)
           renderer (-create-renderer dom/node render-opts)
           controls (OrbitControls. camera (.-domElement renderer))
           render-fn (create-render-fn)
           rect (new (size>  dom/node state))
           [width height] (-size rect)
           cam_mat  (camera-matrix. controls)
           #_#_moves (dom-mousemove. dom/node)
           #_#_[x y e] (dom-click. dom/node)

           #_#_hovered  ((intersected) (-pointer rect moves) scene camera)]
       (println width)
       ;const controls = new OrbitControls( camera, renderer.domElement );
       #_(-clicked (-pointer rect [x y]) e  scene camera)
       #_(when (seq hovered)
           (process-events [hovered]))
       (.setPixelRatio renderer window/devicePixelRatio)
       (render-fn renderer scene camera width height controls cam_mat)

       (e/on-unmount #(do
                        (.removeFromParent renderer)
                        (.dispose renderer))))))

(defmacro element
  [name])

(e/defn ambientlight [color intensity]
  #?(:cljs
     (let [light (new three/AmbientLight color intensity)]
       (e/on-unmount #(do
                        (.removeFromParent light)
                        (.dispose light)))
       light)))


(defn -directionalLight [color intensity pos]
  #?(:cljs (let [light (three/DirectionalLight. color intensity)
                 [x y z] pos]
             (.normalize (.set (.-position light) x y z))
             light)))


(e/defn directionalLight [color intensity pos]
  #?(:cljs
     (let [light (-directionalLight color intensity pos)]
       (e/on-unmount #(do
                        (.removeFromParent light)
                        (.dispose light)))
       light)))


(defn -pointLight [color intensity distance decay pos]
  #?(:cljs (let [light (three/PointLight. color intensity distance decay)
                 [x y z] pos]
             (.normalize (.set (.-position light) x y z))
             light)))


(e/defn pointLight [color intensity distance decay pos]
  #?(:cljs
     (let [light (-pointLight color intensity distance decay pos)]
       (e/on-unmount #(do
                        (.removeFromParent light)
                        (.dispose light)))
       light)))