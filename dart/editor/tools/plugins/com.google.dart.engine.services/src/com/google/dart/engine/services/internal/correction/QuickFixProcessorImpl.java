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
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.CompilationUnitMember;
import com.google.dart.engine.ast.Directive;
import com.google.dart.engine.ast.FunctionBody;
import com.google.dart.engine.ast.ImportDirective;
import com.google.dart.engine.ast.LibraryDirective;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.ast.MethodInvocation;
import com.google.dart.engine.ast.PartDirective;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.SimpleStringLiteral;
import com.google.dart.engine.ast.TypeName;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ElementKind;
import com.google.dart.engine.element.ImportElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.PrefixElement;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.CompileTimeErrorCode;
import com.google.dart.engine.error.ErrorCode;
import com.google.dart.engine.error.StaticTypeWarningCode;
import com.google.dart.engine.error.StaticWarningCode;
import com.google.dart.engine.formatter.edit.Edit;
import com.google.dart.engine.parser.ParserErrorCode;
import com.google.dart.engine.sdk.DartSdk;
import com.google.dart.engine.sdk.SdkLibrary;
import com.google.dart.engine.services.assist.AssistContext;
import com.google.dart.engine.services.change.SourceChange;
import com.google.dart.engine.services.correction.CorrectionKind;
import com.google.dart.engine.services.correction.CorrectionProposal;
import com.google.dart.engine.services.correction.CreateFileCorrectionProposal;
import com.google.dart.engine.services.correction.QuickFixProcessor;
import com.google.dart.engine.services.correction.SourceCorrectionProposal;
import com.google.dart.engine.services.internal.correction.CorrectionUtils.TopInsertDesc;
import com.google.dart.engine.source.FileBasedSource;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceFactory;
import com.google.dart.engine.type.Type;
import com.google.dart.engine.utilities.instrumentation.Instrumentation;
import com.google.dart.engine.utilities.instrumentation.InstrumentationBuilder;
import com.google.dart.engine.utilities.source.SourceRange;

import static com.google.dart.engine.utilities.source.SourceRangeFactory.rangeEndEnd;
import static com.google.dart.engine.utilities.source.SourceRangeFactory.rangeEndStart;
import static com.google.dart.engine.utilities.source.SourceRangeFactory.rangeError;
import static com.google.dart.engine.utilities.source.SourceRangeFactory.rangeNode;
import static com.google.dart.engine.utilities.source.SourceRangeFactory.rangeStartLength;

import java.io.File;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.lang.model.type.TypeKind;

/**
 * Implementation of {@link QuickFixProcessor}.
 */
