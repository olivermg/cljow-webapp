(ns ow.webapp.async.router
  (:require [bidi.bidi :as b]
            [clojure.tools.logging :as log]
            [ow.system :as ows]
            [ow.system.request-listener :as osrl]))

(defn- make-handler [routes]
  (fn handler [this http-request]
    (let [{:keys [uri]} http-request
          {:keys [handler]} (b/match-route routes uri)]
      (if handler
        (osrl/request this handler http-request)
        {:status 404
         :body "resource not found"}))))

(defn make-component [routes]
  {:request-listener {:topic   :http/request
                      :handler (make-handler routes)}})



#_(do (require '[clojure.core.async :as a])
    (require '[ow.webapp.async :as wa])
    (let [request-ch  (a/chan)
          routed-ch   (a/chan)

          webapp      (-> (wa/construct request-ch)
                          owl/start)

          routes      ["/" [["foo" :foo]
                            ["bar" :bar]]]
          router      (-> (construct request-ch routed-ch routes)
                          owl/start)]

      (a/go-loop [{:keys [request response-ch] :as msg} (a/<! routed-ch)]
        (when-not (nil? msg)
          (println "got routed-request:" request)
          (Thread/sleep 1000)
          (a/put! response-ch {:status 201 :body "foobar"})
          (recur (a/<! routed-ch))))
      (Thread/sleep 15000)
      (owl/stop router)
      (owl/stop webapp)
      (a/close! routed-ch)
      (a/close! request-ch)))
