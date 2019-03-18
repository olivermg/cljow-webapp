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
      (->> (owrrc/new-request :http/routed-request handler)
           (owrrc/request this)
           (owrrc/wait-for-response)
           (owrrc/get-data))
      {:status 404
       :body "resource not found"})))

(defn init [this name request-ch response-ch routes]
  (-> this
      (owrrc/init-responder name request-ch response-ch :http/request handle)
      (owrrc/init-requester name request-ch response-ch)
      (assoc ::config {:routes routes}
             ::runtime {})))

(defn start [{{:keys [started?]} ::runtime
              :as this}]
  (if-not started?
    (do (log/info "Starting ow.webapp.async.router.Router")
        (-> this
            (owrrc/start-responder)
            (owrrc/start-requester)
            (assoc this ::runtime {:started? true})))
    this))

(defn stop [{{:keys [started?]} ::runtime
             :as this}]
  (-> this
      (assoc ::runtime {})
      (owrrc/stop-requester)
      (owrrc/stop-responder)))



#_(do (require '[clojure.core.async :as a])
      (require '[ow.webapp.async :as wa])
      (let [ch          (a/chan)
            mult        (a/mult ch)

            webapp-ch   (a/tap mult (a/chan))
            webapp      (-> (wa/webapp webapp-ch ch) owl/start)

            routes      ["/" [["foo" :foo]
                              ["bar" :bar]]]
            router-ch   (a/tap mult (a/chan))
            router      (-> (router router-ch ch routes) owl/start)

            test-ch     (a/tap mult (a/chan))
            pipe        (a/pipe test-ch (a/chan))
            pub         (a/pub pipe :ow.app.messaging/topic)
            sub         (a/sub pub :http/resource (a/chan))]

        (a/go-loop [msg (a/<! sub)]
          (when-not (nil? msg)
            (println "got msg:" msg)
            (Thread/sleep 1000)
            (a/put! ch (owm/message msg :http/response {:status 201 :body (str "yeah, " (owm/get-data msg) "!")}))
            (recur (a/<! sub))))
        (Thread/sleep 15000)
        (owl/stop router)
        (owl/stop webapp)
        (a/close! pipe)
        (a/close! ch)))
