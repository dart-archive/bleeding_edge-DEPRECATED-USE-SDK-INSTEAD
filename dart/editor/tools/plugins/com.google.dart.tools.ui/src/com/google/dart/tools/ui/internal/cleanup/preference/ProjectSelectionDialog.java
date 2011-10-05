/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui.internal.cleanup.preference;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.model.DartModel;
import com.google.dart.tools.core.model.DartProject;
import com.google.dart.tools.ui.DartElementComparator;
import com.google.dart.tools.ui.DartElementLabelProvider;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.StandardDartElementContentProvider;
import com.google.dart.tools.ui.internal.dialogs.StatusInfo;
import com.google.dart.tools.ui.internal.preferences.PreferencesMessages;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.SelectionStatusDialog;

import java.util.Set;

public class ProjectSelectionDialog extends SelectionStatusDialog {

  // the visual selection widget group
  private TableViewer fTableViewer;
  private Set<DartProject> fProjectsWithSpecifics;

  // sizing constants
  private final static int SIZING_SELECTION_WIDGET_HEIGHT = 250;
  private final static int SIZING_SELECTION_WIDGET_WIDTH = 300;

  private final static String DIALOG_SETTINGS_SHOW_ALL = "ProjectSelectionDialog.show_all"; //$NON-NLS-1$

  private ViewerFilter fFilter;

  public ProjectSelectionDialog(Shell parentShell, Set<DartProject> projectsWithSpecifics) {
    super(parentShell);
    setTitle(PreferencesMessages.ProjectSelectionDialog_title);
    setMessage(PreferencesMessages.ProjectSelectionDialog_desciption);
    fProjectsWithSpecifics = projectsWithSpecifics;

    fFilter = new ViewerFilter() {
      @Override
      public boolean select(Viewer viewer, Object parentElement, Object element) {
        return fProjectsWithSpecifics.contains(element);
      }
    };
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.dialogs.SelectionStatusDialog#computeResult()
   */
  @Override
  protected void computeResult() {
  }

  /*
   * (non-Javadoc) Method declared on Dialog.
   */
  @Override
  protected Control createDialogArea(Composite parent) {
    // page group
    Composite composite = (Composite) super.createDialogArea(parent);

    Font font = parent.getFont();
    composite.setFont(font);

    createMessageArea(composite);

    fTableViewer = new TableViewer(composite, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
    fTableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
      @Override
      public void selectionChanged(SelectionChangedEvent event) {
        doSelectionChanged(((IStructuredSelection) event.getSelection()).toArray());
      }
    });
    fTableViewer.addDoubleClickListener(new IDoubleClickListener() {
      @Override
      public void doubleClick(DoubleClickEvent event) {
        okPressed();
      }
    });
    GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
    data.heightHint = SIZING_SELECTION_WIDGET_HEIGHT;
    data.widthHint = SIZING_SELECTION_WIDGET_WIDTH;
    fTableViewer.getTable().setLayoutData(data);

    fTableViewer.setLabelProvider(new DartElementLabelProvider());
    fTableViewer.setContentProvider(new StandardDartElementContentProvider());
    fTableViewer.setComparator(new DartElementComparator());
    fTableViewer.getControl().setFont(font);

    Button checkbox = new Button(composite, SWT.CHECK);
    checkbox.setText(PreferencesMessages.ProjectSelectionDialog_filter);
    checkbox.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, true, false));
    checkbox.addSelectionListener(new SelectionListener() {
      @Override
      public void widgetDefaultSelected(SelectionEvent e) {
        updateFilter(((Button) e.widget).getSelection());
      }

      @Override
      public void widgetSelected(SelectionEvent e) {
        updateFilter(((Button) e.widget).getSelection());
      }
    });
    IDialogSettings dialogSettings = DartToolsPlugin.getDefault().getDialogSettings();
    boolean doFilter = !dialogSettings.getBoolean(DIALOG_SETTINGS_SHOW_ALL)
        && !fProjectsWithSpecifics.isEmpty();
    checkbox.setSelection(doFilter);
    updateFilter(doFilter);

    DartModel input = DartCore.create(ResourcesPlugin.getWorkspace().getRoot());
    fTableViewer.setInput(input);

    doSelectionChanged(new Object[0]);
    Dialog.applyDialogFont(composite);
    return composite;
  }

  protected void updateFilter(boolean selected) {
    if (selected) {
      fTableViewer.addFilter(fFilter);
    } else {
      fTableViewer.removeFilter(fFilter);
    }
    DartToolsPlugin.getDefault().getDialogSettings().put(DIALOG_SETTINGS_SHOW_ALL, !selected);
  }

  private void doSelectionChanged(Object[] objects) {
    if (objects.length != 1) {
      updateStatus(new StatusInfo(IStatus.ERROR, "")); //$NON-NLS-1$
      setSelectionResult(null);
    } else {
      updateStatus(new StatusInfo());
      setSelectionResult(objects);
    }
  }
}
