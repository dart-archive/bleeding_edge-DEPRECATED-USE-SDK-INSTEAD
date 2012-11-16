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
package com.google.dart.tools.ui.internal.text.editor;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.dart.compiler.ast.DartBinaryExpression;
import com.google.dart.compiler.ast.DartClass;
import com.google.dart.compiler.ast.DartDoubleLiteral;
import com.google.dart.compiler.ast.DartExportDirective;
import com.google.dart.compiler.ast.DartField;
import com.google.dart.compiler.ast.DartFieldDefinition;
import com.google.dart.compiler.ast.DartFunctionExpression;
import com.google.dart.compiler.ast.DartFunctionTypeAlias;
import com.google.dart.compiler.ast.DartIdentifier;
import com.google.dart.compiler.ast.DartImportDirective;
import com.google.dart.compiler.ast.DartIntegerLiteral;
import com.google.dart.compiler.ast.DartLibraryDirective;
import com.google.dart.compiler.ast.DartMethodDefinition;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.ast.DartPartOfDirective;
import com.google.dart.compiler.ast.DartSourceDirective;
import com.google.dart.compiler.ast.DartTypeNode;
import com.google.dart.compiler.ast.DartVariable;
import com.google.dart.compiler.ast.ImportCombinator;
import com.google.dart.compiler.ast.LibraryUnit;
import com.google.dart.compiler.parser.Token;
import com.google.dart.compiler.resolver.Element;
import com.google.dart.compiler.resolver.ElementKind;
import com.google.dart.compiler.resolver.FieldElement;
import com.google.dart.compiler.resolver.LibraryElement;
import com.google.dart.compiler.resolver.NodeElement;
import com.google.dart.compiler.util.apache.StringUtils;
import com.google.dart.tools.core.dom.PropertyDescriptorHelper;
import com.google.dart.tools.core.model.SourceRange;
import com.google.dart.tools.core.utilities.ast.DynamicTypesFinder;
import com.google.dart.tools.core.utilities.general.SourceRangeFactory;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.PreferenceConstants;
import com.google.dart.tools.ui.text.IDartColorConstants;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.graphics.RGB;

import java.util.List;

/**
 * Semantic highlightings.
 * 
 * @coverage dart.editor.ui.text.highlighting
 */
public class SemanticHighlightings {

  /**
   * Highlights build-in identifiers - "abstract", "as", "dynamic", "typedef", etc.
   */
  private static class BuiltInHighlighting extends DefaultSemanticHighlighting {
    private List<SourceRange> result = null;

    @Override
    public boolean consumesIdentifier(SemanticToken token) {
      DartIdentifier node = token.getNodeIdentifier();
      // "dynamic" as type
      if (node.getParent() instanceof DartTypeNode) {
        String name = node.getName();
        if ("dynamic".equals(name)) {
          return true;
        }
      }
      // no
      return false;
    }

    @Override
    public List<SourceRange> consumesMulti(SemanticToken token) {
      result = null;
      // prepare DartNode
      DartNode node = token.getNode();
      // typedef
      if (node instanceof DartFunctionTypeAlias) {
        addStartPosition(token, "typedef");
      }
      // as
      if (node instanceof DartBinaryExpression) {
        DartBinaryExpression binary = (DartBinaryExpression) node;
        if (binary.getOperator() == Token.AS) {
          return ImmutableList.of(SourceRangeFactory.forStartLength(
              binary.getOperatorOffset(),
              "as".length()));
        }
      }
      // field modifiers
      if (node instanceof DartFieldDefinition) {
        DartFieldDefinition fieldDef = (DartFieldDefinition) node;
        List<DartField> fields = fieldDef.getFields();
        if (!fields.isEmpty() && fields.get(0).getModifiers().isStatic()) {
          addStartPosition(token, "static");
        }
      }
      // method modifiers
      if (node instanceof DartMethodDefinition) {
        DartMethodDefinition method = (DartMethodDefinition) node;
        if (method.getModifiers().isAbstract()) {
          addStartPosition(token, "abstract");
        }
        if (method.getModifiers().isExternal()) {
          addStartPosition(token, "external");
        }
        if (method.getModifiers().isFactory()) {
          addStartPosition(token, "factory");
        }
        if (method.getModifiers().isGetter()) {
          addMethodModifierPosition(token, method, "get");
        }
        if (method.getModifiers().isOperator()) {
          addMethodModifierPosition(token, method, "operator");
        }
        if (method.getModifiers().isSetter()) {
          addMethodModifierPosition(token, method, "set");
        }
        if (method.getModifiers().isStatic()) {
          addStartPosition(token, "static");
        }
      }
      // implements
      if (node instanceof DartClass) {
        DartClass clazz = (DartClass) node;
        int implementsOffset = clazz.getImplementsOffset();
        return ImmutableList.of(SourceRangeFactory.forStartLength(
            implementsOffset,
            "implements".length()));
      }
      // done
      return result;
    }

