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
package com.google.dart.engine.internal.element;

import com.google.common.collect.Sets;
import com.google.dart.engine.AnalysisEngine;
import com.google.dart.engine.ast.LibraryIdentifier;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.ElementKind;
import com.google.dart.engine.element.ElementVisitor;
import com.google.dart.engine.element.ExportElement;
import com.google.dart.engine.element.FunctionElement;
import com.google.dart.engine.element.ImportElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.PrefixElement;
import com.google.dart.engine.internal.type.DynamicTypeImpl;
import com.google.dart.engine.internal.type.FunctionTypeImpl;
import com.google.dart.engine.internal.type.VoidTypeImpl;
import com.google.dart.engine.sdk.DartSdk;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.type.InterfaceType;
import com.google.dart.engine.type.Type;
import com.google.dart.engine.utilities.general.StringUtilities;
import com.google.dart.engine.utilities.translation.DartName;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Instances of the class {@code LibraryElementImpl} implement a {@code LibraryElement}.
 * 
 * @coverage dart.engine.element
 */
public class LibraryElementImpl extends ElementImpl implements LibraryElement {
  /**
   * An empty array of library elements.
   */
  public static final LibraryElement[] EMPTY_ARRAY = new LibraryElement[0];

  /**
   * Determine if the given library is up to date with respect to the given time stamp.
   * 
   * @param library the library to process
   * @param timeStamp the time stamp to check against
   * @param visitedLibraries the set of visited libraries
   */
  private static boolean safeIsUpToDate(LibraryElement library, long timeStamp,
      Set<LibraryElement> visitedLibraries) {
    if (!visitedLibraries.contains(library)) {
      visitedLibraries.add(library);

      AnalysisContext context = library.getContext();
      // Check the defining compilation unit.
      if (timeStamp < context.getModificationStamp(library.getDefiningCompilationUnit().getSource())) {
        return false;
      }

      // Check the parted compilation units.
      for (CompilationUnitElement element : library.getParts()) {
        if (timeStamp < context.getModificationStamp(element.getSource())) {
          return false;
        }
      }

      // Check the imported libraries.
      for (LibraryElement importedLibrary : library.getImportedLibraries()) {
        if (!safeIsUpToDate(importedLibrary, timeStamp, visitedLibraries)) {
          return false;
        }
      }

      // Check the exported libraries.
      for (LibraryElement exportedLibrary : library.getExportedLibraries()) {
        if (!safeIsUpToDate(exportedLibrary, timeStamp, visitedLibraries)) {
          return false;
        }
      }
    }

    return true;
  }

  /**
   * The analysis context in which this library is defined.
   */
  private AnalysisContext context;

  /**
   * The compilation unit that defines this library.
   */
  private CompilationUnitElement definingCompilationUnit;

  /**
   * The entry point for this library, or {@code null} if this library does not have an entry point.
   */
  private FunctionElement entryPoint;

  /**
   * An array containing specifications of all of the imports defined in this library.
   */
  private ImportElement[] imports = ImportElement.EMPTY_ARRAY;

  /**
   * An array containing specifications of all of the exports defined in this library.
   */
  private ExportElement[] exports = ExportElement.EMPTY_ARRAY;

  /**
   * An array containing all of the compilation units that are included in this library using a
   * {@code part} directive.
   */
  private CompilationUnitElement[] parts = CompilationUnitElementImpl.EMPTY_ARRAY;

  /**
   * Is {@code true} if this library is created for Angular analysis.
   */
  private boolean isAngularHtml;

  /**
   * The element representing the synthetic function {@code loadLibrary} that is defined for this
   * library, or {@code null} if the element has not yet been created.
   */
  private FunctionElement loadLibraryFunction;

  /**
   * Initialize a newly created library element to have the given name.
   * 
   * @param context the analysis context in which the library is defined
   * @param name the name of this element
   */
  @DartName("forNode")
  public LibraryElementImpl(AnalysisContext context, LibraryIdentifier name) {
    super(name);
    this.context = context;
  }

