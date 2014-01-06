/*
 * Copyright (c) 2014, the Dart project authors.
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

package com.google.dart.engine.internal.builder;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.ast.Annotation;
import com.google.dart.engine.ast.ArgumentList;
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.ClassMember;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.CompilationUnitMember;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.FieldDeclaration;
import com.google.dart.engine.ast.MapLiteral;
import com.google.dart.engine.ast.MapLiteralEntry;
import com.google.dart.engine.ast.NamedExpression;
import com.google.dart.engine.ast.NodeList;
import com.google.dart.engine.ast.SimpleStringLiteral;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.ConstructorElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.FieldElement;
import com.google.dart.engine.element.ToolkitObjectElement;
import com.google.dart.engine.element.angular.AngularPropertyElement;
import com.google.dart.engine.element.angular.AngularPropertyKind;
import com.google.dart.engine.element.angular.AngularSelector;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.AnalysisErrorListener;
import com.google.dart.engine.error.AngularCode;
import com.google.dart.engine.error.ErrorCode;
import com.google.dart.engine.internal.element.ClassElementImpl;
import com.google.dart.engine.internal.element.angular.AngularComponentElementImpl;
import com.google.dart.engine.internal.element.angular.AngularControllerElementImpl;
import com.google.dart.engine.internal.element.angular.AngularFilterElementImpl;
import com.google.dart.engine.internal.element.angular.AngularPropertyElementImpl;
import com.google.dart.engine.internal.element.angular.HasAttributeSelector;
import com.google.dart.engine.internal.element.angular.IsTagSelector;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.utilities.general.StringUtilities;

import java.util.List;

/**
 * Instances of the class {@code AngularCompilationUnitBuilder} build an Angular specific element
 * model for a single compilation unit.
 * 
 * @coverage dart.engine.resolver
 */
public class AngularCompilationUnitBuilder {
  private static final String NG_COMPONENT = "NgComponent";
  private static final String NG_CONTROLLER = "NgController";
  private static final String NG_FILTER = "NgFilter";

  private static final String NAME = "name";
  private static final String SELECTOR = "selector";
  private static final String PUBLISH_AS = "publishAs";
  private static final String TEMPLATE_URL = "templateUrl";
  private static final String CSS_URL = "cssUrl";

  private static final String PREFIX_ATTR = "@";
  private static final String PREFIX_CALLBACK = "&";
  private static final String PREFIX_ONE_WAY = "=>";
  private static final String PREFIX_ONE_WAY_ONE_TIME = "=>!";
  private static final String PREFIX_TWO_WAY = "<=>";

  private static final String NG_ATTR = "NgAttr";
  private static final String NG_CALLBACK = "NgCallback";
  private static final String NG_ONE_WAY = "NgOneWay";
  private static final String NG_ONE_WAY_ONE_TIME = "NgOneWayOneTime";
  private static final String NG_TWO_WAY = "NgTwoWay";

  /**
   * Parses given selector text and returns {@link AngularSelector}. May be {@code null} if cannot
   * parse.
   */
  @VisibleForTesting
  public static AngularSelector parseSelector(String text) {
    if (text.startsWith("[") && text.endsWith("]")) {
      return new HasAttributeSelector(text.substring(1, text.length() - 1));
    }
    if (StringUtilities.isTagName(text)) {
      return new IsTagSelector(text);
    }
    return null;
  }

  /**
   * Returns the {@link FieldElement} of the first field in the given {@link FieldDeclaration}.
   */
  private static FieldElement getOnlyFieldElement(FieldDeclaration fieldDeclaration) {
    NodeList<VariableDeclaration> fields = fieldDeclaration.getFields().getVariables();
    return (FieldElement) fields.get(0).getElement();
  }

  /**
   * If given {@link Annotation} has one argument and it is {@link SimpleStringLiteral}, returns it,
   * otherwise returns {@code null}.
   */
  private static SimpleStringLiteral getOnlySimpleStringLiteralArgument(Annotation annotation) {
    SimpleStringLiteral nameLiteral = null;
    ArgumentList argsNode = annotation.getArguments();
    if (argsNode != null) {
      NodeList<Expression> args = argsNode.getArguments();
      if (args.size() == 1) {
        Expression arg = args.get(0);
        if (arg instanceof SimpleStringLiteral) {
          nameLiteral = (SimpleStringLiteral) arg;
        }
      }
    }
    return nameLiteral;
  }

  /**
   * The source containing the unit that will be analyzed.
   */
  private final Source source;

