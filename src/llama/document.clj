(ns llama.document
  (:use (llama 
          [config :only [listen-to-option get-option]]
          [syntax :only [indent parens-count find-unmatched-rparens]] 
          [util :only [*available-fonts* color font log]])
        [clojure.string :only [split split-lines join]]
        [seesaw.invoke :only [invoke-later invoke-now]]
        [seesaw.graphics :only [anti-alias style draw polygon circle rect]])
  (:require [seesaw.core :as ssw])
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

;; undo/redo

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

;; autocompletion

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

;; highlighting

(defmacro syntax-style [style]
  (symbol (str "org.fife.ui.rsyntaxtextarea.SyntaxConstants/SYNTAX_STYLE_" style)))

(defn type->syntax-style [type]
  (case type
    "clj" (syntax-style "CLOJURE")
    "java" (syntax-style "JAVA")
    (syntax-style "NONE")))

(defn set-syntax-style [area type]
  (.setSyntaxEditingStyle area (type->syntax-style type)))

;; code folding

(defn get-viewport-bounds [viewport line-height]
  (let [start-pos (.y (.getViewPosition viewport))
        start-line (quot start-pos line-height)
        number-of-lines (quot (.height (.getExtentSize viewport))
                              line-height)]
    [start-line (+ number-of-lines start-line)]))

(defn draw-block [g line-height state start end]
  (let [b (.getClipBounds g)
        y-start (* line-height start)]
    (.setColor g (case state
                   :open (color 200 0 0)
                   :close (color 0 200 0)))
    (.fillRect g (.x b)     (* line-height start)
                 (.width b) (* line-height (- end start)))
    ;(draw g (polygon [(+ (.x b) 1) (inc y-start)] [(+ (.x b) 8) (inc y-start)] [5 (+ y-start 10)])
    ;      (style :foreground (color 255 255 255)))
;    (draw g (case state
;              :open (circle (+ (.x b) 5) (+ y-start 5) 3.5)
;              :close (rect (inc (.x b)) (inc y-start) 7 7)
;              nil) (style :foreground (color 255 255 255)
;                          :background (color 255 255 255)))
    ))

(defn get-blocks [text]
  (->> text
       split-lines
       (map parens-count)
       (reductions +)
       (partition-by zero?)))

(defn get-block-offsets [text]
  (let [blocks (get-blocks text)]
    (partition 2
      (reductions + (map count (if (= (ffirst blocks) 0)
                                   blocks
                                   (concat [()] blocks)))))))

(defn init-fold-component [scroll_pane text_area]
  (let [get-line-height #(.getHeight (.getFontMetrics text_area (.getFont text_area)))
        c (proxy [javax.swing.JComponent] []
            (getPreferredSize []
              (java.awt.Dimension. 10 (.height (.getPreferredSize text_area))))
            (paintComponent [g]
              (anti-alias g)
              (let [b (.getClipBounds g)
                    line_height (get-line-height)
                    [start-line end-line] (get-viewport-bounds (.getViewport scroll_pane) line_height)]
                (.setColor g (color 0 0 0))
                (.fillRect g (.x b) (.y b) (.width b) (.height b))
                (doseq [[start end] (filter (fn [[start end]]
                                              (and (<= start end-line)
                                                   (>= end start-line))) 
                                            (get-block-offsets (.getText text_area)))]
                  (draw-block g line_height :open start (inc end)))
                (doseq [line (.getFoldedLines (.getDocument text_area))]
                  (draw-block g line_height :close line (inc line))))))]
    (.addMouseListener c
      (proxy [java.awt.event.MouseAdapter] []
        (mouseClicked [e]
          (let [clicked-line (quot (.getY e) (get-line-height))
                line (some (fn [[start end]]
                             (if (and (>= clicked-line start)
                                      (<= clicked-line end))
                               start)) (get-block-offsets (.getText text_area)))
                d (.getDocument text_area)]
            (if line
              (.fold d line)
              (if (some (partial = clicked-line) (.getFoldedLines d))
                (.unfold d clicked-line)))))))
    c))

(defn get-line-offset [text line]
  (->> text
       (reductions #(if (= %2 \newline) (inc %1) %1) 0)
       (take-while #(< % line))
       count))

(defn get-line-for-offset [text offset]
  (count (filter (partial = \newline) (take offset text))))

(defn update-folded-text [folded-text offset line-difference full-text]
  (let [line (get-line-for-offset full-text offset)]
    (map (fn [fold]
           (if (> (:line fold) line)
             (assoc fold :line (+ (:line fold) line-difference))
             fold))
         folded-text)))

;; text

(defprotocol FoldableDocument
  (getFoldedLines [this])
  (fold           [this line])
  (unfold         [this line]))

(defn file-type [obj]
  (cond (:type obj) (:type obj)
        (:path obj) (last (split (:path obj) #"\."))
        :else nil))

(defmulti text-model file-type)

(defmethod text-model "clj" [obj]
  (let [folded-text (atom [])
        document (proxy [org.fife.ui.rsyntaxtextarea.RSyntaxDocument llama.document.FoldableDocument] [nil]
                   (insertString [offset text a]
                     (let [indented_text 
                           (if (= text "\n")
                             (indent (.getText this 0 offset))
                             text)]
                       (swap! folded-text 
                              update-folded-text offset 
                              (count (filter (partial = \newline) text))
                              (.getText this 0 (.getLength this)))
                       (proxy-super insertString offset indented_text a)))
                   (remove [offset length]
                     (swap! folded-text
                            update-folded-text offset
                            (- (count (filter (partial = \newline) (.getText this offset length))))
                            (.getText this 0 (.getLength this)))
                     (proxy-super remove offset length))
                   ;; folding
                   (getFoldedLines [] (map :line @folded-text))
                   (fold [line]
                     (let [text (.getText this 0 (.getLength this))
                           start (get-line-offset text line)
                           subtext (subs text start)
                           end (+ 2 (find-unmatched-rparens (rest subtext)))
                           block (subs subtext 0 end)]
                       (.remove this start end)
                       (.insertString this start (str (first (split-lines subtext)) " ... )") nil)
                       (swap! folded-text conj {:line line :text block}))) 
                   (unfold [line] 
                     (let [text (.getText this 0 (.getLength this))
                           start (get-line-offset text line)
                           subtext (subs text start)
                           end (+ 2 (find-unmatched-rparens (rest subtext)))]
                       (.remove this start end)
                       (.insertString this start (:text (first (filter #(= (:line %) line) @folded-text))) nil)
                       (swap! folded-text (fn [state] (remove #(= (:line %) line) state))))))]
    (let [text (if (:path obj) (slurp (:path obj)) "")]
      (.insertString document 0 text nil))
    document))

(defmethod text-model :default [obj]
  (let [d (org.fife.ui.rsyntaxtextarea.RSyntaxDocument. nil)]
    (let [text (if (:path obj) (slurp (:path obj)) "")]
      (.insertString d 0 text nil))
    d))

(defn text-delegate [obj]
  (let [text (if (:path obj) (slurp (:path obj)) "")
        type (file-type obj)
        document (if (:model obj) 
                   (:model obj) (text-model obj))
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
        content (org.fife.ui.rtextarea.RTextScrollPane. area true)
        manager (init-undoable-edits (.getDocument area))]
    (when (= type "clj")
      (.setAutoIndentEnabled area false)
      (let [ac (org.fife.ui.autocomplete.AutoCompletion. clojure-provider)]
        (.setShowDescWindow ac true)
        (.install ac area))
;      (let [fold-component (init-fold-component content area)]
;        (.setRowHeaderView content fold-component)
;        (.addDocumentListener document
;          (reify javax.swing.event.DocumentListener
;            (changedUpdate [_ _]
;              (.repaint fold-component))
;            (insertUpdate [_ _] )
;            (removeUpdate [_ _] ))))
      )
    (set-syntax-style area type) 
    (listen-to-option :editor :font
      (fn [_ font-name] (.setFont area (java.awt.Font. font-name java.awt.Font/PLAIN 
                                                       (get-option :editor :font-size)))))
    (listen-to-option :editor :font-size
      (fn [_ size] (.setFont area (java.awt.Font. (get-option :editor :font) java.awt.Font/PLAIN
                                                  size))))
    (listen-to-option :editor :numbering?
      (fn [_ enabled?] (.setLineNumbersEnabled content enabled?)))
    (listen-to-option :editor :highlight-line?
      (fn [_ enabled?] (.setHighlightCurrentLine area enabled?)))
    (listen-to-option :editor :wrap?
      (fn [_ enabled?] (.setLineWrap area enabled?)))
    (ssw/listen area :mouse-moved 
                (fn [_] (if (and (.isEditable area)
                                 (get-option :general :mouse-focus))
                          (.requestFocusInWindow area))))
    (assoc obj :content content
               :text-pane area
               :manager manager)))
