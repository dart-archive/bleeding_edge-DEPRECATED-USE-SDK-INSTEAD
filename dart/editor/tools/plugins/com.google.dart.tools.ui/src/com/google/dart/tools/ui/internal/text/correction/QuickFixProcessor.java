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
package com.google.dart.tools.ui.internal.text.correction;

import static com.google.dart.tools.core.dom.PropertyDescriptorHelper.DART_VARIABLE_VALUE;
import static com.google.dart.tools.core.dom.PropertyDescriptorHelper.getLocationInParent;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.dart.compiler.ErrorCode;
import com.google.dart.compiler.ast.DartDirective;
import com.google.dart.compiler.ast.DartExpression;
import com.google.dart.compiler.ast.DartIdentifier;
import com.google.dart.compiler.ast.DartImportDirective;
import com.google.dart.compiler.ast.DartInvocation;
import com.google.dart.compiler.ast.DartLibraryDirective;
import com.google.dart.compiler.ast.DartMethodDefinition;
import com.google.dart.compiler.ast.DartMethodInvocation;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.ast.DartTypeNode;
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.compiler.ast.DartUnqualifiedInvocation;
import com.google.dart.compiler.ast.DartVariable;
import com.google.dart.compiler.resolver.ClassElement;
import com.google.dart.compiler.resolver.Element;
import com.google.dart.compiler.resolver.MethodElement;
import com.google.dart.compiler.resolver.ResolverErrorCode;
import com.google.dart.compiler.resolver.TypeErrorCode;
import com.google.dart.compiler.type.Type;
import com.google.dart.compiler.type.TypeKind;
import com.google.dart.compiler.util.apache.StringUtils;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.dom.PropertyDescriptorHelper;
import com.google.dart.tools.core.dom.StructuralPropertyDescriptor;
import com.google.dart.tools.core.dom.rewrite.TrackedNodePosition;
import com.google.dart.tools.core.internal.model.EditorLibraryManager;
import com.google.dart.tools.core.internal.model.SystemLibraryManagerProvider;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartImport;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartModel;
import com.google.dart.tools.core.model.DartProject;
import com.google.dart.tools.core.model.SourceRange;
import com.google.dart.tools.core.refactoring.CompilationUnitChange;
import com.google.dart.tools.core.utilities.compiler.DartCompilerUtilities;
import com.google.dart.tools.internal.corext.SourceRangeFactory;
import com.google.dart.tools.internal.corext.codemanipulation.StubUtility;
import com.google.dart.tools.internal.corext.dom.ASTNodes;
import com.google.dart.tools.internal.corext.refactoring.code.ExtractUtils;
import com.google.dart.tools.internal.corext.refactoring.util.ExecutionUtils;
import com.google.dart.tools.internal.corext.refactoring.util.Messages;
import com.google.dart.tools.internal.corext.refactoring.util.RunnableEx;
import com.google.dart.tools.ui.DartPluginImages;
import com.google.dart.tools.ui.internal.text.correction.proposals.CUCorrectionProposal;
import com.google.dart.tools.ui.internal.text.correction.proposals.LinkedCorrectionProposal;
import com.google.dart.tools.ui.internal.text.correction.proposals.SourceBuilder;
import com.google.dart.tools.ui.internal.text.correction.proposals.TrackedPositions;
import com.google.dart.tools.ui.text.dart.IDartCompletionProposal;
import com.google.dart.tools.ui.text.dart.IInvocationContext;
import com.google.dart.tools.ui.text.dart.IProblemLocation;
import com.google.dart.tools.ui.text.dart.IQuickFixProcessor;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.swt.graphics.Image;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Standard {@link IQuickFixProcessor} for Dart.
 * 
 * @coverage dart.editor.ui.correction
 */
public class QuickFixProcessor implements IQuickFixProcessor {
  private static final int DEFAULT_RELEVANCE = 50;

//  private static ReplaceEdit createInsertEdit(int offset, String text) {
//    return new ReplaceEdit(offset, 0, text);
//  }
//
//  private static ReplaceEdit createRemoveEdit(SourceRange range) {
//    return createReplaceEdit(range, "");
//  }

  private static ReplaceEdit createReplaceEdit(SourceRange range, String text) {
    return new ReplaceEdit(range.getOffset(), range.getLength(), text);
  }

  /**
   * @return the suggestions for given {@link Type} and {@link DartExpression}, not empty.
   */
  private static String[] getArgumentNameSuggestions(Set<String> excluded, Type type,
      DartExpression expression, int index) {
    String[] suggestions = StubUtility.getVariableNameSuggestions(type, expression, excluded);
    if (suggestions.length != 0) {
      return suggestions;
    }
    return new String[] {"arg" + index};
  }

