(ns nextdoor-store-api.aws.cloudfront
  (:require [cognitect.aws.credentials :as credentials]
            [cognitect.aws.client.api :as aws]
            [cuerdas.core :as str]
            [cprop.core :refer [load-config]]
            [nextdoor-store-api.aws.certificate :as acm]))

(def conf (load-config :file "config.edn"))


(def cloudfront (aws/client {:api                  :cloudfront
                             :region               :us-east-1
                             :credentials-provider (credentials/basic-credentials-provider
                                                     {:access-key-id     (conf :access-key-id)
                                                      :secret-access-key (conf :secret-access-key)})}))

(def current-date (.format (java.text.SimpleDateFormat. "MM-dd-yyyy") (new java.util.Date)))

(defn   create-cloudfornt
  "docstring"
  [domain-name aliases-domain]
  (let [certificatearn (:CertificateSummaryList (acm/certificate-arn-list-from-acm))
        certificatearn (first
                         (vec
                           (filter
                             (comp #{domain-name}
                                   :DomainName)
                             certificatearn)))]
    (aws/invoke cloudfront {:op      :CreateDistribution
                            :request {:DistributionConfig
                                      {:CallerReference      (str/join ["nextdoor-" domain-name current-date])
                                       :ViewerCertificate
                                                             {:ACMCertificateArn      (:CertificateArn certificatearn)
                                                              :SSLSupportMethod       "sni-only"
                                                              :MinimumProtocolVersion "TLSv1.1_2016"
                                                              :CertificateSource      "acm"}
                                       :Comment              ""
                                       :Aliases              {:Quantity 1
                                                              :Items    [aliases-domain]}
                                       :DefaultCacheBehavior {:DefaultTTL           86400
                                                              :MaxTTL               31536000
                                                              :MinTTL               0
                                                              :TrustedSigners       {:Enabled  false
                                                                                     :Quantity 0}
                                                              :ViewerProtocolPolicy "allow-all"
                                                              :ForwardedValues      {:QueryString false
                                                                                     :Cookies     {:Forward "none"}}
                                                              :TargetOriginId       (str/join [aliases-domain ".s3.amazonaws.com-nextdoor-store"])}
                                       :CacheBehaviors       {:Quantity 0}
                                       :Origins              {:Quantity 1
                                                              :Items    [{:Id             (str/join [aliases-domain ".s3.amazonaws.com-nextdoor-store"])
                                                                          :DomainName     (str/join [(conf :BucketPrefix) aliases-domain ".s3.amazonaws.com"])
                                                                          :OriginPath     ""
                                                                          :CustomHeaders  {:Quantity 0}
                                                                          :S3OriginConfig {:OriginAccessIdentity ""}
                                                                          }]}
                                       :Enabled              true
                                       :DefaultRootObject    "index.html"}}}))
  )
