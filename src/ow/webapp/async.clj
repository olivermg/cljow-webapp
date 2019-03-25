(ns ow.webapp.async
  (:require [clojure.core.async :as a]
            [clojure.tools.logging :as log]
            [org.httpkit.server :as hk]
            [ow.lifecycle :as owl]
            [ow.comm :as owc]))

(defn- request-handler [middleware-instance {:keys [::out-ch] :as this} http-req]
  (hk/with-channel http-req ch
    (hk/on-close ch (fn [status]
                      (log/trace "channel closed with status" status)))
    (future
      (let [http-req-after-middlewares (atom http-req)
            _ (middleware-instance (assoc http-req ::captured-request http-req-after-middlewares))
            response (owc/request out-ch @http-req-after-middlewares)]
        (hk/send! ch (middleware-instance (assoc http-req ::captured-response response)))))))

(defn- capturing-handler [{:keys [::captured-request ::captured-response] :as req}]
  (when captured-request
    (reset! captured-request (dissoc req ::captured-request ::captured-response)))
  (when captured-response
    captured-response))

(defn construct [out-ch & {:keys [middleware httpkit-options]}]
  (owl/construct ::webapp {::out-ch out-ch
                           ::middleware (or middleware identity)
                           ::httpkit-options (merge {:port 8080
                                                     :worker-name-prefix "async-webapp-worker-"}
                                                    httpkit-options)}))

(defmethod owl/start ::webapp [{:keys [::middleware ::httpkit-options ::server] :as this}]
  (if-not server
    (let [server (hk/run-server (partial request-handler (middleware capturing-handler) this)
                                httpkit-options)]
      (assoc this ::server server))
    this))

(defmethod owl/stop ::webapp [{:keys [::server] :as this}]
  (when server
    (server))
  (assoc this ::server nil))



#_(do (require '[clojure.core.async :as a])
    (require '[ow.app.component :as owc])
    (let [reqch   (a/chan)
          resch   (a/chan)
          srv     (-> {} (owc/init "webapp-async") (init reqch resch :httpkit-options {:thread 1}) start)]
      (a/go-loop [req (a/<! reqch)]
        (when-not (nil? req)
          (future
            (println "got req:" req)
            (Thread/sleep 1000)
            #_(let [a (atom 0)]
              (dotimes [i 100000000]
                (swap! a inc))
              (println @a))
            (a/put! resch (owrrc/new-response req {:status 200 :body "foo1"})))
          (recur (a/<! reqch))))
      (Thread/sleep 20000)
      (stop srv)
      (a/close! resch)
      (a/close! reqch)))