  private CompilationUnit unit;
  private ExtractUtils utils;
  private DartNode node;

  private final List<ICommandAccess> proposals = Lists.newArrayList();
  private int proposalRelevance = DEFAULT_RELEVANCE;
  private final List<TextEdit> textEdits = Lists.newArrayList();
  private final Map<String, List<TrackedNodePosition>> linkedPositions = Maps.newHashMap();

  private LinkedCorrectionProposal proposal;

  @Override
  public IDartCompletionProposal[] getCorrections(IInvocationContext context,
      IProblemLocation[] locations) throws CoreException {
    proposals.clear();
    unit = context.getCompilationUnit();
    utils = new ExtractUtils(unit, context.getASTRoot());
    for (final IProblemLocation location : locations) {
      resetProposalElements();
      node = location.getCoveringNode(utils.getUnitNode());
      if (node != null) {
        ExecutionUtils.runIgnore(new RunnableEx() {
          @Override
          public void run() throws Exception {
            ErrorCode errorCode = location.getProblemId();
            if (errorCode == ResolverErrorCode.CANNOT_RESOLVE_METHOD) {
              addFix_createUnresolvedMethod(location);
            }
            if (errorCode == TypeErrorCode.IS_STATIC_METHOD_IN) {
              addFix_useStaticAccess_method(location);
            }
            if (errorCode == TypeErrorCode.NO_SUCH_TYPE) {
              addFix_importLibrary_withType(location);
            }
          }

        });
      }
    }
    return proposals.toArray(new IDartCompletionProposal[proposals.size()]);
  }

  @Override
  public boolean hasCorrections(CompilationUnit unit, ErrorCode errorCode) {
    return errorCode == ResolverErrorCode.CANNOT_RESOLVE_METHOD
        || errorCode == TypeErrorCode.IS_STATIC_METHOD_IN
        || errorCode == TypeErrorCode.NO_SUCH_TYPE;
  }

