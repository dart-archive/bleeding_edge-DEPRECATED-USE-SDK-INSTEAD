/*******************************************************************************
 * Copyright (c) 2005, 2011 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.validation;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.quickassist.IQuickAssistProcessor;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.IndexedRegion;
import org.eclipse.wst.sse.ui.internal.reconcile.validator.AnnotationInfo;
import org.eclipse.wst.sse.ui.internal.reconcile.validator.IncrementalReporter;
import org.eclipse.wst.validation.internal.core.IMessageAccess;
import org.eclipse.wst.validation.internal.core.ValidationException;
import org.eclipse.wst.validation.internal.provisional.core.IMessage;
import org.eclipse.wst.validation.internal.provisional.core.IProjectValidationContext;
import org.eclipse.wst.validation.internal.provisional.core.IReporter;
import org.eclipse.wst.validation.internal.provisional.core.IValidationContext;
import org.eclipse.wst.validation.internal.provisional.core.IValidator;
import org.eclipse.wst.validation.internal.provisional.core.IValidatorJob;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMAttr;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMDocument;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMElement;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMNode;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMText;
import org.eclipse.wst.xml.core.internal.validation.AnnotationMsg;
import org.eclipse.wst.xml.ui.internal.Logger;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * A DelegatingReconcileValidator calls its delegate validator to get a list of validation error
 * IMessages. Using information in this IMessage the DelegatingReconcileValidator updates the
 * IMessage with an offset and length to give a good range to be "squiggled" and adds the messages
 * to the IReporter
 * 
 * @author Mark Hutchinson
 */
public abstract class DelegatingSourceValidator implements IValidator {
  // the selection strategies:
  protected static final String ALL_ATTRIBUTES = "ALL_ATTRIBUTES"; //$NON-NLS-1$
  protected static final String ATTRIBUTE_NAME = "ATTRIBUTE_NAME"; //$NON-NLS-1$
  protected static final String ATTRIBUTE_NAME_LAST = "ATTRIBUTE_NAME_LAST"; //$NON-NLS-1$
  protected static final String ATTRIBUTE_VALUE = "ATTRIBUTE_VALUE"; //$NON-NLS-1$
  protected static final String START_TAG = "START_TAG"; //$NON-NLS-1$
  protected static final String TEXT = "TEXT"; //$NON-NLS-1$
  protected static final String FIRST_NON_WHITESPACE_TEXT = "FIRST_NON_WHITESPACE_TEXT"; //$NON-NLS-1$
  protected static final String TEXT_ENTITY_REFERENCE = "TEXT_ENTITY_REFERENCE"; //$NON-NLS-1$
  protected static final String VALUE_OF_ATTRIBUTE_WITH_GIVEN_VALUE = "VALUE_OF_ATTRIBUTE_WITH_GIVEN_VALUE"; //$NON-NLS-1$
  protected static final String END_TAG = "END_TAG"; //$NON-NLS-1$
  private static final String QUICKASSISTPROCESSOR = IQuickAssistProcessor.class.getName();

  /**
   * This constant specifies the attribute name that specifies the side of the 'start tag' that the
   * validator has used to report an error. A validator may choose to report a message at the left
   * (the start of the start tag)or at the right (the end of the start tag). When this attribute is
   * not specified error ERROR_SIDE_LEFT is the default.
   */
  protected static final String ERROR_SIDE = "ERROR_SIDE"; //$NON-NLS-1$

  /**
   * When the ERROR_SIDE attribute specifies the ERROR_SIDE_LEFT value it is assumed that the
   * message specifies a location to the left of the start tag
   */
  protected static final String ERROR_SIDE_LEFT = "ERROR_SIDE_LEFT"; //$NON-NLS-1$	

  /**
   * When the ERROR_SIDE attribute specifies the ERROR_SIDE_RIGHT value it is assumed that the
   * message specifies a location to the right of the start tag
   */
  protected static final String ERROR_SIDE_RIGHT = "ERROR_SIDE_RIGHT"; //$NON-NLS-1$	

