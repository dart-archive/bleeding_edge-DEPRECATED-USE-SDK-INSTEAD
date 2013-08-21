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

    UIInstrumentationBuilder instrumentation = UIInstrumentation.builder("ActiveUIListener");
    try {
      instrumentation.metric("Action", "pageActivated");
      instrumentation.record(page);
    } finally {
      instrumentation.log();
    }
  }

  @Override
  public void pageClosed(IWorkbenchPage page) {
    page.removePartListener(this);

    UIInstrumentationBuilder instrumentation = UIInstrumentation.builder("ActiveUIListener");
    try {
      instrumentation.metric("Action", "pageClosed");
      instrumentation.record(page);
    } finally {
      instrumentation.log();
    }
  }

  @Override
  public void pageOpened(IWorkbenchPage page) {

    UIInstrumentationBuilder instrumentation = UIInstrumentation.builder("ActiveUIListener");
    try {
      instrumentation.metric("Action", "pageOpened");
      instrumentation.record(page);
    } finally {
      instrumentation.log();
    }
  }

  @Override
  public void partActivated(IWorkbenchPartReference partRef) {
    UIInstrumentationBuilder instrumentation = UIInstrumentation.builder("ActiveUIListener");
    try {
      instrumentation.metric("Action", "partActivated");
      instrumentation.record(partRef);
    } finally {
      instrumentation.log();
    }
  }

  @Override
  public void partBroughtToTop(IWorkbenchPartReference partRef) {
    UIInstrumentationBuilder instrumentation = UIInstrumentation.builder("ActiveUIListener");
    try {
      instrumentation.metric("Action", "partBroughtToTop");
      instrumentation.record(partRef);
    } finally {
      instrumentation.log();
    }
  }

  @Override
  public void partClosed(IWorkbenchPartReference partRef) {
    UIInstrumentationBuilder instrumentation = UIInstrumentation.builder("ActiveUIListener");
    try {
      instrumentation.metric("Action", "partClosed");
      instrumentation.record(partRef);
    } finally {
      instrumentation.log();
    }
  }

  @Override
  public void partDeactivated(IWorkbenchPartReference partRef) {
    UIInstrumentationBuilder instrumentation = UIInstrumentation.builder("ActiveUIListener");
    try {
      instrumentation.metric("Action", "partDeactivated");
      instrumentation.record(partRef);
    } finally {
      instrumentation.log();
    }
  }

  @Override
  public void partHidden(IWorkbenchPartReference partRef) {
    UIInstrumentationBuilder instrumentation = UIInstrumentation.builder("ActiveUIListener");
    try {
      instrumentation.metric("Action", "partHidden");
      instrumentation.record(partRef);
    } finally {
      instrumentation.log();
    }
  }

  @Override
  public void partInputChanged(IWorkbenchPartReference partRef) {
    UIInstrumentationBuilder instrumentation = UIInstrumentation.builder("ActiveUIListener");
    try {
      instrumentation.metric("Action", "partInputChanged");
      instrumentation.record(partRef);
    } finally {
      instrumentation.log();
    }
  }

  @Override
  public void partOpened(IWorkbenchPartReference partRef) {
    UIInstrumentationBuilder instrumentation = UIInstrumentation.builder("ActiveUIListener");
    try {
      instrumentation.metric("Action", "partOpened");
      instrumentation.record(partRef);
    } finally {
      instrumentation.log();
    }
  }

  @Override
  public void partVisible(IWorkbenchPartReference partRef) {
    UIInstrumentationBuilder instrumentation = UIInstrumentation.builder("ActiveUIListener");
    try {
      instrumentation.metric("Action", "partVisible");
      instrumentation.record(partRef);
    } finally {
      instrumentation.log();
    }
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

    UIInstrumentationBuilder instrumentation = UIInstrumentation.builder("ActiveUIListener");
    try {
      instrumentation.metric("Action", "windowActivated");
      instrumentation.record(window);
    } finally {
      instrumentation.log();
    }
  }

  @Override
  public void windowClosed(IWorkbenchWindow window) {
    window.removePageListener(this);

    UIInstrumentationBuilder instrumentation = UIInstrumentation.builder("ActiveUIListener");
    try {
      instrumentation.metric("Action", "windowClosed");
      instrumentation.record(window);
    } finally {
      instrumentation.log();
    }
  }

  @Override
  public void windowDeactivated(IWorkbenchWindow window) {
    window.removePageListener(this);

    UIInstrumentationBuilder instrumentation = UIInstrumentation.builder("ActiveUIListener");
    try {
      instrumentation.metric("Action", "windowDeactivated");
      instrumentation.record(window);
    } finally {
      instrumentation.log();
    }
  }

  @Override
  public void windowOpened(IWorkbenchWindow window) {

    UIInstrumentationBuilder instrumentation = UIInstrumentation.builder("ActiveUIListener");
    try {
      instrumentation.metric("Action", "windowOpened");
      instrumentation.record(window);
    } finally {
      instrumentation.log();
    }
  }
}
