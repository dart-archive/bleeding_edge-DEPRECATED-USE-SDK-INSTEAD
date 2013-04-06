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
package com.google.dart.engine.context;

import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.internal.context.AnalysisContextImpl;
import com.google.dart.engine.internal.element.CompilationUnitElementImpl;
import com.google.dart.engine.internal.element.LibraryElementImpl;
import com.google.dart.engine.internal.resolver.TestTypeProvider;
import com.google.dart.engine.sdk.DartSdk;
import com.google.dart.engine.sdk.DirectoryBasedDartSdk;
import com.google.dart.engine.source.DartUriResolver;
import com.google.dart.engine.source.FileUriResolver;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceFactory;

import static com.google.dart.engine.ast.ASTFactory.libraryIdentifier;

import java.util.HashMap;

/**
 * The class {@code AnalysisContextFactory} defines utility methods used to create analysis contexts
 * for testing purposes.
 */
public final class AnalysisContextFactory {
  /**
   * Create an analysis context that has a fake core library already resolved.
   * 
   * @return the analysis context that was created
   */
  public static AnalysisContextImpl contextWithCore() {
    AnalysisContextImpl context = new AnalysisContextImpl();
    SourceFactory sourceFactory = new SourceFactory(new DartUriResolver(
        DirectoryBasedDartSdk.getDefaultSdk()), new FileUriResolver());
    context.setSourceFactory(sourceFactory);
    //
    // dart:core
    //
    TestTypeProvider provider = new TestTypeProvider();
    CompilationUnitElementImpl coreUnit = new CompilationUnitElementImpl("core.dart");
    Source coreSource = sourceFactory.forUri(DartSdk.DART_CORE);
    coreUnit.setSource(coreSource);
    coreUnit.setTypes(new ClassElement[] {
        provider.getBoolType().getElement(), provider.getDoubleType().getElement(),
        provider.getFunctionType().getElement(), provider.getIntType().getElement(),
        provider.getListType().getElement(), provider.getMapType().getElement(),
        provider.getNumType().getElement(), provider.getObjectType().getElement(),
        provider.getStackTraceType().getElement(), provider.getStringType().getElement(),
        provider.getTypeType().getElement()});
    LibraryElementImpl coreLibrary = new LibraryElementImpl(context, libraryIdentifier(
        "dart",
        "core"));
    coreLibrary.setDefiningCompilationUnit(coreUnit);
    //
    // dart:html
    //
    CompilationUnitElementImpl htmlUnit = new CompilationUnitElementImpl("html_dartium.dart");
    Source htmlSource = sourceFactory.forUri(DartSdk.DART_HTML);
    htmlUnit.setSource(htmlSource);
    LibraryElementImpl htmlLibrary = new LibraryElementImpl(context, libraryIdentifier(
        "dart",
        "dom",
        "html"));
    htmlLibrary.setDefiningCompilationUnit(htmlUnit);

    HashMap<Source, LibraryElement> elementMap = new HashMap<Source, LibraryElement>();
    elementMap.put(coreSource, coreLibrary);
    elementMap.put(htmlSource, htmlLibrary);

    context.setContents(coreSource, "");
    context.setContents(htmlSource, "");
    context.recordLibraryElements(elementMap);
    return context;
  }

  /**
   * Prevent the creation of instances of this class.
   */
  private AnalysisContextFactory() {
  }
}
