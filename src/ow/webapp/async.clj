(ns ow.webapp.async
  (:require [clojure.core.async :as a]
            [clojure.tools.logging :as log]
            [org.httpkit.server :as hk]
            [ow.app.lifecycle :as owl]
            [ow.app.messaging :as owm]
            [ow.app.messaging.component-async :as owc]))

(defn- request-handler [middleware-instance
                        {:keys [messaging-component pending-channels] :as this}
                        req]
  (hk/with-channel req ch
    (hk/on-close ch (fn [status]
                      (log/debug "channel closed with status" status)))
    (let [req-after-middlewares (atom req)
          _ (middleware-instance (assoc req ::captured-request req-after-middlewares))
          msg (owm/message :http/request @req-after-middlewares)]
      (swap! pending-channels #(assoc % (owm/get-flow-id msg) [ch req]))   ;; TODO: periodically remove stale entries
      (owm/put! messaging-component msg)
      nil)))

(defn- response-handler [middleware-instance
                         {:keys [messaging-component pending-channels] :as this}
                         msg]
  (let [flow-id (owm/get-flow-id msg)
        response (owm/get-data msg)]
    (when-let [[ch orig-req] (get @pending-channels flow-id)]
      (when (hk/open? ch)
        (hk/send! ch (middleware-instance (assoc orig-req ::captured-response response))))
      (swap! pending-channels #(dissoc % flow-id)))))

(defn- storing-request-handler [{:keys [::captured-request] :as req}]
  (if captured-request
    (reset! captured-request (dissoc req ::captured-request))
    (throw (ex-info "cannot store captured request" {})))
  {:status 500
   :body "This internal server error should never happen"})

(defn- retrieving-request-handler [{:keys [::captured-response] :as req}]
  captured-response)

(defrecord Webapp [messaging-component middleware httpkit-options pending-channels
                   server]

  owl/Lifecycle

  (start [this]
    (if-not server
      (do (log/info "Starting ow.webapp.async.Webapp")
          (let [server (hk/run-server (partial request-handler (middleware storing-request-handler) this)
                                      httpkit-options)]
            (assoc this
                   :server server
                   :messaging-component (owl/start messaging-component))))))

  (stop [this]
    (when server
      (log/info "Stopping ow.webapp.async.Webapp")
      (server))
    (assoc this
           :server nil
           :messaging-component (owl/stop messaging-component))))

(defn webapp [in-ch out-ch & {:keys [middleware httpkit-options]}]
  (let [mc (owc/component "webapp-async" in-ch out-ch :http/response (partial response-handler (middleware retrieving-request-handler)))]
    (map->Webapp {:messaging-component mc
                  :middleware (or middleware identity)
                  :httpkit-options (merge {:port 8080
                                           :worker-name-prefix "async-webapp-worker-"}
                                          httpkit-options)
                  :pending-channels (atom {})})))



#_(let [reqch (a/chan)
        resch (a/chan)
        srv    (-> (webapp reqch resch) owl/start)]
    (a/go-loop [{:keys [:http/request] :as msg} (a/<! reqch)]
      (when-not (nil? msg)
        (println "got msg:" msg)
        (Thread/sleep 1000)
        (a/put! resch (assoc msg :http/response {:status 201 :body "yeah!"}))
        (recur (a/<! reqch))))
    (Thread/sleep 15000)
    (owl/stop srv)
    (a/close! resch)
    (a/close! reqch))