    @Override
    public RGB getDefaultDefaultTextColor() {
      return KEY_WORD_COLOR;
    }

    @Override
    public String getDisplayName() {
      return DartEditorMessages.SemanticHighlighting_directive;
    }

    @Override
    public String getPreferenceKey() {
      return BUILT_IN;
    }

    @Override
    public boolean isBoldByDefault() {
      return true;
    }

    @Override
    public boolean isEnabledByDefault() {
      return true;
    }

    /**
     * Attempts to find special method modifier position - "get", "set" or "operator", which can be
     * not right at the start of the method, but after optional return type.
     */
    private void addMethodModifierPosition(SemanticToken token, DartMethodDefinition method,
        String modifierName) {
      String source = token.getSource();
      int offset = method.getSourceInfo().getOffset();
      // skip return type
      DartTypeNode returnType = method.getFunction().getReturnTypeNode();
      if (returnType != null) {
        int typeOffset = returnType.getSourceInfo().getEnd() - offset;
        offset += typeOffset;
        source = source.substring(typeOffset);
      }
      // skip whitespace
      {
        String trimSource = StringUtils.stripStart(source, null);
        offset += source.length() - trimSource.length();
        source = trimSource;
      }
      // find modifier
      int index = source.indexOf(modifierName);
      if (index == 0) {
        addPosition(offset, modifierName.length());
      }
    }

    private void addPosition(int start, int length) {
      if (result == null) {
        result = Lists.newArrayList();
      }
      result.add(SourceRangeFactory.forStartLength(start, length));
    }

    /**
     * Adds position of token source has <code>str</code> exactly at the start.
     */
    private void addStartPosition(SemanticToken token, String str) {
      DartNode node = token.getNode();
      int index = token.getSource().indexOf(str);
      if (index == 0) {
        int start = node.getSourceInfo().getOffset() + index;
        int length = str.length();
        addPosition(start, length);
      }
    }
  }

  private static class ClassHighlighting extends DefaultSemanticHighlighting {

    @Override
    public boolean consumesIdentifier(SemanticToken token) {
      DartIdentifier node = token.getNodeIdentifier();
      // ignore "void" and "dynamic" - they are reserved word and built-in identifier
      {
        String name = node.getName();
        if ("void".equals(name) || "dynamic".equals(name)) {
          return false;
        }
      }
      // highlight type name in declaration and use
      if (node.getParent() instanceof DartClass) {
        DartClass parentClass = (DartClass) node.getParent();
        return parentClass.getName() == node;
      }
      if (node.getParent() instanceof DartTypeNode) {
        return true;
      }
      // no
      return false;
    }

    @Override
    public String getDisplayName() {
      return DartEditorMessages.SemanticHighlighting_class;
    }

    @Override
    public String getPreferenceKey() {
      return CLASS;
    }

    @Override
    public boolean isEnabledByDefault() {
      return true;
    }
  }
  /**
   * Abstract {@link SemanticHighlighting} with empty methods by default.
   */
  private static abstract class DefaultSemanticHighlighting extends SemanticHighlighting {

    @Override
    public RGB getDefaultDefaultTextColor() {
      return new RGB(0, 0, 0);
    }

    @Override
    public boolean isBoldByDefault() {
      return false;
    }

    @Override
    public boolean isItalicByDefault() {
      return false;
    }

    @Override
    public boolean isStrikethroughByDefault() {
      return false;
    }

    @Override
    public boolean isUnderlineByDefault() {
      return false;
    }

    RGB defaultFieldColor() {
      return new RGB(0, 0, 192);
    }
  }

  /**
   * Semantic highlighting deprecated elements.
   */
  private static final class DeprecatedElementHighlighting extends DefaultSemanticHighlighting {
    @Override
    public boolean consumesIdentifier(SemanticToken token) {
      DartIdentifier node = token.getNodeIdentifier();
      NodeElement element = node.getElement();
      return element != null && element.getMetadata().isDeprecated();
    }

    @Override
    public RGB getDefaultDefaultTextColor() {
      return new RGB(0, 0, 0);
    }

    @Override
    public String getDisplayName() {
      return DartEditorMessages.SemanticHighlighting_deprecatedElement;
    }

