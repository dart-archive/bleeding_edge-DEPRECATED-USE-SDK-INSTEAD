/*
 * Copyright 2013 Dart project authors.
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

package com.google.dart.tools.debug.ui.internal.dialogs;

import com.google.dart.tools.core.internal.util.ResourceUtil;
import com.google.dart.tools.debug.core.configs.DartServerLaunchConfigurationDelegate;
import com.google.dart.tools.debug.core.configs.DartiumLaunchConfigurationDelegate;
import com.google.dart.tools.debug.core.util.IRemoteConnectionDelegate;
import com.google.dart.tools.debug.core.webkit.ChromiumTabInfo;
import com.google.dart.tools.debug.core.webkit.DefaultChromiumTabChooser;
import com.google.dart.tools.debug.core.webkit.IChromiumTabChooser;
import com.google.dart.tools.debug.ui.internal.DartDebugUIPlugin;
import com.google.dart.tools.debug.ui.internal.view.DebuggerViewManager;
import com.google.dart.tools.ui.themes.Fonts;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ListDialog;

import java.util.List;

/**
 * A dialog to connect to remote debug instances.
 */
public class RemoteConnectionDialog extends TitleAreaDialog {

  /**
   * A class to choose a tab from the given list of tabs.
   */
  public static class ChromeTabChooser implements IChromiumTabChooser {
    public ChromeTabChooser() {

    }

    @Override
    public ChromiumTabInfo chooseTab(final List<ChromiumTabInfo> tabs) {
      if (tabs.size() == 0) {
        return null;
      }

      int tabCount = 0;

      for (ChromiumTabInfo tab : tabs) {
        if (!tab.isChromeExtension()) {
          tabCount++;
        }
      }

      // If there is exactly one tab that is not a Chrome extension.
      if (tabCount == 1) {
        for (ChromiumTabInfo tab : tabs) {
          if (!tab.isChromeExtension()) {
            return tab;
          }
        }
      }

      final ChromiumTabInfo[] result = new ChromiumTabInfo[1];

      Display.getDefault().syncExec(new Runnable() {
        @Override
        public void run() {
          ListDialog dlg = new ListDialog(
              PlatformUI.getWorkbench().getWorkbenchWindows()[0].getShell());
          dlg.setInput(tabs);
          dlg.setTitle("Select tab for remote connection");
          dlg.setContentProvider(new ArrayContentProvider());
          dlg.setLabelProvider(new TabLabelProvider());
          if (dlg.open() == Window.OK) {
            result[0] = (ChromiumTabInfo) dlg.getResult()[0];
          }
        }
      });

      if (result[0] != null) {
        return result[0];
      }

      return new DefaultChromiumTabChooser().chooseTab(tabs);
    }
  }

  public static class ConnectionJob extends Job {
    private IRemoteConnectionDelegate connectionDelegate;
    private String host;
    private int port;
    private IFile file;
    private boolean usePubServe;

    public ConnectionJob(IRemoteConnectionDelegate connectionDelegate, String host, int port,
        IFile file, boolean usePubServe) {
      super("Connecting...");

      this.connectionDelegate = connectionDelegate;
      this.host = host;
      this.port = port;
      this.file = file;
      this.usePubServe = usePubServe;
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
      try {
        connectionDelegate.performRemoteConnection(host, port, file, monitor, usePubServe);

        // Show the debugger view.
        Display.getDefault().asyncExec(new Runnable() {
          @Override
          public void run() {
            DebuggerViewManager viewManager = DebuggerViewManager.getDefault();

            if (viewManager.hasDebuggerView()) {
              viewManager.openDebuggerView();
            }
          }
        });
      } catch (CoreException ce) {
        displayError(ce);
      }

      return Status.OK_STATUS;
    }

    private void displayError(final CoreException exception) {
      Display.getDefault().asyncExec(new Runnable() {
        @Override
        public void run() {
          ErrorDialog.openError(
              PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
              "Error Connecting to " + host + ":" + port,
              null,
              exception.getStatus());
        }
      });
    }
  }

  public enum ConnectionType {
    CHROME(
        "Chrome-based browser", //
        "Connect to a Chrome-based browser", //
        "To start Chrome with remote connections enabled, use the --remote-debugging-port=<port> command-line flag.", //
        "localhost", "9222"),

