=============================================================
                            README
              Weka scoring plugin for Kettle 3.0
=============================================================

The Weka Scoring plugin allows pre-built Weka classifiers and
clusterers to be applied inside of a Kettle transform to
score data. See docs/WekaScoring.pdf for more information.

IMPORTANT:

This plugin requires Weka to be available as a library. In order to
compile the code, download the latest stable (3.6.x) or development
(3.7.x) version of Weka, unpack the archive and copy the "weka.jar" to
libext. To deploy the plugin in Kettle, copy the contents of
WekaScoringDeploy, along with weka.jar, to your Kettle plugins
directory.

Weka can be downloaded from

http://www.pentaho.com/download

or

http://www.cs.waikato.ac.nz/ml/weka
