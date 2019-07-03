(ns ow.webapp.async.router
  (:require [bidi.bidi :as b]
            #_[clojure.tools.logging :as log]
            [ow.logging.api.alpha :as log]
            [ow.system :as ows]
            [ow.system.request-listener :as osrl]))

(defn- make-handler [routes]
  (fn handler [this {:keys [http/request]}]
    ;;; TODO: remember to keep handlers idempotent, as they may be retried by the framework
    (let [{:keys [uri]} request
          {:keys [route-params handler]} (b/match-route routes uri)]
      (if handler
        (osrl/request this handler (assoc request :route-params route-params))
        {:status 404
         :body "resource not found"}))))

(defn make-component [routes]
  {:ow.system/request-listener {:topics  #{:http/request}
                                :handler (make-handler routes)}})
