(ns llama.project
  (:use clj-arrow.arrow
        (Hafni.swing 
          [component :only [component input-arr]]
          [tree :only [tree]]
          [view :only [icon]])
        [clojure.java.io :only [file]])
  (:require [llama.leiningen.new :as llama-new]
            (llama [editor :as editor]
                   [repl :as repl])
            (seesaw [core :as ssw]
                    [chooser :as ssw-chooser])
            [hafni-seesaw.core :as hssw]
            (leiningen [core :as lein-core]
                       [run :as lein-run]
                       [deps :as lein-deps])
            (Hafni.swing [dialog :as dialog])))

(def project-pane (ssw/flow-panel))

(defn run-project [project]
  {:pre [(contains? project :project-thread)]}
  (let [a (:project-thread project)]
    (reset! a (Thread. #(lein-run/run project)))
    (.start @a)))

(defn stop-project [project]
  {:pre [(contains? project :project-thread)]}
  (let [a (:project-thread project)]
    (when (and @a (.isAlive @a))
      (.interrupt @a)
      (reset! a nil)))) ; not really needed, but more elegant

(defn create-project-menu [project]
  [(ssw/action :name "run"
               :handler (fn [_] (run-project project)))
   (ssw/action :name "stop"
               :handler (fn [_] (stop-project project)))
   (ssw/action :name "deps"
               :handler (fn [_] (lein-deps/deps project)))
   (ssw/action :name "repl"
               :handler (fn [_] (repl/create-new-repl project)))])

(defn create-file-tree [path]
  (let [root (file path)
        child (map create-file-tree (.listFiles root))]
    {:root (.getName root)
     :child child}))

(def create-project-file-tree
  (>>> :target-dir create-file-tree))

(defn create-new-project-tree [project]
  (let [t (tree :content (create-project-file-tree project))
        tc (component t)]
    (ssw/config! tc :popup (fn [e] 
                             (if-let [raw_path (.getPathForLocation tc (.getX e) (.getY e))]
                               (let [path (->> raw_path
                                            .getPath
                                            (map :root))]
                                 (if (.endsWith (last path) ".clj")
                                   [(ssw/action :name "open file" 
                                                :handler (fn [_]
                                                           (editor/open-file {:path (.getCanonicalPath 
                                                                                      (apply file 
                                                                                             (cons (:target-dir project) (rest path))))
                                                                              :title (last path)
                                                                              })))]
                                   (if (= (count path) 1)
                                     (create-project-menu project)))))))
    (.setCellRenderer
      tc (proxy [javax.swing.tree.DefaultTreeCellRenderer] []
          (getTreeCellRendererComponent [tree value sel expanded leaf row has_focus]
            (let [c (proxy-super getTreeCellRendererComponent tree value sel expanded leaf row has_focus)]
              (when (and (.toString value) (.endsWith (.toString value) ".clj"))
                    (.setIcon c (component (icon :path (.getPath (file "icons" "clj.gif"))))))
              c))))
    tc))

(def load-project
  (>>> clone
       (*** (hssw/output-arr project-pane :items)
            create-new-project-tree)
       #(concat (first %) [(second %)])
       (hssw/input-arr project-pane :items)))

(def create-new-project
  (>>> (fn [& _]
         (ssw-chooser/choose-file))
       #(if %
          (llama-new/new-project (.getName %) (.getPath %)))))

;(def create-and-load-new-project
;  (>>> create-new-project
;       load-project))

(def load-project-from-file
  (>>> (constantly [nil nil])
       (||| (fn [& _] 
              (dialog/open-file))
            (>>> first
                 #(lein-core/read-project (:path %))
                 #(assoc % :project-thread (atom nil))
                 load-project))))

