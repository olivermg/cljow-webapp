(ns ow.webapp.async
  (:require [clojure.core.async :as a]
            [ow.logging.api.alpha :as log]
            [org.httpkit.server :as hk]
            #_[ow.comm :as owc]
            #_[ow.lifecycle :as owl]
            [ow.system :as ows]
            [ow.system.request-listener :as owsr]))

(defn- request-handler [middleware-instance this http-req]
  (letfn [(handle-exception [req ch e]
            (log/debug "FAILED to process http request"
                       {:request req
                        :error   e})
            (hk/send! ch (middleware-instance (assoc http-req ::captured-response {:status (get (ex-data e) :status 500)
                                                                                   :body   {:error (str e)}})))
            (throw e))]

    (hk/with-channel http-req ch
      (log/trace "received http request" {:request http-req})
      (hk/on-close ch (fn [status]
                        (log/trace "channel closed with status" {:status status})))
      (future
        (try
          (let [http-req-after-middlewares (atom http-req)
                _ (middleware-instance (assoc http-req ::captured-request http-req-after-middlewares))
                response (owsr/request this :http/request @http-req-after-middlewares)]
            (log/trace "sending http response" {:response response})
            (hk/send! ch (middleware-instance (assoc http-req ::captured-response response))))
          (catch Exception e
            (handle-exception http-req ch e))
          (catch Error e
            (handle-exception http-req ch e)))))))

(defn- capturing-handler [{:keys [::captured-request ::captured-response] :as req}]
  (when captured-request
    (reset! captured-request (dissoc req ::captured-request ::captured-response)))
  (when captured-response
    captured-response))

#_(defn construct [name out-ch & {:keys [middleware httpkit-options]}]
  (owl/construct ::webapp name {::out-ch out-ch
                                ::middleware (or middleware identity)
                                ::httpkit-options (merge {:port 8080
                                                          :worker-name-prefix "async-webapp-worker-"}
                                                         httpkit-options)}))

#_(defmethod owl/start* ::webapp [{:keys [::middleware ::httpkit-options ::server] :as this}]
  (if-not server
    (let [server (hk/run-server (partial request-handler (middleware capturing-handler) this)
                                httpkit-options)]
      (log/info "started async webapp" {:httpkit-options httpkit-options})
      (assoc this ::server server))
    this))

#_(defmethod owl/stop* ::webapp [{:keys [::server] :as this}]
  (when server
    (server))
    (assoc this ::server nil))

(defn make-lifecycle [& {:keys [middleware httpkit-options]}]
  (let [middleware      (or middleware identity)
        httpkit-options (merge {:port 8080
                                :worker-name-prefix "webapp-async-worker-"}
                               httpkit-options)]

    (letfn [(start [this]
              (let [server (hk/run-server (partial request-handler (middleware capturing-handler) this)
                                          httpkit-options)]
                (assoc this ::server server)))

            (stop [{:keys [::server] :as this}]
              (server :timeout 10000)
              (dissoc this ::server))]

      {:start start
       :stop stop})))



#_(do (require '[clojure.core.async :as a])
    (let [reqch   (a/chan)
          srv     (-> (construct reqch)
                      owl/start)]
      (a/go-loop [{:keys [request response-ch] :as request-map} (a/<! reqch)]
        (when-not (nil? request-map)
          (future
            (println "got req:" request)
            (Thread/sleep 1000)
            (a/put! response-ch {:status 201 :body "foobar"}))
          (recur (a/<! reqch))))
      (Thread/sleep 20000)
      (owl/stop srv)
      (a/close! reqch)))
