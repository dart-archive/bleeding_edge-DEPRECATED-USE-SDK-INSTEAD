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

package com.google.dart.engine.services.internal.correction;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.dart.engine.ast.ArgumentList;
import com.google.dart.engine.ast.AsExpression;
import com.google.dart.engine.ast.AssertStatement;
import com.google.dart.engine.ast.AssignmentExpression;
import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.BinaryExpression;
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.ClassMember;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.CompilationUnitMember;
import com.google.dart.engine.ast.ConstructorDeclaration;
import com.google.dart.engine.ast.ConstructorInitializer;
import com.google.dart.engine.ast.ConstructorName;
import com.google.dart.engine.ast.Directive;
import com.google.dart.engine.ast.DoStatement;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.ExpressionStatement;
import com.google.dart.engine.ast.FieldDeclaration;
import com.google.dart.engine.ast.FunctionBody;
import com.google.dart.engine.ast.Identifier;
import com.google.dart.engine.ast.IfStatement;
import com.google.dart.engine.ast.ImportDirective;
import com.google.dart.engine.ast.InstanceCreationExpression;
import com.google.dart.engine.ast.IsExpression;
import com.google.dart.engine.ast.LibraryDirective;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.ast.MethodInvocation;
import com.google.dart.engine.ast.NamespaceDirective;
import com.google.dart.engine.ast.ParenthesizedExpression;
import com.google.dart.engine.ast.PartDirective;
import com.google.dart.engine.ast.PrefixExpression;
import com.google.dart.engine.ast.PrefixedIdentifier;
import com.google.dart.engine.ast.ReturnStatement;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.SimpleStringLiteral;
import com.google.dart.engine.ast.TypeName;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.ast.WhileStatement;
import com.google.dart.engine.ast.visitor.NodeLocator;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.ConstructorElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ElementKind;
import com.google.dart.engine.element.ExecutableElement;
import com.google.dart.engine.element.FunctionElement;
import com.google.dart.engine.element.ImportElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.element.NamespaceCombinator;
import com.google.dart.engine.element.ParameterElement;
import com.google.dart.engine.element.PrefixElement;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.element.ShowElementCombinator;
import com.google.dart.engine.element.VariableElement;
import com.google.dart.engine.element.visitor.RecursiveElementVisitor;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.AnalysisErrorWithProperties;
import com.google.dart.engine.error.CompileTimeErrorCode;
import com.google.dart.engine.error.ErrorCode;
import com.google.dart.engine.error.ErrorProperty;
import com.google.dart.engine.error.HintCode;
import com.google.dart.engine.error.StaticTypeWarningCode;
import com.google.dart.engine.error.StaticWarningCode;
import com.google.dart.engine.internal.type.VoidTypeImpl;
import com.google.dart.engine.parser.ParserErrorCode;
import com.google.dart.engine.scanner.TokenType;
import com.google.dart.engine.sdk.DartSdk;
import com.google.dart.engine.sdk.SdkLibrary;
import com.google.dart.engine.services.assist.AssistContext;
import com.google.dart.engine.services.change.Edit;
import com.google.dart.engine.services.change.SourceChange;
import com.google.dart.engine.services.correction.AddDependencyCorrectionProposal;
import com.google.dart.engine.services.correction.CorrectionImage;
import com.google.dart.engine.services.correction.CorrectionKind;
import com.google.dart.engine.services.correction.CorrectionProposal;
import com.google.dart.engine.services.correction.CreateFileCorrectionProposal;
import com.google.dart.engine.services.correction.LinkedPositionProposal;
import com.google.dart.engine.services.correction.QuickFixProcessor;
import com.google.dart.engine.services.correction.SourceCorrectionProposal;
import com.google.dart.engine.services.internal.correction.CorrectionUtils.InsertDesc;
import com.google.dart.engine.services.util.HierarchyUtils;
import com.google.dart.engine.source.FileBasedSource;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceFactory;
import com.google.dart.engine.type.FunctionType;
import com.google.dart.engine.type.InterfaceType;
import com.google.dart.engine.type.Type;
import com.google.dart.engine.utilities.dart.ParameterKind;
import com.google.dart.engine.utilities.instrumentation.Instrumentation;
import com.google.dart.engine.utilities.instrumentation.InstrumentationBuilder;
import com.google.dart.engine.utilities.source.SourceRange;
import com.google.dart.engine.utilities.source.SourceRangeFactory;

import static com.google.dart.engine.utilities.source.SourceRangeFactory.rangeEndEnd;
import static com.google.dart.engine.utilities.source.SourceRangeFactory.rangeEndStart;
import static com.google.dart.engine.utilities.source.SourceRangeFactory.rangeError;
import static com.google.dart.engine.utilities.source.SourceRangeFactory.rangeNode;
import static com.google.dart.engine.utilities.source.SourceRangeFactory.rangeShowCombinator;
import static com.google.dart.engine.utilities.source.SourceRangeFactory.rangeStartLength;
import static com.google.dart.engine.utilities.source.SourceRangeFactory.rangeStartStart;
import static com.google.dart.engine.utilities.source.SourceRangeFactory.rangeToken;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Implementation of {@link QuickFixProcessor}.
 */
public class QuickFixProcessorImpl implements QuickFixProcessor {
  /**
   * Helper for finding {@link Element} with name closest to the given.
   */
  private static class ClosestElementFinder {
    private final String targetName;
    private final Predicate<Element> predicate;
    Element element = null;
    int distance = Integer.MAX_VALUE;

    public ClosestElementFinder(String targetName, Predicate<Element> predicate) {
      this.targetName = targetName;
      this.predicate = predicate;
    }

    void update(Element element) {
      if (predicate.apply(element)) {
        int memberDistance = StringUtils.getLevenshteinDistance(element.getName(), targetName);
        if (memberDistance < distance) {
          this.element = element;
          this.distance = memberDistance;
        }
      }
    }

    void update(Iterable<? extends Element> elements) {
      for (Element element : elements) {
        update(element);
      }
    }
  }
  /**
   * Described location for newly created {@link ConstructorDeclaration}.
   */
  private static class NewConstructorLocation {
    final String prefix;
    final int offset;
    final String suffix;

    public NewConstructorLocation(String prefix, int offset, String suffix) {
      this.prefix = prefix;
      this.offset = offset;
      this.suffix = suffix;
    }
  }

  private static final ErrorCode[] FIXABLE_ERROR_CODES = {
      CompileTimeErrorCode.CONST_INITIALIZED_WITH_NON_CONSTANT_VALUE,
      CompileTimeErrorCode.NO_DEFAULT_SUPER_CONSTRUCTOR_EXPLICIT,
      CompileTimeErrorCode.NO_DEFAULT_SUPER_CONSTRUCTOR_IMPLICIT,
      CompileTimeErrorCode.UNDEFINED_CONSTRUCTOR_IN_INITIALIZER_DEFAULT,
      CompileTimeErrorCode.URI_DOES_NOT_EXIST,
      //
      HintCode.DIVISION_OPTIMIZATION,
      HintCode.TYPE_CHECK_IS_NOT_NULL,
      HintCode.TYPE_CHECK_IS_NULL,
      HintCode.UNNECESSARY_CAST,
      HintCode.UNUSED_IMPORT,
      HintCode.UNDEFINED_METHOD,
      //
      ParserErrorCode.EXPECTED_TOKEN,
      ParserErrorCode.GETTER_WITH_PARAMETERS,
      //
      StaticWarningCode.CONCRETE_CLASS_WITH_ABSTRACT_MEMBER,
      StaticWarningCode.EXTRA_POSITIONAL_ARGUMENTS,
      StaticWarningCode.NEW_WITH_UNDEFINED_CONSTRUCTOR,
      StaticWarningCode.NON_ABSTRACT_CLASS_INHERITS_ABSTRACT_MEMBER_ONE,
      StaticWarningCode.NON_ABSTRACT_CLASS_INHERITS_ABSTRACT_MEMBER_TWO,
      StaticWarningCode.NON_ABSTRACT_CLASS_INHERITS_ABSTRACT_MEMBER_THREE,
      StaticWarningCode.NON_ABSTRACT_CLASS_INHERITS_ABSTRACT_MEMBER_FOUR,
      StaticWarningCode.NON_ABSTRACT_CLASS_INHERITS_ABSTRACT_MEMBER_FIVE_PLUS,
      StaticWarningCode.UNDEFINED_CLASS,
      StaticWarningCode.UNDEFINED_CLASS_BOOLEAN,
      StaticWarningCode.UNDEFINED_IDENTIFIER,
      //
      StaticTypeWarningCode.INSTANCE_ACCESS_TO_STATIC_MEMBER,
      StaticTypeWarningCode.INVOCATION_OF_NON_FUNCTION, StaticTypeWarningCode.UNDEFINED_FUNCTION,
      StaticTypeWarningCode.UNDEFINED_GETTER, StaticTypeWarningCode.UNDEFINED_METHOD};

  private static final CorrectionProposal[] NO_PROPOSALS = {};

  /**
   * @return the Java {@link File} which corresponds to the given {@link Source}, may be
   *         {@code null} if cannot be determined.
   */
  @VisibleForTesting
  public static File getSourceFile(Source source) {
    if (source instanceof FileBasedSource) {
      FileBasedSource fileBasedSource = (FileBasedSource) source;
      return new File(fileBasedSource.getFullName()).getAbsoluteFile();
    }
    return null;
  }

  private static void addSuperTypeProposals(SourceBuilder sb, Set<Type> alreadyAdded, Type type) {
    if (type != null && !alreadyAdded.contains(type) && type.getElement() instanceof ClassElement) {
      alreadyAdded.add(type);
      ClassElement element = (ClassElement) type.getElement();
      sb.addProposal(CorrectionImage.IMG_CORRECTION_CLASS, element.getName());
      addSuperTypeProposals(sb, alreadyAdded, element.getSupertype());
      for (InterfaceType interfaceType : element.getInterfaces()) {
        addSuperTypeProposals(sb, alreadyAdded, interfaceType);
      }
    }
  }

  /**
   * @return the {@link Edit} to remove {@link SourceRange}.
   */
  private static Edit createRemoveEdit(SourceRange range) {
    return createReplaceEdit(range, "");
  }