  private void addFix_createUnresolvedMethod(IProblemLocation location) {
    if (node instanceof DartIdentifier && node.getParent() instanceof DartUnqualifiedInvocation) {
      String name = ((DartIdentifier) node).getName();
      DartUnqualifiedInvocation invocation = (DartUnqualifiedInvocation) node.getParent();
      DartMethodDefinition enclosingMethod = ASTNodes.getAncestor(node, DartMethodDefinition.class);
      // prepare environment
      String eol = utils.getEndOfLine();
      String prefix = utils.getNodePrefix(enclosingMethod);
      //
      SourceRange range = SourceRangeFactory.forEndLength(enclosingMethod, 0);
      SourceBuilder sb = new SourceBuilder(range);
      {
        sb.append(eol + eol + prefix);
        // may be return type
        {
          Type type = addFix_createUnresolvedMethod_getReturnType(invocation);
          if (type != null) {
            sb.startPosition("RETURN_TYPE");
            sb.append(ExtractUtils.getTypeSource(type));
            sb.endPosition();
            sb.append(" ");
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
        Set<String> excluded = Sets.newHashSet();
        List<DartExpression> arguments = invocation.getArguments();
        for (int i = 0; i < arguments.size(); i++) {
          DartExpression argument = arguments.get(i);
          // append separator
          if (i != 0) {
            sb.append(", ");
          }
          // append type name
          Type type = argument.getType();
          if (type != null) {
            String typeSource = ExtractUtils.getTypeSource(type);
            {
              sb.startPosition("TYPE" + i);
              sb.append(typeSource);
              sb.endPosition();
            }
            sb.append(" ");
          }
          // append parameter name
          {
            sb.startPosition("ARG" + i);
            String[] suggestions = getArgumentNameSuggestions(excluded, type, argument, i);
            sb.append(suggestions[0]);
            sb.endPosition();
          }
        }
        sb.append(") {" + eol + prefix + "}");
      }
      // insert source
      addReplaceEdit(range, sb.toString());
      addLinkedPosition("NAME", TrackedPositions.forNode(node));
      addLinkedPositions(sb);
      // add proposal
      addUnitCorrectionProposal(
          unit,
          TextFileChange.FORCE_SAVE,
          Messages.format(CorrectionMessages.QuickFixProcessor_addMethod_topLevel, name),
          DartPluginImages.get(DartPluginImages.IMG_CORRECTION_CHANGE));
    }
    // TODO
//    foo(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28);
//    foo(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8);
//    foo("0", "1");
//    foo(true, false);
  }

  /**
   * @return the possible return {@link Type}, may be <code>null</code> if can not be identified.
   *         {@link TypeKind#DYNAMIC} also returned as <code>null</code>.
   */
  private Type addFix_createUnresolvedMethod_getReturnType(DartInvocation invocation) {
    Type type = null;
    StructuralPropertyDescriptor invocationLocation = getLocationInParent(invocation);
    if (invocationLocation == DART_VARIABLE_VALUE) {
      DartVariable variable = (DartVariable) invocation.getParent();
      type = variable.getElement().getType();
    }
    if (TypeKind.of(type) == TypeKind.DYNAMIC) {
      type = null;
    }
    return type;
  }

  private void addFix_importLibrary_withType(IProblemLocation location) throws Exception {
    if (node instanceof DartIdentifier && node.getParent() instanceof DartTypeNode) {
      String typeName = ((DartIdentifier) node).getName();
      // ignore if private
      if (typeName.startsWith("_")) {
        return;
      }
      // prepare base URI
      CompilationUnit libraryUnit = unit.getLibrary().getDefiningCompilationUnit();
      URI unitNormalizedUri;
      {
        URI unitUri = libraryUnit.getUnderlyingResource().getLocationURI();
        unitNormalizedUri = unitUri.resolve(".").normalize();
      }
      // may be there is existing import, but it is with prefix and we don't use this prefix
      for (DartImport imp : unit.getLibrary().getImports()) {
        String prefix = imp.getPrefix();
        if (!StringUtils.isEmpty(prefix) && imp.getLibrary().findType(typeName) != null) {
          SourceRange range = SourceRangeFactory.forStartLength(node, 0);
          addReplaceEdit(range, prefix + ".");
          // add proposal
          proposalRelevance++;
          addUnitCorrectionProposal(
              libraryUnit,
              TextFileChange.FORCE_SAVE,
              Messages.format(
                  CorrectionMessages.QuickFixProcessor_importLibrary_addPrefix,
                  new Object[] {getSource(imp.getUriRange()), prefix}),
              DartPluginImages.get(DartPluginImages.IMG_CORRECTION_CHANGE));
        }
      }
      // prepare Dart model
      DartModel model = DartCore.create(ResourcesPlugin.getWorkspace().getRoot());
      // check workspace libraries
      for (DartProject project : model.getDartProjects()) {
        for (DartLibrary library : project.getDartLibraries()) {
          if (library.findType(typeName) != null) {
            URI libraryUri = library.getUri();
            URI libraryRelativeUri = unitNormalizedUri.relativize(libraryUri);
            if (StringUtils.isEmpty(libraryRelativeUri.getScheme())) {
              String importPath = libraryRelativeUri.toString();
              addFix_importLibrary_withType(importPath);
            }
          }
        }
      }
      // check SDK libraries
      EditorLibraryManager libraryManager = SystemLibraryManagerProvider.getSystemLibraryManager();
      for (DartLibrary library : model.getBundledLibraries()) {
        if (library.findType(typeName) != null) {
          URI libraryUri = library.getUri();
          URI libraryShortUri = libraryManager.getShortUri(libraryUri);
          if (libraryShortUri != null) {
            String importPath = libraryShortUri.toString();
            addFix_importLibrary_withType(importPath);
          }
        }
      }
    }
  }

  private void addFix_importLibrary_withType(String importPath) throws Exception {
    CompilationUnit libraryUnit = unit.getLibrary().getDefiningCompilationUnit();
    // prepare new #import location
    SourceRange range;
    String prefix;
    String suffix;
    {
      String eol = utils.getEndOfLine();
      // if no directives
      range = SourceRangeFactory.forStartEnd(0, 0);
      prefix = "";
      suffix = eol;
      // after last directive in library
      {
        DartUnit libraryUnitNode = DartCompilerUtilities.parseUnit(libraryUnit);
        for (DartDirective directive : libraryUnitNode.getDirectives()) {
          if (directive instanceof DartLibraryDirective || directive instanceof DartImportDirective) {
            range = SourceRangeFactory.forEndLength(directive, 0);
            prefix = eol;
            suffix = "";
          }
        }
      }
    }
    // insert new #import
    String importSource = prefix + "#import('" + importPath + "');" + suffix;
    addReplaceEdit(range, importSource);
    // add proposal
    addUnitCorrectionProposal(
        libraryUnit,
        TextFileChange.FORCE_SAVE,
        Messages.format(CorrectionMessages.QuickFixProcessor_importLibrary, importPath),
        DartPluginImages.get(DartPluginImages.IMG_CORRECTION_CHANGE));
  }

  private void addFix_useStaticAccess_method(IProblemLocation location) throws Exception {
    if (PropertyDescriptorHelper.getLocationInParent(node) == PropertyDescriptorHelper.DART_METHOD_INVOCATION_FUNCTION_NAME) {
      DartMethodInvocation invocation = (DartMethodInvocation) node.getParent();
      Element methodElement = node.getElement();
      if (methodElement instanceof MethodElement
          && methodElement.getEnclosingElement() instanceof ClassElement) {
        ClassElement classElement = (ClassElement) methodElement.getEnclosingElement();
        String className = classElement.getName();
        // if has this class in current library, use name as is
        if (unit.getLibrary().findType(className) != null) {
          addFix_useStaticAccess_method_proposal(invocation, className);
          return;
        }
        // class from other library, may be use prefix
        for (DartImport imp : unit.getLibrary().getImports()) {
          if (imp.getLibrary().findType(className) != null) {
            className = imp.getPrefix() + "." + className;
            addFix_useStaticAccess_method_proposal(invocation, className);
          }
        }
      }
    }
  }

  private void addFix_useStaticAccess_method_proposal(DartMethodInvocation invocation,
      String className) {
    // replace "target" with class name
    SourceRange range = SourceRangeFactory.create(invocation.getTarget());
    addReplaceEdit(range, className);
    // add proposal
    addUnitCorrectionProposal(
        Messages.format(CorrectionMessages.QuickFixProcessor_useStaticAccess_method, className),
        DartPluginImages.get(DartPluginImages.IMG_CORRECTION_CHANGE));
  }

//  private void addInsertEdit(int offset, String text) {
//    textEdits.add(createInsertEdit(offset, text));
//  }

  private void addLinkedPosition(String group, TrackedNodePosition position) {
    List<TrackedNodePosition> positions = linkedPositions.get(group);
    if (positions == null) {
      positions = Lists.newArrayList();
      linkedPositions.put(group, positions);
    }
    positions.add(position);
  }

  /**
   * Adds positions from the given {@link SourceBuilder} to the {@link #linkedPositions}.
   */
  private void addLinkedPositions(SourceBuilder builder) {
    Map<String, List<TrackedNodePosition>> builderPositions = builder.getTrackedPositions();
    for (Entry<String, List<TrackedNodePosition>> entry : builderPositions.entrySet()) {
      String groupId = entry.getKey();
      for (TrackedNodePosition position : entry.getValue()) {
        addLinkedPosition(groupId, position);
      }
    }
  }

  /**
   * Adds {@link #linkedPositions} to the current {@link #proposal}.
   */
  private void addLinkedPositionsToProposal() {
    for (Entry<String, List<TrackedNodePosition>> entry : linkedPositions.entrySet()) {
      String groupId = entry.getKey();
      for (TrackedNodePosition position : entry.getValue()) {
        proposal.addLinkedPosition(position, false, groupId);
      }
    }
  }

//  private void addRemoveEdit(SourceRange range) {
//    textEdits.add(createRemoveEdit(range));
//  }

  private void addReplaceEdit(SourceRange range, String text) {
    textEdits.add(createReplaceEdit(range, text));
  }

  /**
   * Adds new {@link CUCorrectionProposal} using given "unit" and {@link #textEdits}.
   */
  private void addUnitCorrectionProposal(CompilationUnit unit, int saveMode, String label,
      Image image) {
    // prepare change
    CompilationUnitChange change = new CompilationUnitChange(label, unit);
    change.setSaveMode(saveMode);
    change.setEdit(new MultiTextEdit());
    // add edits
    for (TextEdit textEdit : textEdits) {
      change.addEdit(textEdit);
    }
    // add proposal
    if (!textEdits.isEmpty()) {
      proposal = new LinkedCorrectionProposal(label, unit, change, proposalRelevance, image);
      addLinkedPositionsToProposal();
      proposals.add(proposal);
    }
    // done
    resetProposalElements();
  }

  /**
   * Adds new {@link CUCorrectionProposal} using {@link #unit} and {@link #textEdits}.
   */
  private void addUnitCorrectionProposal(String label, Image image) {
    addUnitCorrectionProposal(unit, TextFileChange.LEAVE_DIRTY, label, image);
  }

  /**
   * @return the part of {@link #unit} source.
   */
  private String getSource(int offset, int length) throws Exception {
    return unit.getBuffer().getText(offset, length);
  }

  /**
   * @return the part of {@link #unit} source.
   */
  private String getSource(SourceRange range) throws Exception {
    return getSource(range.getOffset(), range.getLength());
  }

  /**
   * Sets default values for fields used to create proposal in
   * {@link #addUnitCorrectionProposal(String, Image)}.
   */
  private void resetProposalElements() {
    textEdits.clear();
    linkedPositions.clear();
    proposalRelevance = DEFAULT_RELEVANCE;
  }
}
