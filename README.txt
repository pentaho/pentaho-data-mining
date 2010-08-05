=============================================================
                            README
              ARFF output plugin for Kettle 3.0
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

Weka can be downloaded from

http://www.pentaho.com/download

or

http://www.cs.waikato.ac.nz/ml/weka
