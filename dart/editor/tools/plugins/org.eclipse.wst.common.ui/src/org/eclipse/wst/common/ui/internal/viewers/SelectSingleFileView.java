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
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.wizards.datatransfer.FileSystemImportWizard;
import org.eclipse.wst.common.ui.internal.Messages;
import org.eclipse.wst.common.ui.internal.UIPlugin;

import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;

public class SelectSingleFileView {
  protected Composite composite;
  protected IStructuredSelection selection;
  protected boolean isFileMandatory;
  protected TreeViewer sourceFileViewer;
  protected Button importButton;
  protected Vector fFilters;
  protected IFile selectedFile;
  protected ISelection defaultSelection;
  protected Listener listener;

  public static interface Listener {
    public void setControlComplete(boolean isComplete);
  }

  public SelectSingleFileView(IStructuredSelection selection, boolean isFileMandatory) {
    this.selection = selection;
    this.isFileMandatory = isFileMandatory;
    this.selectedFile = null;
    this.defaultSelection = null;
  }

  public Composite createControl(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);
    composite.setLayout(new GridLayout());
    composite.setLayoutData(new GridData(GridData.FILL_BOTH));

    Composite smallComposite = new Composite(composite, SWT.NONE);
    smallComposite.setLayoutData(new GridData(GridData.FILL, GridData.HORIZONTAL_ALIGN_FILL, true,
        false));
    GridLayout gridLayout = new GridLayout(2, false);
    gridLayout.marginHeight = 0;
    smallComposite.setLayout(gridLayout);

    Label label = new Label(smallComposite, SWT.NONE);
    label.setText(Messages._UI_LABEL_SOURCE_FILES);
    label.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING, GridData.END, false,
        false));

    //Collapse and Expand all buttons
    ToolBar toolBar = new ToolBar(smallComposite, SWT.FLAT);
    toolBar.setLayoutData(new GridData(GridData.END, GridData.END, true, false));

    ToolItem toolItem = new ToolItem(toolBar, SWT.NONE);
    ImageDescriptor imageDescriptor = UIPlugin.getDefault().getImageDescriptor(
        "icons/expandAll.gif");
    Image image = imageDescriptor.createImage();
    toolItem.setImage(image);
    toolItem.setToolTipText(Messages._UI_POPUP_EXPAND_ALL);
    toolItem.addSelectionListener(new SelectionListener() {

      public void widgetDefaultSelected(SelectionEvent e) {
      }

      public void widgetSelected(SelectionEvent e) {
        sourceFileViewer.expandAll();

      }
    });

    toolItem = new ToolItem(toolBar, SWT.NONE);
    imageDescriptor = UIPlugin.getDefault().getImageDescriptor("icons/collapseAll.gif");
    image = imageDescriptor.createImage();
    toolItem.setImage(image);
    toolItem.setToolTipText(Messages._UI_POPUP_COLLAPSE_ALL);
    toolItem.addSelectionListener(new SelectionListener() {

      public void widgetDefaultSelected(SelectionEvent e) {
      }

      public void widgetSelected(SelectionEvent e) {
        sourceFileViewer.collapseAll();

      }
    });

    createSourceViewer(composite);
    createFilterControl(composite);
    createImportButton(composite);
    sourceFileViewer.getTree().setFocus();
    return composite;
  }

  protected void createFilterControl(Composite composite) {
  }

  public void setListener(Listener listener) {
    this.listener = listener;
  }

  protected void createSourceViewer(Composite parent) {
    sourceFileViewer = new TreeViewer(new Tree(parent, SWT.SINGLE | SWT.BORDER));
    sourceFileViewer.setContentProvider(new WorkbenchContentProvider());
    sourceFileViewer.setLabelProvider(new WorkbenchLabelProvider());
    sourceFileViewer.addSelectionChangedListener(new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        boolean isComplete = true;
        java.util.List list;
        ISelection selection = event.getSelection();
        if (selection instanceof IStructuredSelection) {
          list = ((IStructuredSelection) selection).toList();
          for (Iterator i = list.iterator(); i.hasNext();) {
            IResource resource = (IResource) i.next();
            if (resource instanceof IFile) {
              selectedFile = (IFile) resource;
              if (isFileMandatory) {
                isComplete = true;
                break;
              }
            } else {
              selectedFile = null;
              if (isFileMandatory) {
                isComplete = false;
              }
            }
          }

          if (listener != null) {
            listener.setControlComplete(isComplete);
          }
        }
      }
    });
    Control treeWidget = sourceFileViewer.getTree();
    GridData gd = new GridData(GridData.FILL_BOTH);
    treeWidget.setLayoutData(gd);
  }

  protected void createImportButton(Composite parent) {
    importButton = new Button(parent, SWT.NONE);
    importButton.setText(Messages._UI_IMPORT_BUTTON);

    GridData gridData = new GridData();
    gridData.horizontalAlignment = GridData.CENTER;
    importButton.setLayoutData(gridData);
    importButton.addSelectionListener(new SelectionListener() {
      public void widgetDefaultSelected(SelectionEvent e) {
      }

      public void widgetSelected(SelectionEvent e) {
        // This listener is if the Import Wizard adds a new few, we want
        // it to be selected when we come back from it
        ImportAddResourceListener importAddResourceListener = new ImportAddResourceListener();

        ResourcesPlugin.getWorkspace().addResourceChangeListener(importAddResourceListener);

        FileSystemImportWizard importWizard = new FileSystemImportWizard();
        IWorkbench workbench = UIPlugin.getDefault().getWorkbench();
        selection = (IStructuredSelection) sourceFileViewer.getSelection();
        importWizard.init(workbench, selection != null ? selection : new StructuredSelection());
        Shell shell = Display.getCurrent().getActiveShell();
        WizardDialog wizardDialog = new WizardDialog(shell, importWizard);
        wizardDialog.create();
        wizardDialog.open();
        sourceFileViewer.refresh();
        ResourcesPlugin.getWorkspace().removeResourceChangeListener(importAddResourceListener);
        IFile importedFile = importAddResourceListener.getImportedFile();
        if (importedFile != null) {
          StructuredSelection structuredSelection = new StructuredSelection(importedFile);
          sourceFileViewer.setSelection(structuredSelection);
        }
      }
    });
    importButton.setToolTipText(Messages._UI_IMPORT_BUTTON_TOOL_TIP);
  }

  public IFile getFile() {
    return selectedFile;
  }

  public void setDefaultSelection(ISelection selection) {
    this.defaultSelection = selection;
  }

  public void resetFilters() {
    fFilters = null;
  }

  public void addFilter(ViewerFilter filter) {
    if (fFilters == null) {
      fFilters = new Vector();
    }
    fFilters.add(filter);
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
      if (filterExtensions[i].startsWith(".")) {
        correctedFilterExtensions[i] = filterExtensions[i];
      } else {
        correctedFilterExtensions[i] = "." + filterExtensions[i];
      }
    }

    ViewerFilter filter = new ResourceFilter(correctedFilterExtensions, null);
    addFilter(filter);
  }

