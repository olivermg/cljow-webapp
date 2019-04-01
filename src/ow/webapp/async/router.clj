(ns ow.webapp.async.router
  (:require [bidi.bidi :as b]
            [clojure.tools.logging :as log]
            [ow.comm :as owc]
            [ow.lifecycle :as owl]))

(defn- make-handler [out-ch routes]
  (fn handler [this http-request]
    (let [{:keys [uri]} http-request
          {:keys [handler]} (b/match-route routes uri)]
      (if handler
        (owc/request out-ch http-request :topic handler)
        {:status 404
         :body "resource not found"}))))

(defn construct [name in-ch out-ch routes]
  (owc/construct name in-ch (make-handler out-ch routes)))



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