    COMMAND_LINE("Command-line VM", //
        "Connect to the stand-alone Dart VM", // 
        "To start the command-line VM in debug mode, use the --debug[:<port>] command-line flag.", //
        "localhost", "5858");

    String label;
    String message;
    String helpMessage;

    String hostDefault;
    String portDefault;

    ConnectionType(String label, String message, String helpMessage, String hostDefault,
        String portDefault) {
      this.label = label;
      this.message = message;
      this.helpMessage = helpMessage;
      this.hostDefault = hostDefault;
      this.portDefault = portDefault;
    }

    public void connection(String host, int port, IFile file, boolean usePubServe) {
      IRemoteConnectionDelegate connectionDelegate = null;

      switch (this) {
        case CHROME: {
          connectionDelegate = new DartiumLaunchConfigurationDelegate(new ChromeTabChooser());
          break;
        }

        case COMMAND_LINE: {
          connectionDelegate = new DartServerLaunchConfigurationDelegate();
          break;
        }

        default: {
          throw new IllegalArgumentException();
        }
      }

      if (connectionDelegate != null) {
        Job job = new ConnectionJob(connectionDelegate, host, port, file, usePubServe);
        job.schedule();
      }
    }
  }

  static class TabLabelProvider extends LabelProvider {

    @Override
    public Image getImage(Object element) {
      return null;
    }

    @Override
    public String getText(Object element) {
      if (element instanceof ChromiumTabInfo) {
        return ((ChromiumTabInfo) element).getTitle();
      }
      return null;
    }
  }

  /**
   * Open an instance of a RemoteConnectionDialog.
   * 
   * @param workbench
   */
  public static void show(IWorkbench workbench) {
    RemoteConnectionDialog dialog = new RemoteConnectionDialog(
        workbench.getActiveWorkbenchWindow().getShell());

    dialog.open();
  }

  private Combo exceptionsCombo;

  private Text hostText;

  private Text portText;

  private Text fileText;

  private Text instructionsLabel;

  private Button usePubServeButton;

  private Group pubGroup;

  /**
   * Create a new RemoteConnectionDialog with the given shell as its parent.
   * 
   * @param shell
   */
  public RemoteConnectionDialog(Shell shell) {
    super(shell);
  }

  @Override
  protected void configureShell(Shell newShell) {
    super.configureShell(newShell);

    newShell.setText("Open Remote Connection");
  }

  @Override
  protected Control createDialogArea(Composite parent) {
    Composite contents = (Composite) super.createDialogArea(parent);

    setTitle(getShell().getText());
    setTitleImage(DartDebugUIPlugin.getImage("wiz/run_wiz.png"));

    Composite composite = new Composite(contents, SWT.NONE);
    GridDataFactory.fillDefaults().grab(true, true).align(SWT.FILL, SWT.FILL).applyTo(composite);
    createDialogUI(composite);

    return contents;
  }

  @Override
  protected void okPressed() {
    ConnectionType connection = getConnectionType();

    String host = hostText.getText().trim();
    String port = portText.getText().trim();
    String filePath = fileText.getText().trim();
    boolean usePubServe = usePubServeButton.getSelection();

    IDialogSettings settings = getDialogSettings();

    settings.put("selected", connection.ordinal());
    settings.put(connection.name() + ".host", host);
    settings.put(connection.name() + ".port", port);
    settings.put(connection.name() + ".file", filePath);
    settings.put(connection.name() + ".usePubServe", usePubServe);

    int connectionPort;

    try {
      connectionPort = Integer.parseInt(port);
    } catch (NumberFormatException nfe) {
      ErrorDialog.openError(getShell(), "Invalid Port", null, new Status(
          IStatus.ERROR,
          DartDebugUIPlugin.PLUGIN_ID,
          "\"" + port + "\" is an invalid port."));
      return;
    }

    IFile file = ResourceUtil.getFile(filePath);
    if (file == null) {
      ErrorDialog.openError(getShell(), "Invalid Application File", null, new Status(
          IStatus.ERROR,
          DartDebugUIPlugin.PLUGIN_ID,
          "\"" + filePath + "\" is an invalid resource path."));
      return;
    }

    connection.connection(host, connectionPort, file, usePubServe);

    super.okPressed();
  }

