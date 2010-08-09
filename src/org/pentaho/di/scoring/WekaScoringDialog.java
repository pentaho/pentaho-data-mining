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
 *    WekaScoringDialog.java
 *    Copyright 2007 Pentaho Corporation.  All rights reserved. 
 *
 */

package org.pentaho.di.scoring;

import java.io.File;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.Props;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStepMeta;
import org.pentaho.di.trans.step.StepDialogInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.ui.core.widget.TextVar;
import org.pentaho.di.ui.trans.step.BaseStepDialog;

import weka.core.Attribute;
import weka.core.Instances;
import weka.core.xml.XStream;

/**
 * The UI class for the WekaScoring transform
 *
 * @author Mark Hall (mhall{[at]}pentaho.org
 * @version 1.0
 */
public class WekaScoringDialog extends BaseStepDialog
  implements StepDialogInterface {

  /** various UI bits and pieces for the dialog */
  private Label m_wlStepname;
  private Text m_wStepname;
  private FormData m_fdlStepname;
  private FormData m_fdStepname;

  private FormData m_fdTabFolder;
  private FormData m_fdFileComp, m_fdFieldsComp, m_fdModelComp;

  // The tabs of the dialog
  private CTabFolder m_wTabFolder;
  private CTabItem m_wFileTab, m_wFieldsTab, m_wModelTab;

  // label for the file name field
  private Label m_wlFilename;
  
  // label for the lookup model file from field check box
  private Label m_wAcceptFileNameFromFieldCheckLab;
  
  // Checkbox for accept filename from field
  private Button m_wAcceptFileNameFromFieldCheckBox;
  
  // Label for accept filename from field TextVar
  private Label m_wAcceptFileNameFromFieldTextLab;
  
  // TextVar for file name field
  private TextVar m_wAcceptFileNameFromFieldText;
  
  // Label for cache models in memory
  private Label m_wCacheModelsLab;
  
  // Check box for caching models in memory
  private Button m_wCacheModelsCheckBox;

  // label for the output probabilities check box
  private Label m_wOutputProbsLab;

  // check box for output probabilities
  private Button m_wOutputProbs;

  // for the output probabilities check box
  private FormData m_fdlOutputProbs, m_fdOutputProbs;

  private Label m_wUpdateModelLab;
  private Button m_wUpdateModel;
  private FormData m_fdlUpdateModel, m_fdUpdateModel;

  // file name field
  private FormData m_fdlFilename, m_fdbFilename, m_fdFilename;

  // Browse file button
  private Button m_wbFilename;

  // Combines text field with widget to insert environment variable
  private TextVar m_wFilename;

  // label for the save file name field
  private Label m_wlSaveFilename;

  // save file name field
  private FormData m_fdlSaveFilename, m_fdbSaveFilename, m_fdSaveFilename;

  // Browse file button for saving incrementally updated model
  private Button m_wbSaveFilename;

  // Combines text field with widget to insert environment variable
  // for saving incrementally updated models
  private TextVar m_wSaveFilename;


  // file extension stuff
  /*  private Label m_wlExtension;
  private Text  m_wExtension;
  private FormData m_fdlExtension, m_fdExtension; */

  // the text area for the model
  private Text m_wModelText;
  private FormData m_fdModelText;

  // the text area for the fields mapping
  private Text m_wMappingText;
  private FormData m_fdMappingText;

  /**
   * meta data for the step. A copy is made so
   * that changes, in terms of choices made by the
   * user, can be detected.
   */
  private WekaScoringMeta m_currentMeta;
  private WekaScoringMeta m_originalMeta;

  public WekaScoringDialog(Shell parent, 
                           Object in, 
                           TransMeta tr, 
                           String sname) {

    super(parent, (BaseStepMeta) in, tr, sname);

    // The order here is important... 
    //m_currentMeta is looked at for changes
    m_currentMeta = (WekaScoringMeta) in;
    m_originalMeta = (WekaScoringMeta) m_currentMeta.clone();
  }

  /**
   * Open the dialog
   *
   * @return the step name
   */
  public String open() {
    
    // Make sure that all Weka packages have been loaded!
    weka.core.WekaPackageManager.loadPackages(false);
    
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
    shell.setText(Messages.getString("WekaScoringDialog.Shell.Title"));

    int middle = props.getMiddlePct();
    int margin = Const.MARGIN;
    
    // Stepname line
    m_wlStepname = new Label(shell, SWT.RIGHT);
    m_wlStepname.
      setText(Messages.getString("WekaScoringDialog.StepName.Label"));
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
      setText(Messages.getString("WekaScoringDialog.FileTab.TabTitle"));
    
    Composite wFileComp = new Composite(m_wTabFolder, SWT.NONE);
    props.setLook(wFileComp);
    
    FormLayout fileLayout = new FormLayout();
    fileLayout.marginWidth  = 3;
    fileLayout.marginHeight = 3;
    wFileComp.setLayout(fileLayout);
    
    // Filename line
    m_wlFilename = new Label(wFileComp, SWT.RIGHT);
    m_wlFilename.
      setText(Messages.getString("WekaScoringDialog.Filename.Label"));
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
    
    m_wUpdateModelLab = new Label(wFileComp, SWT.RIGHT);
    m_wUpdateModelLab.
      setText(Messages.getString("WekaScoringDialog.UpdateModel.Label"));
    props.setLook(m_wUpdateModelLab);
    m_fdlUpdateModel = new FormData();
    m_fdlUpdateModel.left = new FormAttachment(0, 0);
    m_fdlUpdateModel.top  = new FormAttachment(m_wFilename, margin);
    m_fdlUpdateModel.right= new FormAttachment(middle, -margin);
    m_wUpdateModelLab.setLayoutData(m_fdlUpdateModel);
    m_wUpdateModel = new Button(wFileComp, SWT.CHECK);
    props.setLook(m_wUpdateModel);
    m_fdUpdateModel = new FormData();
    m_fdUpdateModel.left = new FormAttachment(middle, 0);
    m_fdUpdateModel.top = new FormAttachment(m_wFilename, margin);
    m_fdUpdateModel.right = new FormAttachment(100, 0);
    m_wUpdateModel.setLayoutData(m_fdUpdateModel);
    m_wUpdateModel.addSelectionListener(new SelectionAdapter() {
        public void widgetSelected(SelectionEvent e) {
          m_currentMeta.setChanged();
          m_wbSaveFilename.setEnabled(m_wUpdateModel.getSelection());
          m_wSaveFilename.setEnabled(m_wUpdateModel.getSelection());
        }
      });

    //----------------------------

    // Save filename line
    m_wlSaveFilename = new Label(wFileComp, SWT.RIGHT);
    m_wlSaveFilename.
      setText(Messages.getString("WekaScoringDialog.SaveFilename.Label"));
    props.setLook(m_wlSaveFilename);
    m_fdlSaveFilename = new FormData();
    m_fdlSaveFilename.left = new FormAttachment(0, 0);
    m_fdlSaveFilename.top = new FormAttachment(m_wUpdateModel, margin);
    m_fdlSaveFilename.right = new FormAttachment(middle, -margin);
    m_wlSaveFilename.setLayoutData(m_fdlSaveFilename);

    // Save file browse button
    m_wbSaveFilename=new Button(wFileComp, SWT.PUSH| SWT.CENTER);
    props.setLook(m_wbSaveFilename);
    m_wbSaveFilename.setText(Messages.getString("System.Button.Browse"));
    m_fdbSaveFilename=new FormData();
    m_fdbSaveFilename.right = new FormAttachment(100, 0);
    m_fdbSaveFilename.top = new FormAttachment(m_wUpdateModel, 0);
    m_wbSaveFilename.setLayoutData(m_fdbSaveFilename);
    m_wbSaveFilename.setEnabled(false);
 
    // combined text field and env variable widget
    m_wSaveFilename = new TextVar(transMeta, wFileComp, 
                                  SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    props.setLook(m_wSaveFilename);
    m_wSaveFilename.addModifyListener(lsMod);
    m_fdSaveFilename=new FormData();
    m_fdSaveFilename.left = new FormAttachment(middle, 0);
    m_fdSaveFilename.top = new FormAttachment(m_wUpdateModel, margin);
    m_fdSaveFilename.right = new FormAttachment(m_wbSaveFilename, -margin);
    m_wSaveFilename.setLayoutData(m_fdSaveFilename);
    m_wSaveFilename.setEnabled(false);
            
    m_fdFileComp = new FormData();
    m_fdFileComp.left = new FormAttachment(0, 0);
    m_fdFileComp.top = new FormAttachment(0, 0);
    m_fdFileComp.right = new FormAttachment(100, 0);
    m_fdFileComp.bottom = new FormAttachment(100, 0);
    wFileComp.setLayoutData(m_fdFileComp);
    
    wFileComp.layout();
    m_wFileTab.setControl(wFileComp);
        
    m_wAcceptFileNameFromFieldCheckLab = new Label(wFileComp, SWT.RIGHT);
    m_wAcceptFileNameFromFieldCheckLab.
      setText(Messages.getString("WekaScoringDialog.AcceptFileNamesFromFieldCheck.Label"));
    props.setLook(m_wAcceptFileNameFromFieldCheckLab);
    FormData fdAcceptCheckLab = new FormData();
    fdAcceptCheckLab.left = new FormAttachment(0, 0);
    fdAcceptCheckLab.top = new FormAttachment(m_wSaveFilename, margin);
    fdAcceptCheckLab.right = new FormAttachment(middle, -margin);
    m_wAcceptFileNameFromFieldCheckLab.setLayoutData(fdAcceptCheckLab);
    m_wAcceptFileNameFromFieldCheckBox = new Button(wFileComp, SWT.CHECK);
    props.setLook(m_wAcceptFileNameFromFieldCheckBox);
    FormData fdAcceptCheckBox = new FormData();
    fdAcceptCheckBox.left = new FormAttachment(middle, 0);
    fdAcceptCheckBox.top = new FormAttachment(m_wSaveFilename, margin);
    fdAcceptCheckBox.right = new FormAttachment(100, 0);
    m_wAcceptFileNameFromFieldCheckBox.setLayoutData(fdAcceptCheckBox);
    
    m_wAcceptFileNameFromFieldCheckBox.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        m_currentMeta.setChanged();
        if (m_wAcceptFileNameFromFieldCheckBox.getSelection()) {
          m_wUpdateModel.setSelection(false);
          m_wUpdateModel.setEnabled(false);
          m_wSaveFilename.setText("");
          m_wlFilename.
            setText(Messages.getString("WekaScoringDialog.Default.Label"));
          if (!Const.isEmpty(m_wFilename.getText())) {
            // load - loadModel() will take care of storing it in
            // either the main or default model in the current meta
            loadModel();
          } else {
            // try and shift the main model (if non-null) over into the
            // default model in current meta
            m_currentMeta.setDefaultModel(m_currentMeta.getModel());
            m_currentMeta.setModel(null);
          }
          
        } else {
          if (!Const.isEmpty(m_wFilename.getText())) {
            // load - loadModel() will take care of storing it in
            // either the main or default model in the current meta
            loadModel();
          } else {
            // try and shift the default model (if non-null) over into the
            // main model in current meta
            m_currentMeta.setModel(m_currentMeta.getDefaultModel());
            m_currentMeta.setDefaultModel(null);
          }
          
          m_wCacheModelsCheckBox.setSelection(false);
          m_wlFilename.
            setText(Messages.getString("WekaScoringDialog.Filename.Label"));
        }

        m_wCacheModelsCheckBox.setEnabled(m_wAcceptFileNameFromFieldCheckBox.getSelection());
        m_wAcceptFileNameFromFieldText.setEnabled(m_wAcceptFileNameFromFieldCheckBox.getSelection());
        m_wbSaveFilename.setEnabled(!m_wAcceptFileNameFromFieldCheckBox.getSelection() && 
            m_wUpdateModel.getSelection());
        m_wSaveFilename.setEnabled(!m_wAcceptFileNameFromFieldCheckBox.getSelection() &&
            m_wUpdateModel.getSelection());
      }
    });
    
    //
    m_wAcceptFileNameFromFieldTextLab = new Label(wFileComp, SWT.RIGHT);
    m_wAcceptFileNameFromFieldTextLab.
      setText(Messages.getString("WekaScoringDialog.AcceptFileNamesFromField.Label"));
    props.setLook(m_wAcceptFileNameFromFieldTextLab);
    FormData fdAcceptLab = new FormData();
    fdAcceptLab.left = new FormAttachment(0, 0);
    fdAcceptLab.top = new FormAttachment(m_wAcceptFileNameFromFieldCheckBox, margin);
    fdAcceptLab.right = new FormAttachment(middle, -margin);
    m_wAcceptFileNameFromFieldTextLab.setLayoutData(fdAcceptLab);
    m_wAcceptFileNameFromFieldText = new TextVar(transMeta, wFileComp, 
        SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    props.setLook(m_wAcceptFileNameFromFieldText);
    m_wAcceptFileNameFromFieldText.addModifyListener(lsMod);
    FormData fdAcceptText = new FormData();
    fdAcceptText.left = new FormAttachment(middle, 0);
    fdAcceptText.top = new FormAttachment(m_wAcceptFileNameFromFieldCheckBox, margin);
    fdAcceptText.right = new FormAttachment(100, 0);
    m_wAcceptFileNameFromFieldText.setLayoutData(fdAcceptText);
    m_wAcceptFileNameFromFieldText.setEnabled(false);
    
    m_wCacheModelsLab = new Label(wFileComp, SWT.RIGHT);
    m_wCacheModelsLab.
      setText(Messages.getString("WekaScoringDialog.CacheModels.Label"));
    props.setLook(m_wCacheModelsLab  );
    FormData fdCacheLab = new FormData();
    fdCacheLab.left = new FormAttachment(0, 0);
    fdCacheLab.top = new FormAttachment(m_wAcceptFileNameFromFieldText, margin);
    fdCacheLab.right = new FormAttachment(middle, -margin);
    m_wCacheModelsLab.setLayoutData(fdCacheLab);
    //
    m_wCacheModelsCheckBox = new Button(wFileComp, SWT.CHECK);
    props.setLook(m_wCacheModelsCheckBox);
    FormData fdCacheCheckBox = new FormData();
    fdCacheCheckBox.left = new FormAttachment(middle, 0);
    fdCacheCheckBox.top = new FormAttachment(m_wAcceptFileNameFromFieldText, margin);
    fdCacheCheckBox.right = new FormAttachment(100, 0);
    m_wCacheModelsCheckBox.setLayoutData(fdCacheCheckBox);
    m_wCacheModelsCheckBox.setEnabled(false);
        

    m_wOutputProbsLab = new Label(wFileComp, SWT.RIGHT);
    m_wOutputProbsLab.
      setText(Messages.getString("WekaScoringDialog.OutputProbs.Label"));
    props.setLook(m_wOutputProbsLab);
    m_fdlOutputProbs = new FormData();
    m_fdlOutputProbs.left = new FormAttachment(0, 0);
    m_fdlOutputProbs.top  = new FormAttachment(m_wCacheModelsCheckBox, margin);
    m_fdlOutputProbs.right= new FormAttachment(middle, -margin);
    m_wOutputProbsLab.setLayoutData(m_fdlOutputProbs);
    m_wOutputProbs = new Button(wFileComp, SWT.CHECK);
    props.setLook(m_wOutputProbs);
    m_fdOutputProbs = new FormData();
    m_fdOutputProbs.left = new FormAttachment(middle, 0);
    m_fdOutputProbs.top = new FormAttachment(m_wCacheModelsCheckBox, margin);
    m_fdOutputProbs.right = new FormAttachment(100, 0);
    m_wOutputProbs.setLayoutData(m_fdOutputProbs);
    
    
    // Fields mapping tab
    m_wFieldsTab = new CTabItem(m_wTabFolder, SWT.NONE);
    m_wFieldsTab.
      setText(Messages.getString("WekaScoringDialog.FieldsTab.TabTitle"));
    
    FormLayout fieldsLayout = new FormLayout();
    fieldsLayout.marginWidth  = 3;
    fieldsLayout.marginHeight = 3;
    
    Composite wFieldsComp = new Composite(m_wTabFolder, SWT.NONE);
    props.setLook(wFieldsComp);
    wFieldsComp.setLayout(fieldsLayout);

    // body of tab to be a scrolling text area
    // to display the mapping
    m_wMappingText = new Text(wFieldsComp, 
                            SWT.MULTI | 
                            SWT.BORDER |
                            SWT.V_SCROLL |
                            SWT.H_SCROLL);
    m_wMappingText.setEditable(false);
    FontData fd = new FontData("Courier New", 10, SWT.NORMAL);
    m_wMappingText.setFont(new Font(getParent().getDisplay(), fd));
    //    m_wModelText.setText(stepname);
    props.setLook(m_wMappingText);
    // format the fields mapping text area
    m_fdMappingText = new FormData();
    m_fdMappingText.left = new FormAttachment(0, 0);
    m_fdMappingText.top = new FormAttachment(0, margin);
    m_fdMappingText.right = new FormAttachment(100, 0);
    m_fdMappingText.bottom = new FormAttachment(100, 0);
    m_wMappingText.setLayoutData(m_fdMappingText);

    
    m_fdFieldsComp = new FormData();
    m_fdFieldsComp.left = new FormAttachment(0, 0);
    m_fdFieldsComp.top = new FormAttachment(0, 0);
    m_fdFieldsComp.right = new FormAttachment(100, 0);
    m_fdFieldsComp.bottom = new FormAttachment(100, 0);
    wFieldsComp.setLayoutData(m_fdFieldsComp);

    wFieldsComp.layout();
    m_wFieldsTab.setControl(wFieldsComp);


    // Model display tab
    m_wModelTab = new CTabItem(m_wTabFolder, SWT.NONE);
    m_wModelTab.
      setText(Messages.getString("WekaScoringDialog.ModelTab.TabTitle"));
    
    FormLayout modelLayout = new FormLayout();
    modelLayout.marginWidth  = 3;
    modelLayout.marginHeight = 3;
    
    Composite wModelComp = new Composite(m_wTabFolder, SWT.NONE);
    props.setLook(wModelComp);
    wModelComp.setLayout(modelLayout);

    // body of tab to be a scrolling text area
    // to display the pre-learned model
    
    m_wModelText = new Text(wModelComp, 
                            SWT.MULTI | 
                            SWT.BORDER |
                            SWT.V_SCROLL |
                            SWT.H_SCROLL);
    m_wModelText.setEditable(false);
    fd = new FontData("Courier New", 10, SWT.NORMAL);
    m_wModelText.setFont(new Font(getParent().getDisplay(), fd));

    //    m_wModelText.setText(stepname);
    props.setLook(m_wModelText);
    // format the model text area
    m_fdModelText = new FormData();
    m_fdModelText.left = new FormAttachment(0, 0);
    m_fdModelText.top = new FormAttachment(0, margin);
    m_fdModelText.right = new FormAttachment(100, 0);
    m_fdModelText.bottom = new FormAttachment(100, 0);
    m_wModelText.setLayoutData(m_fdModelText);
    

    m_fdModelComp = new FormData();
    m_fdModelComp.left = new FormAttachment(0, 0);
    m_fdModelComp.top = new FormAttachment(0, 0);
    m_fdModelComp.right = new FormAttachment(100, 0);
    m_fdModelComp.bottom = new FormAttachment(100, 0);
    wModelComp.setLayoutData(m_fdModelComp);

    wModelComp.layout();
    m_wModelTab.setControl(wModelComp);
    int tempF = m_wModelText.getStyle();
    /*    if ((tempF & (SWT.WRAP)) > 0) {
      System.err.println("Wrap is turned on!!!!");
    } else {
      System.err.println("Wrap turned off");
      }*/




    m_fdTabFolder = new FormData();
    m_fdTabFolder.left  = new FormAttachment(0, 0);
    m_fdTabFolder.top   = new FormAttachment(m_wStepname, margin);
    m_fdTabFolder.right = new FormAttachment(100, 0);
    m_fdTabFolder.bottom= new FormAttachment(100, -50);
    m_wTabFolder.setLayoutData(m_fdTabFolder);

    // Buttons inherited from BaseStepDialog
    wOK = new Button(shell, SWT.PUSH);
    wOK.setText(Messages.getString("System.Button.OK"));
    
    wCancel=new Button(shell, SWT.PUSH);
    wCancel.setText(Messages.getString("System.Button.Cancel"));
    
    setButtonPositions(new Button[] { wOK, wCancel }, 
                       margin, m_wTabFolder);

    // Add listeners
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

 
    // Whenever something changes, set the tooltip to the expanded version:
    m_wFilename.addModifyListener(new ModifyListener() {
        public void modifyText(ModifyEvent e) {
          m_wFilename.
            setToolTipText(transMeta.
                           environmentSubstitute(m_wFilename.getText()));
        }
      });

    m_wSaveFilename.addModifyListener(new ModifyListener() {
        public void modifyText(ModifyEvent e) {
          m_wSaveFilename.setToolTipText(transMeta.
                                         environmentSubstitute(m_wSaveFilename.getText()));
        }
      });

    // listen to the file name text box and try to load a model
    // if the user presses enter
    m_wFilename.addSelectionListener(new SelectionAdapter() {
        public void widgetDefaultSelected(SelectionEvent e) {
          if (!loadModel()) {
            log.logError(Messages.getString("WekaScoringDialog.Log.FileLoadingError"));
            //            System.err.println("Problem loading model file!");
          }
        }
      });

    m_wbFilename.addSelectionListener(
       new SelectionAdapter() {
         public void widgetSelected(SelectionEvent e) {
           FileDialog dialog = new FileDialog(shell, SWT.OPEN);
           String[] extensions = null;
           String[] filterNames = null;
           if (XStream.isPresent()) {
             extensions = new String[4];
             filterNames = new String[4];
             extensions[0] = "*.model";
             filterNames[0] = 
               Messages.getString("WekaScoringDialog.FileType.ModelFileBinary");
             extensions[1] = "*.xstreammodel";
             filterNames[1] = 
               Messages.getString("WekaScoringDialog.FileType.ModelFileXML");
             extensions[2] = "*.xml";
             filterNames[2] = 
               Messages.getString("WekaScoringDialog.FileType.ModelFilePMML");
             extensions[3] = "*";
             filterNames[3] = 
               Messages.getString("System.FileType.AllFiles");
           } else {
             extensions = new String[3];
             filterNames = new String[3];
             extensions[0] = "*.model";
             filterNames[0] = 
               Messages.getString("WekaScoringDialog.FileType.ModelFileBinary");
             extensions[1] = "*.xml";
             filterNames[1] = 
               Messages.getString("WekaScoringDialog.FileType.ModelFilePMML");
             extensions[2] = "*";
             filterNames[2] = 
               Messages.getString("System.FileType.AllFiles");
           }
           dialog.setFilterExtensions(extensions);
           if (m_wFilename.getText() != null) {
             dialog.setFileName(transMeta.
                                environmentSubstitute(m_wFilename.
                                                      getText()));
           }
           dialog.setFilterNames(filterNames);

           if (dialog.open() != null) {
             /*             String extension = m_wExtension.getText();
             if (extension != null && 
                 dialog.getFileName() != null &&
                 dialog.getFileName().endsWith("." + extension)) {
               // The extension is filled in and matches the end 
               // of the selected file => Strip off the extension. */
             //             String fileName = dialog.getFileName();
             /*               m_wFilename.
                              setText(dialog.getFilterPath()
                              + System.getProperty("file.separator")
                              + fileName.substring(0, fileName.length() - 
                                              (extension.length() + 1)));
                                              } else { */
             m_wFilename.setText(dialog.getFilterPath()
                                 + System.getProperty("file.separator")
                                 + dialog.getFileName());
             //             }

             // try to load model file and display model
             if (!loadModel()) {
               log.logError(Messages.getString("WekaScoringDialog.Log.FileLoadingError"));
               //               System.err.println("Problem loading model file!");
             }
           }
         }
       });



    m_wbSaveFilename.addSelectionListener(
       new SelectionAdapter() {
         public void widgetSelected(SelectionEvent e) {
           FileDialog dialog = new FileDialog(shell, SWT.SAVE);
           String[] extensions = null;
           String[] filterNames = null;
           if (XStream.isPresent()) {
             extensions = new String[3];
             filterNames = new String[3];
             extensions[0] = "*.model";
             filterNames[0] = 
               Messages.getString("WekaScoringDialog.FileType.ModelFileBinary");
             extensions[1] = "*.xstreammodel";
             filterNames[1] = 
               Messages.getString("WekaScoringDialog.FileType.ModelFileXML");
             extensions[2] = "*";
             filterNames[2] = 
               Messages.getString("System.FileType.AllFiles");
           } else {
             extensions = new String[2];
             extensions[0] = "*.model";
             filterNames[0] = 
               Messages.getString("WekaScoringDialog.FileType.ModelFileBinary");
             extensions[1] = "*";
             filterNames[1] = 
               Messages.getString("System.FileType.AllFiles");
           }
           dialog.setFilterExtensions(extensions);
           if (m_wSaveFilename.getText() != null) {
             dialog.setFileName(transMeta.
                                environmentSubstitute(m_wFilename.
                                                      getText()));
           }
           dialog.setFilterNames(filterNames);

           if (dialog.open() != null) {

             m_wSaveFilename.setText(dialog.getFilterPath()
                                     + System.getProperty("file.separator")
                                     + dialog.getFileName());
           }
         }
       });




    m_wTabFolder.setSelection(0);

    // Set the shell size, based upon previous time...
    setSize();

    getData();

    shell.open();
    while (!shell.isDisposed()) {
      if (!display.readAndDispatch()) {
        display.sleep();
      }
    }

    return stepname;
  }

  /**
   * Load the model.
   */
  private boolean loadModel() {
    String filename = m_wFilename.getText();
    if (Const.isEmpty(filename)) {
      return false;
    }
    String modName = transMeta.environmentSubstitute(filename);
    File modelFile = null;
    if (modName.startsWith("file:")) {
      try {
        modName = modName.replace(" ", "%20");
        modelFile = new File(new java.net.URI(modName));
    } catch (Exception ex) {
        //      System.err.println("Malformed URI");
        log.logError(Messages.getString("WekaScoringDialog.Log.MalformedURI"));
        return false;
      }
    } else {
      modelFile = new File(modName);
    }
    boolean success = false;

    if (!Const.isEmpty(filename) && modelFile.exists()) {
      try {
        WekaScoringModel tempM = 
          WekaScoringData.loadSerializedModel(modelFile, log);
        m_wModelText.setText(tempM.toString());
        
        if (m_wAcceptFileNameFromFieldCheckBox.getSelection()) {
          m_currentMeta.setDefaultModel(tempM);
        } else {
          m_currentMeta.setModel(tempM);
        }

        checkAbilityToProduceProbabilities(tempM);
        checkAbilityToUpdateModelIncrementally(tempM);

        // see if we can find a previous step and set up the
        // mappings
        mappingString(tempM);
        success = true;
      } catch (Exception ex) {
        log.logError(Messages.getString("WekaScoringDialog.Log.FileLoadingError"));
        //      System.err.println("Problem loading model file...");
      }
    }

    return success;
  }

  /**
   * Build a string that shows the mappings between Weka
   * attributes and incoming Kettle fields.
   *
   * @param model a <code>WekaScoringModel</code> value
   */
  private void mappingString(WekaScoringModel model) {

    try {
      StepMeta stepMeta = transMeta.findStep(stepname);
      if (stepMeta != null) {
        RowMetaInterface rowM = 
          transMeta.getPrevStepFields(stepMeta);
        Instances header = model.getHeader();
        m_currentMeta.mapIncomingRowMetaData(header, rowM);
        int [] mappings = m_currentMeta.getMappingIndexes();

        StringBuffer result 
          = new StringBuffer(header.numAttributes() * 10);

        int maxLength = 0;
        for (int i = 0; i < header.numAttributes(); i++) {
          if (header.attribute(i).name().length() > maxLength) {
            maxLength = header.attribute(i).name().length();
          }
        }
        maxLength += 12; // length of " (nominal)"/" (numeric)"

        int minLength = 16; // "Model attributes".length()
        String headerS = "Model attributes";
        String sep = "----------------";

        if (maxLength < minLength) {
          maxLength = minLength;
        }
        headerS = getFixedLengthString(headerS, ' ', maxLength);
        sep = getFixedLengthString(sep, '-', maxLength);
        sep += "\t    ----------------\n";
        headerS += "\t    Incoming fields\n";
        result.append(headerS);
        result.append(sep);

        for (int i = 0; i < header.numAttributes(); i++) {
          Attribute temp = header.attribute(i);
          String attName = "("
            + ((temp.isNumeric())
               ? "numeric)"
               : "nominal)") 
            + " " + temp.name();
          attName = getFixedLengthString(attName, ' ', maxLength);
          attName +=  "\t--> ";
          result.append(attName);
          String inFieldNum = "";
          if (mappings[i] == WekaScoringData.NO_MATCH) {
            inFieldNum += "- ";
            result.append(inFieldNum + "missing (no match)\n");
          } else if (mappings[i] == WekaScoringData.TYPE_MISMATCH) {
            inFieldNum += (rowM.indexOfValue(temp.name()) + 1) +" ";
            result.append(inFieldNum + "missing (type mis-match)\n");
          } else {
            ValueMetaInterface tempField = rowM.getValueMeta(mappings[i]);
            String fieldName = "" + (mappings[i] + 1) + " (";
            if (tempField.isBoolean()) {
              fieldName += "boolean)";
            } else if (tempField.isNumeric()) {
              fieldName += "numeric)";
            } else if (tempField.isString()) {
              fieldName += "string)";
            }
            fieldName += " " + tempField.getName();
            result.append(fieldName + "\n");
          }
        }
        
        // set the text of the text area in the Mappings tab
        m_wMappingText.setText(result.toString());
      }
    } catch (KettleException e) {
      log.logError(Messages.getString("WekaScoringDialog.Log.UnableToFindInput"));
      return;
    }
  }

  /**
   * Grab data out of the step meta object
   */
  public void getData() {
    
    if (m_currentMeta.getFileNameFromField()) {
      m_wAcceptFileNameFromFieldCheckBox.setSelection(true);
      m_wCacheModelsCheckBox.setEnabled(true);
      m_wSaveFilename.setEnabled(false);
      m_wbSaveFilename.setEnabled(false);
      m_wSaveFilename.setText("");
      m_wUpdateModel.setEnabled(false);
      if (!Const.isEmpty(m_currentMeta.getFieldNameToLoadModelFrom())) {
        m_wAcceptFileNameFromFieldText.
          setText(m_currentMeta.getFieldNameToLoadModelFrom());
      }
      m_wAcceptFileNameFromFieldText.setEnabled(true);
      
      m_wCacheModelsCheckBox.setSelection(m_currentMeta.getCacheLoadedModels());
      m_wlFilename.
        setText(Messages.getString("WekaScoringDialog.Default.Label"));
    }
    
    if (m_currentMeta.getSerializedModelFileName() != null) {
      m_wFilename.setText(m_currentMeta.getSerializedModelFileName());
    }

    m_wOutputProbs.setSelection(m_currentMeta.getOutputProbabilities());
    
    if (!m_currentMeta.getFileNameFromField()) {
      m_wUpdateModel.setSelection(m_currentMeta.getUpdateIncrementalModel());
    }

    if (m_wUpdateModel.getSelection()) {
      if (m_currentMeta.getSavedModelFileName() != null) {
        m_wSaveFilename.setText(m_currentMeta.getSavedModelFileName());
      }
    }
    
    // Grab model if it is available (and we are not reading model file
    // names from a field in the incoming data
  //  if (!m_wAcceptFileNameFromFieldCheckBox.getSelection()) {
      WekaScoringModel tempM = (m_currentMeta.getFileNameFromField()) 
        ? m_currentMeta.getDefaultModel() : m_currentMeta.getModel();
      if (tempM != null) {
        m_wModelText.setText(tempM.toString());

        // Grab mappings if available
        mappingString(tempM);
        checkAbilityToProduceProbabilities(tempM);
        checkAbilityToUpdateModelIncrementally(tempM);
      } else {
        // try loading the model
        loadModel();
      }
    //}
  }

  private void checkAbilityToUpdateModelIncrementally(WekaScoringModel tempM) {
    if (!tempM.isUpdateableModel()) {
      m_wUpdateModel.setSelection(false);
      m_wUpdateModel.setEnabled(false);
      // disable the save field and button
      m_wbSaveFilename.setEnabled(false);
      m_wSaveFilename.setEnabled(false);
      // clear the text field
      m_wSaveFilename.setText("");

    } else if (!m_wAcceptFileNameFromFieldCheckBox.getSelection()) {
      m_wUpdateModel.setEnabled(true);
      // enable the save field and button if the check box is selected
      if(m_wUpdateModel.getSelection()) {
        m_wbSaveFilename.setEnabled(true);
        m_wSaveFilename.setEnabled(true);
      }
    }
  }

  private void checkAbilityToProduceProbabilities(WekaScoringModel tempM) {
    // take a look at the model-type and then the class
    // attribute (if set and if necessary) in order
    // to determine whether to disable/enable the
    // output probabilities checkbox
    if (!tempM.isSupervisedLearningModel()) {
      // now, does the clusterer produce probabilities?
      if (((WekaScoringClusterer)tempM).canProduceProbabilities()) {
        m_wOutputProbs.setEnabled(true);
      } else {
        m_wOutputProbs.setSelection(false);
        m_wOutputProbs.setEnabled(false);
      }
    } else {
      // take a look at the header and disable the output
      // probs checkbox if there is a class attribute set
      // and the class is numeric
      Instances header = tempM.getHeader();
      if (header.classIndex() >= 0) {
        if (header.classAttribute().isNumeric()) {
          m_wOutputProbs.setSelection(false);
          m_wOutputProbs.setEnabled(false);
        } else {
          m_wOutputProbs.setEnabled(true);
        }
      }
    }
  }

  private void cancel() {
    stepname = null;
    m_currentMeta.setChanged(changed);
    //    m_currentMeta.setModel(null);
    // revert to original model
    WekaScoringModel temp = (m_originalMeta.getFileNameFromField())
      ? m_originalMeta.getDefaultModel()
      : m_originalMeta.getModel();
    m_currentMeta.setModel(temp);
    dispose();
  }

  private void ok() {
    if (Const.isEmpty(m_wStepname.getText())) {
      return;
    }

    stepname = m_wStepname.getText(); // return value
    
    m_currentMeta.setFileNameFromField(m_wAcceptFileNameFromFieldCheckBox.getSelection());
    
//    if (!m_wAcceptFileNameFromFieldCheckBox.getSelection()) {
      if (!Const.isEmpty(m_wFilename.getText())) {
        /*      String modFname = transMeta.
              environmentSubstitute(m_wFilename.getText()); */
        m_currentMeta.setSerializedModelFileName(m_wFilename.getText());    
      } else {
        m_currentMeta.setSerializedModelFileName(null);
      }
//    } else {
      if (!Const.isEmpty(m_wAcceptFileNameFromFieldText.getText())) {
        m_currentMeta.setFieldNameToLoadModelFrom(m_wAcceptFileNameFromFieldText.getText());
      }
      m_currentMeta.setCacheLoadedModels(m_wCacheModelsCheckBox.getSelection());
    //}
    
    m_currentMeta.setOutputProbabilities(m_wOutputProbs.getSelection());
    m_currentMeta.setUpdateIncrementalModel(m_wUpdateModel.getSelection());

    if (m_currentMeta.getUpdateIncrementalModel()) {
      if (!Const.isEmpty(m_wSaveFilename.getText())) {
        /*          String modSaveFname = transMeta.
                    environmentSubstitute(m_wSaveFilename.getText()); */
        m_currentMeta.setSavedModelFileName(m_wSaveFilename.getText());
      } else {
        // make sure that save filename is empty
        m_currentMeta.setSavedModelFileName("");
      }
    }

    if (!m_originalMeta.equals(m_currentMeta)) {
      m_currentMeta.setChanged();
      changed = m_currentMeta.hasChanged();
    }
    
    dispose();
  }
  
  
  /**
   * Helper method to pad/truncate strings
   *
   * @param s String to modify
   * @param pad character to pad with
   * @param len length of final string
   * @return final String
   */
  private String getFixedLengthString(String s, char pad, int len) {

    String padded = null;
    if (len <= 0) {
      return s;
    }
    // truncate?
    if (s.length() >= len) {
      return s.substring(0, len);
    } else {
      char [] buf = new char[len - s.length()];
      for (int j = 0; j < len - s.length(); j++) {
        buf[j] = pad;
      }
      padded = s + new String(buf);
    }

    return padded;
  }
}