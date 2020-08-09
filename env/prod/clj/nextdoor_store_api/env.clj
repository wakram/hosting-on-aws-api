(ns nextdoor-store-api.env
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[nextdoor-store-api started successfully]=-"))
   :stop
   (fn []
     (log/info "\n-=[nextdoor-store-api has shut down successfully]=-"))
   :middleware identity})
