# electric-three-demo
[Three.js](https://threejs.org/) adapter for [Electric Clojure](https://github.com/hyperfiddle/electric)

https://github.com/dustingetz/hendrik-electric_three_demo/assets/124158/dc043b09-38f2-4cc7-a172-ef083d45a32d

Commentary:
* I think that Electric and three_js fit quite nicely. Three.js requires you to manually cleanup GPU resources. Electric’s RAII semantics are great to automatically cleanup resources.  And I used three.js raycaster to get mouse events on scene graph objects which you subscribe too similiar to dom/on and dom/on! And a rerender only happens if the scene, camera or dom-size changed. Electrics caching and minimal recompute does a great job.
* Some things which are a little bit hacky at the moment:
  * Orbit controls and camera. OrbitControls are a mutable JS blob which mutates the camera. This is not working completly declaritive, but I didn’t want to reimplement OrbitControls, which is 1000+ lines of mutable JS.
  * (Followup) Today I implemented my own version of OrbitControls, which was super easy with electric/missionary btw. I am exited about how simple the orbit control implementation with electric and [missionary](https://github.com/leonoel/missionary) is. Especially if you compare it against the original javascript implementation. And giving the controls some moment of inertia should be simple. Roughly: Generate a finite flow of declining values on mouse movement and make it debounced.
  * You have to require three.js in everyfile, where you use the wrapper. I think that this is solveable, but I surely reached my Macro Skills limit on this
  * Three.js interfaces are inconsistent. Often you simply can set properties. However, there are some classes that require constructor args and sometimes there are setter functions. Especially the constructor args can be a little bit annoying. First of all, you cannot refer to this (like dom/props can) and secondly you have to account for this in the wrapper. I created the convention, that the first argument is a list of constructor args.

# Setup

```
$ npm install
$ clj -A:dev -X user/main

Starting Electric compiler and server...
shadow-cljs - server version: 2.20.1 running at http://localhost:9630
shadow-cljs - nREPL server started on port 9001
[:app] Configuring build.
[:app] Compiling ...
[:app] Build completed. (224 files, 0 compiled, 0 warnings, 1.93s)

👉 App server available at http://0.0.0.0:8080
```

# Deployment

ClojureScript optimized build, Dockerfile, Uberjar, Github actions CD to fly.io

```
HYPERFIDDLE_ELECTRIC_APP_VERSION=`git describe --tags --long --always --dirty`
clojure -X:build uberjar :jar-name "app.jar" :version '"'$HYPERFIDDLE_ELECTRIC_APP_VERSION'"'
java -DHYPERFIDDLE_ELECTRIC_SERVER_VERSION=$HYPERFIDDLE_ELECTRIC_APP_VERSION -jar app.jar
```

```
docker build --progress=plain --build-arg VERSION="$HYPERFIDDLE_ELECTRIC_APP_VERSION" -t electric-starter-app .
docker run --rm -p 7070:8080 electric-starter-app
```

```
# flyctl launch ... ? create fly app, generate fly.toml, see dashboard
# https://fly.io/apps/electric-starter-app

NO_COLOR=1 flyctl deploy --build-arg VERSION="$HYPERFIDDLE_ELECTRIC_APP_VERSION"
# https://electric-starter-app.fly.dev/
```

- `NO_COLOR=1` disables docker-cli fancy shell GUI, so that we see the full log (not paginated) in case of exception
- `--build-only` tests the build on fly.io without deploying
