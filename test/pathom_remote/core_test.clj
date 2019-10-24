(ns pathom-remote.core-test
  (:require [clojure.test :refer :all]
            [com.wsscode.pathom.core :as p]
            [com.wsscode.pathom.diplomat.http :as p.http]
            [com.wsscode.pathom.diplomat.http.clj-http :as clj-http]
            [com.wsscode.pathom.connect :as pc]
            [pathom-remote.core :as pcr]))

(def home (System/getProperty "user.home"))
(def token (str "Bearer " (slurp (str home "/dev/nu/.nu/tokens/br/prod/access"))))
(def key "/Users/marcelo.piva/dev/nu/.nu/certificates/br/prod/nubanker.p12")
(def query [::pc/indexes])

(require '[clj-http.client :as client])
(require '[cognitect.transit :as transit])
(import '[java.io ByteArrayInputStream ByteArrayOutputStream])

(defn request [req]
  (-> req
      clj-http/build-request-map
      (assoc :keystore key :keystore-pass "nubankp12")
      client/request
      clj-http/build-response-map
      prn))

(def req {::p.http/driver  #'request
          ::p.http/url     "https://prod-global-abrams.nubank.com.br/api/graph"
          ::p.http/headers {"Authorization" [token]}})

(defn query-remote [q]
  (-> {::p.http/content-type ::p.http/transit+json
       ::p.http/accept       ::p.http/transit+json
       ::p.http/method       ::p.http/post
       ::p.http/form-params  q
       ::p.http/driver       #'request
       ::p.http/url          "https://prod-global-abrams.nubank.com.br/api/graph"
       ::p.http/headers      {"Authorization" [token]}}
      p.http/request
      ::p.http/body
      (.getBytes)
      (ByteArrayInputStream.)
      (transit/reader :json)
      transit/read))

(query-remote [{[:customer/cpf "10809232413"]
                [:customer/name]}])
  
(pc/defresolver local-customer
  [env {:customer/keys [name]}]
  {::pc/input  #{:customer/name}
   ::pc/output [:customer/my-name]}
  (prn name)
  {:customer/my-name (str "My name: " name)})

(pc/defresolver global-customer-cpf
  [env _]
  {::pc/input  #{}
   ::pc/output [:customer/cpf]}
  {:customer/cpf "10809232413"})

(def resolvers [local-customer])

(def parser2
  (p/parser
   {::p/plugins [(p/env-plugin {::p/reader               [pc/reader2
                                                          p/map-reader
                                                          pc/open-ident-reader
                                                          (p/placeholder-reader ">")]
                                ::p/placeholder-prefixes #{">"}})
                 (pc/connect-plugin {::pc/register resolvers})
                 (pcr/remote-connect-plugin {::pcr/req req})]}))


(parser2 {} [{[:customer/cpf "10809232413"]
              [:customer/my-name]}])
