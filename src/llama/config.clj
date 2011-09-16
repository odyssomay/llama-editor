(ns llama.config
  (:use [llama 
         [util :only [log *available-fonts* font find-i]]
         [state :only [defstate load-state]]])
  (:require [seesaw 
             [core :as ssw]
             [graphics :as ssw-graphics]
             [mig :as ssw-mig]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; options datastructure

(def options (atom {}))
(defstate :options #(deref options))

(defn get-option
  ([class-id]
   (get @options class-id))
  ([class-id id]
   (get-in @options [class-id id])))

(defn set-option [class-id id value]
  (swap! options assoc-in [class-id id] value))

(defn listen-to-option
  ([class-id f]
   (if-let [v (get-option class-id)]
     (f nil v))
   (add-watch options (gensym) 
     (fn [_ _ old-item item]
       (let [o (get old-item class-id)
             n (get item class-id)]
         (if-not (= o n)
           (f o n))))))
  ([class-id id f]
   (listen-to-option class-id
     (fn [old-item item]
       (let [o (get old-item id)
             n (get item id)]
         (if-not (= o n)
           (f o n)))))))

(let [listeners (atom [])]
  (defn listen-to-option-init* [f]
    (swap! listeners conj f))

  (defmacro listen-to-option-init [& body]
    `(listen-to-option-init* (fn [] ~@body)))

  (defn fire-option-init-listeners []
    (doseq [l @listeners]
      (l))))

(defn init-options []
  (let [saved (load-state :options)]
    (reset! options
;      (if saved saved
        {:general
         {:native-look? false
          :mouse-focus false}
         :color
         {:background [26 26 26]
          :text [0 0 0]
          ;; editor color
          :comment-documentation [0 0 0]
          :comment-EOL [153 153 153]
          :comment-multiline [0 0 0]
          :data-type [0 0 0]
          :error-char [0 0 0]
          :error-id [0 0 0]
          :error-num-format [0 0 0]
          :error-string-double [0 0 0]
          :function [0 153 0]
          :identifier [0 0 0]
          :literal-backquote [0 0 0]
          :literal-boolean [0 0 0]
          :literal-char [0 0 0]
          :literal-number-decimal-int [0 0 0]
          :literal-number-float [0 0 0]
          :literal-number-hexadecimal [0 0 0]
          :literal-string-double-quote [204 0 0]
          :operator [0 0 153]
          :preprocessor [0 0 0]
          :reserved-word [0 153 0]
          :separator [0 153 153]
          :variable [0 0 0]
          }
         :editor 
         {:wrap? false
          :numbering? true
          :highlight-line? false
          :monospaced? true
          :font (first (filter #(case %
                                  "Lucida Console" true
                                  "Liberation Mono" true
                                  "Courier" true
                                  "Courier new" true
                                  "Monospaced" true
                                  nil) *available-fonts*))
          :font-size 11
          :cursor-style "standard"
          }})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; utils

(defn checkbox [text class-id id]
  (let [c (ssw/checkbox :text text)]
    (listen-to-option-init
      (.setSelected c (get-option class-id id)))
    (ssw/listen c :selection
      (fn [& _] (set-option class-id id (.isSelected c))))
    c))

(defn combobox [choices class-id id]
  (let [cb (ssw/combobox :model choices)]
    (listen-to-option-init
      (.setSelectedItem cb (get-option class-id id)))
    (ssw/listen cb :selection
      (fn [& _] (set-option class-id id (.getSelectedItem cb))))
    cb))

(defn number-spinner [init-val minimum maximum class-id id]
  (let [m (javax.swing.SpinnerNumberModel. init-val minimum maximum 1)
        s (javax.swing.JSpinner. m)]
    (-> s
      .getEditor
      .getTextField
      (.setColumns 7))
    (listen-to-option-init
      (.setValue m (get-option class-id id)))
    (.addChangeListener m
      (reify javax.swing.event.ChangeListener
        (stateChanged [_ _]
          (set-option class-id id (.getValue m)))))
    s))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; general

(defn general-options []
  (let [p (ssw-mig/mig-panel :items [[(checkbox "use native look" :general :native-look?) "wrap"]
                                     [(checkbox "mouse focus" :general :mouse-focus) "wrap"]])]
    p))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; colors

(let [components (atom [])]
  (defn listen-to-ui-update [component]
    (swap! components conj component))

  (defn fire-ui-update []
    (doseq [c @components]
      (javax.swing.SwingUtilities/updateComponentTreeUI c))))

(defn color-preview-component [id]
  (let [arc-width 5
        arc-height 5
        width 70
        height 20
        inset 2
        cc (javax.swing.JColorChooser.)
        component
        (proxy [javax.swing.JComponent] []
          (paintComponent [g]
            (let [gc (.create g)
                  b (.getClipBounds g)]
              (ssw-graphics/anti-alias gc)
              (.setColor gc (.getColor cc))
              (.fillRoundRect gc 0 0 width height arc-width arc-height)))
          (getPreferredSize []
            (java.awt.Dimension. width height))
          (getMinimumSize [] (.getPreferredSize this))
          (getMaximumSize [] (.getPreferredSize this)))
        frame (ssw/frame :content cc)]
    (.addChangeListener (.getSelectionModel cc)
      (reify javax.swing.event.ChangeListener
        (stateChanged [_ _]
          (let [color (.getColor cc)]
            (set-option :color id [(.getRed color) (.getGreen color) (.getBlue color)])
            (.repaint component)))))
    (listen-to-option-init
      (if-let [[r g b] (get-option :color id)]
        (.setColor cc (java.awt.Color. r g b))))
    (ssw/listen component :mouse-clicked
      (fn [& _] (-> frame ssw/pack! ssw/show!)))
    (listen-to-ui-update cc)
    component))

(defn generate-color-options [& options]
  (apply concat
    (for [[[name1 id1] [name2 id2]] (partition 2 2 (repeat ["" nil]) options)]
      [[name1] 
       [(color-preview-component id1) "grow 0"]
       [(javax.swing.Box/createHorizontalGlue) "pushx"]
       [name2]
       (if id2
         [(color-preview-component id2) "grow 0, wrap"]
         ["" "wrap"])]
      )))

(defn color-options []
  (ssw-mig/mig-panel :constraints ["fillx" "" ""]
    :items 
    (concat [["ui" "span"]]
            (generate-color-options 
              ["background" :background])
            [["editor"] [(javax.swing.JSeparator.) "span"]]
            (generate-color-options
              ["Comment documentation" :comment-documentation]
              ["Comment EOL" :comment-EOL]
              ["Comment multiline" :comment-multiline]
              ["Data type" :data-type]
              ["Error char" :error-char]
              ["Error identifier" :error-id]
              ["Error number format" :error-num-format]
              ["Error string double" :error-string-double]
              ["Function" :function]
              ["Identifier" :identifier]
              ["Literal backquote" :literal-backquote]
              ["Literal boolean" :literal-boolean]
              ["Literal char" :literal-char]
              ["Literal number decimal int" :literal-number-decimal-int]
              ["Literal number float" :literal-number-float]
              ["Literal number hexadecimal" :literal-number-hexadecimal]
              ["Literal string double quote" :literal-string-double-quote]
;              ["Null" :null]
              ["Operator" :operator]
              ["Preprocessor" :preprocessor]
              ["Reserved word" :reserved-word]
              ["Separator" :separator]
              ["Variable" :variable]))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; editor

(defn is-monospaced? [font-name]
  (let [fm (.getFontMetrics (ssw/label) (java.awt.Font. font-name java.awt.Font/PLAIN 10))]
    (= (.charWidth fm \M) (.charWidth fm \i) (.charWidth fm \.))))

(defn editor-options []
  (ssw-mig/mig-panel :constraints ["fillx" "" ""]
    :items [["View"]                                  
              ["Font" "wrap"]
            [(checkbox "text wrapping" :editor :wrap?)] 
              [(checkbox "monospaced" :editor :monospaced?) "wrap"]
            [(checkbox "line numbering" :editor :numbering?)] 
              [(let [cb (combobox *available-fonts* :editor :font)]
                 (listen-to-option :editor :monospaced?
                                   (fn [_ item]
                                     (let [selected (.getSelectedItem cb)]
                                       (ssw/config! cb :model
                                         (filter (if item is-monospaced? identity) *available-fonts*))
                                       (.setSelectedItem cb selected))))
                 cb) "wrap"]
            [(checkbox "highlight current line" :editor :highlight-line?)] 
              ["size" "split 2"] [(number-spinner 10 1 20 :editor :font-size) "wrap"]
            ["cursor style" "split 2"] [(combobox ["standard" "thick" "underline" 
                                                   "block" "block outline"] 
                                                  :editor :cursor-style)]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; options UI

(defn options-panel []
  (let [tp (ssw/tabbed-panel 
             :tabs [{:title "general" :content (general-options)}
                    {:title "color" :content (color-options)}
                    {:title "editor" :content (editor-options)}])]
    (listen-to-ui-update tp)
    tp))

(def options-dialog
  (memoize 
    (fn [] 
      (let [f (ssw/frame :content (options-panel) :size [700 :by 300] :title "Preferences")]
        (fire-option-init-listeners)
        f))))

;(listen-to-option :color (fn [_ _] (.updateUI (options-panel))))

(defn show-options-dialog []
  (-> (options-dialog) ssw/show!))
