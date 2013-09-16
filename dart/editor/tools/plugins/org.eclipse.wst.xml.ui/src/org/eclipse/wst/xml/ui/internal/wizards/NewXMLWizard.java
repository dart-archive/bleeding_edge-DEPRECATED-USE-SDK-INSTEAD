/*******************************************************************************
 * Copyright (c) 2001, 2008 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation David Schneider, david.schneider@unisys.com - [142500] WTP properties pages fonts
 * don't follow Eclipse preferences
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.wizards;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;
import org.eclipse.wst.common.uriresolver.internal.util.URIHelper;
import org.eclipse.wst.xml.core.internal.XMLCorePlugin;
import org.eclipse.wst.xml.core.internal.catalog.provisional.ICatalogEntry;
import org.eclipse.wst.xml.core.internal.contentmodel.CMDocument;
import org.eclipse.wst.xml.core.internal.contentmodel.CMElementDeclaration;
import org.eclipse.wst.xml.core.internal.contentmodel.CMNamedNodeMap;
import org.eclipse.wst.xml.core.internal.contentmodel.util.DOMContentBuilder;
import org.eclipse.wst.xml.core.internal.contentmodel.util.NamespaceInfo;
import org.eclipse.wst.xml.core.internal.preferences.XMLCorePreferenceNames;
import org.eclipse.wst.xml.ui.internal.Logger;
import org.eclipse.wst.xml.ui.internal.dialogs.NamespaceInfoErrorHelper;
import org.eclipse.wst.xml.ui.internal.dialogs.SelectFileOrXMLCatalogIdPanel;
import org.eclipse.wst.xml.ui.internal.editor.XMLEditorPluginImageHelper;
import org.eclipse.wst.xml.ui.internal.editor.XMLEditorPluginImages;
import org.eclipse.wst.xml.ui.internal.nsedit.CommonEditNamespacesDialog;

import com.ibm.icu.text.Collator;

public class NewXMLWizard extends NewModelWizard {
  protected static final int CREATE_FROM_DTD = 0;
  protected static final int CREATE_FROM_XSD = 1;
  protected static final int CREATE_FROM_SCRATCH = 2;

  protected static final String[] createFromRadioButtonLabel = {
      XMLWizardsMessages._UI_RADIO_XML_FROM_DTD, XMLWizardsMessages._UI_RADIO_XML_FROM_SCHEMA,
      XMLWizardsMessages._UI_RADIO_XML_FROM_SCRATCH};

  protected static final String[] filePageFilterExtensions = {".xml"}; //$NON-NLS-1$
  protected static final String[] browseXSDFilterExtensions = {".xsd"}; //$NON-NLS-1$
  protected static final String[] browseDTDFilterExtensions = {".dtd"}; //$NON-NLS-1$

  protected static final int OPTIONAL_ELEMENT_DEPTH_LIMIT_DEFAULT_VALUE = 2;

  protected static final long XML_EDITOR_FILE_SIZE_LIMIT = 26214400; // 25 mb

  protected NewFilePage newFilePage;
  /**
   * @deprecated clients should not be allowed to change start page
   */
  protected StartPage startPage;
  private StartPage fCreateXMLFromWizardPage;
  protected SelectGrammarFilePage selectGrammarFilePage;
  protected SelectRootElementPage selectRootElementPage;

  protected String cmDocumentErrorMessage;

  protected NewXMLGenerator generator;
  private NewXMLTemplatesWizardPage fNewXMLTemplatesWizardPage;

  public NewXMLWizard() {
    setWindowTitle(XMLWizardsMessages._UI_WIZARD_CREATE_NEW_TITLE);
    ImageDescriptor descriptor = XMLEditorPluginImageHelper.getInstance().getImageDescriptor(
        XMLEditorPluginImages.IMG_WIZBAN_GENERATEXML);
    setDefaultPageImageDescriptor(descriptor);
    generator = new NewXMLGenerator();
  }

  public NewXMLWizard(IFile file, CMDocument cmDocument) {
    this();

    generator.setGrammarURI(URIHelper.getPlatformURI(file));
    generator.setCMDocument(cmDocument);
  }

  public static void showDialog(Shell shell, IFile file, IStructuredSelection structuredSelection) {
    String[] errorInfo = new String[2];
    // (cs) the URI argument to createCMDocument needs to be a fully
    // qualified URI
    //
    CMDocument cmDocument = NewXMLGenerator.createCMDocument(URIHelper.getPlatformURI(file),
        errorInfo);
    if (errorInfo[0] == null) {
      NewXMLWizard wizard = new NewXMLWizard(file, cmDocument);
      wizard.init(PlatformUI.getWorkbench(), structuredSelection);
      wizard.setNeedsProgressMonitor(true);
      WizardDialog dialog = new WizardDialog(shell, wizard);
      dialog.create();
      dialog.getShell().setText(XMLWizardsMessages._UI_DIALOG_NEW_TITLE);
      dialog.setBlockOnOpen(true);
      dialog.open();
    } else {
      MessageDialog.openInformation(shell, errorInfo[0], errorInfo[1]);
    }
  }

  public void addPages() {
    String grammarURI = generator.getGrammarURI();

    // new file page
    newFilePage = new NewFilePage(fSelection);
    newFilePage.setTitle(XMLWizardsMessages._UI_WIZARD_CREATE_XML_FILE_HEADING);
    newFilePage.setDescription(XMLWizardsMessages._UI_WIZARD_CREATE_XML_FILE_EXPL);
    newFilePage.defaultName = (grammarURI != null)
        ? URIHelper.removeFileExtension(URIHelper.getLastSegment(grammarURI)) : "NewFile"; //$NON-NLS-1$
    Preferences preference = XMLCorePlugin.getDefault().getPluginPreferences();
    String ext = preference.getString(XMLCorePreferenceNames.DEFAULT_EXTENSION);
    newFilePage.defaultFileExtension = "." + ext; //$NON-NLS-1$
    newFilePage.filterExtensions = filePageFilterExtensions;
    addPage(newFilePage);

    if (grammarURI == null) {
      // create xml from page
      fCreateXMLFromWizardPage = new StartPage("StartPage", createFromRadioButtonLabel) //$NON-NLS-1$
      {
        public void createControl(Composite parent) {
          super.createControl(parent);
        }

        public void setVisible(boolean visible) {
          super.setVisible(visible);
          getRadioButtonAtIndex(getCreateMode()).setSelection(true);
          getRadioButtonAtIndex(getCreateMode()).setFocus();

          // Set the help context for each button
          PlatformUI.getWorkbench().getHelpSystem().setHelp(
              fCreateXMLFromWizardPage.getRadioButtonAtIndex(0),
              IXMLWizardHelpContextIds.XML_NEWWIZARD_CREATEXML1_HELPID);
          PlatformUI.getWorkbench().getHelpSystem().setHelp(
              fCreateXMLFromWizardPage.getRadioButtonAtIndex(1),
              IXMLWizardHelpContextIds.XML_NEWWIZARD_CREATEXML2_HELPID);
          PlatformUI.getWorkbench().getHelpSystem().setHelp(
              fCreateXMLFromWizardPage.getRadioButtonAtIndex(2),
              IXMLWizardHelpContextIds.XML_NEWWIZARD_CREATEXML3_HELPID);
        }
      };

      fCreateXMLFromWizardPage.setTitle(XMLWizardsMessages._UI_WIZARD_CREATE_XML_HEADING);
      fCreateXMLFromWizardPage.setDescription(XMLWizardsMessages._UI_WIZARD_CREATE_XML_EXPL);
      addPage(fCreateXMLFromWizardPage);
    }

    // selectGrammarFilePage
    selectGrammarFilePage = new SelectGrammarFilePage();
    addPage(selectGrammarFilePage);

    // select root element page
    selectRootElementPage = new SelectRootElementPage();
    selectRootElementPage.setTitle(XMLWizardsMessages._UI_WIZARD_SELECT_ROOT_HEADING);
    selectRootElementPage.setDescription(XMLWizardsMessages._UI_WIZARD_SELECT_ROOT_EXPL);
    addPage(selectRootElementPage);

    // from "scratch"
    fNewXMLTemplatesWizardPage = new NewXMLTemplatesWizardPage();
    addPage(fNewXMLTemplatesWizardPage);
  }

  public IWizardPage getStartingPage() {
    WizardPage result = null;
    if (startPage != null) {
      result = startPage;
    } else {
      result = newFilePage;
    }
    return result;
  }

  public int getCreateMode() {
    String grammarURI = generator.getGrammarURI();

    int result = CREATE_FROM_SCRATCH;
    if (grammarURI != null) {
      if (grammarURI.endsWith(".dtd")) //$NON-NLS-1$
      {
        result = CREATE_FROM_DTD;
      } else if (grammarURI.endsWith(".xsd")) //$NON-NLS-1$
      {
        result = CREATE_FROM_XSD;
      }
    } else if (fCreateXMLFromWizardPage != null) {
      int selectedIndex = fCreateXMLFromWizardPage.getSelectedRadioButtonIndex();
      if (selectedIndex != -1) {
        result = selectedIndex;
      }
    }
    return result;
  }

  public IWizardPage getNextPage(IWizardPage currentPage) {
    WizardPage nextPage = null;
    if (currentPage == startPage) {
      nextPage = newFilePage;
    } else if (currentPage == newFilePage) {
      if (generator.getGrammarURI() == null)
        nextPage = fCreateXMLFromWizardPage;
      else
        nextPage = selectRootElementPage;
    } else if (currentPage == fCreateXMLFromWizardPage) {
      if (getCreateMode() == CREATE_FROM_SCRATCH) {
        nextPage = fNewXMLTemplatesWizardPage;
      } else if (generator.getGrammarURI() == null) {
        nextPage = selectGrammarFilePage;
      } else {
        nextPage = selectRootElementPage;
      }
    } else if (currentPage == selectGrammarFilePage) {
      nextPage = selectRootElementPage;
    }
    return nextPage;
  }

  public boolean canFinish() {
    boolean result = false;

    IWizardPage currentPage = getContainer().getCurrentPage();
    // can finish on: new file page, create from & template page if creating from scratch, select root element page
    if ((currentPage == newFilePage && generator.getGrammarURI() == null)
        || (fCreateXMLFromWizardPage != null && fCreateXMLFromWizardPage.getSelectedRadioButtonIndex() == CREATE_FROM_SCRATCH)
        || (currentPage == selectRootElementPage)) {
      result = currentPage.isPageComplete();
    }
    return result;
  }

  public boolean performFinish() {
    boolean result = super.performFinish();
    // save user options for next use
    fNewXMLTemplatesWizardPage.saveLastSavedPreferences();

    String fileName = null;
    try {

      String[] namespaceErrors = generator.getNamespaceInfoErrors();
      if (namespaceErrors != null) {
        String title = namespaceErrors[0];
        String message = namespaceErrors[1];
        result = MessageDialog.openQuestion(getShell(), title, message);
      }

      if (result) {
        fileName = newFilePage.getFileName();
        if ((new Path(fileName)).getFileExtension() == null) {
          newFilePage.setFileName(fileName.concat(newFilePage.defaultFileExtension));
        }

        final IFile newFile = newFilePage.createNewFile();
        final String xmlFileName = newFile.getLocation().toOSString();
        final String grammarFileName = fileName;

        if (getContainer().getCurrentPage() == selectRootElementPage) {

          int limit = selectRootElementPage.getOptionalElementDepthLimit();
          generator.setOptionalElementDepthLimit(limit);
          setNeedsProgressMonitor(true);
          getContainer().run(true, false, new IRunnableWithProgress() {
            public void run(IProgressMonitor progressMonitor) throws InvocationTargetException,
                InterruptedException {
              progressMonitor.beginTask(XMLWizardsMessages._UI_WIZARD_GENERATING_XML_DOCUMENT,
                  IProgressMonitor.UNKNOWN);
              try {
                generator.createXMLDocument(newFile, xmlFileName);
              } catch (Exception exception) {
                Logger.logException(
                    "Exception completing New XML wizard " + grammarFileName, exception); //$NON-NLS-1$
              }
              progressMonitor.done();
            }
          });
        } else {
          // put template contents into file
          String templateString = fNewXMLTemplatesWizardPage.getTemplateString();
          generator.createTemplateXMLDocument(newFile, templateString);
        }
        newFile.refreshLocal(IResource.DEPTH_ONE, null);
        IWorkbenchWindow workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        BasicNewResourceWizard.selectAndReveal(newFile, workbenchWindow);
        openEditor(newFile);
      }
    } catch (Exception e) {
      Logger.logException("Exception completing New XML wizard " + fileName, e); //$NON-NLS-1$
    }
    return result;
  }

  public void openEditor(IFile file) {
    long length = 0;
    IPath location = file.getLocation();
    if (location != null) {
      File localFile = location.toFile();
      length = localFile.length();
    }
    if (length < XML_EDITOR_FILE_SIZE_LIMIT) {
      // Open editor on new file.
      String editorId = null;
      try {
        IEditorDescriptor editor = PlatformUI.getWorkbench().getEditorRegistry().getDefaultEditor(
            file.getLocation().toOSString(), file.getContentDescription().getContentType());
        if (editor != null) {
          editorId = editor.getId();
        }
      } catch (CoreException e1) {
        // editor id could not be retrieved, so we can not open editor
        return;
      }
      IWorkbenchWindow dw = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
      try {
        if (dw != null) {
          IWorkbenchPage page = dw.getActivePage();
          if (page != null) {
            page.openEditor(new FileEditorInput(file), editorId, true);
          }
        }
      } catch (PartInitException e) {
        // editor can not open for some reason
        return;
      }
    }
  }

  protected String getDefaultSystemId() {
    String relativePath = "platform:/resource/" + newFilePage.getContainerFullPath().toString() + "/dummy"; //$NON-NLS-1$ //$NON-NLS-2$
    return URIHelper.getRelativeURI(generator.getGrammarURI(), relativePath);
  }

  /**
   * SelectGrammarFilePage
   */
  class SelectGrammarFilePage extends WizardPage {
    protected SelectFileOrXMLCatalogIdPanel panel;

    SelectGrammarFilePage() {
      super("SelectGrammarFilePage"); //$NON-NLS-1$
    }

    public void createControl(Composite parent) {
      Composite composite = new Composite(parent, SWT.NONE);
      PlatformUI.getWorkbench().getHelpSystem().setHelp(composite,
          IXMLWizardHelpContextIds.XML_NEWWIZARD_SELECTSOURCE_HELPID);
      composite.setLayout(new GridLayout());
      composite.setLayoutData(new GridData(GridData.FILL_BOTH));
      setControl(composite);

      panel = new SelectFileOrXMLCatalogIdPanel(composite);
      panel.setLayoutData(new GridData(GridData.FILL_BOTH));

      SelectFileOrXMLCatalogIdPanel.Listener listener = new SelectFileOrXMLCatalogIdPanel.Listener() {
        public void completionStateChanged() {
          updateErrorMessage();
        }
      };
      panel.setListener(listener);
      Dialog.applyDialogFont(parent);
    }

    public void setVisible(boolean visible) {
      super.setVisible(visible);
      if (visible) {
        if (getCreateMode() == CREATE_FROM_DTD) {
          setTitle(XMLWizardsMessages._UI_WIZARD_SELECT_DTD_FILE_TITLE);
          setDescription(XMLWizardsMessages._UI_WIZARD_SELECT_DTD_FILE_DESC);
          panel.setFilterExtensions(browseDTDFilterExtensions);
        } else {
          setTitle(XMLWizardsMessages._UI_WIZARD_SELECT_XSD_FILE_TITLE);
          setDescription(XMLWizardsMessages._UI_WIZARD_SELECT_XSD_FILE_DESC);
          panel.setFilterExtensions(browseXSDFilterExtensions);
        }
        generator.setGrammarURI(null);
        generator.setCMDocument(null);
        cmDocumentErrorMessage = null;
      }
      panel.setVisibleHelper(visible);
    }

    public String getURI() {
      String uri = panel.getXMLCatalogURI();
      if (uri == null) {
        IFile file = panel.getFile();
        if (file != null) {
          uri = URIHelper.getPlatformURI(file);
        }
      }
      return uri;
    }

    public boolean isPageComplete() {
      return (getURI() != null) && (getErrorMessage() == null);
    }

    public String getXMLCatalogId() {
      return panel.getXMLCatalogId();
    }

    public ICatalogEntry getXMLCatalogEntry() {
      return panel.getXMLCatalogEntry();
    }

    public String computeErrorMessage() {
      String errorMessage = null;
      String uri = getURI();
      if (uri != null) {
        if (!URIHelper.isReadableURI(uri, false)) {
          errorMessage = XMLWizardsMessages._UI_LABEL_ERROR_CATALOG_ENTRY_INVALID;
        }
      }
      return errorMessage;
    }

    public void updateErrorMessage() {
      String errorMessage = computeErrorMessage();
      setErrorMessage(errorMessage);
      setPageComplete(isPageComplete());
    }
  }

  /**
   * SelectRootElementPage
   */
  class SelectRootElementPage extends WizardPage implements SelectionListener {
    protected Combo combo;
    protected Button[] radioButton;
    protected PageBook pageBook;
    protected XSDOptionsPanel xsdOptionsPanel;
    protected DTDOptionsPanel dtdOptionsPanel;
    protected Text limitOptionalElementDepthTextControl;
    protected Button limitOptionalElementDepthCheckButtonControl;

    SelectRootElementPage() {
      super("SelectRootElementPage"); //$NON-NLS-1$
    }

    public void createControl(Composite parent) {
      // container group
      Composite containerGroup = new Composite(parent, SWT.NONE);
      PlatformUI.getWorkbench().getHelpSystem().setHelp(containerGroup,
          IXMLWizardHelpContextIds.XML_NEWWIZARD_SELECTROOTELEMENT_HELPID);
      containerGroup.setLayout(new GridLayout());
      containerGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
      setControl(containerGroup);

      // select root element
      Label containerLabel = new Label(containerGroup, SWT.NONE);
      containerLabel.setText(XMLWizardsMessages._UI_LABEL_ROOT_ELEMENT);
      combo = new Combo(containerGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
      combo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
      combo.addSelectionListener(this);

      // Options
      {
        Group group = new Group(containerGroup, SWT.NONE);
        group.setText(XMLWizardsMessages._UI_WIZARD_CONTENT_OPTIONS);

        GridLayout layout = new GridLayout();
        layout.numColumns = 1;
        layout.makeColumnsEqualWidth = true;
        layout.marginWidth = 0;
        group.setLayout(layout);
        group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        radioButton = new Button[4];

        radioButton[0] = new Button(group, SWT.CHECK);
        radioButton[0].setText(XMLWizardsMessages._UI_WIZARD_CREATE_OPTIONAL_ATTRIBUTES);
        radioButton[0].setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        radioButton[0].setSelection(false);
        PlatformUI.getWorkbench().getHelpSystem().setHelp(radioButton[0],
            IXMLWizardHelpContextIds.XML_NEWWIZARD_SELECTROOTELEMENT1_HELPID);

        radioButton[1] = new Button(group, SWT.CHECK);
        radioButton[1].setText(XMLWizardsMessages._UI_WIZARD_CREATE_OPTIONAL_ELEMENTS);
        radioButton[1].setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        radioButton[1].setSelection(false);
        PlatformUI.getWorkbench().getHelpSystem().setHelp(radioButton[1],
            IXMLWizardHelpContextIds.XML_NEWWIZARD_SELECTROOTELEMENT2_HELPID);

        radioButton[1].addSelectionListener(new SelectionAdapter() {
          public void widgetSelected(SelectionEvent selectionEvent) {
            boolean enabled = radioButton[1].getSelection();
            limitOptionalElementDepthCheckButtonControl.setEnabled(enabled);
            enabled = enabled && limitOptionalElementDepthCheckButtonControl.getSelection();
            limitOptionalElementDepthTextControl.setEnabled(enabled);
          }
        });
        Composite composite = new Composite(group, SWT.NONE);
        GridLayout gridLayout = new GridLayout();
        gridLayout.marginHeight = 0;
        gridLayout.marginWidth = 0;
        gridLayout.numColumns = 2;
        gridLayout.marginLeft = 20;
        composite.setLayout(gridLayout);
        limitOptionalElementDepthCheckButtonControl = new Button(composite, SWT.CHECK);
        limitOptionalElementDepthCheckButtonControl.setText(XMLWizardsMessages._UI_WIZARD_LIMIT_OPTIONAL_ELEMENT_DEPTH);
        limitOptionalElementDepthCheckButtonControl.setEnabled(false);
        limitOptionalElementDepthCheckButtonControl.setSelection(true);
        limitOptionalElementDepthCheckButtonControl.addSelectionListener(new SelectionAdapter() {
          public void widgetSelected(SelectionEvent selectionEvent) {
            boolean enabled = limitOptionalElementDepthCheckButtonControl.getSelection();
            limitOptionalElementDepthTextControl.setEnabled(enabled);
          }
        });
        limitOptionalElementDepthTextControl = new Text(composite, SWT.BORDER);
        limitOptionalElementDepthTextControl.setText(Integer.toString(OPTIONAL_ELEMENT_DEPTH_LIMIT_DEFAULT_VALUE));
        limitOptionalElementDepthTextControl.setEnabled(false);
        GridData gridaData = new GridData();
        gridaData.widthHint = 25;
        limitOptionalElementDepthTextControl.setLayoutData(gridaData);
        limitOptionalElementDepthTextControl.addListener(SWT.Verify, new Listener() {
          public void handleEvent(Event event) {
            String string = event.text;
            char[] chars = new char[string.length()];
            string.getChars(0, chars.length, chars, 0);
            for (int i = 0; i < chars.length; i++) {
              if (!('0' <= chars[i] && chars[i] <= '9')) {
                event.doit = false;
                return;
              }
            }
          }
        });

        radioButton[2] = new Button(group, SWT.CHECK);
        radioButton[2].setText(XMLWizardsMessages._UI_WIZARD_CREATE_FIRST_CHOICE);
        radioButton[2].setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        radioButton[2].setSelection(true);
        PlatformUI.getWorkbench().getHelpSystem().setHelp(radioButton[2],
            IXMLWizardHelpContextIds.XML_NEWWIZARD_SELECTROOTELEMENT3_HELPID);

        radioButton[3] = new Button(group, SWT.CHECK);
        radioButton[3].setText(XMLWizardsMessages._UI_WIZARD_FILL_ELEMENTS_AND_ATTRIBUTES);
        radioButton[3].setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        radioButton[3].setSelection(true);
        PlatformUI.getWorkbench().getHelpSystem().setHelp(radioButton[3],
            IXMLWizardHelpContextIds.XML_NEWWIZARD_SELECTROOTELEMENT4_HELPID);
        /*
         * radioButton = new Button[2];
         * 
         * radioButton[0] = new Button(group, SWT.RADIO);
         * radioButton[0].setText(XMLWizardsMessages.getString("_UI_WIZARD_CREATE_REQUIRED"));
         * radioButton[0].setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
         * radioButton[0].setSelection(true); WorkbenchHelp.setHelp(radioButton[0],
         * XMLBuilderContextIds.XMLC_CREATE_REQUIRED_ONLY);
         * 
         * radioButton[1] = new Button(group, SWT.RADIO);
         * radioButton[1].setText(XMLWizardsMessages.getString("_UI_WIZARD_CREATE_OPTIONAL"));
         * radioButton[1].setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
         * WorkbenchHelp.setHelp(radioButton[1],
         * XMLBuilderContextIds.XMLC_CREATE_REQUIRED_AND_OPTION);
         */
      }

      // add the grammar specific generation options
      //
      {
        pageBook = new PageBook(containerGroup, SWT.NONE);
        pageBook.setLayoutData(new GridData(GridData.FILL_BOTH));
        xsdOptionsPanel = new XSDOptionsPanel(this, pageBook);
        dtdOptionsPanel = new DTDOptionsPanel(this, pageBook);
        pageBook.showPage(xsdOptionsPanel);
      }
    }

    public void widgetSelected(SelectionEvent event) {
      int index = combo.getSelectionIndex();
      String rootElementName = (index != -1) ? combo.getItem(index) : null;
      generator.setRootElementName(rootElementName);
    }

    public void widgetDefaultSelected(SelectionEvent event) {
      // do nothing
    }

    public void setVisible(boolean visible) {
      super.setVisible(visible);

      if (visible) {
        try {
          if (generator.getGrammarURI() == null) {
            generator.setGrammarURI(selectGrammarFilePage.getURI());
            generator.setXMLCatalogEntry(selectGrammarFilePage.getXMLCatalogEntry());
          }
          Assert.isNotNull(generator.getGrammarURI());

          if (generator.getCMDocument() == null) {
            final String[] errorInfo = new String[2];
            final CMDocument[] cmdocs = new CMDocument[1];
            Runnable r = new Runnable() {
              public void run() {
                cmdocs[0] = NewXMLGenerator.createCMDocument(generator.getGrammarURI(), errorInfo);
              }
            };
            org.eclipse.swt.custom.BusyIndicator.showWhile(Display.getCurrent(), r);

            generator.setCMDocument(cmdocs[0]);
            cmDocumentErrorMessage = errorInfo[1];
          }

          combo.removeAll();
          if ((generator.getCMDocument() != null) && (cmDocumentErrorMessage == null)) {
            CMNamedNodeMap nameNodeMap = generator.getCMDocument().getElements();
            Vector nameNodeVector = new Vector();

            for (int i = 0; i < nameNodeMap.getLength(); i++) {
              CMElementDeclaration cmElementDeclaration = (CMElementDeclaration) nameNodeMap.item(i);
              Object value = cmElementDeclaration.getProperty("Abstract"); //$NON-NLS-1$
              if (value != Boolean.TRUE) {
                nameNodeVector.add(cmElementDeclaration.getElementName());
              }
            }

            Object[] nameNodeArray = nameNodeVector.toArray();
            if (nameNodeArray.length > 0) {
              Arrays.sort(nameNodeArray, Collator.getInstance());
            }

            String defaultRootName = (String) (generator.getCMDocument()).getProperty("http://org.eclipse.wst/cm/properties/defaultRootName"); //$NON-NLS-1$
            int defaultRootIndex = -1;
            for (int i = 0; i < nameNodeArray.length; i++) {
              String elementName = (String) nameNodeArray[i];

              combo.add(elementName);
              if ((defaultRootName != null) && defaultRootName.equals(elementName)) {
                defaultRootIndex = i;
              }
            }

            if (nameNodeArray.length > 0) {
              defaultRootIndex = defaultRootIndex != -1 ? defaultRootIndex : 0;
              combo.select(defaultRootIndex);
              generator.setRootElementName(combo.getItem(defaultRootIndex));
            }
          }

          if (generator.getGrammarURI().endsWith("xsd")) //$NON-NLS-1$
          {
            pageBook.showPage(xsdOptionsPanel);
            generator.setDefaultSystemId(getDefaultSystemId());
            generator.createNamespaceInfoList();

            // Provide default namespace prefix if none
            for (int i = 0; i < generator.namespaceInfoList.size(); i++) {
              NamespaceInfo nsinfo = (NamespaceInfo) generator.namespaceInfoList.get(i);
              if (((nsinfo.prefix == null) || (nsinfo.prefix.trim().length() == 0))
                  && ((nsinfo.uri != null) && (nsinfo.uri.trim().length() != 0))) {
                nsinfo.prefix = getDefaultPrefix(generator.namespaceInfoList);
              }
            }
            xsdOptionsPanel.setNamespaceInfoList(generator.namespaceInfoList);
          } else if (generator.getGrammarURI().endsWith("dtd")) //$NON-NLS-1$
          {
            pageBook.showPage(dtdOptionsPanel);
            dtdOptionsPanel.update();
          }
        } catch (Exception e) {
          // XMLBuilderPlugin.getPlugin().getMsgLogger().writeCurrentThread();
        }

        /*
         * String errorMessage = computeErrorMessage(); if (errorMessage == null)
         * super.setVisible(visible);
         */

        updateErrorMessage();
      }
    }

    private String getDefaultPrefix(List nsInfoList) {
      String defaultPrefix = "p"; //$NON-NLS-1$
      if (nsInfoList == null) {
        return defaultPrefix;
      }

      Vector v = new Vector();
      for (int i = 0; i < nsInfoList.size(); i++) {
        NamespaceInfo nsinfo = (NamespaceInfo) nsInfoList.get(i);
        if (nsinfo.prefix != null) {
          v.addElement(nsinfo.prefix);
        }
      }

      if (v.contains(defaultPrefix)) {
        String s = defaultPrefix;
        for (int j = 0; v.contains(s); j++) {
          s = defaultPrefix + Integer.toString(j);
        }
        return s;
      }
      return defaultPrefix;
    }

    public int getOptionalElementDepthLimit() {
      int depth = -1;
      if (radioButton[1].getSelection()
          && limitOptionalElementDepthCheckButtonControl.getSelection()) {
        try {
          depth = Integer.parseInt(limitOptionalElementDepthTextControl.getText());
        } catch (Exception exception) {
        }
      }
      return depth;
    }

    public boolean isPageComplete() {
      boolean complete = ((generator.getRootElementName() != null) && (generator.getRootElementName().length() > 0))
          && (getErrorMessage() == null);

      if (complete) {
        /*
         * int buildPolicy = radioButton[0].getSelection() ?
         * DOMContentBuilder.BUILD_ONLY_REQUIRED_CONTENT : DOMContentBuilder.BUILD_ALL_CONTENT;
         */
        int buildPolicy = 0;
        if (radioButton[0].getSelection()) {
          buildPolicy = buildPolicy | DOMContentBuilder.BUILD_OPTIONAL_ATTRIBUTES;
        }
        if (radioButton[1].getSelection()) {
          buildPolicy = buildPolicy | DOMContentBuilder.BUILD_OPTIONAL_ELEMENTS;
        }
        if (radioButton[2].getSelection()) {
          buildPolicy = buildPolicy | DOMContentBuilder.BUILD_FIRST_CHOICE
              | DOMContentBuilder.BUILD_FIRST_SUBSTITUTION;
        }
        if (radioButton[3].getSelection()) {
          buildPolicy = buildPolicy | DOMContentBuilder.BUILD_TEXT_NODES;
        }

        generator.setBuildPolicy(buildPolicy);
      }

      return complete;
    }

    public String computeErrorMessage() {
      String errorMessage = null;

      if (cmDocumentErrorMessage != null) {
        errorMessage = cmDocumentErrorMessage;
      } else if ((generator.getRootElementName() == null)
          || (generator.getRootElementName().length() == 0)) {
        errorMessage = XMLWizardsMessages._ERROR_ROOT_ELEMENT_MUST_BE_SPECIFIED;
      }

      return errorMessage;
    }

    public void updateErrorMessage() {
      String errorMessage = computeErrorMessage();
      if (errorMessage == null) {
        if (xsdOptionsPanel.isVisible()) {

          errorMessage = xsdOptionsPanel.computeErrorMessage();
        } else if (dtdOptionsPanel.isVisible()) {
          errorMessage = dtdOptionsPanel.computeErrorMessage();
        }
      }
      setErrorMessage(errorMessage);
      setPageComplete(isPageComplete());
    }
  }

  // //////////////End SelectRootElementPage

  public static GridLayout createOptionsPanelLayout() {
    GridLayout gridLayout = new GridLayout();
    gridLayout.marginWidth = 0;
    gridLayout.horizontalSpacing = 0;
    return gridLayout;
  }

  /**
	 * 
	 */
  class XSDOptionsPanel extends Composite {
    protected String errorMessage = null;
    protected SelectRootElementPage parentPage;
    protected CommonEditNamespacesDialog editNamespaces;
    private IPath currentPath = null;

    public XSDOptionsPanel(SelectRootElementPage parentPage, Composite parent) {
      super(parent, SWT.NONE);
      this.parentPage = parentPage;

      setLayout(createOptionsPanelLayout());
      setLayoutData(new GridData(GridData.FILL_BOTH));
    }

    private CommonEditNamespacesDialog getEditNamespaces() {
      if (editNamespaces == null) {
        Composite co = new Composite(this, SWT.NONE);
        co.setLayout(new GridLayout());
        String tableTitle = XMLWizardsMessages._UI_LABEL_NAMESPACE_INFORMATION;
        editNamespaces = new CommonEditNamespacesDialog(co, null, tableTitle, true, true);
        this.layout();
      }
      return editNamespaces;
    }

    public void setNamespaceInfoList(List list) {
      CommonEditNamespacesDialog editDialog = getEditNamespaces();

      if (newFilePage != null) {
        IPath newPath = newFilePage.getContainerFullPath();
        if (newPath != null) {
          if (!newPath.equals(currentPath)) {
            String resourceURI = "platform:/resource" + newPath.toString() + "/dummy"; //$NON-NLS-1$ //$NON-NLS-2$
            String resolvedPath = URIHelper.normalize(resourceURI, null, null);
            resolvedPath = URIHelper.removeProtocol(resolvedPath);

            currentPath = new Path(resolvedPath);
            editDialog.setResourcePath(currentPath);
          }
        }
      }
      editDialog.setNamespaceInfoList(list);
      editDialog.updateErrorMessage(list);
    }

    public void updateErrorMessage(List namespaceInfoList) {
      NamespaceInfoErrorHelper helper = new NamespaceInfoErrorHelper();
      errorMessage = helper.computeErrorMessage(namespaceInfoList, null);
      parentPage.updateErrorMessage();
    }

    public String computeErrorMessage() {
      return errorMessage;
    }
  }

  /**
	 * 
	 */
  public class DTDOptionsPanel extends Composite implements ModifyListener {
    protected Group group;
    protected Text systemIdField;
    protected Text publicIdField;
    protected SelectRootElementPage parentPage;

    public DTDOptionsPanel(SelectRootElementPage parentPage, Composite parent) {
      super(parent, SWT.NONE);
      this.parentPage = parentPage;
      setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

      setLayout(createOptionsPanelLayout());
      Group group = new Group(this, SWT.NONE);
      group.setText(XMLWizardsMessages._UI_LABEL_DOCTYPE_INFORMATION);

      GridLayout layout = new GridLayout();
      layout.numColumns = 2;
      group.setLayout(layout);
      group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

      Label publicIdLabel = new Label(group, SWT.NONE);
      publicIdLabel.setText(XMLWizardsMessages._UI_LABEL_PUBLIC_ID);
      publicIdField = new Text(group, SWT.SINGLE | SWT.BORDER);
      publicIdField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
      publicIdField.addModifyListener(this);
      PlatformUI.getWorkbench().getHelpSystem().setHelp(publicIdField,
          IXMLWizardHelpContextIds.XML_NEWWIZARD_SELECTROOTELEMENT5_HELPID);

      Label systemIdLabel = new Label(group, SWT.NONE);
      systemIdLabel.setText(XMLWizardsMessages._UI_LABEL_SYSTEM_ID);
      systemIdField = new Text(group, SWT.SINGLE | SWT.BORDER);
      systemIdField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
      systemIdField.addModifyListener(this);
      PlatformUI.getWorkbench().getHelpSystem().setHelp(systemIdField,
          IXMLWizardHelpContextIds.XML_NEWWIZARD_SELECTROOTELEMENT6_HELPID);
    }

    public void update() {
      String thePublicId = null;
      String theSystemId = null;
      ICatalogEntry xmlCatalogEntry = generator.getXMLCatalogEntry();

      if (xmlCatalogEntry != null) {
        if (xmlCatalogEntry.getEntryType() == ICatalogEntry.ENTRY_TYPE_PUBLIC) {
          thePublicId = xmlCatalogEntry.getKey();
          theSystemId = xmlCatalogEntry.getAttributeValue(ICatalogEntry.ATTR_WEB_URL);
          if (theSystemId == null) {
            theSystemId = generator.getGrammarURI().startsWith("http:") ? generator.getGrammarURI() : URIHelper.getLastSegment(generator.getGrammarURI()); //$NON-NLS-1$
          }
        } else {
          theSystemId = xmlCatalogEntry.getKey();
        }
      } else {
        theSystemId = getDefaultSystemId();
      }

      publicIdField.setText(thePublicId != null ? thePublicId : ""); //$NON-NLS-1$
      systemIdField.setText(theSystemId != null ? theSystemId : ""); //$NON-NLS-1$
    }

    public void modifyText(ModifyEvent e) {
      generator.setSystemId(systemIdField.getText());
      generator.setPublicId(publicIdField.getText());
      parentPage.updateErrorMessage();
    }

    public String computeErrorMessage() {
      return null;
    }
  }

}
