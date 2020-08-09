(ns nextdoor-store-api.aws.certificate
  (:require
    [schema.core :as s]
    [cognitect.aws.client.api :as aws]
    [cognitect.aws.credentials :as credentials]
    [clojure.java.io :as io]
    [cuerdas.core :as str]
    [cprop.core :refer [load-config]]))


(defn domain-name-builder
  [domain-name]
  (if (str/includes? domain-name "*")
    (str/replace domain-name "*." "")
    domain-name))

(def conf (load-config :file "config.edn"))

(def acm (aws/client {:api                  :acm
                      :region               :us-east-1
                      :credentials-provider (credentials/basic-credentials-provider
                                              {:access-key-id     (conf :access-key-id)
                                               :secret-access-key (conf :secret-access-key)})}))


;ssl listing
;-----------------------------
(defn certificate-arn-list-from-acm
  []
  (try
    (aws/invoke acm {:op :ListCertificates})
    (catch Exception e)))

;;create ssl certificate
;;---------------------------------------------------------------
(defn create-ssl-certificate
  [domain-name domains]
  (try

    (let [certificate-arn (aws/invoke
                            acm {:op      :RequestCertificate
                                 :request {:DomainName              domain-name
                                           :SubjectAlternativeNames (str/split domains #",")
                                           :ValidationMethod        "DNS"
                                           :Tags                    {:Key   "dnshotname"
                                                                     :Value (domain-name-builder domain-name)}}})]
      {:CertificateArn (:CertificateArn certificate-arn)})
    (catch Exception e
      (println e))))




;check ssl
(defn check-ssl-certificate
  "docstring"
  [domain-name]
  (try
    (let [certificatearn (:CertificateSummaryList (certificate-arn-list-from-acm))
          certificatearn (first
                           (vec
                             (filter
                               (comp #{domain-name}
                                     :DomainName)
                               certificatearn)))]

      (if (empty? certificatearn)
        {:status 404
         :body   {:error "Domain not found."}}
        {:status 200
         :body   {:result (aws/invoke acm {:op      :DescribeCertificate
                                   :request {:CertificateArn (:CertificateArn certificatearn)}})}}))
    (catch Exception e
      (println e)))
  )
;;---------------------------------------------------------


;delete ssl
;;---------------------------------------------------------
(defn delete-ssl-ceritificate
  "docstring"
  [domain-name]
  (try
    (let [certificatearn (:CertificateSummaryList (certificate-arn-list-from-acm))
          certificatearn (first
                           (vec
                             (filter
                               (comp #{domain-name}
                                     :DomainName)
                               certificatearn)))]

      (if (empty? certificatearn)
        {:status 404
         :body   {:error "Domain not found."}}
        {:status 200
         :body   {:result (aws/invoke acm {:op      :DeleteCertificate
                                   :request {:CertificateArn (:CertificateArn certificatearn)}})}})

      ) (catch Exception e
          (println e))))

;;---------------------------------------------------------