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
package com.google.dart.tools.ui.internal.cleanup.migration;

import com.google.common.base.CharMatcher;
import com.google.dart.compiler.ast.ASTVisitor;
import com.google.dart.compiler.ast.DartImportDirective;
import com.google.dart.compiler.ast.DartLibraryDirective;
import com.google.dart.compiler.ast.DartSourceDirective;
import com.google.dart.compiler.ast.DartStringLiteral;
import com.google.dart.compiler.util.apache.StringUtils;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.utilities.general.SourceRangeFactory;
import com.google.dart.tools.internal.corext.refactoring.code.ExtractUtils;

import org.eclipse.text.edits.TextEdit;

/**
 * In specification 1.0 M1 new library/import/source syntax is defined.
 * 
 * @coverage dart.editor.ui.cleanup
 */
public class Migrate_1M1_library_CleanUp extends AbstractMigrateCleanUp {
  /**
   * @return the {@link TextEdit} to insert missing "part of" directive, may be <code>null</code>.
   *         Note that this method does not change if there is already "part of" directive in unit.
   */
  public static TextEdit createEditInsertPartOf(ExtractUtils utils) throws Exception {
    TextEdit edit = null;
    DartLibrary library = utils.getUnit().getLibrary();
    if (library != null) {
      String libraryName = library.getLibraryDirectiveName();
      if (libraryName != null) {
        // prepare position of "part of"
        int insertOffset = 0;
        boolean insertEmptyLineBefore = false;
        boolean insertEmptyLineAfter = false;
        {
          String source = utils.getText();
          while (insertOffset < source.length() - 2) {
            if (utils.getText(insertOffset, 2).equals("//")) {
              insertEmptyLineBefore = true;
              insertOffset = utils.getLineNext(insertOffset);
            } else {
              break;
            }
          }
          // determine if empty line required
          int nextLineOffset = utils.getLineNext(insertOffset);
          String insertLine = source.substring(insertOffset, nextLineOffset);
          if (!insertLine.trim().isEmpty()) {
            insertEmptyLineAfter = true;
          }
        }
        // do insert
        libraryName = mapLibraryName(libraryName);
        String eol = utils.getEndOfLine();
        String source = "part of " + libraryName + ";" + eol;
        if (insertEmptyLineBefore) {
          source = eol + source;
        }
        if (insertEmptyLineAfter) {
          source += eol;
        }
        edit = createReplaceEdit(SourceRangeFactory.forStartLength(insertOffset, 0), source);
      }
    }
    return edit;
  }

  /**
   * Converts string literal version of library name into identifier named.
   */
  public static String mapLibraryName(String name) {
    name = StringUtils.removeEnd(name, ".dart");
    name = CharMatcher.JAVA_LETTER_OR_DIGIT.negate().replaceFrom(name, '_');
    return name;
  }

  @Override
  protected void createFix() throws Exception {
    ensurePartOfDirective();
    unitNode.accept(new ASTVisitor<Void>() {
      @Override
      @SuppressWarnings("deprecation")
      public Void visitImportDirective(DartImportDirective node) {
        if (node.isObsoleteFormat()) {
          DartStringLiteral uriNode = node.getLibraryUri();
          String uriSource = utils.getText(uriNode);
          String source = "import " + uriSource;
          if (node.getOldPrefix() != null) {
            source += " as " + node.getOldPrefix().getValue();
          }
          source += ";";
          addReplaceEdit(SourceRangeFactory.create(node), source);
        }
        return super.visitImportDirective(node);
      }

      @Override
      public Void visitLibraryDirective(DartLibraryDirective node) {
        if (node.isObsoleteFormat()) {
          String name = node.getLibraryName();
          name = mapLibraryName(name);
          addReplaceEdit(SourceRangeFactory.create(node), "library " + name + ";");
        }
        return super.visitLibraryDirective(node);
      }

      @Override
      public Void visitSourceDirective(DartSourceDirective node) {
        if (utils.getText(node).startsWith("#source")) {
          DartStringLiteral uriNode = node.getSourceUri();
          String uriSource = utils.getText(uriNode);
          addReplaceEdit(SourceRangeFactory.create(node), "part " + uriSource + ";");
        }
        return super.visitSourceDirective(node);
      }

    });
  }

  private void ensurePartOfDirective() throws Exception {
    // "part" should not have directives
    if (!unitNode.getDirectives().isEmpty()) {
      return;
    }
    // insert
    TextEdit textEdit = createEditInsertPartOf(utils);
    if (textEdit != null) {
      change.addEdit(textEdit);
    }
  }
}
