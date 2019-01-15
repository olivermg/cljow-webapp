(ns ow.webapp
  (:require [org.httpkit.server :as hk]
            [bidi.bidi :as b]))

(defrecord Webapp [routes resources middleware httpkit-options
                   server])

(defn- app-handler [{:keys [routes resources] :as this} req]
  (let [{:keys [handler]} (some-> (b/match-route routes (:uri req))
                                  (update :handler #(get resources %)))]
    (if handler
      (handler req)
      {:status 404
       :body "resource not found"})))

(defn webapp [routes resources & {:keys [middleware httpkit-options]}]
  (map->Webapp {:routes routes
                :resources resources
                :middleware middleware
                :httpkit-options httpkit-options}))

(defn start [{:keys [server resources middleware httpkit-options] :as this}]
  (if-not server
    (let [server (hk/run-server
                  ((or middleware identity) (partial app-handler this))
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
