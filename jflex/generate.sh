./jflex*/bin/jflex clojure.flex
javac -classpath ../src/ ClojureLexer.java
mv ClojureLexer.class ../src/llama
