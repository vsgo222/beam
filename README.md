# BEAM

[![Build Status](https://beam-ci.tk/job/master/badge/icon)](https://beam-ci.tk/job/master/)

[![Documentation Status](https://readthedocs.org/projects/beam/badge/?version=latest)](http://beam.readthedocs.io/en/latest/?badge=latest)

BEAM stands for Behavior, Energy, Autonomy, and Mobility. The model is being developed as a framework for a series of research studies in sustainable transportation at Lawrence Berkeley National Laboratory.  

BEAM extend the [Multi-Agent Transportation Simulation Framework](https://github.com/matsim-org/matsim) (MATSim) to enable powerful and scalable analysis of urban transportation systems.

[Read more about BEAM](http://beam.readthedocs.io/en/latest/about.html) 

or 

try running BEAM with our simple [getting started guide](http://beam.readthedocs.io/en/latest/users.html#getting-started) 

or  

check out the [developer guide](http://beam.readthedocs.io/en/latest/developers.html) to get serious about power using or contributing.

## Project website: 
http://beam.lbl.gov/


## Building BEAM as a JAR through Gradle:
1. Once your BEAM project is running on an IDE like IntelliJ, go to your build.gradle file.
2. We're going to build a fatJar, which is an executable file that includes all of the dependencies in the jar. 
3. We will need an additional plugin for creating the fatJar. In build.gradle, put this plugin into the plugins section:
[id "com.github.johnrengelman.shadow" version "5.2.0"] // Shadow helps create fatJars

Your plugins should look similar to this: 

plugins {

    id "net.ltgt.apt" version "0.5"
    
    id "de.undercouch.download" version "3.2.0"
    
    id "org.scoverage" version "2.5.0"
    
    id 'maven-publish'
    
    id "me.champeau.gradle.jmh" version "0.5.0"
    
    id "com.github.johnrengelman.shadow" version "5.2.0" // Shadow helps create fatJars

}

4. In the same build.gradle file, scroll down to under tasks.withType(scalaCompile){}
5. Copy and paste this under tasks.withType(scalaCompile) [not the line of code, the entire section]

mainClassName = 'RunBeam'

shadowJar{

    zip64 true
    
    mergeServiceFiles('reference.conf')
    
    manifest{
    
        attributes 'Main-Class': mainClassName
    
    }
}

6. Compile the jar using this command while in your BEAM project directory:

    "gradle shadowJar"
    
    
7. Your new JAR should be in BEAM\build\libs
8. To execute your jar, use this command while in your BEAM project directory:

 "java -jar -Xmx8g .\build\libs\beam-0.8.0-all.jar --config test/input/beamville/beam.conf"
 
 
 This example is using the default scenario BEAMVILLE. 
 
 
 **Trouble Shooting**
 - If you're having trouble with memory issues, you may have a 32-bit Java Virtual Machine. You can lower the memory allocation by changing the -Xmx8g to -Xmx4g, but for bigger scenarios, you'll run out of memory. You can also try to run this on a lab computer to test it. 

