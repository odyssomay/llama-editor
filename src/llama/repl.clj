(ns llama.repl
  (:use (llama [document :only [create-doc]]
               [syntax :only [parens-count]]
;               [lib :only [
                )
        clj-arrow.arrow
        (Hafni.swing component container)
 	[clojure.string :only (split join)])
  (:require [clojure.java.io :as cio]
            [seesaw.core :as ssw]
            llama.leiningen.repl
            (Hafni [utils :as hutil]))
  (:import 
    (javax.swing JTextArea)
    (java.awt event.KeyListener)))

(defn stop-repl [{:keys [thread input_stream output_stream]}]
  (.interrupt thread)
  (.close input_stream)
  (.close output_stream))

(defn init-repl-listener [repl jtext_pane]
  (let [stream (:input_stream repl)
        jdoc (.getDocument jtext_pane)
        text (atom "")]
    (.start (Thread. (fn []
                       (swap! text str (-> stream .read char str))
                       (when (not (.ready stream))
                         (ssw/invoke-now 
                           (Thread/sleep 50) ;; Ugly but effective, the problem is that 
                                             ;; the insertion of the new repls text is faster
                                             ;; than the newline which the user inputs
                           (.insertString jdoc (.getLength jdoc) @text nil)
                           (.setCaretPosition jtext_pane (.getLength jdoc)))
                         (reset! text ""))
                       (recur))))))
;  (.start (Thread. (fn []
;                     (send repl_agent error (.read (:error_stream @repl_agent)))))))

(defn send-to-repl [repl text]
  (.write (:output_stream repl) text)
  (.flush (:output_stream repl)))

(defn repl-key-listener [jdoc repl]
  (proxy [KeyListener] []
    (keyPressed [e]
      (if (= (.getKeyChar e) \newline)
	(let [text (.getText jdoc 0 (.getLength jdoc))]
	  (if (= (parens-count text) 0)
            (send-to-repl repl (join [(last (split text #"=>")) "\n"]))))))
    (keyReleased [e] )
    (keyTyped [e] )))

(defn init-new-repl [project]
  (let [document (create-doc {})
        repl (llama.leiningen.repl/init-repl-server project 6000)
        err_text (JTextArea.)
        out_text (JTextArea.)
        jtext_pane (component (:component document))
        jdoc (.getStyledDocument jtext_pane)
        tabbed_pane (tabbed-pane :content [{:content (:content document) :title "repl"}
                                           {:content (ssw/scrollable out_text) :title "out"}
                                           {:content (ssw/scrollable err_text) :title "err"}])]
    (init-repl-listener repl jtext_pane)
    (.addKeyListener jtext_pane (repl-key-listener jdoc repl))

    (.setEditable err_text false)
    (.setEditable out_text false)
    {:content tabbed_pane :title (:name project)}))

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
