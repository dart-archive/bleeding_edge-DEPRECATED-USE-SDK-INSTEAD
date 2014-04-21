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
import com.google.dart.engine.ast.Annotation;
import com.google.dart.engine.ast.ArgumentList;
import com.google.dart.engine.ast.AssignmentExpression;
import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.ClassMember;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.CompilationUnitMember;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.FieldDeclaration;
import com.google.dart.engine.ast.IndexExpression;
import com.google.dart.engine.ast.MapLiteral;
import com.google.dart.engine.ast.MapLiteralEntry;
import com.google.dart.engine.ast.MethodInvocation;
import com.google.dart.engine.ast.NamedExpression;
import com.google.dart.engine.ast.NodeList;
import com.google.dart.engine.ast.PrefixedIdentifier;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.SimpleStringLiteral;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.ast.visitor.RecursiveAstVisitor;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.ConstructorElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.FieldElement;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.element.ToolkitObjectElement;
import com.google.dart.engine.element.VariableElement;
import com.google.dart.engine.element.angular.AngularComponentElement;
import com.google.dart.engine.element.angular.AngularDecoratorElement;
import com.google.dart.engine.element.angular.AngularElement;
import com.google.dart.engine.element.angular.AngularHasSelectorElement;
import com.google.dart.engine.element.angular.AngularPropertyElement;
import com.google.dart.engine.element.angular.AngularPropertyKind;
import com.google.dart.engine.element.angular.AngularScopePropertyElement;
import com.google.dart.engine.element.angular.AngularSelectorElement;
import com.google.dart.engine.element.angular.AngularViewElement;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.AnalysisErrorListener;
import com.google.dart.engine.error.AngularCode;
import com.google.dart.engine.error.ErrorCode;
import com.google.dart.engine.internal.element.ClassElementImpl;
import com.google.dart.engine.internal.element.CompilationUnitElementImpl;
import com.google.dart.engine.internal.element.angular.AngularComponentElementImpl;
import com.google.dart.engine.internal.element.angular.AngularControllerElementImpl;
import com.google.dart.engine.internal.element.angular.AngularDecoratorElementImpl;
import com.google.dart.engine.internal.element.angular.AngularFormatterElementImpl;
import com.google.dart.engine.internal.element.angular.AngularHasClassSelectorElementImpl;
import com.google.dart.engine.internal.element.angular.AngularPropertyElementImpl;
import com.google.dart.engine.internal.element.angular.AngularScopePropertyElementImpl;
import com.google.dart.engine.internal.element.angular.AngularTagSelectorElementImpl;
import com.google.dart.engine.internal.element.angular.AngularViewElementImpl;
import com.google.dart.engine.internal.element.angular.HasAttributeSelectorElementImpl;
import com.google.dart.engine.internal.element.angular.IsTagHasAttributeSelectorElementImpl;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.type.InterfaceType;
import com.google.dart.engine.type.Type;
import com.google.dart.engine.utilities.general.ObjectUtilities;
import com.google.dart.engine.utilities.general.StringUtilities;

import java.util.List;

/**
 * Instances of the class {@code AngularCompilationUnitBuilder} build an Angular specific element
 * model for a single compilation unit.
 * 
 * @coverage dart.engine.resolver
 */
public class AngularCompilationUnitBuilder {
  private static final String NG_COMPONENT = "Component";
  private static final String NG_CONTROLLER = "Controller";
  private static final String NG_DECORATOR = "Decorator";
  private static final String NG_FORMATTER = "Formatter";

  private static final String NAME = "name";
  private static final String SELECTOR = "selector";
  private static final String PUBLISH_AS = "publishAs";
  private static final String TEMPLATE_URL = "templateUrl";
  private static final String CSS_URL = "cssUrl";

  private static final String NG_ATTR = "NgAttr";
  private static final String NG_CALLBACK = "NgCallback";
  private static final String NG_ONE_WAY = "NgOneWay";
  private static final String NG_ONE_WAY_ONE_TIME = "NgOneWayOneTime";
  private static final String NG_TWO_WAY = "NgTwoWay";

