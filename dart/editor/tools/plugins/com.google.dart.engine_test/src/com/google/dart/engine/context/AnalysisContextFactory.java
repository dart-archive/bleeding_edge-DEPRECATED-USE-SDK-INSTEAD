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
import com.google.dart.engine.element.ConstructorElement;
import com.google.dart.engine.element.FunctionElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.element.ParameterElement;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.element.TopLevelVariableElement;
import com.google.dart.engine.internal.context.AnalysisContextImpl;
import com.google.dart.engine.internal.context.TimestampedData;
import com.google.dart.engine.internal.element.ClassElementImpl;
import com.google.dart.engine.internal.element.CompilationUnitElementImpl;
import com.google.dart.engine.internal.element.ConstructorElementImpl;
import com.google.dart.engine.internal.element.FunctionTypeAliasElementImpl;
import com.google.dart.engine.internal.element.LibraryElementImpl;
import com.google.dart.engine.internal.element.ParameterElementImpl;
import com.google.dart.engine.internal.element.TopLevelVariableElementImpl;
import com.google.dart.engine.internal.resolver.TestTypeProvider;
import com.google.dart.engine.internal.sdk.LibraryMap;
import com.google.dart.engine.internal.sdk.SdkLibraryImpl;
import com.google.dart.engine.internal.type.FunctionTypeImpl;
import com.google.dart.engine.sdk.DartSdk;
import com.google.dart.engine.sdk.DirectoryBasedDartSdk;
import com.google.dart.engine.source.DartUriResolver;
import com.google.dart.engine.source.FileUriResolver;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceFactory;
import com.google.dart.engine.type.InterfaceType;
import com.google.dart.engine.type.Type;
import com.google.dart.engine.utilities.dart.ParameterKind;

import static com.google.dart.engine.ast.AstFactory.libraryIdentifier;
import static com.google.dart.engine.element.ElementFactory.classElement;
import static com.google.dart.engine.element.ElementFactory.constructorElement;
import static com.google.dart.engine.element.ElementFactory.functionElement;
import static com.google.dart.engine.element.ElementFactory.getterElement;
import static com.google.dart.engine.element.ElementFactory.methodElement;
import static com.google.dart.engine.element.ElementFactory.methodElementWithParameters;
import static com.google.dart.engine.element.ElementFactory.namedParameter;
import static com.google.dart.engine.element.ElementFactory.positionalParameter;
import static com.google.dart.engine.element.ElementFactory.requiredParameter;
import static com.google.dart.engine.element.ElementFactory.topLevelVariableElement;

import junit.framework.Assert;

import java.io.File;
import java.util.HashMap;

/**
 * The class {@code AnalysisContextFactory} defines utility methods used to create analysis contexts
 * for testing purposes.
 */
public final class AnalysisContextFactory {
  /**
   * Instances of the class {@code AnalysisContextForTests} implement an analysis context that has a
   * fake SDK that is much smaller and faster for testing purposes.
   */
  public static class AnalysisContextForTests extends AnalysisContextImpl {
    @Override
    public boolean exists(Source source) {
      return super.exists(source) || getSourceFactory().getDartSdk().getContext().exists(source);
    }

    @Override
    public TimestampedData<CharSequence> getContents(Source source) throws Exception {
      if (source.isInSystemLibrary()) {
        return getSourceFactory().getDartSdk().getContext().getContents(source);
      }
      return super.getContents(source);
    }

    @Override
    public long getModificationStamp(Source source) {
      if (source.isInSystemLibrary()) {
        return getSourceFactory().getDartSdk().getContext().getModificationStamp(source);
      }
      return super.getModificationStamp(source);
    }

