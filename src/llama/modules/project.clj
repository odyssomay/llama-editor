(ns llama.modules.project
  (:use clj-arrow.arrow
        (llama [util :only [tab-listener]]
               [state :only [defstate load-state]]
               [leiningen :only [run-leiningen]]
               [module-utils :only [add-view send-to-module set-module-focus]])
        [clojure.java.io :only [file]])
  (:require [llama.leiningen.new :as llama-new]
            (llama [error :as error]
                   [util :as util])
            (llama.modules
              [editor :as editor]
              [repl :as repl])
            (seesaw [core :as ssw]
                    [mig :as ssw-mig]
                    [tree :as ssw-tree]
                    [color :as ssw-color]
                    [chooser :as ssw-chooser])
            (leiningen [core :as lein-core]
                       [run :as lein-run]
                       [deps :as lein-deps]))
  (:import llama.util.tab-model))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; running

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

(defn run-project-command [project]
  (if-let [command (ssw/input "Enter command")]
    (let [p (util/start-process command (:target-dir project))
          output-area (javax.swing.JTextArea. (str "=>" command "\n"))
          input-area (javax.swing.JTextField.)
          dialog (ssw/dialog :content (ssw/border-panel :center (ssw/scrollable output-area) :south input-area)
                             :size [500 :by 500]
                             :success-fn (fn [& _] (.destroy (:process p))))]
      (.setEditable output-area false)
      (.addActionListener input-area
        (reify java.awt.event.ActionListener
          (actionPerformed [_ _]
            (let [text (.getText input-area)]
              (.write (:output-stream p) text)
              (.append output-area text)))))
      (util/write-stream-to-text (:input-stream p) output-area)
      (util/write-stream-to-text (:error-stream p) output-area)
      (ssw/listen dialog :window-closing (fn [& _] (.destroy (:process p))))
      (ssw/show! dialog))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; menu

(defn project-menu [project]
  [:separator
   (ssw/action :name "run"
               :handler (fn [_] (run-project project)))
   (ssw/action :name "stop"
               :handler (fn [_] (stop-project project)))
   (ssw/action :name "deps"
               :handler (fn [_] (run-leiningen project "deps")))
   (ssw/action :name "repl"
               :handler (fn [_] (send-to-module :repl :open project)))
   (ssw/action :name "close"
               :handler (fn [_] ;(close-project project)
                          ))])

(defn specialized-menu [tree project update-tree]
  (fn [e]
    (if-let [raw_path (.getPathForLocation tree (.getX e) (.getY e))]
      (let [path (->> raw_path
                   .getPath
                   (map #(.getName (:file %))))
            selected-file (apply file (cons (:target-dir project) (rest path)))]
        [(ssw/action :name "new file" :enabled? (.isDirectory selected-file)
                     :handler (fn [_]
                                (when-let [name (ssw/input "filename")]
                                  (.createNewFile (file selected-file name))
                                  (update-tree))))
         (ssw/action :name "open file" :enabled? (not (.isDirectory selected-file))
                     :handler (fn [_]
                                (send-to-module :editor :open
                                   selected-file
;                                  {:path (.getCanonicalPath selected_file)
;                                   :title (last path)}
                                               )))
         (ssw/menu :text "advanced"
                   :items 
                   [(ssw/action :name "run command"
                                :handler (fn [_] (run-project-command project)))
                    :separator
                    (ssw/action :name "remove file" :enabled? (not (.isDirectory selected-file))
                                :handler (fn [_]
                                            (.delete selected-file)
                                            (update-tree)))])]))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; model

(defrecord custom-file [file]
  Object
  (toString [_] (.getName file)))

(defn file-tree-model [path]
  (ssw-tree/simple-tree-model 
    (fn [node] (.isDirectory (:file node)))
    (fn [parent] 
      (let [children (.listFiles (:file parent))
            groups (group-by #(.isDirectory %) children)]
       (map #(custom-file. %)
            (concat (sort-by #(.getName %) (get groups true))
                    (sort-by #(.getName %) (get groups false))))))
    (custom-file. (file path))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; cell renderer

(defn set-icon [c icon]
  (.setIcon c (ssw/icon (ClassLoader/getSystemResource (str "icons/" icon)))))

(defn project-tree-renderer []
  (proxy [javax.swing.tree.DefaultTreeCellRenderer] []
    (getTreeCellRendererComponent [tree value sel expanded leaf row has_focus]
                                  (let [c (proxy-super getTreeCellRendererComponent tree value sel expanded leaf row has_focus)]
                                    (if (.toString value)
                                      (condp #(.endsWith (.toString %2) %1) value
                                        ".clj"  (set-icon c "clj.gif")
                                        ".jar"  (set-icon c "java-jar.png")
                                        ".java" (set-icon c "System-Java-icon.png")
                                        nil))
                                    c))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; project tree

(defn project-tree [project]
  (let [model (::model project)
        tc (javax.swing.JTree. (if model model (file-tree-model (:target-dir project))))
        project (assoc project ::project-tree tc :content tc)
        projectm (project-menu project)
        update-tree (fn [& _] (.invalidate tc))
        menu-f (specialized-menu tc project update-tree)]
    (.schedule (java.util.Timer.)
      (proxy [java.util.TimerTask] []
        (run [] (update-tree)))
      (long 1500))
    (ssw/config! tc :popup (fn [e] (concat (menu-f e) projectm)))
    (.setCellRenderer tc (project-tree-renderer))
    project))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; state

(def projects (atom []))

(defn load-project [project]
  (swap! projects conj
         (merge {:title (.getName (file (:target-dir project)))
                 :tip (:target-dir project)
                 :model (file-tree-model (:target-dir project))
                 :project-thread (atom nil)}
                project)))

(defn create-new-project [f]
  (llama-new/new-project (.getName f) (.getPath f)))

(defn create-and-load-new-project [& _]
  (when-let [f (util/new-file-dialog)]
    (create-new-project f)
    (load-project (lein-core/read-project (.getCanonicalPath (file f "project.clj"))))))

(defn load-project-from-file [& _]
  (if-let [f (ssw-chooser/choose-file)]
    (-> (lein-core/read-project (.getCanonicalPath f))
        load-project)))

(defn project-view []
  (let [tp (ssw/tabbed-panel :overflow :scroll)
        tmodel (tab-model. tp projects)
        action-fn
        (fn [id]
          (case id
            :new (create-and-load-new-project)
            :open (load-project-from-file)))]
    (let [listener (tab-listener tmodel 
                     (fn [raw-tab]
                       (let [tab (project-tree raw-tab)]
                         tab)))]
      (add-watch projects (gensym) listener)
      (listener nil nil [] @projects))
    (set-module-focus :project action-fn)
    {:content tp}))

(defstate :projects (fn [] (map :target-dir @projects)))
(load-state :projects
  #(doseq [path %]
     (load-project (lein-core/read-project (.getCanonicalPath (file path "project.clj"))))))

(defn init-module []
  (add-view "project" project-view))
