(ns ow.webapp.integrant
  (:require [integrant.core :as ig]
            [ow.webapp :as wa])
  (:import [ow.webapp Webapp]))

(defmethod ig/init-key :ow.webapp/integrant [_ opts]
  (if-not (instance? Webapp opts)
    (-> (wa/map->Webapp opts)
        (wa/start))
    opts))

(defmethod ig/halt-key! :ow.webapp/integrant [_ this]
  (when (instance? Webapp this)
    (wa/stop this)))
