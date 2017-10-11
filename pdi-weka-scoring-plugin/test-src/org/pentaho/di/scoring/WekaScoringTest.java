/*
* This program is free software; you can redistribute it and/or modify it under the
* terms of the GNU General Public License, version 2 as published by the Free Software
* Foundation.
*
* You should have received a copy of the GNU General Public License along with this
* program; if not, you can obtain a copy at http://www.gnu.org/licenses/gpl-2.0.html
* or from the Free Software Foundation, Inc.,
* 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*
* This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
* without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
* See the GNU General Public License for more details.
*
*
* Copyright 2006 - 2017 Hitachi Vantara.  All rights reserved.
*/

package org.pentaho.di.scoring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.pentaho.di.core.row.RowMeta;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMeta;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.variables.Variables;

/**
 * Unit tests for WekaScoring
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 */
public class WekaScoringTest {

  public static String CLASSIFICATION_MODEL = "test-src/nbIris.model";
  public static String CLUSTERING_MODEL = "test-src/emIris.model";

  public static Object[][] ROWS = { { 5.1, 3.5, 1.4, 0.2, "Iris-setosa" },
      { 4.9, 3.0, 1.4, 0.2, "Iris-setosa" },
      { 7.0, 3.2, 4.7, 1.4, "Iris-versicolor" },
      { 6.4, 3.2, 4.5, 1.5, "Iris-versicolor" },
      { 6.3, 3.3, 6.0, 2.5, "Iris-virginica" },
      { 5.8, 2.7, 5.1, 1.9, "Iris-virginica" } };

  @Test
  public void testLoadClassificationModel() throws Exception {

    WekaScoringModel model = WekaScoringData.loadSerializedModel(
        CLASSIFICATION_MODEL, null, new Variables());

    assertTrue(model != null);
  }

  @Test
  public void testLoadClusteringModel() throws Exception {
    WekaScoringModel model = WekaScoringData.loadSerializedModel(
        CLUSTERING_MODEL, null, new Variables());

    assertTrue(model != null);
  }

  @Test
  public void testGetFieldsNoProbsClassification() throws Exception {
    WekaScoringModel model = WekaScoringData.loadSerializedModel(
        CLASSIFICATION_MODEL, null, new Variables());

    assertTrue(model != null);

    WekaScoringMeta meta = new WekaScoringMeta();

    meta.setModel(model);
    RowMetaInterface rmi = new RowMeta();

    meta.getFields(rmi, null, null, null, new Variables());

    assertTrue(rmi.size() == 1);
    assertEquals(rmi.getValueMeta(0).getName(), "class_predicted");
  }

  @Test
  public void testGetFieldsProbsClassification() throws Exception {
    WekaScoringModel model = WekaScoringData.loadSerializedModel(
        CLASSIFICATION_MODEL, null, new Variables());

    assertTrue(model != null);

    WekaScoringMeta meta = new WekaScoringMeta();

    meta.setModel(model);
    meta.setOutputProbabilities(true);
    RowMetaInterface rmi = new RowMeta();

    meta.getFields(rmi, null, null, null, new Variables());

    assertTrue(rmi.size() == 3);
    assertEquals(rmi.getValueMeta(0).getName(),
        "class:Iris-setosa_predicted_prob");
    assertEquals(rmi.getValueMeta(1).getName(),
        "class:Iris-versicolor_predicted_prob");
    assertEquals(rmi.getValueMeta(2).getName(),
        "class:Iris-virginica_predicted_prob");
  }

  @Test
  public void testGetFieldsNoProbsClustering() throws Exception {
    WekaScoringModel model = WekaScoringData.loadSerializedModel(
        CLUSTERING_MODEL, null, new Variables());

    assertTrue(model != null);

    WekaScoringMeta meta = new WekaScoringMeta();

    meta.setModel(model);
    RowMetaInterface rmi = new RowMeta();

    meta.getFields(rmi, null, null, null, new Variables());

    assertTrue(rmi.size() == 1);
    assertEquals(rmi.getValueMeta(0).getName(), "cluster#_predicted");
  }

