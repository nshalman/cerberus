(ns jingles.server
  (:require [clojure.java.io :as io]
            [jingles.dev :refer [is-dev? inject-devmode-html browser-repl start-figwheel]]
            [compojure.core :refer [GET defroutes]]
            [compojure.route :refer [resources]]
            [net.cgrand.enlive-html :refer [deftemplate]]
            [net.cgrand.reload :refer [auto-reload]]
            [ring.middleware.reload :as reload]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [environ.core :refer [env]]
            [ring.adapter.jetty :refer [run-jetty]]
            [tailrecursion.ring-proxy :refer [wrap-proxy]]
            [cconf.core :as cconf]))


(def settings (-> (cconf/argv)                  ;; Load command-line arguments    (highest priority)
                  (cconf/env)                   ;; Load environment variables
                  (cconf/file "config.json")    ;; Load options from config.json
                  (cconf/defaults {:port 8888
                                   :wiggle "http://127.0.0.1:8080/api"
                                   :howl   "http://127.0.0.1:8081/howl"})))

(deftemplate page (io/resource "index.html") []
  [:body] (if is-dev? inject-devmode-html identity))

(defroutes routes
  (resources "/")
  (resources "/react" {:root "react"})
  (GET "/*" req (page)))

(def http-handler
  ( ->
    (if is-dev?
      (reload/wrap-reload (wrap-defaults #'routes api-defaults))
      (wrap-defaults routes api-defaults))
    (wrap-proxy "/api" (settings :wiggle))
    (wrap-proxy "/howl" (settings :howl))))

(defn run-web-server []
  (let [port (settings :port)]
    (print "Starting web server on port" (str port ".\n"))
    (run-jetty http-handler {:port port :join? false})))

(defn run-auto-reload []
  (auto-reload *ns*)
  (start-figwheel))

(defn run []
  (when is-dev?
    (run-auto-reload))
  (run-web-server))

(defn -main []
  (run))
