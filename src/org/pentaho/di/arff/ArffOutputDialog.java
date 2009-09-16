/*
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

/*
 *    ArffOutputDialog.java
 *    Copyright 2007 Pentaho Corporation.  All rights reserved. 
 *
 */

package org.pentaho.di.arff;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.graphics.Point; 
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.widgets.Composite;

import org.pentaho.di.core.Const;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMeta;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStepMeta;
import org.pentaho.di.trans.step.StepDialogInterface;
import org.pentaho.di.trans.step.StepMeta;

import org.pentaho.di.core.Props;
import org.pentaho.di.ui.core.dialog.EnterSelectionDialog;
import org.pentaho.di.ui.core.widget.ColumnInfo;
import org.pentaho.di.ui.core.widget.TableView;
import org.pentaho.di.ui.trans.step.BaseStepDialog;
import org.pentaho.di.ui.core.widget.TextVar;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.io.File;

/**
 * The UI class for the ArffOutput step
 *
 * @author Mark Hall (mhall{[at]}pentaho.org)
 * @version 1.0
 */
public class ArffOutputDialog extends BaseStepDialog
  implements StepDialogInterface {

  // Step name stuff
  private Label m_wlStepname;
  private Text m_wStepname;
  private FormData m_fdlStepname;
  private FormData m_fdStepname;

  // Tab stuff
  private FormData m_fdTabFolder;
  private FormData m_fdFileComp, m_fdContentComp, m_fdFieldsComp;
  private CTabFolder m_wTabFolder;
  private CTabItem m_wFileTab, m_wContentTab, m_wFieldsTab;

  // File related stuff
  // label for the file name field
  private Label m_wlFilename;

  // file name field
  private FormData m_fdlFilename, m_fdbFilename, m_fdFilename;

  // Browse file button
  private Button m_wbFilename;

  // Combines text field with widget to insert environment variable
  private TextVar m_wFilename;

  // Relation name stuff
  private Label m_wlRelationName;
  private Text m_wRelationName;
  private FormData m_fdlRelationName;
  private FormData m_fdRelationName;

  // Content stuff
  // Format combo
  private Label m_wlFormat;
  private CCombo m_wFormat;
  private FormData m_fdlFormat, m_fdFormat;

  // Encoding combo
  private Label m_wlEncoding;
  private CCombo m_wEncoding;
  private FormData m_fdlEncoding, m_fdEncoding;

  // Compression, to do?
  
  // Fields stuff
  // Fields table stuff
  private Label m_wlFields;
  private TableView m_wFields;
  private FormData m_fdlFields;
  private FormData m_fdFields;

  private ColumnInfo[] m_colinf;
  
  // weight field stuff
  private Label m_weightFieldCheckBoxLab;
  private Button m_weightFieldCheckBox;
  private Label m_weightFieldLab;
  private CCombo m_weightFieldComboBox;

  private boolean m_gotEncodings = false;

  /**
   * meta data for the step. A copy is made so
   * that changes, in terms of choices made by the
   * user, can be detected.
   */
  private ArffOutputMeta m_currentMeta;
  private ArffOutputMeta m_originalMeta;

  public ArffOutputDialog(Shell parent, 
                          Object in, 
                          TransMeta tr, 
                          String sname) {

    super(parent, (BaseStepMeta) in, tr, sname);

    // The order here is important... 
    //m_currentMeta is looked at for changes
    m_currentMeta = (ArffOutputMeta) in;
    m_originalMeta = (ArffOutputMeta) m_currentMeta.clone();
  }

  /**
   * Open the dialog
   *
   * @return the step name
   */
  public String open() {
    Shell parent = getParent();
    Display display = parent.getDisplay();

    shell = 
      new Shell(parent, SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MIN | SWT.MAX);

    props.setLook(shell);
    setShellImage(shell, m_currentMeta);

    // used to listen to a text field (m_wStepname)
    ModifyListener lsMod = new ModifyListener() {
        public void modifyText(ModifyEvent e) {
          m_currentMeta.setChanged();
        }
      };

    changed = m_currentMeta.hasChanged();

    FormLayout formLayout = new FormLayout();
    formLayout.marginWidth = Const.FORM_MARGIN;
    formLayout.marginHeight = Const.FORM_MARGIN;

    shell.setLayout(formLayout);
    shell.setText(Messages.getString("ArffOutputDialog.Shell.Title"));

    int middle = props.getMiddlePct();
    int margin = Const.MARGIN;

    // Stepname line
    m_wlStepname = new Label(shell, SWT.RIGHT);
    m_wlStepname.
      setText(Messages.getString("ArffOutputDialog.StepName.Label"));
    props.setLook(m_wlStepname);

    m_fdlStepname = new FormData();
    m_fdlStepname.left = new FormAttachment(0, 0);
    m_fdlStepname.right = new FormAttachment(middle, -margin);
    m_fdlStepname.top = new FormAttachment(0, margin);
    m_wlStepname.setLayoutData(m_fdlStepname);
    m_wStepname = new Text(shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    m_wStepname.setText(stepname);
    props.setLook(m_wStepname);
    m_wStepname.addModifyListener(lsMod);
    
    // format the text field
    m_fdStepname = new FormData();
    m_fdStepname.left = new FormAttachment(middle, 0);
    m_fdStepname.top = new FormAttachment(0, margin);
    m_fdStepname.right = new FormAttachment(100, 0);
    m_wStepname.setLayoutData(m_fdStepname);
    
    m_wTabFolder = new CTabFolder(shell, SWT.BORDER);
    props.setLook(m_wTabFolder, Props.WIDGET_STYLE_TAB);
    m_wTabFolder.setSimple(false);

    // Start of the file tab
    m_wFileTab = new CTabItem(m_wTabFolder, SWT.NONE);
    m_wFileTab.
      setText(Messages.getString("ArffOutputDialog.FileTab.TabTitle"));
    
    Composite wFileComp = new Composite(m_wTabFolder, SWT.NONE);
    props.setLook(wFileComp);
    
    FormLayout fileLayout = new FormLayout();
    fileLayout.marginWidth  = 3;
    fileLayout.marginHeight = 3;
    wFileComp.setLayout(fileLayout);

    // Filename line
    m_wlFilename = new Label(wFileComp, SWT.RIGHT);
    m_wlFilename.
      setText(Messages.getString("ArffOutputDialog.Filename.Label"));
    props.setLook(m_wlFilename);
    m_fdlFilename = new FormData();
    m_fdlFilename.left = new FormAttachment(0, 0);
    m_fdlFilename.top = new FormAttachment(0, margin);
    m_fdlFilename.right = new FormAttachment(middle, -margin);
    m_wlFilename.setLayoutData(m_fdlFilename);

    // file browse button
    m_wbFilename=new Button(wFileComp, SWT.PUSH| SWT.CENTER);
    props.setLook(m_wbFilename);
    m_wbFilename.setText(Messages.getString("System.Button.Browse"));
    m_fdbFilename=new FormData();
    m_fdbFilename.right = new FormAttachment(100, 0);
    m_fdbFilename.top = new FormAttachment(0, 0);
    m_wbFilename.setLayoutData(m_fdbFilename);

    // combined text field and env variable widget
    m_wFilename = new TextVar(transMeta, wFileComp, 
                              SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    props.setLook(m_wFilename);
    m_wFilename.addModifyListener(lsMod);
    m_fdFilename=new FormData();
    m_fdFilename.left = new FormAttachment(middle, 0);
    m_fdFilename.top = new FormAttachment(0, margin);
    m_fdFilename.right = new FormAttachment(m_wbFilename, -margin);
    m_wFilename.setLayoutData(m_fdFilename);

    m_fdFileComp = new FormData();
    m_fdFileComp.left = new FormAttachment(0, 0);
    m_fdFileComp.top = new FormAttachment(0, 0);
    m_fdFileComp.right = new FormAttachment(100, 0);
    m_fdFileComp.bottom = new FormAttachment(100, 0);
    wFileComp.setLayoutData(m_fdFileComp);    
        
    // Relation name line
    m_wlRelationName = new Label(wFileComp, SWT.RIGHT);
    m_wlRelationName.
      setText(Messages.getString("ArffOutputDialog.RelationName.Label"));
    props.setLook(m_wlRelationName);
    m_fdlRelationName = new FormData();
    m_fdlRelationName.left = new FormAttachment(0, 0);
    m_fdlRelationName.top = new FormAttachment(m_wFilename, margin);
    m_fdlRelationName.right = new FormAttachment(middle, -margin);
    m_wlRelationName.setLayoutData(m_fdlRelationName);
    m_wRelationName = new Text(wFileComp, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    m_wRelationName.setText("NewRelation");
    props.setLook(m_wRelationName);
    m_wRelationName.addModifyListener(lsMod);
    m_fdRelationName = new FormData();
    m_fdRelationName.left = new FormAttachment(middle, 0);
    m_fdRelationName.top = new FormAttachment(m_wFilename, margin);
    m_fdRelationName.right = new FormAttachment(100, 0);
    m_wRelationName.setLayoutData(m_fdRelationName);

    wFileComp.layout();
    m_wFileTab.setControl(wFileComp);

    // Content tab
    m_wContentTab = new CTabItem(m_wTabFolder, SWT.NONE);
    m_wContentTab.
      setText(Messages.getString("ArffOutputDialog.ContentTab.TabTitle"));
    
    FormLayout contentLayout = new FormLayout();
    contentLayout.marginWidth  = 3;
    contentLayout.marginHeight = 3;
    
    Composite wContentComp = new Composite(m_wTabFolder, SWT.NONE);
    props.setLook(wContentComp);
    wContentComp.setLayout(contentLayout);

    // Format line
    m_wlFormat = new Label(wContentComp, SWT.RIGHT);
    m_wlFormat.
      setText(Messages.getString("ArffOutputDialog.Format.Label"));
    props.setLook(m_wlFormat);
    m_fdlFormat = new FormData();
    m_fdlFormat.left = new FormAttachment(0, 0);
    m_fdlFormat.top = new FormAttachment(0, margin);
    m_fdlFormat.right = new FormAttachment(middle, -margin);
    m_wlFormat.setLayoutData(m_fdlFormat);
    m_wFormat = new CCombo(wContentComp, SWT.BORDER | SWT.READ_ONLY);
    m_wFormat.setText(Messages.getString("ArffOutputDialog.Format.Label"));
    props.setLook(m_wFormat);
    m_wFormat.add("DOS");
    m_wFormat.add("Unix");
    m_wFormat.select(0);
    m_wFormat.addModifyListener(lsMod);
    m_fdFormat = new FormData();
    m_fdFormat.left = new FormAttachment(middle, 0);
    m_fdFormat.top = new FormAttachment(0, margin);
    m_fdFormat.right = new FormAttachment(100, 0);
    m_wFormat.setLayoutData(m_fdFormat);

    // Encoding line
    m_wlEncoding = new Label(wContentComp, SWT.RIGHT);
    m_wlEncoding.
      setText(Messages.getString("ArffOutputDialog.Encoding.Label"));
    props.setLook(m_wlEncoding);
    m_fdlEncoding = new FormData();
    m_fdlEncoding.left = new FormAttachment(0, 0);
    m_fdlEncoding.top = new FormAttachment(m_wFormat, margin);
    m_fdlEncoding.right = new FormAttachment(middle, -margin);
    m_wlEncoding.setLayoutData(m_fdlEncoding);
    m_wEncoding = new CCombo(wContentComp, SWT.BORDER | SWT.READ_ONLY);
    m_wEncoding.setEditable(true);
    /*    m_wEncoding.setText(Messages.
          getString("")); */
    props.setLook(m_wEncoding);
    m_wEncoding.addModifyListener(lsMod);
    m_fdEncoding = new FormData();
    m_fdEncoding.left = new FormAttachment(middle, 0);
    m_fdEncoding.top = new FormAttachment(m_wFormat, margin);
    m_fdEncoding.right = new FormAttachment(100, 0);
    m_wEncoding.setLayoutData(m_fdEncoding);
    m_wEncoding.addFocusListener(new FocusListener() {
        public void focusLost(FocusEvent e) {
        }

        public void focusGained(FocusEvent e) {
          Cursor busy = new Cursor(shell.getDisplay(), SWT.CURSOR_WAIT);
          shell.setCursor(busy);
          setEncodings();
          shell.setCursor(null);
          busy.dispose();
        }
      });

    // Compression stuff?

    m_fdContentComp = new FormData();
    m_fdContentComp.left = new FormAttachment(0, 0);
    m_fdContentComp.top = new FormAttachment(0, 0);
    m_fdContentComp.right = new FormAttachment(100, 0);
    m_fdContentComp.bottom = new FormAttachment(100, 0);
    wContentComp.setLayoutData(m_fdContentComp);    
    wContentComp.layout();
    m_wContentTab.setControl(wContentComp);

    // Fields tab
    m_wFieldsTab = new CTabItem(m_wTabFolder, SWT.NONE);
    m_wFieldsTab.
      setText(Messages.getString("ArffOutputDialog.FieldsTab.TabTitle"));
    
    FormLayout fieldsLayout = new FormLayout();
    fieldsLayout.marginWidth  = 3;
    fieldsLayout.marginHeight = 3;
    
    Composite wFieldsComp = new Composite(m_wTabFolder, SWT.NONE);
    props.setLook(wFieldsComp);
    wFieldsComp.setLayout(fieldsLayout);
    
    m_wlFields = new Label(wFieldsComp, SWT.NONE);
    m_wlFields.
      setText(Messages.getString("ArffOutputDialog.FieldsTab.Label"));
    props.setLook(m_wlFields);
    m_fdlFields = new FormData();
    m_fdlFields.left = new FormAttachment(0, 0);
    m_fdlFields.top = new FormAttachment(0, margin);
    m_wlFields.setLayoutData(m_fdlFields);

    /* if (m_currentMeta.getOutputFields().length == 0) {
      setupArffMetas();
      } */

    final int fieldsRows = 5;
      /*      (m_currentMeta.getOutputFields() != null)
      ? m_currentMeta.getOutputFields().length + 1 
      : 1;   */
    
    m_colinf = new ColumnInfo[] {
      new ColumnInfo(Messages.getString(
            "ArffOutputDialog.OutputFieldsColumn.Name"),
                     ColumnInfo.COLUMN_TYPE_TEXT, false),
        new ColumnInfo(Messages.getString(
            "ArffOutputDialog.OutputFieldsColumn.KettleType"),
             ColumnInfo.COLUMN_TYPE_TEXT, false),
        new ColumnInfo(Messages.getString(
            "ArffOutputDialog.OutputFieldsColumn.ArffType"),
             ColumnInfo.COLUMN_TYPE_TEXT, false)
    };
    m_colinf[0].setReadOnly(true);
    m_colinf[1].setReadOnly(true);
    m_colinf[2].setReadOnly(true);

    m_wFields = new TableView(transMeta, wFieldsComp,
                              SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI, 
                              m_colinf, fieldsRows, lsMod,
                              props);

    m_fdFields = new FormData();
    m_fdFields.left = new FormAttachment(0, 0);
    m_fdFields.top = new FormAttachment(m_wlFields, margin);
    m_fdFields.right = new FormAttachment(100, 0);
    m_fdFields.bottom = new FormAttachment(100, -100);
    m_wFields.setLayoutData(m_fdFields);

    wGet = new Button(wFieldsComp, SWT.PUSH);
    wGet.setText(Messages.getString("System.Button.GetFields"));
    wGet.setToolTipText(Messages.getString("System.Tooltip.GetFields"));

    // setButtonPositions(new Button[] { wGet }, margin, null);
    FormData temp = new FormData();
    temp.left = new FormAttachment(0, 0);
    temp.right = new FormAttachment(middle, -margin);
    temp.top = new FormAttachment(m_wFields, margin);
    wGet.setLayoutData(temp);

    lsGet = new Listener() {
        public void handleEvent(Event e) {
          setupArffMetas();
          getData();
        }
      };
    wGet.addListener(SWT.Selection, lsGet);
    
    m_weightFieldCheckBoxLab = new Label(wFieldsComp, SWT.RIGHT);
    m_weightFieldCheckBoxLab.setText("Set instance weights from field");
    props.setLook(m_weightFieldCheckBoxLab);
    FormData fdlWeightFieldCheckBoxLab = new FormData();
    fdlWeightFieldCheckBoxLab.left = new FormAttachment(0, 0);
    fdlWeightFieldCheckBoxLab.right = new FormAttachment(middle, -margin);
    fdlWeightFieldCheckBoxLab.top = new FormAttachment(wGet, margin);
    m_weightFieldCheckBoxLab.setLayoutData(fdlWeightFieldCheckBoxLab);
    
    m_weightFieldCheckBox = new Button(wFieldsComp, SWT.CHECK);
    props.setLook(m_weightFieldCheckBox);
    FormData fdlWeightFieldCheckBox = new FormData();
    fdlWeightFieldCheckBox.left = new FormAttachment(middle, 0);
    fdlWeightFieldCheckBox.right = new FormAttachment(100, 0);
    fdlWeightFieldCheckBox.top = new FormAttachment(wGet, margin);
    m_weightFieldCheckBox.setLayoutData(fdlWeightFieldCheckBox);
    
    m_weightFieldLab = new Label(wFieldsComp, SWT.RIGHT);
    m_weightFieldLab.setText("Weight field");
    props.setLook(m_weightFieldLab);
    FormData fdlWeightFieldLab = new FormData();
    fdlWeightFieldLab.left = new FormAttachment(0, 0);
    fdlWeightFieldLab.right = new FormAttachment(middle, -margin);
    fdlWeightFieldLab.top = new FormAttachment(m_weightFieldCheckBox, margin);
    m_weightFieldLab.setLayoutData(fdlWeightFieldLab);
    
    m_weightFieldComboBox = new CCombo(wFieldsComp, SWT.BORDER | SWT.READ_ONLY);
    m_weightFieldComboBox.setToolTipText("Set instance-level weights using this incoming field");
    props.setLook(m_weightFieldComboBox);
    FormData fdlWeightFieldComboBox = new FormData();
    fdlWeightFieldComboBox.left = new FormAttachment(middle, 0);
    fdlWeightFieldComboBox.right = new FormAttachment(100, 0);
    fdlWeightFieldComboBox.top = new FormAttachment(m_weightFieldCheckBox, margin);
    m_weightFieldComboBox.setLayoutData(fdlWeightFieldComboBox);
    m_weightFieldComboBox.setEnabled(false);
    
    m_weightFieldCheckBox.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        if (m_weightFieldCheckBox.getSelection()) {
          setupWeightFieldComboBox();
        }
        m_weightFieldComboBox.setEnabled(m_weightFieldCheckBox.getSelection());
      }
    });
    

    m_fdFieldsComp = new FormData();
    m_fdFieldsComp.left = new FormAttachment(0, 0);
    m_fdFieldsComp.top = new FormAttachment(0, 0);
    m_fdFieldsComp.right = new FormAttachment(100, 0);
    m_fdFieldsComp.bottom = new FormAttachment(100, 0);
    wFieldsComp.setLayoutData(m_fdFieldsComp);    
    wFieldsComp.layout();
    m_wFieldsTab.setControl(wFieldsComp); 

    m_fdTabFolder = new FormData();
    m_fdTabFolder.left  = new FormAttachment(0, 0);
    m_fdTabFolder.top   = new FormAttachment(m_wStepname, margin);
    m_fdTabFolder.right = new FormAttachment(100, 0);
    m_fdTabFolder.bottom= new FormAttachment(100, -50);
    m_wTabFolder.setLayoutData(m_fdTabFolder);


    // Some buttons
    wOK = new Button(shell, SWT.PUSH);
    wOK.setText(Messages.getString("System.Button.OK"));
    wCancel = new Button(shell, SWT.PUSH);
    wCancel.setText(Messages.getString("System.Button.Cancel"));

    setButtonPositions(new Button[] { wOK, wCancel }, margin, null);

    // Add listeners

    // Whenever something changes, set the tooltip to the expanded version:
    m_wFilename.addModifyListener(new ModifyListener() {
        public void modifyText(ModifyEvent e) {
          m_wFilename.
            setToolTipText(transMeta.
                           environmentSubstitute(m_wFilename.getText()));
        }
      });

    m_wFilename.addSelectionListener(new SelectionAdapter() {
        public void widgetDefaultSelected(SelectionEvent e) {
          m_currentMeta.setFileName(m_wFilename.getText());
        }
      });

    m_wbFilename.addSelectionListener(
       new SelectionAdapter() {
         public void widgetSelected(SelectionEvent e) {
           FileDialog dialog = new FileDialog(shell, SWT.SAVE);
           dialog.setFilterExtensions(new String[] {"*.arff", "*"});
           if (m_wFilename.getText() != null) {
             String fn = m_wFilename.getText();
             int l = fn.lastIndexOf(System.getProperty("file.separator"));
             if (l >= 0) {
               fn = fn.substring(l+1, fn.length());
             }
             dialog.setFileName(transMeta.
                                environmentSubstitute(fn)); 

           }
           dialog.setFilterNames(new String[] {
               Messages.getString("System.FileType.AllFiles")});

           if (dialog.open() != null) {

             String fileName = dialog.getFileName();
             if (!fileName.endsWith(".arff")) { 
               fileName += ".arff";
             }

             m_wFilename.setText(dialog.getFilterPath()
                                 + System.getProperty("file.separator")
                                 + fileName);
             //             }

             // try to load model file and display model
             //             loadModel();
             m_currentMeta.setFileName(fileName);
           }
         }
       });


    lsCancel = new Listener() {
        public void handleEvent(Event e) {
          cancel();
        }
      };
    lsOK = new Listener() {
        public void handleEvent(Event e) {
          ok();
        }
      };

    wCancel.addListener(SWT.Selection, lsCancel);
    wOK.addListener(SWT.Selection, lsOK);

    lsDef = new SelectionAdapter() {
        public void widgetDefaultSelected(SelectionEvent e) {
          ok();
        }
      };
    
    m_wStepname.addSelectionListener(lsDef);

    // Detect X or ALT-F4 or something that kills this window...
    shell.addShellListener(new ShellAdapter() {
        public void shellClosed(ShellEvent e) {
          cancel();
        }
      });

    /*    lsResize = new Listener() {
        public void handleEvent(Event event) {
          Point size = shell.getSize();
          m_wFields.setSize(size.x-10, size.y-50);
          m_wFields.table.setSize(size.x-10, size.y-50);
          m_wFields.redraw();
        }
        };
        shell.addListener(SWT.Resize, lsResize); */

    m_wTabFolder.setSelection(0);
    
    // Set the shell size, based upon previous time...
    setSize();

    getData();
    m_currentMeta.setChanged(changed);

    shell.open();
    
    while (!shell.isDisposed()) {
      if (!display.readAndDispatch()) {
        display.sleep();
      }
    }

    return stepname;
  }

  /**
   * Copy data out of the ArffOutputMeta object and
   * into the GUI
   */
  private void getData() {

    String format = m_currentMeta.getFileFormat();
    if (format != null && format.length() > 0) {
      m_wFormat.setText(format);
    }

    String encoding = m_currentMeta.getEncoding();
    if (encoding != null && encoding.length() > 0) {
      m_wEncoding.setText(encoding);
    }
    String fName = m_currentMeta.getFileName();

    m_wFilename.setText(fName);

    String rName = m_currentMeta.getRelationName();
    m_wRelationName.setText(rName);
    ArffMeta [] fields = m_currentMeta.getOutputFields();
    if (fields == null || fields.length == 0) {
      fields = setupArffMetas();
    }

    if (fields != null) {
      m_wFields.clearAll(false);
      Table table = m_wFields.table;

      int count = 0;
      for (int i = 0; i < fields.length; i++) {
        if (fields[i] != null) {
          //          TableItem item = m_wFields.table.getItem(i);
          TableItem item = new TableItem(table, SWT.NONE);
          item.setText(1, Const.NVL(fields[i].getFieldName(), ""));
          item.setText(2, Const.NVL(getKettleTypeString(fields[i].
                                      getKettleType()), ""));
          item.setText(3, Const.NVL(getArffTypeString(fields[i].
                                      getArffType()), ""));
          /*          if (fields[i].getArffType() == ArffMeta.NUMERIC) {
            item.setText(4, Const.NVL(""+fields[i].getPrecision(), ""));
            } */
        }
      }
      m_wFields.removeEmptyRows();
      m_wFields.setRowNums();
      m_wFields.optWidth(true);
      
      // weight field specified?
      if (!Const.isEmpty(m_currentMeta.getWeightFieldName())) {
        m_weightFieldCheckBox.setSelection(true);
        setupWeightFieldComboBox();
        m_weightFieldComboBox.setEnabled(true);
        m_weightFieldComboBox.setText(m_currentMeta.getWeightFieldName());
      }
    }
  }

  /**
   * Setup meta data for the fields based on row structure
   * coming from previous step (if any)
   *
   * @return an array of ArffMeta
   */
  private ArffMeta[] setupArffMetas() {
    // try and set up from incoming fields from previous step
    StepMeta stepMeta = transMeta.findStep(stepname);
    
    if (stepMeta != null) {
      try {
        RowMetaInterface row = transMeta.getPrevStepFields(stepMeta);
        m_currentMeta.setupArffMeta(row);
        return m_currentMeta.getOutputFields();
      } catch (KettleException ex) {
        log.logError(toString(),
                     Messages.
                     getString("ArffOutputDialog.Log.UnableToFindInput"));
      }
    }
    return null;
  }
  
  private void setupWeightFieldComboBox() {
 // try and set up from incoming fields from previous step
    StepMeta stepMeta = transMeta.findStep(stepname);
    
    if (stepMeta != null) {
      try {
        RowMetaInterface rmi = transMeta.getPrevStepFields(stepMeta);
        m_weightFieldComboBox.removeAll();
        for (int i = 0; i < rmi.size(); i++) {
          ValueMetaInterface inField = rmi.getValueMeta(i);
          int fieldType = inField.getType();
          switch(fieldType) {
          case ValueMetaInterface.TYPE_NUMBER:
          case ValueMetaInterface.TYPE_INTEGER:
            m_weightFieldComboBox.add(inField.getName());
            break;
          }
        }
      } catch (KettleException ex) {
        log.logError(toString(), Messages.
            getString("ArffOutputDialog.Log.UnableToFindInput"));
      }
    }
  }

  /**
   * Convert integer Kettle type to descriptive String
   *
   * @param kettleType the Kettle data type
   * @return the Kettle data type as a String
   */
  private String getKettleTypeString(int kettleType) {
    switch (kettleType) {
    case ValueMetaInterface.TYPE_NUMBER:
      return "Number";
    case ValueMetaInterface.TYPE_INTEGER:
      return "Integer";
    case ValueMetaInterface.TYPE_BOOLEAN:
      return "Boolean";
    case ValueMetaInterface.TYPE_STRING:
      return "String";
    case ValueMetaInterface.TYPE_DATE:
      return "Date";
    } 
    return "Unknown";
  }

  /**
   * Convert String Kettle type to integer code
   *
   * @param kettleType the Kettle data type as a String
   * @return the integer Kettle type (as defined in ValueMetaInterface)
   */
  private int getKettleTypeInt(String kettleType) {
    if (kettleType.equalsIgnoreCase("Number")) {
      return ValueMetaInterface.TYPE_NUMBER;
    }
    if (kettleType.equalsIgnoreCase("Integer")) {
      return ValueMetaInterface.TYPE_INTEGER;
    }
    if (kettleType.equalsIgnoreCase("Boolean")) {
      return ValueMetaInterface.TYPE_BOOLEAN;
    }
    if (kettleType.equalsIgnoreCase("String")) {
      return ValueMetaInterface.TYPE_STRING;
    }
    if (kettleType.equalsIgnoreCase("Date")) {
      return ValueMetaInterface.TYPE_DATE;
    }
    return -1;
  }

  /**
   * Convert ARFF type to a descriptive String
   *
   * @param arffType the ARFF data type as defined in
   * ArffMeta
   * @return the ARFF data type as a String
   */
  private String getArffTypeString(int arffType) {
    if (arffType == ArffMeta.NUMERIC) {
      return "Numeric";
    } else if (arffType == ArffMeta.NOMINAL) {
      return "Nominal";
    }
    return "Date";
  }

  /**
   * Convert ARFF type to an integer code
   *
   * @param arffType the ARFF data type as a String
   * @return the ARFF data type as an integer (as defined
   * in ArffMeta
   */
  private int getArffTypeInt(String arffType) {
    if (arffType.equalsIgnoreCase("Numeric")) {
      return ArffMeta.NUMERIC;
    }
    if (arffType.equalsIgnoreCase("Nominal")) {
      return ArffMeta.NOMINAL;
    }
    return ArffMeta.DATE;
  }

  /**
   * Set up the character encodings combo box
   */
  private void setEncodings() {
    // Encoding of the text file:
    if (!m_gotEncodings) {
      m_gotEncodings = true;
      
      m_wEncoding.removeAll();
      List<Charset> values = 
        new ArrayList<Charset>(Charset.availableCharsets().values());
      for (int i = 0; i < values.size(); i++) {
        Charset charSet = (Charset)values.get(i);
        m_wEncoding.add(charSet.displayName());
      }
                                             
      // Now select the default!
      String defEncoding = 
        Const.getEnvironmentVariable("file.encoding", "UTF-8");
      int idx = Const.indexOfString(defEncoding, m_wEncoding.getItems() );
      if (idx >= 0) {
        m_wEncoding.select(idx);
      }
    }
  }

  private void cancel() {
    stepname = null;
    m_currentMeta.setChanged(changed);

    // revert to original state of the fields
    m_currentMeta.setOutputFields(m_originalMeta.getOutputFields());
    dispose();
  }
  
  private void ok() {
    if (Const.isEmpty(m_wStepname.getText())) {
      return;
    }

    stepname = m_wStepname.getText(); // return value

    /*    String arffOutName = 
          transMeta.environmentSubstitute(m_wFilename.getText()); */

    m_currentMeta.setFileName(m_wFilename.getText());

    String relName = m_wRelationName.getText();
    m_currentMeta.setRelationName(relName);
    
    String encoding = m_wEncoding.getText();
    m_currentMeta.setEncoding(encoding);
    String format = m_wFormat.getText();
    m_currentMeta.setFileFormat(format);

    int nrNonEmptyFields = m_wFields.nrNonEmpty();

    m_currentMeta.allocate(nrNonEmptyFields);

    for (int i = 0; i < nrNonEmptyFields; i++) {
      TableItem item = m_wFields.getNonEmpty(i);
      
      String fieldName = item.getText(1);
      int kettleType = getKettleTypeInt(item.getText(2));
      int arffType = getArffTypeInt(item.getText(3));

      m_currentMeta.getOutputFields()[i] = 
        new ArffMeta(fieldName, kettleType, arffType);
    }
    
    // weight field set?
    if (m_weightFieldCheckBox.getSelection() && 
        !Const.isEmpty(m_weightFieldComboBox.getText())) {
      m_currentMeta.setWeightFieldName(m_weightFieldComboBox.getText());
    } else {
      m_currentMeta.setWeightFieldName(null);
    }

    if (!m_originalMeta.equals(m_currentMeta)) {
      m_currentMeta.setChanged();
      changed = m_currentMeta.hasChanged();
    }
    
    dispose();
  }
}