package com.google.dart.tools.debug.ui.internal.dialogs;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.mobile.AndroidDebugBridge;
import com.google.dart.tools.core.mobile.AndroidDevice;
import com.google.dart.tools.core.mobile.MobileUrlConnectionException;
import com.google.dart.tools.debug.ui.internal.mobile.MobileMainTab;
import com.google.dart.tools.ui.internal.util.ExternalBrowserUtil;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;

import java.net.ConnectException;

public class MobilePortForwardDialog extends Dialog {

  private static final String CHROME_ANDROID_URL = "https://play.google.com/store/apps/details?id=com.android.chrome";
  private static final String CHROME_DEV_MACHINE_URL = "http://www.google.com/chrome";

  private final String pageUrl;
  private Link descriptionLabel;
  private Label testResultLabel;

  public MobilePortForwardDialog(Shell parentShell, String pageUrl, boolean localhostOverUsb) {
    super(parentShell);
    this.pageUrl = pageUrl;
    setShellStyle(SWT.DIALOG_TRIM | SWT.RESIZE);
  }

  @Override
  protected void configureShell(Shell newShell) {
    super.configureShell(newShell);
    newShell.setText("Mobile Port Fowarding");
  }

  @Override
  protected Control createButtonBar(Composite parent) {
    Composite buttonBar = (Composite) super.createButtonBar(parent);
    ((GridLayout) buttonBar.getLayout()).makeColumnsEqualWidth = false;
    buttonBar.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL
        | GridData.VERTICAL_ALIGN_CENTER));
    return buttonBar;
  }

  @Override
  protected void createButtonsForButtonBar(Composite parent) {

    final Button testButton = createButton(
        parent,
        IDialogConstants.CLIENT_ID,
        "Test Connection",
        false);
    testButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetDefaultSelected(SelectionEvent e) {
        widgetSelected(e);
      }

      @Override
      public void widgetSelected(SelectionEvent e) {
        testConnection();
      }
    });
    testButton.getDisplay().asyncExec(new Runnable() {
      @Override
      public void run() {
        testButton.setFocus();
      }
    });

    // increment the number of columns in the button bar
    ((GridLayout) parent.getLayout()).numColumns++;
    testResultLabel = new Label(parent, SWT.NONE);
    testResultLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

    Button closeButton = createButton(
        parent,
        IDialogConstants.CLOSE_ID,
        IDialogConstants.CLOSE_LABEL,
        true);
    closeButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetDefaultSelected(SelectionEvent e) {
        widgetSelected(e);
      }

      @Override
      public void widgetSelected(SelectionEvent e) {
        close();
      }
    });
  }

  @Override
  protected Control createDialogArea(Composite parent) {
    Composite container = (Composite) super.createDialogArea(parent);
    container.setLayout(new GridLayout(2, false));

    descriptionLabel = new Link(container, SWT.WRAP);
    GridData gd_descriptionLabel = new GridData(SWT.LEFT, SWT.CENTER, true, false, 2, 1);
    gd_descriptionLabel.verticalIndent = 20;
    gd_descriptionLabel.horizontalIndent = 20;
    descriptionLabel.setLayoutData(gd_descriptionLabel);
    descriptionLabel.setText("The mobile device cannot access the local web server"
        + " running on the developer machine.\n"
        + "Please take the following steps to fix this problem:\n" //
        + "\n" //

        + "1) Install Chrome on the <a href=\""
        + CHROME_ANDROID_URL
        + "\">mobile device</a>"
        + " and the <a href=\""
        + CHROME_DEV_MACHINE_URL
        + "\">developer machine</a>\n"

        + "2) Ensure that Chrome is running on both the mobile device and the developer machine\n"
        + "\n" //
        + "3) Open chrome://inspect on the developer machine\n"
        + "4) Click \"Port forwarding...\" to open the port forwarding dialog\n"
        + "5) Add \"8080\" and \"localhost:8080\" to the list of forwarded ports if not there already\n"
        + "6) Make sure that \"Enable port forwarding\" is checked\n"
        + "7) Click \"Done\" to close the port forwarding dialog\n" //
        + "\n" //
        + "Ensure that the mobile device appears on the chrome://inspect page and that the port is\n"
        + "listed and has a green circle next to it.\n" //
        + "\n" //
        + "8) In the Manage Launchs dialog, select the mobile launch configuration\n"
        + "and ensure that \""
        + MobileUrlConnectionException.SERVE_OVER_USB_TEXT
        + "\" is selected\n" //
        + "\n" //
        + "For more information, see <a href=\""
        + MobileMainTab.MOBILE_DOC_URL
        + "\">port forwarding setup</a>\n");
    descriptionLabel.addSelectionListener(new SelectionListener() {
      @Override
      public void widgetDefaultSelected(SelectionEvent e) {
        widgetSelected(e);
      }

      @Override
      public void widgetSelected(SelectionEvent e) {
        ExternalBrowserUtil.openInExternalBrowser(e.text.trim());
      }
    });

    return container;
  }

  /**
   * Return the initial size of the dialog.
   */
  @Override
  protected Point getInitialSize() {
    return getShell().computeSize(SWT.DEFAULT, SWT.DEFAULT);
  }

  /**
   * Test the connection between the mobile device and the webserver on the development machine.
   */
  private void testConnection() {
    testResultLabel.setText("Testing connection...");

    // Test connection on a background thread
    Thread thread = new Thread("Test Mobile Port Forwarding") {

      @Override
      public void run() {
        long startTime = System.currentTimeMillis();

        // Test connection from mobile device to developer machine
        final IStatus status = testConnectionInBackground();

        // Ensure text is visible for at least 1 1/2 seconds
        long delta = startTime + 1500 - System.currentTimeMillis();
        if (delta > 0) {
          try {
            Thread.sleep(delta);
          } catch (InterruptedException e) {
            //$FALL-THROUGH$
          }
        }

        getShell().getDisplay().asyncExec(new Runnable() {
          @Override
          public void run() {
            String message;
            if (status.isOK()) {
              message = "Connection validated";
            } else {
              message = status.getMessage();
            }
            if (message.startsWith(ConnectException.class.getName())) {
              message = message.substring(ConnectException.class.getName().length() + 2).trim();
            }
            testResultLabel.setText(message);
          }
        });
      }
    };
    thread.setDaemon(true);
    thread.start();
  }

  private IStatus testConnectionInBackground() {
    AndroidDebugBridge debugBridge = AndroidDebugBridge.getAndroidDebugBridge();

    // Check that the device is still connected and authorized
    AndroidDevice device = debugBridge.getConnectedDevice();
    if (device == null) {
      return new Status(IStatus.ERROR, DartCore.PLUGIN_ID, "No device connected");
    }
    if (!device.isAuthorized()) {
      return new Status(IStatus.ERROR, DartCore.PLUGIN_ID, "Connected device is not authorized");
    }

    return debugBridge.isHtmlPageAccessible(device, pageUrl);
  }
}
