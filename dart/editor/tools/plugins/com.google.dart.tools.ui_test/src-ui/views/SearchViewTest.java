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
package views;

import com.google.common.base.Joiner;
import com.google.dart.tools.search.internal.ui.SearchView;
import com.google.dart.tools.ui.actions.DartEditorActionDefinitionIds;
import com.google.dart.tools.ui.internal.refactoring.RefactoringUtils;
import com.google.dart.ui.test.Condition;
import com.google.dart.ui.test.internal.runtime.ConditionHandler;
import com.google.dart.ui.test.model.Workbench;
import com.google.dart.ui.test.model.Workbench.View;
import com.google.dart.ui.test.util.UiContext2;

import static com.google.dart.ui.test.matchers.WidgetMatchers.withText;
import static com.google.dart.ui.test.util.UiContext2.runAction;
import static com.google.dart.ui.test.util.UiContext2.waitForActionEnabled;

import editor.AbstractDartEditorTabTest2;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

/**
 * Test for the "Search" view.
 */
public final class SearchViewTest extends AbstractDartEditorTabTest2 {
  private static final View VIEW = Workbench.View.SEARCH;
  private static final IProgressMonitor NULL_PM = new NullProgressMonitor();

  /**
   * Function to force formatter to put every string on separate line.
   */
  public static String[] formatLines(String... lines) {
    return lines;
  }

  private static String dumpTree(Tree tree) {
    return dumpTreeItems(UiContext2.getTreeItems(tree), "");
  }

  private static String dumpTreeItems(TreeItem items[], String prefix) {
    String result = "";
    for (TreeItem item : items) {
      result += prefix + UiContext2.getText(item) + "\n";
      result += dumpTreeItems(UiContext2.getTreeItems(item), prefix + "  ");
    }
    return result;
  }

  private final long startTime = System.currentTimeMillis();

  private SearchView instance;

  public void test_references() throws Exception {
    openTestEditor(
        "// filler filler filler filler filler filler filler filler filler filler",
        "library app;",
        "main() {",
        "  var test = 0;",
        "  test += 5;",
        "  print(test);",
        "}");
    findReferences("test = 0");
    waitForQueryExecuted();
    // prepare Tree
    Control viewControl = VIEW.getControl();
    Tree tree = UiContext2.findTree(viewControl);
    assertEquals(
        Joiner.on('\n').join(
            formatLines(
                "app (3 matches)",
                "  test.dart (3 matches)",
                "    main() (3 matches)",
                "        var test = 0;",
                "        test += 5;",
                "        print(test);",
                "")),
        dumpTree(tree));
    // double click the item, editor is activated
    TreeItem item = UiContext2.findWidget(tree, withText("test += 5;"));
    UiContext2.sendDefaultSelection(item);
    assertSame(testEditor, activePage.getActivePart());
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    VIEW.close();
  }

  @Override
  protected void tearDown() throws Exception {
    VIEW.close();
    super.tearDown();
  }

  private void findReferences(String pattern) throws Exception {
    RefactoringUtils.waitReadyForRefactoring(NULL_PM);
    selectOffset(pattern);
    // run action
    IAction action = getEditorAction(DartEditorActionDefinitionIds.SEARCH_REFERENCES_IN_WORKSPACE);
    waitForActionEnabled(action);
    runAction(action);
  }

  private void waitForQueryExecuted() {
    // prepare instance
    VIEW.waitForOpen();
    instance = (SearchView) VIEW.getInstance();
    // wait for query
    ConditionHandler.DEFAULT.waitFor(new Condition() {
      @Override
      public boolean test() {
        return instance.getLastQueryFinishTime() >= startTime;
      }
    });
  }
}
