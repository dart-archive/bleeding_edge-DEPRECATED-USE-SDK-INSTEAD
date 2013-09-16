/*******************************************************************************
 * Copyright (c) 2001, 2010 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.validation;

import java.util.Locale;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.IResourceProxyVisitor;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.quickassist.IQuickAssistProcessor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocumentRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegionContainer;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegionList;
import org.eclipse.wst.sse.ui.internal.reconcile.AbstractStructuredTextReconcilingStrategy;
import org.eclipse.wst.sse.ui.internal.reconcile.ReconcileAnnotationKey;
import org.eclipse.wst.sse.ui.internal.reconcile.validator.AnnotationInfo;
import org.eclipse.wst.sse.ui.internal.reconcile.validator.ISourceValidator;
import org.eclipse.wst.sse.ui.internal.reconcile.validator.IncrementalReporter;
import org.eclipse.wst.validation.AbstractValidator;
import org.eclipse.wst.validation.ValidationResult;
import org.eclipse.wst.validation.ValidationState;
import org.eclipse.wst.validation.internal.core.Message;
import org.eclipse.wst.validation.internal.core.ValidationException;
import org.eclipse.wst.validation.internal.operations.IWorkbenchContext;
import org.eclipse.wst.validation.internal.operations.LocalizedMessage;
import org.eclipse.wst.validation.internal.provisional.core.IMessage;
import org.eclipse.wst.validation.internal.provisional.core.IReporter;
import org.eclipse.wst.validation.internal.provisional.core.IValidationContext;
import org.eclipse.wst.validation.internal.provisional.core.IValidator;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMNode;
import org.eclipse.wst.xml.core.internal.regions.DOMRegionContext;
import org.eclipse.wst.xml.core.internal.validation.ProblemIDsXML;
import org.eclipse.wst.xml.ui.internal.Logger;
import org.eclipse.wst.xml.ui.internal.XMLUIMessages;
import org.w3c.dom.Node;

/**
 * Basic XML syntax checking step.
 * 
 * @deprecated No longer used as the source validator for XML documents. Instead
 *             {@link DelegatingSourceValidatorForXML} is the source validator, and the markup
 *             validation is done as a step in {@link XMLValidator} by
 *             {@link org.eclipse.wst.xml.core.internal.validation.MarkupValidator}.
 * @see org.eclipse.wst.xml.core.internal.validation.MarkupValidator
 * @see DelegatingSourceValidatorForXML
 * @see XMLValidator
 */
public class MarkupValidator extends AbstractValidator implements IValidator, ISourceValidator {
  private String DQUOTE = "\""; //$NON-NLS-1$
  private String SQUOTE = "'"; //$NON-NLS-1$
  private final String QUICKASSISTPROCESSOR = IQuickAssistProcessor.class.getName();

  private IDocument fDocument;

  private IContentType fRootContentType = null;

  private int SEVERITY_ATTRIBUTE_HAS_NO_VALUE = IMessage.HIGH_SEVERITY;
  private int SEVERITY_END_TAG_WITH_ATTRIBUTES = IMessage.HIGH_SEVERITY;
  private int SEVERITY_INVALID_WHITESPACE_BEFORE_TAGNAME = IMessage.HIGH_SEVERITY;
  private int SEVERITY_MISSING_CLOSING_BRACKET = IMessage.HIGH_SEVERITY;
  private int SEVERITY_MISSING_CLOSING_QUOTE = IMessage.HIGH_SEVERITY;
  private int SEVERITY_MISSING_END_TAG = IMessage.HIGH_SEVERITY;
  private int SEVERITY_MISSING_START_TAG = IMessage.HIGH_SEVERITY;
  private int SEVERITY_MISSING_QUOTES = IMessage.HIGH_SEVERITY;
  private int SEVERITY_NAMESPACE_IN_PI_TARGET = IMessage.HIGH_SEVERITY;
  private int SEVERITY_TAG_NAME_MISSING = IMessage.HIGH_SEVERITY;
  private int SEVERITY_WHITESPACE_AT_START = IMessage.HIGH_SEVERITY;

  private void addAttributeError(String messageText, String attributeValueText, int start,
      int length, int problemId, IStructuredDocumentRegion sdRegion, IReporter reporter,
      int messageSeverity) {

    if (sdRegion.isDeleted()) {
      return;
    }

    int lineNo = getLineNumber(start);
    LocalizedMessage message = new LocalizedMessage(messageSeverity, messageText);
    message.setOffset(start);
    message.setLength(length);
    message.setLineNo(lineNo);

    if (reporter instanceof IncrementalReporter) {
      MarkupQuickAssistProcessor processor = new MarkupQuickAssistProcessor();
      processor.setProblemId(problemId);
      processor.setAdditionalFixInfo(attributeValueText);
      message.setAttribute(QUICKASSISTPROCESSOR, processor);
      AnnotationInfo info = new AnnotationInfo(message);
      ((IncrementalReporter) reporter).addAnnotationInfo(this, info);
    } else {
      reporter.addMessage(this, message);
    }
  }

