(ns llama.modules.repl
  (:use (llama [config :only [get-option]]
               [util :only [start-process drop-nth log write-stream-to-text tab-listener]]
               [state :only [defstate load-state save-defined-state]]
               [module-utils :only [add-view set-module-focus]])
        (llama.modules
          [syntax :only [parens-count]]
          [document :only [text-delegate text-model]])
        clj-arrow.arrow
        [clojure.java.io :only [copy resource reader]]
 	[clojure.string :only (split join)])
  (:require [clojure.java.io :as cio]
            [seesaw.core :as ssw]
            llama.leiningen.repl
            (leiningen [core :as lein-core] 
                       [classpath :as lein-classpath]))
  (:import llama.util.tab-model 
    (javax.swing JTextArea)
    (java.awt.event KeyEvent KeyListener)))

(defn start-repl [project]
  (let [classpath (if (::anonymous project)
                    (System/getProperty "java.class.path")
                    (lein-classpath/get-classpath-string project))
        repl-source (with-open [w (java.io.StringWriter.)]
                      (copy (reader (resource "repl.clj")) w)
                      (str w))
        process (start-process (str "java -cp " classpath " clojure.main -") (:target-dir project))]
    (doto (:output-stream process)
      (.write repl-source)
      .flush)
    (merge process project)))

(defn stop-repl [{:keys [process input-stream output-stream] :as repl}]
  (save-defined-state (str "repl-history-" (:name repl)))
  (.destroy process)
  (.close input-stream)
  (.close output-stream))

(defn send-to-repl [repl text]
  (.write (:output-stream repl) text)
  (.flush (:output-stream repl)))

