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
package com.google.dart.tools.ui.test.model;

import com.google.dart.tools.ui.test.model.internal.views.ShowViewCommand;
import com.google.dart.tools.ui.test.model.internal.views.ViewExplorer;
import com.google.dart.tools.ui.test.model.internal.workbench.CommandException;

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.views.IViewDescriptor;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test model of the main DartEditor workbench.
 */
public class Workbench {

  public static class Editor {

    public static Editor WELCOME = new Editor("Welcome");
    private final String title;

    public Editor(String title) {
      this.title = title;
    }

    /**
     * Get the associated editor reference.
     */
    public IEditorReference getReference() {
      return getBot().editorByTitle(title).getReference();
    }
  }

  /**
   * Top level menus.
   */
  public static enum Menu {

    TOOLS("Tools");

    private final String name;

    Menu(String name) {
      this.name = name;
    }

    /**
     * Open this menu.
     */
    public void open() {
      getBot().menu(name);
    }

    /**
     * Select the given menu item in this menu.
     * 
     * @param item the menu item to select
     */
    public void select(String item) {
      getBot().menu(name).menu(item);
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

    public void close() {
      getBot().viewByTitle(name).close();
    }

    /**
     * Get the view name.
     */
    public String getName() {
      return name;
    }

    /**
     * Open this view (and enforce basic view sanity assertions).
     * 
     * @throws CommandException
     */
    public View open() throws CommandException {
      //TODO(pquitslund): consider using a programmatic approach if this is fragile
      //Menu.TOOLS.select(name);

      IViewDescriptor view = ViewExplorer.findView(name);

      new ShowViewCommand(view).run();

      //checkView();
      return this;
    }

    /**
     * Show this view. Before returning, assert that the view is active.
     */
    public View show() {
      getViewBot().show();
      assertTrue(getViewBot().isActive());
      return this;
    }

    //TODO(pquitslund): add validation
    @SuppressWarnings("unused")
    private void checkView() {
      SWTBotView view = getViewBot();
      assertNotNull(view);
      IViewReference viewRef = view.getReference();
      assertNotNull(viewRef);
      assertFalse(viewRef.isFastView());
      assertFalse(viewRef.isDirty());
      assertFalse(viewRef.getTitle().isEmpty());
      assertNotNull(viewRef.getTitleImage());
    }

    private SWTBotView getViewBot() {
      return getBot().viewByTitle(name);
    }

  }

  private static SWTWorkbenchBot bot;

  private static SWTWorkbenchBot getBot() {
    if (bot == null) {
      bot = new SWTWorkbenchBot();
    }
    return bot;
  }

}
