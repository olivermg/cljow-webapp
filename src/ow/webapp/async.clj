(ns ow.webapp.async
  (:require [clojure.core.async :as a]
            [clojure.tools.logging :as log]
            [org.httpkit.server :as hk]
            #_[ow.app.lifecycle :as owl]
            [ow.app.request-response-component :as owrrc]))

(defn- request-handler [middleware-instance this http-req]
  (hk/with-channel http-req ch
    (hk/on-close ch (fn [status]
                      (log/debug "channel closed with status" status)))
    (future
      (let [http-req-after-middlewares (atom http-req)
            _ (middleware-instance (assoc http-req ::captured-request http-req-after-middlewares))
            sys-req (owrrc/new-request :http/request @http-req-after-middlewares)
            response-data (-> (owrrc/request this sys-req)
                              (owrrc/wait-for-response)
                              (owrrc/get-data))]
        (hk/send! ch (middleware-instance (assoc http-req ::captured-response response-data)))))))

(defn- capturing-handler [{:keys [::captured-request ::captured-response] :as req}]
  (when captured-request
    (reset! captured-request (dissoc req ::captured-request ::captured-response)))
  (when captured-response
    captured-response))

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
          srv     (-> {} (init "webapp-async" reqch resch :httpkit-options {:thread 1}) start)]
      (a/go-loop [req (a/<! reqch)]
        (when-not (nil? req)
          (future
            (println "got req:" req)
            (Thread/sleep 1000)
            (let [a (atom 0)]
              (dotimes [i 100000000]
                (swap! a inc))
              (println @a))
            (a/put! resch (owrrc/new-response req {:status 200 :body "foo1"})))
          (recur (a/<! reqch))))
      (Thread/sleep 20000)
      (stop srv)
      (a/close! resch)
      (a/close! reqch)))
