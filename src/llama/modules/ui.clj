(ns llama.ui
  (:use (llama [config :only [get-option listen-to-option fire-ui-update]]))
  (:require [seesaw.core :as ssw])
  (:import javax.swing.UIManager
           org.fife.ui.rsyntaxtextarea.Token))

(defn init-l&f []
  (System/setProperty "apple.laf.useScreenMenuBar" "true")
  (listen-to-option :general :native-look?
    (fn [_ native-look?]
      (if native-look?
        (UIManager/setLookAndFeel (UIManager/getSystemLookAndFeelClassName))
        (UIManager/setLookAndFeel "com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel"))
      (fire-ui-update))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; editor

(def get-syntax-scheme
  (memoize
    #(org.fife.ui.rsyntaxtextarea.SyntaxScheme. true)))

(defn syntax-color-listener [sc t]
  (fn [_ [r g b]]
    (set! (. (nth (. sc styles) t) foreground) (java.awt.Color. r g b))))

(defn init-syntax-color-listeners [sc & id+ts]
  (doseq [[id t] id+ts]
    (listen-to-option :color id
      (syntax-color-listener sc t))))

(defn init-syntax-scheme []
  (let [sc (get-syntax-scheme)]
    (init-syntax-color-listeners sc
      [:comment-documentation Token/COMMENT_DOCUMENTATION]
      [:comment-EOL Token/COMMENT_EOL]
      [:comment-multiline Token/COMMENT_MULTILINE]
      [:data-type Token/DATA_TYPE]
      [:error-char Token/ERROR_CHAR]
      [:error-id Token/ERROR_IDENTIFIER]
      [:error-num-format Token/ERROR_NUMBER_FORMAT]
      [:error-string-double Token/ERROR_STRING_DOUBLE]
      [:function Token/FUNCTION]
      [:identifier Token/IDENTIFIER]
      [:literal-backquote Token/LITERAL_BACKQUOTE]
      [:literal-boolean Token/LITERAL_BOOLEAN]
      [:literal-char Token/LITERAL_CHAR]
      [:literal-number-decimal-int Token/LITERAL_NUMBER_DECIMAL_INT]
      [:literal-number-float Token/LITERAL_NUMBER_FLOAT]
      [:literal-number-hexadecimal Token/LITERAL_NUMBER_HEXADECIMAL]
      [:literal-string-double-quote Token/LITERAL_STRING_DOUBLE_QUOTE]
      [:null Token/NULL]
      [:operator Token/OPERATOR]
      [:preprocessor Token/PREPROCESSOR]
      [:reserved-word Token/RESERVED_WORD]
      [:separator Token/SEPARATOR]
      [:variable Token/VARIABLE])))

(defn init-ui []
  (init-l&f)
  (init-syntax-scheme)
  )

