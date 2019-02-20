(ns ow.webapp
  (:require [bidi.bidi :as b]
            [clojure.tools.logging :as log]
            [org.httpkit.server :as hk]
            [ow.app.lifecycle :as owl]))

(defn- app-handler [{:keys [routes resources] :as this} req]
  (let [{:keys [handler]} (some-> (b/match-route routes (:uri req))
                                  (update :handler #(get resources %)))]
    (if handler
      (handler (assoc req ::this this))
      {:status 404
       :body "resource not found"})))

(defrecord Webapp [routes resources middleware httpkit-options
                   server]

  owl/Lifecycle

  (start [this]
    (if-not server
      (do (log/info "Starting ow.webapp.Webapp")
          (let [server (hk/run-server
                        ((or middleware identity) (partial app-handler this))
                        (merge {:port 8080
                                :worker-name-prefix "httpkit-worker-"}
                               httpkit-options))]
            (assoc this :server server)))
      this))

  (stop [this]
    (when server
      (log/info "Stopping ow.webapp.Webapp")
      (server))
    (assoc this :server nil)))

(defn webapp [routes resources & {:keys [middleware httpkit-options]}]
  (map->Webapp {:routes routes
                :resources resources
                :middleware middleware
                :httpkit-options httpkit-options}))
