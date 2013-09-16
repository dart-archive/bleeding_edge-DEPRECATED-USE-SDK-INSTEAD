/*******************************************************************************
 * Copyright (c) 2001, 2011 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.properties;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySheetEntry;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.IPropertySource2;
import org.eclipse.ui.views.properties.TextPropertyDescriptor;
import org.eclipse.wst.sse.core.internal.provisional.INodeNotifier;
import org.eclipse.wst.sse.ui.views.properties.IPropertySourceExtension;
import org.eclipse.wst.xml.core.internal.contentmodel.CMAttributeDeclaration;
import org.eclipse.wst.xml.core.internal.contentmodel.CMDataType;
import org.eclipse.wst.xml.core.internal.contentmodel.CMElementDeclaration;
import org.eclipse.wst.xml.core.internal.contentmodel.CMNamedNodeMap;
import org.eclipse.wst.xml.core.internal.contentmodel.CMNode;
import org.eclipse.wst.xml.core.internal.contentmodel.basic.CMNamedNodeMapImpl;
import org.eclipse.wst.xml.core.internal.contentmodel.modelquery.ModelQuery;
import org.eclipse.wst.xml.core.internal.contentmodel.util.DOMNamespaceHelper;
import org.eclipse.wst.xml.core.internal.document.DocumentTypeAdapter;
import org.eclipse.wst.xml.core.internal.modelquery.ModelQueryUtil;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMNode;
import org.eclipse.wst.xml.ui.internal.Logger;
import org.eclipse.wst.xml.ui.internal.XMLUIMessages;
import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * An IPropertySource implementation for a JFace viewer used to display properties of DOM nodes.
 */