  /**
   * The listener to which errors will be reported.
   */
  private final AnalysisErrorListener errorListener;

  /**
   * The {@link ClassDeclaration} that is currently being analyzed.
   */
  private ClassDeclaration classDeclaration;

  /**
   * The {@link Annotation} that is currently being analyzed.
   */
  private Annotation annotation;

  /**
   * Initialize a newly created compilation unit element builder.
   * 
   * @param errorListener the listener to which errors will be reported.
   * @param source the source containing the unit that will be analyzed
   */
  public AngularCompilationUnitBuilder(AnalysisErrorListener errorListener, Source source) {
    this.errorListener = errorListener;
    this.source = source;
  }

  /**
   * Builds Angular specific element models and adds them to the existing Dart elements.
   * 
   * @param unit the compilation unit with built Dart element models
   */
  public void build(CompilationUnit unit) {
    for (CompilationUnitMember unitMember : unit.getDeclarations()) {
      if (unitMember instanceof ClassDeclaration) {
        this.classDeclaration = (ClassDeclaration) unitMember;
        NodeList<Annotation> annotations = classDeclaration.getMetadata();
        for (Annotation annotation : annotations) {
          this.annotation = annotation;
          // @NgFilter
          if (isAngularAnnotation(NG_FILTER)) {
            parseNgFilter();
            continue;
          }
          // @NgComponent
          if (isAngularAnnotation(NG_COMPONENT)) {
            parseNgComponent();
            continue;
          }
          // @NgController
          if (isAngularAnnotation(NG_CONTROLLER)) {
            parseNgController();
            continue;
          }
        }
      }
    }
  }

  // TODO(scheglov) add ClassElement.getField(name)
  private FieldElement findField(String name) {
    FieldElement[] fields = classDeclaration.getElement().getFields();
    for (FieldElement field : fields) {
      if (name.equals(field.getName())) {
        return field;
      }
    }
    return null;
  }

  /**
   * @return the argument {@link Expression} with given name form {@link #annotation}, may be
   *         {@code null} if not found.
   */
  private Expression getArgument(String name) {
    List<Expression> arguments = annotation.getArguments().getArguments();
    for (Expression argument : arguments) {
      if (argument instanceof NamedExpression) {
        NamedExpression namedExpression = (NamedExpression) argument;
        String argumentName = namedExpression.getName().getLabel().getName();
        if (name.equals(argumentName)) {
          return namedExpression.getExpression();
        }
      }
    }
    return null;
  }

  /**
   * @return the {@link String} value of the named argument.
   */
  private String getStringArgument(String name) {
    Expression argument = getArgument(name);
    return ((SimpleStringLiteral) argument).getValue();
  }

  /**
   * @return the offset of the value of the named argument.
   */
  private int getStringArgumentOffset(String name) {
    Expression argument = getArgument(name);
    return ((SimpleStringLiteral) argument).getValueOffset();
  }

  /**
   * Checks if {@link #namedArguments} has string value for the argument with the given name.
   */
  private boolean hasStringArgument(String name) {
    Expression argument = getArgument(name);
    return argument instanceof SimpleStringLiteral;
  }

  /**
   * Checks if given {@link Annotation} is an annotation with required name.
   */
  private boolean isAngularAnnotation(Annotation annotation, String name) {
    Element element = annotation.getElement();
    if (element instanceof ConstructorElement) {
      ConstructorElement constructorElement = (ConstructorElement) element;
      return constructorElement.getReturnType().getDisplayName().equals(name);
    }
    return false;
  }

  /**
   * Checks if {@link #annotation} is an annotation with required name.
   */
  private boolean isAngularAnnotation(String name) {
    return isAngularAnnotation(annotation, name);
  }