  /**
   * @return the {@link Edit} to replace {@link SourceRange} with "text".
   */
  private static Edit createReplaceEdit(SourceRange range, String text) {
    return new Edit(range.getOffset(), range.getLength(), text);
  }

  /**
   * Attempts to convert the given absolute {@link File} to the "package" {@link URI}.
   * 
   * @param context the {@link AnalysisContext} to work in.
   * @param file the absolute {@link File}, not null.
   * @return the "package" {@link URI}, may be {@code null}.
   */
  private static URI findPackageUri(AnalysisContext context, File file) {
    Source fileSource = new FileBasedSource(file);
    return context.getSourceFactory().restoreUri(fileSource);
  }

  /**
   * @return the suggestions for given {@link Type} and {@link DartExpression}, not empty.
   */
  private static String[] getArgumentNameSuggestions(Set<String> excluded, Type type,
      Expression expression, int index) {
    String[] suggestions = CorrectionUtils.getVariableNameSuggestions(type, expression, excluded);
    if (suggestions.length != 0) {
      return suggestions;
    }
    return new String[] {"arg" + index};
  }

  /**
   * @return <code>true</code> if given {@link DartNode} could be type name.
   */
  private static boolean mayBeTypeIdentifier(AstNode node) {
    if (node instanceof SimpleIdentifier) {
      AstNode parent = node.getParent();
      if (parent instanceof TypeName) {
        return true;
      }
      if (parent instanceof MethodInvocation) {
        MethodInvocation invocation = (MethodInvocation) parent;
        return invocation.getRealTarget() == node;
      }
      if (parent instanceof PrefixedIdentifier) {
        PrefixedIdentifier prefixed = (PrefixedIdentifier) parent;
        return prefixed.getPrefix() == node;
      }
    }
    return false;
  }

  private final List<CorrectionProposal> proposals = Lists.newArrayList();

  private final List<Edit> textEdits = Lists.newArrayList();
  private AnalysisError problem;
  private Source source;
  private CompilationUnit unit;
  private LibraryElement unitLibraryElement;
  private File unitFile;
  private File unitLibraryFile;
  private File unitLibraryFolder;

  private AstNode node;
  private AstNode coveredNode;

  private int selectionOffset;
  private int selectionLength;
  private CorrectionUtils utils;

  private final Map<SourceRange, Edit> positionStopEdits = Maps.newHashMap();

  private final Map<String, List<SourceRange>> linkedPositions = Maps.newHashMap();
  private final Map<String, List<LinkedPositionProposal>> linkedPositionProposals = Maps.newHashMap();
  private SourceRange endRange = null;

  @Override
  public CorrectionProposal[] computeProposals(AssistContext context, AnalysisError problem)
      throws Exception {
    if (context == null) {
      return NO_PROPOSALS;
    }
    if (problem == null) {
      return NO_PROPOSALS;
    }
    this.problem = problem;
    proposals.clear();
    selectionOffset = problem.getOffset();
    selectionLength = problem.getLength();
    source = context.getSource();
    unitFile = getSourceFile(source);
    unit = context.getCompilationUnit();
    // prepare elements
    {
      CompilationUnitElement unitElement = unit.getElement();
      if (unitElement == null) {
        return NO_PROPOSALS;
      }
      unitLibraryElement = unitElement.getLibrary();
      if (unitLibraryElement == null) {
        return NO_PROPOSALS;
      }
      unitLibraryFile = getSourceFile(unitLibraryElement.getSource());
      if (unitLibraryFile == null) {
        return NO_PROPOSALS;
      }
      unitLibraryFolder = unitLibraryFile.getParentFile();
    }
    // prepare CorrectionUtils
    utils = new CorrectionUtils(unit);
    node = utils.findNode(selectionOffset);
    coveredNode = new NodeLocator(selectionOffset, selectionOffset + selectionLength).searchWithin(unit);
    //
    final InstrumentationBuilder instrumentation = Instrumentation.builder(this.getClass());
    try {
      ErrorCode errorCode = problem.getErrorCode();
      if (errorCode == CompileTimeErrorCode.CONST_INITIALIZED_WITH_NON_CONSTANT_VALUE) {
        addFix_replaceWithConstInstanceCreation();
      }
      if (errorCode == CompileTimeErrorCode.NO_DEFAULT_SUPER_CONSTRUCTOR_EXPLICIT) {
        addFix_createConstructorSuperExplicit();
      }
      if (errorCode == CompileTimeErrorCode.NO_DEFAULT_SUPER_CONSTRUCTOR_IMPLICIT) {
        addFix_createConstructorSuperImplicit();
      }
      if (errorCode == CompileTimeErrorCode.UNDEFINED_CONSTRUCTOR_IN_INITIALIZER_DEFAULT) {
        addFix_createConstructorSuperExplicit();
      }
      if (errorCode == CompileTimeErrorCode.URI_DOES_NOT_EXIST) {
        addFix_createPart();
        addFix_addPackageDependency();
      }
      if (errorCode == HintCode.DIVISION_OPTIMIZATION) {
        addFix_useEffectiveIntegerDivision();
      }
      if (errorCode == HintCode.TYPE_CHECK_IS_NOT_NULL) {
        addFix_isNotNull();
      }
      if (errorCode == HintCode.TYPE_CHECK_IS_NULL) {
        addFix_isNull();
      }
      if (errorCode == HintCode.UNNECESSARY_CAST) {
        addFix_removeUnnecessaryCast();
      }
      if (errorCode == HintCode.UNUSED_IMPORT) {
        addFix_removeUnusedImport();
      }
      if (errorCode == ParserErrorCode.EXPECTED_TOKEN) {
        addFix_insertSemicolon();
      }
      if (errorCode == ParserErrorCode.GETTER_WITH_PARAMETERS) {
        addFix_removeParameters_inGetterDeclaration();
      }
      if (errorCode == StaticWarningCode.CONCRETE_CLASS_WITH_ABSTRACT_MEMBER) {
        addFix_makeEnclosingClassAbstract();
      }
      if (errorCode == StaticWarningCode.EXTRA_POSITIONAL_ARGUMENTS) {
        addFix_createConstructor_insteadOfSyntheticDefault();
      }
      if (errorCode == StaticWarningCode.NEW_WITH_UNDEFINED_CONSTRUCTOR) {
        addFix_createConstructor_named();
      }
      if (errorCode == StaticWarningCode.NON_ABSTRACT_CLASS_INHERITS_ABSTRACT_MEMBER_ONE
          || errorCode == StaticWarningCode.NON_ABSTRACT_CLASS_INHERITS_ABSTRACT_MEMBER_TWO
          || errorCode == StaticWarningCode.NON_ABSTRACT_CLASS_INHERITS_ABSTRACT_MEMBER_THREE
          || errorCode == StaticWarningCode.NON_ABSTRACT_CLASS_INHERITS_ABSTRACT_MEMBER_FOUR
          || errorCode == StaticWarningCode.NON_ABSTRACT_CLASS_INHERITS_ABSTRACT_MEMBER_FIVE_PLUS) {
        // make class abstract
        addFix_makeEnclosingClassAbstract();
        // implement methods
        AnalysisErrorWithProperties errorWithProperties = (AnalysisErrorWithProperties) problem;
        Object property = errorWithProperties.getProperty(ErrorProperty.UNIMPLEMENTED_METHODS);
        ExecutableElement[] missingOverrides = (ExecutableElement[]) property;
        addFix_createMissingOverrides(missingOverrides);
        addFix_createNoSuchMethod();
      }
      if (errorCode == StaticWarningCode.UNDEFINED_CLASS) {
        addFix_importLibrary_withType();
        addFix_createClass();
        addFix_undefinedClass_useSimilar();
      }
      if (errorCode == StaticWarningCode.UNDEFINED_CLASS_BOOLEAN) {
        addFix_boolInsteadOfBoolean();
      }
      if (errorCode == StaticWarningCode.UNDEFINED_IDENTIFIER) {
        addFix_createFunction_forFunctionType();
        addFix_importLibrary_withType();
        addFix_importLibrary_withTopLevelVariable();
      }
      if (errorCode == StaticTypeWarningCode.INSTANCE_ACCESS_TO_STATIC_MEMBER) {
        addFix_useStaticAccess_method();
        addFix_useStaticAccess_property();
      }
      if (errorCode == StaticTypeWarningCode.INVOCATION_OF_NON_FUNCTION) {
        addFix_removeParentheses_inGetterInvocation();
      }
      if (errorCode == StaticTypeWarningCode.UNDEFINED_FUNCTION) {
        addFix_importLibrary_withFunction();
        addFix_undefinedFunction_useSimilar();
        addFix_undefinedFunction_create();
      }
      if (errorCode == StaticTypeWarningCode.UNDEFINED_GETTER) {
        addFix_createFunction_forFunctionType();
      }
      if (errorCode == HintCode.UNDEFINED_METHOD
          || errorCode == StaticTypeWarningCode.UNDEFINED_METHOD) {
        addFix_undefinedMethod_useSimilar();
        addFix_undefinedMethod_create();
        addFix_undefinedFunction_create();
      }
      // clean-up
      resetProposalElements();
      // write instrumentation
      instrumentation.metric("QuickFix-Offset", selectionOffset);
      instrumentation.metric("QuickFix-Length", selectionLength);
      instrumentation.metric("QuickFix-ProposalCount", proposals.size());
      instrumentation.data("QuickFix-Source", utils.getText());
      for (int index = 0; index < proposals.size(); index++) {
        instrumentation.data("QuickFix-Proposal-" + index, proposals.get(index).getName());
      }
      // done
      return proposals.toArray(new CorrectionProposal[proposals.size()]);
    } finally {
      instrumentation.log();
    }
  }

  @Override
  public ErrorCode[] getFixableErrorCodes() {
    return FIXABLE_ERROR_CODES;
  }