  public static Element getElement(AstNode node, int offset) {
    // maybe node is not SimpleStringLiteral
    if (!(node instanceof SimpleStringLiteral)) {
      return null;
    }
    SimpleStringLiteral literal = (SimpleStringLiteral) node;
    // maybe has AngularElement
    {
      Element element = literal.getToolkitElement();
      if (element instanceof AngularElement) {
        return element;
      }
    }
    // prepare enclosing ClassDeclaration
    ClassDeclaration classDeclaration = node.getAncestor(ClassDeclaration.class);
    if (classDeclaration == null) {
      return null;
    }
    // prepare ClassElement
    ClassElement classElement = classDeclaration.getElement();
    if (classElement == null) {
      return null;
    }
    // check toolkit objects
    for (ToolkitObjectElement toolkitObject : classElement.getToolkitObjects()) {
      AngularPropertyElement[] properties = AngularPropertyElement.EMPTY_ARRAY;
      // maybe name
      if (toolkitObject instanceof AngularElement) {
        if (isNameCoveredByLiteral(toolkitObject, node)) {
          return toolkitObject;
        }
      }
      // try selector
      if (toolkitObject instanceof AngularHasSelectorElement) {
        AngularHasSelectorElement hasSelector = (AngularHasSelectorElement) toolkitObject;
        AngularSelectorElement selector = hasSelector.getSelector();
        if (isNameCoveredByLiteral(selector, node)) {
          return selector;
        }
      }
      // try properties of AngularComponentElement
      if (toolkitObject instanceof AngularComponentElement) {
        AngularComponentElement component = (AngularComponentElement) toolkitObject;
        properties = component.getProperties();
      }
      // try properties of AngularDirectiveElement
      if (toolkitObject instanceof AngularDecoratorElement) {
        AngularDecoratorElement directive = (AngularDecoratorElement) toolkitObject;
        properties = directive.getProperties();
      }
      // check properties
      for (AngularPropertyElement property : properties) {
        // property name (use complete node range)
        if (isNameCoveredByLiteral(property, node)) {
          return property;
        }
        // field name (use complete node range, including @, => and <=>)
        FieldElement field = property.getField();
        if (field != null) {
          int fieldOffset = property.getFieldNameOffset();
          int fieldEnd = fieldOffset + field.getName().length();
          if (node.getOffset() <= fieldOffset && fieldEnd < node.getEnd()) {
            return field;
          }
        }
      }
    }
    // no Element
    return null;
  }

