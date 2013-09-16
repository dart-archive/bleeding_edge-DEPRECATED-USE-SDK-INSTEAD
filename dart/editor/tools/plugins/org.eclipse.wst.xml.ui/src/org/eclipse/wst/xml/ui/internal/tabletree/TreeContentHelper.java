/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/

package org.eclipse.wst.xml.ui.internal.tabletree;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.xml.core.internal.contentmodel.util.DOMWriter;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMNode;
import org.w3c.dom.Attr;
import org.w3c.dom.CharacterData;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.ProcessingInstruction;
import org.w3c.dom.Text;

/**
 * This performs the work of taking a DOM tree and converting it to a displayable 'UI' tree. For
 * example : - white space text nodes are ommited from the 'UI' tree - adjacent Text and
 * EntityReference nodes are combined into a single 'UI' node - Elements with 'text only' children
 * are diplayed without children
 */
public class TreeContentHelper {

  public static final int HIDE_WHITE_SPACE_TEXT_NODES = 8;
  public static final int COMBINE_ADJACENT_TEXT_AND_ENTITY_REFERENCES = 16;
  public static final int HIDE_ELEMENT_CHILD_TEXT_NODES = 32;

  protected int style = HIDE_WHITE_SPACE_TEXT_NODES | COMBINE_ADJACENT_TEXT_AND_ENTITY_REFERENCES
      | HIDE_ELEMENT_CHILD_TEXT_NODES;

  /**
	 * 
	 */
  public boolean hasStyleFlag(int flag) {
    return (style & flag) != 0;
  }

  /**
	 * 
	 */
  public Object[] getChildren(Object element) {
    Object[] result = null;

    if (element instanceof Node) {
      Node node = (Node) element;
      List list = new ArrayList();
      boolean textContentOnly = true;

      NamedNodeMap map = node.getAttributes();
      if (map != null) {
        int length = map.getLength();
        for (int i = 0; i < length; i++) {
          list.add(map.item(i));
          textContentOnly = false;
        }
      }

      Node prevIncludedNode = null;
      for (Node childNode = node.getFirstChild(); childNode != null; childNode = childNode.getNextSibling()) {
        int childNodeType = childNode.getNodeType();
        boolean includeNode = true;

        if (includeNode && hasStyleFlag(HIDE_WHITE_SPACE_TEXT_NODES)) {
          if (isIgnorableText(childNode)) {
            // filter out the ignorable text node
            includeNode = false;
          }
        }

        if (includeNode && hasStyleFlag(COMBINE_ADJACENT_TEXT_AND_ENTITY_REFERENCES)) {
          if (isTextOrEntityReferenceNode(childNode) && (prevIncludedNode != null)
              && isTextOrEntityReferenceNode(prevIncludedNode)) {
            // we only show the first of a list of adjacent text
            // or entity reference node in the tree
            // so we filter out this subsequent one
            includeNode = false;
          }
        }

        if (hasStyleFlag(HIDE_ELEMENT_CHILD_TEXT_NODES)) {
          if ((childNodeType != Node.TEXT_NODE) && (childNodeType != Node.ENTITY_REFERENCE_NODE)) {
            textContentOnly = false;
          }
        }

        if (includeNode) {
          list.add(childNode);
          prevIncludedNode = childNode;
        }
      }

      if (hasStyleFlag(HIDE_ELEMENT_CHILD_TEXT_NODES) && textContentOnly) {
        result = new Object[0];
      } else {
        result = list.toArray();
      }
    }
    return result;
  }

  /**
	 * 
	 */
  protected boolean isTextOrEntityReferenceNode(Node node) {
    return (node.getNodeType() == Node.TEXT_NODE)
        || (node.getNodeType() == Node.ENTITY_REFERENCE_NODE);
  }

  /**
	 * 
	 */
  public boolean isIgnorableText(Node node) {
    boolean result = false;
    if (node.getNodeType() == Node.TEXT_NODE) {
      String data = ((Text) node).getData();
      result = ((data == null) || (data.trim().length() == 0));
    }
    return result;
  }

  /**
	 * 
	 */
  public boolean isCombinedTextNode(Node node) {
    boolean result = false;
    if (node.getNodeType() == Node.TEXT_NODE) {
      Node nextNode = node.getNextSibling();
      if (nextNode != null) {
        if (nextNode.getNodeType() == Node.ENTITY_REFERENCE_NODE) {
          result = true;
        }
      }
    } else if (node.getNodeType() == Node.ENTITY_REFERENCE_NODE) {
      result = true;
    }
    return result;
  }