  private void checkAttributesInEndTag(IStructuredDocumentRegion structuredDocumentRegion,
      IReporter reporter) {

    if (structuredDocumentRegion.isDeleted()) {
      return;
    }

    ITextRegionList textRegions = structuredDocumentRegion.getRegions();
    int errorCount = 0;
    int start = structuredDocumentRegion.getEndOffset();
    int end = structuredDocumentRegion.getEndOffset();
    for (int i = 0; (i < textRegions.size())
        && (errorCount < AbstractStructuredTextReconcilingStrategy.ELEMENT_ERROR_LIMIT)
        && !structuredDocumentRegion.isDeleted(); i++) {
      ITextRegion textRegion = textRegions.get(i);
      if ((textRegion.getType() == DOMRegionContext.XML_TAG_ATTRIBUTE_NAME)
          || (textRegion.getType() == DOMRegionContext.XML_TAG_ATTRIBUTE_EQUALS)
          || (textRegion.getType() == DOMRegionContext.XML_TAG_ATTRIBUTE_VALUE)) {
        if (start > structuredDocumentRegion.getStartOffset(textRegion)) {
          start = structuredDocumentRegion.getStartOffset(textRegion);
        }
        end = structuredDocumentRegion.getEndOffset(textRegion);
        errorCount++;
      }
    }
    // create one error for all attributes in the end tag
    if (errorCount > 0) {
      // Position p = new Position(start, end - start);
      String messageText = XMLUIMessages.End_tag_has_attributes;
      LocalizedMessage message = new LocalizedMessage(SEVERITY_END_TAG_WITH_ATTRIBUTES, messageText);
      message.setOffset(start);
      message.setLength(end - start);
      message.setLineNo(getLineNumber(start));

      if (reporter instanceof IncrementalReporter) {
        MarkupQuickAssistProcessor processor = new MarkupQuickAssistProcessor();
        processor.setProblemId(ProblemIDsXML.AttrsInEndTag);
        message.setAttribute(QUICKASSISTPROCESSOR, processor);

        AnnotationInfo info = new AnnotationInfo(message);
        ((IncrementalReporter) reporter).addAnnotationInfo(this, info);
      } else {
        reporter.addMessage(this, message);
      }
    }
  }

  private void checkClosingBracket(IStructuredDocumentRegion structuredDocumentRegion,
      IReporter reporter) {

    if (structuredDocumentRegion.isDeleted()) {
      return;
    }

    ITextRegionList regions = structuredDocumentRegion.getRegions();
    ITextRegion r = null;
    boolean closed = false;
    for (int i = 0; (i < regions.size()) && !structuredDocumentRegion.isDeleted(); i++) {
      r = regions.get(i);
      if ((r.getType() == DOMRegionContext.XML_TAG_CLOSE)
          || (r.getType() == DOMRegionContext.XML_EMPTY_TAG_CLOSE)) {
        closed = true;
      }
    }
    if (!closed) {

      String messageText = XMLUIMessages.ReconcileStepForMarkup_6;

      int start = structuredDocumentRegion.getStartOffset();
      int length = structuredDocumentRegion.getText().trim().length();
      int lineNo = getLineNumber(start);

      LocalizedMessage message = new LocalizedMessage(SEVERITY_MISSING_CLOSING_BRACKET, messageText);
      message.setOffset(start);
      message.setLength(length);
      message.setLineNo(lineNo);

      if (reporter instanceof IncrementalReporter) {
        MarkupQuickAssistProcessor processor = new MarkupQuickAssistProcessor();
        processor.setProblemId(ProblemIDsXML.MissingClosingBracket);
        message.setAttribute(QUICKASSISTPROCESSOR, processor);

        AnnotationInfo info = new AnnotationInfo(message);
        ((IncrementalReporter) reporter).addAnnotationInfo(this, info);
      } else {
        reporter.addMessage(this, message);
      }
    }
  }

  private void checkEmptyTag(IStructuredDocumentRegion structuredDocumentRegion, IReporter reporter) {

    if (structuredDocumentRegion.isDeleted()) {
      return;
    }

    // navigate to name
    ITextRegionList regions = structuredDocumentRegion.getRegions();
    if (regions.size() == 2) {
      // missing name region
      if ((regions.get(0).getType() == DOMRegionContext.XML_TAG_OPEN)
          && (regions.get(1).getType() == DOMRegionContext.XML_TAG_CLOSE)) {
        String messageText = XMLUIMessages.ReconcileStepForMarkup_3;
        int start = structuredDocumentRegion.getStartOffset();
        int length = structuredDocumentRegion.getLength();
        int lineNo = getLineNumber(start);

        LocalizedMessage message = new LocalizedMessage(SEVERITY_TAG_NAME_MISSING, messageText);
        message.setOffset(start);
        message.setLength(length);
        message.setLineNo(lineNo);

        if (reporter instanceof IncrementalReporter) {
          MarkupQuickAssistProcessor processor = new MarkupQuickAssistProcessor();
          processor.setProblemId(ProblemIDsXML.EmptyTag);
          message.setAttribute(QUICKASSISTPROCESSOR, processor);

          AnnotationInfo info = new AnnotationInfo(message);
          ((IncrementalReporter) reporter).addAnnotationInfo(this, info);
        } else {
          reporter.addMessage(this, message);
        }
      }
    }
  }

