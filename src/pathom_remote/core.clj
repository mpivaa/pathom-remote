(ns pathom-remote.core
  (:require [com.wsscode.pathom.connect :as pc]
            [com.wsscode.pathom.core :as p]
            [com.wsscode.pathom.diplomat.http.clj-http :as clj-http]
            [com.wsscode.pathom.diplomat.http :as p.http]
            [cognitect.transit :as transit])
  (:import [java.io ByteArrayInputStream ByteArrayOutputStream]))

(defn query-remote [req q]
  (-> (merge {::p.http/content-type ::p.http/transit+json
              ::p.http/accept       ::p.http/transit+json
              ::p.http/method       ::p.http/post
              ::p.http/form-params  q
              ::p.http/driver       clj-http/request}
             req)
      p.http/request
      ::p.http/body
      (.getBytes)
      (ByteArrayInputStream.)
      (transit/reader :json)
      transit/read))

(def indexes-query [::pc/indexes])

(defn fetch-remote-indexes
  [{::keys [req]}]
  (::pc/indexes (query-remote req indexes-query)))

(defn resolve-remote
  [{::keys [req] :as env} _ out]
  (let [entity (p/entity env)
        q      [{`([:ident/null :null] {:pathom/context ~entity})
                 out}]]
    (-> (query-remote req q)
        vals
        first)))

(defn rebuild-index-resolvers
  [remote-index-resolvers resolver]
  (->> remote-index-resolvers
       (map (fn [[sym config]]
              (let [out (::pc/output config)]
                [sym
                 {::pc/sym     sym
                  ::pc/input   (::pc/input config)
                  ::pc/output  out
                  ::pc/resolve (fn [env in] (resolver env in out))}])))
       (into {})))

(defn rebuild-indexes
  [remote-indexes resolver]
  (-> remote-indexes
      (update ::pc/index-resolvers #(rebuild-index-resolvers % resolver))))

(defn remote-connect-plugin
  "Plugin to add remote integration.
  Options:
  ::driver
  "
  [config]
  (let [remote-indexes  (fetch-remote-indexes config)
        rebuilt-indexes (rebuild-indexes remote-indexes resolve-remote)]
    {::p/wrap-parser2
     (fn [parser {::p/keys [plugins]}]
       (let [idx-atoms (keep ::pc/indexes plugins)]
         (doseq [idx* idx-atoms]
           (swap! idx* pc/merge-indexes rebuilt-indexes))
         (fn [env tx]
           (parser (merge env config) tx))))}))

