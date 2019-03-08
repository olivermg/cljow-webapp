(ns ow.webapp.async.router
  (:require [bidi.bidi :as b]
            [clojure.core.async :as a]
            [clojure.tools.logging :as log]
            [ow.app.lifecycle :as owl]))

(defn- handle [{:keys [routes response-channel resource-channel] :as this} {:keys [:http/request] :as msg}]
  (let [{:keys [handler]} (b/match-route routes (:uri request))]
    (if handler
      (a/put! resource-channel (assoc msg :http/resource handler))
      (a/put! response-channel (assoc msg :http/response {:status 404
                                                          :body "resource not found"})))))

(defrecord Router [request-channel response-channel resource-channel routes
                   in-pipe]

  owl/Lifecycle

  (start [this]
    (if-not in-pipe
      (do (log/info "Starting ow.webapp.async.router.Router")
          (let [in-pipe (a/pipe request-channel (a/chan))]
            (a/go-loop [msg (a/<! in-pipe)]
              (when-not (nil? msg)
                (future
                  (handle this msg))
                (recur (a/<! in-pipe))))
            (assoc this :in-pipe in-pipe)))
      this))

  (stop [this]
    (when in-pipe
      (log/info "Stopping ow.webapp.async.router.Router")
      (a/close! in-pipe))
    (assoc this :in-pipe nil)))

(defn router [request-channel response-channel resource-channel routes]
  (map->Router {:request-channel request-channel
                :response-channel response-channel
                :resource-channel resource-channel
                :routes routes}))

#_(do (require '[ow.webapp.async :as wa])
      (let [request-ch  (a/chan)
            response-ch (a/chan)
            resource-ch (a/chan)
            webapp      (-> (wa/webapp request-ch response-ch) owl/start)
            routes      ["/" [["foo" :foo]
                              ["bar" :bar]]]
            router      (-> (router request-ch response-ch resource-ch routes) owl/start)]
        (a/go-loop [{:keys [:http/resource] :as msg} (a/<! resource-ch)]
          (when-not (nil? msg)
            (println "got resource:" resource)
            (Thread/sleep 1000)
            (a/put! response-ch (assoc msg :http/response {:status 201 :body (str "yeah, " resource "!")}))
            (recur (a/<! resource-ch))))
        (Thread/sleep 15000)
        (owl/stop router)
        (owl/stop webapp)
        (a/close! resource-ch)
        (a/close! response-ch)
        (a/close! request-ch)))