public class XMLPropertySource implements IPropertySource, IPropertySourceExtension,
    IPropertySource2 {
  protected final static String CATEGORY_ATTRIBUTES = XMLUIMessages.XMLPropertySourceAdapter_0;

  /**
   * Controls whether optional attributes are marked as for "experts"
   */
  private static final boolean fSetExpertFilter = false;

  /**
   * Controls whether to derive categories from CMDataTypes; disabled by default until display
   * strings can be planned
   */
  private boolean fShouldDeriveCategories = false;

  private final static boolean fSortEnumeratedValues = true;

  /**
   * Note: we want the default fCaseSensitive to be true, but, to avoid meaningless double
   * initialization, we leave default here, and set in constructor only.
   */
  private boolean fCaseSensitive;
  private IPropertyDescriptor[] fDescriptors = null;
  private Node fNode = null;

  private Stack fValuesBeingSet = new Stack();

  public XMLPropertySource(INodeNotifier target) {
    super();
    fNode = initNode(target);
    fCaseSensitive = initCaseSensitive(fNode);
  }

  public XMLPropertySource(Node target, boolean useCategories) {
    super();
    initNode(target);
    fNode = target;
    fCaseSensitive = initCaseSensitive(fNode);
    fShouldDeriveCategories = useCategories;
  }

  /** Separate method just to isolate error processing */
  private static INodeNotifier initNode(Node target) {
    if (target instanceof INodeNotifier) {
      return (INodeNotifier) target;
    }
    throw new IllegalArgumentException("XMLPropertySource is only for INodeNotifiers"); //$NON-NLS-1$
  }

  /** Separate method just to isolate error processing */
  private static Node initNode(INodeNotifier target) {
    if (target instanceof Node) {
      return (Node) target;
    }
    throw new IllegalArgumentException("XMLPropertySource is only for W3C DOM Nodes"); //$NON-NLS-1$
  }

  private boolean initCaseSensitive(Node node) {
    // almost all tags are case sensitive, except that old HTML
    boolean caseSensitive = true;
    if (node instanceof IDOMNode) {
      DocumentTypeAdapter adapter = getDocTypeFromDOMNode(node);
      if (adapter != null) {
        caseSensitive = (adapter.getTagNameCase() == DocumentTypeAdapter.STRICT_CASE);
      }
    }
    return caseSensitive;
  }

  /**
   * by "internal spec" the DOCTYPE adapter is only available from Document Node
   * 
   * @return {@link DocumentTypeAdapter}
   */
  private DocumentTypeAdapter getDocTypeFromDOMNode(Node node) {
    DocumentTypeAdapter adapter = null;
    Document ownerDocument = node.getOwnerDocument();
    if (ownerDocument == null) {
      // if ownerDocument is null, then fNode must be the Document Node
      // [old, old comment]
      // hmmmm, guess not. See
      // https://bugs.eclipse.org/bugs/show_bug.cgi?id=130233
      // guess this is used for many INodeNotifiers, not just XML.
      // (and DTD's use IDOMNode? ... that doesn't sound quite right
      // ... but, maybe a separate issue).
      if (node instanceof Document) {
        ownerDocument = (Document) node;
      }
    }
    if (ownerDocument != null) {
      adapter = (DocumentTypeAdapter) ((INodeNotifier) ownerDocument).getAdapterFor(DocumentTypeAdapter.class);
    }

    return adapter;
  }

  private String[] _getValidFixedStrings(CMAttributeDeclaration attrDecl, CMDataType helper) {
    String attributeName = DOMNamespaceHelper.computeName(attrDecl, fNode, null);
    List values = new ArrayList(1);
    String impliedValue = helper.getImpliedValue();
    if (impliedValue != null) {
      values.add(impliedValue);
    }
    boolean checkIfCurrentValueIsIncluded = ((fNode.getAttributes() != null)
        && (fNode.getAttributes().getNamedItem(attributeName) != null) && (fNode.getAttributes().getNamedItem(
        attributeName).getNodeValue() != null));
    if (checkIfCurrentValueIsIncluded) {
      String currentValue = null;
      currentValue = fNode.getAttributes().getNamedItem(attributeName).getNodeValue();
      if (!currentValue.equals(impliedValue)) {
        values.add(currentValue);
      }
    }
    String[] validStrings = new String[values.size()];
    validStrings = (String[]) values.toArray(validStrings);
    return validStrings;
  }

  private String[] _getValidStrings(CMAttributeDeclaration attrDecl, CMDataType valuesHelper) {
    String attributeName = DOMNamespaceHelper.computeName(attrDecl, fNode, null);
    List values = new ArrayList(1);
    boolean currentValueKnown = false;
    boolean checkIfCurrentValueIsKnown = ((fNode.getAttributes() != null)
        && (fNode.getAttributes().getNamedItem(attributeName) != null) && (fNode.getAttributes().getNamedItem(
        attributeName).getNodeValue() != null));
    String currentValue = null;
    if (checkIfCurrentValueIsKnown) {
      currentValue = fNode.getAttributes().getNamedItem(attributeName).getNodeValue();
    }

    if ((valuesHelper.getImpliedValueKind() == CMDataType.IMPLIED_VALUE_FIXED)
        && (valuesHelper.getImpliedValue() != null)) {
      // FIXED value
      currentValueKnown = (currentValue != null)
          && valuesHelper.getImpliedValue().equals(currentValue);
      values.add(valuesHelper.getImpliedValue());
    } else {
      // ENUMERATED values
      String[] valueStrings = null;
      // valueStrings = valuesHelper.getEnumeratedValues();
      ModelQuery modelQuery = ModelQueryUtil.getModelQuery(fNode.getOwnerDocument());
      if ((modelQuery != null) && (fNode.getNodeType() == Node.ELEMENT_NODE)) {
        valueStrings = modelQuery.getPossibleDataTypeValues((Element) fNode, attrDecl);
      } else {
        valueStrings = attrDecl.getAttrType().getEnumeratedValues();
      }
      if (valueStrings != null) {
        for (int i = 0; i < valueStrings.length; i++) {
          if (checkIfCurrentValueIsKnown && valueStrings[i].equals(currentValue)) {
            currentValueKnown = true;
          }
          values.add(valueStrings[i]);
        }
      }
    }
    if ((valuesHelper.getImpliedValueKind() != CMDataType.IMPLIED_VALUE_NONE)
        && (valuesHelper.getImpliedValue() != null)) {
      if (!values.contains(valuesHelper.getImpliedValue())) {
        values.add(valuesHelper.getImpliedValue());
      }
    }

    if (checkIfCurrentValueIsKnown && !currentValueKnown && (currentValue != null)
        && (currentValue.length() > 0)) {
      values.add(currentValue);
    }
    String[] validStrings = new String[values.size()];
    validStrings = (String[]) values.toArray(validStrings);
    return validStrings;
  }

  private IPropertyDescriptor createDefaultPropertyDescriptor(String attributeName) {
    return createDefaultPropertyDescriptor(attributeName, false);
  }

  private IPropertyDescriptor createDefaultPropertyDescriptor(String attributeName,
      boolean hideOnFilter) {
    // The descriptor class used here is also used in
    // updatePropertyDescriptors()
    TextPropertyDescriptor descriptor = new TextPropertyDescriptor(attributeName, attributeName);
    descriptor.setCategory(getCategory(null, null));
    descriptor.setDescription(attributeName);
    if (hideOnFilter && fSetExpertFilter) {
      descriptor.setFilterFlags(new String[] {IPropertySheetEntry.FILTER_ID_EXPERT});
    }
    return descriptor;
  }

  /**
   * Creates a property descriptor for an attribute with ENUMERATED values - if the value does not
   * exist, an editable combo box is returned - if the value exists but is not one in the enumerated
   * list of value, a combo box featuring the current and correct values is returned - if the value
   * exists and it is a valid value, a combo box featuring the correct values with the current one
   * visible is returned
   */
  private IPropertyDescriptor createEnumeratedPropertyDescriptor(CMAttributeDeclaration attrDecl,
      CMDataType valuesHelper, Attr attr) {
    // the displayName MUST be set
    String attrName = DOMNamespaceHelper.computeName(attrDecl, fNode, null);
    EnumeratedStringPropertyDescriptor descriptor = new EnumeratedStringPropertyDescriptor(
        attrName, attrName, _getValidStrings(attrDecl, valuesHelper));
    descriptor.setCategory(getCategory(attrDecl, attr));
    descriptor.setDescription(attrName);
    if ((attrDecl.getUsage() != CMAttributeDeclaration.REQUIRED) && fSetExpertFilter) {
      descriptor.setFilterFlags(new String[] {IPropertySheetEntry.FILTER_ID_EXPERT});
    }
    return descriptor;
  }

  /**
   * Creates a property descriptor for an attribute with a FIXED value - if the value does not
   * exist, an editable combo box is returned - if the value exists but is not the fixed/default
   * value, a combo box featuring the current and correct value is returned - if the value exists
   * and it is the fixed/default value, no cell editor is provided "locking" the value in
   */
  private IPropertyDescriptor createFixedPropertyDescriptor(CMAttributeDeclaration attrDecl,
      CMDataType helper, Attr attr) {
    // the displayName MUST be set
    String attrName = DOMNamespaceHelper.computeName(attrDecl, fNode, null);
    EnumeratedStringPropertyDescriptor descriptor = new EnumeratedStringPropertyDescriptor(
        attrName, attrName, _getValidFixedStrings(attrDecl, helper));
    descriptor.setCategory(getCategory(attrDecl, attr));
    descriptor.setDescription(DOMNamespaceHelper.computeName(attrDecl, fNode, null));
    return descriptor;
  }

  protected IPropertyDescriptor createPropertyDescriptor(CMAttributeDeclaration attrDecl, Attr attr) {
    IPropertyDescriptor descriptor = null;
    CMDataType attrType = attrDecl.getAttrType();

    if (attrType != null) {
      // handle declarations that provide FIXED/ENUMERATED values
      if ((attrType.getEnumeratedValues() != null) && (attrType.getEnumeratedValues().length > 0)) {
        descriptor = createEnumeratedPropertyDescriptor(attrDecl, attrType, attr);
      } else if (((attrDecl.getUsage() == CMAttributeDeclaration.FIXED) || (attrType.getImpliedValueKind() == CMDataType.IMPLIED_VALUE_FIXED))
          && (attrType.getImpliedValue() != null)) {
        descriptor = createFixedPropertyDescriptor(attrDecl, attrType, attr);
      } else {
        // plain text
        descriptor = createTextPropertyDescriptor(attrDecl, attr);
      }
    } else {
      // no extra information given
      descriptor = createTextPropertyDescriptor(attrDecl, attr);
    }
    return descriptor;
  }

  /**
   * Returns the current collection of property descriptors.
   * 
   * @return all valid descriptors.
   */
  private IPropertyDescriptor[] createPropertyDescriptors() {
    CMNamedNodeMap attrMap = null;
    CMElementDeclaration ed = getDeclaration();
    if (ed != null) {
      attrMap = ed.getAttributes();
      CMNamedNodeMapImpl allAttributes = new CMNamedNodeMapImpl(attrMap);
      List nodes = ModelQueryUtil.getModelQuery(fNode.getOwnerDocument()).getAvailableContent(
          (Element) fNode, ed, ModelQuery.INCLUDE_ATTRIBUTES);
      for (int k = 0; k < nodes.size(); k++) {
        CMNode cmnode = (CMNode) nodes.get(k);
        if (cmnode.getNodeType() == CMNode.ATTRIBUTE_DECLARATION) {
          allAttributes.put(cmnode);
        }
      }
      attrMap = allAttributes;
    }

    List descriptorList = new ArrayList();
    List names = new ArrayList();
    IPropertyDescriptor descriptor;

    CMAttributeDeclaration attrDecl = null;

    // add descriptors for existing attributes
    NamedNodeMap attributes = fNode.getAttributes();
    if (attributes != null) {
      for (int i = 0; i < attributes.getLength(); i++) {
        Attr attr = (Attr) attributes.item(i);
        // if metainfo is present for this attribute, use the
        // CMAttributeDeclaration to derive a descriptor
        if (attrMap != null) {
          String attrName = attr.getName();
          if (fCaseSensitive) {
            attrDecl = (CMAttributeDeclaration) attrMap.getNamedItem(attrName);
          } else {
            attrDecl = null;
            for (int j = 0; j < attrMap.getLength(); j++) {
              if (!fCaseSensitive && attrMap.item(j).getNodeName().equalsIgnoreCase(attrName)) {
                attrDecl = (CMAttributeDeclaration) attrMap.item(j);
                break;
              }
            }
          }
        }
        // be consistent: if there's metainfo, use *that* as the
        // descriptor ID
        descriptor = null;
        if (attrDecl != null) {
          String attrName = DOMNamespaceHelper.computeName(attrDecl, fNode, null);
          if (!names.contains(attrName)) {
            descriptor = createPropertyDescriptor(attrDecl, attr);
            if (descriptor != null)
              names.add(attrName);
          }
        } else {
          if (!names.contains(attr.getName())) {
            descriptor = createDefaultPropertyDescriptor(attr.getName());
            if (descriptor != null)
              names.add(attr.getName());
          }
        }
        if (descriptor != null) {
          descriptorList.add(descriptor);
        }
      }
    }

    // add descriptors from the metainfo that are not yet listed
    if (attrMap != null) {
      for (int i = 0; i < attrMap.getLength(); i++) {
        attrDecl = (CMAttributeDeclaration) attrMap.item(i);
        String attrName = DOMNamespaceHelper.computeName(attrDecl, fNode, null);
        if (!names.contains(attrName)) {
          IPropertyDescriptor holdDescriptor = createPropertyDescriptor(attrDecl, null);
          if (holdDescriptor != null) {
            names.add(attrName);
            descriptorList.add(holdDescriptor);
          }
        }
      }
    }

    // add MQE-based descriptors
    if (ed != null && fNode.getNodeType() == Node.ELEMENT_NODE) {
      List nodes = ModelQueryUtil.getModelQuery(fNode.getOwnerDocument()).getAvailableContent(
          (Element) fNode, ed, ModelQuery.INCLUDE_ATTRIBUTES);
      for (int i = 0; i < nodes.size(); i++) {
        CMNode node = (CMNode) nodes.get(i);
        if (node.getNodeType() == CMNode.ATTRIBUTE_DECLARATION) {
          attrDecl = (CMAttributeDeclaration) node;
          String attrName = DOMNamespaceHelper.computeName(attrDecl, fNode, null);
          if (!names.contains(attrName)) {
            IPropertyDescriptor holdDescriptor = createPropertyDescriptor(attrDecl, null);
            if (holdDescriptor != null) {
              names.add(attrName);
              descriptorList.add(holdDescriptor);
            }
          }
        }
      }
    }

    IPropertyDescriptor[] descriptors = new IPropertyDescriptor[descriptorList.size()];
    for (int i = 0; i < descriptors.length; i++) {
      descriptors[i] = (IPropertyDescriptor) descriptorList.get(i);
    }
    return descriptors;
  }

  private IPropertyDescriptor createTextPropertyDescriptor(CMAttributeDeclaration attrDecl,
      Attr attr) {
    String attrName = DOMNamespaceHelper.computeName(attrDecl, fNode, null);
    TextPropertyDescriptor descriptor = new TextPropertyDescriptor(attrName, attrName);
    descriptor.setCategory(getCategory(attrDecl, attr));
    descriptor.setDescription(attrName);
    if ((attrDecl.getUsage() != CMAttributeDeclaration.REQUIRED) && fSetExpertFilter) {
      descriptor.setFilterFlags(new String[] {IPropertySheetEntry.FILTER_ID_EXPERT});
    }
    return descriptor;
  }

  private String getCategory(CMAttributeDeclaration attrDecl, Attr attr) {
    if (attr != null) {
      String namespaceURI = attr.getNamespaceURI();
      if (namespaceURI == null)
        namespaceURI = attr.getOwnerElement().getNamespaceURI();
      if (namespaceURI != null)
        return namespaceURI;
    }
    if (attrDecl != null) {
      if (attrDecl.supports("category")) { //$NON-NLS-1$
        return (String) attrDecl.getProperty("category"); //$NON-NLS-1$
      }
      if (fShouldDeriveCategories && (attrDecl.getAttrType() != null)
          && (attrDecl.getAttrType().getDataTypeName() != null)
          && (attrDecl.getAttrType().getDataTypeName().length() > 0)) {
        return attrDecl.getAttrType().getDataTypeName();
      }
    }
    return CATEGORY_ATTRIBUTES;
  }

  private CMElementDeclaration getDeclaration() {
    if ((fNode == null) || (fNode.getNodeType() != Node.ELEMENT_NODE)) {
      return null;
    }
    ModelQuery modelQuery = ModelQueryUtil.getModelQuery(fNode.getOwnerDocument());
    if (modelQuery != null) {
      return modelQuery.getCMElementDeclaration((Element) fNode);
    }
    return null;
  }

  private Display getDisplay() {

    return PlatformUI.getWorkbench().getDisplay();
  }

  /**
   * Returns a value for this Node that can be editted in a property sheet.
   * 
   * @return a value that can be editted
   */
  public Object getEditableValue() {
    return null;
  }

  /**
   * Returns the current collection of property descriptors.
   * 
   * @return all valid descriptors.
   */
  public final IPropertyDescriptor[] getPropertyDescriptors() {
    if ((fDescriptors == null) || (fDescriptors.length == 0)) {
      fDescriptors = createPropertyDescriptors();
    } else {
      updatePropertyDescriptors();
    }
    return fDescriptors;
  }

  /**
   * Returns the current value for the named property.
   */
  public Object getPropertyValue(Object nameObject) {
    String name = nameObject.toString();
    String returnedValue = null;
    NamedNodeMap attrMap = fNode.getAttributes();
    if (attrMap != null) {
      Node attribute = attrMap.getNamedItem(name);
      if (attribute != null) {
        if (attribute instanceof IDOMNode) {
          returnedValue = ((IDOMNode) attribute).getValueSource();
        } else {
          returnedValue = attribute.getNodeValue();
        }
      }
    }
    if (returnedValue == null) {
      returnedValue = ""; //$NON-NLS-1$
    }
    return returnedValue;
  }

  private String[] getValidValues(CMAttributeDeclaration attrDecl) {
    if (attrDecl == null) {
      return new String[0];
    }

    String[] validValues = null;
    CMDataType attrType = attrDecl.getAttrType();
    if (attrType != null) {
      validValues = _getValidStrings(attrDecl, attrType);
      if (fSortEnumeratedValues) {
        Arrays.sort(validValues);
      }
    }
    if (validValues == null) {
      validValues = new String[0];
    }
    return validValues;
  }

  public boolean isPropertyRemovable(Object id) {
    return true;
  }

  public boolean isPropertyResettable(Object id) {
    return fNode != null && fNode.getNodeType() == Node.ELEMENT_NODE;
  }

  /**
   * Returns whether the property value has changed from the default.
   * 
   * @return <code>true</code> if the value of the specified property has changed from its original
   *         default value; <code>false</code> otherwise.
   */
  public boolean isPropertySet(Object propertyObject) {
    String property = propertyObject.toString();

    NamedNodeMap attrMap = fNode.getAttributes();
    if (attrMap != null) {
      Node attr = attrMap.getNamedItem(property);
      return attr != null && (attr instanceof Attr ? ((Attr) attr).getSpecified() : true);
    }
    return false;
  }

  /**
   * Remove the given attribute from the Node
   * 
   * @param propertyObject
   */
  public void removeProperty(Object propertyObject) {
    NamedNodeMap attrMap = fNode.getAttributes();
    if (attrMap != null) {
      Node attribute = attrMap.getNamedItem(propertyObject.toString());
      if (attribute != null) {
        try {
          attrMap.removeNamedItem(propertyObject.toString());
        } catch (DOMException e) {
          if (e.code != DOMException.INVALID_MODIFICATION_ERR) {
            Logger.logException(e);
          }
        }
      }
    }
  }

  /**
   * Resets the specified property's value to its default value.
   */
  public void resetPropertyValue(Object propertyObject) {
    String property = propertyObject.toString();
    CMNamedNodeMap attrDecls = null;

    CMElementDeclaration ed = getDeclaration();
    if (ed != null) {
      attrDecls = ed.getAttributes();
      CMNamedNodeMapImpl allAttributes = new CMNamedNodeMapImpl(attrDecls);
      List nodes = ModelQueryUtil.getModelQuery(fNode.getOwnerDocument()).getAvailableContent(
          (Element) fNode, ed, ModelQuery.INCLUDE_ATTRIBUTES);
      for (int k = 0; k < nodes.size(); k++) {
        CMNode cmnode = (CMNode) nodes.get(k);
        if (cmnode.getNodeType() == CMNode.ATTRIBUTE_DECLARATION) {
          allAttributes.put(cmnode);
        }
      }
      attrDecls = allAttributes;
    }

    NamedNodeMap attrMap = fNode.getAttributes();
    if (attrDecls != null) {
      CMAttributeDeclaration attrDecl = (CMAttributeDeclaration) attrDecls.getNamedItem(property);
      String defValue = null;
      if (attrDecl != null) {
        if (attrDecl.getAttrType() != null) {
          CMDataType helper = attrDecl.getAttrType();
          if ((helper.getImpliedValueKind() != CMDataType.IMPLIED_VALUE_NONE)
              && (helper.getImpliedValue() != null)) {
            defValue = helper.getImpliedValue();
          }
        }
      }
      if ((defValue != null) && (defValue.length() > 0)) {
        // implied values will be in the DOM, but not the source
        attrMap.removeNamedItem(property);
      } else {
        ((Attr) attrMap.getNamedItem(property)).setValue(""); //$NON-NLS-1$
      }
    } else {
      // remember, this method is for reset, not remove
      ((Attr) attrMap.getNamedItem(property)).setValue(""); //$NON-NLS-1$
    }
  }

  /**
   * Sets the named property to the given value.
   */
  public void setPropertyValue(Object nameObject, Object value) {
    // Avoid cycling - can happen if a closing cell editor causes a
    // refresh
    // on the PropertySheet page and the setInput again asks the editor to
    // close; besides, why apply the same value twice?
    if (!fValuesBeingSet.isEmpty() && (fValuesBeingSet.peek() == nameObject)) {
      return;
    }
    fValuesBeingSet.push(nameObject);
    String name = nameObject.toString();
    String valueString = ""; //$NON-NLS-1$
    if (value != null) {
      valueString = value.toString();
    }
    NamedNodeMap attrMap = fNode.getAttributes();
    try {
      if (attrMap != null) {
        Attr attr = (Attr) attrMap.getNamedItem(name);
        if (attr != null && attr.getSpecified()) {
          // EXISTING VALUE
          // potential out of control loop if updating the value
          // triggers a viewer update, forcing the
          // active cell editor to save its value and causing the
          // loop to continue
          if ((attr.getValue() == null) || !attr.getValue().equals(valueString)) {
            if (attr instanceof IDOMNode) {
              ((IDOMNode) attr).setValueSource(valueString);
            } else {
              attr.setValue(valueString);
            }
          }
        } else {
          // NEW(?) value
          Attr newAttr = fNode.getOwnerDocument().createAttribute(name);
          if (newAttr instanceof IDOMNode) {
            ((IDOMNode) newAttr).setValueSource(valueString);
          } else {
            newAttr.setValue(valueString);
          }
          attrMap.setNamedItem(newAttr);
        }
      } else {
        if (fNode instanceof Element) {
          ((Element) fNode).setAttribute(name, valueString);
        }
      }
    } catch (DOMException e) {
      Display d = getDisplay();
      if (d != null) {
        d.beep();
      }
    }
    fValuesBeingSet.pop();
  }

  protected void updatePropertyDescriptors() {
    if ((fDescriptors == null) || (fDescriptors.length == 0)) {
      // Nothing to update
      return;
    }

    // List of all names encountered in the tag and defined by the element
    List declaredNames = new ArrayList();
    // New descriptor list that will become fDescriptors after all
    // processing is done
    List descriptors = new ArrayList();
    // Names of the descriptors in the above List
    List descriptorNames = new ArrayList();

    // Update any descriptors derived from the metainfo
    CMElementDeclaration ed = getDeclaration();
    CMNamedNodeMap attrMap = null;
    if (ed != null) {
      attrMap = ed.getAttributes();
      CMNamedNodeMapImpl allAttributes = new CMNamedNodeMapImpl(attrMap);
      ModelQuery modelQuery = ModelQueryUtil.getModelQuery(fNode.getOwnerDocument());
      if (modelQuery != null) {
        List nodes = modelQuery.getAvailableContent((Element) fNode, ed,
            ModelQuery.INCLUDE_ATTRIBUTES);
        for (int k = 0; k < nodes.size(); k++) {
          CMNode cmnode = (CMNode) nodes.get(k);
          if (cmnode.getNodeType() == CMNode.ATTRIBUTE_DECLARATION) {
            allAttributes.put(cmnode);
          }
        }
      }
      attrMap = allAttributes;
    }
    // Update exiting descriptors; not added to the final list here
    if (attrMap != null) {
      // Update existing descriptor types based on metainfo
      CMAttributeDeclaration attrDecl = null;
      for (int i = 0; i < attrMap.getLength(); i++) {
        attrDecl = (CMAttributeDeclaration) attrMap.item(i);
        String attrName = DOMNamespaceHelper.computeName(attrDecl, fNode, null);
        if (!declaredNames.contains(attrName)) {
          declaredNames.add(attrName);
        }
        for (int j = 0; j < fDescriptors.length; j++) {
          boolean sameName = (fCaseSensitive && fDescriptors[j].getId().equals(
              attrDecl.getNodeName()))
              || (!fCaseSensitive && attrDecl.getNodeName().equals(
                  fDescriptors[j].getId().toString()));
          if (sameName) {
            String[] validValues = getValidValues(attrDecl);
            // Update the descriptor for this
            // CMAttributeDeclaration (only enumerated values get
            // updated for now)
            if (fDescriptors[j] instanceof EnumeratedStringPropertyDescriptor) {
              ((EnumeratedStringPropertyDescriptor) fDescriptors[j]).updateValues(validValues);
            }
            // Replace with better descriptor
            else if ((validValues != null) && (validValues.length > 0)) {
              fDescriptors[j] = createPropertyDescriptor(attrDecl, null);
            }
          }
        }
      }
    } else {
      // Update existing descriptors based on not having any metainfo
      for (int j = 0; j < fDescriptors.length; j++) {
        // Replace with basic descriptor
        if (!(fDescriptors[j] instanceof TextPropertyDescriptor)) {
          fDescriptors[j] = createDefaultPropertyDescriptor((String) fDescriptors[j].getId());
        }
      }
    }

    NamedNodeMap attributes = fNode.getAttributes();

    // Remove descriptors for attributes that aren't present AND aren't
    // known through metainfo,
    // do this by only reusing existing descriptors for attributes that
    // are present or declared
    for (int i = 0; i < fDescriptors.length; i++) {
      if (fDescriptors[i] != null) {
        String descriptorName = fDescriptors[i].getId().toString();
        if ((declaredNames.contains(descriptorName) || (attributes.getNamedItem(descriptorName) != null))
            && !descriptorNames.contains(descriptorName)) {
          descriptorNames.add(descriptorName);
          descriptors.add(fDescriptors[i]);
        }
      }
    }

    // Add descriptors for declared attributes that don't already have one
    if (attrMap != null) {
      // Update existing descriptor types based on metainfo
      CMAttributeDeclaration attrDecl = null;
      for (int i = 0; i < attrMap.getLength(); i++) {
        attrDecl = (CMAttributeDeclaration) attrMap.item(i);
        String attrName = DOMNamespaceHelper.computeName(attrDecl, fNode, null);
        if (fCaseSensitive) {
          if (!descriptorNames.contains(attrName)) {
            IPropertyDescriptor descriptor = createPropertyDescriptor(attrDecl, null);
            if (descriptor != null) {
              descriptorNames.add(attrName);
              descriptors.add(descriptor);
            }
          }
        } else {
          boolean exists = false;
          for (int j = 0; j < descriptorNames.size(); j++) {
            exists = (descriptorNames.get(j).toString().equalsIgnoreCase(attrName)) || exists;
          }
          if (!exists) {
            descriptorNames.add(attrName);
            IPropertyDescriptor descriptor = createPropertyDescriptor(attrDecl, null);
            if (descriptor != null) {
              descriptorNames.add(attrName);
              descriptors.add(descriptor);
            }
          }
        }
      }
    }

    // Add descriptors for existing attributes that don't already have one
    if (attributes != null) {
      for (int i = 0; i < attributes.getLength(); i++) {
        Attr attr = (Attr) attributes.item(i);
        String attrName = attr.getName();
        if (fCaseSensitive) {
          if (!descriptorNames.contains(attrName)) {
            descriptorNames.add(attrName);
            descriptors.add(createDefaultPropertyDescriptor(attrName));
          }
        } else {
          boolean exists = false;
          for (int j = 0; j < descriptorNames.size(); j++) {
            exists = (descriptorNames.get(j).toString().equalsIgnoreCase(attrName)) || exists;
          }
          if (!exists) {
            descriptorNames.add(attrName);
            descriptors.add(createDefaultPropertyDescriptor(attrName));
          }
        }
      }
    }

    // Update fDescriptors
    IPropertyDescriptor[] newDescriptors = new IPropertyDescriptor[descriptors.size()];
    for (int i = 0; i < newDescriptors.length; i++) {
      newDescriptors[i] = (IPropertyDescriptor) descriptors.get(i);
    }
    fDescriptors = newDescriptors;
  }
}