  /**
   * Parses given selector text and returns {@link AngularSelectorElement}. May be {@code null} if
   * cannot parse.
   */
  @VisibleForTesting
  public static AngularSelectorElement parseSelector(int offset, String text) {
    // [attribute]
    if (StringUtilities.startsWithChar(text, '[') && StringUtilities.endsWithChar(text, ']')) {
      int nameOffset = offset + 1;
      String attributeName = text.substring(1, text.length() - 1);
      // TODO(scheglov) report warning if there are spaces between [ and identifier
      return new HasAttributeSelectorElementImpl(attributeName, nameOffset);
    }
    // .class
    if (StringUtilities.startsWithChar(text, '.')) {
      int nameOffset = offset + 1;
      String className = text.substring(1, text.length());
      return new AngularHasClassSelectorElementImpl(className, nameOffset);
    }
    // tag[attribute]
    if (StringUtilities.endsWithChar(text, ']')) {
      int index = StringUtilities.indexOf1(text, 0, '[');
      if (index != -1) {
        String tagName = text.substring(0, index);
        String attributeName = text.substring(index + 1, text.length() - 1);
        if (StringUtilities.isTagName(tagName)) {
          return new IsTagHasAttributeSelectorElementImpl(tagName, attributeName);
        }
      }
    }
    // tag
    if (StringUtilities.isTagName(text)) {
      return new AngularTagSelectorElementImpl(text, offset);
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
   * Checks if the name range of the given {@link Element} is completely covered by the given
   * {@link SimpleStringLiteral}.
   */
  private static boolean isNameCoveredByLiteral(Element element, AstNode node) {
    if (element != null) {
      String name = element.getName();
      if (name != null) {
        int nameOffset = element.getNameOffset();
        int nameEnd = nameOffset + name.length();
        return node.getOffset() <= nameOffset && nameEnd < node.getEnd();
      }
    }
    return false;
  }

  /**
   * Parses given {@link SimpleStringLiteral} using {@link #parseSelector(int, String)}.
   */
  private static AngularSelectorElement parseSelectorFromString(SimpleStringLiteral literal) {
    int offset = literal.getValueOffset();
    String text = literal.getStringValue();
    return parseSelector(offset, text);
  }

  /**
   * The listener to which errors will be reported.
   */
  private final AnalysisErrorListener errorListener;

  /**
   * The source containing the unit that will be analyzed.
   */
  private final Source source;

  /**
   * The compilation unit with built Dart element models.
   */
  private CompilationUnit unit;

  /**
   * The {@link ClassDeclaration} that is currently being analyzed.
   */
  private ClassDeclaration classDeclaration;

  /**
   * The {@link ClassElementImpl} that is currently being analyzed.
   */
  private ClassElementImpl classElement;

  /**
   * The {@link Annotation} that is currently being analyzed.
   */
  private Annotation annotation;

  /**
   * Initialize a newly created compilation unit element builder.
   * 
   * @param errorListener the listener to which errors will be reported.
   * @param source the source containing the unit that will be analyzed
   * @param unit the compilation unit with built Dart element models
   */
  public AngularCompilationUnitBuilder(AnalysisErrorListener errorListener, Source source,
      CompilationUnit unit) {
    this.errorListener = errorListener;
    this.source = source;
    this.unit = unit;
  }

  /**
   * Builds Angular specific element models and adds them to the existing Dart elements.
   */
  public void build() {
    parseViews();
    // process classes
    for (CompilationUnitMember unitMember : unit.getDeclarations()) {
      if (unitMember instanceof ClassDeclaration) {
        this.classDeclaration = (ClassDeclaration) unitMember;
        this.classElement = (ClassElementImpl) classDeclaration.getElement();
        // process annotations
        NodeList<Annotation> annotations = classDeclaration.getMetadata();
        for (Annotation annotation : annotations) {
          // verify annotation
          if (annotation.getArguments() == null) {
            continue;
          }
          this.annotation = annotation;
          // @Formatter
          if (isAngularAnnotation(annotation, NG_FORMATTER)) {
            parseFormatter();
            continue;
          }
          // @Component
          if (isAngularAnnotation(annotation, NG_COMPONENT)) {
            parseComponent();
            continue;
          }
          // @Controller
          if (isAngularAnnotation(annotation, NG_CONTROLLER)) {
            parseController();
            continue;
          }
          // @Decorator
          if (isAngularAnnotation(annotation, NG_DECORATOR)) {
            parseDecorator();
            continue;
          }
        }
      }
    }
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
    return getStringLiteral(name).getValue();
  }

  /**
   * @return the offset of the value of the named argument.
   */
  private int getStringArgumentOffset(String name) {
    Expression argument = getArgument(name);
    return ((SimpleStringLiteral) argument).getValueOffset();
  }

  /**
   * @return the {@link SimpleStringLiteral} of the named argument.
   */
  private SimpleStringLiteral getStringLiteral(String name) {
    Expression argument = getArgument(name);
    return (SimpleStringLiteral) argument;
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

  private void parseComponent() {
    boolean isValid = true;
    // publishAs
    String name = null;
    int nameOffset = -1;
    if (hasStringArgument(PUBLISH_AS)) {
      name = getStringArgument(PUBLISH_AS);
      nameOffset = getStringArgumentOffset(PUBLISH_AS);
    }
    // selector
    AngularSelectorElement selector = null;
    if (!hasStringArgument(SELECTOR)) {
      reportErrorForAnnotation(AngularCode.MISSING_SELECTOR);
      isValid = false;
    } else {
      SimpleStringLiteral selectorLiteral = getStringLiteral(SELECTOR);
      selector = parseSelectorFromString(selectorLiteral);
      if (selector == null) {
        reportErrorForArgument(SELECTOR, AngularCode.CANNOT_PARSE_SELECTOR, selectorLiteral);
        isValid = false;
      }
    }
    // templateUrl
    String templateUri = null;
    int templateUriOffset = -1;
    if (hasStringArgument(TEMPLATE_URL)) {
      templateUri = getStringArgument(TEMPLATE_URL);
      templateUriOffset = getStringArgumentOffset(TEMPLATE_URL);
    }
    // cssUrl
    String styleUri = null;
    int styleUriOffset = -1;
    if (hasStringArgument(CSS_URL)) {
      styleUri = getStringArgument(CSS_URL);
      styleUriOffset = getStringArgumentOffset(CSS_URL);
    }
    // create
    if (isValid) {
      AngularComponentElementImpl element = new AngularComponentElementImpl(
          name,
          nameOffset,
          annotation.getOffset());
      element.setSelector(selector);
      element.setTemplateUri(templateUri);
      element.setTemplateUriOffset(templateUriOffset);
      element.setStyleUri(styleUri);
      element.setStyleUriOffset(styleUriOffset);
      element.setProperties(parseComponentProperties());
      element.setScopeProperties(parseScopeProperties());
      classElement.addToolkitObjects(element);
    }
  }

  /**
   * Parses {@link AngularPropertyElement}s from {@link #annotation} and {@link #classDeclaration}.
   */
  private AngularPropertyElement[] parseComponentProperties() {
    List<AngularPropertyElement> properties = Lists.newArrayList();
    parseComponentProperties_fromMap(properties);
    parseComponentProperties_fromFields(properties);
    return properties.toArray(new AngularPropertyElement[properties.size()]);
  }

  /**
   * Parses {@link AngularPropertyElement}s from {@link #annotation}.
   */
  private void parseComponentProperties_fromFields(List<AngularPropertyElement> properties) {
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
  private void parseComponentProperties_fromMap(List<AngularPropertyElement> properties) {
    Expression mapExpression = getArgument("map");
    // may be not properties
    if (mapExpression == null) {
      return;
    }
    // prepare map literal
    if (!(mapExpression instanceof MapLiteral)) {
      reportErrorForNode(AngularCode.INVALID_PROPERTY_MAP, mapExpression);
      return;
    }
    MapLiteral mapLiteral = (MapLiteral) mapExpression;
    // analyze map entries
    for (MapLiteralEntry entry : mapLiteral.getEntries()) {
      // prepare property name
      Expression nameExpression = entry.getKey();
      if (!(nameExpression instanceof SimpleStringLiteral)) {
        reportErrorForNode(AngularCode.INVALID_PROPERTY_NAME, nameExpression);
        continue;
      }
      SimpleStringLiteral nameLiteral = (SimpleStringLiteral) nameExpression;
      String name = nameLiteral.getValue();
      int nameOffset = nameLiteral.getValueOffset();
      // prepare field specification
      Expression specExpression = entry.getValue();
      if (!(specExpression instanceof SimpleStringLiteral)) {
        reportErrorForNode(AngularCode.INVALID_PROPERTY_SPEC, specExpression);
        continue;
      }
      SimpleStringLiteral specLiteral = (SimpleStringLiteral) specExpression;
      String spec = specLiteral.getValue();
      // parse binding kind and field name
      AngularPropertyKind kind;
      int fieldNameOffset;
      if (StringUtilities.startsWithChar(spec, '@')) {
        kind = AngularPropertyKind.ATTR;
        fieldNameOffset = 1;
      } else if (StringUtilities.startsWithChar(spec, '&')) {
        kind = AngularPropertyKind.CALLBACK;
        fieldNameOffset = 1;
      } else if (StringUtilities.startsWith3(spec, 0, '=', '>', '!')) {
        kind = AngularPropertyKind.ONE_WAY_ONE_TIME;
        fieldNameOffset = 3;
      } else if (StringUtilities.startsWith2(spec, 0, '=', '>')) {
        kind = AngularPropertyKind.ONE_WAY;
        fieldNameOffset = 2;
      } else if (StringUtilities.startsWith3(spec, 0, '<', '=', '>')) {
        kind = AngularPropertyKind.TWO_WAY;
        fieldNameOffset = 3;
      } else {
        reportErrorForNode(AngularCode.INVALID_PROPERTY_KIND, specLiteral, spec);
        continue;
      }
      String fieldName = spec.substring(fieldNameOffset);
      fieldNameOffset += specLiteral.getValueOffset();
      // prepare field
      PropertyAccessorElement setter = classElement.getType().lookUpSetter(
          fieldName,
          classElement.getLibrary());
      if (setter == null) {
        reportErrorForOffset(
            AngularCode.INVALID_PROPERTY_FIELD,
            fieldNameOffset,
            fieldName.length(),
            fieldName);
        continue;
      }
      FieldElement field = (FieldElement) setter.getVariable();
      // add property
      AngularPropertyElementImpl property = new AngularPropertyElementImpl(name, nameOffset);
      property.setField(field);
      property.setPropertyKind(kind);
      property.setFieldNameOffset(fieldNameOffset);
      properties.add(property);
    }
  }

  private void parseController() {
    boolean isValid = true;
    // publishAs
    if (!hasStringArgument(PUBLISH_AS)) {
      reportErrorForAnnotation(AngularCode.MISSING_PUBLISH_AS);
      isValid = false;
    }
    // selector
    AngularSelectorElement selector = null;
    if (!hasStringArgument(SELECTOR)) {
      reportErrorForAnnotation(AngularCode.MISSING_SELECTOR);
      isValid = false;
    } else {
      SimpleStringLiteral selectorLiteral = getStringLiteral(SELECTOR);
      selector = parseSelectorFromString(selectorLiteral);
      if (selector == null) {
        reportErrorForArgument(SELECTOR, AngularCode.CANNOT_PARSE_SELECTOR, selectorLiteral);
        isValid = false;
      }
    }
    // create
    if (isValid) {
      String name = getStringArgument(PUBLISH_AS);
      int nameOffset = getStringArgumentOffset(PUBLISH_AS);
      AngularControllerElementImpl element = new AngularControllerElementImpl(name, nameOffset);
      element.setSelector(selector);
      classElement.addToolkitObjects(element);
    }
  }

  private void parseDecorator() {
    boolean isValid = true;
    // selector
    AngularSelectorElement selector = null;
    if (!hasStringArgument(SELECTOR)) {
      reportErrorForAnnotation(AngularCode.MISSING_SELECTOR);
      isValid = false;
    } else {
      SimpleStringLiteral selectorLiteral = getStringLiteral(SELECTOR);
      selector = parseSelectorFromString(selectorLiteral);
      if (selector == null) {
        reportErrorForArgument(SELECTOR, AngularCode.CANNOT_PARSE_SELECTOR, selectorLiteral);
        isValid = false;
      }
    }
    // create
    if (isValid) {
      int offset = annotation.getOffset();
      AngularDecoratorElementImpl element = new AngularDecoratorElementImpl(offset);
      element.setSelector(selector);
      element.setProperties(parseComponentProperties());
      classElement.addToolkitObjects(element);
    }
  }

  private void parseFormatter() {
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
      classElement.addToolkitObjects(new AngularFormatterElementImpl(name, nameOffset));
    }
  }

  private AngularScopePropertyElement[] parseScopeProperties() {
    final List<AngularScopePropertyElement> properties = Lists.newArrayList();
    classDeclaration.accept(new RecursiveAstVisitor<Void>() {
      @Override
      public Void visitAssignmentExpression(AssignmentExpression node) {
        addProperty(node);
        return super.visitAssignmentExpression(node);
      }

      private void addProperty(AssignmentExpression node) {
        // try to find "name" in scope[name]
        SimpleStringLiteral nameNode = getNameNode(node.getLeftHandSide());
        if (nameNode == null) {
          return;
        }
        // prepare unique
        String name = nameNode.getStringValue();
        if (hasPropertyWithName(name)) {
          return;
        }
        // do add property
        int nameOffset = nameNode.getValueOffset();
        AngularScopePropertyElement property = new AngularScopePropertyElementImpl(
            name,
            nameOffset,
            node.getRightHandSide().getBestType());
        nameNode.setToolkitElement(property);
        properties.add(property);
      }

      private SimpleStringLiteral getNameNode(Expression node) {
        if (node instanceof IndexExpression) {
          IndexExpression indexExpression = (IndexExpression) node;
          Expression target = indexExpression.getTarget();
          Expression index = indexExpression.getIndex();
          if (index instanceof SimpleStringLiteral && isContext(target)) {
            return (SimpleStringLiteral) index;
          }
        }
        return null;
      }

      private boolean hasPropertyWithName(String name) {
        for (AngularScopePropertyElement property : properties) {
          if (property.getName().equals(name)) {
            return true;
          }
        }
        return false;
      }

      private boolean isContext(Expression target) {
        if (target instanceof PrefixedIdentifier) {
          PrefixedIdentifier prefixed = (PrefixedIdentifier) target;
          SimpleIdentifier prefix = prefixed.getPrefix();
          SimpleIdentifier identifier = prefixed.getIdentifier();
          return ObjectUtilities.equals(identifier.getName(), "context") && isScope(prefix);
        }
        return false;
      }

      private boolean isScope(Expression target) {
        if (target != null) {
          Type type = target.getBestType();
          if (type instanceof InterfaceType) {
            InterfaceType interfaceType = (InterfaceType) type;
            return interfaceType.getName().equals("Scope");
          }
        }
        return false;
      }
    });
    return properties.toArray(new AngularScopePropertyElement[properties.size()]);
  }

  /**
   * Create {@link AngularViewElement} for each valid <code>view('template.html')</code> invocation,
   * where <code>view</code> is <code>ViewFactory</code>.
   */
  private void parseViews() {
    final List<AngularViewElement> views = Lists.newArrayList();
    unit.accept(new RecursiveAstVisitor<Void>() {
      @Override
      public Void visitMethodInvocation(MethodInvocation node) {
        addView(node);
        return super.visitMethodInvocation(node);
      }

      private void addView(MethodInvocation node) {
        // only one argument
        List<Expression> arguments = node.getArgumentList().getArguments();
        if (arguments.size() != 1) {
          return;
        }
        // String literal
        Expression argument = arguments.get(0);
        if (!(argument instanceof SimpleStringLiteral)) {
          return;
        }
        SimpleStringLiteral literal = (SimpleStringLiteral) argument;
        // just view('template')
        if (node.getRealTarget() != null) {
          return;
        }
        // should be ViewFactory
        if (!isViewFactory(node.getMethodName())) {
          return;
        }
        // add AngularViewElement
        String templateUri = literal.getStringValue();
        int templateUriOffset = literal.getValueOffset();
        views.add(new AngularViewElementImpl(templateUri, templateUriOffset));
      }

      private boolean isViewFactory(Expression target) {
        if (target instanceof SimpleIdentifier) {
          SimpleIdentifier identifier = (SimpleIdentifier) target;
          Element element = identifier.getStaticElement();
          if (element instanceof VariableElement) {
            VariableElement variable = (VariableElement) element;
            Type type = variable.getType();
            if (type instanceof InterfaceType) {
              InterfaceType interfaceType = (InterfaceType) type;
              return interfaceType.getName().equals("ViewFactory");
            }
          }
        }
        return false;
      }
    });
    if (!views.isEmpty()) {
      AngularViewElement[] viewArray = views.toArray(new AngularViewElement[views.size()]);
      ((CompilationUnitElementImpl) unit.getElement()).setAngularViews(viewArray);
    }
  }

  private void reportErrorForAnnotation(ErrorCode errorCode, Object... arguments) {
    reportErrorForNode(errorCode, annotation, arguments);
  }

  private void reportErrorForArgument(String argumentName, ErrorCode errorCode, Object... arguments) {
    Expression argument = getArgument(argumentName);
    reportErrorForNode(errorCode, argument, arguments);
  }

  private void reportErrorForNode(ErrorCode errorCode, AstNode node, Object... arguments) {
    int offset = node.getOffset();
    int length = node.getLength();
    reportErrorForOffset(errorCode, offset, length, arguments);
  }

  private void reportErrorForOffset(ErrorCode errorCode, int offset, int length,
      Object... arguments) {
    errorListener.onError(new AnalysisError(source, offset, length, errorCode, arguments));
  }
}
