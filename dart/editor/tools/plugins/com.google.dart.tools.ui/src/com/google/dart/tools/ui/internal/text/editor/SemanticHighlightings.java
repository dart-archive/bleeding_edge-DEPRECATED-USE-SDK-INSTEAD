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
package com.google.dart.tools.ui.internal.text.editor;

import com.google.common.collect.Lists;
import com.google.dart.engine.ast.Annotation;
import com.google.dart.engine.ast.ArgumentList;
import com.google.dart.engine.ast.AsExpression;
import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.AwaitExpression;
import com.google.dart.engine.ast.CatchClause;
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.Combinator;
import com.google.dart.engine.ast.ConstructorDeclaration;
import com.google.dart.engine.ast.DoubleLiteral;
import com.google.dart.engine.ast.EnumConstantDeclaration;
import com.google.dart.engine.ast.ExportDirective;
import com.google.dart.engine.ast.FieldDeclaration;
import com.google.dart.engine.ast.FieldFormalParameter;
import com.google.dart.engine.ast.ForEachStatement;
import com.google.dart.engine.ast.FunctionBody;
import com.google.dart.engine.ast.FunctionDeclaration;
import com.google.dart.engine.ast.ImplementsClause;
import com.google.dart.engine.ast.ImportDirective;
import com.google.dart.engine.ast.IntegerLiteral;
import com.google.dart.engine.ast.LibraryDirective;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.ast.NativeClause;
import com.google.dart.engine.ast.NativeFunctionBody;
import com.google.dart.engine.ast.PartDirective;
import com.google.dart.engine.ast.PartOfDirective;
import com.google.dart.engine.ast.PrefixedIdentifier;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.StringLiteral;
import com.google.dart.engine.ast.TryStatement;
import com.google.dart.engine.ast.TypeAlias;
import com.google.dart.engine.ast.TypeName;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.ast.VariableDeclarationList;
import com.google.dart.engine.ast.VariableDeclarationStatement;
import com.google.dart.engine.ast.YieldStatement;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.ConstructorElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ElementKind;
import com.google.dart.engine.element.FieldFormalParameterElement;
import com.google.dart.engine.element.FunctionElement;
import com.google.dart.engine.element.FunctionTypeAliasElement;
import com.google.dart.engine.element.ImportElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.element.PrefixElement;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.element.PropertyInducingElement;
import com.google.dart.engine.element.TypeParameterElement;
import com.google.dart.engine.element.VariableElement;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.type.Type;
import com.google.dart.engine.utilities.source.SourceRange;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.PreferenceConstants;
import com.google.dart.tools.ui.text.IDartColorConstants;

