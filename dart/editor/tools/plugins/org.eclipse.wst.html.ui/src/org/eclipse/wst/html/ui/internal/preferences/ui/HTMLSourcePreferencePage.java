/*******************************************************************************
 * Copyright (c) 2004, 2010 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.html.ui.internal.preferences.ui;

import com.ibm.icu.text.Collator;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.dialogs.PreferenceLinkArea;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;
import org.eclipse.wst.html.core.internal.HTMLCorePlugin;
import org.eclipse.wst.html.core.internal.contentmodel.HTMLCMDocumentFactory;
import org.eclipse.wst.html.core.internal.format.HTMLFormattingUtil;
import org.eclipse.wst.html.core.internal.preferences.HTMLCorePreferenceNames;
import org.eclipse.wst.html.ui.internal.HTMLUIMessages;
import org.eclipse.wst.html.ui.internal.HTMLUIPlugin;
import org.eclipse.wst.html.ui.internal.editor.IHelpContextIds;
import org.eclipse.wst.sse.ui.internal.preferences.ui.AbstractPreferencePage;
import org.eclipse.wst.xml.core.internal.contentmodel.CMNamedNodeMap;
import org.eclipse.wst.xml.core.internal.contentmodel.CMNode;
import org.eclipse.wst.xml.core.internal.provisional.contentmodel.CMDocType;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class HTMLSourcePreferencePage extends AbstractPreferencePage {
  private Button fTagNameUpper = null;
  private Button fTagNameLower = null;
  private Button fAttrNameUpper = null;
  private Button fAttrNameLower = null;

  private final int MIN_INDENTATION_SIZE = 0;
  private final int MAX_INDENTATION_SIZE = 16;

  private Button fClearAllBlankLines;

  // Formatting
  private Text fLineWidthText;
  private Button fSplitMultiAttrs;
  private Button fAlignEndBracket;
  private Button fIndentUsingTabs;
  private Button fIndentUsingSpaces;
  private Spinner fIndentationSize;

  private Button fAddButton;
  private Button fRemoveButton;

  private TableViewer fViewer;

  private ContentProvider fContentProvider;

  private Composite createContentsForPreferredCaseGroup(Composite parent, int columnSpan) {
    Group caseGroup = createGroup(parent, columnSpan);
    caseGroup.setText(HTMLUIMessages.Preferred_markup_case_UI_);

    // d257064 need to associate group w/ radio buttons so radio buttons
    // header can be read
    Group tagNameGroup = createGroup(caseGroup, 1);
    tagNameGroup.setText(HTMLUIMessages.Tag_names__UI_);
    fTagNameUpper = createRadioButton(tagNameGroup, HTMLUIMessages.Tag_names_Upper_case_UI_);
    fTagNameLower = createRadioButton(tagNameGroup, HTMLUIMessages.Tag_names_Lower_case_UI_);

    // d257064 need to associate group w/ radio buttons so radio buttons
    // header can be read
    Group attrNameGroup = createGroup(caseGroup, 1);
    attrNameGroup.setText(HTMLUIMessages.Attribute_names__UI_);
    fAttrNameUpper = createRadioButton(attrNameGroup, HTMLUIMessages.Attribute_names_Upper_case_UI_);
    fAttrNameLower = createRadioButton(attrNameGroup, HTMLUIMessages.Attribute_names_Lower_case_UI_);

    return parent;

  }

  private void createContentsForFormattingGroup(Composite parent) {
    Group formattingGroup = createGroup(parent, 2);
    formattingGroup.setText(HTMLUIMessages.Formatting_UI_);

    createLabel(formattingGroup, HTMLUIMessages.Line_width__UI_);
    fLineWidthText = new Text(formattingGroup, SWT.SINGLE | SWT.BORDER);
    GridData gData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.BEGINNING);
    gData.widthHint = 25;
    fLineWidthText.setLayoutData(gData);
    fLineWidthText.addModifyListener(this);

    fSplitMultiAttrs = createCheckBox(formattingGroup, HTMLUIMessages.Split_multiple_attributes);
    ((GridData) fSplitMultiAttrs.getLayoutData()).horizontalSpan = 2;
    fAlignEndBracket = createCheckBox(formattingGroup, HTMLUIMessages.Align_final_bracket);
    ((GridData) fAlignEndBracket.getLayoutData()).horizontalSpan = 2;
    fClearAllBlankLines = createCheckBox(formattingGroup, HTMLUIMessages.Clear_all_blank_lines_UI_);
    ((GridData) fClearAllBlankLines.getLayoutData()).horizontalSpan = 2;

    // [269224] - Place the indent controls in their own composite for proper tab ordering
    Composite indentComposite = createComposite(formattingGroup, 1);
    ((GridData) indentComposite.getLayoutData()).horizontalSpan = 2;
    ((GridLayout) indentComposite.getLayout()).marginWidth = 0;
    ((GridLayout) indentComposite.getLayout()).marginHeight = 0;

    fIndentUsingTabs = createRadioButton(indentComposite, HTMLUIMessages.Indent_using_tabs);
    ((GridData) fIndentUsingTabs.getLayoutData()).horizontalSpan = 1;

    fIndentUsingSpaces = createRadioButton(indentComposite, HTMLUIMessages.Indent_using_spaces);
    ((GridData) fIndentUsingSpaces.getLayoutData()).horizontalSpan = 1;

    createLabel(formattingGroup, HTMLUIMessages.Indentation_size);
    fIndentationSize = new Spinner(formattingGroup, SWT.READ_ONLY | SWT.BORDER);
    GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
    fIndentationSize.setLayoutData(gd);
    fIndentationSize.setToolTipText(HTMLUIMessages.Indentation_size_tip);
    fIndentationSize.setMinimum(MIN_INDENTATION_SIZE);
    fIndentationSize.setMaximum(MAX_INDENTATION_SIZE);
    fIndentationSize.setIncrement(1);
    fIndentationSize.setPageIncrement(4);
    fIndentationSize.addModifyListener(this);

    GridData data;

    Composite inlineGroup = new Composite(formattingGroup, SWT.NONE);
    GridLayout layout = new GridLayout();
    layout.numColumns = 2;
    layout.marginWidth = 0;
    layout.marginHeight = 0;
    inlineGroup.setLayout(layout);

    data = new GridData(GridData.FILL_BOTH);
    data.horizontalSpan = 2;
    inlineGroup.setLayoutData(data);

    Label label = createLabel(inlineGroup, HTMLUIMessages.Inline_elements_table_label);
    data = new GridData(GridData.FILL_HORIZONTAL);
    data.horizontalSpan = 2;
    label.setLayoutData(data);

    final TableViewer viewer = new TableViewer(inlineGroup, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL
        | SWT.BORDER | SWT.FULL_SELECTION);
    data = new GridData(GridData.FILL_HORIZONTAL);
    data.horizontalSpan = 1;
    data.verticalAlignment = SWT.BEGINNING;
    data.heightHint = convertHeightInCharsToPixels(10);
    viewer.getTable().setLayoutData(data);

    Composite buttonContainer = new Composite(inlineGroup, SWT.NONE);
    data = new GridData(GridData.FILL_VERTICAL);
    buttonContainer.setLayoutData(data);
    layout = new GridLayout();
    layout.numColumns = 1;
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    buttonContainer.setLayout(layout);

    fAddButton = new Button(buttonContainer, SWT.PUSH);
    fAddButton.setText(HTMLUIMessages.Add_inline);
    fAddButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        HTMLElementDialog dialog = new HTMLElementDialog(getShell());
        dialog.setMessage(HTMLUIMessages.Elements_Dialog_message);
        dialog.setTitle(HTMLUIMessages.Elements_Dialog_title);
        dialog.setMultipleSelection(true);
        dialog.setAllowDuplicates(false);
        dialog.open();
        Object[] result = dialog.getResult();
        if (result != null) {
          for (int i = 0; i < result.length; i++) {
            fContentProvider.addElement(result[i].toString());
          }
          fViewer.refresh();
        }
      }
    });
    data = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
    int widthHint = convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
    data.widthHint = Math.max(widthHint, fAddButton.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);
    data.horizontalSpan = 1;
    fAddButton.setLayoutData(data);

    fRemoveButton = new Button(buttonContainer, SWT.PUSH);
    fRemoveButton.setText(HTMLUIMessages.Remove_inline);
    fRemoveButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        ISelection selection = viewer.getSelection();
        if (selection != null && !selection.isEmpty() && selection instanceof StructuredSelection) {
          Object[] remove = ((StructuredSelection) selection).toArray();
          for (int i = 0; i < remove.length; i++) {
            fContentProvider.removeElement(remove[i].toString());
          }
          if (remove.length > 0) {
            fViewer.refresh();
          }
        }
      }
    });
    data = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
    data.horizontalSpan = 1;
    fRemoveButton.setLayoutData(data);

    fViewer = viewer;
    fContentProvider = new ContentProvider();
    viewer.setContentProvider(fContentProvider);
    viewer.setInput(this);
    viewer.setComparator(new ViewerComparator(Collator.getInstance()));
  }

  protected void performDefaults() {
    fTagNameUpper.setSelection(getModelPreferences().getDefaultInt(
        HTMLCorePreferenceNames.TAG_NAME_CASE) == HTMLCorePreferenceNames.UPPER);
    fTagNameLower.setSelection(getModelPreferences().getDefaultInt(
        HTMLCorePreferenceNames.TAG_NAME_CASE) == HTMLCorePreferenceNames.LOWER);
    fAttrNameUpper.setSelection(getModelPreferences().getDefaultInt(
        HTMLCorePreferenceNames.ATTR_NAME_CASE) == HTMLCorePreferenceNames.UPPER);
    fAttrNameLower.setSelection(getModelPreferences().getDefaultInt(
        HTMLCorePreferenceNames.ATTR_NAME_CASE) == HTMLCorePreferenceNames.LOWER);

    performDefaultsForFormattingGroup();

    validateValues();
    enableValues();

    super.performDefaults();
  }

  private void performDefaultsForFormattingGroup() {
    // Formatting
    fLineWidthText.setText(getModelPreferences().getDefaultString(
        HTMLCorePreferenceNames.LINE_WIDTH));
    fSplitMultiAttrs.setSelection(getModelPreferences().getDefaultBoolean(
        HTMLCorePreferenceNames.SPLIT_MULTI_ATTRS));
    fAlignEndBracket.setSelection(getModelPreferences().getDefaultBoolean(
        HTMLCorePreferenceNames.ALIGN_END_BRACKET));
    fClearAllBlankLines.setSelection(getModelPreferences().getDefaultBoolean(
        HTMLCorePreferenceNames.CLEAR_ALL_BLANK_LINES));

    if (HTMLCorePreferenceNames.TAB.equals(getModelPreferences().getDefaultString(
        HTMLCorePreferenceNames.INDENTATION_CHAR))) {
      fIndentUsingTabs.setSelection(true);
      fIndentUsingSpaces.setSelection(false);
    } else {
      fIndentUsingSpaces.setSelection(true);
      fIndentUsingTabs.setSelection(false);
    }
    fIndentationSize.setSelection(getModelPreferences().getDefaultInt(
        HTMLCorePreferenceNames.INDENTATION_SIZE));

    // Inline elements
    fContentProvider.restoreDefaults();
    fViewer.refresh();
  }

  protected void initializeValues() {
    fTagNameUpper.setSelection(getModelPreferences().getInt(HTMLCorePreferenceNames.TAG_NAME_CASE) == HTMLCorePreferenceNames.UPPER);
    fTagNameLower.setSelection(getModelPreferences().getInt(HTMLCorePreferenceNames.TAG_NAME_CASE) == HTMLCorePreferenceNames.LOWER);
    fAttrNameUpper.setSelection(getModelPreferences().getInt(HTMLCorePreferenceNames.ATTR_NAME_CASE) == HTMLCorePreferenceNames.UPPER);
    fAttrNameLower.setSelection(getModelPreferences().getInt(HTMLCorePreferenceNames.ATTR_NAME_CASE) == HTMLCorePreferenceNames.LOWER);

    initializeValuesForFormattingGroup();
  }

  private void initializeValuesForFormattingGroup() {
    // Formatting
    fLineWidthText.setText(getModelPreferences().getString(HTMLCorePreferenceNames.LINE_WIDTH));
    fSplitMultiAttrs.setSelection(getModelPreferences().getBoolean(
        HTMLCorePreferenceNames.SPLIT_MULTI_ATTRS));
    fAlignEndBracket.setSelection(getModelPreferences().getBoolean(
        HTMLCorePreferenceNames.ALIGN_END_BRACKET));
    fClearAllBlankLines.setSelection(getModelPreferences().getBoolean(
        HTMLCorePreferenceNames.CLEAR_ALL_BLANK_LINES));

    if (HTMLCorePreferenceNames.TAB.equals(getModelPreferences().getString(
        HTMLCorePreferenceNames.INDENTATION_CHAR))) {
      fIndentUsingTabs.setSelection(true);
      fIndentUsingSpaces.setSelection(false);
    } else {
      fIndentUsingSpaces.setSelection(true);
      fIndentUsingTabs.setSelection(false);
    }

    fIndentationSize.setSelection(getModelPreferences().getInt(
        HTMLCorePreferenceNames.INDENTATION_SIZE));
  }

  protected void storeValues() {
    if (fTagNameUpper.getSelection())
      getModelPreferences().setValue(HTMLCorePreferenceNames.TAG_NAME_CASE,
          HTMLCorePreferenceNames.UPPER);
    else
      getModelPreferences().setValue(HTMLCorePreferenceNames.TAG_NAME_CASE,
          HTMLCorePreferenceNames.LOWER);
    if (fAttrNameUpper.getSelection())
      getModelPreferences().setValue(HTMLCorePreferenceNames.ATTR_NAME_CASE,
          HTMLCorePreferenceNames.UPPER);
    else
      getModelPreferences().setValue(HTMLCorePreferenceNames.ATTR_NAME_CASE,
          HTMLCorePreferenceNames.LOWER);

    storeValuesForFormattingGroup();
  }

  private void storeValuesForFormattingGroup() {
    // Formatting
    getModelPreferences().setValue(HTMLCorePreferenceNames.LINE_WIDTH, fLineWidthText.getText());
    getModelPreferences().setValue(HTMLCorePreferenceNames.SPLIT_MULTI_ATTRS,
        fSplitMultiAttrs.getSelection());
    getModelPreferences().setValue(HTMLCorePreferenceNames.ALIGN_END_BRACKET,
        fAlignEndBracket.getSelection());
    getModelPreferences().setValue(HTMLCorePreferenceNames.CLEAR_ALL_BLANK_LINES,
        fClearAllBlankLines.getSelection());

    if (fIndentUsingTabs.getSelection()) {
      getModelPreferences().setValue(HTMLCorePreferenceNames.INDENTATION_CHAR,
          HTMLCorePreferenceNames.TAB);
    } else {
      getModelPreferences().setValue(HTMLCorePreferenceNames.INDENTATION_CHAR,
          HTMLCorePreferenceNames.SPACE);
    }
    getModelPreferences().setValue(HTMLCorePreferenceNames.INDENTATION_SIZE,
        fIndentationSize.getSelection());
  }

  public boolean performOk() {
    boolean result = super.performOk();

    doSavePreferenceStore();

    // Save values from inline elements
    HTMLFormattingUtil.exportToPreferences(fContentProvider.fElements.toArray());

    return result;
  }

  protected Preferences getModelPreferences() {
    return HTMLCorePlugin.getDefault().getPluginPreferences();
  }

  protected IPreferenceStore doGetPreferenceStore() {
    return HTMLUIPlugin.getDefault().getPreferenceStore();
  }

  private void doSavePreferenceStore() {
    HTMLUIPlugin.getDefault().savePluginPreferences(); // UI
    HTMLCorePlugin.getDefault().savePluginPreferences(); // model
  }

  protected Control createContents(Composite parent) {
    final Composite composite = super.createComposite(parent, 1);
    PlatformUI.getWorkbench().getHelpSystem().setHelp(composite,
        IHelpContextIds.HTML_PREFWEBX_SOURCE_HELPID);

    new PreferenceLinkArea(
        composite,
        SWT.WRAP | SWT.MULTI,
        "org.eclipse.wst.sse.ui.preferences.editor", HTMLUIMessages._UI_STRUCTURED_TEXT_EDITOR_PREFS_LINK,//$NON-NLS-1$
        (IWorkbenchPreferenceContainer) getContainer(), null).getControl().setLayoutData(
        GridDataFactory.fillDefaults().hint(150, SWT.DEFAULT).create());
    new Label(composite, SWT.NONE).setLayoutData(GridDataFactory.swtDefaults().create());

    createContentsForFormattingGroup(composite);
    createContentsForPreferredCaseGroup(composite, 2);
    setSize(composite);
    loadPreferences();

    return composite;
  }

  protected void validateValues() {
    boolean isError = false;
    String widthText = null;

    if (fLineWidthText != null) {
      try {
        widthText = fLineWidthText.getText();
        int formattingLineWidth = Integer.parseInt(widthText);
        if ((formattingLineWidth < WIDTH_VALIDATION_LOWER_LIMIT)
            || (formattingLineWidth > WIDTH_VALIDATION_UPPER_LIMIT)) {
          throw new NumberFormatException();
        }
      } catch (NumberFormatException nfexc) {
        setInvalidInputMessage(widthText);
        setValid(false);
        isError = true;
      }
    }

    int indentSize = 0;
    if (fIndentationSize != null) {
      try {
        indentSize = fIndentationSize.getSelection();
        if ((indentSize < MIN_INDENTATION_SIZE) || (indentSize > MAX_INDENTATION_SIZE)) {
          throw new NumberFormatException();
        }
      } catch (NumberFormatException nfexc) {
        setInvalidInputMessage(Integer.toString(indentSize));
        setValid(false);
        isError = true;
      }
    }

    if (!isError) {
      setErrorMessage(null);
      setValid(true);
    }
  }

  private static class HTMLElementDialog extends ElementListSelectionDialog {
    public HTMLElementDialog(Shell parent) {
      super(parent, new ILabelProvider() {

        public void removeListener(ILabelProviderListener listener) {
        }

        public boolean isLabelProperty(Object element, String property) {
          return false;
        }

        public void dispose() {
        }

        public void addListener(ILabelProviderListener listener) {
        }

        public String getText(Object element) {
          return element.toString();
        }

        public Image getImage(Object element) {
          return null;
        }
      });
    }

    public int open() {
      final List list = new ArrayList();
      IRunnableContext context = PlatformUI.getWorkbench().getProgressService();
      IRunnableWithProgress runnable = new IRunnableWithProgress() {
        public void run(IProgressMonitor monitor) throws InvocationTargetException,
            InterruptedException {
          if (monitor == null) {
            monitor = new NullProgressMonitor();
          }
          monitor.beginTask("Searching for known elements", 1); //$NON-NLS-1$
          try {
            if (monitor.isCanceled()) {
              throw new InterruptedException();
            }
            CMNamedNodeMap map = HTMLCMDocumentFactory.getCMDocument(CMDocType.HTML5_DOC_TYPE).getElements();
            Iterator it = map.iterator();
            while (it.hasNext()) {
              CMNode node = (CMNode) it.next();
              if (!node.getNodeName().startsWith("SSI:")) { //$NON-NLS-1$
                list.add(node.getNodeName().toLowerCase(Locale.US));
              }
              monitor.worked(1);
            }
          } finally {
            monitor.done();
          }
        }
      };
      try {
        context.run(true, true, runnable);
      } catch (InvocationTargetException e) {
        return CANCEL;
      } catch (InterruptedException e) {
        return CANCEL;
      }
      setElements(list.toArray());
      return super.open();
    }
  }

  private class ContentProvider implements IStructuredContentProvider {

    private List fElements;

    public ContentProvider() {
      fElements = new ArrayList(Arrays.asList(HTMLFormattingUtil.getInlineElements()));
    }

    public void dispose() {
    }

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    }

    public void addElement(String name) {
      if (!fElements.contains(name))
        fElements.add(name);
    }

    public void removeElement(String name) {
      fElements.remove(name);
    }

    public Object[] getElements(Object inputElement) {
      return fElements.toArray();
    }

    public void restoreDefaults() {
      fElements = new ArrayList(Arrays.asList(HTMLFormattingUtil.getDefaultInlineElements()));
    }

  }
}
