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
package com.google.dart.engine.internal.sdk;

import com.google.dart.engine.ast.BooleanLiteral;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.InstanceCreationExpression;
import com.google.dart.engine.ast.MapLiteralEntry;
import com.google.dart.engine.ast.NamedExpression;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.SimpleStringLiteral;
import com.google.dart.engine.ast.visitor.RecursiveASTVisitor;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.AnalysisErrorListener;
import com.google.dart.engine.parser.Parser;
import com.google.dart.engine.scanner.StringScanner;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceImpl;

import java.util.List;

/**
 * Instances of the class {@code SdkLibrariesReader} read and parse the libraries file
 * (dart-sdk/lib/_internal/libraries.dart) for information about the libraries in an SDK. The
 * library information is represented as a Dart file containing a single top-level variable whose
 * value is a const map. The keys of the map are the names of libraries defined in the SDK and the
 * values in the map are info objects defining the library. For example, a subset of a typical SDK
 * might have a libraries file that looks like the following:
 * 
 * <pre>
 * final Map&lt;String, LibraryInfo&gt; LIBRARIES = const &lt;LibraryInfo&gt; {
 *   // Used by VM applications
 *   "builtin" : const LibraryInfo(
 *     "builtin/builtin_runtime.dart",
 *     category: "Server",
 *     platforms: VM_PLATFORM),
 *
 *   "compiler" : const LibraryInfo(
 *     "compiler/compiler.dart",
 *     category: "Tools",
 *     platforms: 0),
 * };
 * </pre>
 */
public class SdkLibrariesReader {
  private static class LibraryBuilder extends RecursiveASTVisitor<Void> {
    /**
     * The prefix added to the name of a library to form the URI used in code to reference the
     * library.
     */
    private static final String LIBRARY_PREFIX = "dart:"; //$NON-NLS-1$

    /**
     * The name of the optional parameter used to indicate whether the library is an implementation
     * library.
     */
    private static final String IMPLEMENTATION = "implementation"; //$NON-NLS-1$

    /**
     * The name of the optional parameter used to indicate whether the library is documented.
     */
    private static final String DOCUMENTED = "documented"; //$NON-NLS-1$

    /**
     * The name of the optional parameter used to specify the category of the library.
     */
    private static final String CATEGORY = "category"; //$NON-NLS-1$

    /**
     * The name of the optional parameter used to specify the platforms on which the library can be
     * used.
     */
    private static final String PLATFORMS = "platforms"; //$NON-NLS-1$

    /**
     * The value of the {@link #PLATFORMS platforms} parameter used to specify that the library can
     * be used on the VM.
     */
    private static final String VM_PLATFORM = "VM_PLATFORM"; //$NON-NLS-1$

    /**
     * The library map that is populated by visiting the AST structure parsed from the contents of
     * the libraries file.
     */
    private LibraryMap librariesMap = new LibraryMap();

    /**
     * Return the library map that was populated by visiting the AST structure parsed from the
     * contents of the libraries file.
     * 
     * @return the library map describing the contents of the SDK
     */
    public LibraryMap getLibrariesMap() {
      return librariesMap;
    }

    @Override
    public Void visitMapLiteralEntry(MapLiteralEntry node) {
      String libraryName = null;
      Expression key = node.getKey();
      if (key instanceof SimpleStringLiteral) {
        libraryName = LIBRARY_PREFIX + ((SimpleStringLiteral) key).getValue();
      }
      Expression value = node.getValue();
      if (value instanceof InstanceCreationExpression) {
        SdkLibrary library = new SdkLibrary(libraryName);
        List<Expression> arguments = ((InstanceCreationExpression) value).getArgumentList().getArguments();
        for (Expression argument : arguments) {
          if (argument instanceof SimpleStringLiteral) {
            library.setPath(((SimpleStringLiteral) argument).getValue());
          } else if (argument instanceof NamedExpression) {
            String name = ((NamedExpression) argument).getName().getLabel().getName();
            Expression expression = ((NamedExpression) argument).getExpression();
            if (name.equals(CATEGORY)) {
              library.setCategory(((SimpleStringLiteral) expression).getValue());
            } else if (name.equals(IMPLEMENTATION)) {
              library.setImplementation(((BooleanLiteral) expression).getValue());
            } else if (name.equals(DOCUMENTED)) {
              library.setDocumented(((BooleanLiteral) expression).getValue());
            } else if (name.equals(PLATFORMS)) {
              if (expression instanceof SimpleIdentifier) {
                String identifier = ((SimpleIdentifier) expression).getName();
                if (identifier.equals(VM_PLATFORM)) {
                  library.setVmLibrary();
                } else {
                  library.setDart2JsLibrary();
                }
              }
            }
          }
        }
        librariesMap.setLibrary(libraryName, library);
      }
      return null;
    }
  }

  /**
   * Return the library map read from the given source.
   * 
   * @return the library map read from the given source
   */
  public LibraryMap readFrom(String libraryFileContents) {
    final boolean[] foundError = {false};
    AnalysisErrorListener errorListener = new AnalysisErrorListener() {
      @Override
      public void onError(AnalysisError error) {
        foundError[0] = true;
      }
    };
    Source source = new SourceImpl(null, null, false);
    StringScanner scanner = new StringScanner(source, libraryFileContents, errorListener);
    Parser parser = new Parser(source, errorListener);
    CompilationUnit unit = parser.parseCompilationUnit(scanner.tokenize());
    LibraryBuilder libraryBuilder = new LibraryBuilder();
    // If any syntactic errors were found then don't try to visit the AST structure.
    if (!foundError[0]) {
      unit.accept(libraryBuilder);
    }
    return libraryBuilder.getLibrariesMap();
  }
}
