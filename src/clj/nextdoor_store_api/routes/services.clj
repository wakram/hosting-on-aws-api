(ns nextdoor-store-api.routes.services
  (:require
    [clojure.spec.alpha :as s]
    [reitit.swagger :as swagger]
    [reitit.swagger-ui :as swagger-ui]
    [reitit.ring.coercion :as coercion]
    [reitit.coercion.spec :as spec-coercion]
    [reitit.ring.middleware.muuntaja :as muuntaja]
    [reitit.ring.middleware.multipart :as multipart]
    [reitit.ring.middleware.parameters :as parameters]
    [nextdoor-store-api.middleware.formats :as formats]
    [nextdoor-store-api.middleware.exception :as exception]
    [ring.util.http-response :refer :all]
    [clojure.java.io :as io]
    [nextdoor-store-api.aws.certificate :as acm]
    [nextdoor-store-api.aws.create-store :as cs]
    [nextdoor-store-api.aws.deploy-build :as dp]
    [nextdoor-store-api.aws.route53 :as r53]
    [spec-tools.core :as st]
    [nextdoor-store-api.aws.s3 :as s3]))

(s/def ::DomainName (st/spec string?))
(s/def ::AlternativeDomain (st/spec string?))
(s/def ::request (s/keys :req-un [::DomainName] :opt-un [::AlternativeDomain]))

