/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.views.contentmodel;

import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.List;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.wst.xml.core.internal.contentmodel.CMAttributeDeclaration;
import org.eclipse.wst.xml.core.internal.contentmodel.CMDataType;
import org.eclipse.wst.xml.core.internal.contentmodel.CMElementDeclaration;
import org.eclipse.wst.xml.core.internal.contentmodel.CMGroup;
import org.eclipse.wst.xml.core.internal.contentmodel.CMNamedNodeMap;
import org.eclipse.wst.xml.core.internal.contentmodel.CMNode;
import org.eclipse.wst.xml.core.internal.contentmodel.util.CMVisitor;
import org.eclipse.wst.xml.ui.internal.XMLUIPlugin;
import org.eclipse.wst.xml.ui.internal.editor.XMLEditorPluginImages;

public class CMListWorkbenchAdapter extends CMVisitor implements IWorkbenchAdapter {

  private CMElementDeclaration fDeclaration;
  private String text;
  private ImageDescriptor image;
  private GroupStack fGroupStack;
  private CMDescriber fRoot;

  public CMListWorkbenchAdapter(CMElementDeclaration decl) {
    text = getFormattedLabel(decl.getNodeName(), decl.getMinOccur(), decl.getMaxOccur(),
        decl.getDataType());
    fDeclaration = decl;
    image = AbstractUIPlugin.imageDescriptorFromPlugin(XMLUIPlugin.ID,
        XMLEditorPluginImages.IMG_OBJ_ELEMENT);

    fGroupStack = new GroupStack();
    fRoot = new CMDescriber(decl);
  }

  public Object[] getChildren(Object o) {
    /*
     * TODO Use the CMElementDeclaration and ModelQuery to get the available children of the root
     * element
     */
    return fRoot.getChildren(o);
  }

  public ImageDescriptor getImageDescriptor(Object object) {
    return image;
  }

  public String getLabel(Object o) {
    return text;
  }

  public Object getParent(Object o) {
    return fDeclaration;
  }

  /**
   * Formats the label for an element declaration. The format will be: <b>name</b>
   * [<i>min</i>..<i>max</i>], where <i>max</i> when unbounded is represented by <b>*</b>
   * 
   * @param name The name of the element declaration
   * @param min The minimum number of occurrences of the element
   * @param max The maximum number of occurrences of the element
   * @return The formatted label String
   */
  static String getFormattedLabel(String name, int min, int max, CMDataType dataType) {
    StringBuffer buffer = new StringBuffer(name);
    buffer.append('[');
    buffer.append(min);
    buffer.append(".."); //$NON-NLS-1$
    buffer.append((max >= 0 ? Integer.toString(max) : "*")); //$NON-NLS-1$
    buffer.append(']');
    if (dataType != null) {
      buffer.append(" {"); //$NON-NLS-1$
      buffer.append(dataType.getDataTypeName());
      buffer.append('}');
    }
    return buffer.toString();
  }

  /**
   * Workbench adapter that describes its CMNode. It calculates its children lazily using the
   * CMVisitor to identify immediate children (i.e., the content of child nodes is not visited).
   */
  private class CMDescriber extends CMVisitor implements IWorkbenchAdapter {

    List fChildren = null;
    ImageDescriptor fImage;
    String label;
    CMNode root;

    public CMDescriber(CMNode node) {
      root = node;
    }

    public Object[] getChildren(Object o) {
      if (fChildren == null) {
        fChildren = new ArrayList();
        if (root.getNodeType() == CMNode.ELEMENT_DECLARATION) {
          CMElementDeclaration ed = (CMElementDeclaration) root;
          CMNamedNodeMap nodeMap = ed.getAttributes();
          int size = nodeMap.getLength();
          for (int i = 0; i < size; i++) {
            visitCMNode(nodeMap.item(i));
          }
          visitCMNode(ed.getContent());
        }
      }
      return fChildren.toArray();
    }

    public ImageDescriptor getImageDescriptor(Object object) {
      return fImage;
    }

    public void setImage(String path) {
      fImage = AbstractUIPlugin.imageDescriptorFromPlugin(XMLUIPlugin.ID, path);
    }

    public String getLabel(Object o) {
      return label;
    }

    public Object getParent(Object o) {
      return root;
    }

    public void visitCMGroup(CMGroup e) {
      fGroupStack.push(e.getMaxOccur());
      super.visitCMGroup(e);
      fGroupStack.pop();
    }

    public void visitCMAttributeDeclaration(CMAttributeDeclaration ad) {
      CMDescriber describer = new CMDescriber(ad);
      describer.label = ad.getNodeName();
      describer.setImage(XMLEditorPluginImages.IMG_OBJ_ATTRIBUTE);
      fChildren.add(describer);
    }

    public void visitCMElementDeclaration(CMElementDeclaration ed) {
      CMDescriber describer = new CMDescriber(ed);

      // If the parent group stack containing this element declaration is unbounded, the element within is unbounded as well
      describer.label = getFormattedLabel(ed.getNodeName(), ed.getMinOccur(),
          (!fGroupStack.isEmpty() && fGroupStack.peek() < 0) ? -1 : ed.getMaxOccur(),
          ed.getDataType());
      describer.setImage(XMLEditorPluginImages.IMG_OBJ_ELEMENT);
      fChildren.add(describer);
    }
  }

  /**
   * A stack of integers used to determine if an element declaration is unbounded
   */
  private static class GroupStack {
    int[] stack;
    int size;

    public GroupStack() {
      stack = new int[5];
    }

    public void push(int i) {
      if (size >= stack.length) {
        int[] tmp = stack;
        stack = new int[stack.length * 2];
        System.arraycopy(tmp, 0, stack, 0, tmp.length);
      }
      stack[size++] = i;
    }

    public int pop() {
      if (isEmpty())
        throw new EmptyStackException();
      return stack[--size];
    }

    public boolean isEmpty() {
      return size == 0;
    }

    public int peek() {
      if (isEmpty())
        throw new EmptyStackException();
      return stack[size - 1];
    }
  }
}
