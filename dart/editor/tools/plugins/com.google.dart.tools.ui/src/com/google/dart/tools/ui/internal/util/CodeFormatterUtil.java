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
package com.google.dart.tools.ui.internal.util;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.formatter.DefaultCodeFormatterConstants;
import com.google.dart.tools.core.model.DartProject;
import com.google.dart.tools.ui.DartToolsPlugin;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.DefaultPositionUpdater;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.Position;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;

public class CodeFormatterUtil {

  /**
   * Creates a string that represents the given number of indentation units. The returned string can
   * contain tabs and/or spaces depending on the core formatter preferences.
   * 
   * @param indentationUnits the number of indentation units to generate
   * @return the indent string
   */
  public static String createIndentString(int indentationUnits) {
    StringBuffer s = new StringBuffer();
    for (int i = 0; i < indentationUnits; i++) {
      s.append("  ");
    }
    return s.toString();
  }

  /**
   * Evaluates the edit on the given string.
   * 
   * @throws IllegalArgumentException If the positions are not inside the string, a
   *           IllegalArgumentException is thrown.
   */
  public static String evaluateFormatterEdit(String string, TextEdit edit, Position[] positions) {
    try {
      Document doc = createDocument(string, positions);
      edit.apply(doc, 0);
      if (positions != null) {
        for (int i = 0; i < positions.length; i++) {
          Assert.isTrue(!positions[i].isDeleted, "Position got deleted"); //$NON-NLS-1$
        }
      }
      return doc.get();
    } catch (BadLocationException e) {
      DartToolsPlugin.log(e); // bug in the formatter
      Assert.isTrue(false, "Formatter created edits with wrong positions: " + e.getMessage()); //$NON-NLS-1$
    }
    return null;
  }

  /**
   * Returns the current indent width.
   * 
   * @param project the project where the source is used or <code>null</code> if the project is
   *          unknown and the workspace default should be used
   * @return the indent width
   */
  public static int getIndentWidth(DartProject project) {
    String key;
    if (DefaultCodeFormatterConstants.MIXED.equals(getCoreOption(
        project,
        DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR))) {
      key = DefaultCodeFormatterConstants.FORMATTER_INDENTATION_SIZE;
    } else {
      key = DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE;
    }

    return getCoreOption(project, key, 2);
  }

  /**
   * Gets the current tab width.
   * 
   * @return The tab width
   */
  public static int getTabWidth() {
    IPreferenceStore preferenceStore = DartToolsPlugin.getDefault().getPreferenceStore();
    if (preferenceStore != null) {
      int width = preferenceStore.getInt(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_TAB_WIDTH);
      if (width != 0) {
        return width;
      }
    }
    return 2;
//    /*
//     * If the tab-char is SPACE, FORMATTER_INDENTATION_SIZE is not used by the core formatter. We
//     * piggy back the visual tab length setting in that preference in that case.
//     */
//    String key;
//    if (JavaScriptCore.SPACE.equals(getCoreOption(
//        project,
//        DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR))) {
//      key = DefaultCodeFormatterConstants.FORMATTER_INDENTATION_SIZE;
//    } else {
//      key = DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE;
//    }
//
//    return getCoreOption(project, key, 2);
  }

  private static Document createDocument(String string, Position[] positions)
      throws IllegalArgumentException {
    Document doc = new Document(string);
    try {
      if (positions != null) {
        final String POS_CATEGORY = "myCategory"; //$NON-NLS-1$

        doc.addPositionCategory(POS_CATEGORY);
        doc.addPositionUpdater(new DefaultPositionUpdater(POS_CATEGORY) {
          @Override
          protected boolean notDeleted() {
            if (fOffset < fPosition.offset
                && (fPosition.offset + fPosition.length < fOffset + fLength)) {
              fPosition.offset = fOffset + fLength; // deleted positions: set to
// end of remove
              return false;
            }
            return true;
          }
        });
        for (int i = 0; i < positions.length; i++) {
          try {
            doc.addPosition(POS_CATEGORY, positions[i]);
          } catch (BadLocationException e) {
            throw new IllegalArgumentException(
                "Position outside of string. offset: " + positions[i].offset + ", length: " + positions[i].length + ", string size: " + string.length()); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
          }
        }
      }
    } catch (BadPositionCategoryException cannotHappen) {
      // can not happen: category is correctly set up
    }
    return doc;
  }

  /**
   * Returns the possibly <code>project</code>-specific core preference defined under
   * <code>key</code>.
   * 
   * @param project the project to get the preference from, or <code>null</code> to get the global
   *          preference
   * @param key the key of the preference
   * @return the value of the preference
   */
  private static String getCoreOption(DartProject project, String key) {
    if (project == null) {
      return DartCore.getOption(key);
    }
    return project.getOption(key, true);
  }

  /**
   * Returns the possibly <code>project</code>-specific core preference defined under
   * <code>key</code>, or <code>def</code> if the value is not a integer.
   * 
   * @param project the project to get the preference from, or <code>null</code> to get the global
   *          preference
   * @param key the key of the preference
   * @param def the default value
   * @return the value of the preference
   */
  private static int getCoreOption(DartProject project, String key, int def) {
    try {
      return Integer.parseInt(getCoreOption(project, key));
    } catch (NumberFormatException e) {
      return def;
    }
  }

}