  @Override
  public boolean hasFix(AnalysisError problem) {
    ErrorCode errorCode = problem.getErrorCode();
//    System.out.println(errorCode.getClass() + " " + errorCode);
    return errorCode == CompileTimeErrorCode.CONST_INITIALIZED_WITH_NON_CONSTANT_VALUE
        || errorCode == CompileTimeErrorCode.NO_DEFAULT_SUPER_CONSTRUCTOR_EXPLICIT
        || errorCode == CompileTimeErrorCode.NO_DEFAULT_SUPER_CONSTRUCTOR_IMPLICIT
        || errorCode == CompileTimeErrorCode.UNDEFINED_CONSTRUCTOR_IN_INITIALIZER_DEFAULT
        || errorCode == CompileTimeErrorCode.URI_DOES_NOT_EXIST
        || errorCode == HintCode.DIVISION_OPTIMIZATION
        || errorCode == HintCode.TYPE_CHECK_IS_NOT_NULL || errorCode == HintCode.TYPE_CHECK_IS_NULL
        || errorCode == HintCode.UNNECESSARY_CAST || errorCode == ParserErrorCode.EXPECTED_TOKEN
        || errorCode == HintCode.UNUSED_IMPORT || errorCode == HintCode.UNDEFINED_METHOD
        || errorCode == ParserErrorCode.GETTER_WITH_PARAMETERS
        || errorCode == StaticWarningCode.CONCRETE_CLASS_WITH_ABSTRACT_MEMBER
        || errorCode == StaticWarningCode.EXTRA_POSITIONAL_ARGUMENTS
        || errorCode == StaticWarningCode.NEW_WITH_UNDEFINED_CONSTRUCTOR
        || errorCode == StaticWarningCode.NON_ABSTRACT_CLASS_INHERITS_ABSTRACT_MEMBER_ONE
        || errorCode == StaticWarningCode.NON_ABSTRACT_CLASS_INHERITS_ABSTRACT_MEMBER_TWO
        || errorCode == StaticWarningCode.NON_ABSTRACT_CLASS_INHERITS_ABSTRACT_MEMBER_THREE
        || errorCode == StaticWarningCode.NON_ABSTRACT_CLASS_INHERITS_ABSTRACT_MEMBER_FOUR
        || errorCode == StaticWarningCode.NON_ABSTRACT_CLASS_INHERITS_ABSTRACT_MEMBER_FIVE_PLUS
        || errorCode == StaticWarningCode.UNDEFINED_CLASS
        || errorCode == StaticWarningCode.UNDEFINED_CLASS_BOOLEAN
        || errorCode == StaticWarningCode.UNDEFINED_IDENTIFIER
        || errorCode == StaticTypeWarningCode.INSTANCE_ACCESS_TO_STATIC_MEMBER
        || errorCode == StaticTypeWarningCode.INVOCATION_OF_NON_FUNCTION
        || errorCode == StaticTypeWarningCode.UNDEFINED_FUNCTION
        || errorCode == StaticTypeWarningCode.UNDEFINED_GETTER
        || errorCode == StaticTypeWarningCode.UNDEFINED_METHOD;
  }

  private void addFix_addPackageDependency() throws Exception {
    if (node instanceof SimpleStringLiteral && node.getParent() instanceof NamespaceDirective) {
      SimpleStringLiteral uriLiteral = (SimpleStringLiteral) node;
      String uriString = uriLiteral.getValue();
      // we need package: import
      if (!uriString.startsWith("package:")) {
        return;
      }
      // prepare package name
      String packageName = StringUtils.removeStart(uriString, "package:");
      packageName = StringUtils.substringBefore(packageName, "/");
      // add proposal
      proposals.add(new AddDependencyCorrectionProposal(
          unitFile,
          packageName,
          CorrectionKind.QF_ADD_PACKAGE_DEPENDENCY,
          packageName));
    }
  }

  private void addFix_boolInsteadOfBoolean() {
    SourceRange range = rangeError(problem);
    addReplaceEdit(range, "bool");
    addUnitCorrectionProposal(CorrectionKind.QF_REPLACE_BOOLEAN_WITH_BOOL);
  }

  private void addFix_createClass() {
    if (mayBeTypeIdentifier(node)) {
      String name = ((SimpleIdentifier) node).getName();
      // prepare environment
      String eol = utils.getEndOfLine();
      CompilationUnitMember enclosingMember = node.getAncestor(CompilationUnitMember.class);
      int offset = enclosingMember.getEnd();
      String prefix = "";
      // prepare source
      SourceBuilder sb = new SourceBuilder(offset);
      {
        sb.append(eol + eol);
        sb.append(prefix);
        // "class"
        sb.append("class ");
        // append name
        {
          sb.startPosition("NAME");
          sb.append(name);
          sb.endPosition();
        }
        // no members
        sb.append(" {");
        sb.append(eol);
        sb.append("}");
      }
      // insert source
      addInsertEdit(offset, sb.toString());
      // add linked positions
      addLinkedPosition("NAME", rangeNode(node));
      addLinkedPositions(sb);
      // add proposal
      addUnitCorrectionProposal(CorrectionKind.QF_CREATE_CLASS, name);
    }
  }

  private void addFix_createConstructor_insteadOfSyntheticDefault() throws Exception {
    TypeName typeName = null;
    ConstructorName constructorName = null;
    InstanceCreationExpression instanceCreation = null;
    if (node instanceof SimpleIdentifier) {
      if (node.getParent() instanceof TypeName) {
        typeName = (TypeName) node.getParent();
        if (typeName.getName() == node && typeName.getParent() instanceof ConstructorName) {
          constructorName = (ConstructorName) typeName.getParent();
          // should be synthetic default constructor
          {
            ConstructorElement constructorElement = constructorName.getStaticElement();
            if (constructorElement == null || !constructorElement.isDefaultConstructor()
                || !constructorElement.isSynthetic()) {
              return;
            }
          }
          // prepare InstanceCreationExpression
          if (constructorName.getParent() instanceof InstanceCreationExpression) {
            instanceCreation = (InstanceCreationExpression) constructorName.getParent();
            if (instanceCreation.getConstructorName() != constructorName) {
              return;
            }
          }
        }
      }
    }
    // do we have enough information?
    if (instanceCreation == null) {
      return;
    }
    // prepare environment
    String eol = utils.getEndOfLine();
    // prepare target
    Type targetType = typeName.getType();
    if (!(targetType instanceof InterfaceType)) {
      return;
    }
    ClassElement targetElement = (ClassElement) targetType.getElement();
    Source targetSource = targetElement.getSource();
    ClassDeclaration targetClass = targetElement.getNode();
    NewConstructorLocation targetLocation = prepareNewConstructorLocation(targetClass, eol);
    // build method source
    SourceBuilder sb = new SourceBuilder(targetLocation.offset);
    {
      String indent = "  ";
      sb.append(targetLocation.prefix);
      sb.append(indent);
      sb.append(targetElement.getName());
      addFix_undefinedMethod_create_parameters(sb, instanceCreation.getArgumentList());
      sb.append(") {" + eol + indent + "}");
      sb.append(targetLocation.suffix);
    }
    // insert source
    addInsertEdit(sb);
    // add linked positions
    addLinkedPositions(sb);
    // add proposal
    addUnitCorrectionProposal(targetSource, CorrectionKind.QF_CREATE_CONSTRUCTOR, constructorName);
  }

  private void addFix_createConstructor_named() throws Exception {
    SimpleIdentifier name = null;
    ConstructorName constructorName = null;
    InstanceCreationExpression instanceCreation = null;
    if (node instanceof SimpleIdentifier) {
      // name
      name = (SimpleIdentifier) node;
      if (name.getParent() instanceof ConstructorName) {
        constructorName = (ConstructorName) name.getParent();
        if (constructorName.getName() == name) {
          // Type.name
          if (constructorName.getParent() instanceof InstanceCreationExpression) {
            instanceCreation = (InstanceCreationExpression) constructorName.getParent();
            // new Type.name()
            if (instanceCreation.getConstructorName() != constructorName) {
              return;
            }
          }
        }
      }
    }
    // do we have enough information?
    if (instanceCreation == null) {
      return;
    }
    // prepare environment
    String eol = utils.getEndOfLine();
    // prepare target interface type
    Type targetType = constructorName.getType().getType();
    if (!(targetType instanceof InterfaceType)) {
      return;
    }
    ClassElement targetElement = (ClassElement) targetType.getElement();
    Source targetSource = targetElement.getSource();
    ClassDeclaration targetClass = targetElement.getNode();
    NewConstructorLocation targetLocation = prepareNewConstructorLocation(targetClass, eol);
    // build method source
    SourceBuilder sb = new SourceBuilder(targetLocation.offset);
    {
      String indent = "  ";
      sb.append(targetLocation.prefix);
      sb.append(indent);
      sb.append(targetElement.getName());
      sb.append(".");
      // append name
      {
        sb.startPosition("NAME");
        sb.append(name.getName());
        sb.endPosition();
      }
      addFix_undefinedMethod_create_parameters(sb, instanceCreation.getArgumentList());
      sb.append(") {" + eol + indent + "}");
      sb.append(targetLocation.suffix);
    }
    // insert source
    addInsertEdit(sb);
    // add linked positions
    if (Objects.equal(targetSource, source)) {
      addLinkedPosition("NAME", sb, rangeNode(name));
    }
    addLinkedPositions(sb);
    // add proposal
    addUnitCorrectionProposal(targetSource, CorrectionKind.QF_CREATE_CONSTRUCTOR, constructorName);
  }