  private int getLineNumber(int start) {
    int lineNo = -1;
    try {
      lineNo = getDocument().getLineOfOffset(start) + 1;
    } catch (BadLocationException e) {
      Logger.logException(e);
    }
    return lineNo;
  }

  private void checkForAttributeValue(IStructuredDocumentRegion structuredDocumentRegion,
      IReporter reporter) {
    if (structuredDocumentRegion.isDeleted()) {
      return;
    }

    // check for attributes without a value
    // track the attribute/equals/value sequence using a state of 0, 1 ,2
    // representing the name, =, and value, respectively
    int attrState = 0;
    ITextRegionList textRegions = structuredDocumentRegion.getRegions();
    // ReconcileAnnotationKey key = createKey(structuredDocumentRegion,
    // getScope());

    int errorCount = 0;
    for (int i = 0; (i < textRegions.size())
        && (errorCount < AbstractStructuredTextReconcilingStrategy.ELEMENT_ERROR_LIMIT); i++) {
      ITextRegion textRegion = textRegions.get(i);
      if ((textRegion.getType() == DOMRegionContext.XML_TAG_ATTRIBUTE_NAME)
          || isTagCloseTextRegion(textRegion)) {
        // dangling name and '='
        if ((attrState == 2) && (i >= 2)) {
          // create annotation
          ITextRegion nameRegion = textRegions.get(i - 2);
          if (!(nameRegion instanceof ITextRegionContainer)) {
            Object[] args = {structuredDocumentRegion.getText(nameRegion)};
            String messageText = NLS.bind(XMLUIMessages.Attribute__is_missing_a_value, args);

            int start = structuredDocumentRegion.getStartOffset(nameRegion);
            int end = structuredDocumentRegion.getEndOffset();
            int lineNo = getLineNumber(start);
            int textLength = structuredDocumentRegion.getText(nameRegion).trim().length();

            LocalizedMessage message = new LocalizedMessage(SEVERITY_ATTRIBUTE_HAS_NO_VALUE,
                messageText);
            message.setOffset(start);
            message.setLength(textLength);
            message.setLineNo(lineNo);

            // quick fix info
            ITextRegion equalsRegion = textRegions.get(i - 2 + 1);
            int insertOffset = structuredDocumentRegion.getTextEndOffset(equalsRegion) - end;
            Object[] additionalFixInfo = {
                structuredDocumentRegion.getText(nameRegion), new Integer(insertOffset)};

            if (reporter instanceof IncrementalReporter) {
              MarkupQuickAssistProcessor processor = new MarkupQuickAssistProcessor();
              processor.setProblemId(ProblemIDsXML.MissingAttrValue);
              processor.setAdditionalFixInfo(additionalFixInfo);
              message.setAttribute(QUICKASSISTPROCESSOR, processor);

              AnnotationInfo info = new AnnotationInfo(message);
              ((IncrementalReporter) reporter).addAnnotationInfo(this, info);
            } else {
              reporter.addMessage(this, message);
            }
            errorCount++;
          }
        }
        // name but no '=' (XML only)
        else if ((attrState == 1) && (i >= 1)) {
          // create annotation
          ITextRegion previousRegion = textRegions.get(i - 1);
          if (!(previousRegion instanceof ITextRegionContainer)) {
            Object[] args = {structuredDocumentRegion.getText(previousRegion)};
            String messageText = NLS.bind(XMLUIMessages.Attribute__has_no_value, args);
            int start = structuredDocumentRegion.getStartOffset(previousRegion);
            int textLength = structuredDocumentRegion.getText(previousRegion).trim().length();
            int lineNo = getLineNumber(start);

            LocalizedMessage message = new LocalizedMessage(SEVERITY_ATTRIBUTE_HAS_NO_VALUE,
                messageText);
            message.setOffset(start);
            message.setLength(textLength);
            message.setLineNo(lineNo);

            if (reporter instanceof IncrementalReporter) {
              MarkupQuickAssistProcessor processor = new MarkupQuickAssistProcessor();
              processor.setProblemId(ProblemIDsXML.NoAttrValue);
              processor.setAdditionalFixInfo(structuredDocumentRegion.getText(previousRegion));
              message.setAttribute(QUICKASSISTPROCESSOR, processor);

              AnnotationInfo info = new AnnotationInfo(message);
              ((IncrementalReporter) reporter).addAnnotationInfo(this, info);
            } else {
              reporter.addMessage(this, message);
            }

            errorCount++;
          }
        }
        attrState = 1;
      } else if (textRegion.getType() == DOMRegionContext.XML_TAG_ATTRIBUTE_EQUALS) {
        attrState = 2;
      } else if (textRegion.getType() == DOMRegionContext.XML_TAG_ATTRIBUTE_VALUE) {
        attrState = 0;
      }
    }

  }

