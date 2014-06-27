/*
 * Copyright 2014 Dart project authors.
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
package com.google.dart.tools.tests.swtbot.conditions;

import org.eclipse.swtbot.swt.finder.utils.internal.Assert;
import org.eclipse.swtbot.swt.finder.waits.DefaultCondition;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;

public class TreeHasSomeRows extends DefaultCondition {

  /**
   * The row count.
   */
  private final int rowCount;
  /**
   * The table (SWTBotTree) instance to check.
   */
  private final SWTBotTree tree;

  /**
   * Constructs an instance of the condition for the given tree. The row count is used to know how
   * many rows it needs to satisfy the condition.
   * 
   * @param tree the tree
   * @param rowCount the number of rows needed.
   * @throws NullPointerException Thrown if the table is <code>null</code>.
   * @throws IllegalArgumentException Thrown if the row count is less then 1.
   */
  public TreeHasSomeRows(SWTBotTree tree, int rowCount) {
    Assert.isNotNull(tree, "The tree can not be null"); //$NON-NLS-1$
    Assert.isLegal(rowCount >= 0, "The node count must be greater than zero (0)"); //$NON-NLS-1$
    this.tree = tree;
    this.rowCount = rowCount;
  }

  /**
   * Gets the failure message if the test is not satisfied.
   * 
   * @see org.eclipse.swtbot.swt.finder.waits.ICondition#getFailureMessage()
   * @return The failure message.
   */
  @Override
  public String getFailureMessage() {
    return "Timed out waiting for " + tree + " to contain " + rowCount + " rows."; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
  }

  /**
   * Performs the check to see if the condition is satisfied.
   * 
   * @see org.eclipse.swtbot.swt.finder.waits.ICondition#test()
   * @return <code>true</code> if the condition node count is at not less than the number of nodes
   *         in the tree. Otherwise <code>false</code> is returned.
   */
  @Override
  public boolean test() {
    return tree.rowCount() >= rowCount;
  }
}