    @Override
    public String getPreferenceKey() {
      return DEPRECATED_ELEMENT;
    }

    @Override
    public boolean isEnabledByDefault() {
      return true;
    }

    @Override
    public boolean isStrikethroughByDefault() {
      return true;
    }
  }

  /**
   * Highlights directives - "library", "import", "part of", etc.
   */
  private static class DirectiveHighlighting extends DefaultSemanticHighlighting {
    private List<SourceRange> result = null;

    @Override
    public List<SourceRange> consumesMulti(SemanticToken token) {
      result = null;
      DartNode node = token.getNode();
      if (node instanceof DartLibraryDirective) {
        addPosition(token, "#library");
        addPosition(token, "library");
      }
      if (node instanceof DartImportDirective) {
        addPosition(token, "#import");
        addPosition(token, "import");
        addPosition(token, "show");
        addPosition(token, "hide");
      }
      if (node instanceof DartExportDirective) {
        DartExportDirective export = (DartExportDirective) node;
        addPosition(token, "export");
        for (ImportCombinator combinator : export.getCombinators()) {
          result.add(SourceRangeFactory.forStartLength(combinator, "show".length()));
        }
      }
      if (node instanceof DartSourceDirective) {
        addPosition(token, "#source");
        addPosition(token, "part");
      }
      if (node instanceof DartPartOfDirective) {
        DartPartOfDirective partOf = (DartPartOfDirective) node;
        int offset = partOf.getSourceInfo().getOffset();
        int length = partOf.getOfOffset() + "of".length() - offset;
        return ImmutableList.of(SourceRangeFactory.forStartLength(offset, length));
      }
      return result;
    }

    @Override
    public RGB getDefaultDefaultTextColor() {
      return KEY_WORD_COLOR;
    }

    @Override
    public String getDisplayName() {
      return DartEditorMessages.SemanticHighlighting_directive;
    }

    @Override
    public String getPreferenceKey() {
      return DIRECTIVE;
    }

    @Override
    public boolean isBoldByDefault() {
      return true;
    }

    @Override
    public boolean isEnabledByDefault() {
      return true;
    }

    private boolean addPosition(SemanticToken token, String str) {
      DartNode node = token.getNode();
      int index = token.getSource().indexOf(str);
      if (index == 0) {
        int start = node.getSourceInfo().getOffset() + index;
        int length = str.length();
        if (result == null) {
          result = Lists.newArrayList();
        }
        result.add(SourceRangeFactory.forStartLength(start, length));
        return true;
      }
      return false;
    }
  }

  /**
   * Semantic highlighting for variables with dynamic types.
   */
  private static final class DynamicTypeHighlighting extends DefaultSemanticHighlighting {

    @Override
    public boolean consumesIdentifier(SemanticToken token) {
      DartIdentifier node = token.getNodeIdentifier();
      return DynamicTypesFinder.isDynamic(node);
    }

    @Override
    public RGB getDefaultDefaultTextColor() {
      return new RGB(0x80, 0x00, 0xCC);
    }

    @Override
    public String getDisplayName() {
      return DartEditorMessages.SemanticHighlighting_dynamicType;
    }

    @Override
    public String getPreferenceKey() {
      return DYNAMIC_TYPE;
    }

    @Override
    public boolean isEnabledByDefault() {
      return true;
    }
  }

  /**
   * Semantic highlighting for fields.
   */
  private static class FieldHighlighting extends DefaultSemanticHighlighting {

    @Override
    public boolean consumesIdentifier(SemanticToken token) {
      DartIdentifier node = token.getNodeIdentifier();
      NodeElement element = node.getElement();
      boolean isField = ElementKind.of(element) == ElementKind.FIELD;
      if (isField) {
        /*
         * Annotations should not be highlighted the same as fields.
         * This whole block needs better support from the model.
         * The initial @ should have the same presentation as the annotation
         * but that's not handled here.
         */
        if (element.getEnclosingElement() instanceof LibraryElement) {
          LibraryElement lib = (LibraryElement) element.getEnclosingElement();
          LibraryUnit libUnit = lib.getLibraryUnit();
          if (libUnit != null && META_LIB_NAME.equals(libUnit.getName())) {
            String name = element.getName();
            for (String annotation : META_NAMES) {
              if (annotation.equals(name)) {
                return false;
              }
            }
          }
        }
      }
      return isField;
    }

    @Override
    public RGB getDefaultDefaultTextColor() {
      return defaultFieldColor();
    }

    @Override
    public String getDisplayName() {
      return DartEditorMessages.SemanticHighlighting_field;
    }

