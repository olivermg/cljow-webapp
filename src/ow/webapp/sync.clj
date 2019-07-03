(ns ow.webapp.sync
  (:require [bidi.bidi :as b]
            #_[clojure.tools.logging :as log]
            [ow.logging.api.alpha :as log]
            [org.httpkit.server :as hk]
            #_[ow.app.lifecycle :as owl]))

(defn- app-handler [{:keys [routes resources] :as this} req]
  (let [{:keys [handler]} (some-> (b/match-route routes (:uri req))
                                  (update :handler #(get resources %)))]
    (if handler
      (handler (assoc req ::this this))
      {:status 404
       :body "resource not found"})))

#_(defrecord Webapp [routes resources middleware httpkit-options
                   server]

  owl/Lifecycle

  (start [this]
    (if-not server
      (do (log/info "Starting ow.webapp.sync.Webapp")
          (let [server (hk/run-server
                        ((or middleware identity) (partial app-handler this))
                        httpkit-options)]
            (assoc this :server server)))
      this))

  (stop [this]
    (when server
      (log/info "Stopping ow.webapp.sync.Webapp")
      (server))
    (assoc this :server nil)))

#_(defn webapp [routes resources & {:keys [middleware httpkit-options]}]
  (map->Webapp {:routes routes
                :resources resources
                :middleware (or middleware identity)
                :httpkit-options (merge {:port 8080
                                         :worker-name-prefix "sync-webapp-worker-"}
                                        httpkit-options)}))
