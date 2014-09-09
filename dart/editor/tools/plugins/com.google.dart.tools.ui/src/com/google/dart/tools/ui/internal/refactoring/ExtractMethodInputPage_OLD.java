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
package com.google.dart.tools.ui.internal.refactoring;

import com.google.dart.engine.services.refactoring.Parameter;
import com.google.dart.tools.internal.corext.refactoring.util.Messages;
import com.google.dart.tools.ui.DartPluginImages;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.PreferenceConstants;
import com.google.dart.tools.ui.internal.dialogs.TextFieldNavigationHandler;
import com.google.dart.tools.ui.internal.text.DartHelpContextIds;
import com.google.dart.tools.ui.internal.text.editor.DartSourceViewer;
import com.google.dart.tools.ui.internal.util.RowLayouter;
import com.google.dart.tools.ui.text.DartSourceViewerConfiguration;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.Document;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

/**
 * @coverage dart.editor.ui.refactoring.ui
 */
public class ExtractMethodInputPage_OLD extends UserInputWizardPage {

  public static final String PAGE_NAME = "ExtractMethodInputPage";//$NON-NLS-1$

  private ServiceExtractMethodRefactoring fRefactoring;
  private Text fTextField;
  private boolean fFirstTime;
  private DartSourceViewer fSignaturePreview;
  private Document fSignaturePreviewDocument;
//  private IDialogSettings fSettings;

  private static final String DESCRIPTION = RefactoringMessages.ExtractMethodInputPage_description;

//  private static final String GENERATE_JAVADOC = "GenerateJavadoc"; //$NON-NLS-1$

  public ExtractMethodInputPage_OLD() {
    super(PAGE_NAME);
    setImageDescriptor(DartPluginImages.DESC_WIZBAN_REFACTOR_CU);
    setDescription(DESCRIPTION);
    fFirstTime = true;
    fSignaturePreviewDocument = new Document();
  }

  @Override
  public void createControl(Composite parent) {
    fRefactoring = (ServiceExtractMethodRefactoring) getRefactoring();
    loadSettings();

    Composite result = new Composite(parent, SWT.NONE);
    setControl(result);
    GridLayout layout = new GridLayout();
    layout.numColumns = 2;
    result.setLayout(layout);
    RowLayouter layouter = new RowLayouter(2);
    GridData gd = null;

    initializeDialogUnits(result);

    Label label = new Label(result, SWT.NONE);
    label.setText(getLabelText());

    fTextField = createTextInputField(result, SWT.BORDER);
    fTextField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    layouter.perform(label, fTextField, 1);

    if (!fRefactoring.getParameters().isEmpty()) {
      // TODO(scheglov) pass some StubTypeContext
      ChangeParametersControl_OLD cp = new ChangeParametersControl_OLD(
          result,
          SWT.NONE,
          RefactoringMessages.ExtractMethodInputPage_parameters,
          new IParameterListChangeListener_OLD.Empty() {
            @Override
            public void parameterChanged(Parameter parameter) {
              parameterModified();
            }

            @Override
            public void parameterListChanged() {
              parameterModified();
            }
          },
          ChangeParametersControl_OLD.Mode.EXTRACT_METHOD);
      gd = new GridData(GridData.FILL_BOTH);
      gd.horizontalSpan = 2;
      cp.setLayoutData(gd);
      cp.setInput(fRefactoring.getParameters());
    }

//    checkBox = new Button(result, SWT.CHECK);
//    checkBox.setText(RefactoringMessages.ExtractMethodInputPage_generateJavadocComment);
//    boolean generate = computeGenerateJavadoc();
//    setGenerateJavadoc(generate);
//    checkBox.setSelection(generate);
//    checkBox.addSelectionListener(new SelectionAdapter() {
//      @Override
//      public void widgetSelected(SelectionEvent e) {
//        setGenerateJavadoc(((Button) e.widget).getSelection());
//      }
//    });
//    layouter.perform(checkBox);

    // occurrences
    {
      int occurrences = fRefactoring.getNumberOfOccurrences();
      Button checkBox = new Button(result, SWT.CHECK);
      if (occurrences == 1) {
        checkBox.setText(RefactoringMessages.ExtractMethodInputPage_duplicates_none);
      } else if (occurrences == 2) {
        checkBox.setText(RefactoringMessages.ExtractMethodInputPage_duplicates_single);
      } else {
        checkBox.setText(Messages.format(
            RefactoringMessages.ExtractMethodInputPage_duplicates_multi,
            occurrences - 1));
      }
      checkBox.setSelection(true);
      checkBox.setEnabled(occurrences > 1);
      checkBox.addSelectionListener(new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent e) {
          fRefactoring.setReplaceAllOccurrences(((Button) e.widget).getSelection());
        }
      });
      layouter.perform(checkBox);
    }

    // getter
    {
      boolean canExtractGetter = fRefactoring.canExtractGetter();
      Button checkBox = new Button(result, SWT.CHECK);
      checkBox.setText(RefactoringMessages.ExtractMethodInputPage_getter);
      checkBox.setEnabled(canExtractGetter);
      if (canExtractGetter) {
        checkBox.setSelection(fRefactoring.getExtractGetter());
      }
      checkBox.addSelectionListener(new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent e) {
          fRefactoring.setExtractGetter(((Button) e.widget).getSelection());
        }
      });
      layouter.perform(checkBox);
    }

    label = new Label(result, SWT.SEPARATOR | SWT.HORIZONTAL);
    label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    layouter.perform(label);

    createSignaturePreview(result, layouter);

    Dialog.applyDialogFont(result);
    PlatformUI.getWorkbench().getHelpSystem().setHelp(
        getControl(),
        DartHelpContextIds.EXTRACT_METHOD_WIZARD_PAGE);
  }

  @Override
  public void setVisible(boolean visible) {
    if (visible) {
      if (fFirstTime) {
        fFirstTime = false;
        setPageComplete(false);
        fRefactoring.setMethodName("methodName");
        updatePreview();
        fTextField.setFocus();
      } else {
        setPageComplete(validatePage(true));
      }
    }
    super.setVisible(visible);
  }

