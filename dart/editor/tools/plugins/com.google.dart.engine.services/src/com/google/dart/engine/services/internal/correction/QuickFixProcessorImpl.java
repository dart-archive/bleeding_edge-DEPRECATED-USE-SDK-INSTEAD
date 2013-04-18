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
import com.google.common.collect.Maps;
import com.google.dart.engine.formatter.edit.Edit;
import com.google.dart.engine.parser.ParserErrorCode;
import com.google.dart.engine.services.assist.AssistContext;
import com.google.dart.engine.services.change.SourceChange;
import com.google.dart.engine.services.correction.CorrectionImage;
import com.google.dart.engine.services.correction.CorrectionProposal;
import com.google.dart.engine.services.correction.ProblemLocation;
import com.google.dart.engine.services.correction.QuickFixProcessor;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.utilities.instrumentation.Instrumentation;
import com.google.dart.engine.utilities.instrumentation.InstrumentationBuilder;
import com.google.dart.engine.utilities.source.SourceRange;

import java.util.List;
import java.util.Map;

/**
 * Implementation of {@link QuickFixProcessor}.
 */
public class QuickFixProcessorImpl implements QuickFixProcessor {
  private static final CorrectionProposal[] NO_PROPOSALS = {};
  private static final int DEFAULT_RELEVANCE = 50;

  private final List<CorrectionProposal> proposals = Lists.newArrayList();
  private final List<Edit> textEdits = Lists.newArrayList();
  private ProblemLocation problem;
  private Source source;
//  private CompilationUnit unit;
//  private ASTNode node;
  private int selectionOffset;
  private int selectionLength;
  private CorrectionUtils utils;

  private int proposalRelevance = DEFAULT_RELEVANCE;
  private final Map<SourceRange, Edit> positionStopEdits = Maps.newHashMap();
  private final Map<String, List<SourceRange>> linkedPositions = Maps.newHashMap();
  private final Map<String, List<LinkedPositionProposal>> linkedPositionProposals = Maps.newHashMap();

//  private SourceRange proposalEndRange = null;

  @Override
  public CorrectionProposal[] computeProposals(AssistContext context, ProblemLocation problem)
      throws Exception {
    if (context == null) {
      return NO_PROPOSALS;
    }
    this.problem = problem;
    proposals.clear();
    source = context.getSource();
//    unit = context.getCompilationUnit();
//    node = context.getCoveringNode();
    selectionOffset = context.getSelectionOffset();
    selectionLength = context.getSelectionLength();
    utils = new CorrectionUtils(context.getCompilationUnit());
    //
    final InstrumentationBuilder instrumentation = Instrumentation.builder(this.getClass());
    try {
      if (problem.getErrorCode() == ParserErrorCode.EXPECTED_TOKEN) {
        addFix_insertSemicolon();
      }
      instrumentation.metric("QuickFix-Offset", selectionOffset);
      instrumentation.metric("QuickFix-Length", selectionLength);
      instrumentation.metric("QuickFix-ProposalCount", proposals.size());
      instrumentation.data("QuickFix-Source", utils.getText());
      for (int index = 0; index < proposals.size(); index++) {
        instrumentation.data("QuickFix-Proposal-" + index, proposals.get(index).getName());
      }
      return proposals.toArray(new CorrectionProposal[proposals.size()]);
    } finally {
      instrumentation.log();
    }
  }

  @Override
  public boolean hasFix(ProblemLocation problem) {
    return problem.getErrorCode() == ParserErrorCode.EXPECTED_TOKEN;
  }

  private void addFix_insertSemicolon() {
    if (!problem.getMessage().contains("';'")) {
      return;
    }
    int insertOffset = problem.getOffset() + problem.getLength();
    addInsertEdit(insertOffset, ";");
    addUnitCorrectionProposal("Insert ';'");
  }

  private void addInsertEdit(int offset, String text) {
    textEdits.add(createInsertEdit(offset, text));
  }

  /**
   * Adds {@link CorrectionProposal} with single {@link SourceChange} to {@link #proposals}.
   */
  private void addUnitCorrectionProposal(String name) {
    addUnitCorrectionProposal(name, CorrectionImage.IMG_CORRECTION_CHANGE);
  }

  /**
   * Adds {@link CorrectionProposal} with single {@link SourceChange} to {@link #proposals}.
   */
  private void addUnitCorrectionProposal(String name, CorrectionImage image) {
    if (!textEdits.isEmpty()) {
      CorrectionProposal proposal = new CorrectionProposal(image, name, proposalRelevance);
      proposal.setLinkedPositions(linkedPositions);
      proposal.setLinkedPositionProposals(linkedPositionProposals);
      // add change
      SourceChange change = new SourceChange(name, source);
      for (Edit edit : textEdits) {
        change.addEdit(edit);
      }
      proposal.addChange(change);
      // done
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
    proposalRelevance = DEFAULT_RELEVANCE;
    linkedPositions.clear();
    positionStopEdits.clear();
    linkedPositionProposals.clear();
//    proposalEndRange = null;
  }
}