    @Override
    public String getPreferenceKey() {
      return FIELD;
    }

    @Override
    public boolean isEnabledByDefault() {
      return true;
    }
  }

  private static class GetterDeclarationHighlighting extends MethodDeclarationHighlighting {

    @Override
    public boolean consumes(SemanticToken token) {
      DartNode node = token.getNode();
      {
        DartMethodDefinition parentMethod = getParentMethod(node);
        if (parentMethod != null && parentMethod.getName() == node) {
          if (parentMethod.getElement().getModifiers().isGetter()) {
            return true;
          }
        }
      }
      return false;
    }

    @Override
    public RGB getDefaultDefaultTextColor() {
      return defaultFieldColor();
    }

    @Override
    public String getDisplayName() {
      return DartEditorMessages.SemanticHighlighting_getter;
    }

    @Override
    public String getPreferenceKey() {
      return GETTER_DECLARATION;
    }
  }

  private static class LocalVariableDeclarationHighlighting extends DefaultSemanticHighlighting {

    @Override
    public boolean consumesIdentifier(SemanticToken token) {
      DartIdentifier node = token.getNodeIdentifier();
      if (node.getParent() instanceof DartVariable) {
        DartVariable parent = (DartVariable) node.getParent();
        return parent.getName() == node;
      }
      return false;
    }

    @Override
    public RGB getDefaultDefaultTextColor() {
      return new RGB(0, 0, 0); // same as parameter
    }

    @Override
    public String getDisplayName() {
      return DartEditorMessages.SemanticHighlighting_localVariableDeclaration;
    }

    @Override
    public String getPreferenceKey() {
      return LOCAL_VARIABLE_DECLARATION;
    }

    @Override
    public boolean isEnabledByDefault() {
      return true;
    }
  }

  private static class LocalVariableHighlighting extends DefaultSemanticHighlighting {

    @Override
    public boolean consumesIdentifier(SemanticToken token) {
      DartIdentifier node = token.getNodeIdentifier();
      NodeElement element = node.getElement();
      return ElementKind.of(element) == ElementKind.VARIABLE;
    }

    @Override
    public RGB getDefaultDefaultTextColor() {
      return new RGB(0, 0, 0); // same as parameter
    }

    @Override
    public String getDisplayName() {
      return DartEditorMessages.SemanticHighlighting_localVariable;
    }

    @Override
    public String getPreferenceKey() {
      return LOCAL_VARIABLE;
    }

    @Override
    public boolean isEnabledByDefault() {
      return true;
    }
  }

  private static class MethodDeclarationHighlighting extends DefaultSemanticHighlighting {

    static DartMethodDefinition getParentMethod(DartNode node) {
      DartMethodDefinition parentMethod = null;
      if (node.getParent() instanceof DartMethodDefinition) {
        parentMethod = (DartMethodDefinition) node.getParent();
      }
      if (node.getParent() instanceof DartField) {
        DartField field = (DartField) node.getParent();
        parentMethod = field.getAccessor();
      }
      return parentMethod;
    }

    @Override
    public boolean consumes(SemanticToken token) {
      DartNode node = token.getNode();
      {
        DartMethodDefinition parentMethod = getParentMethod(node);
        if (parentMethod != null && parentMethod.getName() == node) {
          return true;
        }
      }
      if (node.getParent() instanceof DartFunctionExpression
          && ((DartFunctionExpression) node.getParent()).getName() == node) {
        return true;
      }
      return false;
    }

    @Override
    public RGB getDefaultDefaultTextColor() {
      return new RGB(64, 64, 64);
    }

    @Override
    public String getDisplayName() {
      return DartEditorMessages.SemanticHighlighting_methodDeclaration;
    }

    @Override
    public String getPreferenceKey() {
      return METHOD_DECLARATION;
    }

    @Override
    public boolean isEnabledByDefault() {
      return true;
    }
  }

  private static class MethodHighlighting extends DefaultSemanticHighlighting {

    @Override
    public boolean consumesIdentifier(SemanticToken token) {
      DartIdentifier node = token.getNodeIdentifier();
      NodeElement element = node.getElement();
      return ElementKind.of(element) == ElementKind.METHOD;
    }

    @Override
    public String getDisplayName() {
      return DartEditorMessages.SemanticHighlighting_method;
    }

    @Override
    public String getPreferenceKey() {
      return METHOD;
    }

    @Override
    public boolean isEnabledByDefault() {
      return true;
    }
  }

  private static class NumberHighlighting extends DefaultSemanticHighlighting {

    @Override
    public boolean consumes(SemanticToken token) {
      DartNode node = token.getNode();
      return node instanceof DartIntegerLiteral || node instanceof DartDoubleLiteral;
    }