  private void checkForSpaceBeforeName(IStructuredDocumentRegion structuredDocumentRegion,
      IReporter reporter) {

    if (structuredDocumentRegion.isDeleted()) {
      return;
    }

    String sdRegionText = structuredDocumentRegion.getFullText();
    if (sdRegionText.startsWith(" ")) { //$NON-NLS-1$
      IStructuredDocumentRegion prev = structuredDocumentRegion.getPrevious();
      if (prev != null) {
        // this is possibly the case of "< tag"
        if ((prev.getRegions().size() == 1) && isStartTag(prev)) {
          // add the error for preceding space in tag name
          String messageText = XMLUIMessages.ReconcileStepForMarkup_2;
          int start = structuredDocumentRegion.getStartOffset();
          // find length of whitespace
          int length = sdRegionText.trim().equals("") ? sdRegionText.length() : sdRegionText.indexOf(sdRegionText.trim()); //$NON-NLS-1$

          LocalizedMessage message = new LocalizedMessage(
              SEVERITY_INVALID_WHITESPACE_BEFORE_TAGNAME, messageText);
          message.setOffset(start);
          message.setLength(length);
          message.setLineNo(getLineNumber(start));

          if (reporter instanceof IncrementalReporter) {
            MarkupQuickAssistProcessor processor = new MarkupQuickAssistProcessor();
            processor.setProblemId(ProblemIDsXML.SpacesBeforeTagName);
            message.setAttribute(QUICKASSISTPROCESSOR, processor);

            AnnotationInfo info = new AnnotationInfo(message);
            ((IncrementalReporter) reporter).addAnnotationInfo(this, info);
          } else {
            reporter.addMessage(this, message);
          }
        }
      }
    }
  }

  private void checkNoNamespaceInPI(IStructuredDocumentRegion structuredDocumentRegion,
      IReporter reporter) {

    if (structuredDocumentRegion.isDeleted()) {
      return;
    }

    // navigate to name
    ITextRegionList regions = structuredDocumentRegion.getRegions();
    ITextRegion r = null;
    int errorCount = 0;
    for (int i = 0; (i < regions.size())
        && (errorCount < AbstractStructuredTextReconcilingStrategy.ELEMENT_ERROR_LIMIT)
        && !structuredDocumentRegion.isDeleted(); i++) {
      r = regions.get(i);
      if (r.getType() == DOMRegionContext.XML_TAG_NAME) {
        String piText = structuredDocumentRegion.getText(r);
        int index = piText.indexOf(":"); //$NON-NLS-1$
        if (index != -1) {
          String messageText = XMLUIMessages.ReconcileStepForMarkup_4;
          int start = structuredDocumentRegion.getStartOffset(r) + index;
          int length = piText.trim().length() - index;

          LocalizedMessage message = new LocalizedMessage(SEVERITY_NAMESPACE_IN_PI_TARGET,
              messageText);
          message.setOffset(start);
          message.setLength(length);
          message.setLineNo(getLineNumber(start));

          if (reporter instanceof IncrementalReporter) {
            MarkupQuickAssistProcessor processor = new MarkupQuickAssistProcessor();
            processor.setProblemId(ProblemIDsXML.NamespaceInPI);
            message.setAttribute(QUICKASSISTPROCESSOR, processor);

            AnnotationInfo info = new AnnotationInfo(message);
            ((IncrementalReporter) reporter).addAnnotationInfo(this, info);
          } else {
            reporter.addMessage(this, message);
          }

          errorCount++;
        }
      }
    }
  }

  private void checkQuotesForAttributeValues(IStructuredDocumentRegion structuredDocumentRegion,
      IReporter reporter) {
    ITextRegionList regions = structuredDocumentRegion.getRegions();
    ITextRegion r = null;
    String attrValueText = ""; //$NON-NLS-1$
    int errorCount = 0;
    for (int i = 0; (i < regions.size())
        && (errorCount < AbstractStructuredTextReconcilingStrategy.ELEMENT_ERROR_LIMIT); i++) {
      r = regions.get(i);
      if (r.getType() != DOMRegionContext.XML_TAG_ATTRIBUTE_VALUE) {
        continue;
      }

      attrValueText = structuredDocumentRegion.getText(r);
      // attribute value includes quotes in the string
      // split up attribute value on quotes
      /*
       * WORKAROUND till http://dev.icu-project.org/cgi-bin/icu-bugs/incoming?findid=5207 is fixed.
       * (Also see BUG143628)
       */

      java.util.StringTokenizer st = new java.util.StringTokenizer(attrValueText, "\"'", true); //$NON-NLS-1$
      int size = st.countTokens();
      // get the pieces of the attribute value
      String one = "", two = ""; //$NON-NLS-1$ //$NON-NLS-2$
      if (size > 0) {
        one = st.nextToken();
      }
      if (size > 1) {
        two = st.nextToken();
      }
      if (size > 2) {
        // should be handled by parsing...
        // as in we can't have an attribute value like: <element
        // attr="a"b"c"/>
        // and <element attr='a"b"c' /> is legal
        continue;
      }

      if (size == 1) {
        if (one.equals(DQUOTE) || one.equals(SQUOTE)) {
          // missing closing quote
          String message = XMLUIMessages.ReconcileStepForMarkup_0;
          addAttributeError(message, attrValueText, structuredDocumentRegion.getStartOffset(r),
              attrValueText.trim().length(), ProblemIDsXML.Unclassified, structuredDocumentRegion,
              reporter, SEVERITY_MISSING_CLOSING_QUOTE);
          errorCount++;
        } else {
          // missing both
          String message = XMLUIMessages.ReconcileStepForMarkup_1;
          addAttributeError(message, attrValueText, structuredDocumentRegion.getStartOffset(r),
              attrValueText.trim().length(), ProblemIDsXML.AttrValueNotQuoted,
              structuredDocumentRegion, reporter, SEVERITY_MISSING_QUOTES);
          errorCount++;
        }
      } else if (size == 2) {
        if ((one.equals(SQUOTE) && !two.equals(SQUOTE))
            || (one.equals(DQUOTE) && !two.equals(DQUOTE))) {
          // missing closing quote
          String message = XMLUIMessages.ReconcileStepForMarkup_0;
          addAttributeError(message, attrValueText, structuredDocumentRegion.getStartOffset(r),
              attrValueText.trim().length(), ProblemIDsXML.Unclassified, structuredDocumentRegion,
              reporter, SEVERITY_MISSING_CLOSING_QUOTE);
          errorCount++;
        }
      }
    }
    // end of region for loop
  }

