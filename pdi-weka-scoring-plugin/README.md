# pentaho-weka-scoring-plugin #
_Weka scoring plugin for Kettle_

The Weka Scoring plugin allows pre-built Weka classifiers and
clusterers to be applied inside of a Kettle transform to
score data. See docs/WekaScoring.pdf for more information.

To install the plugin step simply unzip the
"WekaScoring-SNAPSHOT-deploy.zip" archive in the plugins/steps
directory of your PDI installation.

More information on Weka can be found at:

http://www.cs.waikato.ac.nz/ml/weka

and

https://pentaho-community.atlassian.net/wiki/display/DATAMINING/Pentaho+Data+Mining+Community+Documentation

* Maven, version 3+
* Java JDK 1.8
* This [settings.xml](https://github.com/pentaho/maven-parent-poms/blob/master/maven-support-files/settings.xml) in your \<user-home\>/.m2 directory


__Build for nightly/release__

All required profiles are activated by the presence of a property named "release".

```
$ mvn clean install -Drelease
```

This will build, unit test, and package the whole project (all of the sub-modules). The artifact will be generated in: ```target```


__Build for CI/dev__

The `release` builds will compile the source for production (meaning potential obfuscation and/or uglification). To build without that happening, just eliminate the `release` property.

```
$ mvn clean install
```


__Unit tests__

This will run all tests in the project (and sub-modules).
```
$ mvn test
```

If you want to remote debug a single java unit test (default port is 5005):
```
$ cd core
$ mvn test -Dtest=<<YourTest>> -Dmaven.surefire.debug
```

__Integration tests__
In addition to the unit tests, there are integration tests in the core project.
```
$ mvn verify -DrunITs
```

To run a single integration test:
```
$ mvn verify -DrunITs -Dit.test=<<YourIT>>
```

To run a single integration test in debug mode (for remote debugging in an IDE) on the default port of 5005:
```
$ mvn verify -DrunITs -Dit.test=<<YourIT>> -Dmaven.failsafe.debug
```

__IntelliJ__

* Don't use IntelliJ's built-in maven. Make it use the same one you use from the commandline.
  * Project Preferences -> Build, Execution, Deployment -> Build Tools -> Maven ==> Maven home directory
