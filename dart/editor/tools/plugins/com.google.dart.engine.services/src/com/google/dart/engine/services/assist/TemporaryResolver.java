package com.google.dart.engine.services.assist;

import com.google.dart.engine.ast.Annotation;
import com.google.dart.engine.ast.LibraryDirective;
import com.google.dart.engine.ast.LibraryIdentifier;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.FieldElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.element.ParameterElement;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.AnalysisErrorListener;
import com.google.dart.engine.internal.context.AnalysisContextImpl;
import com.google.dart.engine.internal.element.ClassElementImpl;
import com.google.dart.engine.internal.element.CompilationUnitElementImpl;
import com.google.dart.engine.internal.element.FieldElementImpl;
import com.google.dart.engine.internal.element.LibraryElementImpl;
import com.google.dart.engine.internal.element.MethodElementImpl;
import com.google.dart.engine.internal.element.ParameterElementImpl;
import com.google.dart.engine.internal.element.PropertyAccessorElementImpl;
import com.google.dart.engine.internal.element.TypeVariableElementImpl;
import com.google.dart.engine.internal.resolver.LibraryResolver;
import com.google.dart.engine.internal.resolver.TypeProvider;
import com.google.dart.engine.internal.type.BottomTypeImpl;
import com.google.dart.engine.internal.type.DynamicTypeImpl;
import com.google.dart.engine.internal.type.FunctionTypeImpl;
import com.google.dart.engine.internal.type.InterfaceTypeImpl;
import com.google.dart.engine.internal.type.TypeVariableTypeImpl;
import com.google.dart.engine.internal.type.VoidTypeImpl;
import com.google.dart.engine.scanner.Keyword;
import com.google.dart.engine.scanner.KeywordToken;
import com.google.dart.engine.scanner.StringToken;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.scanner.TokenType;
import com.google.dart.engine.sdk.DartSdk;
import com.google.dart.engine.source.DartUriResolver;
import com.google.dart.engine.source.FileUriResolver;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceFactory;
import com.google.dart.engine.type.InterfaceType;
import com.google.dart.engine.type.Type;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Hacked-up resolver that allows testing in the editor. Mostly copied from test code.
 * <p>
 * TODO: Delete this class ASAP.
 */
public class TemporaryResolver {

  static class AnalysisContextFactory {

    public static AnalysisContextImpl contextWithCore() {
      AnalysisContextImpl context = new AnalysisContextImpl();
      SourceFactory sourceFactory = new SourceFactory(
          new DartUriResolver(DartSdk.getDefaultSdk()),
          new FileUriResolver());
      context.setSourceFactory(sourceFactory);

      SimpleTypeProvider provider = new SimpleTypeProvider();

      CompilationUnitElementImpl unit = new CompilationUnitElementImpl("core.dart");
      unit.setTypes(new ClassElement[] {
          provider.getBoolType().getElement(), provider.getDoubleType().getElement(),
          provider.getIntType().getElement(), provider.getListType().getElement(),
          provider.getMapType().getElement(), provider.getObjectType().getElement(),
          provider.getStackTraceType().getElement(), provider.getStringType().getElement(),
          provider.getTypeType().getElement()});
      LibraryElementImpl library = new LibraryElementImpl(context, libraryIdentifier("core"));
      library.setDefiningCompilationUnit(unit);

      HashMap<Source, LibraryElement> elementMap = new HashMap<Source, LibraryElement>();
      Source coreSource = sourceFactory.forUri("dart:core");
      elementMap.put(coreSource, library);
      context.recordLibraryElements(elementMap);

      unit.setSource(coreSource);

      return context;
    }

    private AnalysisContextFactory() {
    }
  }

  static class SimpleTypeProvider implements TypeProvider {
    private InterfaceType boolType;
    private Type bottomType;
    private InterfaceType doubleType;
    private Type dynamicType;
    private InterfaceType functionType;
    private InterfaceType intType;
    private InterfaceType listType;
    private InterfaceType mapType;
    private InterfaceType numType;
    private InterfaceType objectType;
    private InterfaceType stackTraceType;
    private InterfaceType stringType;
    private InterfaceType typeType;

