# Getting started (1/2) #

 * Maven drives the presentation build
 * You can create a maven project skeleton with the dope-archetype:


```bash
    mvn archetype:generate -DarchetypeArtifactId=dope-archetype \
      -DarchetypeGroupId=com.nitorcreations -DgroupId=foo.bar \
      -DdeveloperName="Pasi Niemi" -DorganizationName="Nitor Creations" \
      -DinceptionYear=2013 -Dname="Baz Bar Awesomeness" \
      -Dtheme=light -DartifactId=baz-prez -DinteractiveMode=false
```

 * Parameters explained here: https://github.com/NitorCreations/dope-archetype