  private void parseNgComponent() {
    boolean isValid = true;
    // publishAs
    if (!hasStringArgument(PUBLISH_AS)) {
      reportErrorForAnnotation(AngularCode.MISSING_PUBLISH_AS);
      isValid = false;
    }
    // selector
    AngularSelector selector = null;
    if (!hasStringArgument(SELECTOR)) {
      reportErrorForAnnotation(AngularCode.MISSING_SELECTOR);
      isValid = false;
    } else {
      String selectorText = getStringArgument(SELECTOR);
      selector = parseSelector(selectorText);
      if (selector == null) {
        reportErrorForArgument(SELECTOR, AngularCode.CANNOT_PARSE_SELECTOR, selectorText);
        isValid = false;
      }
    }
    // templateUrl
    if (!hasStringArgument(TEMPLATE_URL)) {
      reportErrorForAnnotation(AngularCode.MISSING_TEMPLATE_URL);
      isValid = false;
    }
    // cssUrl
    if (!hasStringArgument(CSS_URL)) {
      reportErrorForAnnotation(AngularCode.MISSING_CSS_URL);
      isValid = false;
    }
    // create
    if (isValid) {
      String name = getStringArgument(PUBLISH_AS);
      int nameOffset = getStringArgumentOffset(PUBLISH_AS);
      String templateUri = getStringArgument(TEMPLATE_URL);
      int templateUriOffset = getStringArgumentOffset(TEMPLATE_URL);
      String styleUri = getStringArgument(CSS_URL);
      int styleUriOffset = getStringArgumentOffset(CSS_URL);
      AngularComponentElementImpl element = new AngularComponentElementImpl(name, nameOffset);
      element.setSelector(selector);
      element.setTemplateUri(templateUri);
      element.setTemplateUriOffset(templateUriOffset);
      element.setStyleUri(styleUri);
      element.setStyleUriOffset(styleUriOffset);
      element.setProperties(parseNgComponentProperties());
      setToolkitElement(element);
    }
  }

  /**
   * Parses {@link AngularPropertyElement}s from {@link #annotation} and {@link #classDeclaration}.
   */
  private AngularPropertyElement[] parseNgComponentProperties() {
    List<AngularPropertyElement> properties = Lists.newArrayList();
    parseNgComponentProperties_fromMap(properties);
    parseNgComponentProperties_fromFields(properties);
    return properties.toArray(new AngularPropertyElement[properties.size()]);
  }

  /**
   * Parses {@link AngularPropertyElement}s from {@link #annotation}.
   */
  private void parseNgComponentProperties_fromFields(List<AngularPropertyElement> properties) {
    NodeList<ClassMember> members = classDeclaration.getMembers();
    for (ClassMember member : members) {
      if (member instanceof FieldDeclaration) {
        FieldDeclaration fieldDeclaration = (FieldDeclaration) member;
        for (Annotation annotation : fieldDeclaration.getMetadata()) {
          // prepare property kind (if property annotation at all)
          AngularPropertyKind kind = null;
          if (isAngularAnnotation(annotation, NG_ATTR)) {
            kind = AngularPropertyKind.ATTR;
          } else if (isAngularAnnotation(annotation, NG_CALLBACK)) {
            kind = AngularPropertyKind.CALLBACK;
          } else if (isAngularAnnotation(annotation, NG_ONE_WAY)) {
            kind = AngularPropertyKind.ONE_WAY;
          } else if (isAngularAnnotation(annotation, NG_ONE_WAY_ONE_TIME)) {
            kind = AngularPropertyKind.ONE_WAY_ONE_TIME;
          } else if (isAngularAnnotation(annotation, NG_TWO_WAY)) {
            kind = AngularPropertyKind.TWO_WAY;
          }
          // add property
          if (kind != null) {
            SimpleStringLiteral nameLiteral = getOnlySimpleStringLiteralArgument(annotation);
            FieldElement field = getOnlyFieldElement(fieldDeclaration);
            if (nameLiteral != null && field != null) {
              AngularPropertyElementImpl property = new AngularPropertyElementImpl(
                  nameLiteral.getValue(),
                  nameLiteral.getValueOffset());
              property.setField(field);
              property.setPropertyKind(kind);
              properties.add(property);
            }
          }
        }
      }
    }
  }