  protected static final String COLUMN_NUMBER_ATTRIBUTE = "columnNumber"; //$NON-NLS-1$
  protected static final String SQUIGGLE_SELECTION_STRATEGY_ATTRIBUTE = "squiggleSelectionStrategy"; //$NON-NLS-1$
  protected static final String SQUIGGLE_NAME_OR_VALUE_ATTRIBUTE = "squiggleNameOrValue"; //$NON-NLS-1$

  public DelegatingSourceValidator() {
    super(); // constructor
  }

  public void cleanup(IReporter arg0) { // don't need to implement
  }

  // My Implementation of IHelper
  class MyHelper implements IProjectValidationContext {
    InputStream inputStream;

    IFile file;

    public MyHelper(InputStream inputStream, IFile file) {
      this.inputStream = inputStream;
      this.file = file;
    }

    public int getBuildKind() {
      return 0;
    }

    public Object loadModel(String symbolicName, Object[] parms) {
      if (symbolicName.equals("getFile")) { //$NON-NLS-1$
        return file;
      }
      return null;
    }

    public Object loadModel(String symbolicName) {
      if (symbolicName.equals("inputStream")) { //$NON-NLS-1$
        return inputStream;
      }
      return null;
    }

    public String[] getURIs() {
      if (file != null) {
        return new String[] {file.getFullPath().toString()};
      }
      return new String[0];
    }

    public IProject getProject() {
      if (file != null) {
        return file.getProject();
      }
      return null;
    }
  }

  // My Implementation of IReporter
  class MyReporter implements IReporter {
    List list = new ArrayList();

    public MyReporter() {
      super();
    }

    public void addMessage(IValidator origin, IMessage message) {
      list.add(message);
    }

    public void displaySubtask(IValidator validator, IMessage message) {
      /* do not need to implement */
    }

    public IMessageAccess getMessageAccess() {
      return null;
    }

    public boolean isCancelled() {
      return false;
    }

    public void removeAllMessages(IValidator origin, Object object) { // do
      /* do not need to implement */
    }

    public void removeAllMessages(IValidator origin) {
      /* do not need to implement */
    }

    public void removeMessageSubset(IValidator validator, Object obj, String groupName) {// do
      /* do not need to implement */
    }

    public List getMessages() {
      return list;
    }
  }

  protected abstract IValidator getDelegateValidator();

  /**
   * Calls a delegate validator getting and updates it's list of ValidationMessages with a good
   * squiggle offset and length.
   * 
   * @param helper loads an object.
   * @param reporter Is an instance of an IReporter interface, which is used for interaction with
   *          the user.
   */
  public void validate(IValidationContext helper, IReporter reporter) throws ValidationException {
    String[] delta = helper.getURIs();
    if (delta.length > 0) {
      // get the file, model and document:
      IFile file = getFile(delta[0]);
      IDOMModel xmlModel = null;
      if (file != null)
        xmlModel = getModelForResource(file);
      // some problem occurred, abort
      if (xmlModel == null)
        return;

      try {
        IDOMDocument document = xmlModel.getDocument();

        // store the text in a byte array; make a full copy to ease
        // any threading problems
        byte[] byteArray;
        try {
          byteArray = xmlModel.getStructuredDocument().get().getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
          // Not likely to happen
          byteArray = xmlModel.getStructuredDocument().get().getBytes();
        }

        if (isDelegateValidatorEnabled(file)) {
          IValidator validator = getDelegateValidator();
          if (validator != null) {
            // Validate the file:
            IValidationContext vHelper = new MyHelper(new ByteArrayInputStream(byteArray), file);
            MyReporter vReporter = new MyReporter();
            if (validator instanceof IValidatorJob) {
              ((IValidatorJob) validator).validateInJob(vHelper, vReporter);
            } else {
              validator.validate(vHelper, vReporter);
            }
            List messages = vReporter.list;

            // set the offset and length
            updateValidationMessages(messages, document, reporter);
          }
        }
      }

      finally {
        if (xmlModel != null) {
          xmlModel.releaseFromRead();
        }
      }
    }
  }

