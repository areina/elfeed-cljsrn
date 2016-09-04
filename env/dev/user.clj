(ns user
  (:use [figwheel-sidecar.repl-api :as ra]))

(defn start-figwheel
  "Start figwheel for one or more builds"
  [& build-ids]
  (apply ra/start-figwheel! build-ids)
  (ra/cljs-repl))

(defn stop-figwheel
  "Stops figwheel"
  []
  (ra/stop-figwheel!))
