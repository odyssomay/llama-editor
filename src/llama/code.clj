(ns llama.code
  (:use [clojure.string :only (split join)]
        [slam.hound 
         [asplode :only [asplode]]
         [regrow :only [regrow]]
         [stitch :only [stitch-up]]])
  (:import (javax.swing JOptionPane)))

(defn get-proxy-methods  
  "Get the methods from a proxy." 
  [proxy]
  (remove #(case (first %)
		 "__initClojureFnMappings" true
		 "__updateClojureFnMappings" true
		 "__getClojureFnMappings" true
		 false)
	  (map (fn [x] [(.getName x)
			(map #(.toString %) (.getParameterTypes x))])
	       (.getMethods (.getClass proxy)))))

(defn arglist->string [arglist]
  (str "["
       (apply str
              (join " "
                    (map-indexed (fn [index item]
                                   (str "^" (last (split item #" "))
                                        " arg" index))
                                 arglist)))
       "]"))

(defn method->string [method]
  (str "(" (ffirst method) " "
       (case (count method)
         1 (arglist->string (last (first method)))
         (apply str  
                (for [arglist (map last method)]
                  (str "\n(" (arglist->string arglist) ")"))))
       ")\n"))

(defn methods->string [methods]
  (apply str
         (map method->string (partition-by first methods))))

(defn proxy-dialog []
  (let [objects (JOptionPane/showInputDialog "Input interfaces/abstract class/class")
	methods (load-string (str "(get-proxy-methods " objects ")"))]
    (str "(proxy [" objects "] []"
         (methods->string methods)
	 	 ")")))

(defn slamhound-text [text]
  (-> (java.io.StringReader. text)
      asplode
      regrow
      stitch-up))