  /**
   * iterates through the messages and calculates a "better" offset and length
   * 
   * @param messages - a List of IMessages
   * @param document - the document
   * @param reporter - the reporter the messages are to be added to
   */
  protected void updateValidationMessages(List messages, IDOMDocument document, IReporter reporter) {
    for (int i = 0; i < messages.size(); i++) {
      IMessage message = (IMessage) messages.get(i);
      try {
        if (message.getAttribute(COLUMN_NUMBER_ATTRIBUTE) != null) {
          int column = ((Integer) message.getAttribute(COLUMN_NUMBER_ATTRIBUTE)).intValue();
          if (message.getAttribute(AnnotationMsg.PROBMLEM_ID) != null
              && reporter instanceof IncrementalReporter) {
            Integer problemId = (Integer) message.getAttribute(AnnotationMsg.PROBMLEM_ID);
            MarkupQuickAssistProcessor processor = new MarkupQuickAssistProcessor();
            processor.setProblemId(problemId.intValue());
            message.setOffset(column);
            Integer length = (Integer) message.getAttribute(AnnotationMsg.LENGTH);
            message.setLength(length.intValue());
            Object attrValue = message.getAttribute(AnnotationMsg.ATTRVALUETEXT);
            if (attrValue != null)
              processor.setAdditionalFixInfo(attrValue);
            else {
              Object attrValueNo = message.getAttribute(AnnotationMsg.ATTRVALUENO);
              if (attrValueNo != null) {
                int len = ((Integer) attrValueNo).intValue();
                Object[] objArray = new Object[len];
                for (int j = 0; j < len; j++) {
                  objArray[j] = message.getAttribute(AnnotationMsg.ATTRNO + j);
                }
                processor.setAdditionalFixInfo(objArray);
              }

            }
            message.setAttribute(QUICKASSISTPROCESSOR, processor);
            AnnotationInfo info = new AnnotationInfo(message);
            ((IncrementalReporter) reporter).addAnnotationInfo(this, info);
          } else {
            String selectionStrategy = (String) message.getAttribute(SQUIGGLE_SELECTION_STRATEGY_ATTRIBUTE);
            String nameOrValue = (String) message.getAttribute(SQUIGGLE_NAME_OR_VALUE_ATTRIBUTE);

            // convert the line and Column numbers to an offset:
            int start = document.getStructuredDocument().getLineOffset(message.getLineNumber() - 1)
                + column - 1;
            int[] result = computeStartAndEndLocation(start, selectionStrategy,
                getErrorSide(message), nameOrValue, document);
            if (result != null) {
              message.setOffset(result[0]);
              message.setLength(result[1] - result[0]);

              reporter.addMessage(this, message);
            }
          }
        }

      } catch (BadLocationException e) { // this exception should not
        // occur - it is thrown if
        // trying to convert an
        // invalid line number to and
        // offset
      }

    }
  }

  /**
   * @param delta the IFileDelta containing the file name to get
   * @return the IFile
   */
  public IFile getFile(String delta) {
    IPath path = new Path(delta);
    if (path.segmentCount() > 1) {
      IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(path);
      if (file != null && file.exists())
        return file;
    }
    return null;
  }

  /**
   * @param file the file to get the model for
   * @return the file's XMLModel or null
   */
  protected IDOMModel getModelForResource(IFile file) {
    IStructuredModel model = null;
    IModelManager manager = StructuredModelManager.getModelManager();

    try {
      model = manager.getModelForRead(file);
      // TODO.. HTML validator tries again to get a model a 2nd way
    } catch (Exception e) {
      Logger.log(Logger.ERROR_DEBUG, file.getFullPath().toString(), e);
    }

    if (model instanceof IDOMModel)
      return (IDOMModel) model;
    if (model != null)
      model.releaseFromRead();
    return null;
  }