  /**
   * @see StaticWarningCode#NO_DEFAULT_SUPER_CONSTRUCTOR_EXPLICIT
   */
  private void addFix_createConstructorSuperExplicit() {
    ConstructorDeclaration targetConstructor = (ConstructorDeclaration) node.getParent();
    ClassDeclaration targetClassNode = (ClassDeclaration) targetConstructor.getParent();
    ClassElement targetClassElement = targetClassNode.getElement();
    ClassElement superClassElement = targetClassElement.getSupertype().getElement();
    // add proposals for all super constructors
    ConstructorElement[] superConstructors = superClassElement.getConstructors();
    for (ConstructorElement superConstructor : superConstructors) {
      String constructorName = superConstructor.getName();
      // skip private
      if (Identifier.isPrivateName(constructorName)) {
        continue;
      }
      // prepare SourceBuilder
      SourceBuilder sb;
      {
        List<ConstructorInitializer> initializers = targetConstructor.getInitializers();
        if (initializers.isEmpty()) {
          int insertOffset = targetConstructor.getParameters().getEnd();
          sb = new SourceBuilder(insertOffset);
          sb.append(" : ");
        } else {
          ConstructorInitializer lastInitializer = initializers.get(initializers.size() - 1);
          int insertOffset = lastInitializer.getEnd();
          sb = new SourceBuilder(insertOffset);
          sb.append(", ");
        }
      }
      // add super constructor name
      sb.append("super");
      if (!StringUtils.isEmpty(constructorName)) {
        sb.append(".");
        sb.append(constructorName);
      }
      // add arguments
      sb.append("(");
      boolean firstParameter = true;
      for (ParameterElement parameter : superConstructor.getParameters()) {
        // skip non-required parameters
        if (parameter.getParameterKind() != ParameterKind.REQUIRED) {
          break;
        }
        // comma
        if (firstParameter) {
          firstParameter = false;
        } else {
          sb.append(", ");
        }
        // default value
        Type parameterType = parameter.getType();
        sb.startPosition(parameter.getName());
        sb.append(CorrectionUtils.getDefaultValueCode(parameterType));
        sb.endPosition();
      }
      sb.append(")");
      // insert proposal
      addLinkedPositions(sb);
      addInsertEdit(sb);
      // add proposal
      String proposalName = getConstructorProposalName(superConstructor);
      addUnitCorrectionProposal(CorrectionKind.QF_ADD_SUPER_CONSTRUCTOR_INVOCATION, proposalName);
    }
  }

  /**
   * @see StaticWarningCode#NO_DEFAULT_SUPER_CONSTRUCTOR_IMPLICIT
   */
  private void addFix_createConstructorSuperImplicit() {
    ClassDeclaration targetClassNode = (ClassDeclaration) node.getParent();
    ClassElement targetClassElement = targetClassNode.getElement();
    ClassElement superClassElement = targetClassElement.getSupertype().getElement();
    String targetClassName = targetClassElement.getName();
    // add proposals for all super constructors
    ConstructorElement[] superConstructors = superClassElement.getConstructors();
    for (ConstructorElement superConstructor : superConstructors) {
      String constructorName = superConstructor.getName();
      // skip private
      if (Identifier.isPrivateName(constructorName)) {
        continue;
      }
      // prepare parameters and arguments
      StringBuilder parametersBuffer = new StringBuilder();
      StringBuilder argumentsBuffer = new StringBuilder();
      boolean firstParameter = true;
      for (ParameterElement parameter : superConstructor.getParameters()) {
        // skip non-required parameters
        if (parameter.getParameterKind() != ParameterKind.REQUIRED) {
          break;
        }
        // comma
        if (firstParameter) {
          firstParameter = false;
        } else {
          parametersBuffer.append(", ");
          argumentsBuffer.append(", ");
        }
        // name
        String parameterName = parameter.getDisplayName();
        if (parameterName.length() > 1 && parameterName.startsWith("_")) {
          parameterName = parameterName.substring(1);
        }
        // parameter & argument
        appendParameterSource(parametersBuffer, parameter.getType(), parameterName);
        argumentsBuffer.append(parameterName);
      }
      // add proposal
      String eol = utils.getEndOfLine();
      NewConstructorLocation targetLocation = prepareNewConstructorLocation(targetClassNode, eol);
      SourceBuilder sb = new SourceBuilder(targetLocation.offset);
      {
        String indent = utils.getIndent(1);
        sb.append(targetLocation.prefix);
        sb.append(indent);
        sb.append(targetClassName);
        if (!constructorName.isEmpty()) {
          sb.startPosition("NAME");
          sb.append(".");
          sb.append(constructorName);
          sb.endPosition();
        }
        sb.append("(");
        sb.append(parametersBuffer);
        sb.append(") : super");
        if (!constructorName.isEmpty()) {
          sb.append(".");
          sb.append(constructorName);
        }
        sb.append("(");
        sb.append(argumentsBuffer);
        sb.append(");");
        sb.append(targetLocation.suffix);
      }
      addInsertEdit(sb);
      // add proposal
      String proposalName = getConstructorProposalName(superConstructor);
      addUnitCorrectionProposal(CorrectionKind.QF_CREATE_CONSTRUCTOR_SUPER, proposalName);
    }
  }

  private void addFix_createFunction_forFunctionType() throws Exception {
    if (node instanceof SimpleIdentifier) {
      SimpleIdentifier nameNode = (SimpleIdentifier) node;
      // prepare argument expression (to get parameter)
      ClassElement targetElement;
      Expression argument;
      {
        Expression target = CorrectionUtils.getQualifiedPropertyTarget(node);
        if (target != null) {
          Type targetType = target.getBestType();
          if (targetType != null && targetType.getElement() instanceof ClassElement) {
            targetElement = (ClassElement) targetType.getElement();
            argument = (Expression) target.getParent();
          } else {
            return;
          }
        } else {
          ClassDeclaration enclosingClass = node.getAncestor(ClassDeclaration.class);
          targetElement = enclosingClass != null ? enclosingClass.getElement() : null;
          argument = nameNode;
        }
      }
      // should be argument of some invocation
      ParameterElement parameterElement = argument.getBestParameterElement();
      if (parameterElement == null) {
        return;
      }
      // should be parameter of function type
      Type parameterType = parameterElement.getType();
      if (!(parameterType instanceof FunctionType)) {
        return;
      }
      FunctionType functionType = (FunctionType) parameterType;
      // add proposal
      if (targetElement != null) {
        addProposal_createFunction_method(targetElement, functionType);
      } else {
        addProposal_createFunction_function(functionType);
      }
    }
  }

  private void addFix_createMissingOverrides(ExecutableElement[] missingOverrides) throws Exception {
    // sort by name
    Arrays.sort(missingOverrides, new Comparator<Element>() {
      @Override
      public int compare(Element firstElement, Element secondElement) {
        return ObjectUtils.compare(firstElement.getDisplayName(), secondElement.getDisplayName());
      }
    });
    // add elements
    ClassDeclaration targetClass = (ClassDeclaration) node.getParent();
    boolean isFirst = true;
    for (ExecutableElement missingOverride : missingOverrides) {
      addFix_createMissingOverrides_single(targetClass, missingOverride, isFirst);
      isFirst = false;
    }
    // add proposal
    addUnitCorrectionProposal(CorrectionKind.QF_CREATE_MISSING_OVERRIDES, missingOverrides.length);
  }

  private void addFix_createMissingOverrides_single(ClassDeclaration targetClass,
      ExecutableElement missingOverride, boolean isFirst) throws Exception {
    // prepare environment
    String eol = utils.getEndOfLine();
    String prefix = utils.getIndent(1);
    String prefix2 = utils.getIndent(2);
    int insertOffset = targetClass.getEnd() - 1;
    // prepare source
    StringBuilder sb = new StringBuilder();
    // may be empty line
    if (!isFirst || !targetClass.getMembers().isEmpty()) {
      sb.append(eol);
    }
    // may be property
    ElementKind elementKind = missingOverride.getKind();
    boolean isGetter = elementKind == ElementKind.GETTER;
    boolean isSetter = elementKind == ElementKind.SETTER;
    boolean isMethod = elementKind == ElementKind.METHOD;
    boolean isOperator = isMethod && ((MethodElement) missingOverride).isOperator();
    sb.append(prefix);
    if (isGetter) {
      sb.append("// TODO: implement " + missingOverride.getDisplayName());
      sb.append(eol);
      sb.append(prefix);
    }
    // @override
    {
      sb.append("@override");
      sb.append(eol);
      sb.append(prefix);
    }
    // return type
    appendType(sb, missingOverride.getType().getReturnType());
    if (isGetter) {
      sb.append("get ");
    } else if (isSetter) {
      sb.append("set ");
    } else if (isOperator) {
      sb.append("operator ");
    }
    // name
    sb.append(missingOverride.getDisplayName());
    // parameters + body
    if (isGetter) {
      sb.append(" => null;");
    } else if (isMethod || isSetter) {
      ParameterElement[] parameters = missingOverride.getParameters();
      appendParameters(sb, parameters);
      sb.append(" {");
      // TO-DO
      sb.append(eol);
      sb.append(prefix2);
      if (isMethod) {
        sb.append("// TODO: implement " + missingOverride.getDisplayName());
      } else {
        sb.append("// TODO: implement " + missingOverride.getDisplayName());
      }
      sb.append(eol);
      // close method
      sb.append(prefix);
      sb.append("}");
    }
    sb.append(eol);
    // done
    addInsertEdit(insertOffset, sb.toString());
    // maybe set end range
    if (endRange == null) {
      endRange = rangeStartLength(insertOffset, 0);
    }
  }

  private void addFix_createNoSuchMethod() throws Exception {
    ClassDeclaration targetClass = (ClassDeclaration) node.getParent();
    // prepare environment
    String eol = utils.getEndOfLine();
    String prefix = utils.getIndent(1);
    int insertOffset = targetClass.getEnd() - 1;
    // prepare source
    SourceBuilder sb = new SourceBuilder(insertOffset);
    {
      // insert empty line before existing member
      if (!targetClass.getMembers().isEmpty()) {
        sb.append(eol);
      }
      // append method
      sb.append(prefix);
      sb.append("noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);");
      sb.append(eol);
    }
    // done
    addInsertEdit(sb);
    endRange = rangeStartLength(insertOffset, 0);
    // add proposal
    addUnitCorrectionProposal(CorrectionKind.QF_CREATE_NO_SUCH_METHOD);
  }

  private void addFix_createPart() throws Exception {
    if (node instanceof SimpleStringLiteral && node.getParent() instanceof PartDirective) {
      SimpleStringLiteral uriLiteral = (SimpleStringLiteral) node;
      String uriString = uriLiteral.getValue();
      // prepare referenced File
      File newFile;
      {
        URI uri = URI.create(uriString);
        if (uri.isAbsolute()) {
          return;
        }
        newFile = new File(unitLibraryFolder, uriString);
      }
      if (!newFile.exists()) {
        // prepare new source
        String source;
        {
          String eol = utils.getEndOfLine();
          String libraryName = unitLibraryElement.getDisplayName();
          source = "part of " + libraryName + ";" + eol + eol;
        }
        // add proposal
        proposals.add(new CreateFileCorrectionProposal(
            newFile,
            source,
            CorrectionKind.QF_CREATE_PART,
            uriString));
      }
    }
  }