  /**
   * Initialize a newly created library element to have the given name.
   * 
   * @param context the analysis context in which the library is defined
   * @param name the name of this element
   * @param nameOffset the offset of the name of this element in the file that contains the
   *          declaration of this element
   */
  public LibraryElementImpl(AnalysisContext context, String name, int nameOffset) {
    super(name, nameOffset);
    this.context = context;
  }

  @Override
  public <R> R accept(ElementVisitor<R> visitor) {
    return visitor.visitLibraryElement(this);
  }

  @Override
  public boolean equals(Object object) {
    return object != null
        && getClass() == object.getClass()
        && definingCompilationUnit.equals(((LibraryElementImpl) object).getDefiningCompilationUnit());
  }

  @Override
  public ElementImpl getChild(String identifier) {
    if (((CompilationUnitElementImpl) definingCompilationUnit).getIdentifier().equals(identifier)) {
      return (CompilationUnitElementImpl) definingCompilationUnit;
    }
    for (CompilationUnitElement part : parts) {
      if (((CompilationUnitElementImpl) part).getIdentifier().equals(identifier)) {
        return (CompilationUnitElementImpl) part;
      }
    }
    for (ImportElement importElement : imports) {
      if (((ImportElementImpl) importElement).getIdentifier().equals(identifier)) {
        return (ImportElementImpl) importElement;
      }
    }
    for (ExportElement exportElement : exports) {
      if (((ExportElementImpl) exportElement).getIdentifier().equals(identifier)) {
        return (ExportElementImpl) exportElement;
      }
    }
    return null;
  }

  @Override
  public AnalysisContext getContext() {
    return context;
  }

  @Override
  public CompilationUnitElement getDefiningCompilationUnit() {
    return definingCompilationUnit;
  }

  @Override
  public FunctionElement getEntryPoint() {
    return entryPoint;
  }

  @Override
  public LibraryElement[] getExportedLibraries() {
    HashSet<LibraryElement> libraries = new HashSet<LibraryElement>(exports.length);
    for (ExportElement element : exports) {
      LibraryElement library = element.getExportedLibrary();
      if (library != null) {
        libraries.add(library);
      }
    }
    return libraries.toArray(new LibraryElement[libraries.size()]);
  }

  @Override
  public ExportElement[] getExports() {
    return exports;
  }

  @Override
  public LibraryElement[] getImportedLibraries() {
    HashSet<LibraryElement> libraries = new HashSet<LibraryElement>(imports.length);
    for (ImportElement element : imports) {
      LibraryElement library = element.getImportedLibrary();
      if (library != null) {
        libraries.add(library);
      }
    }
    return libraries.toArray(new LibraryElement[libraries.size()]);
  }

  @Override
  public ImportElement[] getImports() {
    return imports;
  }

  @Override
  public ImportElement[] getImportsWithPrefix(PrefixElement prefixElement) {
    int count = imports.length;
    ArrayList<ImportElement> importList = new ArrayList<ImportElement>(count);
    for (int i = 0; i < count; i++) {
      if (imports[i].getPrefix() == prefixElement) {
        importList.add(imports[i]);
      }
    }
    return importList.toArray(new ImportElement[importList.size()]);
  }

  @Override
  public ElementKind getKind() {
    return ElementKind.LIBRARY;
  }

  @Override
  public LibraryElement getLibrary() {
    return this;
  }

  @Override
  public FunctionElement getLoadLibraryFunction() {
    if (loadLibraryFunction == null) {
      FunctionElementImpl function = new FunctionElementImpl(FunctionElement.LOAD_LIBRARY_NAME, -1);
      function.setSynthetic(true);
      function.setEnclosingElement(this);
      function.setReturnType(getLoadLibraryReturnType());
      function.setType(new FunctionTypeImpl(function));
      loadLibraryFunction = function;
    }
    return loadLibraryFunction;
  }

  @Override
  public CompilationUnitElement[] getParts() {
    return parts;
  }

  @Override
  public PrefixElement[] getPrefixes() {
    HashSet<PrefixElement> prefixes = new HashSet<PrefixElement>(imports.length);
    for (ImportElement element : imports) {
      PrefixElement prefix = element.getPrefix();
      if (prefix != null) {
        prefixes.add(prefix);
      }
    }
    return prefixes.toArray(new PrefixElement[prefixes.size()]);
  }

