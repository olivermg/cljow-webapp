(ns ow.webapp.async.resource
  (:require [clojure.tools.logging :as log]
            [ow.comm :as owc]
            [ow.lifecycle :as owl]))

(defn construct [in-ch handler]
  (owc/construct in-ch handler))



#_(do (require '[clojure.core.async :as a])
    (require '[ow.webapp.async :as wa])
    (require '[ow.webapp.async.router :as war])
    (let [request-ch   (a/chan)
          routed-ch    (a/chan)
          routed-pub   (a/pub routed-ch owc/topic-fn)
          foo-sub      (a/sub routed-pub :foo (a/chan))
          bar-sub      (a/sub routed-pub :bar (a/chan))

          webapp       (-> (wa/construct request-ch)
                           owl/start)

          routes       ["/" [["foo" :foo]
                             ["bar" :bar]]]
          router       (-> (war/construct request-ch routed-ch routes)
                           owl/start)

          foo-resource (-> (construct foo-sub (fn [this request]
                                                (println "processing resource foo:" request)
                                                (Thread/sleep 500)
                                                {:status 201
                                                 :body (str request)}))
                           owl/start)

          bar-resource (-> (construct bar-sub (fn [this request]
                                                (println "processing resource bar:" request)
                                                (Thread/sleep 300)
                                                {:status 202
                                                 :body (str request)}))
                           owl/start)]

      (Thread/sleep 15000)
      (owl/stop bar-resource)
      (owl/stop foo-resource)
      (owl/stop router)
      (owl/stop webapp)
      (a/unsub routed-pub :bar bar-sub)
      (a/unsub routed-pub :foo foo-sub)
      (a/close! routed-ch)
      (a/close! request-ch)))