    @Override
    public void setAnalysisOptions(AnalysisOptions options) {
      AnalysisOptions currentOptions = getAnalysisOptions();
      boolean needsRecompute = currentOptions.getAnalyzeFunctionBodies() != options.getAnalyzeFunctionBodies()
          || currentOptions.getGenerateSdkErrors() != options.getGenerateSdkErrors()
          || currentOptions.getEnableAsync() != options.getEnableAsync()
          || currentOptions.getEnableDeferredLoading() != options.getEnableDeferredLoading()
          || currentOptions.getEnableEnum() != options.getEnableEnum()
          || currentOptions.getDart2jsHint() != options.getDart2jsHint()
          || (currentOptions.getHint() && !options.getHint())
          || currentOptions.getPreserveComments() != options.getPreserveComments();
      if (needsRecompute) {
        Assert.fail("Cannot set options that cause the sources to be reanalyzed in a test context");
      }
      super.setAnalysisOptions(options);
    }

    /**
     * Set the analysis options, even if they would force re-analysis. This method should only be
     * invoked before the fake SDK is initialized.
     * 
     * @param options the analysis options to be set
     */
    private void internalSetAnalysisOptions(AnalysisOptions options) {
      super.setAnalysisOptions(options);
    }
  }

  private static final String DART_MATH = "dart:math";
  private static final String DART_INTERCEPTORS = "dart:_interceptors";
  private static final String DART_JS_HELPER = "dart:_js_helper";

  /**
   * The fake SDK used by all of the contexts created by this factory.
   */
  private static DirectoryBasedDartSdk FAKE_SDK;

  /**
   * Create an analysis context that has a fake core library already resolved.
   * 
   * @return the analysis context that was created
   */
  public static AnalysisContextImpl contextWithCore() {
    AnalysisContextForTests context = new AnalysisContextForTests();
    return initContextWithCore(context);
  }

  /**
   * Create an analysis context that uses the given options and has a fake core library already
   * resolved.
   * 
   * @param options the options to be applied to the context
   * @return the analysis context that was created
   */
  public static AnalysisContextImpl contextWithCoreAndOptions(AnalysisOptions options) {
    AnalysisContextForTests context = new AnalysisContextForTests();
    context.internalSetAnalysisOptions(options);
    return initContextWithCore(context);
  }

