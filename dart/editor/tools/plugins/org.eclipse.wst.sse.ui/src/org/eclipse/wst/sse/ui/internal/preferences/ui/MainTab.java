/*******************************************************************************
 * Copyright (c) 2001, 2011 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation David Schneider, david.schneider@unisys.com - [142500] WTP properties pages fonts
 * don't follow Eclipse preferences
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.preferences.ui;

import com.ibm.icu.util.StringTokenizer;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.wst.sse.core.internal.provisional.tasks.TaskTag;
import org.eclipse.wst.sse.core.internal.tasks.TaskTagPreferenceKeys;
import org.eclipse.wst.sse.core.utils.StringUtils;
import org.eclipse.wst.sse.ui.internal.SSEUIMessages;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class MainTab implements IPreferenceTab {
  public class TaskTagDialog extends Dialog {
    public TaskTag taskTag = null;

    Combo priorityCombo = null;
    Text tagText = null;

    public TaskTagDialog(Shell parentShell) {
      super(parentShell);
      setShellStyle(getShellStyle() | SWT.RESIZE);
    }

    protected void configureShell(Shell newShell) {
      super.configureShell(newShell);
      newShell.setText(SSEUIMessages.TaskTagPreferenceTab_5); //$NON-NLS-1$
    }

    protected Control createButtonBar(Composite parent) {
      Control c = super.createButtonBar(parent);
      getButton(IDialogConstants.OK_ID).setEnabled(taskTag == null || taskTag.getTag().length() > 0);
      return c;
    }

    protected Control createDialogArea(Composite parent) {
      Composite composite = new Composite(parent, SWT.NONE);
      composite.setLayout(new GridLayout(2, false));
      GridData gridData = new GridData(GridData.FILL_BOTH);
      gridData.widthHint = parent.getDisplay().getClientArea().width / 5;
      composite.setLayoutData(gridData);
      Label label = new Label(composite, SWT.NONE);
      label.setText(SSEUIMessages.TaskTagPreferenceTab_6); //$NON-NLS-1$
      label.setLayoutData(new GridData());
      tagText = new Text(composite, SWT.BORDER);
      tagText.setText(taskTag != null ? taskTag.getTag() : ""); //$NON-NLS-1$
      tagText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
      tagText.addModifyListener(new ModifyListener() {
        public void modifyText(ModifyEvent e) {
          String testTag = tagText.getText();
          String[] tags = new String[fTaskTags.length];
          for (int i = 0; i < tags.length; i++) {
            tags[i] = fTaskTags[i].getTag();
          }
          getButton(IDialogConstants.OK_ID).setEnabled(
              tagText.getText().length() > 0 && !Arrays.asList(tags).contains(testTag));
          taskTag = new TaskTag(tagText.getText(), priorityCombo.getSelectionIndex());
        }
      });

      label = new Label(composite, SWT.NONE);
      label.setText(SSEUIMessages.TaskTagPreferenceTab_7); //$NON-NLS-1$
      label.setLayoutData(new GridData());
      priorityCombo = new Combo(composite, SWT.READ_ONLY | SWT.SINGLE);
      priorityCombo.setItems(new String[] {
          SSEUIMessages.TaskTagPreferenceTab_0, SSEUIMessages.TaskTagPreferenceTab_1,
          SSEUIMessages.TaskTagPreferenceTab_2});
      priorityCombo.select(taskTag != null ? taskTag.getPriority() : TaskTag.PRIORITY_NORMAL);
      priorityCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
      priorityCombo.addSelectionListener(new SelectionAdapter() {
        public void widgetSelected(SelectionEvent e) {
          taskTag = new TaskTag(taskTag.getTag(), priorityCombo.getSelectionIndex());
        }
      });
      Dialog.applyDialogFont(parent);
      return composite;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.dialogs.Dialog#okPressed()
     */
    protected void okPressed() {
      taskTag = new TaskTag(tagText.getText(), priorityCombo.getSelectionIndex());
      super.okPressed();
    }
  }

  public class TaskTagTableLabelProvider extends LabelProvider implements ITableLabelProvider {
    public TaskTagTableLabelProvider() {
      super();
    }

    public Image getColumnImage(Object element, int columnIndex) {
      return null;
    }

    public String getColumnText(Object element, int columnIndex) {
      if (columnIndex < 1)
        return ((TaskTag) element).getTag();
      for (int i = 0; i < fTaskTags.length; i++) {
        if (fTaskTags[i].equals(element)) {
          if (fTaskTags[i].getPriority() == IMarker.PRIORITY_LOW) {
            return SSEUIMessages.TaskTagPreferenceTab_0; //$NON-NLS-1$
          } else if (fTaskTags[i].getPriority() == IMarker.PRIORITY_HIGH) {
            return SSEUIMessages.TaskTagPreferenceTab_2; //$NON-NLS-1$
          } else {
            return SSEUIMessages.TaskTagPreferenceTab_1; //$NON-NLS-1$
          }
        }
      }
      return SSEUIMessages.TaskTagPreferenceTab_3; //$NON-NLS-1$
    }
  }

  private static final boolean _debugPreferences = "true".equalsIgnoreCase(Platform.getDebugOption("org.eclipse.wst.sse.core/tasks/preferences")); //$NON-NLS-1$ //$NON-NLS-2$

  private Control fControl;

  private TaskTag[] fOriginalTaskTags;
  private TaskTagPreferencePage fOwner = null;
  private IScopeContext[] fPreferencesLookupOrder = null;

  private IPreferencesService fPreferencesService = null;

  private TaskTag[] fTaskTags;

  private TableViewer valueTable = null;

  public MainTab(TaskTagPreferencePage parent, IPreferencesService preferencesService,
      IScopeContext[] lookupOrder) {
    super();
    fOwner = parent;
    fPreferencesLookupOrder = lookupOrder;
    fPreferencesService = preferencesService;
  }

  private void addTag() {
    TaskTagDialog dlg = new TaskTagDialog(fControl.getShell());
    int result = dlg.open();
    if (result == Window.OK) {
      TaskTag newTag = dlg.taskTag;
      List newTags = new ArrayList(Arrays.asList(fTaskTags));
      newTags.add(newTag);
      fTaskTags = (TaskTag[]) newTags.toArray(new TaskTag[newTags.size()]);
      valueTable.setInput(fTaskTags);
      valueTable.getTable().setSelection(fTaskTags.length - 1);
    }
  }

  public Control createContents(Composite tabFolder) {
    loadPreferenceValues();
    fOriginalTaskTags = fTaskTags;

    Composite composite = new Composite(tabFolder, SWT.NONE);
    composite.setLayout(new GridLayout(2, false));
    fControl = composite;

    Label description = new Label(composite, SWT.NONE);
    description.setText(SSEUIMessages.TaskTagPreferenceTab_33); //$NON-NLS-1$
//		description.setBackground(composite.getBackground());
    GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1);
    // https://bugs.eclipse.org/bugs/show_bug.cgi?id=104403
    Point sizeHint = description.computeSize(SWT.DEFAULT, SWT.DEFAULT);
    gd.widthHint = sizeHint.x;
    description.setLayoutData(gd);

    valueTable = new TableViewer(composite, SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION
        | SWT.H_SCROLL | SWT.V_SCROLL);
    valueTable.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
    TableColumn textColumn = new TableColumn(valueTable.getTable(), SWT.NONE, 0);
    textColumn.setText(SSEUIMessages.TaskTagPreferenceTab_12); //$NON-NLS-1$
    TableColumn priorityColumn = new TableColumn(valueTable.getTable(), SWT.NONE, 1);
    priorityColumn.setText(SSEUIMessages.TaskTagPreferenceTab_13); //$NON-NLS-1$
    valueTable.setContentProvider(new ArrayContentProvider());
    valueTable.setLabelProvider(new TaskTagTableLabelProvider());
    valueTable.getTable().setLinesVisible(true);
    valueTable.getTable().setHeaderVisible(true);
    TableLayout layout = new TableLayout();
    layout.addColumnData(new ColumnWeightData(1, 140, true));
    layout.addColumnData(new ColumnWeightData(1, 140, true));
    valueTable.getTable().setLayout(layout);

    Composite buttons = new Composite(composite, SWT.NONE);
    buttons.setLayout(new GridLayout());
    buttons.setLayoutData(new GridData(GridData.FILL_VERTICAL));

    final Button addButton = new Button(buttons, SWT.PUSH);
    addButton.setText(SSEUIMessages.TaskTagPreferenceTab_14); //$NON-NLS-1$
    addButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
    final Button editButton = new Button(buttons, SWT.PUSH);
    editButton.setText(SSEUIMessages.TaskTagPreferenceTab_15); //$NON-NLS-1$
    editButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
    final Button removeButton = new Button(buttons, SWT.PUSH);
    removeButton.setText(SSEUIMessages.TaskTagPreferenceTab_16); //$NON-NLS-1$
    removeButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));

    editButton.setEnabled(false);
    removeButton.setEnabled(false);

    Label warning = new Label(composite, SWT.NONE);
    warning.setLayoutData(new GridData());
    warning.setText(SSEUIMessages.TaskTagPreferenceTab_19); //$NON-NLS-1$

    final ISelectionChangedListener selectionChangedListener = new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        ISelection selection = valueTable.getSelection();
        editButton.setEnabled(((IStructuredSelection) selection).size() == 1);
        removeButton.setEnabled(!selection.isEmpty());
      }
    };
    valueTable.addPostSelectionChangedListener(selectionChangedListener);
    addButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        addTag();
        valueTable.getTable().setSelection(fTaskTags.length - 1);
        selectionChangedListener.selectionChanged(null);
      }
    });
    editButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        int i = valueTable.getTable().getSelectionIndex();
        editTag(i);
        if (i >= 0) {
          valueTable.getTable().setSelection(i);
          selectionChangedListener.selectionChanged(null);
        }
      }
    });
    removeButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        int i = valueTable.getTable().getSelectionIndex();
        removeTags(valueTable.getSelection());
        if (i >= 0 && i < fTaskTags.length) {
          valueTable.getTable().setSelection(i);
        }
      }
    });
    valueTable.addDoubleClickListener(new IDoubleClickListener() {
      public void doubleClick(DoubleClickEvent event) {
        int i = valueTable.getTable().getSelectionIndex();
        editTag(i);
        if (i >= 0) {
          valueTable.getTable().setSelection(i);
          selectionChangedListener.selectionChanged(null);
        }
      }
    });

    valueTable.setInput(fTaskTags);

    return composite;
  }

  /**
   * @param selection
   */
  private void editTag(int i) {
    if (i < 0) {
      return;
    }

    int selection = valueTable.getTable().getSelectionIndex();
    TaskTagDialog dlg = new TaskTagDialog(fControl.getShell());
    dlg.taskTag = fTaskTags[selection];
    int result = dlg.open();
    if (result == Window.OK) {
      fTaskTags[selection] = dlg.taskTag;
      valueTable.refresh();
    }
  }

  public String getTitle() {
    return SSEUIMessages.TaskTagPreferenceTab_20;
  }

  private void loadPreferenceValues() {
    String tags = fPreferencesService.getString(TaskTagPreferenceKeys.TASK_TAG_NODE,
        TaskTagPreferenceKeys.TASK_TAG_TAGS, "", fPreferencesLookupOrder); //$NON-NLS-1$
    String priorities = fPreferencesService.getString(TaskTagPreferenceKeys.TASK_TAG_NODE,
        TaskTagPreferenceKeys.TASK_TAG_PRIORITIES, "", fPreferencesLookupOrder); //$NON-NLS-1$
    loadTagsAndPrioritiesFrom(tags, priorities);
  }

  /**
   * @param tags
   * @param priorities
   */
  private void loadTagsAndPrioritiesFrom(String tagString, String priorityString) {
    String[] tags = StringUtils.unpack(tagString);

    StringTokenizer toker = null;
    List list = new ArrayList();

    toker = new StringTokenizer(priorityString, ","); //$NON-NLS-1$
    while (toker.hasMoreTokens()) {
      Integer number = null;
      try {
        number = Integer.valueOf(toker.nextToken());
      } catch (NumberFormatException e) {
        number = new Integer(IMarker.PRIORITY_NORMAL);
      }
      list.add(number);
    }
    Integer[] priorities = (Integer[]) list.toArray(new Integer[0]);

    fTaskTags = new TaskTag[Math.min(tags.length, priorities.length)];
    for (int i = 0; i < fTaskTags.length; i++) {
      fTaskTags[i] = new TaskTag(tags[i], priorities[i].intValue());
    }
  }

  public void performApply() {
    save();

    if (!Arrays.equals(fOriginalTaskTags, fTaskTags)) {
      fOwner.requestRedetection();
    }
    fOriginalTaskTags = fTaskTags;
  }

  public void performDefaults() {
    if (_debugPreferences) {
      System.out.println("Loading defaults in " + getClass().getName()); //$NON-NLS-1$
    }
    final IEclipsePreferences defaultPreferences = fPreferencesLookupOrder.length > 0
        ? fPreferencesLookupOrder[fPreferencesLookupOrder.length - 1].getNode(TaskTagPreferenceKeys.TASK_TAG_NODE)
        : null;
    String tags = null;
    String priorities = null;
    if (defaultPreferences != null) {
      tags = defaultPreferences.get(TaskTagPreferenceKeys.TASK_TAG_TAGS, null);
      priorities = defaultPreferences.get(TaskTagPreferenceKeys.TASK_TAG_PRIORITIES, null);
    }
    loadTagsAndPrioritiesFrom(tags, priorities);
    int selection = valueTable.getTable().getSelectionIndex();
    valueTable.setInput(fTaskTags);
    valueTable.getTable().setSelection(selection);
  }

  public void performOk() {
    performApply();
  }

  /**
   * @param selection
   */
  private void removeTags(ISelection selection) {
    IStructuredSelection structuredSelection = (IStructuredSelection) selection;
    List taskTags = new ArrayList(Arrays.asList(fTaskTags));
    taskTags.removeAll(structuredSelection.toList());
    fTaskTags = (TaskTag[]) taskTags.toArray(new TaskTag[taskTags.size()]);
    valueTable.setInput(fTaskTags);
  }

  private void save() {
    IEclipsePreferences defaultPreferences = new DefaultScope().getNode(TaskTagPreferenceKeys.TASK_TAG_NODE);
    String defaultTags = defaultPreferences.get(TaskTagPreferenceKeys.TASK_TAG_TAGS, null);
    String defaultPriorities = defaultPreferences.get(TaskTagPreferenceKeys.TASK_TAG_PRIORITIES,
        null);

    StringBuffer buf = new StringBuffer();
    for (int i = 0; i < fTaskTags.length; i++) {
      if (i > 0) {
        buf.append(","); //$NON-NLS-1$
      }
      buf.append(fTaskTags[i].getTag());
    }
    String currentTags = buf.toString();
    if (currentTags.equals(defaultTags)
        && !fPreferencesLookupOrder[0].getName().equals(DefaultScope.SCOPE)) {
      if (_debugPreferences) {
        System.out.println(getClass().getName()
            + " removing " + TaskTagPreferenceKeys.TASK_TAG_TAGS + " from scope " + fPreferencesLookupOrder[0].getName() + ":" + fPreferencesLookupOrder[0].getLocation()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
      }
      fPreferencesLookupOrder[0].getNode(TaskTagPreferenceKeys.TASK_TAG_NODE).remove(
          TaskTagPreferenceKeys.TASK_TAG_TAGS);
    } else {
      fOwner.requestRedetection();
      if (_debugPreferences) {
        System.out.println(getClass().getName()
            + " setting " + TaskTagPreferenceKeys.TASK_TAG_TAGS + " \"" + currentTags + "\" in scope " + fPreferencesLookupOrder[0].getName() + ":" + fPreferencesLookupOrder[0].getLocation()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
      }
      fPreferencesLookupOrder[0].getNode(TaskTagPreferenceKeys.TASK_TAG_NODE).put(
          TaskTagPreferenceKeys.TASK_TAG_TAGS, currentTags);
    }

    StringBuffer buf2 = new StringBuffer();
    for (int i = 0; i < fTaskTags.length; i++) {
      if (i > 0) {
        buf2.append(","); //$NON-NLS-1$
      }
      buf2.append(String.valueOf(fTaskTags[i].getPriority()));
    }
    String priorities = buf2.toString();

    if (priorities.equals(defaultPriorities)
        && !fPreferencesLookupOrder[0].getName().equals(DefaultScope.SCOPE)) {
      if (_debugPreferences) {
        System.out.println(getClass().getName()
            + " removing " + TaskTagPreferenceKeys.TASK_TAG_PRIORITIES + " from scope " + fPreferencesLookupOrder[0].getName() + ":" + fPreferencesLookupOrder[0].getLocation()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
      }
      fPreferencesLookupOrder[0].getNode(TaskTagPreferenceKeys.TASK_TAG_NODE).remove(
          TaskTagPreferenceKeys.TASK_TAG_PRIORITIES);
    } else {
      fOwner.requestRedetection();
      if (_debugPreferences) {
        System.out.println(getClass().getName()
            + " setting " + TaskTagPreferenceKeys.TASK_TAG_PRIORITIES + " \"" + priorities + "\" in scope " + fPreferencesLookupOrder[0].getName() + ":" + fPreferencesLookupOrder[0].getLocation()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
      }
      fPreferencesLookupOrder[0].getNode(TaskTagPreferenceKeys.TASK_TAG_NODE).put(
          TaskTagPreferenceKeys.TASK_TAG_PRIORITIES, priorities);
    }
  }
}
