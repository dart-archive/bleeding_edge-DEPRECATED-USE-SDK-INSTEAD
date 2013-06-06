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
package com.google.dart.engine.internal.verifier;

import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.ImportDirective;
import com.google.dart.engine.ast.StringLiteral;
import com.google.dart.engine.ast.visitor.RecursiveASTVisitor;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.error.PubSuggestionCode;
import com.google.dart.engine.internal.error.ErrorReporter;
import com.google.dart.engine.source.FileUriResolver;
import com.google.dart.engine.source.PackageUriResolver;
import com.google.dart.engine.source.Source;

import java.io.File;

/**
 * Instances of the class {@code PubVerifier} traverse an AST structure looking for deviations from
 * pub best practices.
 */
public class PubVerifier extends RecursiveASTVisitor<Void> {

  /**
   * The error reporter by which errors will be reported.
   */
  private final ErrorReporter errorReporter;

  public PubVerifier(ErrorReporter errorReporter) {
    this.errorReporter = errorReporter;
  }

  @Override
  public Void visitImportDirective(ImportDirective directive) {

    // Don't bother showing a suggestion if there is a more important issue to be solved.
    StringLiteral uriLiteral = directive.getUri();
    if (uriLiteral == null) {
      return null;
    }
    String uriContent = uriLiteral.getStringValue();
    if (uriContent == null) {
      return null;
    }
    uriContent = uriContent.trim();

    // Analyze the URI
    int index = uriContent.indexOf(':');
    String scheme;
    String path;
    if (index > -1) {
      scheme = uriContent.substring(0, index);
      path = uriContent.substring(index + 1);
    } else {
      scheme = FileUriResolver.FILE_SCHEME;
      path = uriContent;
    }

    if (scheme.equals(FileUriResolver.FILE_SCHEME)) {
      if (checkForFileImportOutsideLibReferencesFileInside(directive, path)) {
        // Files outside the lib directory hierarchy should not reference files inside
        // ... use package: url instead
        errorReporter.reportError(
            PubSuggestionCode.FILE_IMPORT_OUTSIDE_LIB_REFERENCES_FILE_INSIDE,
            uriLiteral);
      } else if (checkForFileImportInsideLibReferencesFileOutside(directive, path)) {
        // Files inside the lib directory hierarchy should not reference files outside
        errorReporter.reportError(
            PubSuggestionCode.FILE_IMPORT_INSIDE_LIB_REFERENCES_FILE_OUTSIDE,
            uriLiteral);
      }
    } else if (scheme.equals(PackageUriResolver.PACKAGE_SCHEME)) {
      if (checkForPackageImportContainsDotDot(path)) {
        // Package import should not to contain ".."
        errorReporter.reportError(PubSuggestionCode.PACKAGE_IMPORT_CONTAINS_DOT_DOT, uriLiteral);
      }
    }
    return null;
  }

  /**
   * Determine if the file file path lies inside the "lib" directory hierarchy but references a file
   * outside that directory hierarchy.
   * 
   * @param directive the import directive (not {@code null})
   * @param path the file path being verified (not {@code null})
   * @return {@code true} if the file is inside but references a file outside
   */
  private boolean checkForFileImportInsideLibReferencesFileOutside(ImportDirective directive,
      String path) {
    String fullName = getSourceFullName(directive);
    if (fullName != null) {
      int pathIndex = 0;
      int fullNameIndex = fullName.length();
      while (pathIndex < path.length() && path.startsWith("../", pathIndex)) {
        fullNameIndex = fullName.lastIndexOf('/', fullNameIndex);
        if (fullNameIndex < 4) {
          return false;
        }
        // Check for "/lib" at a specified place in the fullName
        if (fullName.startsWith("/lib", fullNameIndex - 4)) {
          return true;
        }
        pathIndex += 3;
      }
    }
    return false;
  }

  /**
   * Determine if the given file path lies outside the "lib" directory hierarchy but references a
   * file inside that directory hierarchy.
   * 
   * @param directive the import directive (not {@code null})
   * @param path the file path being verified (not {@code null})
   * @return {@code true} if the file is outside but references a file inside
   */
  private boolean checkForFileImportOutsideLibReferencesFileInside(ImportDirective directive,
      String path) {
    if (path.startsWith("lib/") || path.contains("/lib/")) {
      String fullName = getSourceFullName(directive);
      if (fullName != null) {
        if (!fullName.contains("/lib/")) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Determine if the given package import path contains ".."
   * 
   * @param path the path to be validated (not {@code null})
   * @return {@code true} if the import path contains ".."
   */
  private boolean checkForPackageImportContainsDotDot(String path) {
    return path.startsWith("../") || path.contains("/../");
  }

  /**
   * Answer the full name of the source associated with the compilation unit containing the given
   * AST node. The returned value will have all {@link File#separatorChar} replace by '/'.
   * 
   * @param node the node (not {@code null})
   * @return the full name or {@code null} if it could not be determined
   */
  private String getSourceFullName(ASTNode node) {
    CompilationUnit unit = node.getAncestor(CompilationUnit.class);
    if (unit != null) {
      CompilationUnitElement element = unit.getElement();
      if (element != null) {
        Source librarySource = element.getSource();
        if (librarySource != null) {
          String fullName = librarySource.getFullName();
          if (fullName != null) {
            return fullName.replace(File.separatorChar, '/');
          }
        }
      }
    }
    return null;
  }
}
