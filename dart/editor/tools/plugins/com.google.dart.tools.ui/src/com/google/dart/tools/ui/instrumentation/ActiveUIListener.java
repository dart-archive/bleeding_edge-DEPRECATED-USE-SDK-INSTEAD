/*
 * Copyright (c) 2013, the Dart project authors.
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
package com.google.dart.tools.ui.instrumentation;

import org.eclipse.ui.IPageListener;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

/**
 * Track and log information about the active UI elements.
 */
public class ActiveUIListener implements IWindowListener, IPageListener, IPartListener2 {

  @Override
  public void pageActivated(IWorkbenchPage page) {
    page.addPartListener(this);
    log("page activated " + page);
  }

  @Override
  public void pageClosed(IWorkbenchPage page) {
    page.removePartListener(this);
    log("page closed " + page);
  }

  @Override
  public void pageOpened(IWorkbenchPage page) {
    log("page opened " + page);
  }

  @Override
  public void partActivated(IWorkbenchPartReference partRef) {
    log("part activated " + partRef);
  }

  @Override
  public void partBroughtToTop(IWorkbenchPartReference partRef) {
    log("part on top " + partRef);
  }

  @Override
  public void partClosed(IWorkbenchPartReference partRef) {
    log("part closed " + partRef);
  }

  @Override
  public void partDeactivated(IWorkbenchPartReference partRef) {
    log("part deactivated " + partRef);
  }

  @Override
  public void partHidden(IWorkbenchPartReference partRef) {
    log("part hidden " + partRef);
  }

  @Override
  public void partInputChanged(IWorkbenchPartReference partRef) {
    log("part input changed " + partRef);
  }

  @Override
  public void partOpened(IWorkbenchPartReference partRef) {
    log("part opened " + partRef);
  }

  @Override
  public void partVisible(IWorkbenchPartReference partRef) {
    log("part visible " + partRef);
  }

  /**
   * Start tracking the active UI elements.
   */
  public void start() {
    IWorkbench workbench = PlatformUI.getWorkbench();
    workbench.addWindowListener(this);
    IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
    if (window != null) {
      windowActivated(window);
      IWorkbenchPage page = window.getActivePage();
      if (page != null) {
        pageActivated(page);
        IWorkbenchPartReference partRef = page.getActivePartReference();
        if (partRef != null) {
          partActivated(partRef);
        }
      }
    }
  }

  @Override
  public void windowActivated(IWorkbenchWindow window) {
    window.addPageListener(this);
    log("window activated " + window);
  }

  @Override
  public void windowClosed(IWorkbenchWindow window) {
    window.removePageListener(this);
    log("window closed " + window);
  }

  @Override
  public void windowDeactivated(IWorkbenchWindow window) {
    window.removePageListener(this);
    log("window deactivated " + window);
  }

  @Override
  public void windowOpened(IWorkbenchWindow window) {
    log("window opened " + window);
  }

  private void log(String message) {
    System.out.println(message);
  }
}
