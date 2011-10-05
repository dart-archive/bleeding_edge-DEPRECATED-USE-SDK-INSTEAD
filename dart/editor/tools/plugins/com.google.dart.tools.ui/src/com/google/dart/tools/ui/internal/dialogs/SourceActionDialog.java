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
package com.google.dart.tools.ui.internal.dialogs;

import com.google.dart.core.dom.Modifier;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.Method;
import com.google.dart.tools.core.model.SourceRange;
import com.google.dart.tools.core.model.SourceReference;
import com.google.dart.tools.core.model.Type;
import com.google.dart.tools.core.model.TypeMember;
import com.google.dart.tools.ui.CodeTemplatePreferencePage;
import com.google.dart.tools.ui.DartElementLabels;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.IVisibilityChangeListener;
import com.google.dart.tools.ui.JavaPreferencesSettings;
import com.google.dart.tools.ui.Messages;
import com.google.dart.tools.ui.actions.ActionMessages;
import com.google.dart.tools.ui.internal.text.editor.CompilationUnitEditor;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.dialogs.CheckedTreeSelectionDialog;
import org.eclipse.ui.dialogs.PreferencesUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * An advanced version of CheckedTreeSelectionDialog with right-side button layout and extra buttons
 * and composites.
 */
public class SourceActionDialog extends CheckedTreeSelectionDialog {

  protected List<TypeMember> fInsertPositions;
  protected List<String> fLabels;
  protected int fCurrentPositionIndex;

  protected IDialogSettings fSettings;
  protected CompilationUnitEditor fEditor;
  protected ITreeContentProvider fContentProvider;
  protected boolean fGenerateComment;
  protected Type fType;
  protected int fWidth, fHeight;
  protected String fCommentString;
  protected boolean fEnableInsertPosition = true;

  protected int fVisibilityModifier;
  protected boolean fFinal;
  protected boolean fSynchronized;

  protected final String SETTINGS_SECTION_METHODS = "SourceActionDialog.methods"; //$NON-NLS-1$
  protected final String SETTINGS_SECTION_CONSTRUCTORS = "SourceActionDialog.constructors"; //$NON-NLS-1$

  protected final String SETTINGS_INSERTPOSITION = "InsertPosition"; //$NON-NLS-1$
  protected final String SETTINGS_VISIBILITY_MODIFIER = "VisibilityModifier"; //$NON-NLS-1$
  protected final String SETTINGS_FINAL_MODIFIER = "FinalModifier"; //$NON-NLS-1$
  protected final String SETTINGS_SYNCHRONIZED_MODIFIER = "SynchronizedModifier"; //$NON-NLS-1$
  protected final String SETTINGS_COMMENTS = "Comments"; //$NON-NLS-1$
  protected Composite fInsertPositionComposite;

