(ns llama.document
  (:use (llama 
          [syntax :only [indent]] 
          [lib :only [*available-fonts* color font log]])
        [clojure.string :only [split]]
        [seesaw.invoke :only [invoke-later invoke-now]])
  (:require [seesaw.core :as ssw])
  (:import javax.swing.text.AbstractDocument$DefaultDocumentEvent
           javax.swing.event.DocumentEvent$EventType
           java.awt.RenderingHints))

(log :trace "started loading")

(defn get-font []
  (font (first (filter #(case %
                 "Lucida Console" true
                 "Liberation Mono" true
                 "Courier" true
                 "Courier new" true
                 "Monospaced" true
                 nil) *available-fonts*)) 12))

(defn init-undoable-edits [jdoc]
  (let [manager (javax.swing.undo.UndoManager.)
        listener (reify javax.swing.event.UndoableEditListener
                   (undoableEditHappened [this e]
                                         (let [edit (.getEdit e)]
                                           (if-not (and ;(= (class e) AbstractDocument$DefaultDocumentEvent)
                                                        (= (.getType edit) DocumentEvent$EventType/CHANGE))
                                             (.addEdit manager edit) 
                                             ))))]
    (.addUndoableEditListener jdoc listener)
    manager))

(def clojure-provider (org.fife.ui.autocomplete.DefaultCompletionProvider.))

(defmacro core-symbols+metadata []
  (let [symbols (keys (ns-publics 'clojure.core))]
    (vec
      (for [s symbols]
        `(vec ['~s (meta (var ~s))])))))

(defn to-html [string]
  (if (string? string)
    (-> string
      (.replaceAll "<" "<&lt;>")
      (.replaceAll ">" "<&gt;>")
      (.replaceAll "\n" "<br/>"))
    string))

(defn init-clojure-completion-provider []
  (doseq [completion (for [[s m] (core-symbols+metadata)]
                       (let [c (org.fife.ui.autocomplete.FunctionCompletion.
                                 clojure-provider (str s) (str (:arglists m)))]
                         (.setShortDescription c (str "<html>" (to-html (:doc m))))
                         c))]
    (.addCompletion clojure-provider completion)))

(init-clojure-completion-provider)

(.addCompletion clojure-provider
  (org.fife.ui.autocomplete.BasicCompletion. clojure-provider "def"))

(defmacro syntax-style [style]
  (symbol (str "org.fife.ui.rsyntaxtextarea.SyntaxConstants/SYNTAX_STYLE_" style)))

(defn type->syntax-style [type]
  (case type
    "clj" (syntax-style "CLOJURE")
    "java" (syntax-style "JAVA")
    nil))

(defn set-syntax-style [area type]
  (.setSyntaxEditingStyle area (type->syntax-style type)))

(defn create-clojure-document []
  (proxy [org.fife.ui.rsyntaxtextarea.RSyntaxDocument] [nil]
    (insertString [offset text a]
      (let [indented_text 
            (if (= text "\n")
              (indent (.getText this 0 offset))
              text)]
        (proxy-super insertString offset indented_text a)))))

(defn create-text-area [file]
  (let [text (if (:path file) (slurp (:path file)) "")
        type (cond (:type file) (:type file)
                   (:path file) (last (split (:path file) #"\."))
                   true nil)
        document (if (= type "clj") (create-clojure-document) (org.fife.ui.rsyntaxtextarea.RSyntaxDocument. nil))
        area (proxy [org.fife.ui.rsyntaxtextarea.RSyntaxTextArea] [document]
               (paintComponent [g]
                 (doto g 
                   (.setRenderingHint RenderingHints/KEY_ANTIALIASING 
                                      RenderingHints/VALUE_ANTIALIAS_ON)
                   (.setRenderingHint RenderingHints/KEY_TEXT_ANTIALIASING 
                                      RenderingHints/VALUE_TEXT_ANTIALIAS_ON)
                   (.setRenderingHint RenderingHints/KEY_RENDERING
                                      RenderingHints/VALUE_RENDER_QUALITY))
                 (proxy-super paintComponent g)))
        content (ssw/scrollable area)
        manager (init-undoable-edits (.getDocument area))]
    (when (= type "clj")
      (.setAutoIndentEnabled area false)
      (let [ac (org.fife.ui.autocomplete.AutoCompletion. clojure-provider)]
        (.setShowDescWindow ac true)
        (.install ac area)))
    (set-syntax-style area type) 
    (.setFont area (get-font))
    (.setText area text)
    (ssw/listen area :mouse-entered 
                (fn [_] (if (.isEditable area)
                          (.requestFocusInWindow area))))
    (assoc file :content content
                :text-pane area
                :manager manager)))

(log :trace "finished loading")