  private void addFix_importLibrary(CorrectionKind kind, String importPath) throws Exception {
    CompilationUnitElement libraryUnitElement = unitLibraryElement.getDefiningCompilationUnit();
    CompilationUnit libraryUnit = libraryUnitElement.getNode();
    // prepare new import location
    int offset = 0;
    String prefix;
    String suffix;
    {
      String eol = utils.getEndOfLine();
      // if no directives
      prefix = "";
      suffix = eol;
      CorrectionUtils libraryUtils = new CorrectionUtils(libraryUnit);
      // after last directive in library
      for (Directive directive : libraryUnit.getDirectives()) {
        if (directive instanceof LibraryDirective || directive instanceof ImportDirective) {
          offset = directive.getEnd();
          prefix = eol;
          suffix = "";
        }
      }
      // if still beginning of file, skip shebang and line comments
      if (offset == 0) {
        InsertDesc desc = libraryUtils.getInsertDescTop();
        offset = desc.offset;
        prefix = desc.prefix;
        suffix = desc.suffix + eol;
      }
    }
    // insert new import
    String importSource = prefix + "import '" + importPath + "';" + suffix;
    addInsertEdit(offset, importSource);
    // add proposal
    addUnitCorrectionProposal(libraryUnitElement.getSource(), kind, importPath);
  }

  private void addFix_importLibrary_withElement(String name, ElementKind kind) throws Exception {
    // ignore if private
    if (name.startsWith("_")) {
      return;
    }
    // may be there is an existing import, but it is with prefix and we don't use this prefix
    for (ImportElement imp : unitLibraryElement.getImports()) {
      // prepare element
      LibraryElement libraryElement = imp.getImportedLibrary();
      Element element = CorrectionUtils.getExportedElement(libraryElement, name);
      if (element == null) {
        continue;
      }
      if (element instanceof PropertyAccessorElement) {
        element = ((PropertyAccessorElement) element).getVariable();
      }
      if (element.getKind() != kind) {
        continue;
      }
      // may be apply prefix
      PrefixElement prefix = imp.getPrefix();
      if (prefix != null) {
        SourceRange range = rangeStartLength(node, 0);
        addReplaceEdit(range, prefix.getDisplayName() + ".");
        addUnitCorrectionProposal(
            CorrectionKind.QF_IMPORT_LIBRARY_PREFIX,
            libraryElement.getDisplayName(),
            prefix.getDisplayName());
        continue;
      }
      // may be update "show" directive
      NamespaceCombinator[] combinators = imp.getCombinators();
      if (combinators.length == 1 && combinators[0] instanceof ShowElementCombinator) {
        ShowElementCombinator showCombinator = (ShowElementCombinator) combinators[0];
        // prepare new set of names to show
        Set<String> showNames = Sets.newTreeSet();
        Collections.addAll(showNames, showCombinator.getShownNames());
        showNames.add(name);
        // prepare library name - unit name or 'dart:name' for SDK library
        String libraryName = libraryElement.getDefiningCompilationUnit().getDisplayName();
        if (libraryElement.isInSdk()) {
          libraryName = imp.getUri();
        }
        // update library
        String newShowCode = "show " + StringUtils.join(showNames, ", ");
        addReplaceEdit(rangeShowCombinator(showCombinator), newShowCode);
        addUnitCorrectionProposal(
            unitLibraryElement.getSource(),
            CorrectionKind.QF_IMPORT_LIBRARY_SHOW,
            libraryName);
        // we support only one import without prefix
        return;
      }
    }
    // check SDK libraries
    AnalysisContext context = unitLibraryElement.getContext();
    {
      DartSdk sdk = context.getSourceFactory().getDartSdk();
      SdkLibrary[] sdkLibraries = sdk.getSdkLibraries();
      for (SdkLibrary sdkLibrary : sdkLibraries) {
        SourceFactory sdkSourceFactory = context.getSourceFactory();
        String libraryUri = sdkLibrary.getShortName();
        Source librarySource = sdkSourceFactory.resolveUri(null, libraryUri);
        // prepare LibraryElement
        LibraryElement libraryElement = context.getLibraryElement(librarySource);
        if (libraryElement == null) {
          continue;
        }
        // prepare exported Element
        Element element = CorrectionUtils.getExportedElement(libraryElement, name);
        if (element == null) {
          continue;
        }
        if (element instanceof PropertyAccessorElement) {
          element = ((PropertyAccessorElement) element).getVariable();
        }
        if (element.getKind() != kind) {
          continue;
        }
        // add import
        addFix_importLibrary(CorrectionKind.QF_IMPORT_LIBRARY_SDK, libraryUri);
      }
    }
    // check project libraries
    {
      Source[] librarySources = context.getLibrarySources();
      for (Source librarySource : librarySources) {
        // we don't need SDK libraries here
        if (librarySource.isInSystemLibrary()) {
          continue;
        }
        // prepare LibraryElement
        LibraryElement libraryElement = context.getLibraryElement(librarySource);
        if (libraryElement == null) {
          continue;
        }
        // prepare exported Element
        Element element = CorrectionUtils.getExportedElement(libraryElement, name);
        if (element == null) {
          continue;
        }
        if (element.getKind() != kind) {
          continue;
        }
        // prepare "library" file
        File libraryFile = getSourceFile(librarySource);
        if (libraryFile == null) {
          continue;
        }
        // may be "package:" URI
        {
          URI libraryPackageUri = findPackageUri(context, libraryFile);
          if (libraryPackageUri != null) {
            addFix_importLibrary(
                CorrectionKind.QF_IMPORT_LIBRARY_PROJECT,
                libraryPackageUri.toString());
            continue;
          }
        }
        // relative URI
        String relative = URIUtils.computeRelativePath(
            unitLibraryFolder.getAbsolutePath(),
            libraryFile.getAbsolutePath());
        addFix_importLibrary(CorrectionKind.QF_IMPORT_LIBRARY_PROJECT, relative);
      }
    }
  }

  private void addFix_importLibrary_withFunction() throws Exception {
    if (node instanceof SimpleIdentifier && node.getParent() instanceof MethodInvocation) {
      MethodInvocation invocation = (MethodInvocation) node.getParent();
      if (invocation.getRealTarget() == null && invocation.getMethodName() == node) {
        String name = ((SimpleIdentifier) node).getName();
        addFix_importLibrary_withElement(name, ElementKind.FUNCTION);
      }
    }
  }

  private void addFix_importLibrary_withTopLevelVariable() throws Exception {
    if (node instanceof SimpleIdentifier) {
      String name = ((SimpleIdentifier) node).getName();
      addFix_importLibrary_withElement(name, ElementKind.TOP_LEVEL_VARIABLE);
    }
  }

  private void addFix_importLibrary_withType() throws Exception {
    if (mayBeTypeIdentifier(node)) {
      String typeName = ((SimpleIdentifier) node).getName();
      addFix_importLibrary_withElement(typeName, ElementKind.CLASS);
    }
  }

  private void addFix_insertSemicolon() {
    if (problem.getMessage().contains("';'")) {
      int insertOffset = problem.getOffset() + problem.getLength();
      addInsertEdit(insertOffset, ";");
      addUnitCorrectionProposal(CorrectionKind.QF_INSERT_SEMICOLON);
    }
  }

  private void addFix_isNotNull() throws Exception {
    if (coveredNode instanceof IsExpression) {
      IsExpression isExpression = (IsExpression) coveredNode;
      addReplaceEdit(rangeEndEnd(isExpression.getExpression(), isExpression), " != null");
      addUnitCorrectionProposal(CorrectionKind.QF_USE_NOT_EQ_NULL);
    }
  }

  private void addFix_isNull() throws Exception {
    if (coveredNode instanceof IsExpression) {
      IsExpression isExpression = (IsExpression) coveredNode;
      addReplaceEdit(rangeEndEnd(isExpression.getExpression(), isExpression), " == null");
      addUnitCorrectionProposal(CorrectionKind.QF_USE_EQ_EQ_NULL);
    }
  }

  private void addFix_makeEnclosingClassAbstract() {
    ClassDeclaration enclosingClass = node.getAncestor(ClassDeclaration.class);
    String className = enclosingClass.getName().getName();
    addInsertEdit(enclosingClass.getClassKeyword().getOffset(), "abstract ");
    addUnitCorrectionProposal(CorrectionKind.QF_MAKE_CLASS_ABSTRACT, className);
  }

  private void addFix_removeParameters_inGetterDeclaration() throws Exception {
    if (node instanceof SimpleIdentifier && node.getParent() instanceof MethodDeclaration) {
      MethodDeclaration method = (MethodDeclaration) node.getParent();
      FunctionBody body = method.getBody();
      if (method.getName() == node && body != null) {
        addReplaceEdit(rangeEndStart(node, body), " ");
        addUnitCorrectionProposal(CorrectionKind.QF_REMOVE_PARAMETERS_IN_GETTER_DECLARATION);
      }
    }
  }

  private void addFix_removeParentheses_inGetterInvocation() throws Exception {
    if (node instanceof SimpleIdentifier && node.getParent() instanceof MethodInvocation) {
      MethodInvocation invocation = (MethodInvocation) node.getParent();
      if (invocation.getMethodName() == node && invocation.getTarget() != null) {
        addRemoveEdit(rangeEndEnd(node, invocation));
        addUnitCorrectionProposal(CorrectionKind.QF_REMOVE_PARENTHESIS_IN_GETTER_INVOCATION);
      }
    }
  }

  private void addFix_removeUnnecessaryCast() {
    if (!(coveredNode instanceof AsExpression)) {
      return;
    }
    AsExpression asExpression = (AsExpression) coveredNode;
    Expression expression = asExpression.getExpression();
    int expressionPrecedence = CorrectionUtils.getExpressionPrecedence(expression);
    // remove 'as T' from 'e as T'
    addRemoveEdit(rangeEndEnd(expression, asExpression));
    removeEnclosingParentheses(asExpression, expressionPrecedence);
    // done
    addUnitCorrectionProposal(CorrectionKind.QF_REMOVE_UNNECASSARY_CAST);
  }

