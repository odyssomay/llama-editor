(ns llama.project
  (:use clj-arrow.arrow
        [clojure.java.io :only [file]])
  (:require [llama.leiningen.new :as llama-new]
            (llama [editor :as editor]
                   [error :as error]
                   [repl :as repl]
                   [state :as state]
                   [lib :as lib])
            (seesaw [core :as ssw]
                    [mig :as ssw-mig]
                    [tree :as ssw-tree]
                    [color :as ssw-color]
                    [chooser :as ssw-chooser])
            (leiningen [core :as lein-core]
                       [run :as lein-run]
                       [deps :as lein-deps])))

(lib/log :trace "started loading")

(def project-pane 
  (let [p (ssw-mig/mig-panel)]
    (.setBackground p (ssw-color/color 255 255 255))
    p))

(def current-projects (atom []))
(add-watch current-projects nil (fn [_ _ _ items]
                                  (ssw/config! project-pane :items (map #(vec [(::project-tree %) "span"]) items))))

(defn run-project [project]
  {:pre [(contains? project ::project-thread)
         (contains? project ::project-tree)]}
  (if (contains? project :main)
    (let [a (::project-thread project)]
      (reset! a (Thread. 
                  (fn []
                    (let [d (ssw/custom-dialog 
                              :parent (::project-tree project) :on-close :dispose :modal? true
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
                      :parent (::project-tree project))))

(defn stop-project [project]
  {:pre [(contains? project ::project-thread)]}
  (let [a (::project-thread project)]
    (when (and @a (.isAlive @a))
      (.interrupt @a)
      (reset! a nil)))) ; not really needed, but more elegant

(defn create-project-menu [project]
  [:separator
   (ssw/action :name "run"
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
        project (assoc project ::project-tree tc)
        project_menu (create-project-menu project)
        update_tree (fn [& _] (.updateUI tc))]
    (ssw/config! tc :popup (fn [e] 
                             (if-let [raw_path (.getPathForLocation tc (.getX e) (.getY e))]
                               (let [path (->> raw_path
                                            .getPath
                                            (map #(.getName (:file %))))
                                     selected_file (apply file (cons (:target-dir project) (rest path)))]
                                 (concat
                                   (if (.isDirectory selected_file)
                                     [(ssw/action :name "new file"
                                                  :handler (fn [_]
                                                             (when-let [name (ssw/input "filename")]
                                                               (.createNewFile (file selected_file name))
                                                               (update_tree))))])
                                   (cond 
                                     (not (.isDirectory selected_file))
                                     [(ssw/action :name "open file" 
                                                  :handler (fn [_]
                                                             (editor/open-file {:path (.getCanonicalPath selected_file)
                                                                                :title (last path)})))
                                      (ssw/action :name "remove file"
                                                  :handler (fn [_]
                                                             (.delete selected_file)
                                                             (update_tree)))]
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
    project))

(defn load-project [project]
  (swap! current-projects conj 
         (-> (create-new-project-tree project) 
             (assoc ::project-thread (atom nil)))))

(def create-new-project
  (>>> (fn [& _]
         (ssw-chooser/choose-file))
       #(if %
          (llama-new/new-project (.getName %) (.getPath %)))))

(defn create-and-load-new-project [& _]
  (when-let [f (ssw-chooser/choose-file)]
    (llama-new/new-project (.getName f) (.getCanonicalPath f))
    (load-project (lein-core/read-project (.getCanonicalPath (file f "project.clj"))))))

(defn load-project-from-file [& _]
  (if-let [f (ssw-chooser/choose-file)]
    (-> (lein-core/read-project (.getCanonicalPath f))
        load-project)))

(state/defstate :project-pane (fn [] (map :target-dir @current-projects)))
(state/load-state :project-pane
  #(doseq [project %]
     (load-project (lein-core/read-project (.getCanonicalPath (file project "project.clj"))))))

(lib/log :trace "finished loading")
