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
package com.google.dart.tools.internal.corext.refactoring.changes;

import org.eclipse.core.runtime.Assert;
import org.eclipse.ltk.core.refactoring.CategorizedTextEditGroup;
import org.eclipse.ltk.core.refactoring.GroupCategorySet;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.ltk.core.refactoring.TextEditChangeGroup;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;

/**
 * A utility class to provide compatibility with the old text change API of adding text edits
 * directly and auto inserting them into the tree.
 * 
 * @coverage dart.editor.ui.refactoring.core
 */
public class TextChangeCompatibility {

  public static void addTextEdit(TextChange change, String name, TextEdit edit)
      throws MalformedTreeException {
    Assert.isNotNull(change);
    Assert.isNotNull(name);
    Assert.isNotNull(edit);
    TextEdit root = change.getEdit();
    if (root == null) {
      root = new MultiTextEdit();
      change.setEdit(root);
    }
    insert(root, edit);
    change.addTextEditGroup(new TextEditGroup(name, edit));
  }

  public static void addTextEdit(TextChange change, String name, TextEdit edit,
      GroupCategorySet groupCategories) throws MalformedTreeException {
    Assert.isNotNull(change);
    Assert.isNotNull(name);
    Assert.isNotNull(edit);
    TextEdit root = change.getEdit();
    if (root == null) {
      root = new MultiTextEdit();
      change.setEdit(root);
    }
    insert(root, edit);
    change.addTextEditChangeGroup(new TextEditChangeGroup(change, new CategorizedTextEditGroup(
        name, edit, groupCategories)));
  }

  public static void insert(TextEdit parent, TextEdit edit) throws MalformedTreeException {
    if (!parent.hasChildren()) {
      parent.addChild(edit);
      return;
    }
    TextEdit[] children = parent.getChildren();
    // First dive down to find the right parent.
    for (int i = 0; i < children.length; i++) {
      TextEdit child = children[i];
      if (covers(child, edit)) {
        insert(child, edit);
        return;
      }
    }
    // We have the right parent. Now check if some of the children have to
    // be moved under the new edit since it is covering it.
    int removed = 0;
    for (int i = 0; i < children.length; i++) {
      TextEdit child = children[i];
      if (covers(edit, child)) {
        parent.removeChild(i - removed++);
        edit.addChild(child);
      }
    }
    parent.addChild(edit);
  }

  private static boolean covers(TextEdit thisEdit, TextEdit otherEdit) {
    if (thisEdit.getLength() == 0) {
      return false;
    }

    int thisOffset = thisEdit.getOffset();
    int thisEnd = thisEdit.getExclusiveEnd();
    if (otherEdit.getLength() == 0) {
      int otherOffset = otherEdit.getOffset();
      return thisOffset < otherOffset && otherOffset < thisEnd;
    } else {
      int otherOffset = otherEdit.getOffset();
      int otherEnd = otherEdit.getExclusiveEnd();
      return thisOffset <= otherOffset && otherEnd <= thisEnd;
    }
  }

}
