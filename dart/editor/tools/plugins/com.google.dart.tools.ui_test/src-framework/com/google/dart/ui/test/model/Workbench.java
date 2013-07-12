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

import com.google.dart.ui.test.model.internal.views.CloseViewCommand;
import com.google.dart.ui.test.model.internal.views.ShowViewCommand;
import com.google.dart.ui.test.model.internal.views.ViewExplorer;
import com.google.dart.ui.test.model.internal.workbench.CommandException;

import org.eclipse.ui.IEditorReference;
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
    //SEARCH("Search"),
    WELCOME("Welcome");

    private final String name;

    View(String name) {
      this.name = name;
    }

    public void close() throws CommandException {
      new CloseViewCommand(getDescriptor()).run();
    }

    /**
     * Get the view name.
     */
    public String getName() {
      return name;
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

    private IViewDescriptor getDescriptor() throws CommandException {
      IViewDescriptor view = ViewExplorer.findView(name);
      if (view == null) {
        throw new CommandException("no view descriptor found for: " + name);
      }
      return view;
    }

  }

}