  @Test
  public void testGenerateClassLabelsWithClassificationModel() throws Exception {
    WekaScoringModel model = WekaScoringData.loadSerializedModel(
        CLASSIFICATION_MODEL, null, new Variables());

    assertTrue(model != null);

    WekaScoringMeta meta = new WekaScoringMeta();

    meta.setModel(model);

    RowMetaInterface rmi = new RowMeta();
    RowMetaInterface outRowMeta = new RowMeta();
    ValueMetaInterface vmi = new ValueMeta();
    vmi.setName("sepallength");
    vmi.setType(ValueMetaInterface.TYPE_NUMBER);
    rmi.addValueMeta(vmi);
    outRowMeta.addValueMeta(vmi);
    vmi = new ValueMeta();
    vmi.setName("sepalwidth");
    vmi.setType(ValueMetaInterface.TYPE_NUMBER);
    rmi.addValueMeta(vmi);
    outRowMeta.addValueMeta(vmi);
    vmi = new ValueMeta();
    vmi.setName("petallength");
    vmi.setType(ValueMetaInterface.TYPE_NUMBER);
    rmi.addValueMeta(vmi);
    outRowMeta.addValueMeta(vmi);
    vmi = new ValueMeta();
    vmi.setName("petalwidth");
    vmi.setType(ValueMetaInterface.TYPE_NUMBER);
    rmi.addValueMeta(vmi);
    outRowMeta.addValueMeta(vmi);

    WekaScoringData data = new WekaScoringData();
    data.setModel(model);
    data.setOutputRowMeta(outRowMeta);

    meta.getFields(outRowMeta, null, null, null, new Variables());

    assertTrue(outRowMeta.size() == 5);

    data.mapIncomingRowMetaData(model.getHeader(), rmi, false, null);

    for (int i = 0; i < ROWS.length; i++) {
      Object[] row = ROWS[i];

      Object[] rowPlusPreds = data.generatePrediction(rmi, outRowMeta, row,
          meta);
      if (i < 2) {
        assertEquals(rowPlusPreds[4].toString(), "Iris-setosa");
      } else if (i < 4) {
        assertEquals(rowPlusPreds[4].toString(), "Iris-versicolor");
      } else {
        assertEquals(rowPlusPreds[4].toString(), "Iris-virginica");
      }
    }
  }

  @Test
  public void testGenerateClassProbsWithClassificationModel() throws Exception {
    WekaScoringModel model = WekaScoringData.loadSerializedModel(
        CLASSIFICATION_MODEL, null, new Variables());

    assertTrue(model != null);

    WekaScoringMeta meta = new WekaScoringMeta();

    meta.setModel(model);
    meta.setOutputProbabilities(true);

    RowMetaInterface rmi = new RowMeta();
    RowMetaInterface outRowMeta = new RowMeta();
    ValueMetaInterface vmi = new ValueMeta();
    vmi.setName("sepallength");
    vmi.setType(ValueMetaInterface.TYPE_NUMBER);
    rmi.addValueMeta(vmi);
    outRowMeta.addValueMeta(vmi);
    vmi = new ValueMeta();
    vmi.setName("sepalwidth");
    vmi.setType(ValueMetaInterface.TYPE_NUMBER);
    rmi.addValueMeta(vmi);
    outRowMeta.addValueMeta(vmi);
    vmi = new ValueMeta();
    vmi.setName("petallength");
    vmi.setType(ValueMetaInterface.TYPE_NUMBER);
    rmi.addValueMeta(vmi);
    outRowMeta.addValueMeta(vmi);
    vmi = new ValueMeta();
    vmi.setName("petalwidth");
    vmi.setType(ValueMetaInterface.TYPE_NUMBER);
    rmi.addValueMeta(vmi);
    outRowMeta.addValueMeta(vmi);

    WekaScoringData data = new WekaScoringData();
    data.setModel(model);
    data.setOutputRowMeta(outRowMeta);

    meta.getFields(outRowMeta, null, null, null, new Variables());

    // probability distribution - one extra column per class label
    assertTrue(outRowMeta.size() == 7);

    data.mapIncomingRowMetaData(model.getHeader(), rmi, false, null);

    String[] expected = { "1.000 0.000 0.000", "1.000 0.000 0.000",
        "0.000 0.901 0.099", "0.000 0.961 0.039", "0.000 0.000 1.000",
        "0.000 0.010 0.990" };

    for (int i = 0; i < ROWS.length; i++) {
      Object[] row = ROWS[i];

      Object[] rowPlusPreds = data.generatePrediction(rmi, outRowMeta, row,
          meta);

      String result = "" + String.format("%1.3f", rowPlusPreds[4]) + " "
          + String.format("%1.3f", rowPlusPreds[5]) + " "
          + String.format("%1.3f", rowPlusPreds[6]);

      assertEquals(result, expected[i]);
    }
  }

