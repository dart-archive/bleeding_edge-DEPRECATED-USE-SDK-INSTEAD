/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.preferences.ui;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.wst.sse.core.internal.validate.ValidationMessage;
import org.eclipse.wst.sse.ui.internal.SSEUIMessages;
import org.eclipse.wst.validation.ValidationFramework;
import org.osgi.service.prefs.BackingStoreException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Based on org.eclipse.jdt.internal.ui.preferences.OptionsConfigurationBlock
 */
public abstract class AbstractValidationSettingsPage extends PropertyPreferencePage {

  private List fCombos;
  protected List fExpandables;

  private SelectionListener fSelectionListener;

  private IPreferencesService fPreferencesService = null;

  private static final String SETTINGS_EXPANDED = "expanded"; //$NON-NLS-1$

  private ValidationFramework fValidation;

  private class ComboData {
    private String fKey;
    private int[] fSeverities;
    private int fIndex;
    int originalSeverity = -2;

    public ComboData(String key, int[] severities, int index) {
      fKey = key;
      fSeverities = severities;
      fIndex = index;
    }

    public String getKey() {
      return fKey;
    }

    public void setIndex(int index) {
      fIndex = index;
    }

    public int getIndex() {
      return fIndex;
    }

    /**
     * Sets the severity index based on <code>severity</code>. If the severity doesn't exist, the
     * index is set to -1.
     * 
     * @param severity the severity level
     */
    public void setSeverity(int severity) {
      for (int i = 0; fSeverities != null && i < fSeverities.length; i++) {
        if (fSeverities[i] == severity) {
          fIndex = i;
          return;
        }
      }

      fIndex = -1;
    }

    public int getSeverity() {
      return (fIndex >= 0 && fSeverities != null && fIndex < fSeverities.length)
          ? fSeverities[fIndex] : -1;
    }

    boolean isChanged() {
      return fSeverities[fIndex] != originalSeverity;
    }
  }

  public AbstractValidationSettingsPage() {
    super();
    fCombos = new ArrayList();
    fExpandables = new ArrayList();
    fPreferencesService = Platform.getPreferencesService();
    fValidation = ValidationFramework.getDefault();
  }

