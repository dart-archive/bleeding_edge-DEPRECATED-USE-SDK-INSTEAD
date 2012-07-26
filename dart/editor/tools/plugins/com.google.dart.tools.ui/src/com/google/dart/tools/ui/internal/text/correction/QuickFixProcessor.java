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

import com.google.common.collect.Lists;
import com.google.dart.compiler.ErrorCode;
import com.google.dart.compiler.ast.DartDirective;
import com.google.dart.compiler.ast.DartIdentifier;
import com.google.dart.compiler.ast.DartImportDirective;
import com.google.dart.compiler.ast.DartLibraryDirective;
import com.google.dart.compiler.ast.DartMethodInvocation;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.ast.DartTypeNode;
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.compiler.resolver.ClassElement;
import com.google.dart.compiler.resolver.Element;
import com.google.dart.compiler.resolver.MethodElement;
import com.google.dart.compiler.resolver.TypeErrorCode;
import com.google.dart.compiler.util.apache.StringUtils;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.dom.PropertyDescriptorHelper;
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
import com.google.dart.tools.internal.corext.refactoring.code.ExtractUtils;
import com.google.dart.tools.internal.corext.refactoring.util.ExecutionUtils;
import com.google.dart.tools.internal.corext.refactoring.util.Messages;
import com.google.dart.tools.internal.corext.refactoring.util.RunnableEx;
import com.google.dart.tools.ui.DartPluginImages;
import com.google.dart.tools.ui.internal.text.correction.proposals.CUCorrectionProposal;
import com.google.dart.tools.ui.text.dart.IDartCompletionProposal;
import com.google.dart.tools.ui.text.dart.IInvocationContext;
import com.google.dart.tools.ui.text.dart.IProblemLocation;
import com.google.dart.tools.ui.text.dart.IQuickFixProcessor;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.graphics.Image;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

import java.net.URI;
import java.util.List;

/**
 * Standard {@link IQuickFixProcessor} for Dart.
 * 
 * @coverage dart.editor.ui.correction
 */
public class QuickFixProcessor implements IQuickFixProcessor {
  private static final int DEFAULT_RELEVANCE = 50;

  private static ReplaceEdit createReplaceEdit(SourceRange range, String text) {
    return new ReplaceEdit(range.getOffset(), range.getLength(), text);
  }

  private CompilationUnit unit;
  private ExtractUtils utils;
  private DartNode node;

  private final List<ICommandAccess> proposals = Lists.newArrayList();

  private final List<TextEdit> textEdits = Lists.newArrayList();
  private int proposalRelevance = DEFAULT_RELEVANCE;

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
    return errorCode == TypeErrorCode.IS_STATIC_METHOD_IN
        || errorCode == TypeErrorCode.NO_SUCH_TYPE;
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
          textEdits.add(createReplaceEdit(range, prefix + "."));
          // add proposal
          proposalRelevance++;
          addUnitCorrectionProposal(
              libraryUnit,
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
    textEdits.add(createReplaceEdit(range, importSource));
    // add proposal
    addUnitCorrectionProposal(
        libraryUnit,
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
    textEdits.add(createReplaceEdit(range, className));
    // add proposal
    addUnitCorrectionProposal(
        Messages.format(CorrectionMessages.QuickFixProcessor_useStaticAccess_method, className),
        DartPluginImages.get(DartPluginImages.IMG_CORRECTION_CHANGE));
  }

  /**
   * Adds new {@link CUCorrectionProposal} using given "unit" and {@link #textEdits}.
   */
  private void addUnitCorrectionProposal(CompilationUnit unit, String label, Image image) {
    // prepare change
    CompilationUnitChange change = new CompilationUnitChange(label, unit);
    change.setEdit(new MultiTextEdit());
    // add edits
    for (TextEdit textEdit : textEdits) {
      change.addEdit(textEdit);
    }
    // add proposal
    if (!textEdits.isEmpty()) {
      proposals.add(new CUCorrectionProposal(label, unit, change, proposalRelevance, image));
    }
    // done
    resetProposalElements();
  }

  /**
   * Adds new {@link CUCorrectionProposal} using {@link #unit} and {@link #textEdits}.
   */
  private void addUnitCorrectionProposal(String label, Image image) {
    addUnitCorrectionProposal(unit, label, image);
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
    proposalRelevance = DEFAULT_RELEVANCE;
  }
}