    @Override
    public RGB getDefaultDefaultTextColor() {
      return new RGB(0x00, 0x70, 0x00);
    }

    @Override
    public String getDisplayName() {
      return DartEditorMessages.SemanticHighlighting_number;
    }

    @Override
    public String getPreferenceKey() {
      return NUMBER;
    }

    @Override
    public boolean isEnabledByDefault() {
      return true;
    }
  }

  private static class ParameterHighlighting extends DefaultSemanticHighlighting {

    @Override
    public boolean consumesIdentifier(SemanticToken token) {
      DartIdentifier node = token.getNodeIdentifier();
      Element element = node.getElement();
      if (ElementKind.of(element) == ElementKind.PARAMETER) {
        return true;
      }
      if (PropertyDescriptorHelper.getLocationInParent(node) == PropertyDescriptorHelper.DART_NAMED_EXPRESSION_NAME) {
        if (PropertyDescriptorHelper.getLocationInParent(node.getParent()) == PropertyDescriptorHelper.DART_INVOCATION_ARGS) {
          return true;
        }
      }
      return false;
    }

    @Override
    public RGB getDefaultDefaultTextColor() {
      return new RGB(0, 0, 0); // same as local
    }

    @Override
    public String getDisplayName() {
      return DartEditorMessages.SemanticHighlighting_parameterVariable;
    }

    @Override
    public String getPreferenceKey() {
      return PARAMETER_VARIABLE;
    }

    @Override
    public boolean isEnabledByDefault() {
      return true;
    }
  }

  private static class SetterDeclarationHighlighting extends MethodDeclarationHighlighting {

    @Override
    public boolean consumes(SemanticToken token) {
      DartNode node = token.getNode();
      {
        DartMethodDefinition parentMethod = getParentMethod(node);
        if (parentMethod != null && parentMethod.getName() == node) {
          if (parentMethod.getElement().getModifiers().isSetter()) {
            return true;
          }
        }
      }
      return false;
    }

    @Override
    public RGB getDefaultDefaultTextColor() {
      return defaultFieldColor();
    }

    @Override
    public String getDisplayName() {
      return DartEditorMessages.SemanticHighlighting_setter;
    }

    @Override
    public String getPreferenceKey() {
      return SETTER_DECLARATION;
    }
  }

  /**
   * Semantic highlighting for static fields.
   */
  private static class StaticFieldHighlighting extends FieldHighlighting {
    @Override
    public boolean consumesIdentifier(SemanticToken token) {
      DartIdentifier node = token.getNodeIdentifier();
      NodeElement element = node.getElement();
      if (element == null || element.isDynamic()) {
        return false;
      }
      if (element instanceof FieldElement) {
        return ((FieldElement) element).isStatic();
      }
      return false;
    }

    @Override
    public String getDisplayName() {
      return DartEditorMessages.SemanticHighlighting_staticField;
    }

    @Override
    public String getPreferenceKey() {
      return STATIC_FIELD;
    }

    @Override
    public boolean isItalicByDefault() {
      return true;
    }

  }

  private static class StaticMethodDeclarationHighlighting extends MethodDeclarationHighlighting {

    @Override
    public boolean consumes(SemanticToken token) {
      DartNode node = token.getNode();
      DartMethodDefinition parentMethod = getParentMethod(node);
      if (parentMethod == null || parentMethod.getName() != node) {
        return false;
      }
      if (!parentMethod.getModifiers().isStatic()) {
        return false;
      }
      return parentMethod.getName() == node;
    }

    @Override
    public String getDisplayName() {
      return DartEditorMessages.SemanticHighlighting_methodDeclaration;
    }

    @Override
    public String getPreferenceKey() {
      return STATIC_METHOD_DECLARATION;
    }

    @Override
    public boolean isEnabledByDefault() {
      return true;
    }

    @Override
    public boolean isItalicByDefault() {
      return true;
    }
  }

  private static class StaticMethodHighlighting extends MethodHighlighting {

    @Override
    public boolean consumesIdentifier(SemanticToken token) {
      DartIdentifier node = token.getNodeIdentifier();
      NodeElement element = node.getElement();
      return ElementKind.of(element) == ElementKind.METHOD && element.getModifiers().isStatic();
    }

    @Override
    public String getDisplayName() {
      return DartEditorMessages.SemanticHighlighting_staticMethod;
    }

    @Override
    public String getPreferenceKey() {
      return STATIC_METHOD;
    }

    @Override
    public boolean isEnabledByDefault() {
      return true;
    }