  private void checkStartEndTagPairs(IStructuredDocumentRegion sdRegion, IReporter reporter) {

    if (sdRegion.isDeleted()) {
      return;
    }

    // check start/end tag pairs
    IDOMNode xmlNode = getXMLNode(sdRegion);

    if (xmlNode == null) {
      return;
    }

    boolean selfClosed = false;
    String tagName = null;

    /**
     * For tags that aren't meant to be EMPTY, make sure it's empty or has an end tag
     */
    if (xmlNode.isContainer()) {
      IStructuredDocumentRegion endRegion = xmlNode.getEndStructuredDocumentRegion();
      if (endRegion == null) {
        IStructuredDocumentRegion startRegion = xmlNode.getStartStructuredDocumentRegion();
        if (startRegion != null && !startRegion.isDeleted()
            && DOMRegionContext.XML_TAG_OPEN.equals(startRegion.getFirstRegion().getType())) {
          // analyze the tag (check self closing)
          ITextRegionList regions = startRegion.getRegions();
          ITextRegion r = null;
          int start = sdRegion.getStart();
          int length = sdRegion.getTextLength();
          for (int i = 0; i < regions.size(); i++) {
            r = regions.get(i);
            if (r.getType() == DOMRegionContext.XML_TAG_NAME) {
              tagName = sdRegion.getText(r);
              start = sdRegion.getStartOffset(r);
              length = r.getTextLength();
            } else if (r.getType() == DOMRegionContext.XML_EMPTY_TAG_CLOSE) {
              selfClosed = true;
            }
          }

          if (!selfClosed && (tagName != null)) {
            Object[] args = {tagName};
            String messageText = NLS.bind(XMLUIMessages.Missing_end_tag_, args);

            int lineNumber = getLineNumber(start);

            IMessage message = new LocalizedMessage(SEVERITY_MISSING_END_TAG, messageText);
            message.setOffset(start);
            message.setLength(length);
            message.setLineNo(lineNumber);

            if (reporter instanceof IncrementalReporter) {
              Object[] additionalFixInfo = getStartEndFixInfo(xmlNode, tagName, r);

              MarkupQuickAssistProcessor processor = new MarkupQuickAssistProcessor();
              processor.setProblemId(ProblemIDsXML.MissingEndTag);
              processor.setAdditionalFixInfo(additionalFixInfo);
              message.setAttribute(QUICKASSISTPROCESSOR, processor);

              AnnotationInfo info = new AnnotationInfo(message);

              ((IncrementalReporter) reporter).addAnnotationInfo(this, info);
            } else {
              reporter.addMessage(this, message);
            }
          }
        }
      } else {
        IStructuredDocumentRegion startRegion = xmlNode.getStartStructuredDocumentRegion();
        if (startRegion == null || startRegion.isDeleted()) {
          // analyze the tag (check self closing)
          ITextRegionList regions = endRegion.getRegions();
          ITextRegion r = null;
          int start = sdRegion.getStart();
          int length = sdRegion.getTextLength();
          for (int i = 0; i < regions.size(); i++) {
            r = regions.get(i);
            if (r.getType() == DOMRegionContext.XML_TAG_NAME) {
              tagName = sdRegion.getText(r);
              start = sdRegion.getStartOffset(r);
              length = r.getTextLength();
            }
          }

          if (tagName != null) {
            Object[] args = {tagName};
            String messageText = NLS.bind(XMLUIMessages.Missing_start_tag_, args);

            int lineNumber = getLineNumber(start);

            IMessage message = new LocalizedMessage(SEVERITY_MISSING_START_TAG, messageText);
            message.setOffset(start);
            message.setLength(length);
            message.setLineNo(lineNumber);

            if (reporter instanceof IncrementalReporter) {
              Object[] additionalFixInfo = getStartEndFixInfo(xmlNode, tagName, r);

              MarkupQuickAssistProcessor processor = new MarkupQuickAssistProcessor();
              processor.setProblemId(ProblemIDsXML.MissingStartTag);
              processor.setAdditionalFixInfo(additionalFixInfo);
              message.setAttribute(QUICKASSISTPROCESSOR, processor);

              AnnotationInfo info = new AnnotationInfo(message);

              ((IncrementalReporter) reporter).addAnnotationInfo(this, info);
            } else {
              reporter.addMessage(this, message);
            }
          }
        }
      }

    }
    /*
     * Check for an end tag that has no start tag
     */
    else {
      IStructuredDocumentRegion startRegion = xmlNode.getStartStructuredDocumentRegion();
      if (startRegion == null) {
        IStructuredDocumentRegion endRegion = xmlNode.getEndStructuredDocumentRegion();
        if (!endRegion.isDeleted()) {
          // get name
          ITextRegionList regions = endRegion.getRegions();
          ITextRegion r = null;
          for (int i = 0; i < regions.size(); i++) {
            r = regions.get(i);
            if (r.getType() == DOMRegionContext.XML_TAG_NAME) {
              tagName = sdRegion.getText(r);
            }
          }

          if (!selfClosed && (tagName != null)) {
            String messageText = XMLUIMessages.Indicate_no_grammar_specified_severities_error;

            int start = sdRegion.getStart();
            int lineNumber = getLineNumber(start);

            // SEVERITY_STRUCTURE == IMessage.HIGH_SEVERITY
            IMessage message = new LocalizedMessage(IMessage.HIGH_SEVERITY, messageText);
            message.setOffset(start);
            message.setLength(sdRegion.getTextLength());
            message.setLineNo(lineNumber);

            reporter.addMessage(this, message);
          }
        }
      }
    }
  }

