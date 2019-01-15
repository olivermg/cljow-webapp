(ns ow.webapp
  (:require [org.httpkit.server :as hk]
            [ring.middleware.defaults :as rmd]
            [bidi.bidi :as b]))

(defrecord Webapp [routes resources middleware-wrapper httpkit-options
                   server])

(defn- app-handler [{:keys [routes resources] :as this} req]
  (let [{:keys [handler]} (some-> (b/match-route routes (:uri req))
                                  (update :handler #(get resources %)))]
    (if handler
      (handler req)
      {:status 404
       :body "resource not found"})))

(defn webapp [routes resources & {:keys [middleware-wrapper httpkit-options]}]
  (map->Webapp {:routes routes
                :resources resources
                :middleware-wrapper middleware-wrapper
                :httpkit-options httpkit-options}))

(defn start [{:keys [server resources middleware-wrapper httpkit-options] :as this}]
  (if-not server
    (let [server (hk/run-server
                  ((or middleware-wrapper identity) (partial app-handler this))
                  (merge {:port 8080
                          :worker-name-prefix "httpkit-worker-"}
                         httpkit-options))]
      (assoc this :server server))
    this))

(defn stop [{:keys [server] :as this}]
  (when server
    (server))
  (assoc this
         :server nil))
