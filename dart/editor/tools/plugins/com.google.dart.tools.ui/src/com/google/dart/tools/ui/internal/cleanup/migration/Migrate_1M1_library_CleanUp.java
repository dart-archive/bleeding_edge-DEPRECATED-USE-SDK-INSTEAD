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

import com.google.dart.compiler.ast.ASTVisitor;
import com.google.dart.compiler.ast.DartImportDirective;
import com.google.dart.compiler.ast.DartLibraryDirective;
import com.google.dart.compiler.ast.DartSourceDirective;
import com.google.dart.compiler.ast.DartStringLiteral;
import com.google.dart.compiler.util.apache.StringUtils;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.utilities.general.SourceRangeFactory;

/**
 * In specification 1.0 M1 new library/import/source syntax is defined.
 * 
 * @coverage dart.editor.ui.cleanup
 */
public class Migrate_1M1_library_CleanUp extends AbstractMigrateCleanUp {
  private static String mapLibraryName(String name) {
    name = StringUtils.removeEnd(name, ".dart");
    name = StringUtils.replace(name, ".", "_");
    name = StringUtils.replace(name, ":", "_");
    name = StringUtils.replace(name, "-", "_");
    name = StringUtils.removeEnd(name, ".dart");
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
    // do insert
    DartLibrary library = unit.getLibrary();
    if (library != null) {
      String libraryName = library.getLibraryDirectiveName();
      if (libraryName != null) {
        String eol = utils.getEndOfLine();
        String source = "part of " + mapLibraryName(libraryName) + ";" + eol + eol;
        addReplaceEdit(SourceRangeFactory.forStartLength(0, 0), source);
      }
    }
  }
}