  @Override
  public Source getSource() {
    if (definingCompilationUnit == null) {
      return null;
    }
    return definingCompilationUnit.getSource();
  }

  @Override
  public ClassElement getType(String className) {
    ClassElement type = definingCompilationUnit.getType(className);
    if (type != null) {
      return type;
    }
    for (CompilationUnitElement part : parts) {
      type = part.getType(className);
      if (type != null) {
        return type;
      }
    }
    return null;
  }

  @Override
  public CompilationUnitElement[] getUnits() {
    CompilationUnitElement[] units = new CompilationUnitElement[1 + parts.length];
    units[0] = definingCompilationUnit;
    System.arraycopy(parts, 0, units, 1, parts.length);
    return units;
  }

  @Override
  public LibraryElement[] getVisibleLibraries() {
    Set<LibraryElement> visibleLibraries = Sets.newHashSet();
    addVisibleLibraries(visibleLibraries, false);
    return visibleLibraries.toArray(new LibraryElement[visibleLibraries.size()]);
  }

  @Override
  public boolean hasExtUri() {
    return hasModifier(Modifier.HAS_EXT_URI);
  }

  @Override
  public int hashCode() {
    return definingCompilationUnit.hashCode();
  }

  @Override
  public boolean hasLoadLibraryFunction() {
    if (definingCompilationUnit.hasLoadLibraryFunction()) {
      return true;
    }
    for (int i = 0; i < parts.length; i++) {
      if (parts[i].hasLoadLibraryFunction()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean isAngularHtml() {
    return isAngularHtml;
  }

  @Override
  public boolean isBrowserApplication() {
    return entryPoint != null && isOrImportsBrowserLibrary();
  }

  @Override
  public boolean isDartCore() {
    return getName().equals("dart.core");
  }

  @Override
  public boolean isInSdk() {
    return StringUtilities.startsWith5(getName(), 0, 'd', 'a', 'r', 't', '.');
  }

  @Override
  public boolean isUpToDate(long timeStamp) {
    Set<LibraryElement> visitedLibraries = Sets.newHashSet();

    return safeIsUpToDate(this, timeStamp, visitedLibraries);
  }

  /**
   * Specifies if this library is created for Angular analysis.
   */
  public void setAngularHtml(boolean isAngularHtml) {
    this.isAngularHtml = isAngularHtml;
  }

  /**
   * Set the compilation unit that defines this library to the given compilation unit.
   * 
   * @param definingCompilationUnit the compilation unit that defines this library
   */
  public void setDefiningCompilationUnit(CompilationUnitElement definingCompilationUnit) {
    ((CompilationUnitElementImpl) definingCompilationUnit).setEnclosingElement(this);
    this.definingCompilationUnit = definingCompilationUnit;
  }

  /**
   * Set the entry point for this library to the given function.
   * 
   * @param entryPoint the entry point for this library
   */
  public void setEntryPoint(FunctionElement entryPoint) {
    this.entryPoint = entryPoint;
  }

  /**
   * Set the specifications of all of the exports defined in this library to the given array.
   * 
   * @param exports the specifications of all of the exports defined in this library
   */
  public void setExports(ExportElement[] exports) {
    for (ExportElement exportElement : exports) {
      ((ExportElementImpl) exportElement).setEnclosingElement(this);
    }
    this.exports = exports;
  }

  /**
   * Set whether this library has an import of a "dart-ext" URI to the given value.
   * 
   * @param hasExtUri {@code true} if this library has an import of a "dart-ext" URI
   */
  public void setHasExtUri(boolean hasExtUri) {
    setModifier(Modifier.HAS_EXT_URI, hasExtUri);
  }

  /**
   * Set the specifications of all of the imports defined in this library to the given array.
   * 
   * @param imports the specifications of all of the imports defined in this library
   */
  public void setImports(ImportElement[] imports) {
    for (ImportElement importElement : imports) {
      ((ImportElementImpl) importElement).setEnclosingElement(this);
      PrefixElementImpl prefix = (PrefixElementImpl) importElement.getPrefix();
      if (prefix != null) {
        prefix.setEnclosingElement(this);
      }
    }
    this.imports = imports;
  }

  /**
   * Set the compilation units that are included in this library using a {@code part} directive.
   * 
   * @param parts the compilation units that are included in this library using a {@code part}
   *          directive
   */
  public void setParts(CompilationUnitElement[] parts) {
    for (CompilationUnitElement compilationUnit : parts) {
      ((CompilationUnitElementImpl) compilationUnit).setEnclosingElement(this);
    }
    this.parts = parts;
  }

  @Override
  public void visitChildren(ElementVisitor<?> visitor) {
    super.visitChildren(visitor);
    safelyVisitChild(definingCompilationUnit, visitor);
    safelyVisitChildren(exports, visitor);
    safelyVisitChildren(imports, visitor);
    safelyVisitChildren(parts, visitor);
  }

  @Override
  protected String getIdentifier() {
    return definingCompilationUnit.getSource().getEncoding();
  }

  /**
   * Recursively fills set of visible libraries for {@link #getVisibleElementsLibraries}.
   */
  private void addVisibleLibraries(Set<LibraryElement> visibleLibraries, boolean includeExports) {
    // maybe already processed
    if (!visibleLibraries.add(this)) {
      return;
    }
    // add imported libraries
    for (ImportElement importElement : imports) {
      LibraryElement importedLibrary = importElement.getImportedLibrary();
      if (importedLibrary != null) {
        ((LibraryElementImpl) importedLibrary).addVisibleLibraries(visibleLibraries, true);
      }
    }
    // add exported libraries
    if (includeExports) {
      for (ExportElement exportElement : exports) {
        LibraryElement exportedLibrary = exportElement.getExportedLibrary();
        if (exportedLibrary != null) {
          ((LibraryElementImpl) exportedLibrary).addVisibleLibraries(visibleLibraries, true);
        }
      }
    }
  }

  /**
   * Return the object representing the type "Future" from the dart:async library, or the type
   * "void" if the type "Future" cannot be accessed.
   * 
   * @return the type "Future" from the dart:async library
   */
  private Type getLoadLibraryReturnType() {
    try {
      Source asyncSource = context.getSourceFactory().forUri(DartSdk.DART_ASYNC);
      if (asyncSource == null) {
        AnalysisEngine.getInstance().getLogger().logError(
            "Could not create a source for dart:async");
        return VoidTypeImpl.getInstance();
      }
      LibraryElement asyncElement = context.computeLibraryElement(asyncSource);
      if (asyncElement == null) {
        AnalysisEngine.getInstance().getLogger().logError(
            "Could not build the element model for dart:async");
        return VoidTypeImpl.getInstance();
      }
      ClassElement futureElement = asyncElement.getType("Future");
      if (futureElement == null) {
        AnalysisEngine.getInstance().getLogger().logError(
            "Could not find type Future in dart:async");
        return VoidTypeImpl.getInstance();
      }
      InterfaceType futureType = futureElement.getType();
      return futureType.substitute(new Type[] {DynamicTypeImpl.getInstance()});
    } catch (AnalysisException exception) {
      AnalysisEngine.getInstance().getLogger().logError(
          "Could not build the element model for dart:async",
          exception);
      return VoidTypeImpl.getInstance();
    }
  }

  /**
   * Answer {@code true} if the receiver directly or indirectly imports the dart:html libraries.
   * 
   * @return {@code true} if the receiver directly or indirectly imports the dart:html libraries
   */
  private boolean isOrImportsBrowserLibrary() {
    List<LibraryElement> visited = new ArrayList<LibraryElement>(10);
    Source htmlLibSource = context.getSourceFactory().forUri(DartSdk.DART_HTML);
    visited.add(this);
    for (int index = 0; index < visited.size(); index++) {
      LibraryElement library = visited.get(index);
      Source source = library.getDefiningCompilationUnit().getSource();
      if (source.equals(htmlLibSource)) {
        return true;
      }
      for (LibraryElement importedLibrary : library.getImportedLibraries()) {
        if (!visited.contains(importedLibrary)) {
          visited.add(importedLibrary);
        }
      }
      for (LibraryElement exportedLibrary : library.getExportedLibraries()) {
        if (!visited.contains(exportedLibrary)) {
          visited.add(exportedLibrary);
        }
      }
    }
    return false;
  }
}
