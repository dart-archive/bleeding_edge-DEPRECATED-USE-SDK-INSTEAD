/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - Initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.common.ui.internal.viewers;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.wizards.datatransfer.FileSystemImportWizard;
import org.eclipse.wst.common.ui.internal.Messages;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Vector;

// Page to specify the source files
public class SelectMultiFilePage extends WizardPage {
  IWorkbench workbench;
  IStructuredSelection selection;
  boolean isFileMandatory;
  TreeViewer sourceFileViewer;
  Button addButton;
  Button removeButton;
  Button removeAllButton;
  org.eclipse.swt.widgets.List selectedListBox;
  Button importButton;
  private Vector fFilters;
  protected IFile[] fileNames;
  IWorkspaceRoot workspaceRoot;

  private final static int SIZING_LISTS_HEIGHT = 200;
  private final static int SIZING_LISTS_WIDTH = 150;

  // parameter isFileMandatory is used to determine if at least one file must be selected  
  // before being able to proceed to the next page
  public SelectMultiFilePage(IWorkbench workbench, IStructuredSelection selection,
      boolean isFileMandatory) {
    super("SelectMultiFilePage");
    this.workbench = workbench;
    this.selection = selection;
    this.isFileMandatory = isFileMandatory;
    this.workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
    this.fileNames = null;
  }

  public void createControl(Composite parent) {

    Composite pageContent = new Composite(parent, SWT.NONE);
    GridLayout layout = new GridLayout();
    layout.numColumns = 3;
    pageContent.setLayout(layout);
    pageContent.setLayoutData(new GridData(GridData.FILL_BOTH));

    // variable never used ... is pageContent.getLayoutData() needed?
    //GridData outerFrameGridData = (GridData) 
    pageContent.getLayoutData();

    //		outerFrameGridData.horizontalAlignment = GridData.HORIZONTAL_ALIGN_FILL;
    //		outerFrameGridData.verticalAlignment = GridData.VERTICAL_ALIGN_FILL;

    //    WorkbenchHelp.setHelp(
    //        pageContent,
    //        B2BGUIContextIds.BTBG_SELECT_MULTI_FILE_PAGE);

    createLabels(pageContent);
    createSourceViewer(pageContent);
    createButtonPanel(pageContent);
    createSelectedListBox(pageContent);
    createImportButton(pageContent);

    setControl(pageContent);
    if (isFileMandatory)
      setPageComplete(false);

  }

  public IFile[] getFiles() {
    return fileNames;
  }

  // This is a convenience method that allows filtering of the given file
  // exensions. It internally creates a ResourceFilter so that users of this
  // class don't have to construct one.
  // If the extensions provided don't have '.', one will be added.
  public void addFilterExtensions(String[] filterExtensions) {
    // First add the '.' to the filterExtensions if they don't already have one
    String[] correctedFilterExtensions = new String[filterExtensions.length];
    for (int i = 0; i < filterExtensions.length; i++) {
      // If the extension doesn't start with a '.', then add one.
      if (filterExtensions[i].startsWith("."))
        correctedFilterExtensions[i] = filterExtensions[i];
      else
        correctedFilterExtensions[i] = "." + filterExtensions[i];
    }

    ViewerFilter filter = new ResourceFilter(correctedFilterExtensions, null);
    addFilter(filter);
  }

  public boolean isValidSourceFileViewerSelection(ISelection selection) {
    return true;
  }

  public void setVisible(boolean visible) {
    if (visible == true) {
      if (fFilters != null) {
        sourceFileViewer.resetFilters();
        for (Iterator i = fFilters.iterator(); i.hasNext();)
          sourceFileViewer.addFilter((ViewerFilter) i.next());
      }
      sourceFileViewer.setInput(ResourcesPlugin.getWorkspace().getRoot());
    }
    super.setVisible(visible);
  }

  public void setFiles(String[] fileNames) {
    int size = Arrays.asList(fileNames).size();
    Vector iFileNames = new Vector();
    for (int i = 0; i < size; i++) {
      IResource resource = workspaceRoot.findMember(fileNames[i]);
      if (resource instanceof IFile)
        iFileNames.addElement(resource);
    }
    IFile[] dummyArray = new IFile[iFileNames.size()];
    this.fileNames = (IFile[]) (iFileNames.toArray(dummyArray));
  }

  public void resetFilters() {
    fFilters = null;
  }

