(ns llama.document
  (:use (llama 
          [syntax :only [indent]] 
          [highlight :only [clj-highlight]])
        (Hafni.swing
          [utils :only [*available-fonts* color font]])
        [clojure.string :only [split]]
        [seesaw.invoke :only [invoke-later invoke-now]]
        clj-arrow.arrow
        [clj-diff.core :only [diff]])
  (:require [hafni-seesaw.core :as hssw]
            [seesaw.core :as ssw])
  (:import javax.swing.text.AbstractDocument$DefaultDocumentEvent
           javax.swing.event.DocumentEvent$EventType
           java.awt.RenderingHints))

(defn get-font []
  (font (first (filter #(case %
                 "Lucida Console" true
                 "Liberation Mono" true
                 "Courier" true
                 "Courier new" true
                 "Monospaced" true
                 nil) *available-fonts*)) 12))

(def *default-color* (color "black"))

(defn get-styles []
    [{:name "SEPARATOR"   :color (color "blue")}
     {:name "CORE-SYMBOL" :bold true}
     {:name "NEW-CLASS"   :italic true}
     {:name "IDENTIFIER"  :color *default-color*}
     {:name "NUMBER"      :color (color 158 116 237) :bold true}
     {:name "STRING"      :color (color 0 150 0)}
     {:name "COMMENT"     :color (color "light_gray")}
     {:name "KEYWORD"     :color (color 100 150 150)}
     {:name "DEFAULT"     :color *default-color*}
;;
     {:name "REGEX"       :color (color "red")}
     {:name "REGEX2"      :color *default-color*}
     {:name "WARNING"     :color (color "red")}
     {:name "ERROR"       :color (color "red")}])

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

(defn in-string? [text]
  (odd? (count (filter (partial = \") text))))

(comment
(defn create-highlight-fn [jtext_pane]
  (let [old_h (atom ())]
    (fn [& _]
      (invoke-later
        (let [t (.getText jtext_pane)
              pos (.getCaretPosition jtext_pane)
              terminator (if (in-string? (take (inc pos) t)) \" \newline)
              start 0
              end (+ pos 1
                     (count (take-while (partial not= terminator) (drop pos t))))
              h (clj-highlight start (.getText jtext_pane start end))
              new_h (apply concat (map rest (:+ (diff @old_h h))))]
;          (println "pos = " pos)
;          (prn "terminator = " terminator)
;          (println "Text to highlight: " (.getText jtext_pane start end))
          ;          (println "Highlighting " (count new_h) " tokens")
          ;          (println "Total tokens is " (count h))
          (dorun (map (hssw/input-arr jtext_pane :style) h))
          (swap! old_h (constantly h)))))))
)

(comment
(defn create-highlight-fn [jtext_pane]
  (let [old_h (atom ())]
    (fn [& _]
            (let [h (clj-highlight (.getText jtext_pane))
                  new_h (apply concat (map rest (:+ (diff @old_h h))))]
              ;          (println "Highlighting " (count new_h) " tokens")
              ;          (println "Total tokens is " (count h))
              (invoke-now 
                (dorun (map (hssw/input-arr jtext_pane :style)
                            new_h)))
              (swap! old_h (constantly h))))))

(defmulti create-doc (fn [file]
                       (if (:type file)
                         (:type file)
                         (if (:path file)
                           (last (split (:path file) #"\."))))))
(defmethod create-doc "clj" [file]
  (let [text (if (:path file) (slurp (:path file)) "")
        jtext_pane (javax.swing.JTextPane.)
        update-highlight (create-highlight-fn jtext_pane)
        pane (hssw/listen jtext_pane
                          :insert (fn [[index input]]
                                    (if (= input "\n")
                                      (let [text (.getText jtext_pane 0 index)]
                                        [index (indent text)])
                                      [index input]))
                          :inserted update-highlight
                          :removed update-highlight)
        manager (init-undoable-edits (.getDocument jtext_pane))]
    (hssw/config! jtext_pane :font (get-font) :styles (get-styles))
    (hssw/config! jtext_pane :text text) ; text must be added afterwards, since styles wont exist otherwise
    (ssw/listen jtext_pane :mouse-entered 
                (fn [_] (if (.isEditable jtext_pane)
                          (.requestFocusInWindow jtext_pane))))
    (assoc file :content (ssw/scrollable jtext_pane) 
                :text-pane jtext_pane
                :manager manager)))
)

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

(comment 
(defmethod create-doc "clj" [file]
  (let [text (if (:path file) (slurp (:path file)) "")
        area (proxy [org.fife.ui.rsyntaxtextarea.RSyntaxTextArea] []
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
    (.setSyntaxEditingStyle area (org.fife.ui.rsyntaxtextarea.SyntaxConstants/SYNTAX_STYLE_CLOJURE))
    (.setFont area (get-font))
    (.setText area text)
    (ssw/listen area :mouse-entered 
                (fn [_] (if (.isEditable area)
                          (.requestFocusInWindow area))))
    (assoc file :content content
                :text-pane area
                :manager manager)))

(defmethod create-doc :default [file]
  (let [text (if (:path file) (slurp (:path file)) "")
        jtext (javax.swing.JTextPane.)
        manager (init-undoable-edits (.getDocument jtext))]
    (hssw/config! jtext :text text)
    (assoc file :content (ssw/scrollable jtext)
                :text-pane jtext
                :manager manager)))
)