    SimpleTypeProvider() {
      super();
    }

    @Override
    public InterfaceType getBoolType() {
      if (boolType == null) {
        boolType = classElement("bool").getType();
      }
      return boolType;
    }

    @Override
    public Type getBottomType() {
      if (bottomType == null) {
        bottomType = BottomTypeImpl.getInstance();
      }
      return bottomType;
    }

    @Override
    public InterfaceType getDoubleType() {
      if (doubleType == null) {
        initializeNumericTypes();
      }
      return doubleType;
    }

    @Override
    public Type getDynamicType() {
      if (dynamicType == null) {
        dynamicType = DynamicTypeImpl.getInstance();
      }
      return dynamicType;
    }

    @Override
    public InterfaceType getFunctionType() {
      if (functionType == null) {
        functionType = classElement("Function").getType();
      }
      return functionType;
    }

    @Override
    public InterfaceType getIntType() {
      if (intType == null) {
        initializeNumericTypes();
      }
      return intType;
    }

    @Override
    public InterfaceType getListType() {
      if (listType == null) {
        listType = classElement("List", "E").getType();
      }
      return listType;
    }

    @Override
    public InterfaceType getMapType() {
      if (mapType == null) {
        mapType = classElement("Map", "K", "V").getType();
      }
      return mapType;
    }

    public InterfaceType getNumType() {
      if (numType == null) {
        initializeNumericTypes();
      }
      return numType;
    }

    @Override
    public InterfaceType getObjectType() {
      if (objectType == null) {
        objectType = getObject().getType();
      }
      return objectType;
    }

    @Override
    public InterfaceType getStackTraceType() {
      if (stackTraceType == null) {
        stackTraceType = classElement("StackTrace").getType();
      }
      return stackTraceType;
    }

    @Override
    public InterfaceType getStringType() {
      if (stringType == null) {
        stringType = classElement("String").getType();
      }
      return stringType;
    }

    @Override
    public InterfaceType getTypeType() {
      if (typeType == null) {
        typeType = classElement("Type").getType();
      }
      return typeType;
    }

    private void initializeNumericTypes() {
      //
      // Create the type hierarchy.
      //
      ClassElementImpl numElement = (ClassElementImpl) classElement("num");
      numType = numElement.getType();

      ClassElementImpl intElement = (ClassElementImpl) classElement("int", numType);
      intType = intElement.getType();

      ClassElementImpl doubleElement = (ClassElementImpl) classElement("double", numType);
      doubleType = doubleElement.getType();
      //
      // Add the methods.
      //
      numElement.setMethods(new MethodElement[] {
          methodElement("+", numType, numType), methodElement("-", numType, numType),
          methodElement("*", numType, numType), methodElement("%", numType, numType),
          methodElement("/", doubleType, numType), methodElement("~/", numType, numType),
          methodElement("-", numType), methodElement("remainder", numType, numType),
          methodElement("<", boolType, numType), methodElement("<=", boolType, numType),
          methodElement(">", boolType, numType), methodElement(">=", boolType, numType),
          methodElement("isNaN", boolType), methodElement("isNegative", boolType),
          methodElement("isInfinite", boolType), methodElement("abs", numType),
          methodElement("floor", numType), methodElement("ceil", numType),
          methodElement("round", numType), methodElement("truncate", numType),
          methodElement("toInt", intType), methodElement("toDouble", doubleType),
          methodElement("toStringAsFixed", stringType, intType),
          methodElement("toStringAsExponential", stringType, intType),
          methodElement("toStringAsPrecision", stringType, intType),
          methodElement("toRadixString", stringType, intType),});
      intElement.setMethods(new MethodElement[] {
          methodElement("&", intType, intType),
          methodElement("|", intType, intType),
          methodElement("^", intType, intType),
          methodElement("~", intType),
          methodElement("<<", intType, intType),
          methodElement(">>", intType, intType),
//        getterElement("isEven", boolType),
//        getterElement("isOdd", boolType),
          methodElement("-", intType), methodElement("abs", intType),
          methodElement("round", intType), methodElement("floor", intType),
          methodElement("ceil", intType), methodElement("truncate", intType),
          methodElement("toString", stringType),
//        methodElement(/*external static*/ "parse", intType, stringType),
      });
      doubleElement.setFields(new FieldElement[] {
          fieldElement("NAN", true, false, true, doubleType), // 0.0 / 0.0
          fieldElement("INFINITY", true, false, true, doubleType), // 1.0 / 0.0
          fieldElement("NEGATIVE_INFINITY", true, false, true, doubleType), // -INFINITY
          fieldElement("MIN_POSITIVE", true, false, true, doubleType), // 5e-324
          fieldElement("MAX_FINITE", true, false, true, doubleType), // 1.7976931348623157e+308;
      });
      doubleElement.setMethods(new MethodElement[] {
          methodElement("remainder", doubleType, numType), methodElement("+", doubleType, numType),
          methodElement("-", doubleType, numType), methodElement("*", doubleType, numType),
          methodElement("%", doubleType, numType), methodElement("/", doubleType, numType),
          methodElement("~/", doubleType, numType), methodElement("-", doubleType),
          methodElement("abs", doubleType), methodElement("round", doubleType),
          methodElement("floor", doubleType), methodElement("ceil", doubleType),
          methodElement("truncate", doubleType), methodElement("toString", stringType),
//        methodElement(/*external static*/ "parse", doubleType, stringType),
      });
    }
  }

