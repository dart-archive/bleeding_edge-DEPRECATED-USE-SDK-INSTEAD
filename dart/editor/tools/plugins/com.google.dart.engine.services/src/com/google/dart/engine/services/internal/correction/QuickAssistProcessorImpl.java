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

import com.google.common.collect.Lists;
import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.ast.BinaryExpression;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.formatter.edit.Edit;
import com.google.dart.engine.services.assist.AssistContext;
import com.google.dart.engine.services.correction.CorrectionImage;
import com.google.dart.engine.services.correction.CorrectionProposal;
import com.google.dart.engine.services.correction.QuickAssistProcessor;
import com.google.dart.engine.services.correction.SourceChange;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.utilities.source.SourceRange;
import com.google.dart.engine.utilities.source.SourceRangeFactory;

import java.lang.reflect.Method;
import java.nio.CharBuffer;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Implementation of {@link QuickAssistProcessor}.
 */
public class QuickAssistProcessorImpl implements QuickAssistProcessor {
  private static final int DEFAULT_RELEVANCE = 30;

  /**
   * @return the {@link Edit} to replace {@link SourceRange} with "text".
   */
  private static Edit createReplaceEdit(SourceRange range, String text) {
    return new Edit(range.getOffset(), range.getLength(), text);
  }

  /**
   * @return <code>true</code> if selection covers operator of the given {@link BinaryExpression}.
   */
  private static int isOperatorSelected(BinaryExpression binaryExpression, int offset, int length) {
    ASTNode left = binaryExpression.getLeftOperand();
    ASTNode right = binaryExpression.getRightOperand();
    if (isSelectingOperator(left, right, offset, length)) {
      return left.getEndToken().getEnd();
    }
    return -1;
  }

  private static boolean isSelectingOperator(ASTNode n1, ASTNode n2, int offset, int length) {
    // between the nodes
    if (offset >= n1.getEndToken().getEnd() && offset + length <= n2.getOffset()) {
      return true;
    }
    // or exactly select the node (but not with infix expressions)
    if (offset == n1.getOffset() && offset + length == n2.getEndToken().getEnd()) {
      if (n1 instanceof BinaryExpression || n2 instanceof BinaryExpression) {
        return false;
      }
      return true;
    }
    // invalid selection (part of node, etc)
    return false;
  }

  private final List<CorrectionProposal> proposals = Lists.newArrayList();
  private final List<Edit> textEdits = Lists.newArrayList();
  private Source source;
  private ASTNode node;
  private int selectionOffset;
  private int selectionLength;
  private int proposalRelevance = DEFAULT_RELEVANCE;

  @Override
  public CorrectionProposal[] getProposals(AssistContext context) {
    proposals.clear();
    source = context.getSource();
    node = context.getCoveringNode();
    selectionOffset = context.getSelectionOffset();
    selectionLength = context.getSelectionLength();
    // TODO(scheglov) use ExecutionUtils
    for (final Method method : QuickAssistProcessorImpl.class.getDeclaredMethods()) {
      if (method.getName().startsWith("addProposal_")) {
        resetProposalElements();
        try {
          method.invoke(QuickAssistProcessorImpl.this);
        } catch (Throwable e) {
        }
//        ExecutionUtils.runIgnore(new RunnableEx() {
//          @Override
//          public void run() throws Exception {
//            method.invoke(QuickAssistProcessor.this);
//          }
//        });
      }
    }
//    try {
//      textEdits.clear();
//      addProposal_exchangeOperands();
//    } catch (Throwable e) {
//    }
    return proposals.toArray(new CorrectionProposal[proposals.size()]);
  }

  void addProposal_exchangeOperands() throws Exception {
    // check that user invokes quick assist on binary expression
    if (!(node instanceof BinaryExpression)) {
      return;
    }
    BinaryExpression binaryExpression = (BinaryExpression) node;
    // prepare operator position
    int offset = isOperatorSelected(binaryExpression, selectionOffset, selectionLength);
    if (offset == -1) {
      return;
    }
    // add edits
    {
      Expression leftOperand = binaryExpression.getLeftOperand();
      Expression rightOperand = binaryExpression.getRightOperand();
      // find "wide" enclosing binary expression with same operator
      while (binaryExpression.getParent() instanceof BinaryExpression) {
        BinaryExpression newBinaryExpression = (BinaryExpression) binaryExpression.getParent();
        if (newBinaryExpression.getOperator().getType() != binaryExpression.getOperator().getType()) {
          break;
        }
        binaryExpression = newBinaryExpression;
      }
      // exchange parts of "wide" expression parts
      SourceRange leftRange = SourceRangeFactory.rangeStartEnd(binaryExpression, leftOperand);
      SourceRange rightRange = SourceRangeFactory.rangeStartEnd(rightOperand, binaryExpression);
      addReplaceEdit(leftRange, getSource(rightRange));
      addReplaceEdit(rightRange, getSource(leftRange));
    }
    // add proposal
    addUnitCorrectionProposal("Exchange operands", CorrectionImage.IMG_CORRECTION_CHANGE);
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
  private void addUnitCorrectionProposal(String name, CorrectionImage image) {
    CorrectionProposal proposal = new CorrectionProposal(image, name, proposalRelevance);
    SourceChange change = new SourceChange(source);
    for (Edit edit : textEdits) {
      change.addEdit(edit);
    }
    proposal.addChange(change);
    proposals.add(proposal);
  }

  /**
   * @return the part of {@link #source} content.
   */
  private String getSource(final int offset, final int length) throws Exception {
    final AtomicReference<String> result = new AtomicReference<String>();
    source.getContents(new Source.ContentReceiver() {
      @Override
      public void accept(CharBuffer contents) {
        result.set(contents.subSequence(offset, offset + length).toString());
      }

      @Override
      public void accept(String contents) {
        result.set(contents.substring(offset, offset + length));
      }
    });
    return result.get();
  }

//  /**
//   * @return the part of {@link #source} content.
//   */
//  private String getSource(ASTNode node) throws Exception {
//    return getSource(node.getOffset(), node.getLength());
//  }

  /**
   * @return the part of {@link #source} content.
   */
  private String getSource(SourceRange range) throws Exception {
    return getSource(range.getOffset(), range.getLength());
  }

  // TODO(scheglov) add more reset operations
  private void resetProposalElements() {
    textEdits.clear();
    proposalRelevance = DEFAULT_RELEVANCE;
//    linkedPositions.clear();
//    positionStopEdits.clear();
//    linkedPositionProposals.clear();
//    proposal = null;
//    proposalEndRange = null;
  }
}
