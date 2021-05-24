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

# Running BEAM on the SuperComputer:
To run BEAM with a scenario on the SuperComputer, follow these steps:
1. Make sure you have an account with the BYU Research Computing Department. https://rc.byu.edu/ If they ask for a sponsor, find the netID of your professor you're researching with. Set up your account with a password and authentication keys. 

2. You will need to build BEAM as a JAR through Gradle. See down below. You will not be able to use IntelliJ on the SuperComputer, so you will need to use a FatJar, which includes all packages in the JAR. 

3. You will need a few extra files: the common folder (test/input/common), the dtd folder (test/input/dtd) and the scenario folder (test/input/beamville).

4. Your files should be positioned like this:
    -  *folder name*
        - test
            - input
                - beamville (or the scenario you're running)
                - common
                - dtd
        - beam jar

5. Once you have your files positioned, secure-copy your files to the SuperComputer. Open a terminal like Windows Powershell or a Linux Terminal:
    - Navigate to your file that contains the jar and test folder. Hint: "cd .\folder"
    - Use the command "scp -r ./ *your_username*@ssh.rc.byu.edu:/fslhome/*your_username*"
    - Type in your password and verification code according to the terminal prompts.

6. Login to the SuperComputer.
    - Go to https://viz.rc.byu.edu:3443/
        - This is a desktop interface for the SuperComputer. 
    - Start a new session by clicking on the blue + button on the left side of the screen.
    - Click "Cinnamon" and "Launch".
    - You're now viewing a graphical interface for the SuperComputer you're going to run scenarios on. 
 
7. Write the Slurm Script to run your Jar.
    - Slurm is how your job gets allocated resources. You need to build a .sh script for it. 
    - In your Home Directory in the SuperComputer, type vim *nameOfScript*.sh
    - For the first half of your script, go to https://rc.byu.edu/documentation/slurm/script-generator
        - In the parameters section, put in what resources you will need. 
        - Example: ![image](https://user-images.githubusercontent.com/59575386/119387505-45f11880-bc86-11eb-8192-1dad278ee4c2.png)
        - This will generate the first half of your script. Copy and paste it into the .sh file you created. Be sure to familiarize yourself with vim commands. 
    - For the second half of your script, you will need to:
        1. Load some modules. Type on separate lines:
            - module purge 
            - module load java
            - module list
        2. Type the command to get into your scenario's file directory
            - Example: cd beamville
            - Otherwise, your script won't be able to access the jar.
        3. Type the command "java -jar -Xmx8g .\build\libs\beam-0.8.0-all.jar --config test/input/beamville/beam.conf"
            - Xmx8g = 8 GB. Bigger scenarios will 100% need more RAM.
            - --config is directed at the beamville config. Adjust for your scenario's config. 

8. Run the script with some additional commands to view log output in the terminal.
    - sbatch *shell_script.sh* // This runs your script
        - You will see "Submitted batch job ##########"
    - tail -f slurm-######## //This puts your script log into the terminal 
10. View the created outputfiles and SCP them back to your computer for analysis. 
    - Use the command "scp -r *your_username*@ssh.rc.byu.edu:/fslhome/username/*output_folder* Documents/


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
 
 9. You will need the additional input files for your scenario as well, otherwise you'll get a error for missing files. 
 
 **Trouble Shooting**
 - If you're having trouble with memory issues, you may have a 32-bit Java Virtual Machine. You can lower the memory allocation by changing the -Xmx8g to -Xmx4g, but for bigger scenarios, you'll run out of memory. You can also try to run this on a lab computer to test it. 

