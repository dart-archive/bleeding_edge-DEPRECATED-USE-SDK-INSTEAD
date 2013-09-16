/*******************************************************************************
 * Copyright (c) 2001, 2010 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.actions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.xml.core.internal.contentmodel.CMAttributeDeclaration;
import org.eclipse.wst.xml.core.internal.contentmodel.CMDataType;
import org.eclipse.wst.xml.core.internal.contentmodel.CMDocument;
import org.eclipse.wst.xml.core.internal.contentmodel.CMElementDeclaration;
import org.eclipse.wst.xml.core.internal.contentmodel.CMNode;
import org.eclipse.wst.xml.core.internal.contentmodel.modelquery.ModelQuery;
import org.eclipse.wst.xml.core.internal.contentmodel.modelquery.ModelQueryAction;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;
import org.eclipse.wst.xml.ui.internal.XMLUIMessages;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ProcessingInstruction;

public abstract class BaseNodeActionManager {

  /**
   * MyMenuManager
   */
  public static class MyMenuManager extends MenuManager {
    protected String title;

    public MyMenuManager(String s) {
      super(s);
      title = s;
    }

    public boolean isEnabled() {
      return !isEmpty();
    }

    public String toString() {
      return title;
    }
  }

  public static DocumentType getDoctype(Node node) {
    Document document = (node.getNodeType() == Node.DOCUMENT_NODE) ? (Document) node
        : node.getOwnerDocument();
    return document.getDoctype();
  }

  protected MenuBuilder menuBuilder = new MenuBuilder();
  protected IStructuredModel fModel;
  protected ModelQuery modelQuery;

  protected BaseNodeActionManager(IStructuredModel model, ModelQuery modelQuery) {
    this.fModel = model;
    this.modelQuery = modelQuery;
  }

  protected void addActionHelper(IMenuManager menu, List modelQueryActionList) {
    List actionList = new Vector();
    for (Iterator i = modelQueryActionList.iterator(); i.hasNext();) {
      ModelQueryAction action = (ModelQueryAction) i.next();
      if (action.getCMNode() != null) {
        int cmNodeType = action.getCMNode().getNodeType();
        if (action.getKind() == ModelQueryAction.INSERT) {
          switch (cmNodeType) {
            case CMNode.ATTRIBUTE_DECLARATION: {
              actionList.add(createAddAttributeAction((Element) action.getParent(),
                  (CMAttributeDeclaration) action.getCMNode()));
              break;
            }
            case CMNode.ELEMENT_DECLARATION: {
              actionList.add(createAddElementAction(action.getParent(),
                  (CMElementDeclaration) action.getCMNode(), action.getStartIndex()));
              break;
            }
          }
        } else if (action.getKind() == ModelQueryAction.REPLACE) {
          if ((action.getParent() != null) && (action.getCMNode() != null)) {
            actionList.add(createReplaceAction(action.getParent(), action.getCMNode(),
                action.getStartIndex(), action.getEndIndex()));
          }
        }
      }
    }
    menuBuilder.populateMenu(menu, actionList, false);
  }

  protected void contributeAction(IMenuManager menu, Action action) {
    if (action != null) {
      menu.add(action);
    }
  }

  public void contributeActions(IMenuManager menu, List selection) {
    int editMode = modelQuery.getEditMode();
    int ic = ModelQuery.INCLUDE_CHILD_NODES;
    int vc = (editMode == ModelQuery.EDIT_MODE_CONSTRAINED_STRICT) ? ModelQuery.VALIDITY_STRICT
        : ModelQuery.VALIDITY_NONE;

    List implicitlySelectedNodeList = null;

    if (selection.size() > 0) {
      implicitlySelectedNodeList = getSelectedNodes(selection, true);

      // contribute delete actions
      contributeDeleteActions(menu, implicitlySelectedNodeList, ic, vc);
    }

    if (selection.size() == 1) {
      Node node = (Node) selection.get(0);

      // contribute edit actions
      contributeEditActions(menu, node);

      // contribute add child actions
      contributeAddChildActions(menu, node, ic, vc);

      // contribute add before actions
      contributeAddSiblingActions(menu, node, ic, vc);
    }

    if (selection.size() > 0) {
      // contribute replace actions
      contributeReplaceActions(menu, implicitlySelectedNodeList, ic, vc);
    }

    if (selection.size() == 0) {
      Document document = ((IDOMModel) fModel).getDocument();
      contributeAddDocumentChildActions(menu, document, ic, vc);
      contributeEditGrammarInformationActions(menu, document);
    }
  }

  protected boolean canContributeChildActions(Node node) {
    return true;
  }

  protected void contributeAddChildActions(IMenuManager menu, Node node, int ic, int vc) {
    int nodeType = node.getNodeType();

    if (nodeType == Node.ELEMENT_NODE && canContributeChildActions(node)) {
      // 'Add Child...' and 'Add Attribute...' actions
      //
      Element element = (Element) node;

      IMenuManager addAttributeMenu = new MyMenuManager(XMLUIMessages._UI_MENU_ADD_ATTRIBUTE);
      IMenuManager addChildMenu = new MyMenuManager(XMLUIMessages._UI_MENU_ADD_CHILD);
      menu.add(addAttributeMenu);
      menu.add(addChildMenu);

      CMElementDeclaration ed = modelQuery.getCMElementDeclaration(element);
      if (ed != null) {
        // add insert attribute actions
        //
        List modelQueryActionList = new ArrayList();
        modelQuery.getInsertActions(element, ed, -1, ModelQuery.INCLUDE_ATTRIBUTES, vc,
            modelQueryActionList);
        addActionHelper(addAttributeMenu, modelQueryActionList);
        // add insert child node actions
        //
        modelQueryActionList = new ArrayList();
        modelQuery.getInsertActions(element, ed, -1, ic, vc, modelQueryActionList);
        addActionHelper(addChildMenu, modelQueryActionList);
      }

      // add PI and COMMENT
      contributePIAndCommentActions(addChildMenu, element, ed, -1);

      // add PCDATA, CDATA_SECTION
      contributeTextNodeActions(addChildMenu, element, ed, -1);

      // add NEW ELEMENT
      contributeUnconstrainedAddElementAction(addChildMenu, element, ed, -1);

      // add ATTRIBUTE
      contributeUnconstrainedAttributeActions(addAttributeMenu, element, ed);
    }
  }

  protected void contributeAddDocumentChildActions(IMenuManager menu, Document document, int ic,
      int vc) {
    IMenuManager addChildMenu = new MyMenuManager(XMLUIMessages._UI_MENU_ADD_CHILD);
    menu.add(addChildMenu);

    // add PI and COMMENT
    contributePIAndCommentActions(addChildMenu, document, -1);

    // add NEW ELEMENT
    contributeUnconstrainedAddElementAction(addChildMenu, document, -1);
  }

  protected void contributeAddSiblingActions(IMenuManager menu, Node node, int ic, int vc) {
    IMenuManager addBeforeMenu = new MyMenuManager(XMLUIMessages._UI_MENU_ADD_BEFORE);
    IMenuManager addAfterMenu = new MyMenuManager(XMLUIMessages._UI_MENU_ADD_AFTER);
    menu.add(addBeforeMenu);
    menu.add(addAfterMenu);

    Node parentNode = node.getParentNode();
    if (parentNode != null) {
      int index = getIndex(parentNode, node);
      if (parentNode.getNodeType() == Node.ELEMENT_NODE) {
        Element parentElement = (Element) parentNode;
        CMElementDeclaration parentED = modelQuery.getCMElementDeclaration(parentElement);
        if (parentED != null) {
          // 'Add Before...' and 'Add After...' actions
          //
          List modelQueryActionList = new ArrayList();
          modelQuery.getInsertActions(parentElement, parentED, index, ic, vc, modelQueryActionList);
          addActionHelper(addBeforeMenu, modelQueryActionList);

          modelQueryActionList = new ArrayList();
          modelQuery.getInsertActions(parentElement, parentED, index + 1, ic, vc,
              modelQueryActionList);
          addActionHelper(addAfterMenu, modelQueryActionList);
        }

        // add COMMENT and PI before and after
        contributePIAndCommentActions(addBeforeMenu, parentElement, parentED, index);
        contributePIAndCommentActions(addAfterMenu, parentElement, parentED, index + 1);

        // add PCDATA, CDATA_SECTION before and after
        contributeTextNodeActions(addBeforeMenu, parentElement, parentED, index);
        contributeTextNodeActions(addAfterMenu, parentElement, parentED, index + 1);

        // add NEW ELEMENT before and after
        contributeUnconstrainedAddElementAction(addBeforeMenu, parentElement, parentED, index);
        contributeUnconstrainedAddElementAction(addAfterMenu, parentElement, parentED, index + 1);
      } else if (parentNode.getNodeType() == Node.DOCUMENT_NODE) {
        Document document = (Document) parentNode;
        CMDocument cmDocument = modelQuery.getCorrespondingCMDocument(parentNode);
        if (cmDocument != null) {
          // add possible root element insertions
          //        
          List modelQueryActionList = new ArrayList();
          modelQuery.getInsertActions(document, cmDocument, index, ic, vc, modelQueryActionList);
          addActionHelper(addAfterMenu, modelQueryActionList);

          modelQueryActionList = new ArrayList();
          modelQuery.getInsertActions(document, cmDocument, index + 1, ic, vc, modelQueryActionList);
          addActionHelper(addAfterMenu, modelQueryActionList);
        }

        // add COMMENT and PI before and after
        contributePIAndCommentActions(addBeforeMenu, document, index);
        contributePIAndCommentActions(addAfterMenu, document, index + 1);

        // add ELEMENT before and after
        contributeUnconstrainedAddElementAction(addBeforeMenu, document, index);
        contributeUnconstrainedAddElementAction(addAfterMenu, document, index + 1);
      }
    }
  }

  protected void contributeDeleteActions(IMenuManager menu, List list, int ic, int vc) {
    boolean canRemove = modelQuery.canRemove(list, vc);

    // a delete action with an empty list will produce a disabled menu
    // item
    //
    List resultList = canRemove ? list : Collections.EMPTY_LIST;
    contributeAction(menu, createDeleteAction(resultList));
  }

  protected void contributeEditActions(IMenuManager menu, Node node) {
    contributeEditGrammarInformationActions(menu, node);

    if (node.getNodeType() == Node.PROCESSING_INSTRUCTION_NODE) {
      contributeAction(menu, createEditProcessingInstructionAction((ProcessingInstruction) node));
    } else if (node.getNodeType() == Node.ATTRIBUTE_NODE) {
      contributeAction(menu, createEditAttributeAction((Attr) node, null));
    }
  }

  protected void contributeEditGrammarInformationActions(IMenuManager menu, Node node) {
    Document document = node.getNodeType() == Node.DOCUMENT_NODE ? (Document) node
        : node.getOwnerDocument();

    DocumentType doctype = getDoctype(node);
    if (doctype == null) {
      contributeAction(menu, createAddDoctypeAction(document, -1));
    }

    if (node.getNodeType() == Node.DOCUMENT_TYPE_NODE) {
      contributeAction(menu, createEditDoctypeAction((DocumentType) node));
    }

    if ((doctype == null) && (getRootElement(document) != null)) {
      contributeAction(menu, createEditSchemaInfoAction(getRootElement(document)));
    }
  }

  protected void contributePIAndCommentActions(IMenuManager menu, Document document, int index) {
    // test to make sure that the index isn't before the XML declaration
    // 
    contributeAction(menu, createAddCommentAction(document, index));
    contributeAction(menu, createAddProcessingInstructionAction(document, index));
  }

  protected void contributePIAndCommentActions(IMenuManager menu, Element parentElement,
      CMElementDeclaration parentEd, int index) {
    if ((parentEd == null) || isCommentAllowed(parentEd)) {
      contributeAction(menu, createAddCommentAction(parentElement, index));
      contributeAction(menu, createAddProcessingInstructionAction(parentElement, index));
    }
  }

  protected void contributeReplaceActions(IMenuManager menu, List selectedNodeList, int ic, int vc) {
    // 'Replace With...' actions
    //                                                                                                                   
    IMenuManager replaceWithMenu = new MyMenuManager(XMLUIMessages._UI_MENU_REPLACE_WITH);
    menu.add(replaceWithMenu);

    if ((modelQuery.getEditMode() == ModelQuery.EDIT_MODE_CONSTRAINED_STRICT)
        && (selectedNodeList.size() > 0)) {
      Node node = (Node) selectedNodeList.get(0);
      Node parentNode = node.getParentNode();
      if ((parentNode != null) && (parentNode.getNodeType() == Node.ELEMENT_NODE)) {
        Element parentElement = (Element) parentNode;
        CMElementDeclaration parentED = modelQuery.getCMElementDeclaration(parentElement);
        if (parentED != null) {
          List replaceActionList = new Vector();
          modelQuery.getReplaceActions(parentElement, parentED, selectedNodeList, ic, vc,
              replaceActionList);
          addActionHelper(replaceWithMenu, replaceActionList);
        }
      }
    }
  }

  protected void contributeTextNodeActions(IMenuManager menu, Element parentElement,
      CMElementDeclaration parentEd, int index) {
    if ((parentEd == null) || isTextAllowed(parentEd)) {
      CMDataType dataType = parentEd != null ? parentEd.getDataType() : null;
      contributeAction(menu, createAddPCDataAction(parentElement, dataType, index));
      contributeAction(menu, createAddCDataSectionAction(parentElement, index));
    }
  }

  protected void contributeUnconstrainedAddElementAction(IMenuManager menu, Document document,
      int index) {
    if (isUnconstrainedActionAllowed()) {
      if (getRootElement(document) == null) {
        int xmlDeclarationIndex = -1;
        int doctypeIndex = -1;
        NodeList nodeList = document.getChildNodes();
        int nodeListLength = nodeList.getLength();
        for (int i = 0; i < nodeListLength; i++) {
          Node node = nodeList.item(i);
          int nodeType = node.getNodeType();
          if (nodeType == Node.DOCUMENT_TYPE_NODE) {
            doctypeIndex = i;
            break;
          } else if (nodeType == Node.PROCESSING_INSTRUCTION_NODE) {
            ProcessingInstruction pi = (ProcessingInstruction) node;
            if (pi.getTarget().equalsIgnoreCase("xml") && (xmlDeclarationIndex == -1)) { //$NON-NLS-1$
              xmlDeclarationIndex = i;
            }
          }
        }

        if (((xmlDeclarationIndex == -1) || (index > xmlDeclarationIndex))
            && ((doctypeIndex == -1) || (index > doctypeIndex))) {
          contributeAction(menu, createAddElementAction(document, null, index));
        }
      }
    }
  }

  protected void contributeUnconstrainedAddElementAction(IMenuManager menu, Element parentElement,
      CMElementDeclaration parentEd, int index) {
    if (isUnconstrainedActionAllowed()) {
      if ((parentEd == null)
          || (parentEd.getProperty("isInferred") == Boolean.TRUE) || ((modelQuery.getEditMode() != ModelQuery.EDIT_MODE_CONSTRAINED_STRICT) && isElementAllowed(parentEd))) { //$NON-NLS-1$
        contributeAction(menu, createAddElementAction(parentElement, null, index));
      }
    }
  }

  protected void contributeUnconstrainedAttributeActions(IMenuManager menu, Element parentElement,
      CMElementDeclaration parentEd) {
    if (isUnconstrainedActionAllowed()) {
      if ((parentEd == null)
          || (parentEd.getProperty("isInferred") == Boolean.TRUE) || (modelQuery.getEditMode() != ModelQuery.EDIT_MODE_CONSTRAINED_STRICT)) { //$NON-NLS-1$
        contributeAction(menu, createAddAttributeAction(parentElement, null));
      }
    }
  }

  abstract protected Action createAddAttributeAction(Element parent, CMAttributeDeclaration ad);

  abstract protected Action createAddCDataSectionAction(Node parent, int index);

  abstract protected Action createAddCommentAction(Node parent, int index);

  abstract protected Action createAddDoctypeAction(Document parent, int index);

  abstract protected Action createAddElementAction(Node parent, CMElementDeclaration ed, int index);

  abstract protected Action createAddPCDataAction(Node parent, CMDataType dataType, int index);

  abstract protected Action createAddProcessingInstructionAction(Node parent, int index);

  abstract protected Action createAddSchemaInfoAction(Element element);

  abstract protected Action createDeleteAction(List selection);

  abstract protected Action createEditAttributeAction(Attr attribute, CMAttributeDeclaration ad);

  abstract protected Action createEditDoctypeAction(DocumentType doctype);

  abstract protected Action createEditProcessingInstructionAction(ProcessingInstruction pi);

  abstract protected Action createEditSchemaInfoAction(Element element);

  abstract protected Action createRenameAction(Node node);

  abstract protected Action createReplaceAction(Node parent, CMNode cmnode, int startIndex,
      int endIndex);

  public int getIndex(Node parentNode, Node child) {
    NodeList nodeList = parentNode.getChildNodes();
    int index = -1;
    int size = nodeList.getLength();
    for (int i = 0; i < size; i++) {
      if (nodeList.item(i) == child) {
        index = i;
        break;
      }
    }
    return index;
  }

  public Node getRefChildNodeAtIndex(Node parent, int index) {
    NodeList nodeList = parent.getChildNodes();
    Node refChild = ((index >= 0) && (index < nodeList.getLength())) ? nodeList.item(index) : null;
    return refChild;
  }

  protected Element getRootElement(Document document) {
    Element result = null;
    NodeList nodeList = document.getChildNodes();
    int nodeListLength = nodeList.getLength();
    for (int i = 0; i < nodeListLength; i++) {
      Node node = nodeList.item(i);
      if (node.getNodeType() == Node.ELEMENT_NODE) {
        result = (Element) node;
        break;
      }
    }
    return result;
  }

  protected List getSelectedNodes(List list, boolean includeTextNodes) {
    List result = new ArrayList(0);
    for (Iterator i = list.iterator(); i.hasNext();) {
      Object object = i.next();
      if (object instanceof Node) {
        Node node = (Node) object;
        if (node.getNodeType() == Node.TEXT_NODE) {
          if (includeTextNodes) {
            result.add(object);
          }
        } else {
          result.add(node);
        }
      }
    }
    return result;
  }

  protected boolean isCommentAllowed(CMElementDeclaration parentEd) {
    int contentType = parentEd.getContentType();
    return (contentType == CMElementDeclaration.ELEMENT)
        || (contentType == CMElementDeclaration.MIXED)
        || (contentType == CMElementDeclaration.PCDATA)
        || (contentType == CMElementDeclaration.ANY);
  }

  protected boolean isElementAllowed(CMElementDeclaration parentEd) {
    int contentType = parentEd.getContentType();
    return (contentType == CMElementDeclaration.ELEMENT)
        || (contentType == CMElementDeclaration.MIXED) || (contentType == CMElementDeclaration.ANY);
  }

  protected boolean isTextAllowed(CMElementDeclaration parentEd) {
    int contentType = parentEd.getContentType();
    return (contentType == CMElementDeclaration.MIXED)
        || (contentType == CMElementDeclaration.PCDATA)
        || (contentType == CMElementDeclaration.ANY);
  }

  protected boolean isUnconstrainedActionAllowed() {
    return true;
  }

  protected boolean isWhitespaceTextNode(Node node) {
    return (node != null) && (node.getNodeType() == Node.TEXT_NODE)
        && (node.getNodeValue().trim().length() == 0);
  }
}