  /**
	 * 
	 */
  public List getCombinedTextNodeList(Node theNode) {
    List list = new Vector();
    boolean prevIsEntity = false;
    for (Node node = theNode; node != null; node = node.getNextSibling()) {
      int nodeType = node.getNodeType();
      if (nodeType == Node.ENTITY_REFERENCE_NODE) {
        prevIsEntity = true;
        list.add(node);
      } else if ((nodeType == Node.TEXT_NODE) && (prevIsEntity || (node == theNode))) {
        prevIsEntity = false;
        list.add(node);
      } else {
        break;
      }
    }
    return list;
  }

  public String getElementTextValue(Element element) {
    List list = _getElementTextContent(element);
    return list != null ? getValueForTextContent(list) : null;
  }

  public void setElementTextValue(Element element, String value) {
    setElementNodeValue(element, value);
  }

  private List _getElementTextContent(Element element) {
    List result = null;

    for (Node node = element.getFirstChild(); node != null; node = node.getNextSibling()) {
      if ((node.getNodeType() == Node.TEXT_NODE)
          || (node.getNodeType() == Node.ENTITY_REFERENCE_NODE)) {
        if (result == null) {
          result = new Vector();
        }
        result.add(node);
      } else {
        result = null;
        break;
      }
    }
    return result;
  }

  /**
   * If the element is has 'text only' content this method will return the list of elements that
   * compose the text only content
   */
  public List getElementTextContent(Element element) {
    List result = null;
    if (!element.hasAttributes()) {
      result = _getElementTextContent(element);
    }
    return result;
  }

  /**
	 * 
	 */
  public String getNodeValue(Node node) {
    String result = null;
    int nodeType = node.getNodeType();
    switch (nodeType) {
      case Node.ATTRIBUTE_NODE: {
        result = ((Attr) node).getValue();
        break;
      }
      case Node.CDATA_SECTION_NODE:
        // drop thru
      case Node.COMMENT_NODE: {
        result = ((CharacterData) node).getData();
        break;
      }
      case Node.DOCUMENT_TYPE_NODE: {
        result = getDocumentTypeValue((DocumentType) node);
        break;
      }
      case Node.ELEMENT_NODE: {
        result = getElementNodeValue((Element) node);
        break;
      }
      case Node.ENTITY_REFERENCE_NODE:
        // drop thru
      case Node.TEXT_NODE: {
        result = getTextNodeValue(node);
        break;
      }
      case Node.PROCESSING_INSTRUCTION_NODE: {
        result = ((ProcessingInstruction) node).getData();
        break;
      }
    }
    return result;
  }

  /**
	 * 
	 */
  public void setNodeValue(Node node, String value) {
    setNodeValue(node, value, null);
  }

  /**
   * Checks that the resource backing the model is writeable utilizing <code>validateEdit</code> on
   * a given <tt>IWorkspace</tt>.
   * 
   * @param model the model to be checked
   * @param context the shell context for which <code>validateEdit</code> will be run
   * @return boolean result of checking <code>validateEdit</code>. If the resource is unwriteable,
   *         <code>status.isOK()</code> will return true; otherwise, false.
   */
  private boolean validateEdit(IStructuredModel model, Shell context) {
    if (model != null && model.getBaseLocation() != null) {
      IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(
          new Path(model.getBaseLocation()));
      return !file.isAccessible()
          || ResourcesPlugin.getWorkspace().validateEdit(new IFile[] {file}, context).isOK();
    }
    return false;
  }

  /**
	 * 
	 */
  public void setNodeValue(Node node, String value, Shell context) {
    // Model should not be edited because base location is read-only
    if (node instanceof IDOMNode && !validateEdit(((IDOMNode) node).getModel(), context)) {
      return;
    }
    int nodeType = node.getNodeType();
    try {
      switch (nodeType) {
        case Node.ATTRIBUTE_NODE: {
          ((Attr) node).setValue(value);
          break;
        }
        case Node.CDATA_SECTION_NODE:
          // drop thru
        case Node.COMMENT_NODE: {
          ((CharacterData) node).setData(value);
          break;
        }
        case Node.ELEMENT_NODE: {
          setElementNodeValue((Element) node, value);
          break;
        }
        case Node.ENTITY_REFERENCE_NODE:
          // drop thru
        case Node.TEXT_NODE: {
          setTextNodeValue(node, value);
          break;
        }
        case Node.PROCESSING_INSTRUCTION_NODE: {
          ((ProcessingInstruction) node).setData(value);
          break;
        }
      }
    } catch (DOMException e) {
      Display d = getDisplay();
      if (d != null) {
        d.beep();
      }
    }
  }

