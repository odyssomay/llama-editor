(ns llama.code
  (:use [clojure.string :only (split)]
        [slam.hound 
         [asplode :only [asplode]]
         [regrow :only [regrow]]
         [stitch :only [stitch-up]]])
  (:import (javax.swing JOptionPane)))

(defn get-methods [& objects]
  "Get the methods from a proxy that is created with objects."
  (remove #(case (first %)
		 "__initClojureFnMappings" true
		 "__updateClojureFnMappings" true
		 "__getClojureFnMappings" true
		 false)
	  (map (fn [x] [(.getName x)
			(map #(.toString %) (.getParameterTypes x))])
	       (.getMethods (apply get-proxy-class objects)))))

(defn proxy-dialog []
  (let [objects (JOptionPane/showInputDialog "Input interfaces/abstract class/class")
	methods (load-string (str "(get-methods " objects ")"))]
    (str "(proxy [" objects "] []"
	 (apply str
		(map #(str "\n(" (first %) " ["
			   (apply str (interleave
				       (map (fn [x]
					      (str "^" (last (split x #" "))))
					    (second %))
				       (repeat " par")
				       (iterate inc 1)
				       (repeat " ")))
			   "] )")
		     methods))
	 ")")))

(defn slamhound-text [text]
  (-> (java.io.StringReader. text)
      asplode
      regrow
      stitch-up))
