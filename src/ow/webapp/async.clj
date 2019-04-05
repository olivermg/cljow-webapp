(ns ow.webapp.async
  (:require [clojure.core.async :as a]
            [clojure.tools.logging :as log]
            [org.httpkit.server :as hk]
            [ow.comm :as owc]
            [ow.lifecycle :as owl]))

(defn- request-handler [middleware-instance {:keys [::out-ch] :as this} http-req]
  (letfn [(handle-exception [req e]
            (log/debug "FAILED to process http request"
                       {:request req
                        :error   e})
            (throw e))]

    (hk/with-channel http-req ch
      (log/trace "received http request" http-req)
      (hk/on-close ch (fn [status]
                        (log/trace "channel closed with status" status)))
      (future
        (try
          (let [http-req-after-middlewares (atom http-req)
                _ (middleware-instance (assoc http-req ::captured-request http-req-after-middlewares))
                response (owc/request out-ch @http-req-after-middlewares)]
            (log/trace "sending http response" response)
            (hk/send! ch (middleware-instance (assoc http-req ::captured-response response))))
          (catch Exception e
            (handle-exception http-req e))
          (catch Error e
            (handle-exception http-req e)))))))

(defn- capturing-handler [{:keys [::captured-request ::captured-response] :as req}]
  (when captured-request
    (reset! captured-request (dissoc req ::captured-request ::captured-response)))
  (when captured-response
    captured-response))

(defn construct [name out-ch & {:keys [middleware httpkit-options]}]
  (owl/construct ::webapp name {::out-ch out-ch
                                ::middleware (or middleware identity)
                                ::httpkit-options (merge {:port 8080
                                                          :worker-name-prefix "async-webapp-worker-"}
                                                         httpkit-options)}))

(defmethod owl/start* ::webapp [{:keys [::middleware ::httpkit-options ::server] :as this}]
  (if-not server
    (let [server (hk/run-server (partial request-handler (middleware capturing-handler) this)
                                httpkit-options)]
      (log/info "started async webapp" httpkit-options)
      (assoc this ::server server))
    this))

(defmethod owl/stop* ::webapp [{:keys [::server] :as this}]
  (when server
    (server))
  (assoc this ::server nil))



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
