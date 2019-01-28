(ns ow.webapp.resource-wrappers
  (:require [clojure.spec.alpha :as s]))

(defn wrap-resource-with-request-spec [resource spec & {:keys [body-getter conformed-kw]}]
  (let [body-getter  (or body-getter :body-params)
        conformed-kw (or conformed-kw :body-params-conformed)]
    (fn [request]
      (println "REQUEST" request)
      (let [body           (body-getter request)
            conformed-body (s/conform spec body)]
        (println "REQUEST2 " body conformed-body)
        (if-not (= conformed-body :clojure.spec.alpha/invalid)
          (resource (assoc request conformed-kw conformed-body))
          {:status 400
           :body (some->> (s/explain-data spec body)
                          :clojure.spec.alpha/problems
                          pr-str
                          (str "invalid request body: "))})))))