  @Test
  public void testGenerateClusterLabelsWithClusterModel() throws Exception {
    WekaScoringModel model = WekaScoringData.loadSerializedModel(
        CLUSTERING_MODEL, null, new Variables());

    assertTrue(model != null);

    WekaScoringMeta meta = new WekaScoringMeta();

    meta.setModel(model);

    RowMetaInterface rmi = new RowMeta();
    RowMetaInterface outRowMeta = new RowMeta();
    ValueMetaInterface vmi = new ValueMeta();
    vmi.setName("sepallength");
    vmi.setType(ValueMetaInterface.TYPE_NUMBER);
    rmi.addValueMeta(vmi);
    outRowMeta.addValueMeta(vmi);
    vmi = new ValueMeta();
    vmi.setName("sepalwidth");
    vmi.setType(ValueMetaInterface.TYPE_NUMBER);
    rmi.addValueMeta(vmi);
    outRowMeta.addValueMeta(vmi);
    vmi = new ValueMeta();
    vmi.setName("petallength");
    vmi.setType(ValueMetaInterface.TYPE_NUMBER);
    rmi.addValueMeta(vmi);
    outRowMeta.addValueMeta(vmi);
    vmi = new ValueMeta();
    vmi.setName("petalwidth");
    vmi.setType(ValueMetaInterface.TYPE_NUMBER);
    rmi.addValueMeta(vmi);
    outRowMeta.addValueMeta(vmi);

    WekaScoringData data = new WekaScoringData();
    data.setModel(model);
    data.setOutputRowMeta(outRowMeta);

    meta.getFields(outRowMeta, null, null, null, new Variables());

    assertTrue(outRowMeta.size() == 5);

    data.mapIncomingRowMetaData(model.getHeader(), rmi, false, null);

    for (int i = 0; i < ROWS.length; i++) {
      Object[] row = ROWS[i];

      Object[] rowPlusPreds = data.generatePrediction(rmi, outRowMeta, row,
          meta);

      if (i < 2) {
        assertEquals(rowPlusPreds[4].toString(), "0.0");
      } else if (i < 4) {
        assertEquals(rowPlusPreds[4].toString(), "2.0");
      } else if (i == 4) {
        assertEquals(rowPlusPreds[4].toString(), "1.0");
      } else {
        assertEquals(rowPlusPreds[4].toString(), "1.0");
      }
    }
  }

  public static void main(String[] args) {
    try {
      WekaScoringTest test = new WekaScoringTest();
      test.testLoadClassificationModel();
      test.testLoadClusteringModel();
      test.testGetFieldsNoProbsClassification();
      test.testGetFieldsNoProbsClustering();
      test.testGetFieldsProbsClassification();
      test.testGenerateClassLabelsWithClassificationModel();
      test.testGenerateClassProbsWithClassificationModel();
      test.testGenerateClusterLabelsWithClusterModel();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }
}
