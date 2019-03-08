(ns ow.webapp.async
  (:require [clojure.core.async :as a]
            [clojure.tools.logging :as log]
            [org.httpkit.server :as hk]
            [ow.app.lifecycle :as owl]))

(defn- request-handler [{:keys [request-channel pending-channels] :as this} middleware-instance req]
  (hk/with-channel req ch
    (hk/on-close ch (fn [status]
                      (log/debug "channel closed with status" status)))
    (let [request-id (rand-int Integer/MAX_VALUE)
          req (assoc req ::request-id request-id)
          req-after-middlewares (atom req)]
      (middleware-instance (assoc req ::captured-request req-after-middlewares))
      (swap! pending-channels #(assoc % request-id [ch req]))   ;; TODO: remove stale channels periodically
      (a/put! request-channel @req-after-middlewares)
      nil)))

(defn- response-handler [{:keys [response-channel pending-channels] :as this} middleware-instance {:keys [::request-id] :as res}]
  (try
    (when-let [[ch req] (get @pending-channels request-id)]
      (when (hk/open? ch)
        (hk/send! ch (middleware-instance (assoc req ::captured-response res))))
      (swap! pending-channels #(dissoc % request-id)))
    (catch Exception e
      (log/warn "EXCEPTION while processing response" res e))
    (catch Error e
      (log/warn "ERROR while processing response" res e))))

(defn- storing-request-handler [{:keys [::captured-request] :as req}]
  (if captured-request
    (reset! captured-request (dissoc req ::captured-request))
    (throw (ex-info "cannot store captured request" {})))
  {:status 500
   :body "This internal server error should not happen"})

(defn- retrieving-request-handler [{:keys [::captured-response] :as req}]
  captured-response)

(defrecord Webapp [request-channel response-channel routes resources middleware httpkit-options pending-channels
                   server in-pipe]

  owl/Lifecycle

  (start [this]
    (if-not server
      (do (log/info "Starting ow.webapp.async.Webapp")
          (let [in-pipe (a/pipe response-channel (a/chan))
                server (hk/run-server (partial request-handler this (middleware storing-request-handler))
                                      httpkit-options)]
            (a/go-loop [response (a/<! in-pipe)]
              (when-not (nil? response)
                (response-handler this (middleware retrieving-request-handler) response)
                (recur (a/<! in-pipe))))
            (assoc this :server server :in-pipe in-pipe)))
      this))

  (stop [this]
    (when server
      (log/info "Stopping ow.webapp.async.Webapp")
      (server))
    (when in-pipe
      (a/close! in-pipe))
    (assoc this :server nil :in-pipe nil)))

(defn webapp [request-channel response-channel & {:keys [middleware httpkit-options]}]
  (map->Webapp {:request-channel request-channel
                :response-channel response-channel
                :middleware (or middleware identity)
                :httpkit-options (merge {:port 8080
                                         :worker-name-prefix "async-webapp-worker-"}
                                        httpkit-options)
                :pending-channels (atom {})}))



#_(let [reqch (a/chan)
        resch (a/chan)
        srv    (-> (webapp reqch resch) owl/start)]
    (a/go-loop [req (a/<! reqch)]
      (when-not (nil? req)
        (println "got request:" req)
        (Thread/sleep 1000)
        (a/put! resch (assoc req :status 201 :body "yeah!"))
        (recur (a/<! reqch))))
    (Thread/sleep 15000)
    (owl/stop srv)
    (a/close! resch)
    (a/close! reqch))