  private Object[] getStartEndFixInfo(IDOMNode xmlNode, String tagName, ITextRegion r) {
    // quick fix info
    String tagClose = "/>"; //$NON-NLS-1$
    int tagCloseOffset = xmlNode.getFirstStructuredDocumentRegion().getEndOffset();
    if ((r != null) && (r.getType() == DOMRegionContext.XML_TAG_CLOSE)) {
      tagClose = "/"; //$NON-NLS-1$
      tagCloseOffset--;
    }
    IDOMNode firstChild = (IDOMNode) xmlNode.getFirstChild();
    while ((firstChild != null) && (firstChild.getNodeType() == Node.TEXT_NODE)) {
      firstChild = (IDOMNode) firstChild.getNextSibling();
    }
    int endOffset = xmlNode.getEndOffset();
    int firstChildStartOffset = firstChild == null ? endOffset : firstChild.getStartOffset();
    Object[] additionalFixInfo = {
        tagName, tagClose, new Integer(tagCloseOffset),
        new Integer(xmlNode.getFirstStructuredDocumentRegion().getEndOffset()), // startTagEndOffset
        new Integer(firstChildStartOffset), // firstChildStartOffset
        new Integer(endOffset)}; // endOffset
    return additionalFixInfo;
  }

  private void checkStartingSpaceForPI(IStructuredDocumentRegion structuredDocumentRegion,
      IReporter reporter) {

    if (structuredDocumentRegion.isDeleted()) {
      return;
    }

    IStructuredDocumentRegion prev = structuredDocumentRegion.getPrevious();
    if ((prev != null) && prev.getStartOffset() == 0) {
      if (prev.getType() == DOMRegionContext.XML_CONTENT) {
        String messageText = XMLUIMessages.ReconcileStepForMarkup_5;
        int start = prev.getStartOffset();
        int length = prev.getLength();

        LocalizedMessage message = new LocalizedMessage(SEVERITY_WHITESPACE_AT_START, messageText);
        message.setOffset(start);
        message.setLength(length);
        message.setLineNo(getLineNumber(start));

        if (reporter instanceof IncrementalReporter) {
          MarkupQuickAssistProcessor processor = new MarkupQuickAssistProcessor();
          processor.setProblemId(ProblemIDsXML.SpacesBeforePI);
          message.setAttribute(QUICKASSISTPROCESSOR, processor);

          AnnotationInfo info = new AnnotationInfo(message);
          ((IncrementalReporter) reporter).addAnnotationInfo(this, info);
        } else {
          reporter.addMessage(this, message);
        }
        // Position p = new Position(start, length);
        //				
        // ReconcileAnnotationKey key =
        // createKey(structuredDocumentRegion, getScope());
        // TemporaryAnnotation annotation = new TemporaryAnnotation(p,
        // SEVERITY_SYNTAX_ERROR, message, key,
        // ProblemIDsXML.SpacesBeforePI);
        // results.add(annotation);
      }
    }
  }

  public int getScope() {
    return ReconcileAnnotationKey.PARTIAL;
  }

  private IDOMNode getXMLNode(IStructuredDocumentRegion sdRegion) {

    if (sdRegion == null) {
      return null;
    }

    IStructuredModel xModel = null;
    IDOMNode xmlNode = null;
    // get/release models should always be in a try/finally block
    try {
      xModel = StructuredModelManager.getModelManager().getExistingModelForRead(getDocument());
      // xModel is sometime null, when closing editor, for example
      if (xModel != null) {
        xmlNode = (IDOMNode) xModel.getIndexedRegion(sdRegion.getStart());
      }
    } finally {
      if (xModel != null) {
        xModel.releaseFromRead();
      }
    }
    return xmlNode;
  }

