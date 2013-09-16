/*******************************************************************************
 * Copyright (c) 2004, 2008 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.common.ui.internal.search.dialogs;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.IWorkingSetSelectionDialog;
import org.eclipse.wst.common.core.search.scope.ProjectSearchScope;
import org.eclipse.wst.common.core.search.scope.SearchScope;
import org.eclipse.wst.common.core.search.scope.WorkingSetSearchScope;
import org.eclipse.wst.common.core.search.scope.WorkspaceSearchScope;
import org.eclipse.wst.common.ui.internal.Messages;

public class ScopedComponentSearchListDialog extends ComponentSearchListDialog {
  protected final static String DEFAULT_NAME_FIELD_TITLE = Messages._UI_LABEL_COMPONENT_NAME;
  protected final static String DEFAULT_LIST_TITLE = Messages._UI_LABEL_MATCHING_COMPONENTS;
  public static final String SCOPE_SPECIFIED_FILE = Messages._UI_LABEL_SPECIFIED_FILE;
  public static final String SCOPE_ENCLOSING_PROJECT = Messages._UI_LABEL_ENCLOSING_PROJECT;
  public static final String SCOPE_WORKSPACE = Messages._UI_LABEL_WORKSPACE;
  public static final String SCOPE_CURRENT_RESOURCE = Messages._UI_LABEL_CURRENT_RESOURCE;
  public static final String SCOPE_WORKING_SETS = Messages._UI_LABEL_WORKING_SETS;

  private String currentSearchScope = SCOPE_CURRENT_RESOURCE;
  protected Button chooseButton;
  protected Button[] radioButton = new Button[4];
  protected String filterLabel;
  protected String listTitle;
  protected IResource currentResource;
  protected Composite selectWorkingSetsGroup;
  protected Text workingSetsText;

  protected static String valueForWorkingSetsText;
  // Show (default/true) or hide (false) the current resource scope button
  private boolean showCurrentResourceScope = true;

  // working sets currently chosen by the user
  private static IWorkingSet[] workingSets;

  public ScopedComponentSearchListDialog(Shell shell, String dialogTitle,
      ComponentSearchListDialogConfiguration configuration) {
    super(shell, dialogTitle, configuration);
  }

  public void setFilterLabel(String filterLabel) {
    this.filterLabel = filterLabel;
  }

  public void setListTitle(String listTitle) {
    this.listTitle = listTitle;
  }

  protected Control createDialogArea(Composite parent) {
    //super.setFilterLabel(filterLabel != null ? filterLabel : DEFAULT_NAME_FIELD_TITLE);
    //setListLabelText(listTitle != null ? listTitle : DEFAULT_LIST_TITLE);
    super.createDialogArea(parent);
    // We use the Composite topComposite to create additional widgets
    GridLayout layout = new GridLayout();
    layout.marginWidth = 0;
    bottomComposite.setLayout(layout);
    Group group = new Group(bottomComposite, SWT.NONE);
    GridLayout gridLayout = new GridLayout(3, false);
    group.setLayout(gridLayout);
    group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    group.setText(Messages._UI_LABEL_SEARCH_SCOPE);
    ScopeChangeListener scopeChangeListener = new ScopeChangeListener();
    radioButton[0] = new Button(group, SWT.RADIO);
    radioButton[0].setText(SCOPE_WORKSPACE);
    radioButton[1] = new Button(group, SWT.RADIO);
    radioButton[1].setText(SCOPE_ENCLOSING_PROJECT);
    radioButton[2] = new Button(group, SWT.RADIO);
    radioButton[2].setText(SCOPE_CURRENT_RESOURCE);
    radioButton[3] = new Button(group, SWT.RADIO);
    radioButton[3].setText(SCOPE_WORKING_SETS);
    // https://bugs.eclipse.org/bugs/show_bug.cgi?id=250815 
    // If the current resource scope button is hidden, it no longer is the default scope.  We must select one of the remaining 
    // visible radio buttons (Enclosing Project) and make it the default.    
    radioButton[2].setVisible(showCurrentResourceScope);
    if (!showCurrentResourceScope) {
      currentSearchScope = SCOPE_ENCLOSING_PROJECT;
      if (currentResource != null) {
        // This is to ensure that once the dialog is invoked, the list is populated with the
        // appropriate components based on this search scope.
        populateMasterComponentList(new ProjectSearchScope(currentResource.getFullPath()));
      }
    }
    for (int i = 0; i < radioButton.length; i++) {
      if (radioButton[i].getText().equals(currentSearchScope)) {
        radioButton[i].setSelection(true);
      }
      radioButton[i].addSelectionListener(scopeChangeListener);
    }

    selectWorkingSetsGroup = new Composite(group, SWT.NONE);
    GridData gd = new GridData(GridData.FILL_HORIZONTAL);
    gd.horizontalSpan = 2;
    selectWorkingSetsGroup.setLayoutData(gd);
    GridLayout workingSetGroupLayout = new GridLayout(2, false);
    workingSetGroupLayout.marginWidth = 0;
    workingSetGroupLayout.marginHeight = 0;
    selectWorkingSetsGroup.setLayout(workingSetGroupLayout);

    workingSetsText = new Text(selectWorkingSetsGroup, SWT.BORDER | SWT.READ_ONLY);
    workingSetsText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    if (valueForWorkingSetsText != null)
      workingSetsText.setText(valueForWorkingSetsText);

    chooseButton = new Button(selectWorkingSetsGroup, SWT.NONE);
    chooseButton.setText(Messages._UI_LABEL_CHOOSE);
    chooseButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        Shell shell = getShell();
        IWorkingSetSelectionDialog dialog = PlatformUI.getWorkbench().getWorkingSetManager().createWorkingSetSelectionDialog(
            shell, true);
        if (dialog.open() == Window.OK) {
          workingSets = dialog.getSelection();
          valueForWorkingSetsText = "";
          if (workingSets != null) {
            for (int i = 0; i < workingSets.length; i++) {
              valueForWorkingSetsText += workingSets[i].getLabel();
              // if not the last item, we add a comma
              if (i != workingSets.length - 1)
                valueForWorkingSetsText += ", ";
            }
          }
          workingSetsText.setText(valueForWorkingSetsText);
        }
        // Set working sets radio button enabled, disable other buttons
        radioButton[2].setSelection(false);
        radioButton[1].setSelection(false);
        radioButton[0].setSelection(false);
        radioButton[3].setSelection(true);

        scopeChangeHandler(radioButton[3]);
      }
    });

    return parent;
  } /*
     * Returns the selected search scope.
     */

  public String getSearchScope() {
    return currentSearchScope;
  }

  private void scopeChangeHandler(Button b) {
    if ((b.getStyle() & SWT.RADIO) != 0) {
      currentSearchScope = b.getText();

      // TODO (cs) need to do some work to make the default search scope
      // more well defined, currently the default behaviour is to pass a null
      // argument in to populateMasterComponentList but we should provide
      // getters/setters to allow the default to be controlled          
      SearchScope scope = null;
      if (currentSearchScope == SCOPE_ENCLOSING_PROJECT && currentResource != null) {
        scope = new ProjectSearchScope(currentResource.getFullPath());
      } else if (currentSearchScope == SCOPE_WORKSPACE) {
        scope = new WorkspaceSearchScope();
      } else if (currentSearchScope == SCOPE_WORKING_SETS) {

        // Constructs the working sets scope from the working sets the user
        // selected
        WorkingSetSearchScope workingSetsScope = new WorkingSetSearchScope();
        if (workingSets != null) {
          for (int i = 0; i < workingSets.length; i++) {
            workingSetsScope.addAWorkingSetToScope(workingSets[i].getElements());
          }
        }
        scope = workingSetsScope;
      }

      populateMasterComponentList(scope);
      refreshTableViewer(getProcessedFilterString());
      // Select the first matching component. Though we should be
      // smarter here
      // and determine if there was a selection before the scope
      // switch (and if
      // the component is still available.
      Table table = componentTableViewer.getTable();
      TableItem items[] = table.getItems();
      if (items.length > 0) {
        TableItem select[] = new TableItem[1];
        select[0] = items[0];
        table.setSelection(select);
      }
      updateCanFinish();
    }
  }

  private class ScopeChangeListener extends SelectionAdapter {
    public void widgetSelected(SelectionEvent e) {
      if (e.widget instanceof Button) {
        Button b = (Button) e.widget;
        if (b.getSelection()) {
          scopeChangeHandler(b);
        }
      }
    }
  }

  public IResource getCurrentResource() {
    return currentResource;
  }

  public void setCurrentResource(IResource currentResource) {
    this.currentResource = currentResource;
  }

  /**
   * This is non-API. See https://bugs.eclipse.org/bugs/show_bug.cgi?id=250815 Show (by default), or
   * hide the current resource search scope button
   * 
   * @param showCurrentResourceScope. false to hide.
   */
  public void setShowCurrentResourceSearchScopeBug250815(boolean showCurrentResourceScope) {
    this.showCurrentResourceScope = showCurrentResourceScope;
  }
}
