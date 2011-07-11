(ns llama.document
  (:use (llama 
          [syntax :only [indent]] 
          [highlight :only [clj-highlight]])
        (Hafni.swing
          [utils :only [*available-fonts* color font]])
        [seesaw.invoke :only [invoke-later]]
        clj-arrow.arrow
        [clj-diff.core :only [diff]])
  (:require [hafni-seesaw.core :as hssw]
            [seesaw.core :as ssw])
  (:import javax.swing.text.AbstractDocument$DefaultDocumentEvent
           javax.swing.event.DocumentEvent$EventType))

(defn get-font []
  (font (first (filter #(case %
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

(defn apply-edit [jdoc edit]
  (if (string? (second edit))
      (.insertString jdoc (first edit) (second edit) nil)
      (.remove jdoc (first edit) (second edit))))

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

(defn create-highlight-fn [jtext_pane]
  (let [old_h (atom ())]
    (fn [& _]
      (invoke-later
        (let [h (clj-highlight 0 (.getText jtext_pane))
              new_h (apply concat (map rest (:+ (diff @old_h h))))]
          (println "Highlighting " (count new_h) " tokens")
          (println "Total tokens is " (count h))
          (dorun (map (hssw/input-arr jtext_pane :style)
                      new_h))
          (swap! old_h (constantly h)))))))

(defn create-doc [file]
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
;    (update-highlight)
    (assoc file :content (ssw/scrollable jtext_pane) 
                :component jtext_pane
                :manager manager)))