  /**
   * Creates a Combo widget in the composite <code>parent</code>. The data in the Combo is
   * associated with <code>key</code>. The Combo data is generated based on the integer
   * <code>values</code> where the index of <code>values</code> corresponds to the index of
   * <code>valueLabels</code>
   * 
   * @param parent the composite to create the combo box in
   * @param label the label to give the combo box
   * @param key the unique key to identify the combo box
   * @param values the values represented by the combo options
   * @param valueLabels the calues displayed in the combo box
   * @param indent how far to indent the combo box label
   * @return the generated combo box
   */
  protected Combo addComboBox(Composite parent, String label, String key, int[] values,
      String[] valueLabels, int indent) {
    GridData gd = new GridData(GridData.FILL, GridData.CENTER, true, false, 2, 1);
    gd.horizontalIndent = indent;

    Label labelControl = new Label(parent, SWT.LEFT);
    labelControl.setFont(JFaceResources.getDialogFont());
    labelControl.setText(label);
    labelControl.setLayoutData(gd);

    Combo comboBox = newComboControl(parent, key, values, valueLabels);
    comboBox.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));

    return comboBox;
  }

  /**
   * Creates a combo box and associates the combo data with the combo box.
   * 
   * @param composite the composite to create the combo box in
   * @param key the unique key to identify the combo box
   * @param values the values represented by the combo options
   * @param valueLabels the values displayed in the combo box
   * @return the generated combo box
   */
  protected Combo newComboControl(Composite composite, String key, int[] values,
      String[] valueLabels) {
    ComboData data = new ComboData(key, values, -1);

    Combo comboBox = new Combo(composite, SWT.READ_ONLY);
    comboBox.setItems(valueLabels);
    comboBox.setData(data);
    comboBox.addSelectionListener(getSelectionListener());
    comboBox.setFont(JFaceResources.getDialogFont());

    makeScrollableCompositeAware(comboBox);

    int severity = -1;
    if (key != null)
      severity = fPreferencesService.getInt(getPreferenceNodeQualifier(), key,
          ValidationMessage.WARNING, createPreferenceScopes());

    if (severity == ValidationMessage.ERROR || severity == ValidationMessage.WARNING
        || severity == ValidationMessage.IGNORE) {
      data.setSeverity(severity);
      data.originalSeverity = severity;
    }

    if (data.getIndex() >= 0)
      comboBox.select(data.getIndex());

    fCombos.add(comboBox);
    return comboBox;
  }

  protected SelectionListener getSelectionListener() {
    if (fSelectionListener == null) {
      fSelectionListener = new SelectionListener() {
        public void widgetDefaultSelected(SelectionEvent e) {
        }

        public void widgetSelected(SelectionEvent e) {
          controlChanged(e.widget);
        }
      };
    }
    return fSelectionListener;
  }

  protected void controlChanged(Widget widget) {
    ComboData data = (ComboData) widget.getData();
    if (widget instanceof Combo) {
      data.setIndex(((Combo) widget).getSelectionIndex());
    } else {
      return;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.wst.sse.ui.internal.preferences.ui.AbstractSettingsPage#storeValues()
   */
  protected void storeValues() {
    if (fCombos == null || fCombos.size() == 0)
      return;

    Iterator it = fCombos.iterator();

    IScopeContext[] contexts = createPreferenceScopes();

    while (it.hasNext()) {
      ComboData data = (ComboData) ((Combo) it.next()).getData();
      if (data.getKey() != null) {
        contexts[0].getNode(getPreferenceNodeQualifier()).putInt(data.getKey(), data.getSeverity());
      }
    }

    for (int i = 0; i < contexts.length; i++) {
      try {
        contexts[i].getNode(getPreferenceNodeQualifier()).flush();
      } catch (BackingStoreException e) {

      }
    }
  }

  protected ExpandableComposite getParentExpandableComposite(Control control) {
    Control parent = control.getParent();
    while (!(parent instanceof ExpandableComposite) && parent != null) {
      parent = parent.getParent();
    }
    if (parent instanceof ExpandableComposite) {
      return (ExpandableComposite) parent;
    }
    return null;
  }

  protected ExpandableComposite createStyleSection(Composite parent, String label, int nColumns) {
    ExpandableComposite excomposite = new ExpandableComposite(parent, SWT.NONE,
        ExpandableComposite.TWISTIE | ExpandableComposite.CLIENT_INDENT);
    excomposite.setText(label);
    excomposite.setExpanded(false);
    excomposite.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT));
    excomposite.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false, nColumns, 1));
    excomposite.addExpansionListener(new ExpansionAdapter() {
      public void expansionStateChanged(ExpansionEvent e) {
        expandedStateChanged((ExpandableComposite) e.getSource());
      }
    });
    fExpandables.add(excomposite);
    makeScrollableCompositeAware(excomposite);
    return excomposite;
  }

  protected Composite createStyleSectionWithContentComposite(Composite parent, String label,
      int nColumns) {
    ExpandableComposite excomposite = new ExpandableComposite(parent, SWT.NONE,
        ExpandableComposite.TWISTIE | ExpandableComposite.CLIENT_INDENT);
    excomposite.setText(label);
    excomposite.setExpanded(false);
    excomposite.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT));
    excomposite.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false, nColumns, 1));
    excomposite.addExpansionListener(new ExpansionAdapter() {
      public void expansionStateChanged(ExpansionEvent e) {
        expandedStateChanged((ExpandableComposite) e.getSource());
      }
    });
    fExpandables.add(excomposite);
    makeScrollableCompositeAware(excomposite);

    Composite inner = new Composite(excomposite, SWT.NONE);
    inner.setFont(excomposite.getFont());
    inner.setLayout(new GridLayout(nColumns, false));
    excomposite.setClient(inner);
    return inner;
  }

  protected final void expandedStateChanged(ExpandableComposite expandable) {
    ScrolledPageContent parentScrolledComposite = getParentScrolledComposite(expandable);
    if (parentScrolledComposite != null) {
      parentScrolledComposite.reflow(true);
    }
  }

  protected void makeScrollableCompositeAware(Control control) {
    ScrolledPageContent parentScrolledComposite = getParentScrolledComposite(control);
    if (parentScrolledComposite != null) {
      parentScrolledComposite.adaptChild(control);
    }
  }

  protected ScrolledPageContent getParentScrolledComposite(Control control) {
    Control parent = control.getParent();
    while (!(parent instanceof ScrolledPageContent) && parent != null) {
      parent = parent.getParent();
    }
    if (parent instanceof ScrolledPageContent) {
      return (ScrolledPageContent) parent;
    }
    return null;
  }

  protected void storeSectionExpansionStates(IDialogSettings section) {
    for (int i = 0; i < fExpandables.size(); i++) {
      ExpandableComposite comp = (ExpandableComposite) fExpandables.get(i);
      section.put(SETTINGS_EXPANDED + String.valueOf(i), comp.isExpanded());
    }
  }

  protected void restoreSectionExpansionStates(IDialogSettings settings) {
    for (int i = 0; i < fExpandables.size(); i++) {
      ExpandableComposite excomposite = (ExpandableComposite) fExpandables.get(i);
      if (settings == null) {
        excomposite.setExpanded(i == 0); // only expand the first node by default
      } else {
        excomposite.setExpanded(settings.getBoolean(SETTINGS_EXPANDED + String.valueOf(i)));
      }
    }
  }

  protected void resetSeverities() {
    IEclipsePreferences defaultContext = new DefaultScope().getNode(getPreferenceNodeQualifier());
    for (int i = 0; i < fCombos.size(); i++) {
      ComboData data = (ComboData) ((Combo) fCombos.get(i)).getData();
      int severity = defaultContext.getInt(data.getKey(), ValidationMessage.WARNING);
      data.setSeverity(severity);
      ((Combo) fCombos.get(i)).select(data.getIndex());
    }
  }

  protected boolean shouldRevalidateOnSettingsChange() {
    Iterator it = fCombos.iterator();

    while (it.hasNext()) {
      ComboData data = (ComboData) ((Combo) it.next()).getData();
      if (data.isChanged())
        return true;
    }
    return false;
  }

  public boolean performOk() {
    if (super.performOk() && shouldRevalidateOnSettingsChange()) {
      MessageBox mb = new MessageBox(this.getShell(), SWT.APPLICATION_MODAL | SWT.YES | SWT.NO
          | SWT.CANCEL | SWT.ICON_INFORMATION | SWT.RIGHT);
      mb.setText(SSEUIMessages.Validation_Title);
      /* Choose which message to use based on if its project or workspace settings */
      String msg = (getProject() == null) ? SSEUIMessages.Validation_Workspace
          : SSEUIMessages.Validation_Project;
      mb.setMessage(msg);
      switch (mb.open()) {
        case SWT.CANCEL:
          return false;
        case SWT.YES:
          storeValues();
          ValidateJob job = new ValidateJob(SSEUIMessages.Validation_jobName);
          job.schedule();
        case SWT.NO:
          storeValues();
        default:
          return true;
      }
    }
    return true;
  }

  /**
   * Performs validation after validation preferences have been modified.
   */
  private class ValidateJob extends Job {

    public ValidateJob(String name) {
      super(name);
    }

    protected IStatus run(IProgressMonitor monitor) {
      IStatus status = Status.OK_STATUS;
      try {
        IProject[] projects = null;
        /* Changed preferences for a single project, only validate it */
        if (getProject() != null)
          projects = new IProject[] {getProject()};
        /* Workspace-wide preferences changed */
        else {
          /* Get all of the projects in the workspace */
          projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
          IEclipsePreferences prefs = null;
          List projectList = new ArrayList();

          /* Filter out projects that use project-specific settings or have been closed */
          for (int i = 0; i < projects.length; i++) {
            prefs = new ProjectScope(projects[i]).getNode(getPreferenceNodeQualifier());
            if (projects[i].isAccessible() && !prefs.getBoolean(getProjectSettingsKey(), false))
              projectList.add(projects[i]);
          }
          projects = (IProject[]) projectList.toArray(new IProject[projectList.size()]);
        }
        fValidation.validate(projects, true, false, monitor);
      } catch (CoreException ce) {
        status = Status.CANCEL_STATUS;
      }

      return status;
    }

  }

}
