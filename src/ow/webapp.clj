(ns ow.webapp
  (:require [org.httpkit.server :as hk]
            [ring.middleware.defaults :as rmd]
            [bidi.bidi :as b]
            [liberator.core :as lc]))

(defrecord Webapp [routes resource-creators middleware-wrapper httpkit-options
                   resources server])

(defn- app-handler [{:keys [routes resources] :as this} req]
  (let [{:keys [handler]} (some-> (b/match-route routes (:uri req))
                                  (update :handler #(get resources %)))]
    (if handler
      (handler req)
      {:status 404
       :body "resource not found"})))

(defn webapp [routes resource-creators & {:keys [middleware-wrapper httpkit-options]}]
  (map->Webapp {:routes routes
                :resource-creators resource-creators
                :middleware-wrapper middleware-wrapper
                :httpkit-options httpkit-options}))

(defn start [{:keys [server resource-creators middleware-wrapper httpkit-options] :as this}]
  (if-not server
    (let [resources (into {} (map (fn [[id rcreator]]
                                    [id (rcreator this)]))
                          resource-creators)
          this (assoc this :resources resources)
          server (hk/run-server
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
         :server nil
         :resources nil))
