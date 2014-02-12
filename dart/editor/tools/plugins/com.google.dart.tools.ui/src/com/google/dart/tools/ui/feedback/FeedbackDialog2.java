/*
 * Copyright (c) 2014, the Dart project authors.
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

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.ui.DartToolsPlugin;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.operation.ModalContext;
import org.eclipse.jface.wizard.ProgressMonitorPart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
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

public class FeedbackDialog2 extends Dialog {

  /**
   * Key used to retrieve stored settings from the activator's cache.
   */
  private static final String DIALOG_SETTING_CACHE_KEY = FeedbackDialog2.class.getName();
  private static final String USER_EMAIL_KEY = "user.email";
  private static final String INCLUDE_DATA_KEY = "include.data";
  private static final String PUBLIC_KEY = "public";

  private final FeedbackReport report;
  private final Image screenshot;

  private Text feedbackText;
  private Button includeDataCheckbox;
  private Button includeScreenshotCheckbox;
  private Composite dartbugComposite;
  private Button dartbugCheckbox;
  private Composite emailComposite;
  private Text emailText;
  private Label emailLabel1;
  private Label emailLabel2;
  private Label emailLabel3;
  private ProgressMonitorPart progressBar;
  private Button okButton;
  private Button cancelButton;

  public FeedbackDialog2(Shell parentShell, String productName, Image screenshot) {
    super(parentShell);
    this.report = new FeedbackReport(productName, screenshot);
    this.screenshot = screenshot;
    setShellStyle(SWT.SHELL_TRIM);
  }

  @Override
  protected void configureShell(Shell newShell) {
    super.configureShell(newShell);
    newShell.setText("Dart Editor Feedback");
  }

  @Override
  protected Control createButtonBar(Composite parent) {

    Composite buttonBarComposite = new Composite(parent, SWT.NONE);
    GridLayout gl_buttonBarComposite = new GridLayout(2, false);
    gl_buttonBarComposite.marginBottom = 10;
    gl_buttonBarComposite.marginRight = 10;
    gl_buttonBarComposite.marginLeft = 10;
    gl_buttonBarComposite.marginTop = 10;
    buttonBarComposite.setLayout(gl_buttonBarComposite);
    buttonBarComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));

    Composite legalComposite = new Composite(buttonBarComposite, SWT.NONE);
    legalComposite.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
    GridLayout gl_legalComposite = new GridLayout(2, false);
    gl_legalComposite.marginRight = 20;
    gl_legalComposite.verticalSpacing = 0;
    gl_legalComposite.marginWidth = 0;
    gl_legalComposite.marginHeight = 0;
    gl_legalComposite.horizontalSpacing = 0;
    legalComposite.setLayout(gl_legalComposite);

    Label legalLabel1 = new Label(legalComposite, SWT.NONE);
    legalLabel1.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
    legalLabel1.setText("The information above will be sent to Google.");

    Link privacyPolicyLink = new Link(legalComposite, SWT.NONE);
    privacyPolicyLink.setText("See <a>Google's Privacy Policy</a>");

    Link termOfServiceLink = new Link(legalComposite, SWT.NONE);
    termOfServiceLink.setText("and <a>Terms of Service</a>.");

    return super.createButtonBar(buttonBarComposite);
  }

  /**
   * Create contents of the button bar.
   * 
   * @param parent
   */
  @Override
  protected void createButtonsForButtonBar(Composite parent) {
    okButton = createButton(parent, IDialogConstants.OK_ID, "Send Feedback", true);
    cancelButton = createButton(parent, IDialogConstants.CANCEL_ID, "Cancel", false);
    updateEnablement();
  }

  /**
   * Create contents of the dialog.
   * 
   * @param parent
   */
  @Override
  protected Control createDialogArea(Composite parent) {
    Composite container = (Composite) super.createDialogArea(parent);
    GridLayout gl_container = new GridLayout(2, true);
    gl_container.marginBottom = 10;
    gl_container.marginRight = 10;
    gl_container.marginLeft = 10;
    gl_container.marginTop = 10;
    container.setLayout(gl_container);

    Label feedbackLabel = new Label(container, SWT.NONE);
    feedbackLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
    feedbackLabel.setText("Please help improve Dart by telling us what you think. Thanks!");

    feedbackText = new Text(container, SWT.BORDER);
    feedbackText.addModifyListener(new ModifyListener() {
      @Override
      public void modifyText(ModifyEvent e) {
        updateEnablement();
      }
    });
    GridData gd_feedbackText = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1);
    gd_feedbackText.minimumHeight = 50;
    feedbackText.setLayoutData(gd_feedbackText);

    includeScreenshotCheckbox = new Button(container, SWT.CHECK);
    GridData gd_includeScreenshot = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
    gd_includeScreenshot.verticalIndent = 10;
    includeScreenshotCheckbox.setLayoutData(gd_includeScreenshot);
    includeScreenshotCheckbox.setText("Include screenshot");

    Link screenshotLink = new Link(container, SWT.NONE);
    screenshotLink.addSelectionListener(new SelectionListener() {
      @Override
      public void widgetDefaultSelected(SelectionEvent e) {
        showScreenshot();
      }

      @Override
      public void widgetSelected(SelectionEvent e) {
        showScreenshot();
      }
    });
    screenshotLink.setText("<a>show screenshot</a>");

    includeDataCheckbox = new Button(container, SWT.CHECK);
    includeDataCheckbox.setSelection(true);
    GridData gd_includeDataCheckbox = new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1);
    gd_includeDataCheckbox.verticalIndent = 10;
    includeDataCheckbox.setLayoutData(gd_includeDataCheckbox);
    includeDataCheckbox.setText("Include additional editor data");
    includeDataCheckbox.setSelection(getDialogSettings().get(INCLUDE_DATA_KEY) == null
        || getDialogSettings().getBoolean(INCLUDE_DATA_KEY));

    Link dataLink = new Link(container, SWT.NONE);
    dataLink.addSelectionListener(new SelectionListener() {
      @Override
      public void widgetDefaultSelected(SelectionEvent e) {
        showData();
      }

      @Override
      public void widgetSelected(SelectionEvent e) {
        showData();
      }
    });
    dataLink.setText("<a>show data that will be sent</a>");

    dartbugComposite = new Composite(container, SWT.NONE);
    GridLayout gl_dartbugComposite = new GridLayout(2, false);
    gl_dartbugComposite.verticalSpacing = 0;
    gl_dartbugComposite.horizontalSpacing = 0;
    gl_dartbugComposite.marginWidth = 0;
    gl_dartbugComposite.marginHeight = 0;
    dartbugComposite.setLayout(gl_dartbugComposite);
    GridData gd_dartbugComposite = new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1);
    gd_dartbugComposite.verticalIndent = 10;
    dartbugComposite.setLayoutData(gd_dartbugComposite);

    dartbugCheckbox = new Button(dartbugComposite, SWT.CHECK);
    dartbugCheckbox.setText("Yes, you may post this information on");
    dartbugCheckbox.setSelection(getDialogSettings().getBoolean(PUBLIC_KEY));

    Link dartbugLink = new Link(dartbugComposite, SWT.NONE);
    dartbugLink.setText("<a>dartbug.com</a>");

    Label dartbugLabel = new Label(dartbugComposite, SWT.NONE);
    GridData gd_dartbugLabel = new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1);
    gd_dartbugLabel.horizontalIndent = 20;
    dartbugLabel.setLayoutData(gd_dartbugLabel);
    dartbugLabel.setText("This is an open source project; we publicly track our bugs at dartbug.com");

    emailComposite = new Composite(container, SWT.NONE);
    GridLayout gl_emailComposite = new GridLayout(2, false);
    gl_emailComposite.verticalSpacing = 0;
    gl_emailComposite.marginWidth = 0;
    gl_emailComposite.marginHeight = 0;
    gl_emailComposite.horizontalSpacing = 0;
    emailComposite.setLayout(gl_emailComposite);
    GridData gd_emailComposite = new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1);
    gd_emailComposite.verticalIndent = 10;
    emailComposite.setLayoutData(gd_emailComposite);

    emailLabel1 = new Label(emailComposite, SWT.NONE);
    emailLabel1.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
    emailLabel1.setText("CC:");

    emailText = new Text(emailComposite, SWT.BORDER);
    emailText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
    String userEmail = getDialogSettings().get(USER_EMAIL_KEY);
    if (userEmail != null) {
      emailText.setText(userEmail);
    }

    emailLabel2 = new Label(emailComposite, SWT.NONE);
    GridData gd_emailLabel2 = new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1);
    gd_emailLabel2.horizontalIndent = 20;
    emailLabel2.setLayoutData(gd_emailLabel2);
    emailLabel2.setText("You may optionally add an email address. If you do and we post a bug, we will CC you on it.");

    emailLabel3 = new Label(emailComposite, SWT.NONE);
    GridData gd_emailLabel3 = new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1);
    gd_emailLabel3.horizontalIndent = 20;
    emailLabel3.setLayoutData(gd_emailLabel3);
    emailLabel3.setText("We apologize, but we can’t respond directly to all reports, but we read each and every one.");

    progressBar = new ProgressMonitorPart(container, null, 10);
    GridData gd_progressBar = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
    gd_progressBar.verticalIndent = 10;
    progressBar.setLayoutData(gd_progressBar);

    return container;
  }

  @Override
  protected IDialogSettings getDialogBoundsSettings() {
    return getDialogSettings();
  }

  @Override
  protected Point getInitialSize() {
    Point size = super.getInitialSize();
    return new Point(Math.max(size.x, 760), Math.max(size.y, 400));
  }

  @Override
  protected void okPressed() {

    // Create feedback
    final FeedbackSubmissionJob2 job = newFeedbackJob();

    // Save settings
    getDialogSettings().put(INCLUDE_DATA_KEY, includeDataCheckbox.getSelection());
    getDialogSettings().put(USER_EMAIL_KEY, emailText.getText().trim());
    getDialogSettings().put(PUBLIC_KEY, dartbugCheckbox.getSelection());

    // Send feedback
    okButton.setEnabled(false);
    cancelButton.setEnabled(false);
    final IStatus[] result = new IStatus[1];
    try {
      ModalContext.run(new IRunnableWithProgress() {
        @Override
        public void run(IProgressMonitor monitor) throws InvocationTargetException,
            InterruptedException {
          result[0] = job.run(monitor);
        }
      }, true, progressBar, getShell().getDisplay());
    } catch (Throwable e) {
      DartCore.logError("Failed to send feedback", e);
      result[0] = new Status(IStatus.ERROR, DartCore.PLUGIN_ID, e.toString(), e);
    }

    // Report result to the user
    if (result[0].isOK()) {
      super.okPressed();
      MessageDialog.openInformation(
          getParentShell(),
          FeedbackMessages.FeedbackDialog_feedback_sent_label,
          FeedbackMessages.FeedbackDialog_feedback_sent_details);
    } else {
      setReturnCode(CANCEL);
      MessageDialog.openError(
          getParentShell(),
          FeedbackMessages.FeedbackDialog_error_submitting_label,
          FeedbackMessages.FeedbackDialog_error_submitting_detail);
      okButton.setEnabled(true);
      cancelButton.setEnabled(true);
    }
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

  private IDialogSettings getDialogSettings() {
    return DartToolsPlugin.getDefault().getDialogSettingsSection(DIALOG_SETTING_CACHE_KEY);
  }

  private FeedbackSubmissionJob2 newFeedbackJob() {
    report.setFeedbackText(feedbackText.getText());
    String userEmail = emailText.getText().trim();
    if (userEmail != null && userEmail.length() > 0) {
      report.setUserEmail(userEmail);
    }
    return new FeedbackSubmissionJob2(
        report,
        includeDataCheckbox.getSelection(),
        includeScreenshotCheckbox.getSelection(),
        dartbugCheckbox.getSelection());
  }

  private void showData() {
    try {
      LogViewer logViewer = new LogViewer(getShell(), newFeedbackJob().getDataAsText());
      logViewer.open();
    } catch (Throwable th) {
      MessageDialog.openError(
          getParentShell(),
          FeedbackMessages.FeedbackDialog_error_opening_log_label,
          FeedbackMessages.FeedbackDialog_error_opening_log_detail);
    }
  }

  private void showScreenshot() {
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
}
