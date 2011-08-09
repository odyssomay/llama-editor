(ns llama.env
  (:use [clojure.java.io :only [file]]
        [lazytest 
         [tracker :only [tracker]]
         [reload :only [reload]]])
  (:import (java.util.concurrent
            ScheduledThreadPoolExecutor TimeUnit)))

(let [t (tracker [(file "src" "llama")] 0)]
  (defn reload-env []
    (let [namespaces (remove #(case %
                                'llama.core true
                                'llama.statics true
                                false)
                             (t))]
      (when (seq namespaces)
        (try
          (print "\nReloading: llama.core ")
          (doseq [n namespaces]
            (print n "")
            (reload n))
          (prn)
          (reload 'llama.core)
          (catch Exception e
            (println "ERROR:\n" e)
            (.printStackTrace e)))))))

(defn watch-env []
  (doto (ScheduledThreadPoolExecutor. 1)
    (.scheduleWithFixedDelay reload-env 0 500 TimeUnit/MILLISECONDS)))