  /**
   * Determines whether the IStructuredDocumentRegion is a XML "end tag" since they're not allowed
   * to have attribute ITextRegions
   * 
   * @param structuredDocumentRegion
   */
  private boolean isEndTag(IStructuredDocumentRegion structuredDocumentRegion) {
    if ((structuredDocumentRegion == null) || structuredDocumentRegion.isDeleted()) {
      return false;
    }
    return structuredDocumentRegion.getFirstRegion().getType() == DOMRegionContext.XML_END_TAG_OPEN;
  }

  /**
   * Determines if the IStructuredDocumentRegion is an XML Processing Instruction
   * 
   * @param structuredDocumentRegion
   */
  private boolean isPI(IStructuredDocumentRegion structuredDocumentRegion) {
    if ((structuredDocumentRegion == null) || structuredDocumentRegion.isDeleted()) {
      return false;
    }
    return structuredDocumentRegion.getFirstRegion().getType() == DOMRegionContext.XML_PI_OPEN;
  }

  /**
   * Determines whether the IStructuredDocumentRegion is a XML "start tag" since they need to be
   * checked for proper XML attribute region sequences
   * 
   * @param structuredDocumentRegion
   */
  private boolean isStartTag(IStructuredDocumentRegion structuredDocumentRegion) {
    if ((structuredDocumentRegion == null) || structuredDocumentRegion.isDeleted()) {
      return false;
    }
    return structuredDocumentRegion.getFirstRegion().getType() == DOMRegionContext.XML_TAG_OPEN;
  }

  // Because we check the "proper" closing separately from attribute
  // sequencing, we need to know what's
  // an appropriate close.
  private boolean isTagCloseTextRegion(ITextRegion textRegion) {
    return (textRegion.getType() == DOMRegionContext.XML_TAG_CLOSE)
        || (textRegion.getType() == DOMRegionContext.XML_EMPTY_TAG_CLOSE);
  }

  /**
   * Determines if the IStructuredDocumentRegion is XML Content
   * 
   * @param structuredDocumentRegion
   */
  private boolean isXMLContent(IStructuredDocumentRegion structuredDocumentRegion) {
    if ((structuredDocumentRegion == null) || structuredDocumentRegion.isDeleted()) {
      return false;
    }
    return structuredDocumentRegion.getFirstRegion().getType() == DOMRegionContext.XML_CONTENT;
  }

  private void setDocument(IDocument doc) {
    fDocument = doc;
  }

  private IDocument getDocument() {
    return fDocument;
  }

  public void connect(IDocument document) {
    setDocument(document);
  }

  public void disconnect(IDocument document) {
    setDocument(null);
  }

  public void validate(IRegion dirtyRegion, IValidationContext helper, IReporter reporter) {
    if (getDocument() == null) {
      return;
    }
    if (!(reporter instanceof IncrementalReporter)) {
      return;
    }
    if (!(getDocument() instanceof IStructuredDocument)) {
      return;
    }

    // remove old messages
    reporter.removeAllMessages(this);

    IStructuredDocumentRegion[] regions = ((IStructuredDocument) fDocument).getStructuredDocumentRegions(
        dirtyRegion.getOffset(), dirtyRegion.getLength());
    for (int i = 0; i < regions.length; i++) {
      validate(regions[i], reporter);
    }
  }

  public void validate(IStructuredDocumentRegion structuredDocumentRegion, IReporter reporter) {

    if (structuredDocumentRegion == null) {
      return;
    }

    if (isStartTag(structuredDocumentRegion)) {
      // check for attributes without a value
      checkForAttributeValue(structuredDocumentRegion, reporter);
      // check if started tag is ended
      checkStartEndTagPairs(structuredDocumentRegion, reporter);
      // check empty tag <>
      checkEmptyTag(structuredDocumentRegion, reporter);
      // check that each attribute has quotes
      checkQuotesForAttributeValues(structuredDocumentRegion, reporter);
      // check that the closing '>' is there
      checkClosingBracket(structuredDocumentRegion, reporter);
    } else if (isEndTag(structuredDocumentRegion)) {
      // check if ending tag was started
      checkStartEndTagPairs(structuredDocumentRegion, reporter);
      // check for attributes in an end tag
      checkAttributesInEndTag(structuredDocumentRegion, reporter);
      // check that the closing '>' is there
      checkClosingBracket(structuredDocumentRegion, reporter);
    } else if (isPI(structuredDocumentRegion)) {
      // check validity of processing instruction
      checkStartingSpaceForPI(structuredDocumentRegion, reporter);
      checkNoNamespaceInPI(structuredDocumentRegion, reporter);
    } else if (isXMLContent(structuredDocumentRegion)) {
      checkForSpaceBeforeName(structuredDocumentRegion, reporter);
    } else if (isXMLDoctypeDeclaration(structuredDocumentRegion)) {
      checkDocumentTypeReferences(structuredDocumentRegion, reporter);
    }
  }

