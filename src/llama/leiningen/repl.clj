(ns llama.leiningen.repl
  (:require (leiningen [repl :as lein-repl]))
  (:use [leiningen.core :only [user-settings]]
        [leiningen.compile :only [eval-in-project]]))

(defn init-repl-server [project port]
  (.start
    (Thread. 
      #(eval-in-project project 
                        (apply lein-repl/repl-server project "localhost" port
                               (concat (:repl-options project)
                                       (:repl-options (user-settings)))))))
  (Thread/sleep 2000)
  (let [t (Thread. (fn [] (lein-repl/poll-repl-connection port)))
        s (java.net.Socket. "localhost" port)]
    (.start t)
    {:thread t
     :input_stream (java.io.InputStreamReader. (.getInputStream s))
     :output_stream (java.io.OutputStreamWriter. (.getOutputStream s))}))