  private void addFix_removeUnusedImport() {
    // prepare ImportDirective
    ImportDirective importDirective = node.getAncestor(ImportDirective.class);
    if (importDirective == null) {
      return;
    }
    // remove the whole line with import
    addRemoveEdit(utils.getLinesRange(rangeNode(importDirective)));
    // done
    addUnitCorrectionProposal(CorrectionKind.QF_REMOVE_UNUSED_IMPORT);
  }

  private void addFix_replaceWithConstInstanceCreation() throws Exception {
    if (coveredNode instanceof InstanceCreationExpression) {
      InstanceCreationExpression instanceCreation = (InstanceCreationExpression) coveredNode;
      addReplaceEdit(rangeToken(instanceCreation.getKeyword()), "const");
      addUnitCorrectionProposal(CorrectionKind.QF_USE_CONST);
    }
  }

  private void addFix_undefinedClass_useSimilar() {
    if (mayBeTypeIdentifier(node)) {
      String name = ((SimpleIdentifier) node).getName();
      final ClosestElementFinder finder = new ClosestElementFinder(name, new Predicate<Element>() {
        @Override
        public boolean apply(Element element) {
          return element instanceof ClassElement;
        }
      });
      // find closest element
      {
        // elements of this library
        unitLibraryElement.accept(new RecursiveElementVisitor<Void>() {
          @Override
          public Void visitClassElement(ClassElement element) {
            finder.update(element);
            return null;
          }
        });
        // elements from imports
        for (ImportElement importElement : unitLibraryElement.getImports()) {
          if (importElement.getPrefix() == null) {
            Map<String, Element> namespace = CorrectionUtils.getImportNamespace(importElement);
            finder.update(namespace.values());
          }
        }
      }
      // if we have close enough element, suggest to use it
      if (finder != null && finder.distance < 5) {
        String closestName = finder.element.getName();
        addReplaceEdit(rangeNode(node), closestName);
        // add proposal
        if (closestName != null) {
          addUnitCorrectionProposal(CorrectionKind.QF_CHANGE_TO, closestName);
        }
      }
    }
  }

  private void addFix_undefinedFunction_create() throws Exception {
    // should be the name of the invocation
    if (node instanceof SimpleIdentifier && node.getParent() instanceof MethodInvocation) {
    } else {
      return;
    }
    String name = ((SimpleIdentifier) node).getName();
    MethodInvocation invocation = (MethodInvocation) node.getParent();
    // function invocation has no target
    Expression target = invocation.getRealTarget();
    if (target != null) {
      return;
    }
    // prepare environment
    String eol = utils.getEndOfLine();
    int insertOffset;
    String sourcePrefix;
    AstNode enclosingMember = node.getAncestor(CompilationUnitMember.class);
    insertOffset = enclosingMember.getEnd();
    sourcePrefix = eol + eol;
    // build method source
    SourceBuilder sb = new SourceBuilder(insertOffset);
    {
      sb.append(sourcePrefix);
      // may be return type
      {
        Type type = addFix_undefinedMethod_create_getReturnType(invocation);
        if (type != null) {
          String typeSource = utils.getTypeSource(type);
          if (!typeSource.equals("dynamic")) {
            sb.startPosition("RETURN_TYPE");
            sb.append(typeSource);
            sb.endPosition();
            sb.append(" ");
          }
        }
      }
      // append name
      {
        sb.startPosition("NAME");
        sb.append(name);
        sb.endPosition();
      }
      addFix_undefinedMethod_create_parameters(sb, invocation.getArgumentList());
      sb.append(") {" + eol + "}");
    }
    // insert source
    addInsertEdit(insertOffset, sb.toString());
    // add linked positions
    addLinkedPosition("NAME", sb, rangeNode(node));
    addLinkedPositions(sb);
    // add proposal
    addUnitCorrectionProposal(CorrectionKind.QF_CREATE_FUNCTION, name);
  }

  private void addFix_undefinedFunction_useSimilar() throws Exception {
    if (node instanceof SimpleIdentifier) {
      String name = ((SimpleIdentifier) node).getName();
      final ClosestElementFinder finder = new ClosestElementFinder(name, new Predicate<Element>() {
        @Override
        public boolean apply(Element element) {
          return element instanceof FunctionElement;
        }
      });
      // this library
      unitLibraryElement.accept(new RecursiveElementVisitor<Void>() {
        @Override
        public Void visitFunctionElement(FunctionElement element) {
          finder.update(element);
          return null;
        }
      });
      // imports
      for (ImportElement importElement : unitLibraryElement.getImports()) {
        if (importElement.getPrefix() == null) {
          Map<String, Element> namespace = CorrectionUtils.getImportNamespace(importElement);
          finder.update(namespace.values());
        }
      }
      // if we have close enough element, suggest to use it
      String closestName = null;
      if (finder != null && finder.distance < 5) {
        closestName = finder.element.getName();
        addReplaceEdit(rangeNode(node), closestName);
        addUnitCorrectionProposal(CorrectionKind.QF_CHANGE_TO, closestName);
      }
    }
  }

  private void addFix_undefinedMethod_create() throws Exception {
    if (node instanceof SimpleIdentifier && node.getParent() instanceof MethodInvocation) {
      String name = ((SimpleIdentifier) node).getName();
      MethodInvocation invocation = (MethodInvocation) node.getParent();
      // prepare environment
      String eol = utils.getEndOfLine();
      Source targetSource;
      String prefix;
      int insertOffset;
      String sourcePrefix;
      String sourceSuffix;
      boolean staticModifier = false;
      Expression target = invocation.getRealTarget();
      if (target == null) {
        targetSource = source;
        ClassMember enclosingMember = node.getAncestor(ClassMember.class);
        staticModifier = inStaticMemberContext(enclosingMember);
        prefix = utils.getNodePrefix(enclosingMember);
        insertOffset = enclosingMember.getEnd();
        sourcePrefix = eol + prefix + eol;
        sourceSuffix = "";
      } else {
        // prepare target interface type
        Type targetType = target.getBestType();
        if (!(targetType instanceof InterfaceType)) {
          return;
        }
        ClassElement targetElement = (ClassElement) targetType.getElement();
        targetSource = targetElement.getSource();
        // may be static
        if (target instanceof Identifier) {
          staticModifier = ((Identifier) target).getBestElement().getKind() == ElementKind.CLASS;
        }
        // prepare insert offset
        ClassDeclaration targetClass = targetElement.getNode();
        prefix = "  ";
        insertOffset = targetClass.getEnd() - 1;
        if (targetClass.getMembers().isEmpty()) {
          sourcePrefix = "";
        } else {
          sourcePrefix = prefix + eol;
        }
        sourceSuffix = eol;
      }
      // build method source
      SourceBuilder sb = new SourceBuilder(insertOffset);
      {
        sb.append(sourcePrefix);
        sb.append(prefix);
        // may be "static"
        if (staticModifier) {
          sb.append("static ");
        }
        // may be return type
        {
          Type type = addFix_undefinedMethod_create_getReturnType(invocation);
          if (type != null) {
            String typeSource = utils.getTypeSource(type);
            if (!typeSource.equals("dynamic")) {
              sb.startPosition("RETURN_TYPE");
              sb.append(typeSource);
              sb.endPosition();
              sb.append(" ");
            }
          }
        }
        // append name
        {
          sb.startPosition("NAME");
          sb.append(name);
          sb.endPosition();
        }
        addFix_undefinedMethod_create_parameters(sb, invocation.getArgumentList());
        sb.append(") {" + eol + prefix + "}");
        sb.append(sourceSuffix);
      }
      // insert source
      addInsertEdit(insertOffset, sb.toString());
      // add linked positions
      if (Objects.equal(targetSource, source)) {
        addLinkedPosition("NAME", sb, rangeNode(node));
      }
      addLinkedPositions(sb);
      // add proposal
      addUnitCorrectionProposal(targetSource, CorrectionKind.QF_CREATE_METHOD, name);
    }
  }

  /**
   * @return the possible return {@link Type}, may be <code>null</code> if can not be identified.
   */
  private Type addFix_undefinedMethod_create_getReturnType(MethodInvocation invocation) {
    AstNode parent = invocation.getParent();
    // myFunction();
    if (parent instanceof ExpressionStatement) {
      return VoidTypeImpl.getInstance();
    }
    // return myFunction();
    if (parent instanceof ReturnStatement) {
      ExecutableElement executable = CorrectionUtils.getEnclosingExecutableElement(invocation);
      return executable != null ? executable.getReturnType() : null;
    }
    // int v = myFunction();
    if (parent instanceof VariableDeclaration) {
      VariableDeclaration variableDeclaration = (VariableDeclaration) parent;
      if (variableDeclaration.getInitializer() == invocation) {
        VariableElement variableElement = variableDeclaration.getElement();
        if (variableElement != null) {
          return variableElement.getType();
        }
      }
    }
    // v = myFunction();
    if (parent instanceof AssignmentExpression) {
      AssignmentExpression assignment = (AssignmentExpression) parent;
      if (assignment.getRightHandSide() == invocation) {
        if (assignment.getOperator().getType() == TokenType.EQ) {
          // v = myFunction();
          Expression lhs = assignment.getLeftHandSide();
          if (lhs != null) {
            return lhs.getBestType();
          }
        } else {
          // v += myFunction();
          MethodElement method = assignment.getBestElement();
          if (method != null) {
            ParameterElement[] parameters = method.getParameters();
            if (parameters.length == 1) {
              return parameters[0].getType();
            }
          }
        }
      }
    }
    // v + myFunction();
    if (parent instanceof BinaryExpression) {
      BinaryExpression binary = (BinaryExpression) parent;
      MethodElement method = binary.getBestElement();
      if (method != null) {
        if (binary.getRightOperand() == invocation) {
          ParameterElement[] parameters = method.getParameters();
          return parameters.length == 1 ? parameters[0].getType() : null;
        }
      }
    }
    // foo( myFunction() );
    if (parent instanceof ArgumentList) {
      ParameterElement parameter = invocation.getBestParameterElement();
      return parameter != null ? parameter.getType() : null;
    }
    // bool
    {
      // assert( myFunction() );
      if (parent instanceof AssertStatement) {
        AssertStatement statement = (AssertStatement) parent;
        if (statement.getCondition() == invocation) {
          return getCoreTypeBool();
        }
      }
      // if ( myFunction() ) {}
      if (parent instanceof IfStatement) {
        IfStatement statement = (IfStatement) parent;
        if (statement.getCondition() == invocation) {
          return getCoreTypeBool();
        }
      }
      // while ( myFunction() ) {}
      if (parent instanceof WhileStatement) {
        WhileStatement statement = (WhileStatement) parent;
        if (statement.getCondition() == invocation) {
          return getCoreTypeBool();
        }
      }
      // do {} while ( myFunction() );
      if (parent instanceof DoStatement) {
        DoStatement statement = (DoStatement) parent;
        if (statement.getCondition() == invocation) {
          return getCoreTypeBool();
        }
      }
      // !myFunction()
      if (parent instanceof PrefixExpression) {
        PrefixExpression prefixExpression = (PrefixExpression) parent;
        if (prefixExpression.getOperator().getType() == TokenType.BANG) {
          return getCoreTypeBool();
        }
      }
      // binary expression '&&' or '||'
      if (parent instanceof BinaryExpression) {
        BinaryExpression binaryExpression = (BinaryExpression) parent;
        TokenType operatorType = binaryExpression.getOperator().getType();
        if (operatorType == TokenType.AMPERSAND_AMPERSAND || operatorType == TokenType.BAR_BAR) {
          return getCoreTypeBool();
        }
      }
    }
    // we don't know
    return null;
  }

