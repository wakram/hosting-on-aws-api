(ns nextdoor-store-api.aws.deploy-build
  (:require [nextdoor-store-api.aws.s3 :as s3]))



(defn deploybuild
  "docstring"
  [domain-name]

  (s3/copy-from-master-bucket domain-name)

  )
