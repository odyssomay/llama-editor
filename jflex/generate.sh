java -cp ../lib/clojure-1.2.1.jar clojure.main createcljflex.clj
./jflex*/bin/jflex clojure.flex
javac -classpath ../src/ ClojureLexer.java
mv ClojureLexer.class ../src/llama
