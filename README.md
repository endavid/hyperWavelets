HyperProc
=====================================================
(c) 2002 David Gavilan, Universitat Autonoma de Barcelona

How to build
---------------
You need the jakarta-ant tool:
http://jakarta.apache.org/site/binindex.html

Once installed, just cd to the directory where you installed this project
sources. There should be a file called 'build.xml'. Just type in the shell

> ant

and it should build the files.

To generate API documentation:

> ant doc


How to execute
-----------------

Once the sources are built, you just need to invoke the JVM like this:

> java -jar dist/HyperProject.jar

or just execute the batch file HP:

> ./HP



How to use contained classes
-----------------------------

Under linux, edit your .bash_profile file:


CLASSPATH=$CLASSPATH:$HOME/java/hyper-project/dist/HyperProject.jar:.

export CLASSPATH


Now you can import hyper.*;



What about "toys" ?
-------------------

Toys are some little tools to test classes separately. For instance,
CompressMe does the complete image compression process.




