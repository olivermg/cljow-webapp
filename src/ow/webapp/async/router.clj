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
  {:ow.system/request-listener {:topic   :http/request
                                :handler (make-handler routes)}})
