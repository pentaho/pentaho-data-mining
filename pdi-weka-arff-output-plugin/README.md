# pentaho-weka-arff-output-plugin #
_ARFF Output Plugin for Kettle_

The ARFF output plugin allows Kettle data streams to be saved to a
file in Weka's ARFF format. See docs/ArffOutput.pdf for more
information.

IMPORTANT:

This plugin requires Weka to be available as a library. In order to
compile the code, download either the stable 3.6.x or developer 3.7.x
version of Weka, unpack the archive and copy the "weka.jar" to
libext. To deploy the plugin in Kettle, copy the contents of
ArffOutputDeploy, along with weka.jar, to your Kettle plugins
directory.

NOTE: Weka >=3.7.1 requires Java 1.6, so Kettle needs to be
run with a 1.6 or higher JRE.

Weka can be downloaded from

http://www.pentaho.com/download

or

http://www.cs.waikato.ac.nz/ml/weka

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
