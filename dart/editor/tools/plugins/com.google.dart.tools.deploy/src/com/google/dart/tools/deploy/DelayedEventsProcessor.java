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
package com.google.dart.tools.deploy;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

import java.util.ArrayList;

/**
 * Helper class used to process delayed events. Events currently supported:
 * <ul>
 * <li>SWT.OpenDocument</li>
 * </ul>
 */
public class DelayedEventsProcessor implements Listener {

  private ArrayList<String> filesToOpen = new ArrayList<String>(1);

  /**
   * Constructor.
   * 
   * @param display display used as a source of event
   */
  public DelayedEventsProcessor(Display display) {
    display.addListener(SWT.OpenDocument, this);
  }

  /**
   * Process delayed events.
   * 
   * @param display display associated with the workbench
   */
  public void catchUp(Display display) {
    if (filesToOpen.isEmpty()) {
      return;
    }

    String[] filePaths = new String[filesToOpen.size()];
    filesToOpen.toArray(filePaths);
    filesToOpen.clear();
    for (int i = 0; i < filePaths.length; i++) {
      openFile(display, filePaths[i]);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
   */
  @Override
  public void handleEvent(Event event) {
    final String path = event.text;
    if (path == null) {
      return;
    }
    filesToOpen.add(path);
  }

  private void openFile(Display display, final String path) {
    display.asyncExec(new Runnable() {
      @Override
      public void run() {
        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (window == null) {
          return;
        }
        IFileStore fileStore = EFS.getLocalFileSystem().getStore(new Path(path));
        IFileInfo fetchInfo = fileStore.fetchInfo();
        if (!fetchInfo.isDirectory() && fetchInfo.exists()) {
          IWorkbenchPage page = window.getActivePage();
          if (page == null) {

            // TODO: Add a more robust way of displaying error messages at this point. Maybe a read
            // only editor page like the Welcome editor so that users can get to the information.

            String msg = NLS.bind("The file ''{0}'' could not be opened.", path);
            MessageDialog.open(MessageDialog.ERROR, window.getShell(), "Open File", msg, SWT.SHEET);
          }
          try {
            IDE.openInternalEditorOnFileStore(page, fileStore);
            IWorkbenchPartReference partRef = page.getActivePartReference();
            if (partRef != null) {
              if (!page.isPageZoomed()) {
                page.toggleZoom(partRef);
              }
            }
            Shell shell = window.getShell();
            if (shell != null) {
              if (shell.getMinimized()) {
                shell.setMinimized(false);
              }
              shell.forceActive();
            } else {
              throw new PartInitException("Shell could not be found");
            }
          } catch (PartInitException e) {
            String msg = NLS.bind("The file ''{0}'' could not be opened.\nSee log for details.",
                fileStore.getName());
            CoreException eLog = new PartInitException(e.getMessage());
            Activator.getDefault().getLog().log(
                new Status(IStatus.ERROR, Activator.PLUGIN_ID, msg, eLog));
            MessageDialog.open(MessageDialog.ERROR, window.getShell(), "Open File", msg, SWT.SHEET);
          }
        } else {
          String msg = NLS.bind("The file ''{0}'' could not be found.", path);
          MessageDialog.open(MessageDialog.ERROR, window.getShell(), "Open File", msg, SWT.SHEET);
        }
      }
    });
  }

}
