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
package com.google.dart.engine.internal.element.handle;

import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.ElementKind;
import com.google.dart.engine.element.ExportElement;
import com.google.dart.engine.element.FunctionElement;
import com.google.dart.engine.element.ImportElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.PrefixElement;

/**
 * Instances of the class {@code LibraryElementHandle} implement a handle to a
 * {@code LibraryElement}.
 * 
 * @coverage dart.engine.element
 */
public class LibraryElementHandle extends ElementHandle implements LibraryElement {
  /**
   * Initialize a newly created element handle to represent the given element.
   * 
   * @param element the element being represented
   */
  public LibraryElementHandle(LibraryElement element) {
    super(element);
  }

  @Override
  public CompilationUnitElement getDefiningCompilationUnit() {
    return getActualElement().getDefiningCompilationUnit();
  }

  @Override
  public FunctionElement getEntryPoint() {
    return getActualElement().getEntryPoint();
  }

  @Override
  public LibraryElement[] getExportedLibraries() {
    return getActualElement().getExportedLibraries();
  }

  @Override
  public ExportElement[] getExports() {
    return getActualElement().getExports();
  }

  @Override
  public LibraryElement[] getImportedLibraries() {
    return getActualElement().getImportedLibraries();
  }

  @Override
  public ImportElement[] getImports() {
    return getActualElement().getImports();
  }

  @Override
  public ImportElement[] getImportsWithPrefix(PrefixElement prefixElement) {
    return getActualElement().getImportsWithPrefix(prefixElement);
  }

  @Override
  public ElementKind getKind() {
    return ElementKind.LIBRARY;
  }

  @Override
  public FunctionElement getLoadLibraryFunction() {
    return getActualElement().getLoadLibraryFunction();
  }

  @Override
  public CompilationUnitElement[] getParts() {
    return getActualElement().getParts();
  }

  @Override
  public PrefixElement[] getPrefixes() {
    return getActualElement().getPrefixes();
  }

  @Override
  public ClassElement getType(String className) {
    return getActualElement().getType(className);
  }

  @Override
  public CompilationUnitElement[] getUnits() {
    return getActualElement().getUnits();
  }

  @Override
  public LibraryElement[] getVisibleLibraries() {
    return getActualElement().getVisibleLibraries();
  }

  @Override
  public boolean hasExtUri() {
    return getActualElement().hasExtUri();
  }

  @Override
  public boolean hasLoadLibraryFunction() {
    return getActualElement().hasLoadLibraryFunction();
  }

  @Override
  public boolean isAngularHtml() {
    return getActualElement().isAngularHtml();
  }

  @Override
  public boolean isBrowserApplication() {
    return getActualElement().isBrowserApplication();
  }

  @Override
  public boolean isDartCore() {
    return getActualElement().isDartCore();
  }

  @Override
  public boolean isInSdk() {
    return getActualElement().isInSdk();
  }

  @Override
  public boolean isUpToDate(long timeStamp) {
    return getActualElement().isUpToDate(timeStamp);
  }

  @Override
  protected LibraryElement getActualElement() {
    return (LibraryElement) super.getActualElement();
  }
}
