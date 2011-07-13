(ns llama.project
  (:use clj-arrow.arrow
        [clojure.java.io :only [file]])
  (:require [llama.leiningen.new :as llama-new]
            (llama [editor :as editor]
                   [error :as error]
                   [repl :as repl])
            (seesaw [core :as ssw]
                    [mig :as ssw-mig]
                    [tree :as ssw-tree]
                    [color :as ssw-color]
                    [chooser :as ssw-chooser])
            [hafni-seesaw.core :as hssw]
            (leiningen [core :as lein-core]
                       [run :as lein-run]
                       [deps :as lein-deps])))

(def project-pane 
  (let [p (ssw-mig/mig-panel)]
    (.setBackground p (ssw-color/color 255 255 255))
    p))

(defn run-project [project]
  {:pre [(contains? project ::project-thread)
         (contains? project ::file-tree)]}
  (if (contains? project :main)
    (let [a (::project-thread project)]
      (reset! a (Thread. 
                  (fn []
                    (let [d (ssw/custom-dialog 
                              :parent (::file-tree project) :on-close :dispose :modal? true
                              :content (ssw/vertical-panel 
                                         :items [(ssw/label :text (str "Running project " (:name project) ".")
                                                            :border 10)
                                                 (ssw/progress-bar :indeterminate? true :border 10)]))
                          t (Thread. #(do (lein-run/run project)
                                        (ssw/dispose! d)))]
                      (.start t)
                      (-> d ssw/pack! ssw/show!)
                      (if (.isAlive t) (.interrupt t))))))
      (.start @a))
    (error/show-error "no main in project" 
                      :parent (::file-tree project))))

(defn stop-project [project]
  {:pre [(contains? project ::project-thread)]}
  (let [a (::project-thread project)]
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

(defrecord custom-file [file]
  Object
  (toString [_] (.getName file)))

(defn create-file-tree-model [path]
  (ssw-tree/simple-tree-model 
    (fn [node] (.isDirectory (:file node)))
    (fn [parent] 
      (let [children (.listFiles (:file parent))
            groups (group-by #(.isDirectory %) children)]
       (map #(custom-file. %)
            (concat (sort-by #(.getName %) (get groups true))
                    (sort-by #(.getName %) (get groups false))))))
    (custom-file. (file path))))

(defn set-icon [c icon]
  (.setIcon c (ssw/icon (ClassLoader/getSystemResource (str "icons/" icon)))))

(defn create-new-project-tree [project]
  (let [tc (javax.swing.JTree. (create-file-tree-model (:target-dir project)))
        project (assoc project ::file-tree tc)
        project_menu (create-project-menu project)]
    (ssw/config! tc :popup (fn [e] 
                             (if-let [raw_path (.getPathForLocation tc (.getX e) (.getY e))]
                               (let [path (->> raw_path
                                            .getPath
                                            (map #(.getName (:file %))))
                                     selected_file (apply file (cons (:target-dir project) (rest path)))]
                                 (concat 
                                   (cond 
                                     (not (.isDirectory selected_file))
                                     [(ssw/action :name "open file" 
                                                  :handler (fn [_]
                                                             (editor/open-file {:path (.getCanonicalPath selected_file)
                                                                                :title (last path)})))
                                      :separator]
                                     :else [])
                                   project_menu)))))
    (.setCellRenderer
      tc (proxy [javax.swing.tree.DefaultTreeCellRenderer] []
          (getTreeCellRendererComponent [tree value sel expanded leaf row has_focus]
            (let [c (proxy-super getTreeCellRendererComponent tree value sel expanded leaf row has_focus)]
              (if (.toString value)
                (condp #(.endsWith (.toString %2) %1) value
                  ".clj"  (set-icon c "clj.gif")
                  ".jar"  (set-icon c "java-jar.png")
                  ".java" (set-icon c "System-Java-icon.png")
                  nil))
              c))))
    tc))

(def load-project
  (>>> clone
       (*** (hssw/output-arr project-pane :items)
            (>>> 
              #(assoc % ::project-thread (atom nil))
              create-new-project-tree))
       #(concat (first %) [[(second %) "span"]])
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
              (ssw-chooser/choose-file))
            (>>> first
                 #(lein-core/read-project (.getCanonicalPath %))
                 load-project))))

