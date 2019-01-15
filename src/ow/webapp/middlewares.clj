(ns ow.webapp.middlewares
  (:require [ring.middleware.defaults :as rmd]
            [ring.middleware.gzip :as rmg]))

(defn- api-middleware* [defaults handler]
  (-> handler
      (rmd/wrap-defaults defaults)
      (rmg/wrap-gzip)))

(defn api-middleware [handler]
  (api-middleware* rmd/api-defaults handler))

(defn secure-api-middleware [handler]
  (api-middleware* rmd/secure-api-defaults handler))


(defn- site-middleware* [defaults handler]
  (-> handler
      (rmd/wrap-defaults defaults)
      (rmg/wrap-gzip)))

(defn site-middleware [handler]
  (site-middleware* rmd/site-defaults handler))

(defn secure-site-middleware [handler]
  (site-middleware* rmd/secure-site-defaults handler))
