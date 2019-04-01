(ns ow.webapp.common.middlewares
  (:require [buddy.auth.backends :as bbe]
            [buddy.auth.middleware :refer [wrap-authentication]]
            [muuntaja.middleware :as mm]
            [ring.middleware.defaults :as rmd]
            [ring.middleware.gzip :as rmg]))

(defn make-middleware [& {:keys [api?
                                 test-environment?
                                 authentication-backend]}]

  (let [defaults   (if api?
                     (if-not test-environment?
                       rmd/secure-api-defaults
                       rmd/api-defaults)
                     (if-not test-environment?
                       rmd/secure-site-defaults
                       rmd/site-defaults))
        wrap-auth  (if authentication-backend
                     (fn [handler]
                       (wrap-authentication handler authentication-backend))
                     identity)]

    (fn ow.webapp.common.middleware [handler]
      (-> handler
          (mm/wrap-format)
          (rmd/wrap-defaults defaults)
          (wrap-auth)
          (rmg/wrap-gzip)))))



#_(let [mw (make-middleware :test-environment? true
                          :api? true
                          :authentication-backend (bbe/basic {:realm "foobar"
                                                              :authfn (fn [request authdata]
                                                                        (:username authdata))}))
      h  (mw (fn [req]
               (println "REQ" req)
               {:status 201
                :body {:foo "bar" :username (:identity req)}}))]
  (-> (h {:uri "/foo"
          :request-method :get
          :headers {"authorization" "Basic Zm9vOmJhcg=="}})
      (update :body slurp)))
