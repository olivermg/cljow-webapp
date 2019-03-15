(ns ow.webapp.async
  (:require [clojure.tools.logging :as log]
            [org.httpkit.server :as hk]
            [ow.app.lifecycle :as owl]
            #_[ow.app.messaging :as owm]
            #_[ow.app.messaging.component :as owc]
            [ow.app.request-response-component :as owrrc]))

(defn- request-handler [middleware-instance this http-req]
  (hk/with-channel http-req ch
    (hk/on-close ch (fn [status]
                      (log/debug "channel closed with status" status)))
    (let [http-req-after-middlewares (atom http-req)
          _ (middleware-instance (assoc http-req ::captured-request http-req-after-middlewares))
          sys-req (owrrc/new-request :http/request @http-req-after-middlewares)]
      (-> (owrrc/request this sys-req)
          (owrrc/handle-response #(when (hk/open? ch)
                                    (hk/send! ch (middleware-instance (assoc http-req ::captured-response (owrrc/get-data %)))))))
      nil)))

#_(defn- response-handler [middleware-instance pending-channels messaging-component msg]
  (let [flow-id (owm/get-flow-id msg)
        response (owm/get-data msg)]
    (when-let [[ch orig-req] (get @pending-channels flow-id)]
      (when (hk/open? ch)
        (hk/send! ch (middleware-instance (assoc orig-req ::captured-response response))))
      (swap! pending-channels #(dissoc % flow-id)))))

(defn- capturing-handler [{:keys [::captured-request ::captured-response] :as req}]
  (when captured-request
    (reset! captured-request (dissoc req ::captured-request ::captured-response)))
  (when captured-response
    captured-response))

#_(defn- retrieving-request-handler [{:keys [::captured-response] :as req}]
  captured-response)

#_(defrecord Webapp [messaging-component middleware httpkit-options pending-channels
                   server]

  owl/Lifecycle

  (start [this]
    (if-not server
      (do (log/info "Starting ow.webapp.async.Webapp")
          (let [server (hk/run-server (partial request-handler (middleware storing-request-handler) this)
                                      httpkit-options)]
            (assoc this
                   :server server
                   :messaging-component (owl/start messaging-component))))
      this))

  (stop [this]
    (when server
      (log/info "Stopping ow.webapp.async.Webapp")
      (server))
    (assoc this
           :server nil
           :messaging-component (owl/stop messaging-component))))

#_(defn webapp [in-ch out-ch & {:keys [middleware httpkit-options]}]
  (let [middleware (or middleware identity)
        pending-channels (atom {})
        partial-handler (partial response-handler (middleware retrieving-request-handler) pending-channels)
        mc (owc/component "webapp-async" in-ch out-ch :http/response partial-handler)]
    (map->Webapp {:messaging-component mc
                  :middleware middleware
                  :httpkit-options (merge {:port 8080
                                           :worker-name-prefix "async-webapp-worker-"}
                                          httpkit-options)
                  :pending-channels pending-channels})))



(defn init [this name request-ch response-ch & {:keys [middleware httpkit-options]}]
  (let [middleware (or middleware identity)]
    (-> this
        (owrrc/init-requester name request-ch response-ch)
        (assoc ::config {:name name
                         :middleware middleware
                         :httpkit-options (merge {:port 8080
                                                  :worker-name-prefix "async-webapp-worker-"}
                                                 httpkit-options)}
               ::runtime {}))))

(defn start [{{:keys [name middleware httpkit-options]} ::config
              {:keys [server]} ::runtime
              :as this}]
  (if-not server
    (do (log/info "Starting ow.webapp.async.Webapp")
        (let [this   (owrrc/start-requester this)
              server (hk/run-server (partial request-handler (middleware capturing-handler) this)
                                    httpkit-options)]
          (assoc this ::runtime {:server server})))
    this))

(defn stop [{{:keys [name]} ::config
             {:keys [server]} ::runtime
             :as this}]
  (when server
    (log/info "Stopping ow.webapp.async.Webapp")
    (server))
  (-> this
      (assoc ::runtime {})
      owrrc/stop-requester))



#_(do (require '[clojure.core.async :as a])
    (let [reqch   (a/chan)
          resch   (a/chan)
          srv     (-> {} (init "webapp-async" reqch resch) start)]
      (a/go-loop [req (a/<! reqch)]
        (when-not (nil? req)
          (println "got req:" req)
          (Thread/sleep 1000)
          (a/put! resch (owrrc/new-response req {:status 200 :body "foo1"}))
          (recur (a/<! reqch))))
      (Thread/sleep 15000)
      (stop srv)
      (a/close! resch)
      (a/close! reqch)))