  private static ClassElement objectElement;

  static ClassElement classElement(String typeName, InterfaceType superclassType,
      String... parameterNames) {
    ClassElementImpl element = new ClassElementImpl(identifier(typeName));
    element.setSupertype(superclassType);
    InterfaceTypeImpl type = new InterfaceTypeImpl(element);
    element.setType(type);

    int count = parameterNames.length;
    if (count > 0) {
      TypeVariableElementImpl[] typeVariables = new TypeVariableElementImpl[count];
      TypeVariableTypeImpl[] typeArguments = new TypeVariableTypeImpl[count];
      for (int i = 0; i < count; i++) {
        TypeVariableElementImpl variable = new TypeVariableElementImpl(
            identifier(parameterNames[i]));
        typeVariables[i] = variable;
        typeArguments[i] = new TypeVariableTypeImpl(variable);
        variable.setType(typeArguments[i]);
      }
      element.setTypeVariables(typeVariables);
      type.setTypeArguments(typeArguments);
    }

    return element;
  }

  static ClassElement classElement(String typeName, String... parameterNames) {
    return classElement(typeName, getObject().getType(), parameterNames);
  }

  static File createFile(String path) {
    return new File(convertPath(path)).getAbsoluteFile();
  }

  static FieldElement fieldElement(String name, boolean isStatic, boolean isFinal, boolean isConst,
      Type type) {
    FieldElementImpl field = new FieldElementImpl(identifier(name));
    field.setConst(isConst);
    field.setFinal(isFinal);
    field.setStatic(isStatic);
    field.setType(type);

    PropertyAccessorElementImpl getter = new PropertyAccessorElementImpl(field);
    getter.setGetter(true);
    getter.setSynthetic(true);
    field.setGetter(getter);

    FunctionTypeImpl getterType = new FunctionTypeImpl(getter);
    getterType.setReturnType(type);
    getter.setType(getterType);

    if (!isConst && !isFinal) {
      PropertyAccessorElementImpl setter = new PropertyAccessorElementImpl(field);
      setter.setSetter(true);
      setter.setSynthetic(true);
      field.setSetter(setter);

      FunctionTypeImpl setterType = new FunctionTypeImpl(getter);
      setterType.setNormalParameterTypes(new Type[] {type});
      setterType.setReturnType(VoidTypeImpl.getInstance());
      setter.setType(setterType);
    }

    return field;
  }

  static ClassElement getObject() {
    if (objectElement == null) {
      objectElement = classElement("Object", (InterfaceType) null);
    }
    return objectElement;
  }

  static SimpleIdentifier identifier(String lexeme) {
    return new SimpleIdentifier(token(TokenType.IDENTIFIER, lexeme));
  }

