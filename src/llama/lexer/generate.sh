dir=../../..

java -cp $dir/lib/clojure-1.2.1.jar clojure.main createcljflex.clj
$dir/jflex/jflex*/bin/jflex clojure.flex
javac -classpath $dir/src/:$dir/classes/:$dir/jflex/jflex* ClojureLexer.java
