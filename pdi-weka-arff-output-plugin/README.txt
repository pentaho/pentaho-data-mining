=============================================================
                            README
              ARFF output plugin for Kettle 3.x
=============================================================

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

NOTE: Weka >=3.7.1 requires Java 1.6, so Kettle must be run with a
1.6 or higher JRE in this case.

Weka can be downloaded from

http://www.pentaho.com/download

or

http://www.cs.waikato.ac.nz/ml/weka