  /**
   * @deprecated use computeStartEndLocation(int startOffset, String errorMessage, String
   *             selectionStrategy, boolean leftError, String nameOrValue, IDOMDocument document) {
   */
  protected int[] computeStartEndLocation(int startOffset, String errorMessage,
      String selectionStrategy, String nameOrValue, IDOMDocument document) {
    return computeStartAndEndLocation(startOffset, selectionStrategy, ERROR_SIDE_RIGHT,
        nameOrValue, document);
  }

  /**
   * Calculates the "better" offsets.
   * 
   * @param startOffset - the offset given by Xerces
   * @param errorMessage - the Xerces error Message
   * @param selectionStrategy - the selectionStrategy
   * @param document - the document
   * @return int[] - position 0 has the start offset of the squiggle range, position 1 has the
   *         endOffset
   */
  /*
   * The way the offsets is calculated is: - find the indexed region (element) closest to the given
   * offset - if we are between two elements, choosing left or right element will depend on
   * parameter 'errorSide' - based on the selectionStrategy choose the underlining strategy (eg
   * START_TAG means underline the start tag of that element) - use information from nameOrValue and
   * the DOM to get better offsets
   */
  protected int[] computeStartAndEndLocation(int startOffset, String selectionStrategy,
      String errorSide, String nameOrValue, IDOMDocument document) {
    try {
      int startEndPositions[] = new int[2];

      IndexedRegion region = document.getModel().getIndexedRegion(startOffset);
      IndexedRegion prevRegion = document.getModel().getIndexedRegion(startOffset - 1);

      if (prevRegion != region) {
        // if between two regions we use the 'errorSide' to understand
        // which
        // element is applicable. if we know the error has been
        // reported to the
        // right of the tag, then we can assume we need to step back
        // to the previous
        // region to land at the 'correct location. Otherwise assume
        // we're
        // exactly where we need to be.
        if (ERROR_SIDE_LEFT.equals(errorSide)) {
          region = prevRegion;
        }
      }

      // initialize start and end positions to be the start positions
      // this means if the
      // special case is not taken care of below the start and end
      // offset are set to be
      // the start of the region where the error was
      if (region != null) {
        startEndPositions[0] = region.getStartOffset();
        startEndPositions[1] = startEndPositions[0];
      } else { // this will message will not get added to the IReporter
        // since the length is 0
        startEndPositions[0] = 0;
        startEndPositions[1] = 0;
      }
      if (region instanceof Node) {
        Node node = (Node) region;

        if (START_TAG.equals(selectionStrategy)) {// then we want to
          // underline the opening tag
          if (node.getNodeType() == Node.ELEMENT_NODE) {
            IDOMElement element = (IDOMElement) node;
            startEndPositions[0] = element.getStartOffset() + 1;
            startEndPositions[1] = startEndPositions[0] + element.getTagName().length();
          }
        } else if (END_TAG.equals(selectionStrategy)) {// then we want to
          // underline the end tag
          if (node.getNodeType() == Node.ELEMENT_NODE) {
            IDOMElement element = (IDOMElement) node;
            startEndPositions[0] = element.getEndStartOffset();
            startEndPositions[1] = element.getEndOffset();
          }
        } else if (ATTRIBUTE_NAME.equals(selectionStrategy)) { // in
          // underline the attribute's name
          if (node.getNodeType() == Node.ELEMENT_NODE) {
            IDOMElement element = (IDOMElement) node;
            IDOMNode attributeNode = (IDOMNode) (element.getAttributeNode(nameOrValue));
            if (attributeNode != null) {
              startEndPositions[0] = attributeNode.getStartOffset();
              startEndPositions[1] = attributeNode.getStartOffset() + nameOrValue.length();
            }
          }
        } else if (ATTRIBUTE_VALUE.equals(selectionStrategy)) {
          // underline the attribute's value
          if (node.getNodeType() == Node.ELEMENT_NODE) {
            IDOMElement element = (IDOMElement) node;
            IDOMAttr attributeNode = (IDOMAttr) (element.getAttributeNode(nameOrValue));
            if (attributeNode != null) {
              startEndPositions[0] = attributeNode.getValueRegionStartOffset();
              String valueRegionText = attributeNode.getValueRegionText();
              int valueRegionLength = valueRegionText == null ? 0 : valueRegionText.length();
              startEndPositions[1] = startEndPositions[0] + valueRegionLength;
            }
          }
        } else if (ALL_ATTRIBUTES.equals(selectionStrategy)) {
          // underline all attributes
          if (node.getNodeType() == Node.ELEMENT_NODE) {
            IDOMElement element = (IDOMElement) node;
            NamedNodeMap attributes = element.getAttributes();
            if (attributes != null) {
              IDOMNode first = (IDOMNode) attributes.item(0);
              IDOMNode last = (IDOMNode) attributes.item(attributes.getLength() - 1);
              if ((first != null) && (last != null)) {
                startEndPositions[0] = first.getStartOffset();
                startEndPositions[1] = last.getEndOffset();
              }
            }
          }
        } else if (TEXT.equals(selectionStrategy)) {
          // underline the text between the tags
          if (node.getNodeType() == Node.TEXT_NODE) {
            IDOMText textNode = (IDOMText) node;
            int start = textNode.getStartOffset();
            String value = textNode.getNodeValue();
            int index = 0;
            char curChar = value.charAt(index);
            // here we are finding start offset by skipping over
            // whitespace:
            while ((curChar == '\n') || (curChar == '\t') || (curChar == '\r') || (curChar == ' ')) {
              curChar = value.charAt(index);
              index++;
            }
            if (index > 0) {
              index--;

            }
            start = start + index;
            startEndPositions[0] = start;
            startEndPositions[1] = start + value.trim().length();
          } else if (node.getNodeType() == Node.ELEMENT_NODE) {
            IDOMElement element = (IDOMElement) node;
            Node child = element.getFirstChild();
            if (child instanceof IDOMNode) {
              IDOMNode xmlChild = ((IDOMNode) child);
              startEndPositions[0] = xmlChild.getStartOffset();
              startEndPositions[1] = xmlChild.getEndOffset();
            }
          }
        } else if (FIRST_NON_WHITESPACE_TEXT.equals(selectionStrategy)) {
          // search through all child nodes and return range of
          // first non-whitespace
          // text node
          if (node.getNodeType() == Node.ELEMENT_NODE) {
            NodeList nodes = node.getChildNodes();
            for (int i = 0; i < nodes.getLength(); i++) {
              Node currentNode = nodes.item(i);
              if (currentNode.getNodeType() == Node.TEXT_NODE) {
                // TODO (Trung) I don't think we should call
                // getNodeValue(), trim(), length()
                // repeatedly.
                // This is inefficient, to improve use local
                // variables to store values.
                IDOMText textNode = (IDOMText) currentNode;
                if (textNode.getNodeValue().trim().length() > 0) {
                  String value = textNode.getNodeValue();
                  int index = 0;
                  int start = textNode.getStartOffset();
                  char curChar = value.charAt(index);
                  // here we are finding start offset by
                  // skipping over whitespace:
                  while ((curChar == '\n') || (curChar == '\t') || (curChar == '\r')
                      || (curChar == ' ')) {
                    curChar = value.charAt(index);
                    index++;
                  }
                  if (index > 0) {
                    index--;

                  }
                  start = start + index;
                  startEndPositions[0] = start;
                  startEndPositions[1] = start + value.trim().length();
                  break;
                }
              }

            }
          }
        }

        else if (TEXT_ENTITY_REFERENCE.equals(selectionStrategy)) {
          if (node.getNodeType() == Node.ENTITY_REFERENCE_NODE) {
            startEndPositions[0] = region.getStartOffset();
            startEndPositions[1] = region.getEndOffset();
          } else if (node.getNodeType() == Node.ELEMENT_NODE) {
            /*
             * In this case the undeclared entity might be in one of the attribute values. Search
             * through the attributes to find the range of the undeclared entity.
             */
            String entity = "&" + nameOrValue + ";"; //$NON-NLS-1$ //$NON-NLS-2$
            NamedNodeMap attributes = node.getAttributes();
            for (int i = 0; i < attributes.getLength(); i++) {
              IDOMAttr attr = (IDOMAttr) attributes.item(i);
              String nodeValue = attr.getNodeValue();
              int index = nodeValue.indexOf(entity);
              if (index != -1) {
                startEndPositions[0] = attr.getValueRegionStartOffset() + index + 1;
                startEndPositions[1] = startEndPositions[0] + entity.length();
              }
            }
          }

        } else if (VALUE_OF_ATTRIBUTE_WITH_GIVEN_VALUE.equals(selectionStrategy)) {
          // TODO (Trung) do we really need this strategy ?
          // If we know the name of the name of the attribute, we
          // can retrieve its value.
          // Hence, we can incoperate this strategy with
          // ATTRIBUTE_VALUE ?
          if (node.getNodeType() == Node.ELEMENT_NODE) {
            // here we will search through all attributes for the
            // one with the
            // with the value we want:
            // TODO (Trung) I see a potential problem here.
            // What happens when there is another attribute having
            // the same value
            // with this attribute's buggy value ?
            // Need to solve when time permits.
            NamedNodeMap attributes = node.getAttributes();
            for (int i = 0; i < attributes.getLength(); i++) {
              IDOMAttr attr = (IDOMAttr) attributes.item(i);
              String nodeValue = attr.getNodeValue().trim();
              if (nodeValue.equals(nameOrValue)) {
                startEndPositions[0] = attr.getValueRegionStartOffset() + 1;
                startEndPositions[1] = startEndPositions[0] + nodeValue.length();
                break;
              }
            }
          }
        } else if (ATTRIBUTE_NAME_LAST.equals(selectionStrategy)) {
          // underline the last attribute's name
          if (node.getNodeType() == Node.ELEMENT_NODE) {
            NamedNodeMap attributeMap = node.getAttributes();
            final int length = attributeMap.getLength();
            Node tempNode = null;
            Node attrNode = null;
            for (int i = 0; i < length; i++) {
              tempNode = attributeMap.item(i);
              if (tempNode != null && tempNode.getNodeName().equals(nameOrValue)) {
                attrNode = tempNode;
              }
            }
            IDOMNode attributeNode = (IDOMNode) (attrNode);
            if (attributeNode != null) {
              startEndPositions[0] = attributeNode.getStartOffset();
              startEndPositions[1] = attributeNode.getStartOffset() + nameOrValue.length();
            }
          }
        }
      }
      return startEndPositions;
    }
    // catch (Exception e) { // e.printStackTrace();
    // }
    finally {
    }
    // return null;
  }

  /**
   * Returns true if delegate validator is enabled based on Validation preferences
   * 
   * @param file
   * @return false if delegate validator is not enabled based on Validatoin preferences, true
   *         otherwise
   */
  protected boolean isDelegateValidatorEnabled(IFile file) {
    return true;
  }

  protected String getErrorSide(IMessage message) {
    // note that if the ERROR_SIDE is unspecified we return the default
    // value ERROR_SIDE_LEFT
    Object value = message.getAttribute(ERROR_SIDE);
    return ERROR_SIDE_RIGHT.equals(value) ? ERROR_SIDE_RIGHT : ERROR_SIDE_LEFT;
  }
}
