dir=../../..

java -cp $dir/lib/clojure-1.2.1.jar clojure.main createcljflex.clj
./jflex*/bin/jflex clojure.flex
javac -classpath $dir/src/:$dir/classes/:./jflex* ClojureLexer.java
