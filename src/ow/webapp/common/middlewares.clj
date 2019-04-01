(ns ow.webapp.common.middlewares
  (:require [buddy.auth.backends :as bbe]
            [buddy.auth.middleware :refer [wrap-authentication]]
            [muuntaja.middleware :as mm]
            [ring.middleware.defaults :as rmd]
            [ring.middleware.gzip :as rmg]))

(defn make-middleware [handler & {:keys [api?
                                         test-environment?
                                         authentication-backend]}]
  (let [defaults (if api?
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
    (-> handler
        (mm/wrap-format)
        (rmd/wrap-defaults defaults)
        (wrap-auth)
        (rmg/wrap-gzip))))
