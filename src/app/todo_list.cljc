(ns app.todo-list
  (:require contrib.str
            #?(:clj [datascript.core :as d]) ; database on server
            [hyperfiddle.electric :as e]
            [hyperfiddle.electric-dom2 :as dom]
            [missionary.core :as m]
            #?(:cljs ["three" :as three])
            #?(:cljs ["three/examples/jsm/controls/OrbitControls" :as  orbitcontrols])

            [app.three2 :as th2]
            [hyperfiddle.electric-ui4 :as ui])
  )

(defn foo[]
  (let [f three/BoxGeometry]
    (println "bar")
    (new f 1 1 1)))

(e/defn Some []
  (e/client
   (dom/div
    (dom/style {:background-color "red"})
    (dom/h1 (dom/text "minimal todo list"))
    (let [!state (atom 0)
          state (e/watch !state)]
      (ui/button (e/fn [] (swap! !state  inc)) (dom/text "inc"))
      (ui/button (e/fn [] (swap! !state dec)) (dom/text "dec"))
      (dom/div
       (dom/style {:background-color "blue"})
       (dom/props {:class ["full-height"]})
       (th2/canvas
        (th2/WebGLRenderer [])
        (let [camera (th2/PerspectiveCamera [75 th2/view-port-ratio 0.1 1000]
                                            (th2/props {:position {:z 5}}))]
          (th2/OrbitControls [camera dom/node]
                             (th2/control))
          camera)

        (th2/Scene []
                   (th2/AmbientLight [ 0xFFFFFF 0.2])
                   (th2/DirectionalLight [ 0xFFFFFF 0.2])
                   (th2/DirectionalLight [ 0xFFFFFF 0.3]
                                         (th2/props {:position {:x 1 :y 1}}))
                   (th2/DirectionalLight [ 0xFFFFFF 0.4]
                                         (th2/props {:position {:x -1 :y -1 :z -1}}))
                   (th2/DirectionalLight [ 0xFFFFFF 0.3]
                                         (th2/props {:position {:x -1 :y 0 :z -1}}))
                   (when (> state -1)
                     (th2/Mesh [ (th2/BoxGeometry [2 2 2])
                                (th2/MeshLambertMaterial []
                                                         (th2/props {:color  {:r 0.0 :g 0.9}}))]
                               (th2/on! "click" #(println %))
                               (th2/props {:position {:z -2}})))
                   (th2/Group []
                              (th2/Mesh
                               [(th2/DodecahedronGeometry [1 0])
                                (let [h (th2/Hovered?.)]
                                  (th2/MeshLambertMaterial []
                                                           (th2/props {:color  {:r 0.5 :g (if h 0 1)}})))]
                               )))))))))
