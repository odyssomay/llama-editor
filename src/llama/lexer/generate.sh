dir=../../..

java -cp $dir/lib/clojure-1.2.1.jar clojure.main createcljflex.clj
jflex clojure.flex
javac -classpath $dir/src/:$dir/classes/ ClojureLexer.java