//  private boolean computeGenerateJavadoc() {
//    boolean result = fRefactoring.getGenerateJavadoc();
//    if (result) {
//      return result;
//    }
//    return fSettings.getBoolean(GENERATE_JAVADOC);
//  }

  private void createSignaturePreview(Composite composite, RowLayouter layouter) {
    Label previewLabel = new Label(composite, SWT.NONE);
    previewLabel.setText(RefactoringMessages.ExtractMethodInputPage_signature_preview);
    layouter.perform(previewLabel);

    IPreferenceStore store = DartToolsPlugin.getDefault().getCombinedPreferenceStore();
    fSignaturePreview = new DartSourceViewer(composite, null, null, false, SWT.READ_ONLY
        | SWT.V_SCROLL | SWT.WRAP /*| SWT.BORDER*/, store);
    fSignaturePreview.configure(new DartSourceViewerConfiguration(
        DartToolsPlugin.getDefault().getDartTextTools().getColorManager(),
        store,
        null,
        null));
    fSignaturePreview.getTextWidget().setFont(
        JFaceResources.getFont(PreferenceConstants.EDITOR_TEXT_FONT));
    fSignaturePreview.adaptBackgroundColor(composite);
    fSignaturePreview.setDocument(fSignaturePreviewDocument);
    fSignaturePreview.setEditable(false);

    Control signaturePreviewControl = fSignaturePreview.getControl();
    PixelConverter pixelConverter = new PixelConverter(signaturePreviewControl);
    GridData gdata = new GridData(GridData.FILL_BOTH);
    gdata.widthHint = pixelConverter.convertWidthInCharsToPixels(50);
    gdata.heightHint = pixelConverter.convertHeightInCharsToPixels(2);
    signaturePreviewControl.setLayoutData(gdata);
    layouter.perform(signaturePreviewControl);
  }

  private Text createTextInputField(Composite parent, int style) {
    Text result = new Text(parent, style);
    result.addModifyListener(new ModifyListener() {
      @Override
      public void modifyText(ModifyEvent e) {
        textModified(getText());
      }
    });
    TextFieldNavigationHandler.install(result);
    return result;
  }

  private String getLabelText() {
    return RefactoringMessages.ExtractMethodInputPage_label_text;
  }

  private String getText() {
    if (fTextField == null) {
      return null;
    }
    return fTextField.getText();
  }

  private void loadSettings() {
    // TODO(scheglov)
//    fSettings = getDialogSettings().getSection(ExtractMethodWizard.DIALOG_SETTING_SECTION);
//    if (fSettings == null) {
//      fSettings = getDialogSettings().addNewSection(ExtractMethodWizard.DIALOG_SETTING_SECTION);
//      fSettings.put(
//          GENERATE_JAVADOC,
//          JavaPreferencesSettings.getCodeGenerationSettings(fRefactoring.getCompilationUnit().getJavaProject()).createComments);
//    }
  }

//  private void setGenerateJavadoc(boolean value) {
//    fSettings.put(GENERATE_JAVADOC, value);
//    fRefactoring.setGenerateJavadoc(value);
//  }

  private void parameterModified() {
    updatePreview();
    setPageComplete(validatePage(false));
  }

  private void textModified(String text) {
    fRefactoring.setMethodName(text);
    RefactoringStatus status = validatePage(true);
    if (!status.hasFatalError()) {
      updatePreview();
    } else {
      fSignaturePreviewDocument.set(""); //$NON-NLS-1$
    }
    setPageComplete(status);
  }

  private void updatePreview() {
    if (fSignaturePreview == null) {
      return;
    }

    int top = fSignaturePreview.getTextWidget().getTopPixel();
    String signature;
    try {
      signature = fRefactoring.getSignature();
    } catch (IllegalArgumentException e) {
      signature = ""; //$NON-NLS-1$
    }
    fSignaturePreviewDocument.set(signature);
    fSignaturePreview.getTextWidget().setTopPixel(top);
  }

  private RefactoringStatus validateMethodName() {
    RefactoringStatus result = new RefactoringStatus();
    String text = getText();
    if ("".equals(text)) { //$NON-NLS-1$
      result.addFatalError(RefactoringMessages.ExtractMethodInputPage_validation_emptyMethodName);
      return result;
    }
    result.merge(fRefactoring.checkMethodName());
    return result;
  }

  private RefactoringStatus validatePage(boolean text) {
    RefactoringStatus result = new RefactoringStatus();
    if (text) {
      result.merge(validateMethodName());
      result.merge(validateParameters());
    } else {
      result.merge(validateParameters());
      result.merge(validateMethodName());
    }
    return result;
  }

  private RefactoringStatus validateParameters() {
    RefactoringStatus result = new RefactoringStatus();
    for (Parameter parameter : fRefactoring.getParameters()) {
      if ("".equals(parameter.getNewName())) {
        result.addFatalError(RefactoringMessages.ExtractMethodInputPage_validation_emptyParameterName);
        return result;
      }
      // TODO(scheglov) test types
//      result.merge(TypeContextChecker.checkParameterTypeSyntax(
//          parameter.getNewTypeName(),
//          fRefactoring.getUnit()));
    }
    result.merge(fRefactoring.checkParameterNames());
    return result;
  }
}
