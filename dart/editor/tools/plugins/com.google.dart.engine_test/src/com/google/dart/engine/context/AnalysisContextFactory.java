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
import com.google.dart.engine.element.FunctionElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.element.TopLevelVariableElement;
import com.google.dart.engine.internal.context.AnalysisContextImpl;
import com.google.dart.engine.internal.context.DelegatingAnalysisContextImpl;
import com.google.dart.engine.internal.element.ClassElementImpl;
import com.google.dart.engine.internal.element.CompilationUnitElementImpl;
import com.google.dart.engine.internal.element.LibraryElementImpl;
import com.google.dart.engine.internal.element.TopLevelVariableElementImpl;
import com.google.dart.engine.internal.resolver.TestTypeProvider;
import com.google.dart.engine.sdk.DartSdk;
import com.google.dart.engine.sdk.DirectoryBasedDartSdk;
import com.google.dart.engine.source.DartUriResolver;
import com.google.dart.engine.source.FileUriResolver;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceFactory;
import com.google.dart.engine.type.InterfaceType;
import com.google.dart.engine.type.Type;

import static com.google.dart.engine.ast.AstFactory.libraryIdentifier;
import static com.google.dart.engine.element.ElementFactory.classElement;
import static com.google.dart.engine.element.ElementFactory.functionElement;
import static com.google.dart.engine.element.ElementFactory.methodElement;
import static com.google.dart.engine.element.ElementFactory.topLevelVariableElement;

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
    return initContextWithCore(new DelegatingAnalysisContextImpl());
  }

  /**
   * Initialize the given analysis context with a fake core library already resolved.
   * 
   * @param context the context to be initialized (not {@code null})
   * @return the analysis context that was created
   */
  public static AnalysisContextImpl initContextWithCore(AnalysisContextImpl context) {
    AnalysisContext sdkContext = DirectoryBasedDartSdk.getDefaultSdk().getContext();
    SourceFactory sourceFactory = sdkContext.getSourceFactory();
    //
    // dart:core
    //
    TestTypeProvider provider = new TestTypeProvider();
    CompilationUnitElementImpl coreUnit = new CompilationUnitElementImpl("core.dart");
    Source coreSource = sourceFactory.forUri(DartSdk.DART_CORE);
    sdkContext.setContents(coreSource, "");
    coreUnit.setSource(coreSource);
    coreUnit.setTypes(new ClassElement[] {
        provider.getBoolType().getElement(), provider.getDeprecatedType().getElement(),
        provider.getDoubleType().getElement(), provider.getFunctionType().getElement(),
        provider.getIntType().getElement(), provider.getListType().getElement(),
        provider.getMapType().getElement(), provider.getNullType().getElement(),
        provider.getNumType().getElement(), provider.getObjectType().getElement(),
        provider.getStackTraceType().getElement(), provider.getStringType().getElement(),
        provider.getSymbolType().getElement(), provider.getTypeType().getElement()});
    coreUnit.setFunctions(new FunctionElement[] {functionElement(
        "identical",
        provider.getBoolType().getElement(),
        new ClassElement[] {
            provider.getObjectType().getElement(), provider.getObjectType().getElement()},
        null)});
    TopLevelVariableElement proxyTopLevelVariableElt = topLevelVariableElement(
        "proxy",
        true,
        false,
        classElement("_Proxy").getType());
    TopLevelVariableElement deprecatedTopLevelVariableElt = topLevelVariableElement(
        "deprecated",
        true,
        false,
        provider.getDeprecatedType());
    coreUnit.setAccessors(new PropertyAccessorElement[] {
        proxyTopLevelVariableElt.getGetter(), proxyTopLevelVariableElt.getSetter(),
        deprecatedTopLevelVariableElt.getGetter(), deprecatedTopLevelVariableElt.getSetter()});
    coreUnit.setTopLevelVariables(new TopLevelVariableElement[] {
        proxyTopLevelVariableElt, deprecatedTopLevelVariableElt});
    LibraryElementImpl coreLibrary = new LibraryElementImpl(sdkContext, libraryIdentifier(
        "dart",
        "core"));
    coreLibrary.setDefiningCompilationUnit(coreUnit);
    //
    // dart:html
    //
    CompilationUnitElementImpl htmlUnit = new CompilationUnitElementImpl("html_dartium.dart");
    Source htmlSource = sourceFactory.forUri(DartSdk.DART_HTML);
    sdkContext.setContents(htmlSource, "");
    htmlUnit.setSource(htmlSource);
    ClassElementImpl elementElement = classElement("Element");
    InterfaceType elementType = elementElement.getType();
    ClassElementImpl documentElement = classElement("Document", elementType);
    ClassElementImpl htmlDocumentElement = classElement("HtmlDocument", documentElement.getType());
    htmlDocumentElement.setMethods(new MethodElement[] {methodElement(
        "query",
        elementType,
        new Type[] {provider.getStringType()})});
    htmlUnit.setTypes(new ClassElement[] {
        classElement("AnchorElement", elementType), classElement("BodyElement", elementType),
        classElement("ButtonElement", elementType), classElement("DivElement", elementType),
        documentElement, elementElement, htmlDocumentElement,
        classElement("InputElement", elementType), classElement("SelectElement", elementType),});
    htmlUnit.setFunctions(new FunctionElement[] {functionElement(
        "query",
        elementElement,
        new ClassElement[] {provider.getStringType().getElement()},
        ClassElementImpl.EMPTY_ARRAY)});
    TopLevelVariableElementImpl document = topLevelVariableElement(
        "document",
        false,
        true,
        htmlDocumentElement.getType());
    htmlUnit.setTopLevelVariables(new TopLevelVariableElement[] {document});
    htmlUnit.setAccessors(new PropertyAccessorElement[] {document.getGetter()});
    LibraryElementImpl htmlLibrary = new LibraryElementImpl(sdkContext, libraryIdentifier(
        "dart",
        "dom",
        "html"));
    htmlLibrary.setDefiningCompilationUnit(htmlUnit);

    HashMap<Source, LibraryElement> elementMap = new HashMap<Source, LibraryElement>();
    elementMap.put(coreSource, coreLibrary);
    elementMap.put(htmlSource, htmlLibrary);
    ((AnalysisContextImpl) sdkContext).recordLibraryElements(elementMap);

    sourceFactory = new SourceFactory(new DartUriResolver(
        sdkContext.getSourceFactory().getDartSdk()), new FileUriResolver());
    context.setSourceFactory(sourceFactory);
    return context;
  }

  /**
   * Prevent the creation of instances of this class.
   */
  private AnalysisContextFactory() {
  }
}
