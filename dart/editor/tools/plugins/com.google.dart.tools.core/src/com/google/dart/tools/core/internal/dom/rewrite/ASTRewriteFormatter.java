/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.google.dart.tools.core.internal.dom.rewrite;

import com.google.dart.compiler.ast.DartNode;
import com.google.dart.tools.core.DartCore;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.DefaultPositionUpdater;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.Position;
import org.eclipse.text.edits.TextEdit;

import java.util.Map;

/**
 * Instances of the class <code>ASTRewriteFormatter</code>
 */
public class ASTRewriteFormatter {
  public static interface BlockContext {
    String[] getPrefixAndSuffix(int indent, DartNode node, RewriteEventStore events);
  }

  public static class ConstPrefix implements Prefix {
    private String prefix;

    public ConstPrefix(String prefix) {
      this.prefix = prefix;
    }

    @Override
    public String getPrefix(int indent) {
      return prefix;
    }
  }

  public static interface Prefix {
    String getPrefix(int indent);
  }

  private class BlockFormattingPrefix implements BlockContext {
    private String prefix;
    private int start;

    public BlockFormattingPrefix(String prefix, int start) {
      this.start = start;
      this.prefix = prefix;
    }

    @Override
    public String[] getPrefixAndSuffix(int indent, DartNode node, RewriteEventStore events) {
      DartCore.notYetImplemented();
      return null;
      // String nodeString= ASTRewriteFlattener.asString(node, events);
      // String str= prefix + nodeString;
      // Position pos= new Position(start, prefix.length() + 1 -
      // start);
      //
      // TextEdit res= formatString(CodeFormatter.K_STATEMENTS, str, 0,
      // str.length(), indent);
      // if (res != null) {
      // str= evaluateFormatterEdit(str, res, new Position[] { pos });
      // }
      //      return new String[] { str.substring(pos.offset + 1, pos.offset + pos.length - 1), ""}; //$NON-NLS-1$
    }
  }

  private class BlockFormattingPrefixSuffix implements BlockContext {
    private String prefix;
    private String suffix;
    private int start;

    public BlockFormattingPrefixSuffix(String prefix, String suffix, int start) {
      this.start = start;
      this.suffix = suffix;
      this.prefix = prefix;
    }

    @Override
    public String[] getPrefixAndSuffix(int indent, DartNode node, RewriteEventStore events) {
      DartCore.notYetImplemented();
      return null;
      // String nodeString= ASTRewriteFlattener.asString(node, events);
      // int nodeStart= prefix.length();
      // int nodeEnd= nodeStart + nodeString.length() - 1;
      //
      // String str= prefix + nodeString + suffix;
      //
      // Position pos1= new Position(start, nodeStart + 1 - start);
      // Position pos2= new Position(nodeEnd, 2);
      //
      // TextEdit res= formatString(CodeFormatter.K_STATEMENTS, str, 0,
      // str.length(), indent);
      // if (res != null) {
      // str= evaluateFormatterEdit(str, res, new Position[] { pos1, pos2 });
      // }
      // return new String[] {
      // str.substring(pos1.offset + 1, pos1.offset + pos1.length - 1),
      // str.substring(pos2.offset + 1, pos2.offset + pos2.length - 1)
      // };
    }
  }

  private class FormattingPrefix implements Prefix {
    private int kind;
    private String string;
    private int start;
    private int length;

    public FormattingPrefix(String string, String sub, int kind) {
      this.start = string.indexOf(sub);
      this.length = sub.length();
      this.string = string;
      this.kind = kind;
    }

    @Override
    public String getPrefix(int indent) {
      Position pos = new Position(start, length);
      String str = string;
      TextEdit res = formatString(kind, str, 0, str.length(), indent);
      if (res != null) {
        str = evaluateFormatterEdit(str, res, new Position[] {pos});
      }
      return str.substring(pos.offset + 1, pos.offset + pos.length - 1);
    }
  }

  public final static Prefix NONE = new ConstPrefix(""); //$NON-NLS-1$
  public final static Prefix SPACE = new ConstPrefix(" "); //$NON-NLS-1$
  public final static Prefix ASSERT_COMMENT = new ConstPrefix(" : "); //$NON-NLS-1$

  /**
   * Evaluates the edit on the given string.
   * 
   * @param string The string to format
   * @param edit The edit resulted from the code formatter
   * @param positions Positions to update or <code>null</code>.
   * @return The formatted string
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
      // JavaPlugin.log(e); // bug in the formatter
      Assert.isTrue(false, "Fromatter created edits with wrong positions: " + e.getMessage()); //$NON-NLS-1$
    }
    return null;
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
            int start = fOffset;
            int end = start + fLength;
            if (start < fPosition.offset && (fPosition.offset + fPosition.length < end)) {
              fPosition.offset = end; // deleted positions: set to end of
                                      // remove
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

  public final BlockContext IF_BLOCK_WITH_ELSE = new BlockFormattingPrefixSuffix(
      "if (true)", "else{}", 8); //$NON-NLS-1$ //$NON-NLS-2$
  public final BlockContext IF_BLOCK_NO_ELSE = new BlockFormattingPrefix("if (true)", 8); //$NON-NLS-1$

  public final BlockContext ELSE_AFTER_STATEMENT = new BlockFormattingPrefix(
      "if (true) foo();else ", 15); //$NON-NLS-1$
  public final BlockContext ELSE_AFTER_BLOCK = new BlockFormattingPrefix("if (true) {}else ", 11); //$NON-NLS-1$
  public final Prefix VAR_INITIALIZER = new FormattingPrefix("A a={};", "a={", 2); //$NON-NLS-1$ //$NON-NLS-2$
  public final Prefix METHOD_BODY = new FormattingPrefix("void a() {}", ") {", 4); //$NON-NLS-1$ //$NON-NLS-2$

  public final Prefix FINALLY_BLOCK = new FormattingPrefix("try {} finally {}", "} finally {", 2); //$NON-NLS-1$ //$NON-NLS-2$
  public final Prefix CATCH_BLOCK = new FormattingPrefix("try {} catch(Exception e) {}", "} c", 2); //$NON-NLS-1$ //$NON-NLS-2$
  public final BlockContext FOR_BLOCK = new BlockFormattingPrefix("for (;;) ", 7); //$NON-NLS-1$

  public final BlockContext WHILE_BLOCK = new BlockFormattingPrefix("while (true)", 11); //$NON-NLS-1$

  public final BlockContext DO_BLOCK = new BlockFormattingPrefixSuffix("do ", "while (true);", 1); //$NON-NLS-1$ //$NON-NLS-2$

  static {
    DartCore.notYetImplemented();
  }

  public ASTRewriteFormatter(NodeInfoStore placeholders, RewriteEventStore eventStore,
      Map<String, String> options, String lineDelimiter) {
    DartCore.notYetImplemented();
    // this.placeholders= placeholders;
    // this.eventStore= eventStore;
    //
    // this.options= options;
    // this.lineDelimiter= lineDelimiter;
    //
    // this.tabWidth= IndentManipulation.getTabWidth(options);
    // this.indentWidth= IndentManipulation.getIndentWidth(options);
  }

  public TextEdit formatString(int kind, String string, int offset, int length, int indentationLevel) {
    DartCore.notYetImplemented();
    return null;
    // return ToolFactory.createCodeFormatter(options).format(kind, string,
    // offset, length, indentationLevel, lineDelimiter);
  }
}
