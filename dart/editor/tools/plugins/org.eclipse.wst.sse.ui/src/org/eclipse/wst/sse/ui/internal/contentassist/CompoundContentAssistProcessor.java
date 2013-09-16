/*******************************************************************************
 * Copyright (c) 2005, 2013 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/

package org.eclipse.wst.sse.ui.internal.contentassist;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.contentassist.IContentAssistSubjectControl;
import org.eclipse.jface.contentassist.ISubjectControlContentAssistProcessor;
import org.eclipse.jface.contentassist.ISubjectControlContextInformationPresenter;
import org.eclipse.jface.contentassist.ISubjectControlContextInformationValidator;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationExtension;
import org.eclipse.jface.text.contentassist.IContextInformationPresenter;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.swt.graphics.Image;
import org.eclipse.wst.sse.ui.contentassist.StructuredContentAssistProcessor;
import org.eclipse.wst.sse.ui.internal.IReleasable;
import org.eclipse.wst.sse.ui.internal.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * <p>
 * A processor that aggregates the proposals of multiple other processors. When proposals are
 * requested, the contained processors are queried in the order they were added to the compound
 * object. Copied from org.eclipse.jdt.internal.ui.text.CompoundContentAssistProcessor. Modification
 * was made to add a dispose() method.
 * </p>
 */
