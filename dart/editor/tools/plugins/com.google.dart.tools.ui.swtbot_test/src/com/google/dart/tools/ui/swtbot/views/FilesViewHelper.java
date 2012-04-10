/*
 * Copyright 2012 Dart project authors.
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
package com.google.dart.tools.ui.swtbot.views;

import com.google.dart.tools.ui.swtbot.DartEditorUiTest;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotButton;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;

import static org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory.widgetOfType;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Access methods for the "Files" view.
 */
public class FilesViewHelper {

  public static String SDK_TEXT = "SDK Libraries";

  public static String REMOVE_FROM_EDITOR = "Remove from Editor";

  private SWTBotTree tree;

  private SWTWorkbenchBot bot;

  public FilesViewHelper(SWTWorkbenchBot bot) {
    this.bot = bot;
    SWTBotView view = bot.viewByTitle(DartEditorUiTest.FILES_VIEW_NAME);
    view.show();
    assertTrue(view.isActive());
    Composite composite = (Composite) view.getWidget();
    Tree filesTree = bot.widget(widgetOfType(Tree.class), composite);
    tree = new SWTBotTree(filesTree);
  }

  public void assertTreeItemCount(int expectedItemCount) {
    int actualItemCount = getItems().length;
    assertEquals("Expected " + expectedItemCount + ", but found " + actualItemCount
        + " items in the Files view: " + getItemsToString(), expectedItemCount, actualItemCount);
  }

  public void assertTreeItemsEqual(String... items) {
    assertTreeItemCount(items.length);
    Collection<String> itemCollection = getItemsInStringCollection();
    for (String item : items) {
      if (!itemCollection.contains(item)) {
        fail("The item \"" + item + "\" was not found in the Files view.");
      }
    }
  }

  public boolean contextClick(String projectLabel, String commandLabel) {
    for (SWTBotTreeItem treeItem : getItems()) {
      if (treeItem.getText().equals(projectLabel)) {
        return treeItem.contextMenu(commandLabel).click() != null;
      }
    }
    return false;
  }

  public boolean contextClick_removeFromEditor(String projectLabel) {
    int beforeCount = getItems().length;
    // Click the Remove action, if successful, wait for dialog
    if (contextClick(projectLabel, REMOVE_FROM_EDITOR)) {
      SWTBotShell shell = bot.activeShell();
      shell.activate();

      // Click OK in dialog that appears
      SWTBotButton okButton = bot.button("OK");
      okButton.click();
      assertEquals("After removing " + projectLabel
          + ", expected one less item in the Files view, but instead there are "
          + getItems().length + " items.", beforeCount - 1, getItems().length);
      return true;
    }
    assertEquals(beforeCount, getItems().length);
    return false;
  }

  public SWTBotTreeItem[] getItems() {
    return tree.getAllItems();
  }

  /**
   * Used for messages only, don't make assertions on the returned content.
   * 
   * @return
   */
  public String getItemsToString() {
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("[");
    for (SWTBotTreeItem treeItem : getItems()) {
      stringBuilder.append(treeItem.getText());
      stringBuilder.append(", ");
    }
    stringBuilder.append("]");
    return stringBuilder.toString();
  }

  private Collection<String> getItemsInStringCollection() {
    Collection<String> itemCollection = new ArrayList<String>(getItems().length);
    for (SWTBotTreeItem treeItem : getItems()) {
      assertNotNull(treeItem);
      String treeItemLabel = treeItem.getText();
      assertNotNull(treeItem);
      assertFalse(treeItemLabel.isEmpty());
      itemCollection.add(treeItemLabel);;
    }
    return itemCollection;
  }
}