  public SourceActionDialog(Shell parent, ILabelProvider labelProvider,
      ITreeContentProvider contentProvider, CompilationUnitEditor editor, Type type,
      boolean isConstructor) throws DartModelException {
    super(parent, labelProvider, contentProvider);
    fEditor = editor;
    fContentProvider = contentProvider;
    fType = type;
    fCommentString = ActionMessages.SourceActionDialog_createMethodComment;
    setEmptyListMessage(ActionMessages.SourceActionDialog_no_entries);

    fWidth = 60;
    fHeight = 18;

    int insertionDefault = isConstructor ? 0 : 1;
    boolean generateCommentsDefault = JavaPreferencesSettings.getCodeGenerationSettings(type.getDartProject()).createComments;

    IDialogSettings dialogSettings = DartToolsPlugin.getDefault().getDialogSettings();
    String sectionId = isConstructor ? SETTINGS_SECTION_CONSTRUCTORS : SETTINGS_SECTION_METHODS;
    fSettings = dialogSettings.getSection(sectionId);
    if (fSettings == null) {
      fSettings = dialogSettings.addNewSection(sectionId);
    }

    fVisibilityModifier = asInt(fSettings.get(SETTINGS_VISIBILITY_MODIFIER), Modifier.PUBLIC);
    fFinal = asBoolean(fSettings.get(SETTINGS_FINAL_MODIFIER), false);
    fSynchronized = asBoolean(fSettings.get(SETTINGS_SYNCHRONIZED_MODIFIER), false);
    fCurrentPositionIndex = asInt(fSettings.get(SETTINGS_INSERTPOSITION), insertionDefault);
    fGenerateComment = asBoolean(fSettings.get(SETTINGS_COMMENTS), generateCommentsDefault);
    setupInsertPostions();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.dialogs.Dialog#close()
   */
  @Override
  public boolean close() {
    fSettings.put(SETTINGS_VISIBILITY_MODIFIER, StringConverter.asString(fVisibilityModifier));
    fSettings.put(SETTINGS_FINAL_MODIFIER, StringConverter.asString(fFinal));
    fSettings.put(SETTINGS_SYNCHRONIZED_MODIFIER, StringConverter.asString(fSynchronized));

    if (fCurrentPositionIndex == 0 || fCurrentPositionIndex == 1) {
      fSettings.put(SETTINGS_INSERTPOSITION, StringConverter.asString(fCurrentPositionIndex));
    }
    fSettings.put(SETTINGS_COMMENTS, fGenerateComment);
    return super.close();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.dialogs.CheckedTreeSelectionDialog#create()
   */
  @Override
  public void create() {
    super.create();

    // select the first checked element, or if none are checked, the first
// element
    CheckboxTreeViewer treeViewer = getTreeViewer();
    TreeItem[] items = treeViewer.getTree().getItems();
    if (items.length > 0) {
      Object revealedElement = items[0];

      for (int i = 0; i < items.length; i++) {
        if (items[i].getChecked()) {
          revealedElement = items[i].getData();
          break;
        }
      }
      treeViewer.setSelection(new StructuredSelection(revealedElement));
      treeViewer.reveal(revealedElement);
    }
  }

  /*
   * Determine where in the file to enter the newly created methods.
   */
  public DartElement getElementPosition() {
    return fInsertPositions.get(fCurrentPositionIndex);
  }

  public boolean getFinal() {
    return fFinal;
  }

  public boolean getGenerateComment() {
    return fGenerateComment;
  }

  public int getInsertOffset() throws DartModelException {
    DartElement elementPosition = getElementPosition();
    if (elementPosition instanceof SourceReference) {
      return ((SourceReference) elementPosition).getSourceRange().getOffset();
    }
    return -1;
  }

  public boolean getSynchronized() {
    return fSynchronized;
  }

  public int getVisibilityModifier() {
    return fVisibilityModifier;
  }

  public boolean isElementPositionEnabled() {
    return fEnableInsertPosition;
  }

  public boolean isFinal() {
    return fFinal;
  }

  public boolean isSynchronized() {
    return fSynchronized;
  }

  public void setCommentString(String string) {
    fCommentString = string;
  }

  public void setElementPositionEnabled(boolean enabled) {
    fEnableInsertPosition = enabled;
  }

  public void setGenerateComment(boolean comment) {
    fGenerateComment = comment;
  }

  /**
   * Sets the size of the tree in unit of characters.
   * 
   * @param width the width of the tree.
   * @param height the height of the tree.
   */
  @Override
  public void setSize(int width, int height) {
    fWidth = width;
    fHeight = height;
  }

  public void setupInsertPostions() {
    fInsertPositions = new ArrayList<TypeMember>();
    fLabels = new ArrayList<String>();

    DartElement[] members = new DartElement[0];
    Method[] methods = new Method[0];
    try {
      members = fType.getChildren();
      methods = fType.getMethods();
    } catch (DartModelException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    fInsertPositions.add(methods.length > 0 ? methods[0] : null); // first
    fInsertPositions.add(null); // last

    fLabels.add(ActionMessages.SourceActionDialog_first_method);
    fLabels.add(ActionMessages.SourceActionDialog_last_method);

    try {
      if (hasCursorPositionElement(fEditor, members, fInsertPositions)) {
        fLabels.add(ActionMessages.SourceActionDialog_cursor);
        fCurrentPositionIndex = 2;
      } else {
        // code is needed to deal with bogus values already present in the
// dialog store.
        fCurrentPositionIndex = Math.max(fCurrentPositionIndex, 0);
        fCurrentPositionIndex = Math.min(fCurrentPositionIndex, 1);
      }
    } catch (DartModelException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    for (int i = 0; i < methods.length; i++) {
      Method curr = methods[i];
      String methodLabel = DartElementLabels.getElementLabel(curr,
          DartElementLabels.M_PARAMETER_TYPES);
      fLabels.add(Messages.format(ActionMessages.SourceActionDialog_after, methodLabel));
      try {
        fInsertPositions.add(findSibling(curr, members));
      } catch (DartModelException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
    fInsertPositions.add(null);
  }

  protected Composite addVisibilityAndModifiersChoices(Composite buttonComposite) {
    // Add visibility and modifiers buttons:
// http://bugs.eclipse.org/bugs/show_bug.cgi?id=35870
    // Add persistence of options:
// http://bugs.eclipse.org/bugs/show_bug.cgi?id=38400
    IVisibilityChangeListener visibilityChangeListener = new IVisibilityChangeListener() {
      @Override
      public void modifierChanged(int modifier, boolean isChecked) {
        switch (modifier) {
          case Modifier.FINAL: {
            setFinal(isChecked);
            return;
          }
          case Modifier.SYNCHRONIZED: {
            setSynchronized(isChecked);
            return;
          }
          default:
            return;
        }
      }

      @Override
      public void visibilityChanged(int newVisibility) {
        setVisibility(newVisibility);
      }
    };

    int initialVisibility = getVisibilityModifier();
    int[] availableVisibilities = new int[] {
        Modifier.PUBLIC, Modifier.PROTECTED, Modifier.PRIVATE, Modifier.NONE};

    Composite visibilityComposite = createVisibilityControlAndModifiers(buttonComposite,
        visibilityChangeListener, availableVisibilities, initialVisibility);
    return visibilityComposite;
  }

  protected boolean asBoolean(String string, boolean defaultValue) {
    if (string != null) {
      return StringConverter.asBoolean(string, defaultValue);
    }
    return defaultValue;
  }

  protected int asInt(String string, int defaultValue) {
    if (string != null) {
      return StringConverter.asInt(string, defaultValue);
    }
    return defaultValue;
  }

  @Override
  protected void buttonPressed(int buttonId) {
    switch (buttonId) {
      case IDialogConstants.OK_ID: {
        okPressed();
        break;
      }
      case IDialogConstants.CANCEL_ID: {
        cancelPressed();
        break;
      }
    }
  }

  protected Composite createCommentSelection(Composite composite) {
    Composite commentComposite = new Composite(composite, SWT.NONE);
    GridLayout layout = new GridLayout();
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    commentComposite.setLayout(layout);
    commentComposite.setFont(composite.getFont());

    Button commentButton = new Button(commentComposite, SWT.CHECK);
    commentButton.setText(fCommentString);

    commentButton.addSelectionListener(new SelectionListener() {
      @Override
      public void widgetDefaultSelected(SelectionEvent e) {
        widgetSelected(e);
      }

      @Override
      public void widgetSelected(SelectionEvent e) {
        boolean isSelected = (((Button) e.widget).getSelection());
        setGenerateComment(isSelected);
      }
    });
    commentButton.setSelection(getGenerateComment());
    GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
    gd.horizontalSpan = 2;
    commentButton.setLayoutData(gd);

    return commentComposite;
  }

  /*
   * @see Dialog#createDialogArea(Composite)
   */
  @Override
  protected Control createDialogArea(Composite parent) {
    initializeDialogUnits(parent);

    Composite composite = new Composite(parent, SWT.NONE);
    GridLayout layout = new GridLayout();
    GridData gd = null;

    layout.marginHeight = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
    layout.marginWidth = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
    layout.verticalSpacing = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
    layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
    composite.setLayout(layout);

    Label messageLabel = createMessageArea(composite);
    if (messageLabel != null) {
      gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
      gd.horizontalSpan = 2;
      messageLabel.setLayoutData(gd);
    }

    Composite inner = new Composite(composite, SWT.NONE);
    GridLayout innerLayout = new GridLayout();
    innerLayout.numColumns = 2;
    innerLayout.marginHeight = 0;
    innerLayout.marginWidth = 0;
    inner.setLayout(innerLayout);
    inner.setFont(parent.getFont());

    CheckboxTreeViewer treeViewer = createTreeViewer(inner);
    gd = new GridData(GridData.FILL_BOTH);
    gd.widthHint = convertWidthInCharsToPixels(fWidth);
    gd.heightHint = convertHeightInCharsToPixels(fHeight);
    treeViewer.getControl().setLayoutData(gd);

    Composite buttonComposite = createSelectionButtons(inner);
    gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_FILL);
    buttonComposite.setLayoutData(gd);

    gd = new GridData(GridData.FILL_BOTH);
    inner.setLayoutData(gd);

    fInsertPositionComposite = createInsertPositionCombo(composite);
    fInsertPositionComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    Composite commentComposite = createCommentSelection(composite);
    commentComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    Control linkControl = createLinkControl(composite);
    if (linkControl != null) {
      linkControl.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    }

    gd = new GridData(GridData.FILL_BOTH);
    composite.setLayoutData(gd);

    applyDialogFont(composite);

    return composite;
  }

  protected Composite createInsertPositionCombo(Composite composite) {
    Composite selectionComposite = new Composite(composite, SWT.NONE);
    GridLayout layout = new GridLayout();
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    selectionComposite.setLayout(layout);

    addOrderEntryChoices(selectionComposite);

    return selectionComposite;
  }

  protected Control createLinkControl(Composite composite) {
    return null; // No link as default
  }

  /**
   * Returns a composite containing the label created at the top of the dialog. Returns null if
   * there is the message for the label is null.
   */
  @Override
  protected Label createMessageArea(Composite composite) {
    if (getMessage() != null) {
      Label label = new Label(composite, SWT.NONE);
      label.setText(getMessage());
      label.setFont(composite.getFont());
      return label;
    }
    return null;
  }

  @Override
  protected Composite createSelectionButtons(Composite composite) {
    Composite buttonComposite = super.createSelectionButtons(composite);

    GridLayout layout = new GridLayout();
    buttonComposite.setLayout(layout);

    layout.marginHeight = 0;
    layout.marginWidth = 0;
    layout.numColumns = 1;

    return buttonComposite;
  }

  protected Composite createVisibilityControl(Composite parent,
      final IVisibilityChangeListener visibilityChangeListener, int[] availableVisibilities,
      int correctVisibility) {
    List<Integer> allowedVisibilities = convertToIntegerList(availableVisibilities);
    if (allowedVisibilities.size() == 1) {
      return null;
    }

    Group group = new Group(parent, SWT.NONE);
    group.setText(ActionMessages.SourceActionDialog_modifier_group);
    GridData gd = new GridData(GridData.FILL_BOTH);
    group.setLayoutData(gd);
    GridLayout layout = new GridLayout();
    layout.makeColumnsEqualWidth = true;
    layout.numColumns = 4;
    group.setLayout(layout);

    String[] labels = new String[] {
        ActionMessages.SourceActionDialog_modifier_public,
        ActionMessages.SourceActionDialog_modifier_protected,
        ActionMessages.SourceActionDialog_modifier_default,
        ActionMessages.SourceActionDialog_modifier_private,};
    Integer[] data = new Integer[] {
        new Integer(Modifier.PUBLIC), new Integer(Modifier.PROTECTED), new Integer(Modifier.NONE),
        new Integer(Modifier.PRIVATE)};
    Integer initialVisibility = new Integer(correctVisibility);
    for (int i = 0; i < labels.length; i++) {
      Button radio = new Button(group, SWT.RADIO);
      Integer visibilityCode = data[i];
      radio.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
      radio.setText(labels[i]);
      radio.setData(visibilityCode);
      radio.setSelection(visibilityCode.equals(initialVisibility));
      radio.setEnabled(allowedVisibilities.contains(visibilityCode));
      radio.addSelectionListener(new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent event) {
          visibilityChangeListener.visibilityChanged(((Integer) event.widget.getData()).intValue());
        }
      });
    }
    return group;
  }

  protected Composite createVisibilityControlAndModifiers(Composite parent,
      final IVisibilityChangeListener visibilityChangeListener, int[] availableVisibilities,
      int correctVisibility) {
    Composite visibilityComposite = createVisibilityControl(parent, visibilityChangeListener,
        availableVisibilities, correctVisibility);

    Button finalCheckboxButton = new Button(visibilityComposite, SWT.CHECK);
    finalCheckboxButton.setText(ActionMessages.SourceActionDialog_modifier_final);
    GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
    finalCheckboxButton.setLayoutData(gd);
    finalCheckboxButton.setData(new Integer(Modifier.FINAL));
    finalCheckboxButton.setEnabled(true);
    finalCheckboxButton.setSelection(isFinal());
    finalCheckboxButton.addSelectionListener(new SelectionListener() {
      @Override
      public void widgetDefaultSelected(SelectionEvent event) {
        widgetSelected(event);
      }

      @Override
      public void widgetSelected(SelectionEvent event) {
        visibilityChangeListener.modifierChanged(((Integer) event.widget.getData()).intValue(),
            ((Button) event.widget).getSelection());
      }
    });

    Button syncCheckboxButton = new Button(visibilityComposite, SWT.CHECK);
    syncCheckboxButton.setText(ActionMessages.SourceActionDialog_modifier_synchronized);
    gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
    syncCheckboxButton.setLayoutData(gd);
    syncCheckboxButton.setData(new Integer(Modifier.SYNCHRONIZED));
    syncCheckboxButton.setEnabled(true);
    syncCheckboxButton.setSelection(isSynchronized());
    syncCheckboxButton.addSelectionListener(new SelectionListener() {
      @Override
      public void widgetDefaultSelected(SelectionEvent event) {
        widgetSelected(event);
      }

      @Override
      public void widgetSelected(SelectionEvent event) {
        visibilityChangeListener.modifierChanged(((Integer) event.widget.getData()).intValue(),
            ((Button) event.widget).getSelection());
      }
    });
    return visibilityComposite;
  }

  protected ITreeContentProvider getContentProvider() {
    return fContentProvider;
  }

  protected IDialogSettings getDialogSettings() {
    return fSettings;
  }

  protected Type getType() {
    return fType;
  }

  protected boolean hasCursorPositionElement(CompilationUnitEditor editor, DartElement[] members,
      List<TypeMember> insertPositions) throws DartModelException {
    if (editor == null) {
      return false;
    }
    int offset = ((ITextSelection) editor.getSelectionProvider().getSelection()).getOffset();

    for (int i = 0; i < members.length; i++) {
      TypeMember curr = (TypeMember) members[i];
      SourceRange range = curr.getSourceRange();
      if (offset < range.getOffset()) {
        insertPositions.add(curr);
        return true;
      } else if (offset < range.getOffset() + range.getLength()) {
        return false; // in the middle of a member
      }
    }
    insertPositions.add(null);
    return true;
  }

  protected void openCodeTempatePage(String id) {
    HashMap<Object, String> arg = new HashMap<Object, String>();
    arg.put(CodeTemplatePreferencePage.DATA_SELECT_TEMPLATE, id);
    PreferencesUtil.createPropertyDialogOn(getShell(), fType.getDartProject().getProject(),
        CodeTemplatePreferencePage.PROP_ID, null, arg).open();
  }

  protected void setVisibility(int visibility) {
    fVisibilityModifier = visibility;
  }

  private Composite addOrderEntryChoices(Composite buttonComposite) {
    Label enterLabel = new Label(buttonComposite, SWT.NONE);
    enterLabel.setText(ActionMessages.SourceActionDialog_enterAt_label);
    if (!fEnableInsertPosition) {
      enterLabel.setEnabled(false);
    }
    GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
    enterLabel.setLayoutData(gd);

    final Combo enterCombo = new Combo(buttonComposite, SWT.READ_ONLY);
    if (!fEnableInsertPosition) {
      enterCombo.setEnabled(false);
    }
    fillWithPossibleInsertPositions(enterCombo);

    gd = new GridData(GridData.FILL_BOTH);
    gd.widthHint = convertWidthInCharsToPixels(fWidth);
    enterCombo.setLayoutData(gd);
    enterCombo.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        int index = enterCombo.getSelectionIndex();
        // Add persistence only if first or last method:
// http://bugs.eclipse.org/bugs/show_bug.cgi?id=38400
        setInsertPosition(index);
      }
    });

    return buttonComposite;
  }

  private List<Integer> convertToIntegerList(int[] array) {
    List<Integer> result = new ArrayList<Integer>(array.length);
    for (int i = 0; i < array.length; i++) {
      result.add(new Integer(array[i]));
    }
    return result;
  }

  private void fillWithPossibleInsertPositions(Combo combo) {
    combo.setItems(fLabels.toArray(new String[fLabels.size()]));
    combo.select(fCurrentPositionIndex);
  }

  private TypeMember findSibling(Method curr, DartElement[] members) throws DartModelException {
    TypeMember res = null;
    int methodStart = curr.getSourceRange().getOffset();
    for (int i = members.length - 1; i >= 0; i--) {
      TypeMember member = (TypeMember) members[i];
      if (methodStart >= member.getSourceRange().getOffset()) {
        return res;
      }
      res = member;
    }
    return null;
  }

  private void setFinal(boolean value) {
    fFinal = value;
  }

  /***
   * Set insert position valid input is 0 for the first position, 1 for the last position, > 1 for
   * all else.
   */
  private void setInsertPosition(int insert) {
    fCurrentPositionIndex = insert;
  }

  private void setSynchronized(boolean value) {
    fSynchronized = value;
  }
}
