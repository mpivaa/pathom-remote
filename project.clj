(defproject pathom-remote "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url  "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [clj-http "3.10.0"]
                 [com.cognitect/transit-clj "0.8.319"]
                 [com.wsscode/pathom "2.2.26"]]
  :repl-options {:init-ns pathom-remote.core})