(defn service-routes []
  ["/api"
   {:coercion   spec-coercion/coercion
    :muuntaja   formats/instance
    :swagger    {:id ::api}
    :middleware [;; query-params & form-params
                 parameters/parameters-middleware
                 ;; content-negotiation
                 muuntaja/format-negotiate-middleware
                 ;; encoding response body
                 muuntaja/format-response-middleware
                 ;; exception handling
                 exception/exception-middleware
                 ;; decoding request body
                 muuntaja/format-request-middleware
                 ;; coercing response bodys
                 coercion/coerce-response-middleware
                 ;; coercing request parameters
                 coercion/coerce-request-middleware
                 ;; multipart
                 multipart/multipart-middleware]}

   ;; swagger documentation
   ["" {:no-doc  true
        :swagger {:info {:title       "NextDoor Store API"
                         :description "https://www.orientsoftware.net/"}}}

    ["/swagger.json"
     {:get (swagger/create-swagger-handler)}]

    ["/api-docs/*"
     {:get (swagger-ui/create-swagger-ui-handler
             {:url    "/api/swagger.json"
              :config {:validator-url nil}})}]]

   ["/ping"
    {:get (constantly (ok {:message "pong"}))}]


   ["/ssl"
    {:swagger {:tags ["SSL"]}}

    ["/create"
     {:post {:summary     "AWS Certificate Manager - Create SSL Certificate for domain"
             :description "use comma (,) to separate alternativeDomain (i.e : api.aws.com,store.aws.com)"
             :parameters  {:form ::request}
             :handler     (fn [{{{:keys [DomainName AlternativeDomain]} :form} :parameters}]
                            {:status 200
                             :body   (acm/create-ssl-certificate DomainName AlternativeDomain)})}
      }]

    ["/status"
     {:get {:summary    "AWS Certificate Manager - Check SSL Certificate for domain"
            :description "If certificate arn created for wildcard with domain. Use domain name for statu(i.e : example.com)"
            :parameters {:query {:DomainName string?}}
            ;:responses {200 {:body {:result any?}}}
            :handler    (fn [{{{:keys [DomainName]} :query} :parameters}]
                          (acm/check-ssl-certificate DomainName))}
      }]

    ["/delete"
     {:delete {:summary    "AWS Certificate Manager - Delete SSL Certificate for domain"
               :parameters {:query {:DomainName string?}}
               ;:responses {200 {:body {:result any?}}}
               :handler    (fn [{{{:keys [DomainName]} :query} :parameters}]
                             (acm/delete-ssl-ceritificate DomainName))}
      }]

    ]


   ["/hostedzone"
    {:swagger {:tags ["Hostedzone"]}}

    ["/create"
     {:post {:summary     "Create a new hostedzone in rotue53 for domain"
             :description ""
             :parameters  {:form {:DomainName string?,}}
             :handler     (fn [{{{:keys [DomainName]} :form} :parameters}]
                            {:status 200
                             :body   {:result (r53/create-hosted-zone DomainName)}}
                            )}
      }]

    ["/list"
     {:get {:summary     "listing hostedzone in rotue53 for domain"
            :description ""
            :handler     (fn [_]
                           {:status 200
                            :body   {:result (r53/listed-hosted-zone)}}
                           )}
      }]

    ["/delete"
     {:delete {:summary     "Delete hostedzone in rotue53 for domain"
               :description ""
               :parameters  {:form {:DomainName string?}}
               :handler     (fn [{{{:keys [DomainName]} :form} :parameters}]
                              {:status 200
                               :body   {:result (r53/delete-hosted-zone DomainName)}}
                              )}
      }]

    ["/create-record-set"
     {:post {:summary     "Create new hostedzone record set in rotue53 for domain"
             :description ""
             :parameters  {:form {:DomainName string?, :Name string?, :Value string?}}
             :handler     (fn [{{{:keys [DomainName Name Value]} :form} :parameters}]
                            {:status 200
                             :body   {:result (r53/create-record-set DomainName Name Value)}}
                            )}
      }]


    ["/hosted-record-info"
     {:get {:summary    "listing record set"
            :parameters {:query {:DomainName string?}}
            :handler    (fn [{{{:keys [DomainName]} :query} :parameters}]
                          {:status 200
                           :body   {:result (r53/hosted-record-set-info DomainName)}})}
      }]



    ["/delete-record-set"
     {:delete {:summary     "delete hostedzone record set in rotue53 for domain"
               :description ""
               :parameters  {:form {:DomainName string?, :Name string?, :Value string?}}
               :handler     (fn [{{{:keys [DomainName Name Value]} :form} :parameters}]
                              {:status 200
                               :body   {:result (r53/delete-record-set DomainName Name Value)}}
                              )}
      }]

    ]


   ["/store"
    {:swagger {:tags ["Store"]}}

    ["/create"
     {:post {:summary     "Create a new store"
             :description "if acm was created for domain and wildcard together, domain name will be (i.e example.com}. if acm created for wildcard separately then domain will be (i.e : *.example.com or store.example.com)
            \n aliases domain (i.e: store.example.com)"
             :parameters  {:form {:DomainName string?, :AliasesDomain string?}}
             :handler     (fn [{{{:keys [DomainName AliasesDomain]} :form} :parameters}]
                            (cs/create-new-branded-store DomainName AliasesDomain)
                            )}
      }]]

   ["/file"
    {:swagger {:tags ["File"]}}

    ["/upload"
     {:post {:summary     "upload a file"
             :description "Defualt key value (/). For sub bucket use (i.e : public/css/)"
             :parameters  {;:query     {:storeID string?}
                           :multipart {:file multipart/temp-file-part, :DomainName string?, :Key string?}}
             :handler     (fn [{{{:keys [DomainName file Key]} :multipart} :parameters}]
                            {:status 200
                             :body   {:name (:filename file)
                                      :size (:size file)
                                      :data (s3/put-Object-S3 file DomainName Key)
                                      }})}}]
    ]


   ["/deploy"
    {:swagger {:tags ["Deploy"]}}

    ["/build"
     {:post {:summary     "Copy file from master bucket to s3 bucket"
             :description ""
             :parameters  {:form {:DomainName string?,}}
             :handler     (fn [{{{:keys [DomainName]} :form} :parameters}]

                            {:status 200
                             :body   {:result (dp/deploybuild DomainName)}}
                            ;{:status 200
                            ; :body   {:result (cs/create-new-branded-store DomainName)}}

                            )}
      }]]

   ])
