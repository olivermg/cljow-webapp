(ns ow.webapp.async.router
  (:require [bidi.bidi :as b]
            [clojure.core.async :as a]
            [clojure.tools.logging :as log]
            [ow.app.lifecycle :as owl]
            [ow.app.messaging :as owm]
            [ow.app.messaging.component-async :as owc]))

(defn- handle [routes this msg]
  (let [{:keys [uri] :as request} (owm/get-data msg)
        {:keys [handler]} (b/match-route routes uri)]
    (if handler
      (owm/put! this (owm/message msg :http/resource handler))
      (owm/put! this (owm/message msg :http/response {:status 404
                                                      :body "resource not found"})))))

(defn router [in-ch out-ch routes]
  (owc/component "router-async" in-ch out-ch :http/request (partial handle routes)))



#_(do (require '[ow.webapp.async :as wa])
      (let [request-ch  (a/chan)
            response-ch (a/chan)
            resource-ch (a/chan)
            webapp      (-> (wa/webapp request-ch response-ch) owl/start)
            routes      ["/" [["foo" :foo]
                              ["bar" :bar]]]
            router      (-> (router request-ch response-ch resource-ch routes) owl/start)]
        (a/go-loop [{:keys [:http/resource] :as msg} (a/<! resource-ch)]
          (when-not (nil? msg)
            (println "got resource:" resource)
            (Thread/sleep 1000)
            (a/put! response-ch (assoc msg :http/response {:status 201 :body (str "yeah, " resource "!")}))
            (recur (a/<! resource-ch))))
        (Thread/sleep 15000)
        (owl/stop router)
        (owl/stop webapp)
        (a/close! resource-ch)
        (a/close! response-ch)
        (a/close! request-ch)))
