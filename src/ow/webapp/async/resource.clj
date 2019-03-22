(ns ow.webapp.async.resource
  (:require [clojure.tools.logging :as log]
            [ow.app.request-response-component :as owrrc]))


(defn init [this resource-in-ch resource-out-ch handler-topic handler]
  (-> this
      (owrrc/init-responder resource-in-ch resource-out-ch :http/routed-request handler
                            :data-topic-fn (fn [_ {:keys [handler-topic] :as request}] handler-topic)
                            :data-topic handler-topic)))

(defn start [this]
  (-> this
      (owrrc/start-responder)))

(defn stop [this]
  (-> this
      (owrrc/stop-responder)))



#_(do (require '[clojure.core.async :as a])
    (require '[ow.app.component :as owc])
    (require '[ow.webapp.async :as wa])
    (require '[ow.webapp.async.router :as war])
    (let [ch           (a/chan)
          mult         (a/mult ch)

          webapp       (-> {}
                           (owc/init "webapp1")
                           (wa/init ch (a/tap mult (a/chan)))
                           wa/start)

          routes       ["/" [["foo" :foo]
                             ["bar" :bar]]]
          router       (-> {}
                           (owc/init "router1")
                           (war/init (a/tap mult (a/chan)) ch ch (a/tap mult (a/chan)) routes)
                           war/start)

          foo-resource (-> {}
                           (owc/init "foo-resource")
                           (init (a/tap mult (a/chan)) ch :foo (fn [this request-data]
                                                                 (println "processing resource foo:" request-data)
                                                                 (Thread/sleep 500)
                                                                 {:status 201
                                                                  :body (str request-data)}))
                           start)

          bar-resource (-> {}
                           (owc/init "bar-resource")
                           (init (a/tap mult (a/chan)) ch :bar (fn [this request-data]
                                                                 (println "processing resource bar:" request-data)
                                                                 (Thread/sleep 300)
                                                                 {:status 202
                                                                  :body (str request-data)}))
                           start)

          ;;;test-ch      (a/tap mult (a/chan))
          ;;;pub          (a/pub test-ch (fn [r] [(get r :ow.app.request-response-component/type) (get r :ow.app.request-response-component/topic)]))
          ;;;sub          (a/sub pub [:request :http/routed-request] (a/chan))
          ]

      #_(a/go-loop [req (a/<! sub)]
        (when-not (nil? req)
          (println "got routed-request:" req)
          (Thread/sleep 1000)
          (a/put! ch (owrrc/new-response req {:status 201 :body (str req)}))
          (recur (a/<! sub))))
      (Thread/sleep 15000)
      (stop bar-resource)
      (stop foo-resource)
      (war/stop router)
      (wa/stop webapp)
      (a/close! ch)))
