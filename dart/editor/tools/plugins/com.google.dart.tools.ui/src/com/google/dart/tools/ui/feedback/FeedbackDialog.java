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
package com.google.dart.tools.ui.feedback;

import com.google.dart.tools.ui.DartToolsPlugin;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.operation.ModalContext;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.ProgressMonitorPart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import java.lang.reflect.InvocationTargetException;

/**
 * A dialog to collect user feedback.
 */
public class FeedbackDialog extends Dialog implements IRunnableContext, DisposeListener {

  /**
   * Key used to retrieve stored settings from the activator's cache.
   */
  private static final String DIALOG_SETTING_CACHE_KEY = FeedbackDialog.class.getSimpleName();

  /**
   * Key for caching opt-in setting state.
   */
  private static final String OPT_IN_SETTING_KEY = "opt.in.enabled"; //$NON-NLS-1$

  private Button okButton;
  private Text feedbackText;
  private Button sendAdditionalDataButton;
  private Link previewDataLink;
  private Button sendScreenshotButton;
  private Link previewScreenshotLink;

  private ProgressMonitorPart progressMonitorPart;

  /**
   * FeedbackReport report for preview and submission.
   */
  private final FeedbackReport feedbackReport;

  /**
   * Feedback image, cached for proper disposal.
   */
  private Image feedbackImage;

  /**
   * The screenshot of the Editor/ Editor Plug-in just before the Send Feedback button was pressed.
   */
  private Image screenshot;

  /**
   * Create the feedback dialog.
   * 
   * @param parentShell the parent shell
   * @param productName the name of the installed product (e.g., "Editor" vs. "Editor Plugin")
   */
  public FeedbackDialog(Shell parentShell, String productName, Image screenshot) {
    super(parentShell);
    feedbackReport = new FeedbackReport(productName, screenshot);
    this.screenshot = screenshot;
  }

  @Override
  public void run(boolean fork, boolean cancelable, IRunnableWithProgress runnable)
      throws InvocationTargetException, InterruptedException {
    progressMonitorPart.getParent().setVisible(true);
    ModalContext.run(runnable, fork, progressMonitorPart, getShell().getDisplay());
  }

  @Override
  public void widgetDisposed(DisposeEvent e) {
    if (feedbackImage != null) {
      feedbackImage.dispose();
    }
  }

  @Override
  protected void buttonPressed(int buttonId) {
    updateFeedbackTextForClick(buttonId);
    super.buttonPressed(buttonId);
  }

  @Override
  protected void configureShell(Shell newShell) {
    super.configureShell(newShell);
    newShell.setText(FeedbackMessages.FeedbackDialog_Title);
    newShell.addDisposeListener(this);
  }

  @Override
  protected Control createButtonBar(Composite parent) {
    //overriding to tweak layout

    Composite composite = new Composite(parent, SWT.NONE);
    GridLayout layout = new GridLayout();
    layout.numColumns = 0; // this is incremented by createButton
    layout.makeColumnsEqualWidth = true;
    layout.marginWidth = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
    layout.marginHeight = 0; //convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
    layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
    layout.verticalSpacing = 0; //convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
    //put some padding at the bottom
    layout.marginBottom = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
    composite.setLayout(layout);
    GridData data = new GridData(GridData.HORIZONTAL_ALIGN_END | GridData.VERTICAL_ALIGN_CENTER);
    composite.setLayoutData(data);
    composite.setFont(parent.getFont());

    // Add the buttons to the button bar.
    createButtonsForButtonBar(composite);
    return composite;
  }

  @Override
  protected void createButtonsForButtonBar(Composite parent) {
    okButton = createButton(
        parent,
        IDialogConstants.OK_ID,
        FeedbackMessages.FeedbackDialog_OK_Button_Text,
        true);
    createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    updateEnablement();
  }

