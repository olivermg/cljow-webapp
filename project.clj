(defproject cljow-webapp "0.1.1-SNAPSHOT"

  :description "Handling webapps & their lifecycles in a uniform way"

  :url "https://github.com/olivermg/cljow-webapp"

  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}

  :dependencies [[bidi "2.1.5"]
                 [bk/ring-gzip "0.3.0"]
                 [buddy "2.0.0"]
                 [cljow-app "0.1.0-SNAPSHOT"]
                 [com.cognitect/transit-clj "0.8.313"]
                 [com.fasterxml.jackson.core/jackson-databind "2.9.8"]  ;; to remove ambiguous dependencies
                 [http-kit "2.4.0-alpha4"]
                 [io.clojure/liberator-transit "0.3.1" :exclusions [com.cognitect/transit-clj]]
                 [liberator "0.15.2"]
                 [metosin/muuntaja "0.6.3"]
                 [org.clojure/clojure "1.10.0" :scope "provided"]
                 [org.clojure/core.async "0.4.490"]
                 [org.clojure/tools.logging "0.4.1"]
                 [ring/ring-defaults "0.3.2"]]

  :profiles {:dev {:dependencies [[org.apache.logging.log4j/log4j-core "2.11.2"]]}}

  :pedantic? :abort)
