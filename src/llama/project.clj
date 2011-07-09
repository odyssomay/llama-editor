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
            (leiningen [core :as lein-core]
                       [run :as lein-run]
                       [deps :as lein-deps]
                       [new :as lein-new])
            (Hafni.swing [dialog :as dialog])))

(def *current-project* (atom nil))
(def *current-project-thread* (atom nil))

(def *project-tree* 
  (let [t (tree)
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
                                                                                             (cons (:target-dir @*current-project*) (rest path))))
                                                                              :title (last path)
                                                                              })))]
                                   [(ssw/menu-item :text "non clojure file")])))))
    (.setCellRenderer
      tc (proxy [javax.swing.tree.DefaultTreeCellRenderer] []
          (getTreeCellRendererComponent [tree value sel expanded leaf row has_focus]
            (let [c (proxy-super getTreeCellRendererComponent tree value sel expanded leaf row has_focus)]
              (when (and (.toString value) (.endsWith (.toString value) ".clj"))
                    (.setIcon c (component (icon :path (.getPath (file "icons" "clj.gif")))))
                ;    (ssw/config! c :popup (fn [_] [(ssw/menu-item :text "clojure file")]))
                )
              c))))
    t))

(defn current-project [& _]
  @*current-project*)

(defn current-project-thread [& _]
  @*current-project-thread*)

(defn create-file-tree [path]
  (let [root (file path)
        child (map create-file-tree (.listFiles root))]
    {:root (.getName root)
     :child child}))

(def create-project-file-tree
  (>>> current-project
       :target-dir
       create-file-tree))

(defn run-project [file]
  (lein-run/run (lein-core/read-project file)))

(def create-new-project
  (>>> (fn [& _]
         (ssw-chooser/choose-file))
       #(if %
          (llama-new/new-project (.getName %) (.getPath %)))))

(def load-project-from-file
  (>>> (constantly [nil nil])
       (||| (fn [& _] 
              (dialog/open-file))
            (>>> first
                 #(swap! *current-project* 
                         (constantly (lein-core/read-project (:path %))))
                 current-project
                 create-project-file-tree
                 (input-arr *project-tree* :content)))))

(defn run-current-project [& _]
  (if @(current-project)
    (do (reset! *current-project-thread*
                (Thread. 
                  #(lein-run/run (current-project))))
        (.start (current-project-thread)))))

(defn stop-current-project [& _]
  (if (and (current-project)
           (current-project-thread)
           (.isAlive (current-project-thread)))
    (.interrupt (current-project-thread))))

(defn current-project-dependencies [& _]
  (if (current-project)
    (lein-deps/deps (current-project))))

(def start-project-repl
  (>>> current-project
       repl/create-new-repl))

(def project-pane
  (ssw/scrollable (component *project-tree*)))


(comment
  (:import (java.io File)
	   (javax.swing JScrollPane tree.TreeModel))

(defn create-tree [^File root]
  (let [children (map #(File. root %) (.list root))
	grouped_children (group-by #(.isDirectory %) children)
	files (map #(.getName %) (grouped_children false))
	dirs (map create-tree (grouped_children true))]
    {:dir root :files files :dirs dirs}))

(defn file-tree-model [root]
  (proxy [TreeModel] []
    (getRoot [] root )
    (isLeaf [node] (.isFile node))
    (getChildCount [parent] )
    (getChild [parent index] )
    (getIndexOfChild [parent child] )
    (valueForPathChanged [path newvalue] )
    (addTreeModelListener [listener] )
    (removeTreeModelListener [listener] )))

(defn create-project-tree [path]
  )


(defn get-project-pane []
  (JScrollPane.))
)

