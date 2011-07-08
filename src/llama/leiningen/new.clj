(ns llama.leiningen.new
  (:use leiningen.new
        [leiningen.core :only [abort]]
        [clojure.java.io :only [file]]
        [leiningen.util.paths :only [ns->path]]))

;; Copy of leiningen.new/new with a minor correction (sob)

(defn new-project
  "Create a new project skeleton."
  ([project-name]
     (leiningen.new/new project-name (name (symbol project-name))))
  ([project-name project-dir]
     (when (re-find project-name-blacklist project-name)
       (abort "Sorry, *jure names are no longer allowed."))
     (try (read-string project-name)
          (catch Exception _
            (abort "Sorry, project names must be valid Clojure symbols.")))
     (let [project-name (symbol project-name)
           group-id (namespace project-name)
           artifact-id (name project-name)
           project-dir (-> ;(System/getProperty "leiningen.original.pwd")
                           (file project-dir)
                           (.getAbsolutePath ))]
       (write-project project-dir project-name)
       (let [prefix (.replace (str project-name) "/" ".")
             project-ns (str prefix ".core")
             test-ns (str prefix ".test.core")
             project-clj (ns->path project-ns)]
         (spit (file project-dir ".gitignore")
               (apply str (interleave ["pom.xml" "*jar" "/lib/" "/classes/"
                                       ".lein-failures" ".lein-deps-sum"]
                                      (repeat "\n"))))
         (write-implementation project-dir project-clj project-ns)
         (write-test project-dir test-ns project-ns)
         (write-readme project-dir artifact-id)
         (println "Created new project in:" project-dir)))))
