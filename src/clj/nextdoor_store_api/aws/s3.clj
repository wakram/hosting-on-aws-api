(ns nextdoor-store-api.aws.s3
  (:require
    [schema.core :as s]
    [cognitect.aws.client.api :as aws]
    [cognitect.aws.credentials :as credentials]
    [clojure.java.io :as io]
    [cprop.core :refer [load-config]]
    [cuerdas.core :as str]))


(def conf (load-config :file "config.edn"))


(def s3 (aws/client {:api                  :s3
                     :region               :us-east-1
                     :credentials-provider (credentials/basic-credentials-provider
                                             {:access-key-id     (conf :access-key-id)
                                              :secret-access-key (conf :secret-access-key)})}))

;;create and delete bucket
;;---------------------------------------------------------------

(defn policy
  "docstring"
  [domain-name]
 (str/join ["{\n  \"Version\":\"2012-10-17\",\n  \"Statement\":[{\n\t\"Sid\":\"PublicReadGetObject\",\n        \"Effect\":\"Allow\",\n\t  \"Principal\": \"*\",\n      \"Action\":[\"s3:GetObject\"],\n      \"Resource\":[\"arn:aws:s3:::"domain-name"/*\"\n      ]\n    }\n  ]\n}"
            ])
  )


(defn create-s3-bucket
  [doamin-name]
  (try
    (aws/invoke s3 {:op      :CreateBucket
                    :request {:Bucket (str/join [(conf :BucketPrefix) doamin-name])}})

    (aws/invoke s3 {:op      :PutBucketPolicy
                    :request {:Bucket (str/join [(conf :BucketPrefix) doamin-name])
                              :Policy (policy (str/join [(conf :BucketPrefix) doamin-name]))}})
    (catch Exception e
      (println e))))

(defn delete-s3-bucket
  [doamin-name]
  (try
    (aws/invoke s3 {:op :DeleteBucket :request {:Bucket (str/join [(conf :BucketPrefix) doamin-name])}})
    (catch Exception e
      (println e)))
  )
;;end of create and delete bucket
;;---------------------------------------------------------------


(defn s3-bucket
  "docstring"
  [doamin-name]
  (let [s3-bucket-list (aws/invoke s3 {:op :ListBuckets})
        bucket-exists? (empty? (vec (filter (comp #{(str/join [(conf :BucketPrefix) doamin-name])} :Name)
                                            (s3-bucket-list :Buckets))))]
    (if bucket-exists?
      (create-s3-bucket doamin-name)
      ;;"bucket already exists "
      {:error 1}
      )
    )

  )

;;upload file and delete
;;---------------------------------------------------------------
(defn put-Object-S3
  "docstring"
  [object doamin-name key]
  (aws/invoke s3 {:op :PutObject :request {:Bucket (str/join [(conf :BucketPrefix) doamin-name]) :Key (str/join [key (:filename object)])
                                           :ACL    "public-read"
                                           :Body   (io/input-stream (:tempfile object))}}))


(defn s3-bucket-file-listing
  "docstring"
  [doamin-name]
  (aws/invoke s3 {:op :ListObjectsV2 :request {:Bucket (str/join [(conf :BucketPrefix) doamin-name])}}))


(defn delte-file-from-bucket
  "docstring"
  [doamin-name]
  (aws/invoke s3 {:op :DeleteObject :request {:Bucket (str/join [(conf :BucketPrefix) doamin-name])}}))



;;;----------------------
;;; copy bucket
;;-----------------------

(defn copy-from-master-bucket
  "docstring"
  [domain-name]
  (let [file-list (s3-bucket-file-listing (conf :MasterBucket))]
    (doseq [file (:Contents file-list)]
      (try
        (aws/invoke s3 {:op      :CopyObject
                        :request {:Bucket     (str/join [(conf :BucketPrefix) domain-name])
                                  :CopySource (str/join [(conf :MasterBucket) "/" (:Key file)])
                                  :Key        (:Key file)
                                  :ACL        "public-read"}})
        (catch Exception e
          (println e)
          ))
      )))
