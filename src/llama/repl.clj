(ns llama.repl
  (:use (llama [document :only [create-doc]]
               [syntax :only [parens-count]]
               [lib :only [start-process]]
                )
        clj-arrow.arrow
        (Hafni.swing component container)
 	[clojure.string :only (split join)])
  (:require [clojure.java.io :as cio]
            [seesaw.core :as ssw]
            llama.leiningen.repl
            (leiningen [classpath :as lein-classpath])
            (Hafni [utils :as hutil]))
  (:import 
    (javax.swing JTextArea)
    (java.awt.event KeyEvent KeyListener)))

(defn start-repl [project]
  (let [classpath (if (::anonymous project)
                    (.getPath (ClassLoader/getSystemResource "clojure-1.2.1.jar"))
                    (lein-classpath/get-classpath-string project))
        repl_path (.getPath (ClassLoader/getSystemResource "repl.clj"))]
    (start-process (str "java -cp " classpath " clojure.main " repl_path))))

(defn stop-repl [{:keys [process input_stream output_stream]}]
;  (.interrupt thread)
  (.destroy process)
  (.close input_stream)
  (.close output_stream))

(defn write-stream-to-text [stream jtext_pane]
  (let [jdoc (.getDocument jtext_pane)
        text (atom "")]
    (.start (Thread. (fn []
                       (try 
                         (do
                           (swap! text str (-> stream .read char str))
                           (when (not (.ready stream))
                             (ssw/invoke-now 
                               (.insertString jdoc (.getLength jdoc) @text nil)
                               (.setCaretPosition jtext_pane (.getLength jdoc)))
                             (reset! text ""))
                           (recur))
                         ; this means that the stream is closed
                         (catch java.lang.IllegalArgumentException _ )))))))

(defn send-to-repl [repl text]
  (.write (:output_stream repl) text)
  (.flush (:output_stream repl)))
(defn init-repl-input-field [repl repl_pane]
  (let [input_field (:text-pane (create-doc {}))
        jdoc (.getDocument repl_pane) 
        reset-text? (atom false)]
    (.addKeyListener
      input_field
      (reify java.awt.event.KeyListener
        (keyPressed 
          [_ e]
          (if (= (.getKeyChar e) \newline)
            (let [text (str (.getText input_field) "\n")]
              (if (= (.trim text) "")
                (reset! reset-text? true)
                (when (zero? (parens-count text))
                  (reset! reset-text? true)
                  (.insertString jdoc (.getLength jdoc) text nil) 
                  (send-to-repl repl text))))))
        (keyTyped [_ e] )
        (keyReleased 
          [_ e]
          (when @reset-text?
            (.setText input_field "")
            (reset! reset-text? false)))))
    input_field)) 

(defn init-repl-text-fields [repl output_pane error_pane]
  (write-stream-to-text (:input_stream repl) output_pane)
  (write-stream-to-text (:error_stream repl) error_pane))

(defn get-icon-url [icon]
  (ClassLoader/getSystemResource (str "icons/" icon)))

(defn init-repl-panels [repl output_pane error_pane input_panel]
  (let [input_field  (init-repl-input-field repl output_pane)]
    (.setText output_pane "") ; these are for restarting
    (.setText error_pane "")
    (.removeAll input_panel)
    (.add input_panel (ssw/scrollable output_pane) java.awt.BorderLayout/CENTER)
    (.add input_panel input_field java.awt.BorderLayout/SOUTH)
    (init-repl-text-fields repl output_pane error_pane)
    (ssw/listen output_pane :mouse-entered (fn [_] (.requestFocusInWindow input_field)))))

(declare close-current-repl)

(defn init-new-repl [project]
  (let [repl (atom (start-repl project)) ;(llama.leiningen.repl/init-repl-server project 6000)
        jtext_pane (:text-pane (create-doc {}))
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
                                  (.updateUI input_panel)))
           (ssw/action :icon (get-icon-url "gnome_process_stop.png")
                       :tip "Destroy the repl process and close the repl tab."
                       :handler (fn [_] (stop-repl @repl) (close-current-repl nil)))])
        panel (ssw/border-panel :center repl_panel
                                :west button_panel)]
    (init-repl-panels @repl jtext_pane err_text input_panel)
    (.add repl_panel input_panel "input-panel")
    (.add repl_panel (ssw/scrollable err_text) "error-panel")

    (.setEditable jtext_pane false)
    (.setEditable err_text false)
    {:content panel  
     :title (:name project)}))

(let [tabbed_pane (tabbed-pane :tab_placement "right")]
  (def selected-index 
    (fn [& _] (.getSelectedIndex (component tabbed_pane))))

  (def current-repl
    (>>> (&&& (output-arr tabbed_pane :content)
              selected-index)
         (fn [[coll index]]
           (nth coll index))))

  (def create-new-repl
    (>>> (&&& (output-arr tabbed_pane :content)
              init-new-repl)
         (fn [[items new_repl]] 
           ((input-arr tabbed_pane :content) (conj items new_repl))
           (.setSelectedComponent (component tabbed_pane) (:content new_repl)))))

  (def create-new-anonymous-repl
    (>>> (constantly {:name "anonymous" ::anonymous true})
         create-new-repl))

  (def close-current-repl
    (>>>
      (&&&
        (output-arr tabbed_pane :content)
        selected-index)
      (fn [[coll index]]
        (hutil/drop-nth index coll))
      (input-arr tabbed_pane :content)))

  (def repl-pane
    (component tabbed_pane)))
