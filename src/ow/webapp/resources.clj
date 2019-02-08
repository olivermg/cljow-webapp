(ns ow.webapp.resources
  (:require [clojure.spec.alpha :as s]
            [io.clojure.liberator-transit]  ;; needs to be loaded for liberator to support transit
            [liberator.core :as lc]))

;;;
;;; default liberator resource maps
;;;

(def default-web-resource {:allowed-methods #{:get}
                           :available-media-types #{"text/html"}})

(def default-api-resource {:allowed-methods #{:get}
                           :available-media-types #{"application/json" "application/edn"
                                                    "application/transit+json" "application/transit+msgpack"}})

;;;
;;; wrappers
;;;

(defn wrap-resource-with-request-spec [resource spec & {:keys [body-getter conformed-kw]}]
  (let [body-getter  (or body-getter :body-params)
        conformed-kw (or conformed-kw :body-params-conformed)]
    (fn [request]
      (let [body           (body-getter request)
            conformed-body (s/conform spec body)]
        (if-not (= conformed-body :clojure.spec.alpha/invalid)
          (resource (assoc request conformed-kw conformed-body))
          {:status 400
           :body (some->> (s/explain-data spec body)
                          :clojure.spec.alpha/problems
                          pr-str
                          (str "invalid request body: "))})))))