    @Override
    public boolean isItalicByDefault() {
      return true;
    }
  }

  private static final RGB KEY_WORD_COLOR = PreferenceConverter.getColor(
      DartToolsPlugin.getDefault().getPreferenceStore(),
      IDartColorConstants.JAVA_KEYWORD);

//  /**
//   * Semantic highlighting for top level members.
//   */
//  private static class TopLevelMemberHighlighting extends DefaultSemanticHighlighting {
//    @Override
//    public boolean consumesIdentifier(SemanticToken token) {
//      DartIdentifier node = token.getNodeIdentifier();
//      NodeElement element = node.getElement();
//
//      if (element == null || element instanceof ClassElement
//          || element instanceof LibraryPrefixElement) {
//        return false;
//      }
//
//      DartNode parent = node.getParent();
//      if (parent instanceof DartDeclaration<?>) {
//        if (((DartDeclaration<?>) parent).getName().equals(node)) {
//          return false;
//        }
//      }
//
//      return element.getEnclosingElement() instanceof LibraryElement;
//    }
//
//    @Override
//    public RGB getDefaultDefaultTextColor() {
//      return new RGB(0x40, 0x40, 0x40);
//    }
//
//    @Override
//    public String getDisplayName() {
//      return DartEditorMessages.SemanticHighlighting_topLevelMember;
//    }
//
//    @Override
//    public String getPreferenceKey() {
//      return TOP_LEVEL_MEMBER;
//    }
//
//    @Override
//    public boolean isBoldByDefault() {
//      return true;
//    }
//
//    @Override
//    public boolean isEnabledByDefault() {
//      return true;
//    }
//
//    @Override
//    public boolean isItalicByDefault() {
//      return false;
//    }
//
//  }

  // Constants used to distinguish annotations from fields.
  private static final String[] META_NAMES = {"deprecated", "override"};
  private static final String META_LIB_NAME = "meta";

  /**
   * A named preference part that controls the highlighting of deprecated elements.
   */
  public static final String DEPRECATED_ELEMENT = "deprecated"; //$NON-NLS-1$

  /**
   * A named preference part that controls the highlighting of static final fields.
   */
  public static final String STATIC_FINAL_FIELD = "staticFinalField"; //$NON-NLS-1$

  /**
   * A named preference part that controls the highlighting of static fields.
   */
  public static final String STATIC_FIELD = "staticField"; //$NON-NLS-1$

  /**
   * A named preference part that controls the highlighting of top level members.
   */
  public static final String TOP_LEVEL_MEMBER = "topLevelMember"; //$NON-NLS-1$

  /**
   * A named preference part that controls the highlighting of built-in identifiers.
   */
  public static final String BUILT_IN = "builtin"; //$NON-NLS-1$

  /**
   * A named preference part that controls the highlighting of directives.
   */
  public static final String DIRECTIVE = "directive"; //$NON-NLS-1$

  /**
   * A named preference part that controls the highlighting of fields.
   */
  public static final String FIELD = "field"; //$NON-NLS-1$

  /**
   * A named preference part that controls the highlighting of method declarations.
   */
  public static final String METHOD_DECLARATION = "methodDeclarationName"; //$NON-NLS-1$

  /**
   * A named preference part that controls the highlighting of static method declarations.
   */
  public static final String STATIC_METHOD_DECLARATION = "staticMethodDeclarationName"; //$NON-NLS-1$

  /**
   * A named preference part that controls the highlighting of static method invocations.
   */
  public static final String STATIC_METHOD_INVOCATION = "staticMethodInvocation"; //$NON-NLS-1$

  /**
   * A named preference part that controls the highlighting of inherited method invocations.
   */
  public static final String INHERITED_METHOD_INVOCATION = "inheritedMethodInvocation"; //$NON-NLS-1$

  /**
   * A named preference part that controls the highlighting of abstract method invocations.
   */
  public static final String ABSTRACT_METHOD_INVOCATION = "abstractMethodInvocation"; //$NON-NLS-1$

  /**
   * A named preference part that controls the highlighting of local variables.
   */
  public static final String LOCAL_VARIABLE_DECLARATION = "localVariableDeclaration"; //$NON-NLS-1$

  /**
   * A named preference part that controls the highlighting of getters.
   */
  public static final String GETTER_DECLARATION = "getterDeclaration"; //$NON-NLS-1$

  /**
   * A named preference part that controls the highlighting of setters.
   */
  public static final String SETTER_DECLARATION = "setterDeclaration"; //$NON-NLS-1$