public class CompoundContentAssistProcessor implements IContentAssistProcessor,
    ISubjectControlContentAssistProcessor, IReleasable {

  /** the compound processors */
  private final Set fProcessors = new LinkedHashSet();

  /** Aggregated error message from the compound processors */
  private String fErrorMessage;

  /**
   * Creates a new instance.
   */
  public CompoundContentAssistProcessor() {
  }

  /**
   * Creates a new instance with one child processor.
   * 
   * @param processor the processor to add
   */
  public CompoundContentAssistProcessor(IContentAssistProcessor processor) {
    add(processor);
  }

  /**
   * Adds a processor to this compound processor.
   * 
   * @param processor the processor to add
   */
  public void add(IContentAssistProcessor processor) {
    Assert.isNotNull(processor);
    fProcessors.add(processor);
  }

  /**
   * @param processor check to see if this {@link IContentAssistProcessor} is one in this compound
   *          processor.
   * @return <code>true</code> if this compound processor contains the given processor,
   *         <code>false</code> otherwise
   */
  public boolean containsProcessor(IContentAssistProcessor processor) {
    return fProcessors.contains(processor);
  }

  /**
   * Removes a processor from this compound processor.
   * 
   * @param processor the processor to remove
   */
  public void remove(IContentAssistProcessor processor) {
    fProcessors.remove(processor);
  }

  /**
   * Creates a new instance and adds all specified processors.
   * 
   * @param processors
   */
  public CompoundContentAssistProcessor(IContentAssistProcessor[] processors) {
    for (int i = 0; i < processors.length; i++) {
      add(processors[i]);
    }
  }

  /*
   * @see
   * org.eclipse.jface.text.contentassist.IContentAssistProcessor#computeCompletionProposals(org
   * .eclipse.jface.text.ITextViewer, int)
   */
  public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int documentOffset) {
    fErrorMessage = null;
    List ret = new LinkedList();
    for (Iterator it = fProcessors.iterator(); it.hasNext();) {
      IContentAssistProcessor p = (IContentAssistProcessor) it.next();
      try {
        // isolate calls to each processor
        ICompletionProposal[] proposals = p.computeCompletionProposals(viewer, documentOffset);
        if (proposals != null && proposals.length > 0) {
          ret.addAll(Arrays.asList(proposals));
          fErrorMessage = null; // Hide previous errors
        } else {
          if (fErrorMessage == null && ret.isEmpty()) {
            String errorMessage = p.getErrorMessage();
            if (errorMessage != null) {
              fErrorMessage = errorMessage;
            }
          }
        }
      } catch (Exception e) {
        Logger.logException(e);
      }
    }
    return (ICompletionProposal[]) ret.toArray(new ICompletionProposal[ret.size()]);
  }

  /**
   * {@inheritDoc}
   * <p>
   * The returned objects are wrapper objects around the real information containers.
   * </p>
   * 
   * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#computeContextInformation(org.eclipse.jface.text.ITextViewer,
   *      int)
   */
  public IContextInformation[] computeContextInformation(ITextViewer viewer, int documentOffset) {
    fErrorMessage = null;
    List ret = new LinkedList();
    for (Iterator it = fProcessors.iterator(); it.hasNext();) {
      IContentAssistProcessor p = (IContentAssistProcessor) it.next();
      IContextInformation[] informations = p.computeContextInformation(viewer, documentOffset);
      if (informations != null && informations.length > 0) {
        for (int i = 0; i < informations.length; i++)
          ret.add(new WrappedContextInformation(informations[i], p));
        fErrorMessage = null; // Hide previous errors
      } else {
        if (fErrorMessage == null && ret.isEmpty()) {
          String errorMessage = p.getErrorMessage();
          if (errorMessage != null) {
            fErrorMessage = errorMessage;
          }
        }
      }
    }
    return (IContextInformation[]) ret.toArray(new IContextInformation[ret.size()]);
  }

  /*
   * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#
   * getCompletionProposalAutoActivationCharacters()
   */
  public char[] getCompletionProposalAutoActivationCharacters() {
    Set ret = new LinkedHashSet();
    for (Iterator it = fProcessors.iterator(); it.hasNext();) {
      IContentAssistProcessor p = (IContentAssistProcessor) it.next();
      char[] chars = p.getCompletionProposalAutoActivationCharacters();
      if (chars != null)
        for (int i = 0; i < chars.length; i++)
          ret.add(new Character(chars[i]));
    }

    char[] chars = new char[ret.size()];
    int i = 0;
    for (Iterator it = ret.iterator(); it.hasNext(); i++) {
      Character ch = (Character) it.next();
      chars[i] = ch.charValue();
    }
    return chars;
  }

  /*
   * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#
   * getContextInformationAutoActivationCharacters()
   */
  public char[] getContextInformationAutoActivationCharacters() {
    Set ret = new LinkedHashSet();
    for (Iterator it = fProcessors.iterator(); it.hasNext();) {
      IContentAssistProcessor p = (IContentAssistProcessor) it.next();
      char[] chars = p.getContextInformationAutoActivationCharacters();
      if (chars != null)
        for (int i = 0; i < chars.length; i++)
          ret.add(new Character(chars[i]));
    }

    char[] chars = new char[ret.size()];
    int i = 0;
    for (Iterator it = ret.iterator(); it.hasNext(); i++) {
      Character ch = (Character) it.next();
      chars[i] = ch.charValue();
    }
    return chars;
  }

  /**
   * Returns the error message of one of contained processor if any, or <code>null</code> if no
   * processor has an error message.
   * 
   * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getErrorMessage()
   * @return {@inheritDoc}
   */
  public String getErrorMessage() {
    return fErrorMessage;
  }

  /**
   * {@inheritDoc}
   * <p>
   * The returned validator is a wrapper around the validators provided by the child processors.
   * </p>
   * 
   * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getContextInformationValidator()
   */
  public IContextInformationValidator getContextInformationValidator() {
    boolean hasValidator = false;
    boolean hasPresenter = false;
    boolean hasExtension = false;

    Iterator itp = fProcessors.iterator();
    while (itp.hasNext() && (!hasPresenter || !hasExtension)) {
      IContentAssistProcessor p = (IContentAssistProcessor) itp.next();
      IContextInformationValidator v = p.getContextInformationValidator();
      if (v != null) {
        hasValidator = true;
        if (v instanceof IContextInformationPresenter) {
          hasPresenter = true;
        }
        if (v instanceof ISubjectControlContextInformationPresenter
            || v instanceof ISubjectControlContextInformationValidator) {
          hasExtension = true;
        }
      }
    }

    CompoundContentAssistValidator validator = null;
    if (hasPresenter && hasExtension)
      validator = new CompoundContentAssistValidatorPresenterEx();
    else if (hasPresenter)
      validator = new CompoundContentAssistValidatorPresenter();
    else if (hasExtension)
      validator = new CompoundContentAssistValidatorEx();
    else if (hasValidator)
      validator = new CompoundContentAssistValidator();

    if (validator != null)
      for (Iterator it = fProcessors.iterator(); it.hasNext();) {
        IContentAssistProcessor p = (IContentAssistProcessor) it.next();
        IContextInformationValidator v = p.getContextInformationValidator();
        if (v != null)
          validator.add(v);
      }

    return validator;
  }

  /*
   * @see
   * ISubjectControlContentAssistProcessor#computeCompletionProposals(IContentAssistSubjectControl,
   * int)
   */
  public ICompletionProposal[] computeCompletionProposals(
      IContentAssistSubjectControl contentAssistSubjectControl, int documentOffset) {
    fErrorMessage = null;
    List ret = new LinkedList();
    for (Iterator it = fProcessors.iterator(); it.hasNext();) {
      Object o = it.next();
      if (o instanceof ISubjectControlContentAssistProcessor) {
        ISubjectControlContentAssistProcessor p = (ISubjectControlContentAssistProcessor) o;
        ICompletionProposal[] proposals = p.computeCompletionProposals(contentAssistSubjectControl,
            documentOffset);
        if (proposals != null && proposals.length > 0) {
          ret.addAll(Arrays.asList(proposals));
          fErrorMessage = null; // Hide previous errors
        } else {
          if (fErrorMessage == null && ret.isEmpty()) {
            String errorMessage = p.getErrorMessage();
            if (errorMessage != null) {
              fErrorMessage = errorMessage;
            }
          }
        }
      }
    }

    return (ICompletionProposal[]) ret.toArray(new ICompletionProposal[ret.size()]);
  }

  /**
   * {@inheritDoc}
   * <p>
   * The returned objects are wrapper objects around the real information containers.
   * </p>
   * 
   * @see org.eclipse.jface.contentassist.ISubjectControlContentAssistProcessor#computeContextInformation(org.eclipse.jface.text.contentassist.IContentAssistSubject,
   *      int)
   */
  public IContextInformation[] computeContextInformation(
      IContentAssistSubjectControl contentAssistSubjectControl, int documentOffset) {
    fErrorMessage = null;
    List ret = new LinkedList();
    for (Iterator it = fProcessors.iterator(); it.hasNext();) {
      Object o = it.next();
      if (o instanceof ISubjectControlContentAssistProcessor) {
        ISubjectControlContentAssistProcessor p = (ISubjectControlContentAssistProcessor) o;
        IContextInformation[] informations = p.computeContextInformation(
            contentAssistSubjectControl, documentOffset);
        if (informations != null && informations.length > 0) {
          for (int i = 0; i < informations.length; i++)
            ret.add(new WrappedContextInformation(informations[i], p));
          fErrorMessage = null; // Hide previous errors
        } else {
          if (fErrorMessage == null && ret.isEmpty()) {
            String errorMessage = p.getErrorMessage();
            if (errorMessage != null) {
              fErrorMessage = errorMessage;
            }
          }
        }
      }
    }
    return (IContextInformation[]) ret.toArray(new IContextInformation[ret.size()]);
  }

  /**
   * Dispose of any content assist processors that need disposing
   * 
   * @deprecated use {@link #release()}
   */
  public void dispose() {
    this.release();
  }

  public void install(ITextViewer viewer) {
    for (Iterator it = fProcessors.iterator(); it.hasNext();) {
      IContentAssistProcessor p = (IContentAssistProcessor) it.next();
      if (p instanceof StructuredContentAssistProcessor) {
        ((StructuredContentAssistProcessor) p).install(viewer);
      }
    }
  }

  /**
   * @see org.eclipse.wst.sse.ui.internal.IReleasable#release()
   */
  public void release() {
    // go through list of content assist processors and dispose
    for (Iterator it = fProcessors.iterator(); it.hasNext();) {
      IContentAssistProcessor p = (IContentAssistProcessor) it.next();
      if (p instanceof IReleasable) {
        ((IReleasable) p).release();
      }
    }
    fProcessors.clear();
  }

  private static class WrappedContextInformation implements IContextInformation,
      IContextInformationExtension {
    private IContextInformation fInfo;
    private IContentAssistProcessor fProcessor;

    WrappedContextInformation(IContextInformation info, IContentAssistProcessor processor) {
      fInfo = info;
      fProcessor = processor;
    }

    /*
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
      return fInfo.equals(obj);
    }

    /*
     * @see org.eclipse.jface.text.contentassist.IContextInformation#getContextDisplayString()
     */
    public String getContextDisplayString() {
      return fInfo.getContextDisplayString();
    }

    /*
     * @see org.eclipse.jface.text.contentassist.IContextInformation#getImage()
     */
    public Image getImage() {
      return fInfo.getImage();
    }

    /*
     * @see org.eclipse.jface.text.contentassist.IContextInformation#getInformationDisplayString()
     */
    public String getInformationDisplayString() {
      return fInfo.getInformationDisplayString();
    }

    /*
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
      return fInfo.hashCode();
    }

    /*
     * @see java.lang.Object#toString()
     */
    public String toString() {
      return fInfo.toString();
    }

    IContentAssistProcessor getProcessor() {
      return fProcessor;
    }

    IContextInformation getContextInformation() {
      return fInfo;
    }

    public int getContextInformationPosition() {
      int position = -1;
      if (fInfo instanceof IContextInformationExtension)
        position = ((IContextInformationExtension) fInfo).getContextInformationPosition();
      return position;
    }
  }

  private static class CompoundContentAssistValidator implements IContextInformationValidator {
    List fValidators = new ArrayList();
    IContextInformationValidator fValidator;

    void add(IContextInformationValidator validator) {
      fValidators.add(validator);
    }

    /*
     * @see
     * org.eclipse.jface.text.contentassist.IContextInformationValidator#install(org.eclipse.jface
     * .text.contentassist.IContextInformation, org.eclipse.jface.text.ITextViewer, int)
     */
    public void install(IContextInformation info, ITextViewer viewer, int documentPosition) {
      // install either the validator in the info, or all validators
      fValidator = getValidator(info);
      IContextInformation realInfo = getContextInformation(info);
      if (fValidator != null)
        fValidator.install(realInfo, viewer, documentPosition);
      else {
        for (Iterator it = fValidators.iterator(); it.hasNext();) {
          IContextInformationValidator v = (IContextInformationValidator) it.next();
          v.install(realInfo, viewer, documentPosition);
        }
      }
    }

    IContextInformationValidator getValidator(IContextInformation info) {
      if (info instanceof WrappedContextInformation) {
        WrappedContextInformation wrap = (WrappedContextInformation) info;
        return wrap.getProcessor().getContextInformationValidator();
      }

      return null;
    }

    IContextInformation getContextInformation(IContextInformation info) {
      IContextInformation realInfo = info;
      if (info instanceof WrappedContextInformation) {
        WrappedContextInformation wrap = (WrappedContextInformation) info;
        realInfo = wrap.getContextInformation();
      }

      return realInfo;
    }

    /*
     * @see
     * org.eclipse.jface.text.contentassist.IContextInformationValidator#isContextInformationValid
     * (int)
     */
    public boolean isContextInformationValid(int documentPosition) {
      // use either the validator in the info, or all validators
      boolean isValid = false;
      if (fValidator != null)
        isValid = fValidator.isContextInformationValid(documentPosition);
      else {
        for (Iterator it = fValidators.iterator(); it.hasNext();) {
          IContextInformationValidator v = (IContextInformationValidator) it.next();
          isValid |= v.isContextInformationValid(documentPosition);
        }
      }
      return isValid;
    }

  }

  private static class CompoundContentAssistValidatorPresenter extends
      CompoundContentAssistValidator implements IContextInformationPresenter {
    public boolean updatePresentation(int offset, TextPresentation presentation) {
      // use either the validator in the info, or all validators
      boolean presentationUpdated = false;
      if (fValidator instanceof IContextInformationPresenter)
        presentationUpdated = ((IContextInformationPresenter) fValidator).updatePresentation(
            offset, presentation);
      else {
        for (Iterator it = fValidators.iterator(); it.hasNext();) {
          IContextInformationValidator v = (IContextInformationValidator) it.next();
          if (v instanceof IContextInformationPresenter)
            presentationUpdated |= ((IContextInformationPresenter) v).updatePresentation(offset,
                presentation);
        }
      }
      return presentationUpdated;
    }
  }

  private static class CompoundContentAssistValidatorEx extends CompoundContentAssistValidator
      implements ISubjectControlContextInformationValidator {
    /*
     * @see ISubjectControlContextInformationValidator#install(IContextInformation,
     * IContentAssistSubjectControl, int)
     */
    public void install(IContextInformation info,
        IContentAssistSubjectControl contentAssistSubjectControl, int documentPosition) {
      // install either the validator in the info, or all validators
      fValidator = getValidator(info);
      IContextInformation realInfo = getContextInformation(info);
      if (fValidator instanceof ISubjectControlContextInformationValidator)
        ((ISubjectControlContextInformationValidator) fValidator).install(realInfo,
            contentAssistSubjectControl, documentPosition);
      else {
        for (Iterator it = fValidators.iterator(); it.hasNext();) {
          if (it.next() instanceof ISubjectControlContextInformationValidator)
            ((ISubjectControlContextInformationValidator) it.next()).install(realInfo,
                contentAssistSubjectControl, documentPosition);
        }
      }
    }

  }

  private static class CompoundContentAssistValidatorPresenterEx extends
      CompoundContentAssistValidatorPresenter implements
      ISubjectControlContextInformationPresenter, ISubjectControlContextInformationValidator {
    /*
     * @see ISubjectControlContextInformationPresenter#install(IContextInformation,
     * IContentAssistSubjectControl, int)
     */
    public void install(IContextInformation info,
        IContentAssistSubjectControl contentAssistSubjectControl, int documentPosition) {
      // install either the validator in the info, or all validators
      fValidator = getValidator(info);
      IContextInformation realInfo = getContextInformation(info);

      if (fValidator instanceof ISubjectControlContextInformationValidator)
        ((ISubjectControlContextInformationValidator) fValidator).install(realInfo,
            contentAssistSubjectControl, documentPosition);
      else {
        for (Iterator it = fValidators.iterator(); it.hasNext();) {
          if (it.next() instanceof ISubjectControlContextInformationValidator)
            ((ISubjectControlContextInformationValidator) it.next()).install(realInfo,
                contentAssistSubjectControl, documentPosition);
        }
      }
    }

  }
}
