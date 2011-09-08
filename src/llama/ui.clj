(ns llama.ui
  (:use (llama [config :only [get-option listen-to-option fire-ui-update]]))
  (:require [seesaw.core :as ssw])
  (:import javax.swing.UIManager))

(defn init-l&f []
  (System/setProperty "apple.laf.useScreenMenuBar" "true")
  (listen-to-option :general :native-look?
    (fn [_ native-look?]
      (if native-look?
        (UIManager/setLookAndFeel (UIManager/getSystemLookAndFeelClassName))
        (UIManager/setLookAndFeel "com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel"))
      (fire-ui-update))))

(defn init-ui []
  (init-l&f))
