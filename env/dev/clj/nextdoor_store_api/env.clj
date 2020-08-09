(ns nextdoor-store-api.env
  (:require
    [selmer.parser :as parser]
    [clojure.tools.logging :as log]
    [nextdoor-store-api.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[nextdoor-store-api started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "\n-=[nextdoor-store-api has shut down successfully]=-"))
   :middleware wrap-dev})
