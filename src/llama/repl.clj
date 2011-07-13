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
  (let [classpath (lein-classpath/get-classpath-string project)
        repl_path (.getPath (ClassLoader/getSystemResource "repl.clj"))]
    (start-process (str "java -cp " classpath " clojure.main " repl_path) )))

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
                         (catch java.lang.IllegalArgumentException _ 
                           ; this means that the stream is closed
                           )))))))

(defn send-to-repl [repl text]
  (.write (:output_stream repl) text)
  (.flush (:output_stream repl)))

(comment
(defn repl-key-listener [jtext_pane repl]
  (let [jdoc (.getStyledDocument jtext_pane)
        state (atom {:pos 0})
        update-state! (fn [field f] 
                       (swap! state assoc field (f (get @state field))))
        reset-state! (fn [field value]
                       (swap! state assoc field value))
        update-caret! (fn []
                       (if (:text @state)
                         (let [pos (+ (:start @state) (:pos @state))]
                           (if-not (= (.getCaretPosition jtext_pane) pos)
                             (.setCaretPosition jtext_pane pos)))
                         (if-not (= (.getCaretPosition jtext_pane) (.getLength jdoc))
                           (.setCaretPosition jtext_pane (.getLength jdoc)))))]
    (.addCaretListener jtext_pane
                       (reify javax.swing.event.CaretListener
                         (caretUpdate [_ _] (update-caret!)))) 
    (proxy [KeyListener] []
      (keyPressed 
        [e]
        (let [s @state]
          (update-caret!)
          (condp = (.getKeyCode e)
            KeyEvent/VK_LEFT (update-state! :pos #(max 0 (dec %)))
            KeyEvent/VK_RIGHT (update-state! :pos #(min (count (:text @state)) (inc %)))
            KeyEvent/VK_UP (update-state! :history-pos #(min (inc %) (count (:history @state))))
            KeyEvent/VK_DOWN (update-state! :history-pos #(max (dec %) 0))
            nil)))
      (keyReleased [e] 
                   (update-caret!)
;                   (println "pos = " (:pos @state))
;                   (println "text = " (:text @state))
                   )
      (keyTyped 
        [e]
        (let [c (.getKeyChar e)
              s @state
              text (:text s)]
          (if (= c \newline)
            (when (and text (zero? (parens-count text)))
              (.insertString jdoc (.getLength jdoc) "\n" nil)
              (send-to-repl repl (str text "\n"))
              (reset-state! :text nil))
            (do
              (if (not text)
                (swap! state assoc :start (.getLength jdoc) 
                                   :pos 0))
              (.insertString jdoc (+ (:start @state) (:pos @state)) (str c) nil)
              (update-state! :text #(let [text (if % % "")]
                                      (str (.substring text 0 (:pos @state))
                                           c (.substring text (:pos @state)))))
              (update-state! :pos inc)))))
      )))
)

(defn init-repl-input-field [repl repl_pane]
  (let [input_field (:component (create-doc {}))
        jdoc (.getDocument repl_pane) 
        set-height (fn [rows] 
                     (println "yep"))
        get-height (fn [] 1)
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
                (if (zero? (parens-count text))
                  (do
                    (reset! reset-text? true)
                    (.insertString jdoc (.getLength jdoc) text nil) 
                    (send-to-repl repl text))
                  (set-height (inc (get-height))))))))
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
        jtext_pane (:component (create-doc {}))
        err_text (javax.swing.JTextArea.) 
        input_panel (ssw/border-panel) 
        repl_panel (javax.swing.JPanel. (java.awt.CardLayout.))
        button_panel 
        (ssw/vertical-panel 
          :items 
          [(ssw/action :icon (get-icon-url "gnome_terminal.png")
;                       :tip "Show the input 
                       :handler (fn [_] (.show (.getLayout repl_panel) repl_panel "input-panel")))
           (ssw/action :icon (get-icon-url "gnome_dialog_warning.png")
                       :tip "Show the error output of this repl."
                       :handler (fn [_] (.show (.getLayout repl_panel) repl_panel "error-panel")))
           (ssw/action :icon (get-icon-url "gnome_view_refresh.png")
                       :handler (fn [_]
                                  (stop-repl @repl)
                                  (reset! repl (start-repl project))
                                  (init-repl-panels @repl jtext_pane err_text input_panel)
                                  (.updateUI input_panel)))
           (ssw/action :icon (get-icon-url "gnome_process_stop.png")
                       :tip "Destroy the repl process and close this repl's window."
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

  (defn create-new-repl-arr [project]
    (>>> (output-arr tabbed_pane :content)
         #(conj % (init-new-repl project))
         (input-arr tabbed_pane :content)))

  (def create-new-repl
    (>>> (&&& (output-arr tabbed_pane :content)
              init-new-repl)
         #(conj (first %) (second %))
         (input-arr tabbed_pane :content)))

  (def create-new-anonymous-repl
    (fn [_] ))
;    (create-new-repl-arr "repl" []))

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