  /**
   * A named preference part that controls the highlighting of local variables.
   */
  public static final String LOCAL_VARIABLE = "localVariable"; //$NON-NLS-1$

  /**
   * A named preference part that controls the highlighting of parameter variables.
   */
  public static final String PARAMETER_VARIABLE = "parameterVariable"; //$NON-NLS-1$

  /**
   * A named preference part that controls the highlighting of dynamic types.
   */
  public static final String DYNAMIC_TYPE = "dynamicType"; //$NON-NLS-1$

  /**
   * A named preference part that controls the highlighting of type parameters.
   */
  public static final String TYPE_VARIABLE = "typeParameter"; //$NON-NLS-1$

  /**
   * A named preference part that controls the highlighting of type parameters.
   * 
   * @author STP
   */
  public static final String OBJECT_INITIALIZER = "objectInitializer"; //$NON-NLS-1$

  /**
   * A named preference part that controls the highlighting of methods (invocations and
   * declarations).
   */
  public static final String METHOD = "method"; //$NON-NLS-1$

  /**
   * A named preference part that controls the highlighting of static methods (invocations and
   * declarations).
   */
  public static final String STATIC_METHOD = "staticMethod"; //$NON-NLS-1$

  /**
   * A named preference part that controls the highlighting of classes.
   */
  public static final String CLASS = "class"; //$NON-NLS-1$

  /**
   * A named preference part that controls the highlighting of interfaces.
   */
  public static final String INTERFACE = "interface"; //$NON-NLS-1$

  /**
   * A named preference part that controls the highlighting of annotations.
   */
  public static final String ANNOTATION = "annotation"; //$NON-NLS-1$

  /**
   * A named preference part that controls the highlighting of type arguments.
   */
  public static final String TYPE_ARGUMENT = "typeArgument"; //$NON-NLS-1$

  /**
   * A named preference part that controls the highlighting of numbers.
   */
  public static final String NUMBER = "number"; //$NON-NLS-1$

  /**
   * Semantic highlightings
   */
  private static SemanticHighlighting[] SEMANTIC_HIGHTLIGHTINGS;

  /**
   * Tests whether <code>event</code> in <code>store</code> affects the enablement of semantic
   * highlighting.
   * 
   * @param store the preference store where <code>event</code> was observed
   * @param event the property change under examination
   * @return <code>true</code> if <code>event</code> changed semantic highlighting enablement,
   *         <code>false</code> if it did not
   */
  public static boolean affectsEnablement(IPreferenceStore store, PropertyChangeEvent event) {
    return false;
//    String relevantKey = null;
//    SemanticHighlighting[] highlightings = getSemanticHighlightings();
//    for (int i = 0; i < highlightings.length; i++) {
//      if (event.getProperty().equals(getEnabledPreferenceKey(highlightings[i]))) {
//        relevantKey = event.getProperty();
//        break;
//      }
//    }
//    if (relevantKey == null) {
//      return false;
//    }
//
//    for (int i = 0; i < highlightings.length; i++) {
//      String key = getEnabledPreferenceKey(highlightings[i]);
//      if (key.equals(relevantKey)) {
//        continue;
//      }
//      if (store.getBoolean(key)) {
//        return false; // another is still enabled or was enabled before
//      }
//    }
//
//    // all others are disabled, so toggling relevantKey affects the enablement
//    return true;
  }

  /**
   * A named preference that controls if the given semantic highlighting has the text attribute
   * bold.
   * 
   * @param semanticHighlighting the semantic highlighting
   * @return the bold preference key
   */
  public static String getBoldPreferenceKey(SemanticHighlighting semanticHighlighting) {
    return PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_PREFIX
        + semanticHighlighting.getPreferenceKey()
        + PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_BOLD_SUFFIX;
  }

  /**
   * A named preference that controls the given semantic highlighting's color.
   * 
   * @param semanticHighlighting the semantic highlighting
   * @return the color preference key
   */
  public static String getColorPreferenceKey(SemanticHighlighting semanticHighlighting) {
    return PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_PREFIX
        + semanticHighlighting.getPreferenceKey()
        + PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_COLOR_SUFFIX;
  }

  /**
   * A named preference that controls if the given semantic highlighting is enabled.
   * 
   * @param semanticHighlighting the semantic highlighting
   * @return the enabled preference key
   */
  public static String getEnabledPreferenceKey(SemanticHighlighting semanticHighlighting) {
    return PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_PREFIX
        + semanticHighlighting.getPreferenceKey()
        + PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_ENABLED_SUFFIX;
  }

