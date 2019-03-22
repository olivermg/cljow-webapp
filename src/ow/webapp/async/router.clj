(ns ow.webapp.async.router
  (:require [bidi.bidi :as b]
            [clojure.tools.logging :as log]
            [ow.app.request-response-component :as owrrc]))

(defn- handle [{{:keys [routes]} ::config :as this} http-request]
  (let [{:keys [uri]} http-request
        {:keys [handler]} (b/match-route routes uri)]
    (if handler
      (-> (owrrc/request this :http/routed-request (assoc http-request :handler handler))
          (owrrc/wait-for-response))
      {:status 404
       :body "resource not found"})))

(defn init [this request-ch response-ch routed-request-ch routed-response-ch routes]
  (-> this
      (owrrc/init-responder request-ch response-ch :http/request handle)
      (owrrc/init-requester routed-request-ch routed-response-ch)
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
    (require '[ow.app.component :as owc])
    (require '[ow.webapp.async :as wa])
    (let [ch          (a/chan)
          mult        (a/mult ch)

          webapp      (-> {}
                          (owc/init "webapp1")
                          (wa/init ch (a/tap mult (a/chan)))
                          wa/start)

          routes      ["/" [["foo" :foo]
                            ["bar" :bar]]]
          router      (-> {}
                          (owc/init "router1")
                          (init (a/tap mult (a/chan)) ch ch (a/tap mult (a/chan)) routes)
                          start)

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
