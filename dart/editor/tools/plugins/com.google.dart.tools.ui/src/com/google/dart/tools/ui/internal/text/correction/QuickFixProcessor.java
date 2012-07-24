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
import com.google.dart.compiler.ast.DartMethodInvocation;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.resolver.ClassElement;
import com.google.dart.compiler.resolver.Element;
import com.google.dart.compiler.resolver.MethodElement;
import com.google.dart.compiler.resolver.TypeErrorCode;
import com.google.dart.tools.core.dom.PropertyDescriptorHelper;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.SourceRange;
import com.google.dart.tools.core.refactoring.CompilationUnitChange;
import com.google.dart.tools.internal.corext.SourceRangeFactory;
import com.google.dart.tools.internal.corext.refactoring.code.ExtractUtils;
import com.google.dart.tools.internal.corext.refactoring.util.Messages;
import com.google.dart.tools.ui.DartPluginImages;
import com.google.dart.tools.ui.internal.text.correction.proposals.CUCorrectionProposal;
import com.google.dart.tools.ui.text.dart.IDartCompletionProposal;
import com.google.dart.tools.ui.text.dart.IInvocationContext;
import com.google.dart.tools.ui.text.dart.IProblemLocation;
import com.google.dart.tools.ui.text.dart.IQuickFixProcessor;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.graphics.Image;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

import java.util.List;

/**
 * Standard {@link IQuickFixProcessor} for Dart.
 * 
 * @coverage dart.editor.ui.correction
 */
public class QuickFixProcessor implements IQuickFixProcessor {
  private static ReplaceEdit createReplaceEdit(SourceRange range, String text) {
    return new ReplaceEdit(range.getOffset(), range.getLength(), text);
  }

  private CompilationUnit unit;
  private ExtractUtils utils;
  private DartNode node;
  private final List<ICommandAccess> proposals = Lists.newArrayList();

  private final List<TextEdit> textEdits = Lists.newArrayList();

  @Override
  public IDartCompletionProposal[] getCorrections(IInvocationContext context,
      IProblemLocation[] locations) throws CoreException {
    proposals.clear();
    unit = context.getCompilationUnit();
    utils = new ExtractUtils(unit, context.getASTRoot());
    for (IProblemLocation location : locations) {
      textEdits.clear();
      node = location.getCoveringNode(utils.getUnitNode());
      if (node != null) {
        ErrorCode errorCode = location.getProblemId();
        if (errorCode == TypeErrorCode.IS_STATIC_METHOD_IN) {
          addFix_useStaticAccess_method(location);
        }
      }
    }
    return proposals.toArray(new IDartCompletionProposal[proposals.size()]);
  }

  @Override
  public boolean hasCorrections(CompilationUnit unit, ErrorCode problemId) {
    return true;
  }

  private void addFix_useStaticAccess_method(IProblemLocation location) {
    if (PropertyDescriptorHelper.getLocationInParent(node) == PropertyDescriptorHelper.DART_METHOD_INVOCATION_FUNCTION_NAME) {
      DartMethodInvocation invocation = (DartMethodInvocation) node.getParent();
      Element methodElement = node.getElement();
      if (methodElement instanceof MethodElement
          && methodElement.getEnclosingElement() instanceof ClassElement) {
        ClassElement classElement = (ClassElement) methodElement.getEnclosingElement();
        // TODO(scheglov) we should use name with import prefix
        String className = classElement.getName();
        // replace "target" with class name
        SourceRange range = SourceRangeFactory.create(invocation.getTarget());
        textEdits.add(createReplaceEdit(range, className));
        // add proposal
        addUnitCorrectionProposal(
            Messages.format(CorrectionMessages.QuickFixProcessor_useStaticAccess_method, className),
            DartPluginImages.get(DartPluginImages.IMG_CORRECTION_CHANGE));
      }
    }
  }

  /**
   * Adds new {@link CUCorrectionProposal} using {@link #unit} and {@link #textEdits}.
   */
  private void addUnitCorrectionProposal(String label, Image image) {
    // prepare change
    CompilationUnitChange change = new CompilationUnitChange(label, unit);
    change.setEdit(new MultiTextEdit());
    // add edits
    for (TextEdit textEdit : textEdits) {
      change.addEdit(textEdit);
    }
    // add proposal
    if (!textEdits.isEmpty()) {
      proposals.add(new CUCorrectionProposal(label, unit, change, 1, image));
    }
  }
}
