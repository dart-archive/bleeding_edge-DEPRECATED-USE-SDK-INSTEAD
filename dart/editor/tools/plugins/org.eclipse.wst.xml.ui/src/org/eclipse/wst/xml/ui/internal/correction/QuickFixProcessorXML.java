/*******************************************************************************
 * Copyright (c) 2001, 2010 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.correction;

import java.util.ArrayList;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.wst.sse.ui.internal.correction.IQuickFixProcessor;
import org.eclipse.wst.sse.ui.internal.reconcile.TemporaryAnnotation;
import org.eclipse.wst.xml.core.internal.validation.ProblemIDsXML;
import org.eclipse.wst.xml.ui.internal.XMLUIMessages;
import org.eclipse.wst.xml.ui.internal.editor.XMLEditorPluginImageHelper;
import org.eclipse.wst.xml.ui.internal.editor.XMLEditorPluginImages;

/**
 * @deprecated since 2.0 RC0 Use org.eclipse.jface.text.quickassist.IQuickAssistProcessor;
 */
public class QuickFixProcessorXML implements IQuickFixProcessor {

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.wst.sse.ui.correction.IQuickFixProcessor#canFix(int)
   */
  public boolean canFix(Annotation annotation) {
    boolean result = false;

    if (annotation instanceof TemporaryAnnotation) {
      TemporaryAnnotation tempAnnotation = (TemporaryAnnotation) annotation;
      int problemID = tempAnnotation.getProblemID();
      switch (problemID) {
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
    }

    return result;
  }

  public Image getImage() {
    // return
    // JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
    return XMLEditorPluginImageHelper.getInstance().getImage(
        XMLEditorPluginImages.IMG_OBJ_CORRECTION_CHANGE);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.wst.sse.ui.correction.IQuickFixProcessor#getProposals(org.eclipse.wst.sse.ui.internal
   * .reconcile.TemporaryAnnotation)
   */
  public ICompletionProposal[] getProposals(Annotation annotation) throws CoreException {
    ArrayList proposals = new ArrayList();

    if (annotation instanceof TemporaryAnnotation) {
      TemporaryAnnotation tempAnnotation = (TemporaryAnnotation) annotation;
      int problemID = tempAnnotation.getProblemID();
      switch (problemID) {
        case ProblemIDsXML.EmptyTag:
          proposals.add(new CompletionProposal(
              "", tempAnnotation.getPosition().getOffset(), tempAnnotation.getPosition().getLength(), 0, getImage(), XMLUIMessages.QuickFixProcessorXML_0, null, "")); //$NON-NLS-1$ //$NON-NLS-2$ 
          break;
        case ProblemIDsXML.MissingEndTag:
          String tagName = (String) ((Object[]) tempAnnotation.getAdditionalFixInfo())[0];
          String tagClose = (String) ((Object[]) tempAnnotation.getAdditionalFixInfo())[1];
          int tagCloseOffset = ((Integer) ((Object[]) tempAnnotation.getAdditionalFixInfo())[2]).intValue();
          int startTagEndOffset = ((Integer) ((Object[]) tempAnnotation.getAdditionalFixInfo())[3]).intValue();
          int firstChildStartOffset = ((Integer) ((Object[]) tempAnnotation.getAdditionalFixInfo())[4]).intValue();
          int endOffset = ((Integer) ((Object[]) tempAnnotation.getAdditionalFixInfo())[5]).intValue();
          proposals.add(new CompletionProposal(tagClose, tagCloseOffset, 0, 0, getImage(),
              XMLUIMessages.QuickFixProcessorXML_1, null, "")); //$NON-NLS-1$ 
          proposals.add(new CompletionProposal(
              "", tempAnnotation.getPosition().getOffset(), startTagEndOffset - tempAnnotation.getPosition().getOffset(), 0, getImage(), XMLUIMessages.QuickFixProcessorXML_2, null, "")); //$NON-NLS-1$ //$NON-NLS-2$ 
          proposals.add(new CompletionProposal(
              "</" + tagName + ">", firstChildStartOffset, 0, 0, getImage(), XMLUIMessages.QuickFixProcessorXML_3, null, "")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ 
          proposals.add(new CompletionProposal(
              "</" + tagName + ">", endOffset, 0, 0, getImage(), XMLUIMessages.QuickFixProcessorXML_4, null, "")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ 
          break;
        case ProblemIDsXML.AttrsInEndTag:
          proposals.add(new CompletionProposal(
              "", tempAnnotation.getPosition().getOffset(), tempAnnotation.getPosition().getLength(), 0, getImage(), XMLUIMessages.QuickFixProcessorXML_5, null, "")); //$NON-NLS-1$ //$NON-NLS-2$ 
          break;
        case ProblemIDsXML.MissingAttrValue:
          String defaultAttrValue = (String) ((Object[]) tempAnnotation.getAdditionalFixInfo())[0];
          int insertOffset = ((Integer) ((Object[]) tempAnnotation.getAdditionalFixInfo())[1]).intValue();
          proposals.add(new CompletionProposal(
              "\"" + defaultAttrValue + "\"", tempAnnotation.getPosition().getOffset() + tempAnnotation.getPosition().getLength() + insertOffset, 0, defaultAttrValue.length() + 2, getImage(), XMLUIMessages.QuickFixProcessorXML_6, null, "")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ 
          proposals.add(new CompletionProposal(
              "", tempAnnotation.getPosition().getOffset(), tempAnnotation.getPosition().getLength(), 0, getImage(), XMLUIMessages.QuickFixProcessorXML_7, null, "")); //$NON-NLS-1$ //$NON-NLS-2$ 
          break;
        case ProblemIDsXML.NoAttrValue:
          defaultAttrValue = (String) tempAnnotation.getAdditionalFixInfo();
          proposals.add(new CompletionProposal(
              "=\"" + defaultAttrValue + "\"", tempAnnotation.getPosition().getOffset() + tempAnnotation.getPosition().getLength(), 0, defaultAttrValue.length() + 3, getImage(), XMLUIMessages.QuickFixProcessorXML_6, null, "")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ 
          proposals.add(new CompletionProposal(
              "", tempAnnotation.getPosition().getOffset(), tempAnnotation.getPosition().getLength(), 0, getImage(), XMLUIMessages.QuickFixProcessorXML_7, null, "")); //$NON-NLS-1$ //$NON-NLS-2$ 
          break;
        case ProblemIDsXML.SpacesBeforeTagName:
          proposals.add(new CompletionProposal(
              "", tempAnnotation.getPosition().getOffset(), tempAnnotation.getPosition().getLength(), 0, getImage(), XMLUIMessages.QuickFixProcessorXML_8, null, "")); //$NON-NLS-1$ //$NON-NLS-2$ 
          break;
        case ProblemIDsXML.SpacesBeforePI:
          proposals.add(new CompletionProposal(
              "", tempAnnotation.getPosition().getOffset(), tempAnnotation.getPosition().getLength(), 0, getImage(), XMLUIMessages.QuickFixProcessorXML_9, null, "")); //$NON-NLS-1$ //$NON-NLS-2$ 
          break;
        case ProblemIDsXML.NamespaceInPI:
          proposals.add(new CompletionProposal(
              "", tempAnnotation.getPosition().getOffset(), tempAnnotation.getPosition().getLength(), 0, getImage(), XMLUIMessages.QuickFixProcessorXML_10, null, "")); //$NON-NLS-1$ //$NON-NLS-2$ 
          break;
        case ProblemIDsXML.UnknownElement:
          proposals.add(new RemoveUnknownElementQuickFixProposal(
              tempAnnotation.getAdditionalFixInfo(), getImage(),
              XMLUIMessages.QuickFixProcessorXML_11));
          proposals.add(new RenameInFileQuickAssistProposal());
          break;
        case ProblemIDsXML.UnknownAttr:
          proposals.add(new CompletionProposal(
              "", tempAnnotation.getPosition().getOffset(), tempAnnotation.getPosition().getLength(), 0, getImage(), XMLUIMessages.QuickFixProcessorXML_7, null, "")); //$NON-NLS-1$ //$NON-NLS-2$ 
          proposals.add(new RenameInFileQuickAssistProposal());
          break;
        case ProblemIDsXML.InvalidAttrValue:
          proposals.add(new CompletionProposal(
              "", tempAnnotation.getPosition().getOffset(), tempAnnotation.getPosition().getLength(), 0, getImage(), XMLUIMessages.QuickFixProcessorXML_12, null, "")); //$NON-NLS-1$ //$NON-NLS-2$ 
          break;
        case ProblemIDsXML.MissingRequiredAttr:
          String requiredAttr = (String) ((Object[]) tempAnnotation.getAdditionalFixInfo())[0];
          insertOffset = ((Integer) ((Object[]) tempAnnotation.getAdditionalFixInfo())[1]).intValue();
          proposals.add(new CompletionProposal(requiredAttr,
              tempAnnotation.getPosition().getOffset() + insertOffset, 0, requiredAttr.length(),
              getImage(), XMLUIMessages.QuickFixProcessorXML_13, null, "")); //$NON-NLS-1$ 
          break;
        case ProblemIDsXML.AttrValueNotQuoted:
          String attrValue = (String) tempAnnotation.getAdditionalFixInfo();
          proposals.add(new CompletionProposal(
              "\"" + attrValue + "\"", tempAnnotation.getPosition().getOffset(), tempAnnotation.getPosition().getLength(), attrValue.length() + 2, getImage(), XMLUIMessages.QuickFixProcessorXML_14, null, "")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ 
          break;
        case ProblemIDsXML.MissingClosingBracket:
          proposals.add(new CompletionProposal(
              ">", tempAnnotation.getPosition().getOffset() + tempAnnotation.getPosition().getLength(), 0, 1, getImage(), XMLUIMessages.QuickFixProcessorXML_15, null, "")); //$NON-NLS-1$ //$NON-NLS-2$ 
          break;
      }
    }

    return (ICompletionProposal[]) proposals.toArray(new ICompletionProposal[proposals.size()]);
  }
}