  static LibraryDirective libraryDirective(List<Annotation> metadata, LibraryIdentifier libraryName) {
    return new LibraryDirective(
        null,
        metadata,
        token(Keyword.LIBRARY),
        libraryName,
        token(TokenType.SEMICOLON));
  }

  static LibraryDirective libraryDirective(String libraryName) {
    return libraryDirective(new ArrayList<Annotation>(), libraryIdentifier(libraryName));
  }

  static LibraryIdentifier libraryIdentifier(String... components) {
    ArrayList<SimpleIdentifier> componentList = new ArrayList<SimpleIdentifier>();
    for (String component : components) {
      componentList.add(identifier(component));
    }
    return new LibraryIdentifier(componentList);
  }

  static MethodElement methodElement(String methodName, Type returnType, Type... argumentTypes) {
    MethodElementImpl method = new MethodElementImpl(identifier(methodName));

    int count = argumentTypes.length;
    ParameterElement[] parameters = new ParameterElement[count];
    for (int i = 0; i < count; i++) {
      ParameterElementImpl parameter = new ParameterElementImpl(identifier("a" + i));
      parameter.setType(argumentTypes[i]);
      parameters[i] = parameter;
    }
    method.setParameters(parameters);

    FunctionTypeImpl methodType = new FunctionTypeImpl(method);
    methodType.setNormalParameterTypes(argumentTypes);
    methodType.setReturnType(returnType);
    method.setType(methodType);
    return method;
  }

  static Token token(Keyword keyword) {
    return new KeywordToken(keyword, 0);
  }

  static Token token(TokenType type) {
    return new Token(type, 0);
  }

  static Token token(TokenType type, String lexeme) {
    return new StringToken(type, lexeme, 0);
  }

  private static String convertPath(String path) {
    if (File.separator.equals("/")) {
      // We're on a unix-ish OS.
      return path;
    } else {
      // On windows, the path separator is '\'.
      return path.replaceAll("/", "\\\\");
    }
  }

  private AnalysisContextImpl analysisContext = AnalysisContextFactory.contextWithCore();

  public Source addSource(String filePath, String contents) {
    Source source = analysisContext.getSourceFactory().forFile(createFile(filePath));
    analysisContext.getSourceFactory().setContents(source, contents);
    return source;
  }

  public LibraryElementImpl createTestLibrary() {
    return createTestLibrary(new AnalysisContextImpl(), "test");
  }

  public LibraryElementImpl createTestLibrary(AnalysisContext context, String libraryName,
      String... typeNames) {
    int count = typeNames.length;
    CompilationUnitElementImpl[] sourcedCompilationUnits = new CompilationUnitElementImpl[count];
    for (int i = 0; i < count; i++) {
      String typeName = typeNames[i];
      ClassElementImpl type = new ClassElementImpl(identifier(typeName));
      String fileName = typeName + ".dart";
      CompilationUnitElementImpl compilationUnit = new CompilationUnitElementImpl(fileName);
      compilationUnit.setSource(analysisContext.getSourceFactory().forFile(createFile(fileName)));
      compilationUnit.setTypes(new ClassElement[] {type});
      sourcedCompilationUnits[i] = compilationUnit;
    }
    String fileName = libraryName + ".dart";
    CompilationUnitElementImpl compilationUnit = new CompilationUnitElementImpl(fileName);
    compilationUnit.setSource(analysisContext.getSourceFactory().forFile(createFile(fileName)));

    LibraryElementImpl library = new LibraryElementImpl(context, libraryIdentifier(libraryName));
    library.setDefiningCompilationUnit(compilationUnit);
    library.setParts(sourcedCompilationUnits);
    return library;
  }

  public AnalysisContext getAnalysisContext() {
    return analysisContext;
  }

  public void resolve(Source librarySource) throws AnalysisException {
    final boolean[] errorFound = {false};
    AnalysisErrorListener listener = new AnalysisErrorListener() {
      @Override
      public void onError(AnalysisError error) {
        errorFound[0] = true;
      }
    };
    LibraryResolver resolver = new LibraryResolver(analysisContext, listener);
    resolver.resolveLibrary(librarySource, true);
  }

}