  private void createDialogUI(Composite parent) {
    GridLayoutFactory.fillDefaults().numColumns(2).margins(12, 6).applyTo(parent);

    Label label = new Label(parent, SWT.NONE);
    label.setText("Connect to:");

    exceptionsCombo = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
    exceptionsCombo.setItems(getConnectionLabels());
    exceptionsCombo.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        handleComboChanged();
      }
    });

    // spacer
    label = new Label(parent, SWT.NONE);

    Group group = new Group(parent, SWT.NONE);
    group.setText("Connection parameters");
    GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
    GridLayoutFactory.fillDefaults().numColumns(2).margins(12, 6).applyTo(group);

    label = new Label(group, SWT.NONE);
    label.setText("Host:");

    hostText = new Text(group, SWT.SINGLE | SWT.BORDER);
    GridDataFactory.fillDefaults().grab(true, false).applyTo(hostText);

    label = new Label(group, SWT.NONE);
    label.setText("Port:");

    portText = new Text(group, SWT.SINGLE | SWT.BORDER);
    GridDataFactory.fillDefaults().grab(true, false).applyTo(portText);

    label = new Label(group, SWT.NONE);
    label.setText("File:");

    fileText = new Text(group, SWT.SINGLE | SWT.BORDER);
    GridDataFactory.fillDefaults().grab(true, false).applyTo(fileText);

    label = new Label(parent, SWT.NONE);

    instructionsLabel = new Text(parent, SWT.WRAP | SWT.READ_ONLY);
    instructionsLabel.setBackground(parent.getBackground());
    GridDataFactory.fillDefaults().grab(true, false).hint(100, -1).applyTo(instructionsLabel);

    // spacer
    label = new Label(parent, SWT.NONE);

    label = new Label(parent, SWT.NONE);
    label = new Label(parent, SWT.NONE);

    pubGroup = new Group(parent, SWT.NONE);
    pubGroup.setText("Pub settings");
    GridDataFactory.fillDefaults().grab(true, false).applyTo(pubGroup);
    GridLayoutFactory.fillDefaults().margins(12, 6).applyTo(pubGroup);

    usePubServeButton = new Button(pubGroup, SWT.CHECK);
    usePubServeButton.setText("Using pub to serve the application");
    usePubServeButton.setSelection(true);
    GridDataFactory.fillDefaults().grab(true, false).applyTo(usePubServeButton);
    Label message = new Label(pubGroup, SWT.NONE);
    message.setText("(This information will be used to resolve breakpoints)");
    message.setFont(Fonts.getItalicFont(message.getFont()));

    try {
      exceptionsCombo.select(getDialogSettings().getInt("selected"));
    } catch (NumberFormatException nfe) {
      exceptionsCombo.select(0);
    }

    handleComboChanged();
  }

  private String[] getConnectionLabels() {
    String[] labels = new String[ConnectionType.values().length];

    for (int i = 0; i < labels.length; i++) {
      labels[i] = ConnectionType.values()[i].label;
    }

    return labels;
  }

  private ConnectionType getConnectionType() {
    return ConnectionType.values()[exceptionsCombo.getSelectionIndex()];
  }

  private IDialogSettings getDialogSettings() {
    final String sectionName = "remoteConnectionSettings";

    IDialogSettings settings = DartDebugUIPlugin.getDefault().getDialogSettings();

    if (settings.getSection(sectionName) == null) {
      IDialogSettings section = settings.addNewSection(sectionName);

      for (ConnectionType connection : ConnectionType.values()) {
        section.put(connection.name() + ".host", connection.hostDefault);
        section.put(connection.name() + ".port", connection.portDefault);
      }
    }

    return settings.getSection(sectionName);
  }

  private void handleComboChanged() {
    ConnectionType connection = getConnectionType();

    setMessage(connection.message);
    instructionsLabel.setText(connection.helpMessage);

    IDialogSettings settings = getDialogSettings();
    hostText.setText(notNull(settings.get(connection.name() + ".host")));
    portText.setText(notNull(settings.get(connection.name() + ".port")));
    fileText.setText(notNull(settings.get(connection.name() + ".file")));

    pubGroup.setVisible(connection == ConnectionType.CHROME);
  }

  private String notNull(String str) {
    return str == null ? "" : str;
  }

}