  public void addFilter(ViewerFilter filter) {
    if (fFilters == null)
      fFilters = new Vector();
    fFilters.add(filter);
  }

  public void setAddButtonEnabled(boolean isEnabled) {
    addButton.setEnabled(isEnabled);
  }

  public void setRemoveButtonEnabled(boolean isEnabled) {
    removeButton.setEnabled(isEnabled);
  }

  private void createLabels(Composite pageContent) {
    Label label = new Label(pageContent, SWT.LEFT);
    label.setText(Messages._UI_LABEL_SOURCE_FILES);

    GridData data = new GridData();
    data.horizontalAlignment = GridData.FILL;
    data.horizontalSpan = 2;
    label.setLayoutData(data);

    label = new Label(pageContent, SWT.LEFT);
    label.setText(Messages._UI_LABEL_SELECTED_FILES);
  }

  public boolean checkIfFileInTarget(IFile fileToCheck) {
    String[] strings = selectedListBox.getItems();
    int size = selectedListBox.getItemCount();
    for (int i = 0; i < size; i++) {
      if (strings[i].compareTo(fileToCheck.getFullPath().toString()) == 0)
        return true;
    }
    return false;
  }

  private void createSourceViewer(Composite parent) {
    sourceFileViewer = new TreeViewer(new Tree(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL
        | SWT.BORDER));
    sourceFileViewer.setContentProvider(new WorkbenchContentProvider());
    sourceFileViewer.setLabelProvider(new WorkbenchLabelProvider());
    sourceFileViewer.addSelectionChangedListener(new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        java.util.List list;
        ISelection selection = event.getSelection();
        boolean newFilesSelected = false;

        if (selection instanceof IStructuredSelection) {
          list = ((IStructuredSelection) selection).toList();
          for (Iterator i = list.iterator(); i.hasNext();) {
            IResource resource = (IResource) i.next();
            if (resource instanceof IFile) {
              if (checkIfFileInTarget((IFile) resource) == false)
                newFilesSelected = true;
            }
          }
          setAddButtonEnabled(newFilesSelected);
        }
      }
    });
    sourceFileViewer.addDoubleClickListener(new IDoubleClickListener() {
      public void doubleClick(DoubleClickEvent event) {
        addSelectedFilesToTargetList();
      }
    });

    Control treeWidget = sourceFileViewer.getTree();
    GridData gd = new GridData(GridData.FILL_BOTH);
    gd.widthHint = SIZING_LISTS_WIDTH;
    gd.heightHint = SIZING_LISTS_HEIGHT;
    treeWidget.setLayoutData(gd);
  }

  private void createButtonPanel(Composite pageContent) {
    Composite buttonPanel = new Composite(pageContent, SWT.NONE);
    GridLayout layout = new GridLayout();
    layout.numColumns = 1;
    buttonPanel.setLayout(layout);

    GridData gridData = new GridData();
    gridData.grabExcessHorizontalSpace = false;
    gridData.grabExcessVerticalSpace = true;
    gridData.verticalAlignment = GridData.CENTER;
    gridData.horizontalAlignment = GridData.CENTER;
    buttonPanel.setLayoutData(gridData);

    addButton = new Button(buttonPanel, SWT.PUSH);
    addButton.setText(Messages._UI_ADD_BUTTON);
    gridData = new GridData();
    gridData.horizontalAlignment = GridData.FILL;
    gridData.verticalAlignment = GridData.CENTER;
    addButton.setLayoutData(gridData);
    addButton.addSelectionListener(new ButtonSelectListener());
    addButton.setToolTipText(Messages._UI_ADD_BUTTON_TOOL_TIP);
    addButton.setEnabled(false);

    removeButton = new Button(buttonPanel, SWT.PUSH);
    removeButton.setText(Messages._UI_REMOVE_BUTTON);
    gridData = new GridData();
    gridData.horizontalAlignment = GridData.FILL;
    gridData.verticalAlignment = GridData.CENTER;
    removeButton.setLayoutData(gridData);
    removeButton.addSelectionListener(new ButtonSelectListener());
    removeButton.setToolTipText(Messages._UI_REMOVE_BUTTON_TOOL_TIP);
    removeButton.setEnabled(false);

    removeAllButton = new Button(buttonPanel, SWT.PUSH);
    removeAllButton.setText(Messages._UI_REMOVE_ALL_BUTTON);
    gridData = new GridData();
    gridData.horizontalAlignment = GridData.FILL;
    gridData.verticalAlignment = GridData.CENTER;
    removeAllButton.setLayoutData(gridData);
    removeAllButton.addSelectionListener(new ButtonSelectListener());
    removeAllButton.setToolTipText(Messages._UI_REMOVE_ALL_BUTTON_TOOL_TIP);
    removeAllButton.setEnabled(false);
  }

  private void createSelectedListBox(Composite parent) {
    selectedListBox = new List(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
    selectedListBox.addSelectionListener(new SelectionListener() {
      public void widgetDefaultSelected(SelectionEvent event) {
      }

      public void widgetSelected(SelectionEvent event) {

        if (selectedListBox.getSelectionCount() > 0)
          setRemoveButtonEnabled(true);
        else
          setRemoveButtonEnabled(false);
        return;
      }
    });

    GridData gd = new GridData(GridData.FILL_BOTH);
    gd.widthHint = SIZING_LISTS_WIDTH;
    gd.heightHint = SIZING_LISTS_HEIGHT;
    selectedListBox.setLayoutData(gd);
  }

  void createImportButton(Composite parent) {
    importButton = new Button(parent, SWT.PUSH);
    importButton.setText(Messages._UI_IMPORT_BUTTON);

    GridData gridData = new GridData();
    gridData.horizontalAlignment = GridData.CENTER;
    importButton.setLayoutData(gridData);
    importButton.addSelectionListener(new SelectionListener() {
      public void widgetDefaultSelected(SelectionEvent e) {
      }

      public void widgetSelected(SelectionEvent e) {
        FileSystemImportWizard importWizard = new FileSystemImportWizard();
        importWizard.init(workbench, selection);
        Shell shell = Display.getCurrent().getActiveShell();
        WizardDialog wizardDialog = new WizardDialog(shell, importWizard);
        wizardDialog.create();
        wizardDialog.open();
        sourceFileViewer.refresh();
      }
    });
    importButton.setToolTipText(Messages._UI_IMPORT_BUTTON_TOOL_TIP);
  }

  public void addSelectedFilesToTargetList() {
    ISelection selection = sourceFileViewer.getSelection();

    if (isValidSourceFileViewerSelection(selection)) {
      java.util.List list = null;
      if (selection instanceof IStructuredSelection) {
        list = ((IStructuredSelection) selection).toList();

        if (list != null) {
          list = ((IStructuredSelection) selection).toList();
          for (Iterator i = list.iterator(); i.hasNext();) {
            IResource resource = (IResource) i.next();
            if (resource instanceof IFile) {
              // Check if its in the list. Don't add it if it is.
              String resourceName = resource.getFullPath().toString();
              if (selectedListBox.indexOf(resourceName) == -1)
                selectedListBox.add(resourceName);
            }
          }
          setFiles(selectedListBox.getItems());
        }

        setAddButtonEnabled(false);

        if (selectedListBox.getItemCount() > 0) {
          removeAllButton.setEnabled(true);
          if (isFileMandatory)
            setPageComplete(true);
          if (selectedListBox.getSelectionCount() > 0)
            setRemoveButtonEnabled(true);
          else
            setRemoveButtonEnabled(false);
        }
      }
    }
  }

  class ButtonSelectListener implements SelectionListener {
    public void widgetDefaultSelected(SelectionEvent e) {
    }

    public void widgetSelected(SelectionEvent e) {
      if (e.widget == addButton) {
        addSelectedFilesToTargetList();
      } else if (e.widget == removeButton) {
        String[] strings = selectedListBox.getSelection();
        int size = selectedListBox.getSelectionCount();
        for (int i = 0; i < size; i++) {
          selectedListBox.remove(strings[i]);
        }
        removeButton.setEnabled(false);
        if (selectedListBox.getItemCount() == 0) {
          removeAllButton.setEnabled(false);
          if (isFileMandatory)
            setPageComplete(false);
        }
        setFiles(selectedListBox.getItems());
      } else if (e.widget == removeAllButton) {
        selectedListBox.removeAll();
        removeButton.setEnabled(false);
        removeAllButton.setEnabled(false);
        if (isFileMandatory)
          setPageComplete(false);
        setFiles(selectedListBox.getItems());
      }
    }
  }
}
