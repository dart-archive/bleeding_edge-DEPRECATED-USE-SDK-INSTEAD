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

import com.google.dart.engine.ast.Annotation;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.Directive;
import com.google.dart.engine.ast.ExportDirective;
import com.google.dart.engine.ast.ImportDirective;
import com.google.dart.engine.ast.LibraryDirective;
import com.google.dart.engine.ast.NodeList;
import com.google.dart.engine.ast.PrefixedIdentifier;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.visitor.RecursiveAstVisitor;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ImportElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.MultiplyDefinedElement;
import com.google.dart.engine.element.PrefixElement;
import com.google.dart.engine.error.HintCode;
import com.google.dart.engine.internal.error.ErrorReporter;
import com.google.dart.engine.internal.resolver.ElementResolver;
import com.google.dart.engine.internal.scope.Namespace;
import com.google.dart.engine.internal.scope.NamespaceBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Instances of the class {@code ImportsVerifier} visit all of the referenced libraries in the
 * source code verifying that all of the imports are used, otherwise a
 * {@link HintCode#UNUSED_IMPORT} is generated with
 * {@link #generateUnusedImportHints(ErrorReporter)}.
 * <p>
 * While this class does not yet have support for an "Organize Imports" action, this logic built up
 * in this class could be used for such an action in the future.
 * 
 * @coverage dart.engine.resolver
 */
public class ImportsVerifier extends RecursiveAstVisitor<Void> {

  /**
   * This is set to {@code true} if the current compilation unit which is being visited is the
   * defining compilation unit for the library, its value can be set with
   * {@link #setInDefiningCompilationUnit(boolean)}.
   */
  private boolean inDefiningCompilationUnit = false;

  /**
   * The current library.
   */
  private final LibraryElement currentLibrary;

  /**
   * A list of {@link ImportDirective}s that the current library imports, as identifiers are visited
   * by this visitor and an import has been identified as being used by the library, the
   * {@link ImportDirective} is removed from this list. After all the sources in the library have
   * been evaluated, this list represents the set of unused imports.
   * 
   * @see ImportsVerifier#generateUnusedImportErrors(ErrorReporter)
   */
  private final ArrayList<ImportDirective> unusedImports;

  /**
   * After the list of {@link #unusedImports} has been computed, this list is a proper subset of the
   * unused imports that are listed more than once.
   */
  private final ArrayList<ImportDirective> duplicateImports;

  /**
   * This is a map between the set of {@link LibraryElement}s that the current library imports, and
   * a list of {@link ImportDirective}s that imports the library. In cases where the current library
   * imports a library with a single directive (such as {@code import lib1.dart;}), the library
   * element will map to a list of one {@link ImportDirective}, which will then be removed from the
   * {@link #unusedImports} list. In cases where the current library imports a library with multiple
   * directives (such as {@code import lib1.dart; import lib1.dart show C;}), the
   * {@link LibraryElement} will be mapped to a list of the import directives, and the namespace
   * will need to be used to compute the correct {@link ImportDirective} being used, see
   * {@link #namespaceMap}.
   */
  private final HashMap<LibraryElement, ArrayList<ImportDirective>> libraryMap;

  /**
   * In cases where there is more than one import directive per library element, this mapping is
   * used to determine which of the multiple import directives are used by generating a
   * {@link Namespace} for each of the imports to do lookups in the same way that they are done from
   * the {@link ElementResolver}.
   */
  private final HashMap<ImportDirective, Namespace> namespaceMap;

  /**
   * This is a map between prefix elements and the import directive from which they are derived. In
   * cases where a type is referenced via a prefix element, the import directive can be marked as
   * used (removed from the unusedImports) by looking at the resolved {@code lib} in {@code lib.X},
   * instead of looking at which library the {@code lib.X} resolves.
   */
  private final HashMap<PrefixElement, ImportDirective> prefixElementMap;

  /**
   * Create a new instance of the {@link ImportsVerifier}.
   * 
   * @param errorReporter the error reporter
   */
  public ImportsVerifier(LibraryElement library) {
    this.currentLibrary = library;
    this.unusedImports = new ArrayList<ImportDirective>();
    this.duplicateImports = new ArrayList<ImportDirective>();
    this.libraryMap = new HashMap<LibraryElement, ArrayList<ImportDirective>>();
    this.namespaceMap = new HashMap<ImportDirective, Namespace>();
    this.prefixElementMap = new HashMap<PrefixElement, ImportDirective>();
  }

  /**
   * Any time after the defining compilation unit has been visited by this visitor, this method can
   * be called to report an {@link HintCode#DUPLICATE_IMPORT} hint for each of the import directives
   * in the {@link #duplicateImports} list.
   * 
   * @param errorReporter the error reporter to report the set of {@link HintCode#DUPLICATE_IMPORT}
   *          hints to
   */
  public void generateDuplicateImportHints(ErrorReporter errorReporter) {
    for (ImportDirective duplicateImport : duplicateImports) {
      errorReporter.reportErrorForNode(HintCode.DUPLICATE_IMPORT, duplicateImport.getUri());
    }
  }

  /**
   * After all of the compilation units have been visited by this visitor, this method can be called
   * to report an {@link HintCode#UNUSED_IMPORT} hint for each of the import directives in the
   * {@link #unusedImports} list.
   * 
   * @param errorReporter the error reporter to report the set of {@link HintCode#UNUSED_IMPORT}
   *          hints to
   */
  public void generateUnusedImportHints(ErrorReporter errorReporter) {
    for (ImportDirective unusedImport : unusedImports) {
      // Check that the import isn't dart:core
      ImportElement importElement = unusedImport.getElement();
      if (importElement != null) {
        LibraryElement libraryElement = importElement.getImportedLibrary();
        if (libraryElement != null && libraryElement.isDartCore()) {
          continue;
        }
      }
      errorReporter.reportErrorForNode(HintCode.UNUSED_IMPORT, unusedImport.getUri());
    }
  }

  /*
   * Should we mark imports which are only used by comments as unused?  While the VM and dart2js
   * don't care, the analyzer (and thus the hyperlink in the Editor) and the link in dartdoc is
   * lost- thus we have made the decision for the time being to not mark them as unused.
   */
//  @Override
//  public Void visitComment(Comment node) {
//    return null;
//  }

  @Override
  public Void visitCompilationUnit(CompilationUnit node) {
    if (inDefiningCompilationUnit) {
      NodeList<Directive> directives = node.getDirectives();
      for (Directive directive : directives) {
        if (directive instanceof ImportDirective) {
          ImportDirective importDirective = (ImportDirective) directive;
          LibraryElement libraryElement = importDirective.getUriElement();
          if (libraryElement != null) {
            unusedImports.add(importDirective);
            //
            // Initialize prefixElementMap
            //
            if (importDirective.getAsToken() != null) {
              SimpleIdentifier prefixIdentifier = importDirective.getPrefix();
              if (prefixIdentifier != null) {
                Element element = prefixIdentifier.getStaticElement();
                if (element instanceof PrefixElement) {
                  PrefixElement prefixElementKey = (PrefixElement) element;
                  prefixElementMap.put(prefixElementKey, importDirective);
                }
                // TODO (jwren) Can the element ever not be a PrefixElement?
              }
            }
            //
            // Initialize libraryMap: libraryElement -> importDirective
            //
            putIntoLibraryMap(libraryElement, importDirective);

            //
            // For this new addition to the libraryMap, also recursively add any exports from the
            // libraryElement
            //
            addAdditionalLibrariesForExports(
                libraryElement,
                importDirective,
                new ArrayList<LibraryElement>());
          }
        }
      }
    }
    // If there are no imports in this library, don't visit the identifiers in the library- there
    // can be no unused imports.
    if (unusedImports.isEmpty()) {
      return null;
    }
    if (unusedImports.size() > 1) {
      // order the list of unusedImports to find duplicates in faster than O(n^2) time
      ImportDirective[] importDirectiveArray = unusedImports.toArray(new ImportDirective[unusedImports.size()]);
      Arrays.sort(importDirectiveArray, ImportDirective.COMPARATOR);
      ImportDirective currentDirective = importDirectiveArray[0];
      for (int i = 1; i < importDirectiveArray.length; i++) {
        ImportDirective nextDirective = importDirectiveArray[i];
        if (ImportDirective.COMPARATOR.compare(currentDirective, nextDirective) == 0) {
          // Add either the currentDirective or nextDirective depending on which comes second, this
          // guarantees that the first of the duplicates won't be highlighted.
          if (currentDirective.getOffset() < nextDirective.getOffset()) {
            duplicateImports.add(nextDirective);
          } else {
            duplicateImports.add(currentDirective);
          }
        }
        currentDirective = nextDirective;
      }
    }
    return super.visitCompilationUnit(node);
  }

  @Override
  public Void visitExportDirective(ExportDirective node) {
    visitMetadata(node.getMetadata());
    return null;
  }

  @Override
  public Void visitImportDirective(ImportDirective node) {
    visitMetadata(node.getMetadata());
    return null;
  }

  @Override
  public Void visitLibraryDirective(LibraryDirective node) {
    visitMetadata(node.getMetadata());
    return null;
  }

  @Override
  public Void visitPrefixedIdentifier(PrefixedIdentifier node) {
    if (unusedImports.isEmpty()) {
      return null;
    }
    // If the prefixed identifier references some A.B, where A is a library prefix, then we can
    // lookup the associated ImportDirective in prefixElementMap and remove it from the
    // unusedImports list.
    SimpleIdentifier prefixIdentifier = node.getPrefix();
    Element element = prefixIdentifier.getStaticElement();
    if (element instanceof PrefixElement) {
      unusedImports.remove(prefixElementMap.get(element));
      return null;
    }
    // Otherwise, pass the prefixed identifier element and name onto visitIdentifier.
    return visitIdentifier(element, prefixIdentifier.getName());
  }

  @Override
  public Void visitSimpleIdentifier(SimpleIdentifier node) {
    if (unusedImports.isEmpty()) {
      return null;
    }
    return visitIdentifier(node.getStaticElement(), node.getName());
  }

  void setInDefiningCompilationUnit(boolean inDefiningCompilationUnit) {
    this.inDefiningCompilationUnit = inDefiningCompilationUnit;
  }

  /**
   * Recursively add any exported library elements into the {@link #libraryMap}.
   */
  private void addAdditionalLibrariesForExports(LibraryElement library,
      ImportDirective importDirective, ArrayList<LibraryElement> exportPath) {
    if (exportPath.contains(library)) {
      return;
    }
    exportPath.add(library);
    for (LibraryElement exportedLibraryElt : library.getExportedLibraries()) {
      putIntoLibraryMap(exportedLibraryElt, importDirective);
      addAdditionalLibrariesForExports(exportedLibraryElt, importDirective, exportPath);
    }
  }

  /**
   * Lookup and return the {@link Namespace} from the {@link #namespaceMap}, if the map does not
   * have the computed namespace, compute it and cache it in the map. If the import directive is not
   * resolved or is not resolvable, {@code null} is returned.
   * 
   * @param importDirective the import directive used to compute the returned namespace
   * @return the computed or looked up {@link Namespace}
   */
  private Namespace computeNamespace(ImportDirective importDirective) {
    Namespace namespace = namespaceMap.get(importDirective);
    if (namespace == null) {
      // If the namespace isn't in the namespaceMap, then compute and put it in the map
      ImportElement importElement = importDirective.getElement();
      if (importElement != null) {
        NamespaceBuilder builder = new NamespaceBuilder();
        namespace = builder.createImportNamespaceForDirective(importElement);
        namespaceMap.put(importDirective, namespace);
      }
    }
    return namespace;
  }

  /**
   * The {@link #libraryMap} is a mapping between a library elements and a list of import
   * directives, but when adding these mappings into the {@link #libraryMap}, this method can be
   * used to simply add the mapping between the library element an an import directive without
   * needing to check to see if a list needs to be created.
   */
  private void putIntoLibraryMap(LibraryElement libraryElement, ImportDirective importDirective) {
    ArrayList<ImportDirective> importList = libraryMap.get(libraryElement);
    if (importList == null) {
      importList = new ArrayList<ImportDirective>(3);
      libraryMap.put(libraryElement, importList);
    }
    importList.add(importDirective);
  }

  private Void visitIdentifier(Element element, String name) {
    if (element == null) {
      return null;
    }

    // If the element is multiply defined then call this method recursively for each of the conflicting elements.
    if (element instanceof MultiplyDefinedElement) {
      MultiplyDefinedElement multiplyDefinedElement = (MultiplyDefinedElement) element;
      for (Element elt : multiplyDefinedElement.getConflictingElements()) {
        visitIdentifier(elt, name);
      }
      return null;
    } else if (element instanceof PrefixElement) {
      unusedImports.remove(prefixElementMap.get(element));
      return null;
    } else if (!(element.getEnclosingElement() instanceof CompilationUnitElement)) {
      // Identifiers that aren't a prefix element and whose enclosing element isn't a
      // CompilationUnit are ignored- this covers the case the identifier is a relative-reference,
      // a reference to an identifier not imported by this library.
      return null;
    }
    LibraryElement containingLibrary = element.getLibrary();
    if (containingLibrary == null) {
      return null;
    }

    // If the element is declared in the current library, return.
    if (currentLibrary.equals(containingLibrary)) {
      return null;
    }
    ArrayList<ImportDirective> importsFromSameLibrary = libraryMap.get(containingLibrary);
    if (importsFromSameLibrary == null) {
      return null;
    }
    if (importsFromSameLibrary.size() == 1) {
      // If there is only one import directive for this library, then it must be the directive that
      // this element is imported with, remove it from the unusedImports list.
      ImportDirective usedImportDirective = importsFromSameLibrary.get(0);
      unusedImports.remove(usedImportDirective);
    } else {
      // Otherwise, for each of the imported directives, use the namespaceMap to 
      for (ImportDirective importDirective : importsFromSameLibrary) {
        // Get the namespace for this import
        Namespace namespace = computeNamespace(importDirective);
        if (namespace != null && namespace.get(name) != null) {
          unusedImports.remove(importDirective);
        }
      }
    }
    return null;
  }

  /**
   * Given some {@link NodeList} of {@link Annotation}s, ensure that the identifiers are visited by
   * this visitor. Specifically, this covers the cases where AST nodes don't have their identifiers
   * visited by this visitor, but still need their annotations visited.
   * 
   * @param annotations the list of annotations to visit
   */
  private void visitMetadata(NodeList<Annotation> annotations) {
    int count = annotations.size();
    for (int i = 0; i < count; i++) {
      annotations.get(i).accept(this);
    }
  }
}
