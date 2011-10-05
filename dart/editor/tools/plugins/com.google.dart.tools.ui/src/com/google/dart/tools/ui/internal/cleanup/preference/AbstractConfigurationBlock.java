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

import com.google.dart.tools.ui.Messages;
import com.google.dart.tools.ui.internal.dialogs.StatusInfo;
import com.google.dart.tools.ui.internal.dialogs.StatusUtil;
import com.google.dart.tools.ui.internal.preferences.OverlayPreferenceStore;
import com.google.dart.tools.ui.internal.preferences.PreferencesMessages;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Configures Java Editor typing preferences.
 * 
 * @since 3.1
 */
abstract class AbstractConfigurationBlock implements IPreferenceConfigurationBlock {

  /**
   * Use as follows:
   * 
   * <pre>
	 * SectionManager manager= new SectionManager();
	 * Composite composite= manager.createSectionComposite(parent);
	 *
	 * Composite xSection= manager.createSection("section X"));
	 * xSection.setLayout(new FillLayout());
	 * new Button(xSection, SWT.PUSH); // add controls to section..
	 *
	 * [...]
	 *
	 * return composite; // return main composite
	 * </pre>
   */
  protected final class SectionManager {
    /** The preference setting for keeping no section open. */
    private static final String __NONE = "__none"; //$NON-NLS-1$
    private Set<ExpandableComposite> fSections = new HashSet<ExpandableComposite>();
    private boolean fIsBeingManaged = false;
    private ExpansionAdapter fListener = new ExpansionAdapter() {
      @Override
      public void expansionStateChanged(ExpansionEvent e) {
        ExpandableComposite source = (ExpandableComposite) e.getSource();
        updateSectionStyle(source);
        if (fIsBeingManaged) {
          return;
        }
        if (e.getState()) {
          try {
            fIsBeingManaged = true;
            for (Iterator<ExpandableComposite> iter = fSections.iterator(); iter.hasNext();) {
              ExpandableComposite composite = iter.next();
              if (composite != source) {
                composite.setExpanded(false);
              }
            }
          } finally {
            fIsBeingManaged = false;
          }
          if (fLastOpenKey != null && fDialogSettingsStore != null) {
            fDialogSettingsStore.setValue(fLastOpenKey, source.getText());
          }
        } else {
          if (!fIsBeingManaged && fLastOpenKey != null && fDialogSettingsStore != null) {
            fDialogSettingsStore.setValue(fLastOpenKey, __NONE);
          }
        }
        ExpandableComposite exComp = getParentExpandableComposite(source);
        if (exComp != null) {
          exComp.layout(true, true);
        }
        ScrolledPageContent parentScrolledComposite = getParentScrolledComposite(source);
        if (parentScrolledComposite != null) {
          parentScrolledComposite.reflow(true);
        }
      }
    };
    private Composite fBody;
    private final String fLastOpenKey;
    private final IPreferenceStore fDialogSettingsStore;
    private ExpandableComposite fFirstChild = null;

    /**
     * Creates a new section manager.
     */
    public SectionManager() {
      this(null, null);
    }

    /**
     * Creates a new section manager.
     */
    public SectionManager(IPreferenceStore dialogSettingsStore, String lastOpenKey) {
      fDialogSettingsStore = dialogSettingsStore;
      fLastOpenKey = lastOpenKey;
    }

    /**
     * Creates an expandable section within the parent created previously by calling
     * <code>createSectionComposite</code>. Controls can be added directly to the returned
     * composite, which has no layout initially.
     * 
     * @param label the display name of the section
     * @return a composite within the expandable section
     */
    public Composite createSection(String label) {
      Assert.isNotNull(fBody);
      final ExpandableComposite excomposite = new ExpandableComposite(fBody, SWT.NONE,
          ExpandableComposite.TWISTIE | ExpandableComposite.CLIENT_INDENT
              | ExpandableComposite.COMPACT);
      if (fFirstChild == null) {
        fFirstChild = excomposite;
      }
      excomposite.setText(label);
      String last = null;
      if (fLastOpenKey != null && fDialogSettingsStore != null) {
        last = fDialogSettingsStore.getString(fLastOpenKey);
      }

      if (fFirstChild == excomposite && !__NONE.equals(last) || label.equals(last)) {
        excomposite.setExpanded(true);
        if (fFirstChild != excomposite) {
          fFirstChild.setExpanded(false);
        }
      } else {
        excomposite.setExpanded(false);
      }
      excomposite.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true, false));

      updateSectionStyle(excomposite);
      manage(excomposite);

      Composite contents = new Composite(excomposite, SWT.NONE);
      excomposite.setClient(contents);

