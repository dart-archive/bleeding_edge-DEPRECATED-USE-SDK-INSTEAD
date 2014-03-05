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
package com.google.dart.engine.internal.hint;

import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.ImportDirective;
import com.google.dart.engine.ast.StringLiteral;
import com.google.dart.engine.ast.visitor.RecursiveAstVisitor;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.error.PubSuggestionCode;
import com.google.dart.engine.internal.error.ErrorReporter;
import com.google.dart.engine.source.FileUriResolver;
import com.google.dart.engine.source.PackageUriResolver;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.utilities.general.StringUtilities;

import java.io.File;

/**
 * Instances of the class {@code PubVerifier} traverse an AST structure looking for deviations from
 * pub best practices.
 */
public class PubVerifier extends RecursiveAstVisitor<Void> {

  private static final String PUBSPEC_YAML = "pubspec.yaml";

  /**
   * The analysis context containing the sources to be analyzed
   */
  private final AnalysisContext context;

  /**
   * The error reporter by which errors will be reported.
   */
  private final ErrorReporter errorReporter;

  public PubVerifier(AnalysisContext context, ErrorReporter errorReporter) {
    this.context = context;
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
      if (!checkForFileImportOutsideLibReferencesFileInside(uriLiteral, path)) {
        checkForFileImportInsideLibReferencesFileOutside(uriLiteral, path);
      }
    } else if (scheme.equals(PackageUriResolver.PACKAGE_SCHEME)) {
      checkForPackageImportContainsDotDot(uriLiteral, path);
    }
    return null;
  }

  /**
   * This verifies that the passed file import directive is not contained in a source inside a
   * package "lib" directory hierarchy referencing a source outside that package "lib" directory
   * hierarchy.
   * 
   * @param uriLiteral the import URL (not {@code null})
   * @param path the file path being verified (not {@code null})
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see PubSuggestionCode.FILE_IMPORT_INSIDE_LIB_REFERENCES_FILE_OUTSIDE
   */
  private boolean checkForFileImportInsideLibReferencesFileOutside(StringLiteral uriLiteral,
      String path) {
    Source source = getSource(uriLiteral);
    String fullName = getSourceFullName(source);
    if (fullName != null) {
      int pathIndex = 0;
      int fullNameIndex = fullName.length();
      while (pathIndex < path.length()
          && StringUtilities.startsWith3(path, pathIndex, '.', '.', '/')) {
        fullNameIndex = fullName.lastIndexOf('/', fullNameIndex);
        if (fullNameIndex < 4) {
          return false;
        }
        // Check for "/lib" at a specified place in the fullName
        if (StringUtilities.startsWith4(fullName, fullNameIndex - 4, '/', 'l', 'i', 'b')) {
          String relativePubspecPath = path.substring(0, pathIndex + 3).concat(PUBSPEC_YAML);
          Source pubspecSource = context.getSourceFactory().resolveUri(source, relativePubspecPath);
          if (context.exists(pubspecSource)) {
            // Files inside the lib directory hierarchy should not reference files outside
            errorReporter.reportErrorForNode(
                PubSuggestionCode.FILE_IMPORT_INSIDE_LIB_REFERENCES_FILE_OUTSIDE,
                uriLiteral);
          }
          return true;
        }
        pathIndex += 3;
      }
    }
    return false;
  }

  /**
   * This verifies that the passed file import directive is not contained in a source outside a
   * package "lib" directory hierarchy referencing a source inside that package "lib" directory
   * hierarchy.
   * 
   * @param uriLiteral the import URL (not {@code null})
   * @param path the file path being verified (not {@code null})
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see PubSuggestionCode.FILE_IMPORT_OUTSIDE_LIB_REFERENCES_FILE_INSIDE
   */
  private boolean checkForFileImportOutsideLibReferencesFileInside(StringLiteral uriLiteral,
      String path) {
    if (StringUtilities.startsWith4(path, 0, 'l', 'i', 'b', '/')) {
      if (checkForFileImportOutsideLibReferencesFileInsideAtIndex(uriLiteral, path, 0)) {
        return true;
      }
    }
    int pathIndex = StringUtilities.indexOf5(path, 0, '/', 'l', 'i', 'b', '/');
    while (pathIndex != -1) {
      if (checkForFileImportOutsideLibReferencesFileInsideAtIndex(uriLiteral, path, pathIndex + 1)) {
        return true;
      }
      pathIndex = StringUtilities.indexOf5(path, pathIndex + 4, '/', 'l', 'i', 'b', '/');
    }
    return false;
  }

  private boolean checkForFileImportOutsideLibReferencesFileInsideAtIndex(StringLiteral uriLiteral,
      String path, int pathIndex) {
    Source source = getSource(uriLiteral);
    String relativePubspecPath = path.substring(0, pathIndex).concat(PUBSPEC_YAML);
    Source pubspecSource = context.getSourceFactory().resolveUri(source, relativePubspecPath);
    if (!context.exists(pubspecSource)) {
      return false;
    }
    String fullName = getSourceFullName(source);
    if (fullName != null) {
      if (StringUtilities.indexOf5(fullName, 0, '/', 'l', 'i', 'b', '/') < 0) {
        // Files outside the lib directory hierarchy should not reference files inside
        // ... use package: url instead
        errorReporter.reportErrorForNode(
            PubSuggestionCode.FILE_IMPORT_OUTSIDE_LIB_REFERENCES_FILE_INSIDE,
            uriLiteral);
        return true;
      }
    }
    return false;
  }

  /**
   * This verifies that the passed package import directive does not contain ".."
   * 
   * @param uriLiteral the import URL (not {@code null})
   * @param path the path to be validated (not {@code null})
   * @return {@code true} if and only if an error code is generated on the passed node
   * @see PubSuggestionCode.PACKAGE_IMPORT_CONTAINS_DOT_DOT
   */
  private boolean checkForPackageImportContainsDotDot(StringLiteral uriLiteral, String path) {
    if (StringUtilities.startsWith3(path, 0, '.', '.', '/')
        || StringUtilities.indexOf4(path, 0, '/', '.', '.', '/') >= 0) {
      // Package import should not to contain ".."
      errorReporter.reportErrorForNode(PubSuggestionCode.PACKAGE_IMPORT_CONTAINS_DOT_DOT, uriLiteral);
      return true;
    }
    return false;
  }

  /**
   * Answer the source associated with the compilation unit containing the given AST node.
   * 
   * @param node the node (not {@code null})
   * @return the source or {@code null} if it could not be determined
   */
  private Source getSource(AstNode node) {
    Source source = null;
    CompilationUnit unit = node.getAncestor(CompilationUnit.class);
    if (unit != null) {
      CompilationUnitElement element = unit.getElement();
      if (element != null) {
        source = element.getSource();
      }
    }
    return source;
  }

  /**
   * Answer the full name of the given source. The returned value will have all
   * {@link File#separatorChar} replace by '/'.
   * 
   * @param source the source
   * @return the full name or {@code null} if it could not be determined
   */
  private String getSourceFullName(Source source) {
    if (source != null) {
      String fullName = source.getFullName();
      if (fullName != null) {
        return fullName.replace(File.separatorChar, '/');
      }
    }
    return null;
  }
}
