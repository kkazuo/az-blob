(defproject az-blob "0.1.1"
  :description "A Clojure library designed to access Azure Blob Storage API without Azure SDK."
  :url "https://github.com/kkazuo/az-blob"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [hato "0.4.0"]
                 [clojure.java-time "0.3.2"]]
  :repl-options {:init-ns az-blob.core})