// This is a convenience method that allows filtering of the given file
// exensions. It internally creates a ResourceFilter so that users of this
// class don't have to construct one.
// If the extensions provided don't have '.', one will be added.
  public void addFilterExtensions(String[] filterExtensions, IFile[] excludedFiles) {
    // First add the '.' to the filterExtensions if they don't already have one
    String[] correctedFilterExtensions = new String[filterExtensions.length];
    for (int i = 0; i < filterExtensions.length; i++) {
      // If the extension doesn't start with a '.', then add one.
      if (filterExtensions[i].startsWith(".")) {
        correctedFilterExtensions[i] = filterExtensions[i];
      } else {
        correctedFilterExtensions[i] = "." + filterExtensions[i];
      }
    }
    ViewerFilter filter;
    if (excludedFiles != null) {
      filter = new ResourceFilter(correctedFilterExtensions, excludedFiles, null);
    } else {
      filter = new ResourceFilter(correctedFilterExtensions, null);
    }
    addFilter(filter);
  }

  // This is a convenience method that allows filtering of the given file
  // exensions. It internally creates a ResourceFilter so that users of this
  // class don't have to construct one.
  // If the extensions provided don't have '.', one will be added.
  public void setFilterExtensions(String[] filterExtensions) {
    // First add the '.' to the filterExtensions if they don't already have one
    String[] correctedFilterExtensions = new String[filterExtensions.length];
    for (int i = 0; i < filterExtensions.length; i++) {
      // If the extension doesn't start with a '.', then add one.
      if (filterExtensions[i].startsWith(".")) {
        correctedFilterExtensions[i] = filterExtensions[i];
      } else {
        correctedFilterExtensions[i] = "." + filterExtensions[i];
      }
    }
    ViewerFilter filter = new ResourceFilter(correctedFilterExtensions, null);
    fFilters = new Vector();
    fFilters.add(filter);
    if (sourceFileViewer != null) {
      sourceFileViewer.getTree().setRedraw(false);
      sourceFileViewer.resetFilters();
      for (Iterator i = fFilters.iterator(); i.hasNext();) {
        sourceFileViewer.addFilter((ViewerFilter) i.next());
      }
      sourceFileViewer.getTree().setRedraw(true);
      sourceFileViewer.getTree().redraw();
    }
  }

  // this method should be called by a Wizard page or Dialog when it becomes visible
  public void setVisibleHelper(boolean visible) {
    if (visible == true) {
      if (fFilters != null) {
        sourceFileViewer.resetFilters();
        for (Iterator i = fFilters.iterator(); i.hasNext();)
          sourceFileViewer.addFilter((ViewerFilter) i.next());
      }
      sourceFileViewer.setInput(ResourcesPlugin.getWorkspace().getRoot());
      sourceFileViewer.expandToLevel(1);

      if (defaultSelection != null) {
        sourceFileViewer.setSelection(defaultSelection, true);
      } else if (!sourceFileViewer.getSelection().isEmpty()) {
        sourceFileViewer.setSelection(sourceFileViewer.getSelection());
      } else {
        if (isFileMandatory && listener != null) {
          listener.setControlComplete(false);
        }
      }
    }
  }

  class ImportAddResourceListener implements IResourceChangeListener, IResourceDeltaVisitor {
    Vector importedFiles;

    ImportAddResourceListener() {
      importedFiles = new Vector();
    }

    public void resourceChanged(IResourceChangeEvent event) {
      IResourceDelta resourceDelta = event.getDelta();

      try {
        if (resourceDelta != null) {
          resourceDelta.accept(this);
        }
      } catch (Exception e) {
        //TODO... log exception
        //UIPlugin.getMsgLogger().write("Exception caught during resource change" + e);
        //UIPlugin.getMsgLogger().writeCurrentThread(); 
      }
    }

    public boolean visit(IResourceDelta delta) {
      if (delta.getKind() == IResourceDelta.ADDED) {
        if (delta.getResource() instanceof IFile)
          importedFiles.add(delta.getResource());
      }
      return true;
    }

    public Collection getImportedFiles() {
      return importedFiles;
    }

    // This returns the first imported file in the list of imported files
    public IFile getImportedFile() {
      if (importedFiles.isEmpty() == false)
        return (IFile) importedFiles.firstElement();

      return null;
    }
  }

  public void addSelectionChangedTreeListener(ISelectionChangedListener treeListener) {
    sourceFileViewer.addSelectionChangedListener(treeListener);
  }
}