      return contents;
    }

    /**
     * Creates a new composite that can contain a set of expandable sections. A
     * <code>ScrolledPageComposite</code> is created and a new composite within that, to ensure that
     * expanding the sections will always have enough space, unless there already is a
     * <code>ScrolledComposite</code> along the parent chain of <code>parent</code>, in which case a
     * normal <code>Composite</code> is created.
     * <p>
     * The receiver keeps a reference to the inner body composite, so that new sections can be added
     * via <code>createSection</code>.
     * </p>
     * 
     * @param parent the parent composite
     * @return the newly created composite
     */
    public Composite createSectionComposite(Composite parent) {
      Assert.isTrue(fBody == null);
      boolean isNested = isNestedInScrolledComposite(parent);
      Composite composite;
      if (isNested) {
        composite = new Composite(parent, SWT.NONE);
        fBody = composite;
      } else {
        composite = new ScrolledPageContent(parent);
        fBody = ((ScrolledPageContent) composite).getBody();
      }

      fBody.setLayout(new GridLayout());

      return composite;
    }

    private void manage(ExpandableComposite section) {
      if (section == null) {
        throw new NullPointerException();
      }
      if (fSections.add(section)) {
        section.addExpansionListener(fListener);
      }
      makeScrollableCompositeAware(section);
    }
  }

  protected static final int INDENT = 20;

  protected static void indent(Control control) {
    ((GridData) control.getLayoutData()).horizontalIndent += INDENT;
  }

  private OverlayPreferenceStore fStore;
  private Map<Button, String> fCheckBoxes = new HashMap<Button, String>();

  private SelectionListener fCheckBoxListener = new SelectionListener() {
    @Override
    public void widgetDefaultSelected(SelectionEvent e) {
    }

    @Override
    public void widgetSelected(SelectionEvent e) {
      Button button = (Button) e.widget;
      fStore.setValue(fCheckBoxes.get(button), button.getSelection());
    }
  };
  private Map<Text, String> fTextFields = new HashMap<Text, String>();

  private ModifyListener fTextFieldListener = new ModifyListener() {
    @Override
    public void modifyText(ModifyEvent e) {
      Text text = (Text) e.widget;
      fStore.setValue(fTextFields.get(text), text.getText());
    }
  };
  private ArrayList<Text> fNumberFields = new ArrayList<Text>();

  private ModifyListener fNumberFieldListener = new ModifyListener() {
    @Override
    public void modifyText(ModifyEvent e) {
      numberFieldChanged((Text) e.widget);
    }
  };

  /**
   * List of master/slave listeners when there's a dependency.
   * 
   * @see #createDependency(Button, Control)
   * @since 3.0
   */
  private ArrayList<SelectionListener> fMasterSlaveListeners = new ArrayList<SelectionListener>();
  private StatusInfo fStatus;

  private final PreferencePage fMainPage;

  public AbstractConfigurationBlock(OverlayPreferenceStore store) {
    Assert.isNotNull(store);
    fStore = store;
    fMainPage = null;
  }

  public AbstractConfigurationBlock(OverlayPreferenceStore store, PreferencePage mainPreferencePage) {
    Assert.isNotNull(store);
    Assert.isNotNull(mainPreferencePage);
    fStore = store;
    fMainPage = mainPreferencePage;
  }

  /*
   * @see org.eclipse.jdt.internal.ui.preferences.IPreferenceConfigurationBlock#dispose()
   * 
   * @since 3.0
   */
  @Override
  public void dispose() {
  }

  @Override
  public void initialize() {
    initializeFields();
  }

  @Override
  public void performDefaults() {
    initializeFields();
  }

  @Override
  public void performOk() {
  }

  protected Button addCheckBox(Composite parent, String label, String key, int indentation) {
    Button checkBox = new Button(parent, SWT.CHECK);
    checkBox.setText(label);

    GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
    gd.horizontalIndent = indentation;
    gd.horizontalSpan = 2;
    checkBox.setLayoutData(gd);
    checkBox.addSelectionListener(fCheckBoxListener);
    makeScrollableCompositeAware(checkBox);

    fCheckBoxes.put(checkBox, key);

    return checkBox;
  }

  /**
   * Returns an array of size 2: - first element is of type <code>Label</code> - second element is
   * of type <code>Text</code> Use <code>getLabelControl</code> and <code>getTextControl</code> to
   * get the 2 controls.
   * 
   * @param composite the parent composite
   * @param label the text field's label
   * @param key the preference key
   * @param textLimit the text limit
   * @param indentation the field's indentation
   * @param isNumber <code>true</code> iff this text field is used to e4dit a number
   * @return the controls added
   */
  protected Control[] addLabelledTextField(Composite composite, String label, String key,
      int textLimit, int indentation, boolean isNumber) {

    PixelConverter pixelConverter = new PixelConverter(composite);

    Label labelControl = new Label(composite, SWT.NONE);
    labelControl.setText(label);
    GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
    gd.horizontalIndent = indentation;
    labelControl.setLayoutData(gd);

    Text textControl = new Text(composite, SWT.BORDER | SWT.SINGLE);
    gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
    gd.widthHint = pixelConverter.convertWidthInCharsToPixels(textLimit + 1);
    textControl.setLayoutData(gd);
    textControl.setTextLimit(textLimit);
    fTextFields.put(textControl, key);
    if (isNumber) {
      fNumberFields.add(textControl);
      textControl.addModifyListener(fNumberFieldListener);
    } else {
      textControl.addModifyListener(fTextFieldListener);
    }

    return new Control[] {labelControl, textControl};
  }

  protected void createDependency(final Button master, final Control slave) {
    createDependency(master, new Control[] {slave});
  }

  protected void createDependency(final Button master, final Control[] slaves) {
    Assert.isTrue(slaves.length > 0);
    indent(slaves[0]);
    SelectionListener listener = new SelectionListener() {
      @Override
      public void widgetDefaultSelected(SelectionEvent e) {
      }

      @Override
      public void widgetSelected(SelectionEvent e) {
        boolean state = master.getSelection();
        for (int i = 0; i < slaves.length; i++) {
          slaves[i].setEnabled(state);
        }
      }
    };
    master.addSelectionListener(listener);
    fMasterSlaveListeners.add(listener);
  }

  protected Composite createSubsection(Composite parent, SectionManager manager, String label) {
    if (manager != null) {
      return manager.createSection(label);
    } else {
      Group group = new Group(parent, SWT.SHADOW_NONE);
      group.setText(label);
      GridData data = new GridData(SWT.FILL, SWT.CENTER, true, false);
      group.setLayoutData(data);
      return group;
    }
  }

  protected final ScrolledPageContent getParentScrolledComposite(Control control) {
    Control parent = control.getParent();
    while (!(parent instanceof ScrolledPageContent) && parent != null) {
      parent = parent.getParent();
    }
    if (parent instanceof ScrolledPageContent) {
      return (ScrolledPageContent) parent;
    }
    return null;
  }

  protected final OverlayPreferenceStore getPreferenceStore() {
    return fStore;
  }

  protected void updateSectionStyle(ExpandableComposite excomposite) {
    excomposite.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT));
  }

  protected void updateStatus(IStatus status) {
    if (fMainPage == null) {
      return;
    }
    fMainPage.setValid(status.isOK());
    StatusUtil.applyToStatusLine(fMainPage, status);
  }

  IStatus getStatus() {
    if (fStatus == null) {
      fStatus = new StatusInfo();
    }
    return fStatus;
  }

  private final ExpandableComposite getParentExpandableComposite(Control control) {
    Control parent = control.getParent();
    while (!(parent instanceof ExpandableComposite) && parent != null) {
      parent = parent.getParent();
    }
    if (parent instanceof ExpandableComposite) {
      return (ExpandableComposite) parent;
    }
    return null;
  }

  private void initializeFields() {

    Iterator<Button> iter = fCheckBoxes.keySet().iterator();
    while (iter.hasNext()) {
      Button b = iter.next();
      String key = fCheckBoxes.get(b);
      b.setSelection(fStore.getBoolean(key));
    }

    Iterator<Text> iter2 = fTextFields.keySet().iterator();
    while (iter2.hasNext()) {
      Text t = iter2.next();
      String key = fTextFields.get(t);
      t.setText(fStore.getString(key));
    }

    // Update slaves
    Iterator<SelectionListener> iter3 = fMasterSlaveListeners.iterator();
    while (iter3.hasNext()) {
      SelectionListener listener = iter3.next();
      listener.widgetSelected(null);
    }

    updateStatus(new StatusInfo());
  }

  private boolean isNestedInScrolledComposite(Composite parent) {
    return getParentScrolledComposite(parent) != null;
  }

  private void makeScrollableCompositeAware(Control control) {
    ScrolledPageContent parentScrolledComposite = getParentScrolledComposite(control);
    if (parentScrolledComposite != null) {
      parentScrolledComposite.adaptChild(control);
    }
  }

  private void numberFieldChanged(Text textControl) {
    String number = textControl.getText();
    IStatus status = validatePositiveNumber(number);
    if (!status.matches(IStatus.ERROR)) {
      fStore.setValue(fTextFields.get(textControl), number);
    }
    updateStatus(status);
  }

  private IStatus validatePositiveNumber(String number) {
    StatusInfo status = new StatusInfo();
    if (number.length() == 0) {
      status.setError(PreferencesMessages.DartEditorPreferencePage_empty_input);
    } else {
      try {
        int value = Integer.parseInt(number);
        if (value < 0) {
          status.setError(Messages.format(
              PreferencesMessages.DartEditorPreferencePage_invalid_input, number));
        }
      } catch (NumberFormatException e) {
        status.setError(Messages.format(PreferencesMessages.DartEditorPreferencePage_invalid_input,
            number));
      }
    }
    return status;
  }
}
