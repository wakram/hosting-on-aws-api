(ns nextdoor-store-api.aws.route53
  (:require
    [schema.core :as s]
    [cognitect.aws.client.api :as aws]
    [cognitect.aws.credentials :as credentials]
    [clojure.java.io :as io]
    [cuerdas.core :as str]
    [cprop.core :refer [load-config]]))


(def conf (load-config :file "config.edn"))

(def route53 (aws/client {:api                  :route53
                          :region               :us-east-1
                          :credentials-provider (credentials/basic-credentials-provider
                                                  {:access-key-id     (conf :access-key-id)
                                                   :secret-access-key (conf :secret-access-key)})}))
(defn listed-hosted-zone
  "docstring"
  []
  (aws/invoke route53 {:op :ListHostedZones}))

(defn create-hosted-zone
  "docstring"
  [domain-name]

  (aws/invoke route53 {:op      :CreateHostedZone
                       :request {:Name            domain-name
                                 :CallerReference (str/join
                                                    [domain-name
                                                     "-nextdoor"
                                                     (.format
                                                       (java.text.SimpleDateFormat. "MM-dd-yyyy-hh-mm-ss")
                                                       (new java.util.Date))
                                                     ])
                                 }})
  )



(defn create-record-set
  "docstring"
  [domain-name name value]
  (let [hostedinfo (aws/invoke route53 {:op :ListHostedZones})
        domain-info (vec (filter (comp #{(str domain-name ".")} :Name)
                                 (:HostedZones hostedinfo)))

        hosted-id (last (str/split (:Id (first domain-info)) #"/"))
        ]
    (aws/invoke route53 {:op      :ChangeResourceRecordSets
                         :request {:HostedZoneId hosted-id,
                                   :ChangeBatch  {:Changes
                                                  [{:Action            "UPSERT"
                                                    :ResourceRecordSet {:ResourceRecords [{:Value value}]
                                                                        :TTL             300
                                                                        :Name            name
                                                                        :Type            "CNAME"}}]}}})
    )

  )


(defn delete-record-set
  "docstring"
  [domain-name name value]
  (let [hostedinfo (aws/invoke route53 {:op :ListHostedZones})
        domain-info (vec (filter (comp #{(str domain-name ".")} :Name)
                                 (:HostedZones hostedinfo)))

        hosted-id (last (str/split (:Id (first domain-info)) #"/"))
        ]
    (aws/invoke route53 {:op      :ChangeResourceRecordSets
                         :request {:HostedZoneId hosted-id,
                                   :ChangeBatch  {:Changes
                                                  [{:Action            "DELETE"
                                                    :ResourceRecordSet {:ResourceRecords [{:Value value}]
                                                                        :TTL             300
                                                                        :Name            name
                                                                        :Type            "CNAME"}}]}}})
    )

  )

(defn hosted-record-set-info
  "docstring"
  [domain-name]
  (let [hosted-info (vec (filter (comp #{(str domain-name ".")} :Name)
                       (:HostedZones (listed-hosted-zone))))

        hosted-id (last (str/split (:Id (first hosted-info)) #"/"))
        ]
    (aws/invoke route53 {:op      :ListResourceRecordSets
                         :request {:HostedZoneId hosted-id}})
    )
  )


(defn listed-hosted-zone
  "docstring"
  []
  (aws/invoke route53 {:op :ListHostedZones}))

(defn delete-hosted-zone
  "docstring"
  [domain-name]
  (let [hostedinfo (aws/invoke route53 {:op :ListHostedZones})
        domain-info (vec (filter (comp #{(str domain-name ".")} :Name)
                                 (:HostedZones hostedinfo)))

        hosted-id (last (str/split (:Id (first domain-info)) #"/"))]
    (aws/invoke route53 {:op      :DeleteHostedZone
                         :request {:Id hosted-id}})
    )
  )