  private void addFix_undefinedMethod_create_parameters(SourceBuilder sb, ArgumentList argumentList) {
    // append parameters
    sb.append("(");
    Set<String> excluded = Sets.newHashSet();
    List<Expression> arguments = argumentList.getArguments();
    for (int i = 0; i < arguments.size(); i++) {
      Expression argument = arguments.get(i);
      // append separator
      if (i != 0) {
        sb.append(", ");
      }
      // append type name
      Type type = argument.getBestType();
      String typeSource = utils.getTypeSource(type);
      {
        sb.startPosition("TYPE" + i);
        sb.append(typeSource);
        addSuperTypeProposals(sb, Sets.<Type> newHashSet(), type);
        sb.endPosition();
      }
      sb.append(" ");
      // append parameter name
      {
        String[] suggestions = getArgumentNameSuggestions(excluded, type, argument, i);
        String favorite = suggestions[0];
        excluded.add(favorite);
        sb.startPosition("ARG" + i);
        sb.append(favorite);
        sb.setProposals(suggestions);
        sb.endPosition();
      }
    }
  }

  private void addFix_undefinedMethod_useSimilar() throws Exception {
    if (node instanceof SimpleIdentifier && node.getParent() instanceof MethodInvocation) {
      MethodInvocation invocation = (MethodInvocation) node.getParent();
      String name = ((SimpleIdentifier) node).getName();
      ClosestElementFinder finder = new ClosestElementFinder(name, new Predicate<Element>() {
        @Override
        public boolean apply(Element element) {
          if (element instanceof MethodElement) {
            MethodElement methodElement = (MethodElement) element;
            return !methodElement.isOperator();
          }
          return false;
        }
      });
      // unqualified invocation
      Expression target = invocation.getRealTarget();
      if (target == null) {
        ClassDeclaration clazz = invocation.getAncestor(ClassDeclaration.class);
        if (clazz != null) {
          ClassElement classElement = clazz.getElement();
          updateFinderWithClassMembers(finder, classElement);
        }
      } else {
        Type type = target.getBestType();
        if (type instanceof InterfaceType) {
          ClassElement classElement = ((InterfaceType) type).getElement();
          updateFinderWithClassMembers(finder, classElement);
        }
      }
      // if we have close enough element, suggest to use it
      String closestName = null;
      if (finder != null && finder.distance < 5) {
        closestName = finder.element.getName();
        addReplaceEdit(rangeNode(node), closestName);
        addUnitCorrectionProposal(CorrectionKind.QF_CHANGE_TO, closestName);
      }
    }
  }

  private void addFix_useEffectiveIntegerDivision() throws Exception {
    for (AstNode n = node; n != null; n = n.getParent()) {
      if (n instanceof MethodInvocation && n.getOffset() == selectionOffset
          && n.getLength() == selectionLength) {
        MethodInvocation invocation = (MethodInvocation) n;
        Expression target = invocation.getTarget();
        while (target instanceof ParenthesizedExpression) {
          target = ((ParenthesizedExpression) target).getExpression();
        }
        // replace "/" with "~/"
        BinaryExpression binary = (BinaryExpression) target;
        addReplaceEdit(rangeToken(binary.getOperator()), "~/");
        // remove everything before and after
        addRemoveEdit(rangeStartStart(invocation, binary.getLeftOperand()));
        addRemoveEdit(rangeEndEnd(binary.getRightOperand(), invocation));
        // add proposal
        addUnitCorrectionProposal(CorrectionKind.QF_USE_EFFECTIVE_INTEGER_DIVISION);
        // done
        break;
      }
    }
  }

  private void addFix_useStaticAccess_method() throws Exception {
    if (node instanceof SimpleIdentifier && node.getParent() instanceof MethodInvocation) {
      MethodInvocation invocation = (MethodInvocation) node.getParent();
      if (invocation.getMethodName() == node) {
        Expression target = invocation.getTarget();
        String targetType = utils.getTypeSource(target);
        // replace "target" with class name
        SourceRange range = SourceRangeFactory.rangeNode(target);
        addReplaceEdit(range, targetType);
        // add proposal
        addUnitCorrectionProposal(CorrectionKind.QF_CHANGE_TO_STATIC_ACCESS, targetType);
      }
    }
  }

  private void addFix_useStaticAccess_property() throws Exception {
    if (node instanceof SimpleIdentifier) {
      if (node.getParent() instanceof PrefixedIdentifier) {
        PrefixedIdentifier prefixed = (PrefixedIdentifier) node.getParent();
        if (prefixed.getIdentifier() == node) {
          Expression target = prefixed.getPrefix();
          String targetType = utils.getTypeSource(target);
          // replace "target" with class name
          SourceRange range = SourceRangeFactory.rangeNode(target);
          addReplaceEdit(range, targetType);
          // add proposal
          addUnitCorrectionProposal(CorrectionKind.QF_CHANGE_TO_STATIC_ACCESS, targetType);
        }
      }
    }
  }

  private void addInsertEdit(int offset, String text) {
    textEdits.add(createInsertEdit(offset, text));
  }

  private void addInsertEdit(SourceBuilder builder) {
    addInsertEdit(builder.getOffset(), builder.toString());
  }

  /**
   * Adds single linked position to the group. If {@link SourceBuilder} will be inserted before
   * "position", translate it.
   */
  private void addLinkedPosition(String group, SourceBuilder sb, SourceRange position) {
    if (sb.getOffset() < position.getOffset()) {
      int delta = sb.length();
      position = position.getTranslated(delta);
    }
    addLinkedPosition(group, position);
  }

  /**
   * Adds single linked position to the group.
   */
  private void addLinkedPosition(String group, SourceRange position) {
    List<SourceRange> positions = linkedPositions.get(group);
    if (positions == null) {
      positions = Lists.newArrayList();
      linkedPositions.put(group, positions);
    }
    positions.add(position);
  }

  private void addLinkedPositionProposal(String group, LinkedPositionProposal proposal) {
    List<LinkedPositionProposal> nodeProposals = linkedPositionProposals.get(group);
    if (nodeProposals == null) {
      nodeProposals = Lists.newArrayList();
      linkedPositionProposals.put(group, nodeProposals);
    }
    nodeProposals.add(proposal);
  }

  /**
   * Adds positions from the given {@link SourceBuilder} to the {@link #linkedPositions}.
   */
  private void addLinkedPositions(SourceBuilder builder) {
    // positions
    for (Entry<String, List<SourceRange>> linkedEntry : builder.getLinkedPositions().entrySet()) {
      String group = linkedEntry.getKey();
      for (SourceRange position : linkedEntry.getValue()) {
        addLinkedPosition(group, position);
      }
    }
    // proposals for positions
    for (Entry<String, List<LinkedPositionProposal>> entry : builder.getLinkedProposals().entrySet()) {
      String group = entry.getKey();
      for (LinkedPositionProposal proposal : entry.getValue()) {
        addLinkedPositionProposal(group, proposal);
      }
    }
  }

  /**
   * Prepares proposal for creating function corresponding to the given {@link FunctionType}.
   */
  private void addProposal_createFunction(FunctionType functionType, String name,
      Source targetSource, int insertOffset, boolean isStatic, String eol, String prefix,
      String sourcePrefix, String sourceSuffix) {
    // build method source
    SourceBuilder sb = new SourceBuilder(insertOffset);
    {
      sb.append(sourcePrefix);
      sb.append(prefix);
      // may be static
      if (isStatic) {
        sb.append("static ");
      }
      // may be return type
      {
        Type returnType = functionType.getReturnType();
        if (returnType != null) {
          String typeSource = utils.getTypeSource(returnType);
          if (!typeSource.equals("dynamic")) {
            sb.startPosition("RETURN_TYPE");
            sb.append(typeSource);
            sb.endPosition();
            sb.append(" ");
          }
        }
      }
      // append name
      {
        sb.startPosition("NAME");
        sb.append(name);
        sb.endPosition();
      }
      // append parameters
      sb.append("(");
      ParameterElement[] parameters = functionType.getParameters();
      for (int i = 0; i < parameters.length; i++) {
        ParameterElement parameter = parameters[i];
        // append separator
        if (i != 0) {
          sb.append(", ");
        }
        // append type name
        Type type = parameter.getType();
        String typeSource = utils.getTypeSource(type);
        {
          sb.startPosition("TYPE" + i);
          sb.append(typeSource);
          addSuperTypeProposals(sb, Sets.<Type> newHashSet(), type);
          sb.endPosition();
        }
        sb.append(" ");
        // append parameter name
        {
          sb.startPosition("ARG" + i);
          sb.append(parameter.getDisplayName());
          sb.endPosition();
        }
      }
      sb.append(")");
      // close method
      sb.append(" {" + eol + prefix + "}");
      sb.append(sourceSuffix);
    }
    // insert source
    addInsertEdit(insertOffset, sb.toString());
    // add linked positions
    if (Objects.equal(targetSource, source)) {
      addLinkedPosition("NAME", sb, rangeNode(node));
    }
    addLinkedPositions(sb);
  }

