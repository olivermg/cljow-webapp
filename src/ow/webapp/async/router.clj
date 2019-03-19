(ns ow.webapp.async.router
  (:require [bidi.bidi :as b]
            [clojure.tools.logging :as log]
            #_[ow.app.lifecycle :as owl]
            #_[ow.app.messaging :as owm]
            [ow.app.request-response-component :as owrrc]))

#_(defn- handle [routes this msg]
  (let [{:keys [uri] :as request} (owm/get-data msg)
        {:keys [handler]} (b/match-route routes uri)]
    (if handler
      (owm/put! this (owm/message msg :http/resource handler))
      (owm/put! this (owm/message msg :http/response {:status 404
                                                      :body "resource not found"})))))

#_(defn router [in-ch out-ch routes]
  (owc/component "router-async" in-ch out-ch :http/request (partial handle routes)))



(defn- handle [{{:keys [routes]} ::config :as this} http-request]
  (let [{:keys [uri]} http-request
        {:keys [handler]} (b/match-route routes uri)]
    (if handler
      (-> (owrrc/request this :http/routed-request handler)
          (owrrc/wait-for-response))
      {:status 404
       :body "resource not found"})))

(defn init [this name request-ch response-ch routed-request-ch routed-response-ch routes]
  (-> this
      (owrrc/init-responder name request-ch response-ch :http/request handle)
      (owrrc/init-requester name routed-request-ch routed-response-ch)
      (assoc ::config {:routes routes}
             ::runtime {})))

(defn start [{{:keys [started?]} ::runtime
              :as this}]
  (if-not started?
    (do (log/info "Starting ow.webapp.async.router.Router")
        (-> this
            (assoc ::runtime {:started? true})
            (owrrc/start-requester)
            (owrrc/start-responder)))
    this))

(defn stop [{{:keys [started?]} ::runtime
             :as this}]
  (-> this
      (owrrc/stop-responder)
      (owrrc/stop-requester)
      (assoc ::runtime {})))



#_(do (require '[clojure.core.async :as a])
    (require '[ow.webapp.async :as wa])
    (let [ch          (a/chan)
          mult        (a/mult ch)

          webapp      (-> {} (wa/init "webapp1" ch (a/tap mult (a/chan))) wa/start)

          routes      ["/" [["foo" :foo]
                            ["bar" :bar]]]
          router      (-> {} (init "router1" (a/tap mult (a/chan)) ch ch (a/tap mult (a/chan)) routes) start)

          test-ch     (a/tap mult (a/chan))
          pub         (a/pub test-ch (fn [r] [(get r :ow.app.request-response-component/type) (get r :ow.app.request-response-component/topic)]))
          sub         (a/sub pub [:request :http/routed-request] (a/chan))
          ]

      (a/go-loop [req (a/<! sub)]
        (when-not (nil? req)
          (println "got routed-request:" req)
          (Thread/sleep 1000)
          (a/put! ch (owrrc/new-response req {:status 201 :body (str req)}))
          (recur (a/<! sub))))
      (Thread/sleep 15000)
      (stop router)
      (wa/stop webapp)
      (a/close! ch)))
