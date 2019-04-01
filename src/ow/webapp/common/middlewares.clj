(ns ow.webapp.common.middlewares
  (:require [buddy.auth.backends :as bbe]
            [buddy.auth.middleware :refer [wrap-authentication]]
            [muuntaja.middleware :as mm]
            [ring.middleware.defaults :as rmd]
            [ring.middleware.gzip :as rmg]))

(defn- api-middleware* [defaults handler]
  (-> handler
      (mm/wrap-format)
      (rmd/wrap-defaults defaults)
      (rmg/wrap-gzip)))

(defn api-middleware [handler & {:keys [test-environment?]}]
  (api-middleware* (if-not test-environment?
                     rmd/secure-api-defaults
                     rmd/api-defaults)
                   handler))


(defn- site-middleware* [defaults handler]
  (-> handler
      (mm/wrap-format)
      (rmd/wrap-defaults defaults)
      (rmg/wrap-gzip)))

(defn site-middleware [handler & {:keys [test-environment?]}]
  (site-middleware* (if-not test-environment?
                      rmd/secure-site-defaults
                      rmd/site-defaults)
                    handler))
