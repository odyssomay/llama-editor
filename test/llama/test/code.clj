(ns llama.test.code
  (:use llama.code
        [lazytest.describe :only [describe it testing]]))

(describe get-proxy-methods 
  (it "should return methods"
      (let [methods (get-proxy-methods (proxy [java.lang.Object] [] ))]
        (and (some #(= (first %) "toString") methods)
             (some #(= (first %) "hashCode") methods)))))

(describe arglist->string
  (it "should correctly transform to an arglist"
      (= (arglist->string ["class java.lang.Object" "int" "long"])
         "[^java.lang.Object arg0 ^int arg1 ^long arg2]"))
  (it "should create a valid arglist"
      (load-string (str "(fn " (arglist->string ["class java.lang.Object" "int" "long"]) ")"))))

(describe method->string
  (it "should create a valid method"
      (= (.trim (method->string [["test-fn" ["int" "long" "mumbo"]]]))
         "(test-fn [^int arg0 ^long arg1 ^mumbo arg2])")))

(describe methods->string
  (it "should return methods compitable with for example proxy"
      (load-string 
        (str "(proxy [java.lang.Object] []\n"
             (methods->string (get-proxy-methods (proxy [java.lang.Object] [])))
             ")"))))

(describe slamhound-text
  (it "should correctly transform a namespace declaration"
      (let [t (slamhound-text "(ns test-ns) (split \"hello\" #\"l\")")]
        (.contains t "clojure.string :only [split]"))))

