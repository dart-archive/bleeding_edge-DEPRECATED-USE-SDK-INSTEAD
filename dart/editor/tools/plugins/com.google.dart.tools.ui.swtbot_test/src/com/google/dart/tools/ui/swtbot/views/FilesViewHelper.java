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

import com.google.dart.tools.ui.test.model.Workbench;
import com.google.dart.tools.ui.test.model.Workbench.View;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;

import static org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory.widgetOfType;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 * Access methods for the "Files" view.
 */
public class FilesViewHelper {

  public static String SDK_TEXT = "Dart SDK";

  public static String CLOSE_FOLDER_TEXT = "Close Folder";
  public static String RUN_TEXT = "Run";

  private SWTBotTree tree;

  private SWTWorkbenchBot bot;

  public FilesViewHelper(SWTWorkbenchBot bot) {
    this.bot = bot;
    Workbench.View.FILES.show();

    //TODO(pquitslund): push in model view
    SWTBotView view = bot.viewByTitle(View.FILES.getName());
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

  /**
   * This method performs a specified menu context click on a top level directory in the Files view.
   * 
   * @param projectLabel the name of the top level directory in the Files view that will get this
   *          context menu click
   * @param commandLabel the name of the action on the context menu for the specified project, an
   *          example is {@link #CLOSE_FOLDER_TEXT}
   * @return <code>true</code> if the click action was successful
   */
  public boolean contextClick(String projectLabel, String commandLabel) {
    tree.setFocus();
    for (SWTBotTreeItem treeItem : getItems()) {
      if (treeItem.getText().equals(projectLabel)) {
        treeItem.setFocus();
        return treeItem.contextMenu(commandLabel).click() != null;
      }
    }
    return false;
  }

  /**
   * Similar to {@link #contextClick(String, String)}, except this method allows you to specify a
   * file or folder within the specified top level directory.
   * 
   * @param projectLabel the name of the top level directory in the Files view that will get this
   *          context menu click
   * @param filePath some path within the specified top level directory, such as
   *          <code>folder1 / folder2 / file.txt</code>
   * @param commandLabel the name of the action on the context menu for the specified project, an
   *          example is {@link #RUN_TEXT}
   * @return <code>true</code> if the click action was successful
   */
  public boolean contextClick(String projectLabel, String filePath, String commandLabel) {
    tree.setFocus();
    for (SWTBotTreeItem treeItem : getItems()) {
      if (treeItem.getText().equals(projectLabel)) {
        treeItem.expand();
        String[] filePaths = filePath.split(java.io.File.separator + "{1}?");
        SWTBotTreeItem itemToClick = recursivelyFind(filePaths, treeItem);
        itemToClick.setFocus();
        return itemToClick.contextMenu(commandLabel).click() != null;
      }
    }
    return false;
  }

  /**
   * This method calls {@link #contextClick(String, String)} with the specified top level directory
   * name.
   */
  public boolean contextClick_removeFromEditor(String projectLabel) {
    int beforeCount = getItems().length;
    // Click the Remove action, if successful, wait for dialog
    if (contextClick(projectLabel, CLOSE_FOLDER_TEXT)) {
      // assert that the item disappears from the view within 200 ms
      bot.sleep(200);
      assertEquals("After removing " + projectLabel
          + ", expected one less item in the Files view, but instead there are "
          + getItems().length + " items.", beforeCount - 1, getItems().length);
      return true;
    }
    assertEquals(beforeCount, getItems().length);
    return false;
  }

  /**
   * Returns an array of {@link SWTBotTreeItem}s.
   */
  public SWTBotTreeItem[] getItems() {
    return tree.getAllItems();
  }

  /**
   * Used for messages only, don't make assertions on the returned content.
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
      itemCollection.add(treeItemLabel);
    }
    return itemCollection;
  }

  private SWTBotTreeItem recursivelyFind(String[] filePaths, SWTBotTreeItem parent) {
    assertNotNull(parent);
    parent.expand();
    assertTrue(parent.isExpanded());
    assertTrue(filePaths.length > 0);
    SWTBotTreeItem[] items = parent.getItems();
    for (SWTBotTreeItem childTreeItem : items) {
      if (childTreeItem.getText().startsWith(filePaths[0])) {
        if (filePaths.length == 1) {
          //base case:
          return childTreeItem;
        } else {
          // recursive case:
          return recursivelyFind(Arrays.copyOfRange(filePaths, 1, filePaths.length), childTreeItem);
        }
      }
    }
    fail("Could not find the tree element " + filePaths[0] + " under " + parent.getText()
        + " in the Files view.");
    return null;
  }

}
