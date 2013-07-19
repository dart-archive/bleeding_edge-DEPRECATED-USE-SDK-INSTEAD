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
package com.google.dart.ui.test.model;

import com.google.dart.tools.internal.corext.refactoring.util.ExecutionUtils;
import com.google.dart.tools.internal.corext.refactoring.util.RunnableEx;
import com.google.dart.tools.internal.corext.refactoring.util.RunnableObjectEx;
import com.google.dart.ui.test.Condition;
import com.google.dart.ui.test.WaitTimedOutException;
import com.google.dart.ui.test.internal.runtime.ConditionHandler;
import com.google.dart.ui.test.model.internal.views.CloseViewCommand;
import com.google.dart.ui.test.model.internal.views.ShowViewCommand;
import com.google.dart.ui.test.model.internal.views.ViewExplorer;
import com.google.dart.ui.test.model.internal.views.ViewFinder;
import com.google.dart.ui.test.model.internal.workbench.CommandException;

import org.eclipse.core.resources.IFile;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.views.IViewDescriptor;

/**
 * Test model of the main DartEditor workbench.
 */
public class Workbench {

  public static class Editor {

    public static Editor WELCOME = new Editor("Welcome");
    @SuppressWarnings("unused")
    private final String title;

    public Editor(String title) {
      this.title = title;
    }

    /**
     * Get the associated editor reference.
     */
    public IEditorReference getReference() {
      //TODO (pquitslund): implement
      return null;
    }
  }

  /**
   * Views.
   */
  public static enum View {

    APPS("Apps"),
    CALLERS("Callers"),
    CONSOLE("Console"),
    DEBUG("Debugger"),
    FILES("Files"),
    OUTLINE("Outline"),
    PROBLEMS("Problems"),
    SEARCH("Search"),
    WELCOME("Welcome");

    private final String name;

    View(String name) {
      this.name = name;
    }

    public void close() throws CommandException {
      new CloseViewCommand(getDescriptor()).run();
    }

    /**
     * @return the {@link Control} of the first instance of this view, may be {@code null}.
     */
    public Control getControl() {
      IViewPart view = getInstance();
      if (view == null) {
        return null;
      }
      return ViewFinder.getControl(view);
    }

    /**
     * @return the first instance of this view, may be {@code null}.
     */
    public IViewPart getInstance() {
      IViewReference reference = ViewFinder.findNamed(name);
      if (reference == null) {
        return null;
      }
      return reference.getView(false);
    }

    /**
     * Get the view name.
     */
    public String getName() {
      return name;
    }

    public boolean isOpen() {
      return ViewExplorer.findView(name) != null;
    }

    /**
     * Ensure that this view is open.
     */
    public View open() throws CommandException {
      return show();
    }

    /**
     * Show this view. Before returning, assert that the view is active.
     */
    public View show() throws CommandException {
      IViewDescriptor view = getDescriptor();
      new ShowViewCommand(view).run();
      return this;
    }

    /**
     * Waits for the view to open and returns its {@link Control}.
     */
    public void waitForOpen() throws WaitTimedOutException {
      ConditionHandler.DEFAULT.waitFor(new Condition() {
        @Override
        public boolean test() {
          return isOpen();
        }
      });
    }

    private IViewDescriptor getDescriptor() throws CommandException {
      IViewDescriptor view = ViewExplorer.findView(name);
      if (view == null) {
        throw new CommandException("no view descriptor found for: " + name);
      }
      return view;
    }

  }

  public static void closeAllEditors() {
    ExecutionUtils.runRethrowUI(new RunnableEx() {
      @Override
      public void run() throws Exception {
        getActivePage().closeAllEditors(false);
      }
    });
  }

  public static IEditorPart openEditor(final IFile file) {
    return ExecutionUtils.runObjectUI(new RunnableObjectEx<IEditorPart>() {
      @Override
      public IEditorPart runObject() throws Exception {
        return IDE.openEditor(getActivePage(), file);
      }
    });
  }

  private static IWorkbenchPage getActivePage() {
    return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
  }

}