  @Override
  protected Control createDialogArea(Composite parent) {
    Composite container = (Composite) super.createDialogArea(parent);
    Composite composite = new Composite(container, SWT.NONE);
    GridLayout gl_composite = new GridLayout(2, false);
    gl_composite.marginHeight = 0;
    composite.setLayout(gl_composite);
    composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

    Label imageLabel = new Label(composite, SWT.NONE);
    GridData data = new GridData(SWT.CENTER, SWT.TOP, false, false, 1, 1);
    data.verticalSpan = 2;
    imageLabel.setLayoutData(data);
    ImageDescriptor imageDescriptor = DartToolsPlugin.getImageDescriptor("icons/insert_comment.png"); //$NON-NLS-1$
    feedbackImage = imageDescriptor.createImage();
    imageLabel.setImage(feedbackImage);

    Label inviteText = new Label(composite, SWT.NONE);
    inviteText.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false, 1, 1));
    inviteText.setText(FeedbackMessages.FeedbackDialog_Description_Text);

    feedbackText = new Text(composite, SWT.BORDER | SWT.WRAP | SWT.V_SCROLL | SWT.MULTI);
    GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
    gd.minimumWidth = 420;
    gd.minimumHeight = 160;
    feedbackText.setLayoutData(gd);

    //spacer
    new Label(composite, SWT.NONE);

    Composite logOptinComposite = new Composite(composite, SWT.NONE);
    logOptinComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
    GridLayout gl_logOptinComposite = new GridLayout(2, false);
    gl_logOptinComposite.marginWidth = 0;
    gl_logOptinComposite.horizontalSpacing = 0;
    gl_logOptinComposite.verticalSpacing = 0;
    gl_logOptinComposite.marginHeight = 0;
    logOptinComposite.setLayout(gl_logOptinComposite);

    sendAdditionalDataButton = new Button(logOptinComposite, SWT.CHECK);
    sendAdditionalDataButton.setText(FeedbackMessages.FeedbackDialog_send_additional_data_optin_Text);

    previewDataLink = new Link(logOptinComposite, SWT.NONE);
    GridData gd_1 = new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1);
    gd_1.verticalIndent = 2;
    previewDataLink.setLayoutData(gd_1);
    previewDataLink.setText(FeedbackMessages.FeedbackDialog_link_text);

    if (OpenFeedbackDialogAction.SCREEN_CAPTURE_ENABLED) {
      sendScreenshotButton = new Button(logOptinComposite, SWT.CHECK);
      sendScreenshotButton.setText(FeedbackMessages.FeedbackDialog_send_screenshot_optin_Text);

      previewScreenshotLink = new Link(logOptinComposite, SWT.NONE);
      GridData gd_2 = new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1);
      gd_2.verticalIndent = 2;
      previewScreenshotLink.setLayoutData(gd_2);
      previewScreenshotLink.setText(FeedbackMessages.FeedbackDialog_link_screenshot_text);
    }

    //spacer
    new Label(composite, SWT.NONE);

    Composite monitorComposite = new Composite(composite, SWT.NONE);
    GridLayout layout = new GridLayout(2, false);
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    layout.numColumns = 2;
    monitorComposite.setLayout(layout);
    monitorComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
    GridLayout pmLayout = new GridLayout();
    progressMonitorPart = new ProgressMonitorPart(monitorComposite, pmLayout, false);
    gd = new GridData(GridData.FILL_HORIZONTAL);
    progressMonitorPart.setLayoutData(gd);
    monitorComposite.setVisible(false);
    new Label(monitorComposite, SWT.NONE);

    restoreSettings();
    hookupListeners();

    return container;
  }

  /**
   * Return the dialog store to cache values into
   */
  protected IDialogSettings getDialogSettings() {
    IDialogSettings bundleSettings = DartToolsPlugin.getDefault().getDialogSettings();
    IDialogSettings dialogSettings = bundleSettings.getSection(DIALOG_SETTING_CACHE_KEY);
    if (dialogSettings == null) {
      dialogSettings = bundleSettings.addNewSection(DIALOG_SETTING_CACHE_KEY);
    }
    return dialogSettings;
  }

  @Override
  protected int getShellStyle() {
    return SWT.MODELESS | SWT.CLOSE | SWT.MIN | SWT.MAX | SWT.RESIZE;
  }

  @Override
  protected boolean isResizable() {
    return true;
  }

  @Override
  protected void okPressed() {
    try {
      if (submitFeedback().isOK()) {
        saveSettings();
        super.okPressed();
        MessageDialog.openInformation(
            getParentShell(),
            FeedbackMessages.FeedbackDialog_feedback_sent_label,
            FeedbackMessages.FeedbackDialog_feedback_sent_details);
        return;
      }
    } catch (Throwable th) {
      DartToolsPlugin.log(th);
    }
    MessageDialog.openError(
        getParentShell(),
        FeedbackMessages.FeedbackDialog_error_submitting_label,
        FeedbackMessages.FeedbackDialog_error_submitting_detail);
    setReturnCode(CANCEL);
  }

  /**
   * Update button and link enablement.
   */
  protected void updateEnablement() {
    // Controls are null during dialog creation
    if (okButton != null) {
      boolean hasContent = !feedbackText.getText().isEmpty();
      okButton.setEnabled(hasContent);
    }
  }

  private void hookupListeners() {
    feedbackText.addModifyListener(new ModifyListener() {
      @Override
      public void modifyText(ModifyEvent e) {
        updateEnablement();
      }
    });
    previewDataLink.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        try {
          LogViewer logViewer = new LogViewer(
              getShell(),
              feedbackReport.getDetailString(sendLogData()));

          logViewer.open();
        } catch (Throwable th) {
          MessageDialog.openError(
              getParentShell(),
              FeedbackMessages.FeedbackDialog_error_opening_log_label,
              FeedbackMessages.FeedbackDialog_error_opening_log_detail);
        }
      }
    });
    sendAdditionalDataButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        updateEnablement();
      }
    });
    if (OpenFeedbackDialogAction.SCREEN_CAPTURE_ENABLED) {
      previewScreenshotLink.addSelectionListener(new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent e) {
          if (screenshot == null) {
            MessageDialog.openError(
                getParentShell(),
                FeedbackMessages.FeedbackDialog_error_opening_screenshot_label,
                FeedbackMessages.FeedbackDialog_error_opening_screenshot_detail);
          }
          try {
            ScreenshotViewer screenshotViewer = new ScreenshotViewer(getShell(), screenshot);
            screenshotViewer.open();
          } catch (Throwable t) {
            MessageDialog.openError(
                getParentShell(),
                FeedbackMessages.FeedbackDialog_error_opening_screenshot_label,
                FeedbackMessages.FeedbackDialog_error_opening_screenshot_detail);
          }
        }
      });
      sendScreenshotButton.addSelectionListener(new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent e) {
          updateEnablement();
        }
      });
    }
  }

  private void restoreSettings() {
    sendAdditionalDataButton.setSelection(getDialogSettings().getBoolean(OPT_IN_SETTING_KEY));
  }

  private void saveSettings() {
    getDialogSettings().put(OPT_IN_SETTING_KEY, sendLogData());
  }

  private boolean sendLogData() {
    return sendAdditionalDataButton.getSelection();
  }

  private boolean sendScreenshot() {
    // TODO (jwren) remove this check.  This is currently needed when
    // OpenFeedbackDialogAction.SCREEN_CAPTURE_ENABLED is false.
    if (sendScreenshotButton == null) {
      return false;
    } else {
      return sendScreenshotButton.getSelection();
    }

  }

  private IStatus submitFeedback() {
    final IStatus[] status = new IStatus[1];

    try {
      run(false, false, new IRunnableWithProgress() {
        @Override
        public void run(IProgressMonitor monitor) throws InvocationTargetException,
            InterruptedException {
          status[0] = new FeedbackSubmissionJob(new FeedbackWriter(
              feedbackReport,
              sendLogData(),
              sendScreenshot())).run(monitor);
        }
      });
    } catch (InvocationTargetException e) {
      status[0] = DartToolsPlugin.createErrorStatus(e.getMessage());
    } catch (InterruptedException e) {
      status[0] = DartToolsPlugin.createErrorStatus(e.getMessage());
    }

    return status[0];

  }

  private void updateFeedbackTextForClick(int buttonId) {
    feedbackReport.setFeedbackText(buttonId == IDialogConstants.OK_ID ? feedbackText.getText() : "");
  }
}
