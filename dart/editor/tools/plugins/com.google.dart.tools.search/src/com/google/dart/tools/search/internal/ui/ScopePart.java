/*
 * Copyright (c) 2012, the Dart project authors.
 * 
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.search.internal.ui;

import com.google.dart.tools.search.internal.ui.util.SWTUtil;
import com.google.dart.tools.search.ui.ISearchPageContainer;
import com.google.dart.tools.search.ui.NewSearchUI;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.accessibility.AccessibleAdapter;
import org.eclipse.swt.accessibility.AccessibleEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.IWorkingSetSelectionDialog;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ScopePart {

  // Settings store
  private static final String DIALOG_SETTINGS_KEY = "SearchDialog.ScopePart"; //$NON-NLS-1$
  private static final String STORE_SCOPE = "scope"; //$NON-NLS-1$
  private static final String STORE_LRU_WORKING_SET_NAME = "lastUsedWorkingSetName"; //$NON-NLS-1$
  private static final String STORE_LRU_WORKING_SET_NAMES = "lastUsedWorkingSetNames"; //$NON-NLS-1$

  @SuppressWarnings("unchecked")
  public static String toString(IWorkingSet[] workingSets) {
    String result = ""; //$NON-NLS-1$
    if (workingSets != null && workingSets.length > 0) {
      Arrays.sort(workingSets, new WorkingSetComparator());
      boolean firstFound = false;
      for (int i = 0; i < workingSets.length; i++) {
        String workingSetName = workingSets[i].getLabel();
        if (firstFound) {
          result = Messages.format(SearchMessages.ScopePart_workingSetConcatenation, new String[] {
              result, workingSetName});
        } else {
          result = workingSetName;
          firstFound = true;
        }
      }
    }
    return result;
  }

  private static int getStoredScope(IDialogSettings settingsStore,
      boolean canSearchEnclosingProjects) {
    int scope;
    try {
      scope = settingsStore.getInt(STORE_SCOPE);
    } catch (NumberFormatException ex) {
      scope = ISearchPageContainer.WORKSPACE_SCOPE;
    }
    if (scope != ISearchPageContainer.WORKING_SET_SCOPE
        && scope != ISearchPageContainer.SELECTION_SCOPE
        && scope != ISearchPageContainer.SELECTED_PROJECTS_SCOPE
        && scope != ISearchPageContainer.WORKSPACE_SCOPE) {
      scope = ISearchPageContainer.WORKSPACE_SCOPE;
    }

    if (!canSearchEnclosingProjects && scope == ISearchPageContainer.SELECTED_PROJECTS_SCOPE) {
      scope = ISearchPageContainer.WORKSPACE_SCOPE;
    }

    return scope;
  }

  private IDialogSettings fSettingsStore;
  private Group fPart;
  // Scope radio buttons
  private Button fUseWorkspace;
  private Button fUseSelection;

  private Button fUseProject;
  private Button fUseWorkingSet;
  private int fScope;
  private boolean fCanSearchEnclosingProjects;

  private Text fWorkingSetText;

  private IWorkingSet[] fWorkingSets;

  // Reference to its search page container (can be null)
  private SearchDialog fSearchDialog;

  private boolean fActiveEditorCanProvideScopeSelection;

  /**
   * Returns a new scope part with workspace as initial scope. The part is not yet created.
   * 
   * @param searchDialog The parent container
   * @param searchEnclosingProjects If true, add the 'search enclosing project' radio button
   */
  public ScopePart(SearchDialog searchDialog, boolean searchEnclosingProjects) {
    fSearchDialog = searchDialog;
    fCanSearchEnclosingProjects = searchEnclosingProjects;

    fSettingsStore = SearchPlugin.getDefault().getDialogSettingsSection(DIALOG_SETTINGS_KEY);
    fScope = getStoredScope(fSettingsStore, searchEnclosingProjects);

    fWorkingSets = getStoredWorkingSets();
  }

  /**
   * Creates this scope part.
   * 
   * @param parent a widget which will be the parent of the new instance (cannot be null)
   * @return Returns the created part control
   */
  public Composite createPart(Composite parent) {
    fPart = new Group(parent, SWT.NONE);
    fPart.setText(SearchMessages.ScopePart_group_text);

    GridLayout layout = new GridLayout();
    layout.numColumns = 4;
    fPart.setLayout(layout);
    fPart.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    fUseWorkspace = new Button(fPart, SWT.RADIO);
    fUseWorkspace.setData(new Integer(ISearchPageContainer.WORKSPACE_SCOPE));
    fUseWorkspace.setText(SearchMessages.ScopePart_workspaceScope_text);

    fUseSelection = new Button(fPart, SWT.RADIO);
    fUseSelection.setData(new Integer(ISearchPageContainer.SELECTION_SCOPE));
    fUseSelection.setText(SearchMessages.ScopePart_selectedResourcesScope_text);

    boolean canSearchInSelection = canSearchInSelection();
    fUseSelection.setEnabled(canSearchInSelection);

    GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
    gd.horizontalIndent = 8;
    fUseSelection.setLayoutData(gd);

    fUseProject = new Button(fPart, SWT.RADIO);
    fUseProject.setData(new Integer(ISearchPageContainer.SELECTED_PROJECTS_SCOPE));
    fUseProject.setText(SearchMessages.ScopePart_enclosingProjectsScope_text);
    fUseProject.setEnabled(fSearchDialog.getEnclosingProjectNames().length > 0);

    gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
    gd.horizontalSpan = 2;
    gd.horizontalIndent = 8;
    fUseProject.setLayoutData(gd);
    if (!fCanSearchEnclosingProjects) {
      fUseProject.setVisible(false);
    }

    fUseWorkingSet = new Button(fPart, SWT.RADIO);
    fUseWorkingSet.setData(new Integer(ISearchPageContainer.WORKING_SET_SCOPE));
    fUseWorkingSet.setText(SearchMessages.ScopePart_workingSetScope_text);
    fWorkingSetText = new Text(fPart, SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY);
    fWorkingSetText.getAccessible().addAccessibleListener(new AccessibleAdapter() {
      @Override
      public void getName(AccessibleEvent e) {
        e.result = SearchMessages.ScopePart_workingSetText_accessible_label;
      }
    });

    Button chooseWorkingSet = new Button(fPart, SWT.PUSH);
    chooseWorkingSet.setLayoutData(new GridData());
    chooseWorkingSet.setText(SearchMessages.ScopePart_workingSetChooseButton_text);
    SWTUtil.setButtonDimensionHint(chooseWorkingSet);
    chooseWorkingSet.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        if (handleChooseWorkingSet()) {
          setSelectedScope(ISearchPageContainer.WORKING_SET_SCOPE);
        }
      }
    });
    gd = new GridData(GridData.FILL_HORIZONTAL);
    gd.horizontalIndent = 8;
    gd.horizontalSpan = 2;
    gd.widthHint = new PixelConverter(fWorkingSetText).convertWidthInCharsToPixels(30);
    fWorkingSetText.setLayoutData(gd);

    // Add scope change listeners
    SelectionAdapter scopeChangedLister = new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        handleScopeChanged(e);
      }
    };
    fUseWorkspace.addSelectionListener(scopeChangedLister);
    fUseSelection.addSelectionListener(scopeChangedLister);
    fUseProject.addSelectionListener(scopeChangedLister);
    fUseWorkingSet.addSelectionListener(scopeChangedLister);

    // Set initial scope
    setSelectedScope(fScope);

    // Set initial working set
    if (fWorkingSets != null) {
      fWorkingSetText.setText(toString(fWorkingSets));
    }

    return fPart;
  }

  /**
   * Returns the scope selected in this part
   * 
   * @return the selected scope
   */
  public int getSelectedScope() {
    return fScope;
  }

  /**
   * Returns the selected working set of this part.
   * 
   * @return the selected working set or null - if the scope is not WORKING_SET_SCOPE - if there is
   *         no working set selected
   */
  public IWorkingSet[] getSelectedWorkingSets() {
    if (getSelectedScope() == ISearchPageContainer.WORKING_SET_SCOPE) {
      return fWorkingSets;
    }
    return null;
  }

  public void setActiveEditorCanProvideScopeSelection(boolean state) {
    fActiveEditorCanProvideScopeSelection = state;
    fUseSelection.setEnabled(canSearchInSelection());

    // Reinitialize the controls
    fScope = getStoredScope(fSettingsStore, fCanSearchEnclosingProjects);
    setSelectedScope(fScope);
  }

  /**
   * Sets the selected scope. This method must only be called on a created part.
   * 
   * @param scope the scope to be selected in this part
   */
  public void setSelectedScope(int scope) {
    Assert.isLegal(scope >= 0 && scope <= 3);
    Assert.isNotNull(fUseWorkspace);
    Assert.isNotNull(fUseSelection);
    Assert.isNotNull(fUseWorkingSet);
    Assert.isNotNull(fUseProject);

    fSettingsStore.put(STORE_SCOPE, scope);

    if (scope == ISearchPageContainer.SELECTED_PROJECTS_SCOPE) {
      if (!fCanSearchEnclosingProjects) {
        SearchPlugin.log(new Status(
            IStatus.WARNING,
            NewSearchUI.PLUGIN_ID,
            IStatus.WARNING,
            "Enclosing projects scope set on search page that does not support it", null)); //$NON-NLS-1$
        scope = ISearchPageContainer.WORKSPACE_SCOPE;
      } else if (!fUseProject.isEnabled()) {
        scope = ISearchPageContainer.WORKSPACE_SCOPE;
      }
    } else if (scope == ISearchPageContainer.SELECTION_SCOPE && !fUseSelection.isEnabled()) {
      scope = fUseProject.isEnabled() ? ISearchPageContainer.SELECTED_PROJECTS_SCOPE
          : ISearchPageContainer.WORKSPACE_SCOPE;
    }
    fScope = scope;

    fUseWorkspace.setSelection(scope == ISearchPageContainer.WORKSPACE_SCOPE);
    fUseSelection.setSelection(scope == ISearchPageContainer.SELECTION_SCOPE);
    fUseProject.setSelection(scope == ISearchPageContainer.SELECTED_PROJECTS_SCOPE);
    fUseWorkingSet.setSelection(scope == ISearchPageContainer.WORKING_SET_SCOPE);

    updateSearchPageContainerActionPerformedEnablement();

  }

  /**
   * Sets the selected working set for this part. This method must only be called on a created part.
   * 
   * @param workingSets the working set to be selected
   */
  public void setSelectedWorkingSets(IWorkingSet[] workingSets) {
    Assert.isNotNull(workingSets);
    setSelectedScope(ISearchPageContainer.WORKING_SET_SCOPE);
    fWorkingSets = null;
    Set<IWorkingSet> existingWorkingSets = new HashSet<IWorkingSet>(workingSets.length);
    for (int i = 0; i < workingSets.length; i++) {
      String name = workingSets[i].getName();
      IWorkingSet workingSet = PlatformUI.getWorkbench().getWorkingSetManager().getWorkingSet(name);
      if (workingSet != null) {
        existingWorkingSets.add(workingSet);
      }
    }
    if (!existingWorkingSets.isEmpty()) {
      fWorkingSets = existingWorkingSets.toArray(new IWorkingSet[existingWorkingSets.size()]);
    }

    saveState();

    if (fWorkingSetText != null) {
      fWorkingSetText.setText(toString(fWorkingSets));
    }
  }

  void setVisible(boolean state) {
    if (state) {
      fPart.layout();
    }
    fPart.setVisible(state);
  }

  private boolean canSearchInSelection() {
    ISelection selection = fSearchDialog.getSelection();
    return (selection instanceof IStructuredSelection) && !selection.isEmpty()
        || fActiveEditorCanProvideScopeSelection && fSearchDialog.getActiveEditorInput() != null;

  }

  private IWorkingSet[] getStoredWorkingSets() {
    String[] lruWorkingSetNames = fSettingsStore.getArray(STORE_LRU_WORKING_SET_NAMES);

    IWorkingSetManager workingSetManager = PlatformUI.getWorkbench().getWorkingSetManager();
    if (lruWorkingSetNames != null) {
      Set<IWorkingSet> existingWorkingSets = new HashSet<IWorkingSet>(lruWorkingSetNames.length);
      for (int i = 0; i < lruWorkingSetNames.length; i++) {
        IWorkingSet workingSet = getWorkingSet(workingSetManager, lruWorkingSetNames[i]);
        if (workingSet != null) {
          existingWorkingSets.add(workingSet);
        }
      }
      if (!existingWorkingSets.isEmpty()) {
        return existingWorkingSets.toArray(new IWorkingSet[existingWorkingSets.size()]);
      }
    } else {
      // Backward compatibility
      String workingSetName = fSettingsStore.get(STORE_LRU_WORKING_SET_NAME);
      if (workingSetName != null) {
        IWorkingSet workingSet = getWorkingSet(workingSetManager, workingSetName);
        if (workingSet != null) {
          return new IWorkingSet[] {workingSet};
        }
      }
    }
    return null;
  }

  private IWorkingSet getWorkingSet(IWorkingSetManager workingSetManager, String storedName) {
    if (storedName.length() == 0) {
      IWorkbenchPage page = fSearchDialog.getWorkbenchWindow().getActivePage();
      if (page != null) {
        return page.getAggregateWorkingSet();
      }
      return null;
    }
    return workingSetManager.getWorkingSet(storedName);
  }

  private boolean handleChooseWorkingSet() {
    IWorkingSetManager workingSetManager = PlatformUI.getWorkbench().getWorkingSetManager();
    IWorkingSetSelectionDialog dialog = workingSetManager.createWorkingSetSelectionDialog(
        fUseSelection.getShell(),
        true);

    if (fWorkingSets != null) {
      dialog.setSelection(fWorkingSets);
    }
    if (dialog.open() == Window.OK) {
      Object[] result = dialog.getSelection();
      if (result.length > 0) {
        setSelectedWorkingSets((IWorkingSet[]) result);
        return true;
      }
      fWorkingSetText.setText(""); //$NON-NLS-1$
      fWorkingSets = null;
      if (fScope == ISearchPageContainer.WORKING_SET_SCOPE) {
        setSelectedScope(ISearchPageContainer.WORKSPACE_SCOPE);
      }
      return false;
    }
    if (fWorkingSets != null) {
      // test if selected working set has been removed
      int i = 0;
      while (i < fWorkingSets.length) {
        IWorkingSet workingSet = fWorkingSets[i];
        if (!workingSet.isAggregateWorkingSet()
            && workingSetManager.getWorkingSet(workingSet.getName()) == null) {
          break;
        }
        i++;
      }
      if (i < fWorkingSets.length) {
        fWorkingSetText.setText(""); //$NON-NLS-1$
        fWorkingSets = null;
        updateSearchPageContainerActionPerformedEnablement();
      }
    }
    return false;
  }

  private void handleScopeChanged(SelectionEvent e) {
    Object source = e.getSource();
    if (source instanceof Button) {
      Button button = (Button) source;
      if (button.getSelection()) {
        setSelectedScope(((Integer) button.getData()).intValue());
      }
    }
  }

  /**
   * Saves the last recently used working sets, if any.
   */
  private void saveState() {
    if (fWorkingSets != null && fWorkingSets.length > 0) {
      String[] existingWorkingSetNames = new String[fWorkingSets.length];
      for (int i = 0; i < fWorkingSets.length; i++) {
        IWorkingSet curr = fWorkingSets[i];
        // use empty name for aggregateWS
        existingWorkingSetNames[i] = curr.isAggregateWorkingSet() ? "" : curr.getName(); //$NON-NLS-1$
      }
      fSettingsStore.put(STORE_LRU_WORKING_SET_NAMES, existingWorkingSetNames);
    }
  }

  private void updateSearchPageContainerActionPerformedEnablement() {
    fSearchDialog.notifyScopeSelectionChanged();
  }
}
