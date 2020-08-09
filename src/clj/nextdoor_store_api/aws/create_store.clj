(ns nextdoor-store-api.aws.create-store
  (:require [nextdoor-store-api.aws.s3 :as s3]
            [nextdoor-store-api.aws.cloudfront :as cf]))


(defn create-new-branded-store
  "docstring"
  [domain-name aliases-domain]
  (let []

    (if (= (:error (s3/s3-bucket aliases-domain)) 1)
      {:status 409
       :body {:error "bucket already exists"}}
      {:status 200
       :body {:result (cf/create-cloudfornt domain-name aliases-domain)}}
      )
    )
  )
