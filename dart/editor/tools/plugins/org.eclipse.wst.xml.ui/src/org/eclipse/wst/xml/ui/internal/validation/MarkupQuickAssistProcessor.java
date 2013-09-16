/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.validation;

import java.util.ArrayList;

import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.quickassist.IQuickAssistInvocationContext;
import org.eclipse.jface.text.quickassist.IQuickAssistProcessor;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.wst.xml.core.internal.validation.ProblemIDsXML;
import org.eclipse.wst.xml.ui.internal.XMLUIMessages;
import org.eclipse.wst.xml.ui.internal.correction.RemoveUnknownElementQuickFixProposal;
import org.eclipse.wst.xml.ui.internal.correction.RenameInFileQuickAssistProposal;
import org.eclipse.wst.xml.ui.internal.editor.XMLEditorPluginImageHelper;
import org.eclipse.wst.xml.ui.internal.editor.XMLEditorPluginImages;

/**
 * Quick assist processor for problems found by the markup validator
 */
class MarkupQuickAssistProcessor implements IQuickAssistProcessor {
  private int fProblemId;
  private Object fAdditionalFixInfo = null;

  public boolean canAssist(IQuickAssistInvocationContext invocationContext) {
    return false;
  }

  public boolean canFix(Annotation annotation) {
    boolean result = false;

    switch (fProblemId) {
      case ProblemIDsXML.EmptyTag:
      case ProblemIDsXML.MissingEndTag:
      case ProblemIDsXML.AttrsInEndTag:
      case ProblemIDsXML.MissingAttrValue:
      case ProblemIDsXML.NoAttrValue:
      case ProblemIDsXML.SpacesBeforeTagName:
      case ProblemIDsXML.SpacesBeforePI:
      case ProblemIDsXML.NamespaceInPI:
      case ProblemIDsXML.UnknownElement:
      case ProblemIDsXML.UnknownAttr:
      case ProblemIDsXML.InvalidAttrValue:
      case ProblemIDsXML.MissingRequiredAttr:
      case ProblemIDsXML.AttrValueNotQuoted:
      case ProblemIDsXML.MissingClosingBracket:
        result = true;
    }

    return result;
  }