  /**
   * A named preference that controls if the given semantic highlighting has the text attribute
   * italic.
   * 
   * @param semanticHighlighting the semantic highlighting
   * @return the italic preference key
   */
  public static String getItalicPreferenceKey(SemanticHighlighting semanticHighlighting) {
    return PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_PREFIX
        + semanticHighlighting.getPreferenceKey()
        + PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_ITALIC_SUFFIX;
  }

  /**
   * @return The semantic highlightings, the order defines the precedence of matches, the first
   *         match wins.
   */
  public static SemanticHighlighting[] getSemanticHighlightings() {
    if (SEMANTIC_HIGHTLIGHTINGS == null) {
      SEMANTIC_HIGHTLIGHTINGS = new SemanticHighlighting[] {
          new DirectiveHighlighting(), new BuiltInHighlighting(),
          new DeprecatedElementHighlighting(), new GetterDeclarationHighlighting(),
          new SetterDeclarationHighlighting(), new StaticFieldHighlighting(),
          new FieldHighlighting(), new DynamicTypeHighlighting(), new ClassHighlighting(),
          new NumberHighlighting(), new LocalVariableDeclarationHighlighting(),
          new LocalVariableHighlighting(), new ParameterHighlighting(),
          new StaticMethodDeclarationHighlighting(), new StaticMethodHighlighting(),
          new MethodDeclarationHighlighting(), new MethodHighlighting()};
    }
    return SEMANTIC_HIGHTLIGHTINGS;
  }

  /**
   * A named preference that controls if the given semantic highlighting has the text attribute
   * strikethrough.
   * 
   * @param semanticHighlighting the semantic highlighting
   * @return the strikethrough preference key
   */
  public static String getStrikethroughPreferenceKey(SemanticHighlighting semanticHighlighting) {
    return PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_PREFIX
        + semanticHighlighting.getPreferenceKey()
        + PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_STRIKETHROUGH_SUFFIX;
  }

  /**
   * A named preference that controls if the given semantic highlighting has the text attribute
   * underline.
   * 
   * @param semanticHighlighting the semantic highlighting
   * @return the underline preference key
   */
  public static String getUnderlinePreferenceKey(SemanticHighlighting semanticHighlighting) {
    return PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_PREFIX
        + semanticHighlighting.getPreferenceKey()
        + PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_UNDERLINE_SUFFIX;
  }

  /**
   * Initialize default preferences in the given preference store.
   * 
   * @param store The preference store
   */
  public static void initDefaults(IPreferenceStore store) {
    SemanticHighlighting[] semanticHighlightings = getSemanticHighlightings();
    for (SemanticHighlighting highlighting : semanticHighlightings) {
      setDefaultAndFireEvent(
          store,
          getColorPreferenceKey(highlighting),
          highlighting.getDefaultTextColor());
      store.setDefault(getBoldPreferenceKey(highlighting), highlighting.isBoldByDefault());
      store.setDefault(getItalicPreferenceKey(highlighting), highlighting.isItalicByDefault());
      store.setDefault(
          getStrikethroughPreferenceKey(highlighting),
          highlighting.isStrikethroughByDefault());
      store.setDefault(getUnderlinePreferenceKey(highlighting), highlighting.isUnderlineByDefault());
      store.setDefault(getEnabledPreferenceKey(highlighting), highlighting.isEnabledByDefault());
    }
  }

  /**
   * Tests whether semantic highlighting is currently enabled.
   * 
   * @param store the preference store to consult
   * @return <code>true</code> if semantic highlighting is enabled, <code>false</code> if it is not
   */
  public static boolean isEnabled(IPreferenceStore store) {
    SemanticHighlighting[] highlightings = getSemanticHighlightings();
    for (SemanticHighlighting highlighting : highlightings) {
      String enabledKey = getEnabledPreferenceKey(highlighting);
      if (store.getBoolean(enabledKey)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Sets the default value and fires a property change event if necessary.
   * 
   * @param store the preference store
   * @param key the preference key
   * @param newValue the new value
   */
  private static void setDefaultAndFireEvent(IPreferenceStore store, String key, RGB newValue) {
    RGB oldValue = null;
    if (store.isDefault(key)) {
      oldValue = PreferenceConverter.getDefaultColor(store, key);
    }

    PreferenceConverter.setDefault(store, key, newValue);

    if (oldValue != null && !oldValue.equals(newValue)) {
      store.firePropertyChangeEvent(key, oldValue, newValue);
    }
  }

  /**
   * Do not instantiate
   */
  private SemanticHighlightings() {
  }

  public RGB getDefaultDefaultTextColor() {
    return new RGB(13, 100, 0);
  }
}