  /**
   * @param structuredDocumentRegion
   * @param reporter
   */
  private void checkDocumentTypeReferences(IStructuredDocumentRegion structuredDocumentRegion,
      IReporter reporter) {
  }

  /**
   * @param structuredDocumentRegion
   * @return
   */
  private boolean isXMLDoctypeDeclaration(IStructuredDocumentRegion structuredDocumentRegion) {
    if ((structuredDocumentRegion == null) || structuredDocumentRegion.isDeleted()) {
      return false;
    }
    return structuredDocumentRegion.getFirstRegion().getType() == DOMRegionContext.XML_DECLARATION_OPEN
        && structuredDocumentRegion.getType().equals(DOMRegionContext.XML_DOCTYPE_DECLARATION);
  }

  public void cleanup(IReporter reporter) {
    fDocument = null;
  }

  public void validate(IValidationContext helper, IReporter reporter) throws ValidationException {
    String[] uris = helper.getURIs();
    IWorkspaceRoot wsRoot = ResourcesPlugin.getWorkspace().getRoot();
    if (uris.length > 0) {
      IFile currentFile = null;

      for (int i = 0; i < uris.length && !reporter.isCancelled(); i++) {
        // might be called with just project path?
        IPath path = new Path(uris[i]);
        if (path.segmentCount() > 1) {
          currentFile = wsRoot.getFile(path);
          if (shouldValidate(currentFile, true)) {
            validateV1File(currentFile, reporter);
          }
        } else if (uris.length == 1) {
          validateV1Project(helper, reporter);
        }
      }
    } else
      validateV1Project(helper, reporter);
  }

  private boolean shouldValidate(IResourceProxy proxy) {
    if (proxy.getType() == IResource.FILE) {
      String name = proxy.getName();
      if (name.toLowerCase(Locale.US).endsWith(".xml")) {
        return true;
      }
    }
    return shouldValidate(proxy.requestResource(), false);
  }

  private boolean shouldValidate(IResource file, boolean checkExtension) {
    if (file == null || !file.exists() || file.getType() != IResource.FILE)
      return false;
    if (checkExtension) {
      String extension = file.getFileExtension();
      if (extension != null && "xml".endsWith(extension.toLowerCase(Locale.US)))
        return true;
    }

    IContentDescription contentDescription = null;
    try {
      contentDescription = ((IFile) file).getContentDescription();
      if (contentDescription != null) {
        IContentType contentType = contentDescription.getContentType();
        return contentDescription != null && contentType.isKindOf(getXMLContentType());
      }
    } catch (CoreException e) {
      Logger.logException(e);
    }
    return false;
  }

  /**
   * @param helper
   * @param reporter
   */
  private void validateV1Project(IValidationContext helper, final IReporter reporter) {
    // if uris[] length 0 -> validate() gets called for each project
    if (helper instanceof IWorkbenchContext) {
      IProject project = ((IWorkbenchContext) helper).getProject();
      IResourceProxyVisitor visitor = new IResourceProxyVisitor() {
        public boolean visit(IResourceProxy proxy) throws CoreException {
          if (shouldValidate(proxy)) {
            validateV1File((IFile) proxy.requestResource(), reporter);
          }
          return true;
        }
      };
      try {
        // collect all jsp files for the project
        project.accept(visitor, IResource.DEPTH_INFINITE);
      } catch (CoreException e) {
        Logger.logException(e);
      }
    }
  }

  /**
   * @param currentFile
   * @param reporter
   */
  private void validateV1File(IFile currentFile, IReporter reporter) {
    Message message = new LocalizedMessage(IMessage.LOW_SEVERITY,
        currentFile.getFullPath().toString().substring(1));
    reporter.displaySubtask(MarkupValidator.this, message);

    IStructuredModel model = null;
    try {
      model = StructuredModelManager.getModelManager().getModelForRead(currentFile);
      IStructuredDocument document = null;
      if (model != null) {
        document = model.getStructuredDocument();
        connect(document);
        IStructuredDocumentRegion validationRegion = document.getFirstStructuredDocumentRegion();
        while (validationRegion != null) {
          validate(validationRegion, reporter);
          validationRegion = validationRegion.getNext();
        }
        disconnect(document);
      }
    } catch (Exception e) {
      Logger.logException(e);
    } finally {
      if (model != null) {
        model.releaseFromRead();
      }
    }
  }

  /**
   * @return
   */
  private IContentType getXMLContentType() {
    if (fRootContentType == null) {
      fRootContentType = Platform.getContentTypeManager().getContentType(
          "org.eclipse.core.runtime.xml");
    }
    return fRootContentType;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.wst.validation.AbstractValidator#validate(org.eclipse.core.resources.IResource,
   * int, org.eclipse.wst.validation.ValidationState, org.eclipse.core.runtime.IProgressMonitor)
   */
  public ValidationResult validate(IResource resource, int kind, ValidationState state,
      IProgressMonitor monitor) {
    if (resource.getType() != IResource.FILE)
      return null;
    ValidationResult result = new ValidationResult();
    IReporter reporter = result.getReporter(monitor);
    validateV1File((IFile) resource, reporter);
    return result;
  }
}