public class QuickFixProcessorImpl implements QuickFixProcessor {
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
   * @return <code>true</code> if given {@link DartNode} could be type name.
   */
  private static boolean mayBeTypeIdentifier(ASTNode node) {
    if (node instanceof SimpleIdentifier) {
      if (node.getParent() instanceof TypeName) {
        return true;
      }
      if (node.getParent() instanceof MethodInvocation) {
        MethodInvocation invocation = (MethodInvocation) node.getParent();
        return invocation.getRealTarget() == node;
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
  private File unitLibraryFile;
  private File unitLibraryFolder;
  private ASTNode node;
  private int selectionOffset;
  private int selectionLength;
  private CorrectionUtils utils;

//  private SourceRange proposalEndRange = null;

  private final Map<SourceRange, Edit> positionStopEdits = Maps.newHashMap();

  private final Map<String, List<SourceRange>> linkedPositions = Maps.newHashMap();

  private final Map<String, List<LinkedPositionProposal>> linkedPositionProposals = Maps.newHashMap();

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
    node = utils.findNode(selectionOffset, ASTNode.class);
    //
    final InstrumentationBuilder instrumentation = Instrumentation.builder(this.getClass());
    try {
      ErrorCode errorCode = problem.getErrorCode();
      if (errorCode == CompileTimeErrorCode.URI_DOES_NOT_EXIST) {
        addFix_createPart();
      }
      if (errorCode == ParserErrorCode.EXPECTED_TOKEN) {
        addFix_insertSemicolon();
      }
      if (errorCode == ParserErrorCode.GETTER_WITH_PARAMETERS) {
        addFix_removeParameters_inGetterDeclaration();
      }
      if (errorCode == StaticWarningCode.UNDEFINED_CLASS) {
        addFix_importLibrary_withType();
        addFix_createClass();
      }
      if (errorCode == StaticWarningCode.UNDEFINED_CLASS_BOOLEAN) {
        addFix_boolInsteadOfBoolean();
      }
      if (errorCode == StaticWarningCode.UNDEFINED_IDENTIFIER) {
        addFix_importLibrary_withType();
        addFix_importLibrary_withTopLevelVariable();
      }
      if (errorCode == StaticTypeWarningCode.INVOCATION_OF_NON_FUNCTION) {
        addFix_removeParentheses_inGetterInvocation();
      }
      if (errorCode == StaticTypeWarningCode.UNDEFINED_FUNCTION) {
        addFix_importLibrary_withFunction();
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
  public boolean hasFix(AnalysisError problem) {
    ErrorCode errorCode = problem.getErrorCode();
//    System.out.println(errorCode.getClass() + " " + errorCode);
    return errorCode == CompileTimeErrorCode.URI_DOES_NOT_EXIST
        || errorCode == ParserErrorCode.EXPECTED_TOKEN
        || errorCode == ParserErrorCode.GETTER_WITH_PARAMETERS
        || errorCode == StaticWarningCode.UNDEFINED_CLASS
        || errorCode == StaticWarningCode.UNDEFINED_CLASS_BOOLEAN
        || errorCode == StaticWarningCode.UNDEFINED_IDENTIFIER
        || errorCode == StaticTypeWarningCode.INVOCATION_OF_NON_FUNCTION
        || errorCode == StaticTypeWarningCode.UNDEFINED_FUNCTION;
  }

  // TODO(scheglov) implement this
//  private void addFix_createConstructor() {
//    DartNewExpression newExpression = null;
//    DartNode nameNode = null;
//    String namePrefix = null;
//    String name = null;
//    // prepare "new X()"
//    if (node instanceof DartIdentifier && node.getParent().getParent() instanceof DartNewExpression) {
//      newExpression = (DartNewExpression) node.getParent().getParent();
//      // default constructor
//      if (node.getParent() instanceof DartTypeNode) {
//        namePrefix = ((DartIdentifier) node).getName();
//        name = "";
//      }
//      // named constructor
//      if (node.getParent() instanceof DartPropertyAccess) {
//        DartPropertyAccess constructorNameNode = (DartPropertyAccess) node.getParent();
//        nameNode = constructorNameNode.getName();
//        namePrefix = constructorNameNode.getQualifier().toSource() + ".";
//        name = constructorNameNode.getName().getName();
//      }
//    }
//    // prepare environment
//    String eol = utils.getEndOfLine();
//    String prefix = "  ";
//    CompilationUnit targetUnit;
//    SourceRange range;
//    {
//      ClassElement targetElement = (ClassElement) newExpression.getType().getElement();
//      {
//        SourceInfo targetSourceInfo = targetElement.getSourceInfo();
//        Source targetSource = targetSourceInfo.getSource();
//        IResource targetResource = ResourceUtil.getResource(targetSource);
//        targetUnit = (CompilationUnit) DartCore.create(targetResource);
//      }
//      range = SourceRangeFactory.forStartLength(
//          targetElement.getOpenBraceOffset() + "{".length(),
//          0);
//    }
//    // build source
//    SourceBuilder sb = new SourceBuilder(range);
//    {
//      sb.append(eol);
//      sb.append(prefix);
//      // append name
//      {
//        sb.append(namePrefix);
//        if (name != null) {
//          sb.startPosition("NAME");
//          sb.append(name);
//          sb.endPosition();
//        }
//      }
//      addFix_unresolvedMethodCreate_parameters(sb, newExpression);
//      sb.append(") {" + eol + prefix + "}");
//      sb.append(eol);
//    }
//    // insert source
//    addReplaceEdit(range, sb.toString());
//    // add linked positions
//    // TODO(scheglov) disabled, caused exception in old model, don't know why
////    if (Objects.equal(targetUnit, unit) && nameNode != null) {
////      addLinkedPosition("NAME", TrackedPositions.forNode(nameNode));
////    }
//    addLinkedPositions(sb);
//    // add proposal
//    {
//      String msg = Messages.format(
//          CorrectionMessages.QuickFixProcessor_createConstructor,
//          namePrefix + name);
//      addUnitCorrectionProposal(targetUnit, TextFileChange.FORCE_SAVE, msg, OBJ_CONSTRUCTOR_IMG);
//    }
//  }

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

  private void addFix_createPart() throws Exception {
    if (node instanceof SimpleStringLiteral && node.getParent() instanceof PartDirective) {
      SimpleStringLiteral uriLiteral = (SimpleStringLiteral) node;
      String uriString = uriLiteral.getValue();
      File newFile;
      try {
        URI uri = URI.create(uriString);
        if (uri.isAbsolute()) {
          return;
        }
        newFile = new File(unitLibraryFolder, uriString);
      } catch (IllegalArgumentException e) {
        return;
      }
      if (!newFile.exists()) {
        // prepare new source
        String source;
        {
          String eol = utils.getEndOfLine();
          String libraryName = unitLibraryElement.getName();
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
    CompilationUnit libraryUnit = CorrectionUtils.getResolvedUnit(libraryUnitElement);
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
        TopInsertDesc desc = libraryUtils.getTopInsertDesc();
        offset = desc.offset;
        prefix = desc.insertEmptyLineBefore ? eol : "";
        suffix = eol + (desc.insertEmptyLineAfter ? eol : "");
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
    // may be there is existing import, but it is with prefix and we don't use this prefix
    for (ImportElement imp : unitLibraryElement.getImports()) {
      // prepare prefix
      PrefixElement prefix = imp.getPrefix();
      if (prefix == null) {
        continue;
      }
      // prepare element
      LibraryElement libraryElement = imp.getImportedLibrary();
      Element element = CorrectionUtils.getExportedElement(libraryElement, name);
      if (element == null) {
        continue;
      }
      if (element.getKind() != kind) {
        continue;
      }
      // insert prefix
      SourceRange range = rangeStartLength(node, 0);
      addReplaceEdit(range, prefix.getName() + ".");
      addUnitCorrectionProposal(
          CorrectionKind.QF_IMPORT_LIBRARY_PREFIX,
          libraryElement.getName(),
          prefix.getName());
    }
    // check SDK libraries
    AnalysisContext context = unitLibraryElement.getContext();
    {
      DartSdk sdk = context.getSourceFactory().getDartSdk();
      AnalysisContext sdkContext = sdk.getContext();
      SdkLibrary[] sdkLibraries = sdk.getSdkLibraries();
      for (SdkLibrary sdkLibrary : sdkLibraries) {
        SourceFactory sdkSourceFactory = sdkContext.getSourceFactory();
        String libraryUri = sdkLibrary.getShortName();
        Source librarySource = sdkSourceFactory.resolveUri(null, libraryUri);
        // prepare LibraryElement
        LibraryElement libraryElement = sdkContext.getLibraryElement(librarySource);
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
        // add import
        URI unitLibraryUri = unitLibraryFolder.toURI();
        URI libraryUri = libraryFile.toURI();
        String relative = unitLibraryUri.relativize(libraryUri).getPath();
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

  /**
   * @return the possible return {@link Type}, may be <code>null</code> if can not be identified.
   *         {@link TypeKind#DYNAMIC} also returned as <code>null</code>.
   */
  // TODO(scheglov) implement this
//  private Type addFix_unresolvedMethodCreate_getReturnType(MethodInvocation invocation) {
//    Type type = null;
//    StructuralPropertyDescriptor invocationLocation = getLocationInParent(invocation);
//    if (invocationLocation == DART_VARIABLE_VALUE) {
//      DartVariable variable = (DartVariable) invocation.getParent();
//      type = variable.getElement().getType();
//    }
//    if (TypeKind.of(type) == TypeKind.DYNAMIC) {
//      type = null;
//    }
//    return type;
//  }

  // TODO(scheglov) implement this
//  private void addFix_unresolvedMethodCreate_parameters(SourceBuilder sb,
//      MethodInvocation invocation) {
//    // append parameters
//    sb.append("(");
//    Set<String> excluded = Sets.newHashSet();
//    List<DartExpression> arguments = invocation.getArguments();
//    for (int i = 0; i < arguments.size(); i++) {
//      DartExpression argument = arguments.get(i);
//      // append separator
//      if (i != 0) {
//        sb.append(", ");
//      }
//      // append type name
//      Type type = argument.getType();
//      if (type != null) {
//        String typeSource = ExtractUtils.getTypeSource(type);
//        {
//          sb.startPosition("TYPE" + i);
//          sb.append(typeSource);
//          addSuperTypeProposals(sb, Sets.<Type> newHashSet(), type);
//          sb.endPosition();
//        }
//        sb.append(" ");
//      }
//      // append parameter name
//      {
//        String[] suggestions = getArgumentNameSuggestions(excluded, type, argument, i);
//        String favorite = suggestions[0];
//        excluded.add(favorite);
//        sb.startPosition("ARG" + i);
//        sb.append(favorite);
//        sb.setProposals(suggestions);
//        sb.endPosition();
//      }
//    }
//  }

  // TODO(scheglov) waiting for https://code.google.com/p/dart/issues/detail?id=10053
//  private void addFix_useEffectiveIntegerDivision(IProblemLocation location) throws Exception {
//    for (DartNode n = node; n != null; n = n.getParent()) {
//      if (n instanceof DartMethodInvocation
//          && n.getSourceInfo().getOffset() == location.getOffset()
//          && n.getSourceInfo().getLength() == location.getLength()) {
//        DartMethodInvocation invocation = (DartMethodInvocation) n;
//        DartExpression target = invocation.getTarget();
//        while (target instanceof DartParenthesizedExpression) {
//          target = ((DartParenthesizedExpression) target).getExpression();
//        }
//        // replace "/" with "~/"
//        DartBinaryExpression binary = (DartBinaryExpression) target;
//        addReplaceEdit(
//            SourceRangeFactory.forStartLength(binary.getOperatorOffset(), "/".length()),
//            "~/");
//        // remove everything before and after
//        addRemoveEdit(SourceRangeFactory.forStartStart(invocation, binary.getArg1()));
//        addRemoveEdit(SourceRangeFactory.forEndEnd(binary.getArg2(), invocation));
//        // add proposal
//        addUnitCorrectionProposal(
//            CorrectionMessages.QuickFixProcessor_useEffectiveIntegerDivision,
//            DartPluginImages.get(DartPluginImages.IMG_CORRECTION_CHANGE));
//        // done
//        break;
//      }
//    }
//  }

  // TODO(scheglov) implement this
//  private void addFix_unresolvedMethodSimilar() throws Exception {
//    if (node instanceof DartIdentifier && node.getParent() instanceof DartInvocation) {
//      DartInvocation invocation = (DartInvocation) node.getParent();
//      String name = ((DartIdentifier) node).getName();
//      String closestName = null;
//      ClosestElementFinder finder = new ClosestElementFinder(MethodElement.class, name);
//      // unqualified invocation
//      if (invocation instanceof DartUnqualifiedInvocation
//          && ((DartUnqualifiedInvocation) invocation).getTarget() == node) {
//        // may be invocation of method
//        {
//          DartClass enclosingClass = ASTNodes.getAncestor(invocation, DartClass.class);
//          if (enclosingClass != null) {
//            List<Element> allMembers = Elements.getAllMembers(enclosingClass.getElement());
//            finder.update(allMembers);
//          }
//        }
//        // may be invocation of top-level function
//        {
//          DartUnit enclosingUnit = ASTNodes.getAncestor(invocation, DartUnit.class);
//          LibraryElement enclosingLibrary = enclosingUnit.getLibrary().getElement();
//          finder.update(enclosingLibrary.getImportScope().getElements().values());
//          finder.update(enclosingLibrary.getScope().getElements().values());
//        }
//      }
//      // qualified invocation
//      if (invocation instanceof DartMethodInvocation
//          && ((DartMethodInvocation) invocation).getFunctionName() == node) {
//        DartExpression targetExpression = ((DartMethodInvocation) invocation).getRealTarget();
//        Element targetElement = getTypeElement(targetExpression);
//        if (targetElement instanceof ClassElement) {
//          ClassElement targetClassElement = (ClassElement) targetElement;
//          List<Element> allMembers = Elements.getAllMembers(targetClassElement);
//          finder.update(allMembers);
//        }
//      }
//      // if we have close enough element, suggest to use it
//      if (finder != null && finder.distance < 5) {
//        closestName = finder.element.getName();
//        addReplaceEdit(SourceRangeFactory.create(node), closestName);
//      }
//      // add proposal
//      if (closestName != null) {
//        proposalRelevance += 1;
//        addUnitCorrectionProposal(
//            Messages.format(CorrectionMessages.QuickFixProcessor_changeTo, closestName),
//            DartPluginImages.get(DartPluginImages.IMG_CORRECTION_CHANGE));
//      }
//    }
//  }

  // https://code.google.com/p/dart/issues/detail?id=10058
  // TODO(scheglov) implement this
//  private void addFix_useStaticAccess_method() throws Exception {
//    if (getLocationInParent(node) == DART_METHOD_INVOCATION_FUNCTION_NAME) {
//      DartMethodInvocation invocation = (DartMethodInvocation) node.getParent();
//      Element methodElement = node.getElement();
//      if (methodElement instanceof MethodElement
//          && methodElement.getEnclosingElement() instanceof ClassElement) {
//        ClassElement classElement = (ClassElement) methodElement.getEnclosingElement();
//        String className = classElement.getName();
//        // if has this class in current library, use name as is
//        if (unit.getLibrary().findType(className) != null) {
//          addFix_useStaticAccess_method_proposal(invocation, className);
//          return;
//        }
//        // class from other library, may be use prefix
//        for (DartImport imp : unit.getLibrary().getImports()) {
//          if (imp.getLibrary().findType(className) != null) {
//            className = imp.getPrefix() + "." + className;
//            addFix_useStaticAccess_method_proposal(invocation, className);
//          }
//        }
//      }
//    }
//  }

//  private void addFix_useStaticAccess_method_proposal(MethodInvocation invocation, String className) {
//    DartExpression target = invocation.getTarget();
//    if (target == null) {
//      return;
//    }
//    // replace "target" with class name
//    SourceRange range = SourceRangeFactory.create(target);
//    addReplaceEdit(range, className);
//    // add proposal
//    addUnitCorrectionProposal(
//        Messages.format(CorrectionMessages.QuickFixProcessor_useStaticAccess_method, className),
//        DartPluginImages.get(DartPluginImages.IMG_CORRECTION_CHANGE));
//  }

  private void addInsertEdit(int offset, String text) {
    textEdits.add(createInsertEdit(offset, text));
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

  // TODO(scheglov) use or remove
//  private void addLinkedPositionProposal(String group, CorrectionImage icon, String text) {
//    LinkedPositionProposal proposal = new LinkedPositionProposal(icon, text);
//    addLinkedPositionProposal(group, proposal);
//  }

  private void addLinkedPositionProposal(String group, LinkedPositionProposal proposal) {
    List<LinkedPositionProposal> nodeProposals = linkedPositionProposals.get(group);
    if (nodeProposals == null) {
      nodeProposals = Lists.newArrayList();
      linkedPositionProposals.put(group, nodeProposals);
    }
    nodeProposals.add(proposal);
  }

  // TODO(scheglov) implement this
//  private void addFix_unresolvedClass_useSimilar() {
//    if (mayBeTypeIdentifier(node)) {
//      String name = ((DartIdentifier) node).getName();
//      ClosestElementFinder finder = new ClosestElementFinder(ClassElement.class, name);
//      // find closest element
//      {
//        DartUnit enclosingUnit = ASTNodes.getAncestor(node, DartUnit.class);
//        LibraryElement enclosingLibrary = enclosingUnit.getLibrary().getElement();
//        finder.update(enclosingLibrary.getImportScope().getElements().values());
//        finder.update(enclosingLibrary.getScope().getElements().values());
//      }
//      // if we have close enough element, suggest to use it
//      if (finder != null && finder.distance < 5) {
//        String closestName = finder.element.getName();
//        addReplaceEdit(SourceRangeFactory.create(node), closestName);
//        // add proposal
//        if (closestName != null) {
//          proposalRelevance += 1;
//          addUnitCorrectionProposal(
//              Messages.format(CorrectionMessages.QuickFixProcessor_changeTo, closestName),
//              DartPluginImages.get(DartPluginImages.IMG_CORRECTION_CHANGE));
//        }
//      }
//    }
//  }

  // TODO(scheglov) implement this
//  private void addFix_unresolvedMethodCreate() throws Exception {
//    if (node instanceof DartIdentifier && node.getParent() instanceof DartInvocation) {
//      String name = ((DartIdentifier) node).getName();
//      DartInvocation invocation = (DartInvocation) node.getParent();
//      // prepare environment
//      String eol = utils.getEndOfLine();
//      CompilationUnit targetUnit;
//      String prefix;
//      SourceRange range;
//      String sourcePrefix;
//      String sourceSuffix;
//      boolean staticModifier = false;
//      if (invocation instanceof DartUnqualifiedInvocation) {
//        targetUnit = unit;
//        DartClassMember<?> enclosingMember = ASTNodes.getAncestor(node, DartClassMember.class);
//        staticModifier = enclosingMember.getModifiers().isStatic();
//        prefix = utils.getNodePrefix(enclosingMember);
//        range = SourceRangeFactory.forEndLength(enclosingMember, 0);
//        sourcePrefix = eol + eol;
//        sourceSuffix = "";
//      } else {
//        SourceInfo targetSourceInfo;
//        {
//          DartExpression targetExpression = ((DartMethodInvocation) invocation).getRealTarget();
//          staticModifier = ElementKind.of(targetExpression.getElement()) == ElementKind.CLASS;
//          Element targetElement = getTypeElement(targetExpression);
//          targetSourceInfo = targetElement.getSourceInfo();
//          Source targetSource = targetSourceInfo.getSource();
//          IResource targetResource = ResourceUtil.getResource(targetSource);
//          targetUnit = (CompilationUnit) DartCore.create(targetResource);
//        }
//        prefix = "  ";
//        range = SourceRangeFactory.forStartLength(targetSourceInfo.getEnd() - 1, 0);
//        sourcePrefix = eol;
//        sourceSuffix = eol;
//      }
//      //
//      SourceBuilder sb = new SourceBuilder(range);
//      {
//        sb.append(sourcePrefix);
//        sb.append(prefix);
//        // may be "static"
//        if (staticModifier) {
//          sb.append("static ");
//        }
//        // may be return type
//        {
//          Type type = addFix_unresolvedMethodCreate_getReturnType(invocation);
//          if (type != null) {
//            sb.startPosition("RETURN_TYPE");
//            sb.append(ExtractUtils.getTypeSource(type));
//            sb.endPosition();
//            sb.append(" ");
//          }
//        }
//        // append name
//        {
//          sb.startPosition("NAME");
//          sb.append(name);
//          sb.endPosition();
//        }
//        addFix_unresolvedMethodCreate_parameters(sb, invocation);
//        sb.append(") {" + eol + prefix + "}");
//        sb.append(sourceSuffix);
//      }
//      // insert source
//      addReplaceEdit(range, sb.toString());
//      // add linked positions
//      // TODO(scheglov) disabled, caused exception in old model, don't know why
////      if (Objects.equal(targetUnit, unit)) {
////        addLinkedPosition("NAME", TrackedPositions.forNode(node));
////      }
//      addLinkedPositions(sb);
//      // add proposal
//      addUnitCorrectionProposal(
//          targetUnit,
//          TextFileChange.FORCE_SAVE,
//          Messages.format(CorrectionMessages.QuickFixProcessor_createMethod, name),
//          DartPluginImages.get(DartPluginImages.IMG_CORRECTION_CHANGE));
//    }
//  }

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
      // done
      proposal.setLinkedPositions(linkedPositions);
      proposal.setLinkedPositionProposals(linkedPositionProposals);
      proposals.add(proposal);
    }
    // reset
    resetProposalElements();
  }

  private Edit createInsertEdit(int offset, String text) {
    return new Edit(offset, 0, text);
  }

  private void resetProposalElements() {
    textEdits.clear();
    linkedPositions.clear();
    positionStopEdits.clear();
    linkedPositionProposals.clear();
//    proposalEndRange = null;
  }
}