  /**
   * Parses {@link AngularPropertyElement}s from {@link #annotation}.
   */
  private void parseNgComponentProperties_fromMap(List<AngularPropertyElement> properties) {
    Expression mapExpression = getArgument("map");
    // may be not properties
    if (mapExpression == null) {
      return;
    }
    // prepare map literal
    if (!(mapExpression instanceof MapLiteral)) {
      reportError(mapExpression, AngularCode.INVALID_PROPERTY_MAP);
      return;
    }
    MapLiteral mapLiteral = (MapLiteral) mapExpression;
    // analyze map entries
    for (MapLiteralEntry entry : mapLiteral.getEntries()) {
      // prepare property name
      Expression nameExpression = entry.getKey();
      if (!(nameExpression instanceof SimpleStringLiteral)) {
        reportError(nameExpression, AngularCode.INVALID_PROPERTY_NAME);
        continue;
      }
      SimpleStringLiteral nameLiteral = (SimpleStringLiteral) nameExpression;
      String name = nameLiteral.getValue();
      int nameOffset = nameLiteral.getValueOffset();
      // prepare field specification
      Expression specExpression = entry.getValue();
      if (!(specExpression instanceof SimpleStringLiteral)) {
        reportError(specExpression, AngularCode.INVALID_PROPERTY_SPEC);
        continue;
      }
      SimpleStringLiteral specLiteral = (SimpleStringLiteral) specExpression;
      String spec = specLiteral.getValue();
      // parse binding kind and field name
      AngularPropertyKind kind;
      int fieldNameOffset;
      if (spec.startsWith(PREFIX_ATTR)) {
        kind = AngularPropertyKind.ATTR;
        fieldNameOffset = 1;
      } else if (spec.startsWith(PREFIX_CALLBACK)) {
        kind = AngularPropertyKind.CALLBACK;
        fieldNameOffset = 1;
      } else if (spec.startsWith(PREFIX_ONE_WAY_ONE_TIME)) {
        kind = AngularPropertyKind.ONE_WAY_ONE_TIME;
        fieldNameOffset = 3;
      } else if (spec.startsWith(PREFIX_ONE_WAY)) {
        kind = AngularPropertyKind.ONE_WAY;
        fieldNameOffset = 2;
      } else if (spec.startsWith(PREFIX_TWO_WAY)) {
        kind = AngularPropertyKind.TWO_WAY;
        fieldNameOffset = 3;
      } else {
        reportError(specLiteral, AngularCode.INVALID_PROPERTY_KIND, spec);
        continue;
      }
      String fieldName = spec.substring(fieldNameOffset);
      fieldNameOffset += specLiteral.getValueOffset();
      // prepare field
      FieldElement field = findField(fieldName);
      if (field == null) {
        reportError(specLiteral, AngularCode.INVALID_PROPERTY_FIELD, fieldName);
        continue;
      }
      // add property
      AngularPropertyElementImpl property = new AngularPropertyElementImpl(name, nameOffset);
      property.setField(field);
      property.setPropertyKind(kind);
      property.setFieldNameOffset(fieldNameOffset);
      properties.add(property);
    }
  }

  private void parseNgController() {
    boolean isValid = true;
    // publishAs
    if (!hasStringArgument(PUBLISH_AS)) {
      reportErrorForAnnotation(AngularCode.MISSING_PUBLISH_AS);
      isValid = false;
    }
    // selector
    AngularSelector selector = null;
    if (!hasStringArgument(SELECTOR)) {
      reportErrorForAnnotation(AngularCode.MISSING_SELECTOR);
      isValid = false;
    } else {
      String selectorText = getStringArgument(SELECTOR);
      selector = parseSelector(selectorText);
      if (selector == null) {
        reportErrorForArgument(SELECTOR, AngularCode.CANNOT_PARSE_SELECTOR, selectorText);
        isValid = false;
      }
    }
    // create
    if (isValid) {
      String name = getStringArgument(PUBLISH_AS);
      int nameOffset = getStringArgumentOffset(PUBLISH_AS);
      AngularControllerElementImpl element = new AngularControllerElementImpl(name, nameOffset);
      element.setSelector(selector);
      setToolkitElement(element);
    }
  }

  private void parseNgFilter() {
    boolean isValid = true;
    // name
    if (!hasStringArgument(NAME)) {
      reportErrorForAnnotation(AngularCode.MISSING_NAME);
      isValid = false;
    }
    // create
    if (isValid) {
      String name = getStringArgument(NAME);
      int nameOffset = getStringArgumentOffset(NAME);
      setToolkitElement(new AngularFilterElementImpl(name, nameOffset));
    }
  }

  private void reportError(ASTNode node, ErrorCode errorCode, Object... arguments) {
    errorListener.onError(new AnalysisError(
        source,
        node.getOffset(),
        node.getLength(),
        errorCode,
        arguments));
  }

  private void reportErrorForAnnotation(ErrorCode errorCode, Object... arguments) {
    reportError(annotation, errorCode, arguments);
  }

  private void reportErrorForArgument(String argumentName, ErrorCode errorCode, Object... arguments) {
    Expression argument = getArgument(argumentName);
    reportError(argument, errorCode, arguments);
  }

  /**
   * Set the given {@link ToolkitObjectElement} for {@link ClassElement} of
   * {@link #classDeclaration}.
   */
  private void setToolkitElement(ToolkitObjectElement element) {
    ClassElementImpl classElement = (ClassElementImpl) classDeclaration.getElement();
    classElement.setToolkitObjects(new ToolkitObjectElement[] {element});
  }
}