  /**
   * Initialize the given analysis context with a fake core library already resolved.
   * 
   * @param context the context to be initialized (not {@code null})
   * @return the analysis context that was created
   */
  public static AnalysisContextImpl initContextWithCore(AnalysisContextImpl context) {
    DirectoryBasedDartSdk sdk = new DirectoryBasedDartSdk(new File("/fake/sdk")) {
      @Override
      protected LibraryMap initialLibraryMap(boolean useDart2jsPaths) {
        LibraryMap map = new LibraryMap();
        addLibrary(map, DART_ASYNC, false, "async.dart");
        addLibrary(map, DART_CORE, false, "core.dart");
        addLibrary(map, DART_HTML, false, "html_dartium.dart");
        addLibrary(map, DART_MATH, false, "math.dart");
        addLibrary(map, DART_INTERCEPTORS, true, "_interceptors.dart");
        addLibrary(map, DART_JS_HELPER, true, "_js_helper.dart");
        return map;
      }

      private void addLibrary(LibraryMap map, String uri, boolean isInternal, String path) {
        SdkLibraryImpl library = new SdkLibraryImpl(uri);
        if (isInternal) {
          library.setCategory("Internal");
        }
        library.setPath(path);
        map.setLibrary(uri, library);
      }
    };
    SourceFactory sourceFactory = new SourceFactory(new DartUriResolver(sdk), new FileUriResolver());
    context.setSourceFactory(sourceFactory);
    AnalysisContext coreContext = sdk.getContext();
    //
    // dart:core
    //
    TestTypeProvider provider = new TestTypeProvider();
    CompilationUnitElementImpl coreUnit = new CompilationUnitElementImpl("core.dart");
    Source coreSource = sourceFactory.forUri(DartSdk.DART_CORE);
    coreContext.setContents(coreSource, "");
    coreUnit.setSource(coreSource);
    ClassElementImpl proxyClassElement = classElement("_Proxy");
    coreUnit.setTypes(new ClassElement[] {
        provider.getBoolType().getElement(), provider.getDeprecatedType().getElement(),
        provider.getDoubleType().getElement(), provider.getFunctionType().getElement(),
        provider.getIntType().getElement(), provider.getIterableType().getElement(),
        provider.getListType().getElement(), provider.getMapType().getElement(),
        provider.getNullType().getElement(), provider.getNumType().getElement(),
        provider.getObjectType().getElement(), proxyClassElement,
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
        proxyClassElement.getType());
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
    LibraryElementImpl coreLibrary = new LibraryElementImpl(coreContext, libraryIdentifier(
        "dart",
        "core"));
    coreLibrary.setDefiningCompilationUnit(coreUnit);
    //
    // dart:async
    //
    CompilationUnitElementImpl asyncUnit = new CompilationUnitElementImpl("async.dart");
    Source asyncSource = sourceFactory.forUri(DartSdk.DART_ASYNC);
    coreContext.setContents(asyncSource, "");
    asyncUnit.setSource(asyncSource);
    // Future
    ClassElementImpl futureElement = classElement("Future", "T");
    InterfaceType futureType = futureElement.getType();
    //   factory Future.value([value])
    ConstructorElementImpl futureConstructor = constructorElement(futureElement, "value");
    futureConstructor.setParameters(new ParameterElement[] {positionalParameter(
        "value",
        provider.getDynamicType())});
    futureConstructor.setFactory(true);
    ((FunctionTypeImpl) futureConstructor.getType()).setTypeArguments(futureElement.getType().getTypeArguments());
    futureElement.setConstructors(new ConstructorElement[] {futureConstructor});
    //   Future then(onValue(T value), { Function onError });
    ParameterElement[] parameters = new ParameterElement[] {requiredParameter(
        "value",
        futureElement.getTypeParameters()[0].getType())};
    FunctionTypeAliasElementImpl aliasElement = new FunctionTypeAliasElementImpl(null);
    aliasElement.setSynthetic(true);
    aliasElement.shareParameters(parameters);
    aliasElement.setReturnType(provider.getDynamicType());
    FunctionTypeImpl aliasType = new FunctionTypeImpl(aliasElement);
    aliasElement.shareTypeParameters(futureElement.getTypeParameters());
    aliasType.setTypeArguments(futureElement.getType().getTypeArguments());
    MethodElement thenMethod = methodElementWithParameters(
        "then",
        futureElement.getType().getTypeArguments(),
        futureType,
        requiredParameter("onValue", aliasType),
        namedParameter("onError", provider.getFunctionType()));

    futureElement.setMethods(new MethodElement[] {thenMethod});
    // Completer
    ClassElementImpl completerElement = classElement("Completer", "T");
    ConstructorElementImpl completerConstructor = constructorElement(completerElement, null);
    ((FunctionTypeImpl) completerConstructor.getType()).setTypeArguments(completerElement.getType().getTypeArguments());
    completerElement.setConstructors(new ConstructorElement[] {completerConstructor});

    asyncUnit.setTypes(new ClassElement[] {
        completerElement, futureElement, classElement("Stream", "T")});
    LibraryElementImpl asyncLibrary = new LibraryElementImpl(coreContext, libraryIdentifier(
        "dart",
        "async"));
    asyncLibrary.setDefiningCompilationUnit(asyncUnit);
    //
    // dart:html
    //
    CompilationUnitElementImpl htmlUnit = new CompilationUnitElementImpl("html_dartium.dart");
    Source htmlSource = sourceFactory.forUri(DartSdk.DART_HTML);
    coreContext.setContents(htmlSource, "");
    htmlUnit.setSource(htmlSource);
    ClassElementImpl elementElement = classElement("Element");
    InterfaceType elementType = elementElement.getType();
    ClassElementImpl canvasElement = classElement("CanvasElement", elementType);
    ClassElementImpl contextElement = classElement("CanvasRenderingContext");
    InterfaceType contextElementType = contextElement.getType();
    ClassElementImpl context2dElement = classElement("CanvasRenderingContext2D", contextElementType);
    canvasElement.setMethods(new MethodElement[] {methodElement(
        "getContext",
        contextElementType,
        provider.getStringType())});
    canvasElement.setAccessors(new PropertyAccessorElement[] {getterElement(
        "context2D",
        false,
        context2dElement.getType())});
    ClassElementImpl documentElement = classElement("Document", elementType);
    ClassElementImpl htmlDocumentElement = classElement("HtmlDocument", documentElement.getType());
    htmlDocumentElement.setMethods(new MethodElement[] {methodElement(
        "query",
        elementType,
        new Type[] {provider.getStringType()})});
    htmlUnit.setTypes(new ClassElement[] {
        classElement("AnchorElement", elementType), classElement("BodyElement", elementType),
        classElement("ButtonElement", elementType), canvasElement, contextElement,
        context2dElement, classElement("DivElement", elementType), documentElement, elementElement,
        htmlDocumentElement, classElement("InputElement", elementType),
        classElement("SelectElement", elementType)});
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
    LibraryElementImpl htmlLibrary = new LibraryElementImpl(coreContext, libraryIdentifier(
        "dart",
        "dom",
        "html"));
    htmlLibrary.setDefiningCompilationUnit(htmlUnit);
    //
    // dart:math
    //
    CompilationUnitElementImpl mathUnit = new CompilationUnitElementImpl("math.dart");
    Source mathSource = sourceFactory.forUri(DART_MATH);
    coreContext.setContents(mathSource, "");
    mathUnit.setSource(mathSource);

    FunctionElement cosElement = functionElement(
        "cos",
        provider.getDoubleType().getElement(),
        new ClassElement[] {provider.getNumType().getElement()},
        new ClassElement[] {});
    TopLevelVariableElement ln10Element = topLevelVariableElement(
        "LN10",
        true,
        false,
        provider.getDoubleType());
    TopLevelVariableElement piElement = topLevelVariableElement(
        "PI",
        true,
        false,
        provider.getDoubleType());
    ClassElementImpl randomElement = classElement("Random");
    randomElement.setAbstract(true);
    ConstructorElementImpl randomConstructor = constructorElement(randomElement, null);
    randomConstructor.setFactory(true);
    ParameterElementImpl seedParam = new ParameterElementImpl("seed", 0);
    seedParam.setParameterKind(ParameterKind.POSITIONAL);
    seedParam.setType(provider.getIntType());
    randomConstructor.setParameters(new ParameterElement[] {seedParam});
    randomElement.setConstructors(new ConstructorElement[] {randomConstructor});
    FunctionElement sinElement = functionElement(
        "sin",
        provider.getDoubleType().getElement(),
        new ClassElement[] {provider.getNumType().getElement()},
        new ClassElement[] {});
    FunctionElement sqrtElement = functionElement(
        "sqrt",
        provider.getDoubleType().getElement(),
        new ClassElement[] {provider.getNumType().getElement()},
        new ClassElement[] {});

    mathUnit.setAccessors(new PropertyAccessorElement[] {
        ln10Element.getGetter(), piElement.getGetter()});
    mathUnit.setFunctions(new FunctionElement[] {cosElement, sinElement, sqrtElement});
    mathUnit.setTopLevelVariables(new TopLevelVariableElement[] {ln10Element, piElement});
    mathUnit.setTypes(new ClassElement[] {randomElement});

    LibraryElementImpl mathLibrary = new LibraryElementImpl(coreContext, libraryIdentifier(
        "dart",
        "math"));
    mathLibrary.setDefiningCompilationUnit(mathUnit);
    //
    // Set empty sources for the rest of the libraries.
    //
    Source source = sourceFactory.forUri(DART_INTERCEPTORS);
    coreContext.setContents(source, "");
    source = sourceFactory.forUri(DART_JS_HELPER);
    coreContext.setContents(source, "");
    //
    // Record the elements.
    //
    HashMap<Source, LibraryElement> elementMap = new HashMap<Source, LibraryElement>();
    elementMap.put(coreSource, coreLibrary);
    elementMap.put(asyncSource, asyncLibrary);
    elementMap.put(htmlSource, htmlLibrary);
    elementMap.put(mathSource, mathLibrary);
    context.recordLibraryElements(elementMap);
    return context;
  }

  /**
   * Prevent the creation of instances of this class.
   */
  private AnalysisContextFactory() {
  }
}
