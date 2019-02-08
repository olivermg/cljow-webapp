(defproject cljow-webapp "0.1.0-SNAPSHOT"

  :description "Handling webapps & their lifecycles in a uniform way"

  :url "https://github.com/olivermg/cljow-webapp"

  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}

  :dependencies [[bk/ring-gzip "0.3.0"]
                 #_[cljow-app "0.1.0-SNAPSHOT"]
                 [com.cognitect/transit-clj "0.8.313"]
                 [http-kit "2.3.0"]
                 [io.clojure/liberator-transit "0.3.1" :exclusions [com.cognitect/transit-clj]]
                 [liberator "0.15.2"]
                 [org.clojure/clojure "1.10.0"]
                 [org.clojure/tools.logging "0.4.1"]
                 [metosin/muuntaja "0.6.3"]
                 [ring/ring-defaults "0.3.2"]
                 [bidi "2.1.5"]])