  /**
   * Adds proposal for creating method corresponding to the given {@link FunctionType} in the given
   * {@link ClassElement}.
   */
  private void addProposal_createFunction_function(FunctionType functionType) throws Exception {
    String name = ((SimpleIdentifier) node).getName();
    // prepare environment
    String eol = utils.getEndOfLine();
    int insertOffset = unit.getEnd();
    // prepare prefix
    String prefix = "";
    String sourcePrefix = eol + eol;
    String sourceSuffix = eol;
    addProposal_createFunction(
        functionType,
        name,
        source,
        insertOffset,
        false,
        eol,
        prefix,
        sourcePrefix,
        sourceSuffix);
    // add proposal
    addUnitCorrectionProposal(source, CorrectionKind.QF_CREATE_FUNCTION, name);
  }

  /**
   * Adds proposal for creating method corresponding to the given {@link FunctionType} in the given
   * {@link ClassElement}.
   */
  private void addProposal_createFunction_method(ClassElement targetClassElement,
      FunctionType functionType) throws Exception {
    String name = ((SimpleIdentifier) node).getName();
    // prepare environment
    String eol = utils.getEndOfLine();
    Source targetSource = targetClassElement.getSource();
    // prepare insert offset
    ClassDeclaration targetClassNode = targetClassElement.getNode();
    int insertOffset = targetClassNode.getEnd() - 1;
    // prepare prefix
    String prefix = "  ";
    String sourcePrefix;
    if (targetClassNode.getMembers().isEmpty()) {
      sourcePrefix = "";
    } else {
      sourcePrefix = prefix + eol;
    }
    String sourceSuffix = eol;
    addProposal_createFunction(
        functionType,
        name,
        targetSource,
        insertOffset,
        inStaticMemberContext(),
        eol,
        prefix,
        sourcePrefix,
        sourceSuffix);
    // add proposal
    addUnitCorrectionProposal(targetSource, CorrectionKind.QF_CREATE_METHOD, name);
  }

  private void addRemoveEdit(SourceRange range) {
    textEdits.add(createRemoveEdit(range));
  }

  /**
   * Adds {@link Edit} to {@link #textEdits}.
   */
  private void addReplaceEdit(SourceRange range, String text) {
    textEdits.add(createReplaceEdit(range, text));
  }

  /**
   * Adds {@link CorrectionProposal} with single {@link SourceChange} to {@link #proposals}.
   */
  private void addUnitCorrectionProposal(CorrectionKind kind, Object... arguments) {
    addUnitCorrectionProposal(source, kind, arguments);
  }

  /**
   * Adds {@link CorrectionProposal} with single {@link SourceChange} to {@link #proposals}.
   */
  private void addUnitCorrectionProposal(Source source, CorrectionKind kind, Object... arguments) {
    if (!textEdits.isEmpty()) {
      // prepare SourceChange
      SourceChange change = new SourceChange(source.getShortName(), source);
      for (Edit edit : textEdits) {
        change.addEdit(edit);
      }
      // create SourceCorrectionProposal
      SourceCorrectionProposal proposal = new SourceCorrectionProposal(change, kind, arguments);
      proposal.setLinkedPositions(linkedPositions);
      proposal.setLinkedPositionProposals(linkedPositionProposals);
      proposal.setEndRange(endRange);
      // done
      proposals.add(proposal);
    }
    // reset
    resetProposalElements();
  }

  private void appendParameters(StringBuilder sb, ParameterElement[] parameters) throws Exception {
    Map<ParameterElement, String> defaultValueMap = getDefaultValueMap(parameters);
    appendParameters(sb, parameters, defaultValueMap);
  }

  private void appendParameters(StringBuilder sb, ParameterElement[] parameters,
      Map<ParameterElement, String> defaultValueMap) {
    sb.append("(");
    boolean firstParameter = true;
    boolean sawNamed = false;
    boolean sawPositional = false;
    for (ParameterElement parameter : parameters) {
      if (!firstParameter) {
        sb.append(", ");
      } else {
        firstParameter = false;
      }
      // may be optional
      ParameterKind parameterKind = parameter.getParameterKind();
      if (parameterKind == ParameterKind.NAMED) {
        if (!sawNamed) {
          sb.append("{");
          sawNamed = true;
        }
      }
      if (parameterKind == ParameterKind.POSITIONAL) {
        if (!sawPositional) {
          sb.append("[");
          sawPositional = true;
        }
      }
      // parameter
      appendParameterSource(sb, parameter.getType(), parameter.getName());
      // default value
      if (defaultValueMap != null) {
        String defaultSource = defaultValueMap.get(parameter);
        if (defaultSource != null) {
          if (sawPositional) {
            sb.append(" = ");
          } else {
            sb.append(": ");
          }
          sb.append(defaultSource);
        }
      }
    }
    // close parameters
    if (sawNamed) {
      sb.append("}");
    }
    if (sawPositional) {
      sb.append("]");
    }
    sb.append(")");
  }

  private void appendParameterSource(StringBuilder sb, Type type, String name) {
    String parameterSource = utils.getParameterSource(type, name);
    sb.append(parameterSource);
  }

  private void appendType(StringBuilder sb, Type type) {
    if (type != null && !type.isDynamic()) {
      String typeSource = utils.getTypeSource(type);
      sb.append(typeSource);
      sb.append(" ");
    }
  }

  private Edit createInsertEdit(int offset, String text) {
    return new Edit(offset, 0, text);
  }

  /**
   * @return the string to display as the name of the given constructor in a proposal name.
   */
  private String getConstructorProposalName(ConstructorElement constructor) {
    StringBuilder proposalNameBuffer = new StringBuilder();
    proposalNameBuffer.append("super");
    // may be named
    String constructorName = constructor.getDisplayName();
    if (!constructorName.isEmpty()) {
      proposalNameBuffer.append(".");
      proposalNameBuffer.append(constructorName);
    }
    // parameters
    appendParameters(proposalNameBuffer, constructor.getParameters(), null);
    // done
    return proposalNameBuffer.toString();
  }

  /**
   * Returns the {@link Type} with given name from the {@code dart:core} library.
   */
  private Type getCoreType(String name) {
    LibraryElement[] libraries = unitLibraryElement.getImportedLibraries();
    for (LibraryElement library : libraries) {
      if (library.isDartCore()) {
        ClassElement classElement = library.getType(name);
        if (classElement != null) {
          return classElement.getType();
        }
        return null;
      }
    }
    return null;
  }

  private Type getCoreTypeBool() {
    return getCoreType("bool");
  }

  private Map<ParameterElement, String> getDefaultValueMap(ParameterElement[] parameters)
      throws Exception {
    Map<ParameterElement, String> defaultSourceMap = Maps.newHashMap();
    for (ParameterElement parameter : parameters) {
      defaultSourceMap.put(parameter, parameter.getDefaultValueCode());
    }
    return defaultSourceMap;
  }

  /**
   * @return {@code true} if {@link #node} if part of a static method or any field initializer.
   */
  private boolean inStaticMemberContext() {
    ClassMember member = node.getAncestor(ClassMember.class);
    return inStaticMemberContext(member);
  }

  /**
   * @return {@code true} if the given {@link ClassMember} is a part of a static method or any field
   *         initializer.
   */
  private boolean inStaticMemberContext(ClassMember member) {
    if (member instanceof MethodDeclaration) {
      return ((MethodDeclaration) member).isStatic();
    }
    // field initializer cannot reference "this"
    if (member instanceof FieldDeclaration) {
      return true;
    }
    return false;
  }

  private NewConstructorLocation prepareNewConstructorLocation(ClassDeclaration classDeclaration,
      String eol) {
    List<ClassMember> members = classDeclaration.getMembers();
    // find the last field/constructor
    ClassMember lastFieldOrConstructor = null;
    for (ClassMember member : members) {
      if (member instanceof FieldDeclaration || member instanceof ConstructorDeclaration) {
        lastFieldOrConstructor = member;
      } else {
        break;
      }
    }
    // after the field/constructor
    if (lastFieldOrConstructor != null) {
      return new NewConstructorLocation(eol + eol, lastFieldOrConstructor.getEnd(), "");
    }
    // at the beginning of the class
    String suffix = members.isEmpty() ? "" : eol;
    return new NewConstructorLocation(eol, classDeclaration.getLeftBracket().getEnd(), suffix);
  }

  /**
   * Removes any {@link ParenthesizedExpression} enclosing the given {@link Expression}.
   * 
   * @param expr the expression in {@link ParenthesizedExpression}
   * @param exprPrecedence the effective precedence of the "expr", may be not its
   *          {@link Expression#getPrecedence()}
   */
  private void removeEnclosingParentheses(Expression expr, int exprPrecedence) {
    while (expr.getParent() instanceof ParenthesizedExpression) {
      ParenthesizedExpression parenthesized = (ParenthesizedExpression) expr.getParent();
      if (CorrectionUtils.getExpressionParentPrecedence(parenthesized) > exprPrecedence) {
        break;
      }
      addRemoveEdit(rangeToken(parenthesized.getLeftParenthesis()));
      addRemoveEdit(rangeToken(parenthesized.getRightParenthesis()));
      expr = parenthesized;
    }
  }

  private void resetProposalElements() {
    textEdits.clear();
    linkedPositions.clear();
    positionStopEdits.clear();
    linkedPositionProposals.clear();
    endRange = null;
  }

  private void updateFinderWithClassMembers(ClosestElementFinder finder, ClassElement classElement) {
    if (classElement != null) {
      List<Element> members = HierarchyUtils.getMembers(classElement, false);
      finder.update(members);
    }
  }
}