  public ICompletionProposal[] computeQuickAssistProposals(
      IQuickAssistInvocationContext invocationContext) {
    ArrayList proposals = new ArrayList();

    switch (fProblemId) {
      case ProblemIDsXML.EmptyTag:
        proposals.add(new CompletionProposal(
            "", invocationContext.getOffset(), invocationContext.getLength(), 0, getImage(), XMLUIMessages.QuickFixProcessorXML_0, null, "")); //$NON-NLS-1$ //$NON-NLS-2$ 
        break;
      case ProblemIDsXML.MissingEndTag:
        String tagName = (String) ((Object[]) fAdditionalFixInfo)[0];
        String tagClose = (String) ((Object[]) fAdditionalFixInfo)[1];
        int tagCloseOffset = ((Integer) ((Object[]) fAdditionalFixInfo)[2]).intValue();
        int startTagEndOffset = ((Integer) ((Object[]) fAdditionalFixInfo)[3]).intValue();
        int firstChildStartOffset = ((Integer) ((Object[]) fAdditionalFixInfo)[4]).intValue();
        int endOffset = ((Integer) ((Object[]) fAdditionalFixInfo)[5]).intValue();
        proposals.add(new CompletionProposal(tagClose, tagCloseOffset, 0, 0, getImage(),
            XMLUIMessages.QuickFixProcessorXML_1, null, "")); //$NON-NLS-1$ 
        proposals.add(new CompletionProposal(
            "", invocationContext.getOffset(), startTagEndOffset - invocationContext.getOffset(), 0, getImage(), XMLUIMessages.QuickFixProcessorXML_2, null, "")); //$NON-NLS-1$ //$NON-NLS-2$ 
        proposals.add(new CompletionProposal(
            "</" + tagName + ">", firstChildStartOffset, 0, 0, getImage(), XMLUIMessages.QuickFixProcessorXML_3, null, "")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ 
        proposals.add(new CompletionProposal(
            "</" + tagName + ">", endOffset, 0, 0, getImage(), XMLUIMessages.QuickFixProcessorXML_4, null, "")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ 
        break;
      case ProblemIDsXML.AttrsInEndTag:
        proposals.add(new CompletionProposal(
            "", invocationContext.getOffset(), invocationContext.getLength(), 0, getImage(), XMLUIMessages.QuickFixProcessorXML_5, null, "")); //$NON-NLS-1$ //$NON-NLS-2$ 
        break;
      case ProblemIDsXML.MissingAttrValue:
        String defaultAttrValue = (String) ((Object[]) fAdditionalFixInfo)[0];
        int insertOffset = ((Integer) ((Object[]) fAdditionalFixInfo)[1]).intValue();
        proposals.add(new CompletionProposal(
            "\"" + defaultAttrValue + "\"", invocationContext.getOffset() + invocationContext.getLength() + insertOffset, 0, defaultAttrValue.length() + 2, getImage(), XMLUIMessages.QuickFixProcessorXML_6, null, "")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ 
        proposals.add(new CompletionProposal(
            "", invocationContext.getOffset(), invocationContext.getLength(), 0, getImage(), XMLUIMessages.QuickFixProcessorXML_7, null, "")); //$NON-NLS-1$ //$NON-NLS-2$ 
        break;
      case ProblemIDsXML.NoAttrValue:
        defaultAttrValue = (String) fAdditionalFixInfo;
        proposals.add(new CompletionProposal(
            "=\"" + defaultAttrValue + "\"", invocationContext.getOffset() + invocationContext.getLength(), 0, defaultAttrValue.length() + 3, getImage(), XMLUIMessages.QuickFixProcessorXML_6, null, "")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ 
        proposals.add(new CompletionProposal(
            "", invocationContext.getOffset(), invocationContext.getLength(), 0, getImage(), XMLUIMessages.QuickFixProcessorXML_7, null, "")); //$NON-NLS-1$ //$NON-NLS-2$ 
        break;
      case ProblemIDsXML.SpacesBeforeTagName:
        proposals.add(new CompletionProposal(
            "", invocationContext.getOffset(), invocationContext.getLength(), 0, getImage(), XMLUIMessages.QuickFixProcessorXML_8, null, "")); //$NON-NLS-1$ //$NON-NLS-2$ 
        break;
      case ProblemIDsXML.SpacesBeforePI:
        proposals.add(new CompletionProposal(
            "", invocationContext.getOffset(), invocationContext.getLength(), 0, getImage(), XMLUIMessages.QuickFixProcessorXML_9, null, "")); //$NON-NLS-1$ //$NON-NLS-2$ 
        break;
      case ProblemIDsXML.NamespaceInPI:
        proposals.add(new CompletionProposal(
            "", invocationContext.getOffset(), invocationContext.getLength(), 0, getImage(), XMLUIMessages.QuickFixProcessorXML_10, null, "")); //$NON-NLS-1$ //$NON-NLS-2$ 
        break;
      case ProblemIDsXML.UnknownElement:
        proposals.add(new RemoveUnknownElementQuickFixProposal(fAdditionalFixInfo, getImage(),
            XMLUIMessages.QuickFixProcessorXML_11));
        proposals.add(new RenameInFileQuickAssistProposal());
        break;
      case ProblemIDsXML.UnknownAttr:
        proposals.add(new CompletionProposal(
            "", invocationContext.getOffset(), invocationContext.getLength(), 0, getImage(), XMLUIMessages.QuickFixProcessorXML_7, null, "")); //$NON-NLS-1$ //$NON-NLS-2$ 
        proposals.add(new RenameInFileQuickAssistProposal());
        break;
      case ProblemIDsXML.InvalidAttrValue:
        proposals.add(new CompletionProposal(
            "", invocationContext.getOffset(), invocationContext.getLength(), 0, getImage(), XMLUIMessages.QuickFixProcessorXML_12, null, "")); //$NON-NLS-1$ //$NON-NLS-2$ 
        break;
      case ProblemIDsXML.MissingRequiredAttr:
        String requiredAttr = (String) ((Object[]) fAdditionalFixInfo)[0];
        insertOffset = ((Integer) ((Object[]) fAdditionalFixInfo)[1]).intValue();
        proposals.add(new CompletionProposal(requiredAttr, invocationContext.getOffset()
            + insertOffset, 0, requiredAttr.length(), getImage(),
            XMLUIMessages.QuickFixProcessorXML_13, null, "")); //$NON-NLS-1$ 
        break;
      case ProblemIDsXML.AttrValueNotQuoted:
        String attrValue = (String) fAdditionalFixInfo;
        proposals.add(new CompletionProposal(
            "\"" + attrValue + "\"", invocationContext.getOffset(), invocationContext.getLength(), attrValue.length() + 2, getImage(), XMLUIMessages.QuickFixProcessorXML_14, null, "")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ 
        break;
      case ProblemIDsXML.MissingClosingBracket:
        proposals.add(new CompletionProposal(
            ">", invocationContext.getOffset() + invocationContext.getLength(), 0, 1, getImage(), XMLUIMessages.QuickFixProcessorXML_15, null, "")); //$NON-NLS-1$ //$NON-NLS-2$ 
        break;
    }
    return (ICompletionProposal[]) proposals.toArray(new ICompletionProposal[proposals.size()]);
  }

  public String getErrorMessage() {
    return null;
  }

  private Image getImage() {
    return XMLEditorPluginImageHelper.getInstance().getImage(
        XMLEditorPluginImages.IMG_OBJ_CORRECTION_CHANGE);
  }

  public void setProblemId(int problemId) {
    fProblemId = problemId;
  }

  public void setAdditionalFixInfo(Object fixInfo) {
    fAdditionalFixInfo = fixInfo;
  }
}
