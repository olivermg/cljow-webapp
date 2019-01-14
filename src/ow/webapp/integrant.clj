(ns ow.webapp.integrant
  (:require [integrant.core :as ic]
            [ow.webapp :as wa])
  (:import [ow.webapp Webapp]))

(defmethod ic/init-key :ow.webapp/integrant [_ opts]
  (if-not (instance? Webapp opts)
    (wa/map->Webapp opts)
    opts))
