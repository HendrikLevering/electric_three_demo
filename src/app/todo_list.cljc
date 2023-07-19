(ns app.todo-list
  (:require contrib.str
            #?(:clj [datascript.core :as d]) ; database on server
            [hyperfiddle.electric :as e]
            [hyperfiddle.electric-dom2 :as dom]
            [app.orbit-controls :as ctrl]
            #?(:cljs ["three" :as three])
            #?(:cljs ["three/examples/jsm/controls/OrbitControls" :as  orbitcontrols])

            [app.three :as th]
            [hyperfiddle.electric-ui4 :as ui])
  )


(e/defn Some []
  (e/client
   (dom/div
    (let [!state (atom 0)
          state (e/watch !state)
          !pos (atom {:x 0 :y 0 :z 5})
          pos (e/watch !pos)
          look_at {:x 0 :y 0 :z 0}  ]
      (ui/button (e/fn [] (swap! !state  inc)) (dom/text "inc"))
      (ui/button (e/fn [] (swap! !state dec)) (dom/text "dec"))
      (dom/div
       (dom/style {:background-color "blue"})
       (dom/props {:class ["full-height"]})
       (th/canvas
        (th/WebGLRenderer [])
        (let [camera (th/PerspectiveCamera [75 th/view-port-ratio 0.1 1000]
                                            (th/setter th/reset_camera th/set_camera pos look_at))
              new-pos (ctrl/orbit-controls. camera {:rotateSpeed 1})]
          (reset! !pos new-pos)
          camera)
        (th/Scene []
                   (th/AmbientLight [0xFFFFFF 0.2])
                   (th/DirectionalLight [0xFFFFFF 0.3]
                                         (th/props {:position {:x 1 :y 1}}))
                   (when (> state 2)
                     (th/Mesh [(th/BoxGeometry [2 2 2])
                                (th/MeshLambertMaterial []
                                                         (th/props {:color  {:r 0.0 :g 0.9}}))]
                               (when (< state 4)
                                 (th/on! "click" #(println %)))
                               (th/props {:position {:z -2}})))
                   (th/Group []
                              (th/Mesh
                               [(th/DodecahedronGeometry [1 0])
                                (let [h (th/Hovered?.)]
                                  (th/MeshLambertMaterial []
                                                           (th/props {:color  {:r 0.1 :g (if h 0 1)}})))])))))))))
