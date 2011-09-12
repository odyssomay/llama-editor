(ns llama.leiningen
  (:use (llama [statics :only [llama-dir]]
               [util :only [start-process]])
        [clojure.java.io :only [copy file reader]]
        [clojure.string :only [join]]))

(def lein-file     (file llama-dir "lein.sh"))
(def lein-bat-file (file llama-dir "lein.bat"))
(def wget-file     (file llama-dir "wget.exe"))

(defn fetch-leiningen* []
  (copy (reader "https://raw.github.com/technomancy/leiningen/1.6.1/bin/lein")     lein-file)
  (copy (reader "https://raw.github.com/technomancy/leiningen/1.6.1/bin/lein.bat") lein-bat-file)
  (copy (reader "http://users.ugent.be/~bpuype/cgi-bin/fetch.pl?dl=wget/wget.exe")  wget-file))

(defn fetch-leiningen []
  (if-not (and (.exists lein-file)
               (.exists lein-bat-file)
               (.exists wget-file))
    (fetch-leiningen*)))

(defn get-leiningen-command [args]
  (let [win? (.contains (.toLowerCase (System/getProperty "os.name"))
                       "win")]
    (str (if win? 
           (.getCanonicalPath lein-bat-file)
           (str "sh " (.getCanonicalPath lein-file)))
         " " (join " " args))))

(defn run-leiningen [project & args]
  (start-process
    (get-leiningen-command args)
    (:target-dir project)))