import static com.google.dart.engine.utilities.source.SourceRangeFactory.rangeNode;
import static com.google.dart.engine.utilities.source.SourceRangeFactory.rangeStartEnd;
import static com.google.dart.engine.utilities.source.SourceRangeFactory.rangeToken;

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
   * Semantic highlighting for annotations.
   */
  private static class AnnotationHighlighting extends DefaultSemanticHighlighting {
    @Override
    public List<SourceRange> consumesMulti(SemanticToken token) {
      AstNode node = token.getNode();
      if (node instanceof Annotation) {
        Annotation annotation = (Annotation) node;
        List<SourceRange> positions = Lists.newArrayList();
        ArgumentList arguments = annotation.getArguments();
        if (arguments != null) {
          positions.add(rangeStartEnd(annotation, arguments.getBeginToken()));
          positions.add(rangeToken(arguments.getRightParenthesis()));
        } else {
          positions.add(rangeNode(annotation));
        }
        return positions;
      }
      return null;
    }

    @Override
    public RGB getDefaultDefaultTextColor() {
      return defaultFieldColor();
    }

    @Override
    public String getDisplayName() {
      return DartEditorMessages.SemanticHighlighting_annotation;
    }

    @Override
    public String getPreferenceKey() {
      return ANNOTATION;
    }

    @Override
    public boolean isEnabledByDefault() {
      return true;
    }
  }

  /**
   * Highlights build-in identifiers - "abstract", "as", "dynamic", "typedef", etc.
   */
  private static class BuiltInHighlighting extends DefaultSemanticHighlighting {

    @Override
    public boolean consumesIdentifier(SemanticToken token) {
      SimpleIdentifier node = token.getNodeIdentifier();
      // "dynamic" as type
      if (node.getParent() instanceof TypeName) {
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
      List<SourceRange> result = null;
      // prepare ASTNode
      AstNode node = token.getNode();
      // typedef
      if (node instanceof TypeAlias) {
        TypeAlias typeAlias = (TypeAlias) node;
        Token keyword = typeAlias.getKeyword();
        result = addPosition(result, keyword);
      }
      // as
      if (node instanceof AsExpression) {
        AsExpression asExpression = (AsExpression) node;
        Token asOperator = asExpression.getAsOperator();
        result = addPosition(result, asOperator);
      }
      // field modifiers
      if (node instanceof FieldDeclaration) {
        FieldDeclaration fieldDecl = (FieldDeclaration) node;
        Token staticKeyword = fieldDecl.getStaticKeyword();
        if (staticKeyword != null) {
          result = addPosition(result, staticKeyword);
        }
      }
      // function modifiers
      if (node instanceof FunctionDeclaration) {
        FunctionDeclaration function = (FunctionDeclaration) node;
        {
          Token keyword = function.getExternalKeyword();
          if (keyword != null) {
            result = addPosition(result, keyword);
          }
        }
        {
          Token keyword = function.getPropertyKeyword();
          if (keyword != null) {
            result = addPosition(result, keyword);
          }
        }
      }
      // function body modifier
      if (node instanceof FunctionBody) {
        FunctionBody body = (FunctionBody) node;
        Token keyword = body.getKeyword();
        if (keyword != null) {
          Token star = body.getStar();
          int offset = keyword.getOffset();
          int end = star != null ? star.getEnd() : keyword.getEnd();
          result = addPosition(result, rangeStartEnd(offset, end));
        }
      }
      // class
      if (node instanceof ClassDeclaration) {
        ClassDeclaration clazz = (ClassDeclaration) node;
        // "abstract"
        {
          Token keyword = clazz.getAbstractKeyword();
          if (keyword != null) {
            result = addPosition(result, keyword);
          }
        }
        // implements
        {
          ImplementsClause implementsClause = clazz.getImplementsClause();
          if (implementsClause != null) {
            Token keyword = implementsClause.getKeyword();
            if (keyword != null) {
              result = addPosition(result, keyword);
            }
          }
        }
        // native
        {
          NativeClause nativeClause = clazz.getNativeClause();
          if (nativeClause != null) {
            Token keyword = nativeClause.getKeyword();
            if (keyword != null) {
              result = addPosition(result, keyword);
            }
          }
        }
      }
      // constructor modifiers
      if (node instanceof ConstructorDeclaration) {
        ConstructorDeclaration method = (ConstructorDeclaration) node;
        {
          Token keyword = method.getExternalKeyword();
          if (keyword != null) {
            result = addPosition(result, keyword);
          }
        }
        {
          Token keyword = method.getConstKeyword();
          if (keyword != null) {
            result = addPosition(result, keyword);
          }
        }
        {
          Token keyword = method.getFactoryKeyword();
          if (keyword != null) {
            result = addPosition(result, keyword);
          }
        }
      }
      // method modifiers
      if (node instanceof MethodDeclaration) {
        MethodDeclaration method = (MethodDeclaration) node;
        {
          Token keyword = method.getExternalKeyword();
          if (keyword != null) {
            result = addPosition(result, keyword);
          }
        }
        {
          Token keyword = method.getModifierKeyword();
          if (keyword != null) {
            result = addPosition(result, keyword);
          }
        }
        {
          Token keyword = method.getOperatorKeyword();
          if (keyword != null) {
            result = addPosition(result, keyword);
          }
        }
        {
          Token keyword = method.getPropertyKeyword();
          if (keyword != null) {
            result = addPosition(result, keyword);
          }
        }
      }
      // native body
      if (node instanceof NativeFunctionBody) {
        Token keyword = ((NativeFunctionBody) node).getNativeToken();
        if (keyword != null) {
          result = addPosition(result, keyword);
        }
      }
      // try {} on
      if (node instanceof TryStatement) {
        TryStatement tryStatement = (TryStatement) node;
        for (CatchClause catchClause : tryStatement.getCatchClauses()) {
          {
            Token onToken = catchClause.getOnKeyword();
            if (onToken != null) {
              result = addPosition(result, onToken);
            }
          }
        }
      }
      // await
      if (node instanceof AwaitExpression) {
        AwaitExpression awaitExpression = (AwaitExpression) node;
        result = addPosition(result, awaitExpression.getAwaitKeyword());
      }
      if (node instanceof ForEachStatement) {
        ForEachStatement forEachStatement = (ForEachStatement) node;
        result = addPosition(result, forEachStatement.getAwaitKeyword());
      }
      // yield
      if (node instanceof YieldStatement) {
        YieldStatement yieldStatement = (YieldStatement) node;
        Token keyword = yieldStatement.getYieldKeyword();
        if (keyword != null) {
          Token star = yieldStatement.getStar();
          int offset = keyword.getOffset();
          int end = star != null ? star.getEnd() : keyword.getEnd();
          result = addPosition(result, rangeStartEnd(offset, end));
        }
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
  }

  private static class ClassHighlighting extends DefaultSemanticHighlighting {
    @Override
    public boolean consumesIdentifier(SemanticToken token) {
      SimpleIdentifier node = token.getNodeIdentifier();
      // ignore "void" and "dynamic" - they are reserved word and built-in identifier
      {
        String name = node.getName();
        if ("void".equals(name) || "dynamic".equals(name)) {
          return false;
        }
      }
      // ignore if in Annotation
      if (node.getParent() instanceof Annotation) {
        return false;
      }
      if (node.getParent() instanceof PrefixedIdentifier
          && node.getParent().getParent() instanceof Annotation) {
        return false;
      }
      // highlight type name in declaration and use
      if (node.getStaticElement() instanceof ClassElement) {
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

  private static class ConstructorHighlighting extends DefaultSemanticHighlighting {
    @Override
    public boolean consumesIdentifier(SemanticToken token) {
      SimpleIdentifier node = token.getNodeIdentifier();
      Element element = node.getStaticElement();
      return element instanceof ConstructorElement;
    }

    @Override
    public String getDisplayName() {
      return DartEditorMessages.SemanticHighlighting_constructor;
    }

    @Override
    public String getPreferenceKey() {
      return CONSTRUCTOR;
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

    protected List<SourceRange> addPosition(List<SourceRange> positions, SourceRange range) {
      if (positions == null) {
        positions = Lists.newArrayList();
      }
      positions.add(range);
      return positions;
    }

    protected List<SourceRange> addPosition(List<SourceRange> positions, Token token) {
      if (token == null) {
        return positions;
      }
      return addPosition(positions, rangeToken(token));
    }

    RGB defaultFieldColor() {
      return new RGB(0, 0, 192);
    }
  }

  /**
   * Semantic highlighting deprecated elements.
   */
  private static final class DeprecatedElementHighlighting extends DefaultSemanticHighlighting {
    private static boolean isDeprecatedElement(Element element) {
      if (element != null) {
        if (element instanceof PropertyAccessorElement) {
          PropertyAccessorElement accessor = (PropertyAccessorElement) element;
          if (accessor.isSynthetic()) {
            element = accessor.getVariable();
          }
        }
        return element.isDeprecated();
      }
      return false;
    }

    @Override
    public boolean consumes(SemanticToken token) {
      AstNode node = token.getNode();
      if (node instanceof StringLiteral && node.getParent() instanceof ImportDirective) {
        ImportDirective importDirective = (ImportDirective) node.getParent();
        ImportElement importElement = importDirective.getElement();
        if (importElement != null) {
          LibraryElement importedLibrary = importElement.getImportedLibrary();
          return isDeprecatedElement(importedLibrary);
        }
      }
      return false;
    }

    @Override
    public boolean consumesIdentifier(SemanticToken token) {
      SimpleIdentifier node = token.getNodeIdentifier();
      Element element = node.getBestElement();
      return isDeprecatedElement(element);
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
    @Override
    public List<SourceRange> consumesMulti(SemanticToken token) {
      List<SourceRange> result = null;
      AstNode node = token.getNode();
      if (node instanceof LibraryDirective) {
        LibraryDirective directive = (LibraryDirective) node;
        result = addPosition(result, directive.getKeyword());
      }
      if (node instanceof ImportDirective) {
        ImportDirective directive = (ImportDirective) node;
        result = addPosition(result, directive.getKeyword());
        result = addPosition(result, directive.getDeferredToken());
        result = addPosition(result, directive.getAsToken());
        for (Combinator combinator : directive.getCombinators()) {
          result = addPosition(result, combinator.getKeyword());
        }
      }
      if (node instanceof ExportDirective) {
        ExportDirective directive = (ExportDirective) node;
        result = addPosition(result, directive.getKeyword());
        for (Combinator combinator : directive.getCombinators()) {
          result = addPosition(result, combinator.getKeyword());
        }
      }
      if (node instanceof PartDirective) {
        PartDirective directive = (PartDirective) node;
        result = addPosition(result, directive.getKeyword());
      }
      if (node instanceof PartOfDirective) {
        PartOfDirective directive = (PartOfDirective) node;
        result = addPosition(
            result,
            rangeStartEnd(directive.getPartToken(), directive.getOfToken()));
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
  }

  /**
   * Semantic highlighting for variables with dynamic types.
   */
  private static final class DynamicTypeHighlighting extends DefaultSemanticHighlighting {
    @Override
    public boolean consumesIdentifier(SemanticToken token) {
      SimpleIdentifier node = token.getNodeIdentifier();
      // should be variable
      Element element = node.getStaticElement();
      if (!(element instanceof VariableElement)) {
        return false;
      }
      // may be has propagated type
      Type propagatedType = node.getPropagatedType();
      if (propagatedType != null && !propagatedType.isDynamic()) {
        return false;
      }
      // has dynamic static type
      Type staticType = node.getStaticType();
      return staticType != null && staticType.isDynamic();
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
   * Semantic highlighting for static fields.
   */
  private static class EnumConstantHighlighting extends DefaultSemanticHighlighting {
    @Override
    public boolean consumesIdentifier(SemanticToken token) {
      SimpleIdentifier node = token.getNodeIdentifier();
      return node.getParent() instanceof EnumConstantDeclaration;
    }

    @Override
    public String getDisplayName() {
      return DartEditorMessages.SemanticHighlighting_enumConstant;
    }

    @Override
    public String getPreferenceKey() {
      return ENUM_CONSTANT;
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

  private static class EnumHighlighting extends DefaultSemanticHighlighting {
    @Override
    public boolean consumesIdentifier(SemanticToken token) {
      SimpleIdentifier node = token.getNodeIdentifier();
      Element element = node.getStaticElement();
      if (element instanceof ClassElement) {
        return ((ClassElement) element).isEnum();
      }
      return false;
    }

    @Override
    public String getDisplayName() {
      return DartEditorMessages.SemanticHighlighting_enum;
    }

    @Override
    public String getPreferenceKey() {
      return ENUM;
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
      SimpleIdentifier node = token.getNodeIdentifier();
      Element element = node.getBestElement();
      if (node.getParent() instanceof FieldFormalParameter
          && element instanceof FieldFormalParameterElement) {
        element = ((FieldFormalParameterElement) element).getField();
      }
      return element instanceof PropertyInducingElement
          || element instanceof PropertyAccessorElement;
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

  private static class FunctionHighlighting extends DefaultSemanticHighlighting {
    @Override
    public boolean consumesIdentifier(SemanticToken token) {
      SimpleIdentifier node = token.getNodeIdentifier();
      Element element = node.getStaticElement();
      return element instanceof FunctionElement;
    }

    @Override
    public String getDisplayName() {
      return DartEditorMessages.SemanticHighlighting_function;
    }

    @Override
    public String getPreferenceKey() {
      return FUNCTION;
    }

    @Override
    public boolean isEnabledByDefault() {
      return true;
    }
  }

  private static class FunctionTypeAliasHighlighting extends DefaultSemanticHighlighting {
    @Override
    public boolean consumesIdentifier(SemanticToken token) {
      SimpleIdentifier node = token.getNodeIdentifier();
      return node.getStaticElement() instanceof FunctionTypeAliasElement;
    }

    @Override
    public String getDisplayName() {
      return DartEditorMessages.SemanticHighlighting_functionTypeAlias;
    }

    @Override
    public String getPreferenceKey() {
      return FUNCTION_TYPE_ALIAS;
    }

    @Override
    public boolean isEnabledByDefault() {
      return true;
    }
  }

  private static class GetterDeclarationHighlighting extends MethodDeclarationHighlighting {
    @Override
    public boolean consumes(SemanticToken token) {
      AstNode node = token.getNode();
      {
        MethodDeclaration method = getParentMethod(node);
        if (method != null && method.isGetter()) {
          return true;
        }
      }
      {
        FunctionDeclaration function = getParentFunction(node);
        if (function != null && function.isGetter()) {
          return true;
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

  private static class ImportPrefixHighlighting extends DefaultSemanticHighlighting {
    @Override
    public boolean consumesIdentifier(SemanticToken token) {
      SimpleIdentifier node = token.getNodeIdentifier();
      Element element = node.getStaticElement();
      return element instanceof PrefixElement;
    }

    @Override
    public String getDisplayName() {
      return DartEditorMessages.SemanticHighlighting_importPrefix;
    }

    @Override
    public String getPreferenceKey() {
      return IMPORT_PREFIX;
    }

    @Override
    public boolean isEnabledByDefault() {
      return true;
    }
  }

  private static class LocalVariableDeclarationHighlighting extends DefaultSemanticHighlighting {
    @Override
    public boolean consumesIdentifier(SemanticToken token) {
      SimpleIdentifier node = token.getNodeIdentifier();
      return node.getParent() instanceof VariableDeclaration
          && node.getParent().getParent() instanceof VariableDeclarationList
          && node.getParent().getParent().getParent() instanceof VariableDeclarationStatement;
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
      SimpleIdentifier node = token.getNodeIdentifier();
      Element element = node.getStaticElement();
      return ElementKind.of(element) == ElementKind.LOCAL_VARIABLE;
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
    static FunctionDeclaration getParentFunction(AstNode node) {
      if (node.getParent() instanceof FunctionDeclaration) {
        FunctionDeclaration method = (FunctionDeclaration) node.getParent();
        if (method.getName() == node) {
          return method;
        }
      }
      return null;
    }

    static MethodDeclaration getParentMethod(AstNode node) {
      if (node.getParent() instanceof MethodDeclaration) {
        MethodDeclaration method = (MethodDeclaration) node.getParent();
        if (method.getName() == node) {
          return method;
        }
      }
      return null;
    }

    @Override
    public boolean consumes(SemanticToken token) {
      AstNode node = token.getNode();
      {
        MethodDeclaration method = getParentMethod(node);
        if (method != null) {
          return true;
        }
      }
      {
        FunctionDeclaration function = getParentFunction(node);
        if (function != null) {
          return true;
        }
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
      SimpleIdentifier node = token.getNodeIdentifier();
      Element element = node.getBestElement();
      return element instanceof MethodElement;
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
      AstNode node = token.getNode();
      return node instanceof IntegerLiteral || node instanceof DoubleLiteral;
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
      SimpleIdentifier node = token.getNodeIdentifier();
      Element element = node.getStaticElement();
      return ElementKind.of(element) == ElementKind.PARAMETER;
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
      AstNode node = token.getNode();
      {
        MethodDeclaration method = getParentMethod(node);
        if (method != null && method.isSetter()) {
          return true;
        }
      }
      {
        FunctionDeclaration function = getParentFunction(node);
        if (function != null && function.isSetter()) {
          return true;
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
      SimpleIdentifier node = token.getNodeIdentifier();
      Element element = node.getStaticElement();
      if (element instanceof PropertyInducingElement) {
        return ((PropertyInducingElement) element).isStatic();
      }
      if (element instanceof PropertyAccessorElement) {
        return ((PropertyAccessorElement) element).isStatic();
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
      AstNode node = token.getNode();
      MethodDeclaration method = getParentMethod(node);
      return method != null && method.isStatic();
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
      SimpleIdentifier node = token.getNodeIdentifier();
      Element element = node.getStaticElement();
      if (element instanceof MethodElement) {
        return ((MethodElement) element).isStatic();
      }
      return false;
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

  private static class TypeVariableHighlighting extends DefaultSemanticHighlighting {
    @Override
    public boolean consumesIdentifier(SemanticToken token) {
      SimpleIdentifier node = token.getNodeIdentifier();
      return node.getStaticElement() instanceof TypeParameterElement;
    }

    @Override
    public String getDisplayName() {
      return DartEditorMessages.SemanticHighlighting_typeVariable;
    }

    @Override
    public String getPreferenceKey() {
      return TYPE_VARIABLE;
    }

    @Override
    public boolean isEnabledByDefault() {
      return true;
    }
  }

  private static final RGB KEY_WORD_COLOR = PreferenceConverter.getColor(
      DartToolsPlugin.getDefault().getPreferenceStore(),
      IDartColorConstants.JAVA_KEYWORD);

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
   * A named preference part that controls the highlighting of constructor.
   */
  public static final String CONSTRUCTOR = "constructor"; //$NON-NLS-1$

  /**
   * A named preference part that controls the highlighting of enumerations.
   */
  public static final String ENUM = "enum"; //$NON-NLS-1$

  /**
   * A named preference part that controls the highlighting of enumeration constants.
   */
  public static final String ENUM_CONSTANT = "enumConstant"; //$NON-NLS-1$

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
   * A named preference part that controls the highlighting of methods (invocations and references).
   */
  public static final String METHOD = "method"; //$NON-NLS-1$

  /**
   * A named preference part that controls the highlighting of functions (invocations and
   * references).
   */
  public static final String FUNCTION = "function"; //$NON-NLS-1$

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
   * A named preference part that controls the highlighting of function type aliases (typedefs).
   */
  public static final String FUNCTION_TYPE_ALIAS = "functionTypeAlias"; //$NON-NLS-1$

  /**
   * A named preference part that controls the highlighting of import prefix.
   */
  public static final String IMPORT_PREFIX = "importPrefix"; //$NON-NLS-1$

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
          new SetterDeclarationHighlighting(), new AnnotationHighlighting(),
          new EnumConstantHighlighting(), new StaticFieldHighlighting(), new FieldHighlighting(),
          new DynamicTypeHighlighting(), new EnumHighlighting(), new ClassHighlighting(),
          new FunctionTypeAliasHighlighting(), new TypeVariableHighlighting(),
          new NumberHighlighting(), new LocalVariableDeclarationHighlighting(),
          new LocalVariableHighlighting(), new ParameterHighlighting(),
          new StaticMethodDeclarationHighlighting(), new StaticMethodHighlighting(),
          new ConstructorHighlighting(), new MethodDeclarationHighlighting(),
          new MethodHighlighting(), new FunctionHighlighting(), new ImportPrefixHighlighting()};
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

    if (newValue != null) {
      PreferenceConverter.setDefault(store, key, newValue);

      if (oldValue != null && !oldValue.equals(newValue)) {
        store.firePropertyChangeEvent(key, oldValue, newValue);
      }
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