  private Display getDisplay() {

    return PlatformUI.getWorkbench().getDisplay();
  }

  /**
	 * 
	 */
  protected String getDocumentTypeValue(DocumentType documentType) {
    return DOMWriter.getDocumentTypeData(documentType);
  }

  /**
	 * 
	 */
  protected String getElementNodeValue(Element element) {
    String result = null;
    List list = getElementTextContent(element);
    if (list != null) {
      result = getValueForTextContent(list);
    }
    return result;
  }

  /**
	 * 
	 */
  protected void setElementNodeValue(Element element, String value) {
    List list = getElementTextContent(element);
    if (list != null) {
      setValueForTextContent(list, value);
    } else {
      Document document = element.getOwnerDocument();
      Text text = document.createTextNode(value);
      element.appendChild(text);
    }
  }

  /**
	 * 
	 */
  protected String getTextNodeValue(Node node) {
    String result = null;
    List list = null;
    if (isCombinedTextNode(node)) {
      list = getCombinedTextNodeList(node);
    } else {
      list = new Vector();
      list.add(node);
    }
    result = getValueForTextContent(list);
    return result;
  }

  /**
	 * 
	 */
  protected void setTextNodeValue(Node node, String value) {
    List list = null;
    if (isCombinedTextNode(node)) {
      list = getCombinedTextNodeList(node);
    } else {
      list = new Vector();
      list.add(node);
    }
    setValueForTextContent(list, value);
  }

  public Text getEffectiveTextNodeForCombinedNodeList(List list) {
    Text result = null;
    for (Iterator i = list.iterator(); i.hasNext();) {
      Node node = (Node) i.next();
      if (node.getNodeType() == Node.TEXT_NODE) {
        result = (Text) node;
        break;
      }
    }
    return result;
  }

  /**
	 * 
	 */
  protected String getValueForTextContent(List list) {
    String result = null;
    if (list.size() > 0) {
      if (list.get(0) instanceof IDOMNode) {
        IDOMNode first = (IDOMNode) list.get(0);
        IDOMNode last = (IDOMNode) list.get(list.size() - 1);
        IDOMModel model = first.getModel();
        int start = first.getStartOffset();
        int end = last.getEndOffset();
        try {
          result = model.getStructuredDocument().get(start, end - start);
        } catch (Exception e) {

        }
      } else {
        if (list.get(0) instanceof Node) {
          Node n = (Node) list.get(0);
          for (Node node = n.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node.getNodeType() == Node.TEXT_NODE) {
              String text = node.getNodeValue();
              if (!((text == null) || (text.trim().length() == 0))) {
                result = text.trim();
              }
            }
          }
        }
      }
    }

    // we trim the content so that it looks nice when viewed
    // we need to be carfull to preserve the 'trimmed' text when the value
    // is set (see setValueForTextContent)
    if (result != null) {
      result = result.trim();
    }
    return result;
  }

  /**
	 * 
	 */
  protected void setValueForTextContent(List list, String value) {
    // String oldValue = getValueForTextContent();
    // we worry about preserving trimmed text
    if (list.size() > 0) {
      if (list.get(0) instanceof IDOMNode) {
        IDOMNode first = (IDOMNode) list.get(0);
        IDOMNode last = (IDOMNode) list.get(list.size() - 1);
        int start = first.getStartOffset();
        int end = last.getEndOffset();
        first.getModel().getStructuredDocument().replaceText(this, start, end - start, value);
      }
    }
  }

  /**
	 * 
	 */
  public boolean isEditable(Node node) {
    int nodeType = node.getNodeType();
    boolean result = false;
    switch (nodeType) {
      case Node.ATTRIBUTE_NODE:
        // drop thru
      case Node.CDATA_SECTION_NODE:
        // drop thru
      case Node.COMMENT_NODE:
        // drop thru
      case Node.ENTITY_REFERENCE_NODE:
        // drop thru
      case Node.TEXT_NODE:
        // drop thru
      case Node.PROCESSING_INSTRUCTION_NODE: {
        result = true;
        break;
      }
      case Node.ELEMENT_NODE: {
        result = (getElementTextContent((Element) node) != null)
            || (node.getChildNodes().getLength() == 0);
        break;
      }
    }
    return result;
  }
}
