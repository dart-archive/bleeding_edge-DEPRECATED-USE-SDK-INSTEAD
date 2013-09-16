/*******************************************************************************
 * Copyright (c) 2001, 2006 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.preferences.ui;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.wst.sse.core.internal.SSECorePlugin;
import org.eclipse.wst.sse.ui.internal.SSEUIMessages;
import org.eclipse.wst.sse.ui.internal.SSEUIPlugin;

/**
 * (pa) why is this class abstract if there are no abstract methods?
 */
public abstract class AbstractPreferencePage extends PreferencePage implements ModifyListener,
    SelectionListener, IWorkbenchPreferencePage {

  protected final static int WIDTH_VALIDATION_LOWER_LIMIT = 0; //$NON-NLS-1$
  protected final static int WIDTH_VALIDATION_UPPER_LIMIT = 999; //$NON-NLS-1$

  protected Button createCheckBox(Composite group, String label) {
    Button button = new Button(group, SWT.CHECK | SWT.LEFT);
    button.setText(label);

    //GridData
    GridData data = new GridData(GridData.FILL);
    data.verticalAlignment = GridData.CENTER;
    data.horizontalAlignment = GridData.FILL;
    button.setLayoutData(data);

    return button;
  }

  protected void initCheckbox(Button box, String key) {
    if (box != null && key != null) {
      box.setSelection(getPreferenceStore().getBoolean(key));
    }
  }

  protected void defaultCheckbox(Button box, String key) {
    if (box != null && key != null) {
      box.setSelection(getPreferenceStore().getDefaultBoolean(key));
    }
  }

  protected Composite createComposite(Composite parent, int numColumns) {
    Composite composite = new Composite(parent, SWT.NULL);

    //GridLayout
    GridLayout layout = new GridLayout();
    layout.numColumns = numColumns;
    composite.setLayout(layout);

    //GridData
    GridData data = new GridData(GridData.FILL);
    data.horizontalIndent = 0;
    data.verticalAlignment = GridData.FILL;
    data.horizontalAlignment = GridData.FILL;
    composite.setLayoutData(data);

    return composite;
  }

  protected Control createContents(Composite parent) {
    return createScrolledComposite(parent);
  }

  protected Combo createDropDownBox(Composite parent) {
    Combo comboBox = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY);

    //GridData
    GridData data = new GridData();
    data.verticalAlignment = GridData.CENTER;
    data.horizontalAlignment = GridData.FILL;
    data.grabExcessHorizontalSpace = true;
    comboBox.setLayoutData(data);

    return comboBox;
  }

  protected Group createGroup(Composite parent, int numColumns) {
    Group group = new Group(parent, SWT.NULL);

    //GridLayout
    GridLayout layout = new GridLayout();
    layout.numColumns = numColumns;
    group.setLayout(layout);

    //GridData
    GridData data = new GridData(GridData.FILL);
    data.horizontalIndent = 0;
    data.verticalAlignment = GridData.FILL;
    data.horizontalAlignment = GridData.FILL;
    data.grabExcessHorizontalSpace = true;
    group.setLayoutData(data);

    return group;
  }

  protected Label createLabel(Composite parent, String text) {
    Label label = new Label(parent, SWT.LEFT);
    label.setText(text);

    //GridData
    GridData data = new GridData(GridData.FILL);
    data.verticalAlignment = GridData.CENTER;
    data.horizontalAlignment = GridData.FILL;
    label.setLayoutData(data);

    return label;
  }

  protected Button createRadioButton(Composite group, String label) {
    Button button = new Button(group, SWT.RADIO);
    button.setText(label);

    //GridData
    GridData data = new GridData(GridData.FILL);
    data.verticalAlignment = GridData.CENTER;
    data.horizontalAlignment = GridData.FILL;
    button.setLayoutData(data);

    return button;
  }

  protected Composite createScrolledComposite(Composite parent) {
    // create scrollbars for this parent when needed
    final ScrolledComposite sc1 = new ScrolledComposite(parent, SWT.H_SCROLL | SWT.V_SCROLL);
    sc1.setLayoutData(new GridData(GridData.FILL_BOTH));
    Composite composite = createComposite(sc1, 1);
    sc1.setContent(composite);

    // not calling setSize for composite will result in a blank composite,
    // so calling it here initially
    // setSize actually needs to be called after all controls are created,
    // so scrolledComposite
    // has correct minSize
    setSize(composite);
    return composite;
  }

  protected Label createSeparator(Composite parent, int columnSpan) {
    // Create a spacer line
    Label separator = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);

    //GridData
    GridData data = new GridData(GridData.FILL);
    data.verticalAlignment = GridData.CENTER;
    data.horizontalAlignment = GridData.FILL;
    data.horizontalSpan = columnSpan;

    separator.setLayoutData(data);
    return separator;
  }

  protected Text createTextField(Composite parent) {
    Text text = new Text(parent, SWT.SINGLE | SWT.BORDER);

    //GridData
    GridData data = new GridData();
    data.verticalAlignment = GridData.CENTER;
    data.horizontalAlignment = GridData.FILL;
    data.grabExcessHorizontalSpace = true;
    text.setLayoutData(data);

    return text;
  }

  protected void enableValues() {
  }

  protected Preferences getModelPreferences() {
    return SSECorePlugin.getDefault().getPluginPreferences();
  }

  public void init(IWorkbench workbench) {
  }

  protected void initializeValues() {
  }

  protected boolean loadPreferences() {
    BusyIndicator.showWhile(getControl().getDisplay(), new Runnable() {
      public void run() {
        initializeValues();
        validateValues();
        enableValues();
      }
    });
    return true;
  }

  public void modifyText(ModifyEvent e) {
    // If we are called too early, i.e. before the controls are created
    // then return
    // to avoid null pointer exceptions
    if (e.widget != null && e.widget.isDisposed())
      return;

    validateValues();
    enableValues();
  }

  protected void performDefaults() {
    super.performDefaults();
  }

  public boolean performOk() {
    savePreferences();
    return true;
  }

  protected boolean savePreferences() {
    BusyIndicator.showWhile(getControl().getDisplay(), new Runnable() {
      public void run() {
        storeValues();
      }
    });
    return true;
  }

  protected void setInvalidInputMessage(String widthText) {
    String msg = NLS.bind(SSEUIMessages._4concat, (new Object[] {widthText}));
    setErrorMessage(msg);
  }

  protected void setSize(Composite composite) {
    if (composite != null) {
      // Note: The font is set here in anticipation that the class inheriting
      //       this base class may add widgets to the dialog.   setSize
      //       is assumed to be called just before we go live.
      applyDialogFont(composite);
      Point minSize = composite.computeSize(SWT.DEFAULT, SWT.DEFAULT);
      composite.setSize(minSize);
      // set scrollbar composite's min size so page is expandable but
      // has scrollbars when needed
      if (composite.getParent() instanceof ScrolledComposite) {
        ScrolledComposite sc1 = (ScrolledComposite) composite.getParent();
        sc1.setMinSize(minSize);
        sc1.setExpandHorizontal(true);
        sc1.setExpandVertical(true);
      }
    }
  }

  protected void storeValues() {
    SSEUIPlugin.getDefault().savePluginPreferences();
  }

  protected void validateValues() {
  }

  public void widgetDefaultSelected(SelectionEvent e) {
    widgetSelected(e);
  }

  public void widgetSelected(SelectionEvent e) {
    // If we are called too early, i.e. before the controls are created
    // then return
    // to avoid null pointer exceptions
    if (e.widget != null && e.widget.isDisposed())
      return;

    validateValues();
    enableValues();
  }
}
