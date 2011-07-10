(ns llama.document
  (:use (llama 
          [syntax :only [indent]] 
          [highlight :only [clj-highlight]])
        (Hafni.swing
          [utils :only [*available-fonts* color font]])
        clj-arrow.arrow)
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
    [{:name "OPERATOR"   :color (color "gray")}
     {:name "KEYWORD"    :bold true};:color (color "blue")}
     {:name "KEYWORD2"   :bold true};:color (color "blue")}
     {:name "IDENTIFIER" :color *default-color*}
     {:name "NUMBER"     :color (color 158 116 237)}
     {:name "STRING"     :color (color 0 150 0)}
     {:name "STRING2"    :color *default-color*}
     {:name "COMMENT"    :color (color "light_gray")}
     {:name "COMMENT2"   :color *default-color*}
     {:name "REGEX"      :color (color "red")}
     {:name "REGEX2"     :color *default-color*}
     {:name "TYPE"       :color *default-color*}
     {:name "TYPE2"      :color *default-color*}
     {:name "TYPE3"      :color *default-color*}
     {:name "DEFAULT"    :color *default-color*}
     {:name "WARNING"    :color (color "red")}
     {:name "ERROR"      :color (color "red")}])

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

(defrecord Edit_add [offset text length])

(defrecord Edit_rem [offset text length])

(defn apply-edit [jdoc edit]
  (if (string? (second edit))
      (.insertString jdoc (first edit) (second edit) nil)
      (.remove jdoc (first edit) (second edit))))

(defn create-doc [file]
  (let [text (if (:path file) (slurp (:path file)) "") ; was file_content
        ;text (apply str (interpose "\n" file_content))
;        c (text-pane :text text
;                     :font (get-font) :styles (get-styles))
        jtext_pane (javax.swing.JTextPane.) ;(component c)
        update-highlight (fn [& _] 
                           (dorun (map (hssw/input-arr jtext_pane :style) 
                                       (clj-highlight 0 (.getText jtext_pane)))))
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
    (hssw/config! jtext_pane :text text) ; text must be added afterwards, since styles otherwise don't exist
;    (update-highlight)
    (assoc file :content (ssw/scrollable jtext_pane) 
                :component jtext_pane
                :manager manager)))

