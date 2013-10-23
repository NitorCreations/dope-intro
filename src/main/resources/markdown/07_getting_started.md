# Getting started (2/2) #

 * Add you slides in ```src/main/resources/markdown```
 * JavaFX needs to be on your JDK classpath
    * You may need to run ```mvn com.zenjava:javafx-maven-plugin:2.0:fix-classpath```
    * One time only operation per JDK
    * Java 8 has this sorted
 * The project contains all of the runtime java code
 * You can change all runtime behaviour like:
     * controllers
     * http server
     * animations
