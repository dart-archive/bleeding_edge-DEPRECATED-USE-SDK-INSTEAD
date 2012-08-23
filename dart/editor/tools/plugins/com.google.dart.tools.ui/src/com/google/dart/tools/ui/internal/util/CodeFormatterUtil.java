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

import com.google.dart.compiler.ast.DartNode;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.formatter.CodeFormatter;
import com.google.dart.tools.core.formatter.DefaultCodeFormatterConstants;
import com.google.dart.tools.core.internal.formatter.DartCodeFormatter;
import com.google.dart.tools.core.model.DartProject;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.DartX;
import com.google.dart.tools.ui.text.editor.tmp.JavaScriptCore;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.DefaultPositionUpdater;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.Position;
import org.eclipse.text.edits.DeleteEdit;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

import java.util.HashMap;
import java.util.Map;

public class CodeFormatterUtil {

  private static final int FORMAT_NEW = 0;
  private static final int FORMAT_EXISTING = 1;

  /**
   * Creates a string that represents the given number of indentation units. The returned string can
   * contain tabs and/or spaces depending on the core formatter preferences.
   * 
   * @param indentationUnits the number of indentation units to generate
   * @param project the project from which to get the formatter settings, <code>null</code> if the
   *          workspace default should be used
   * @return the indent string
   */
  public static String createIndentString(int indentationUnits, DartProject project) {
    Map<String, String> options = project != null ? project.getOptions(true)
        : DartCore.getOptions();
    return createCodeFormatter(options, FORMAT_NEW).createIndentationString(indentationUnits);
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
   * Old API. Consider to use format2 (TextEdit)
   */
//  public static String format(DartNode node, String string,
//      int indentationLevel, int[] positions, String lineSeparator, Map options) {
//
//    TextEdit edit = format2(node, string, indentationLevel, lineSeparator,
//        options);
//    if (edit == null) {
//      DartToolsPlugin.logErrorMessage("formatter failed to format (no edit returned). Will use unformatted text instead. node: " + node.getNodeType() + ", string: " + string); //$NON-NLS-1$ //$NON-NLS-2$
//      return string;
//    }
//    return getOldAPICompatibleResult(string, edit, indentationLevel, positions,
//        lineSeparator, options);
//  }

  /**
   * Old API. Consider to use format2 (TextEdit)
   */
  public static String format(int kind, String string, int offset, int length,
      int indentationLevel, int[] positions, String lineSeparator, Map<String, String> options) {
    TextEdit edit = format2(kind, string, offset, length, indentationLevel, lineSeparator, options);
    if (edit == null) {
      //DartToolsPlugin.logErrorMessage("formatter failed to format (no edit returned). Will use unformatted text instead. kind: " + kind + ", string: " + string); //$NON-NLS-1$ //$NON-NLS-2$
      return string.substring(offset, offset + length);
    }
    String formatted = getOldAPICompatibleResult(
        string,
        edit,
        indentationLevel,
        positions,
        lineSeparator,
        options);
    return formatted.substring(offset, formatted.length() - (string.length() - (offset + length)));
  }

  public static String format(int kind, String string, int indentationLevel, int[] positions,
      String lineSeparator, DartProject project) {
    Map<String, String> options = project != null ? project.getOptions(true) : null;
    return format(
        kind,
        string,
        0,
        string.length(),
        indentationLevel,
        positions,
        lineSeparator,
        options);
  }

  // transition code

  /**
   * Old API. Consider to use format2 (TextEdit)
   */
  public static String format(int kind, String string, int indentationLevel, int[] positions,
      String lineSeparator, Map<String, String> options) {
    return format(
        kind,
        string,
        0,
        string.length(),
        indentationLevel,
        positions,
        lineSeparator,
        options);
  }

  /**
   * Creates edits that describe how to format the given string. Returns <code>null</code> if the
   * code could not be formatted for the given kind.
   * 
   * @throws IllegalArgumentException If the offset and length are not inside the string, a
   *           IllegalArgumentException is thrown.
   */
  public static TextEdit format2(DartNode node, String str, int indentationLevel,
      String lineSeparator, Map<String, String> options) {
    int code = 0;;
    String prefix = ""; //$NON-NLS-1$
    String suffix = ""; //$NON-NLS-1$
    DartX.todo();
//    if (node instanceof DartStatement) {
//      code = CodeFormatter.K_STATEMENTS;
//      if (node.getNodeType() == DartNode.SWITCH_CASE) {
//        prefix = "switch(1) {"; //$NON-NLS-1$
//        suffix = "}"; //$NON-NLS-1$
//        code = CodeFormatter.K_STATEMENTS;
//      }
//    } else if (node instanceof DartExpression
//        && node.getNodeType() != DartNode.VARIABLE_DECLARATION_EXPRESSION) {
//      code = CodeFormatter.K_EXPRESSION;
//    } else if (node instanceof DartBlock) {
//      code = CodeFormatter.K_CLASS_BODY_DECLARATIONS;
//    } else {
//      switch (node.getNodeType()) {
//        case DartNode.ARRAY_TYPE:
//        case DartNode.PRIMITIVE_TYPE:
//        case DartNode.QUALIFIED_TYPE:
//        case DartNode.SIMPLE_TYPE:
//          suffix = " x;"; //$NON-NLS-1$
//          code = CodeFormatter.K_CLASS_BODY_DECLARATIONS;
//          break;
//        case DartNode.JAVASCRIPT_UNIT:
//          code = CodeFormatter.K_JAVASCRIPT_UNIT;
//          break;
//        case DartNode.VARIABLE_DECLARATION_EXPRESSION:
//        case DartNode.SINGLE_VARIABLE_DECLARATION:
//          suffix = ";"; //$NON-NLS-1$
//          code = CodeFormatter.K_STATEMENTS;
//          break;
//        case DartNode.VARIABLE_DECLARATION_FRAGMENT:
//          prefix = "A "; //$NON-NLS-1$
//          suffix = ";"; //$NON-NLS-1$
//          code = CodeFormatter.K_STATEMENTS;
//          break;
//        case DartNode.PACKAGE_DECLARATION:
//        case DartNode.IMPORT_DECLARATION:
//          suffix = "\nclass A {}"; //$NON-NLS-1$
//          code = CodeFormatter.K_JAVASCRIPT_UNIT;
//          break;
//        case DartNode.JSDOC:
//          suffix = "void foo();"; //$NON-NLS-1$
//          code = CodeFormatter.K_CLASS_BODY_DECLARATIONS;
//          break;
//        case DartNode.CATCH_CLAUSE:
//          prefix = "try {}"; //$NON-NLS-1$
//          code = CodeFormatter.K_STATEMENTS;
//          break;
//        case DartNode.ANONYMOUS_CLASS_DECLARATION:
//          prefix = "new A()"; //$NON-NLS-1$
//          suffix = ";"; //$NON-NLS-1$
//          code = CodeFormatter.K_STATEMENTS;
//          break;
//        case DartNode.MODIFIER:
//          suffix = " class x {}"; //$NON-NLS-1$
//          code = CodeFormatter.K_JAVASCRIPT_UNIT;
//          break;
//        case DartNode.MEMBER_REF:
//        case DartNode.FUNCTION_REF:
//        case DartNode.FUNCTION_REF_PARAMETER:
//        case DartNode.TAG_ELEMENT:
//        case DartNode.TEXT_ELEMENT:
//          // Javadoc formatting not yet supported:
//          return null;
//        default:
//          //Assert.isTrue(false, "Node type not covered: " + node.getClass().getName()); //$NON-NLS-1$
//          return null;
//      }
//    }
//
    String concatStr = prefix + str + suffix;
    TextEdit edit = createCodeFormatter(options, FORMAT_NEW).format(
        code,
        concatStr,
        prefix.length(),
        str.length(),
        indentationLevel,
        lineSeparator);
    if (prefix.length() > 0) {
      edit = shifEdit(edit, prefix.length());
    }
    return edit;
  }

  /**
   * Creates edits that describe how to format the given string. Returns <code>null</code> if the
   * code could not be formatted for the given kind.
   * 
   * @throws IllegalArgumentException If the offset and length are not inside the string, a
   *           IllegalArgumentException is thrown.
   */
  public static TextEdit format2(int kind, String string, int offset, int length,
      int indentationLevel, String lineSeparator, Map<String, String> options) {
    if (offset < 0 || length < 0 || offset + length > string.length()) {
      throw new IllegalArgumentException(
          "offset or length outside of string. offset: " + offset + ", length: " + length + ", string size: " + string.length()); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
    }
    return createCodeFormatter(options, FORMAT_NEW).format(
        kind,
        string,
        offset,
        length,
        indentationLevel,
        lineSeparator);
  }

  public static TextEdit format2(int kind, String string, int indentationLevel,
      String lineSeparator, Map<String, String> options) {
    return format2(kind, string, 0, string.length(), indentationLevel, lineSeparator, options);
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
   * @param project The project where the source is used, used for project specific options or
   *          <code>null</code> if the project is unknown and the workspace default should be used
   * @return The tab width
   */
  public static int getTabWidth(DartProject project) {
    /*
     * If the tab-char is SPACE, FORMATTER_INDENTATION_SIZE is not used by the core formatter. We
     * piggy back the visual tab length setting in that preference in that case.
     */
    String key;
    if (JavaScriptCore.SPACE.equals(getCoreOption(
        project,
        DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR))) {
      key = DefaultCodeFormatterConstants.FORMATTER_INDENTATION_SIZE;
    } else {
      key = DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE;
    }

    return getCoreOption(project, key, 2);
  }

  public static TextEdit reformat(int kind, String string, int offset, int length,
      int indentationLevel, String lineSeparator, Map<String, String> options) {
    if (offset < 0 || length < 0 || offset + length > string.length()) {
      throw new IllegalArgumentException(
          "offset or length outside of string. offset: " + offset + ", length: " + length + ", string size: " + string.length()); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
    }
    return createCodeFormatter(options, FORMAT_EXISTING).format(
        kind,
        string,
        offset,
        length,
        indentationLevel,
        lineSeparator);
  }

  public static TextEdit reformat(int kind, String string, int indentationLevel,
      String lineSeparator, Map<String, String> options) {
    return reformat(kind, string, 0, string.length(), indentationLevel, lineSeparator, options);
  }

  private static CodeFormatter createCodeFormatter(Map<String, String> options, int mode) {
    if (options == null) {
      options = DartCore.getOptions();
    }
    Map<String, String> currentOptions = new HashMap<String, String>(options);
    if (mode == FORMAT_NEW) {
      // disable the option for not indenting comments starting on first column
      currentOptions.put(
          DefaultCodeFormatterConstants.FORMATTER_NEVER_INDENT_BLOCK_COMMENTS_ON_FIRST_COLUMN,
          DefaultCodeFormatterConstants.FALSE);
      currentOptions.put(
          DefaultCodeFormatterConstants.FORMATTER_NEVER_INDENT_LINE_COMMENTS_ON_FIRST_COLUMN,
          DefaultCodeFormatterConstants.FALSE);
    }
//    return new DefaultCodeFormatter(currentOptions);
    return new DartCodeFormatter(currentOptions);
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

  private static String getOldAPICompatibleResult(String string, TextEdit edit,
      int indentationLevel, int[] positions, String lineSeparator, Map<String, String> options) {
    Position[] p = null;

    if (positions != null) {
      p = new Position[positions.length];
      for (int i = 0; i < positions.length; i++) {
        p[i] = new Position(positions[i], 0);
      }
    }
    String res = evaluateFormatterEdit(string, edit, p);

    if (positions != null) {
      for (int i = 0; i < positions.length; i++) {
        Position curr = p[i];
        positions[i] = curr.getOffset();
      }
    }
    return res;
  }

  private static TextEdit shifEdit(TextEdit oldEdit, int diff) {
    TextEdit newEdit;
    if (oldEdit instanceof ReplaceEdit) {
      ReplaceEdit edit = (ReplaceEdit) oldEdit;
      newEdit = new ReplaceEdit(edit.getOffset() - diff, edit.getLength(), edit.getText());
    } else if (oldEdit instanceof InsertEdit) {
      InsertEdit edit = (InsertEdit) oldEdit;
      newEdit = new InsertEdit(edit.getOffset() - diff, edit.getText());
    } else if (oldEdit instanceof DeleteEdit) {
      DeleteEdit edit = (DeleteEdit) oldEdit;
      newEdit = new DeleteEdit(edit.getOffset() - diff, edit.getLength());
    } else if (oldEdit instanceof MultiTextEdit) {
      newEdit = new MultiTextEdit();
    } else {
      return null; // not supported
    }
    TextEdit[] children = oldEdit.getChildren();
    for (int i = 0; i < children.length; i++) {
      TextEdit shifted = shifEdit(children[i], diff);
      if (shifted != null) {
        newEdit.addChild(shifted);
      }
    }
    return newEdit;
  }

}