(defn init-repl-input-field [repl repl_pane]
  (let [input_field (:text-pane (text-delegate {:type "clj"}))
        jdoc (.getDocument repl_pane) 
        reset-text? (atom false)
        input_text (atom "")
        history (atom [])
        history_pos (atom 0)]
    (let [state_name (str "repl-history-" (:name repl))]
      (defstate state_name (fn [] @history))
      (load-state state_name #(reset! history (take 50 %))))
    (add-watch history_pos nil
      (fn [_ _ _ value]
        (if (== value 0)
          (.setText input_field @input_text)
          (let [text (nth @history (dec @history_pos))]
            (.setText input_field text)
            (.setCaretPosition input_field (count text))))))
    (.addKeyListener
      input_field
      (reify java.awt.event.KeyListener
        (keyPressed 
          [_ e]
          (condp = (.getKeyCode e)
            KeyEvent/VK_UP (when (and (not (some (partial = \newline) (.getText input_field 0 (.getCaretPosition input_field))))
                                      (< @history_pos (count @history)))
                             (if (== @history_pos 0)
                               (reset! input_text (.getText input_field)))
                             (swap! history_pos inc))
            KeyEvent/VK_DOWN (if (and (not (some (partial = \newline) 
                                                 (drop (.getCaretPosition input_field)
                                                       (.getText input_field))))
                                      (> @history_pos 0))
                               (swap! history_pos dec))
            KeyEvent/VK_ENTER
            (let [text (str (.getText input_field) "\n")]
              (if (= (.trim text) "")
                (reset! reset-text? true)
                (when (<= (parens-count text) 0)
                  (reset! reset-text? true)
                  (.insertString jdoc (.getLength jdoc) text nil) 
                  (send-to-repl repl text)
                  (swap! history #(concat [(apply str (butlast text))] %))
                  (reset! history_pos 0))))
            nil))
        (keyTyped [_ e] )
        (keyReleased 
          [_ e]
          (when @reset-text?
            (.setText input_field "")
            (reset! reset-text? false)))))
    input_field)) 

(defn init-repl-text-fields [repl output_pane error_pane]
  (write-stream-to-text (:input-stream repl) output_pane)
  (write-stream-to-text (:error-stream repl) error_pane))

(defn get-icon-url [icon]
  (ClassLoader/getSystemResource (str "icons/" icon)))

(defn init-repl-panels [repl output_pane error_pane input_panel]
  (let [input_field  (init-repl-input-field repl output_pane)]
    (.setText output_pane "") ; these are for restarting
    (.setText error_pane "")
    (.removeAll input_panel)
    (.add input_panel (ssw/scrollable output_pane) java.awt.BorderLayout/CENTER)
    (.add input_panel input_field java.awt.BorderLayout/SOUTH)
    (write-stream-to-text (:error-stream repl) error_pane)
    (ssw/listen output_pane :mouse-moved 
      (fn [_]
        (if (get-option :general :mouse-focus)
          (.requestFocusInWindow input_field))))
    ))

(declare close-project-repl)

(defn init-repl [obj]
  (let [project (:project obj)
        repl (if (:repl obj) (:repl obj)
               (atom (start-repl project)))
        jtext_pane (:text-pane (text-delegate {:type "clj" :model (:model obj)}))
        err_text (javax.swing.JTextArea.)
        input_panel (ssw/border-panel) 
        repl_panel (javax.swing.JPanel. (java.awt.CardLayout.))
        button_panel 
        (ssw/vertical-panel 
          :items 
          [(ssw/action :icon (get-icon-url "gnome_terminal.png")
                       :tip "Show the input and output of this repl." 
                       :handler (fn [_] (.show (.getLayout repl_panel) repl_panel "input-panel")))
           (ssw/action :icon (get-icon-url "gnome_dialog_warning.png")
                       :tip "Show the error output of this repl."
                       :handler (fn [_] (.show (.getLayout repl_panel) repl_panel "error-panel")))
           (ssw/action :icon (get-icon-url "gnome_view_refresh.png")
                       :tip "Restart this repl."
                       :handler (fn [_]
                                  (stop-repl @repl)
                                  (reset! repl (start-repl project))
                                  (init-repl-panels @repl jtext_pane err_text input_panel)
                                  (.revalidate input_panel)))
           (ssw/action :icon (get-icon-url "gnome_process_stop.png")
                       :tip "Destroy the repl process and close the repl tab."
                       :handler (fn [_] (stop-repl @repl) (close-project-repl project)))])
        panel (ssw/border-panel :center repl_panel
                                :west button_panel)]
    (init-repl-panels @repl jtext_pane err_text input_panel)
    (.add repl_panel input_panel "input-panel")
    (.add repl_panel (ssw/scrollable err_text) "error-panel")

    (.setEditable jtext_pane false)
    (.setEditable err_text false)
    {:content panel
     :path (:target-dir project)
     :title (:name project)}))

(def repls (atom []))

(defn open-repl [tmodel project]
  (let [repl (atom nil)
        tab {:project project
             :model (text-model {:type "clj"})
             :repl repl}]
    (add-watch repl (gensym)
      (fn [_ _ _ new-repl] 
        (write-stream-to-text (:input-stream new-repl) (:model tab))))
    (reset! repl (start-repl project))
    (swap! repls conj tab)))

(defn close-project-repl [project]
  (swap! repls (fn [coll] (remove #(= project (:project %)) coll))))

(defn repl-view []
  (let [tp (ssw/tabbed-panel :placement :right)
        tmodel (tab-model. tp repls)
        action-fn
        (fn [id & [v]]
          (case id
            :experiment (open-repl tmodel {:name "exp" ::anonymous true})
            :open       (open-repl tmodel v)))]
    (let [listener (tab-listener tmodel (constantly false)
                     (fn [raw-tab]
                       (let [tab (init-repl raw-tab)]
                         tab)))]
      (add-watch repls (gensym) listener)
      (listener nil nil [] @repls))
    (set-module-focus :repl action-fn)
    {:content tp}))

(defn init-module []
  (add-view "repl" repl-view))

;(let [tabbed_pane (ssw/tabbed-panel :placement :right)
;      current_repls (atom [])]
;  (add-watch current_repls nil (fn [_ _ _ items] 
;                                 (.removeAll tabbed_pane)
;                                 (ssw/config! tabbed_pane :tabs items)))

;  (defn selected-index [& _] (.getSelectedIndex tabbed_pane))

;  (defn current-repl [& _]
;    (nth @current_repls (.getSelectedIndex tabbed_pane) nil))

;  (defn create-new-repl [project]
;    (let [tab (init-new-repl project)]
;      (swap! current_repls conj tab)
;      (.setSelectedComponent tabbed_pane (:content tab))))

;  (defn create-new-anonymous-repl [& _]
;    (create-new-repl {:name "anonymous" ::anonymous true}))

;    (swap! current_repls drop-nth (.getSelectedIndex tabbed_pane)))

;  (def repl-pane tabbed_pane)

;  (defstate :repl-pane #(map :path @current_repls))
;  (load-state :repl-pane (fn [paths]
;                           (doseq [project (map (comp lein-core/read-project #(.getCanonicalPath (cio/file % "project.clj")))
;                                                paths)]
;                                  (create-new-repl project))))

