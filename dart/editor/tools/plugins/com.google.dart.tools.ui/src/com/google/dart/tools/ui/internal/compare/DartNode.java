/*
 * Copyright (c) 2012, the Dart project authors.
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
package com.google.dart.tools.ui.internal.compare;

import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.ui.DartToolsPlugin;

import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.DocumentRangeNode;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.graphics.Image;

/**
 * Comparable Dart elements are represented as {@link DartNode}s. Extends the DocumentRangeNode with
 * method signature information.
 */
class DartNode extends DocumentRangeNode implements ITypedElement {

  public static final int CU = 0;
  public static final int PACKAGE = 1;
  public static final int IMPORT_CONTAINER = 2;
  public static final int IMPORT = 3;
  public static final int INTERFACE = 4;
  public static final int CLASS = 5;
  public static final int ENUM = 6;
  public static final int ANNOTATION = 7;
  public static final int FIELD = 8;
  public static final int INIT = 9;
  public static final int CONSTRUCTOR = 10;
  public static final int METHOD = 11;

  private int fInitializerCount = 1;

  /**
   * Creates a {@link DartNode} under the given parent.
   * 
   * @param parent the parent node
   * @param type the Dart elements type. Legal values are from the range CU to METHOD of this class.
   * @param name the name of the Dart element
   * @param start the starting position of the Dart element in the underlying document
   * @param length the number of characters of the Dart element in the underlying document
   */
  public DartNode(DartNode parent, int type, String name, int start, int length) {
    super(
        parent,
        type,
        DartCompareUtilities.buildID(type, name),
        parent.getDocument(),
        start,
        length);
    parent.addChild(this);
  }

  /**
   * Creates a {@link DartNode} for a CU. It represents the root of a {@link DartNode} tree, so its
   * parent is null.
   * 
   * @param document the document which contains the Dart element
   */
  public DartNode(IDocument document) {
    super(CU, DartCompareUtilities.buildID(CU, "root"), document, 0, document.getLength()); //$NON-NLS-1$
  }

  /**
   * Extracts the method's arguments name the signature. Used for smart matching.
   */
  public String extractArgumentList() {
    String id = getId();
    int pos = id.indexOf('(');
    if (pos >= 0) {
      return id.substring(pos + 1);
    }
    return id.substring(1);
  }

  /**
   * Extracts the method name from the signature. Used for smart matching.
   */
  public String extractMethodName() {
    String id = getId();
    int pos = id.indexOf('(');
    if (pos > 0) {
      return id.substring(1, pos);
    }
    return id.substring(1);
  }

  /**
   * Returns a shared image for this Dart element. see ITypedInput.getImage
   */
  @Override
  public Image getImage() {

    ImageDescriptor id = null;

    switch (getTypeCode()) {
      case CU:
        id = DartCompareUtilities.getImageDescriptor(DartElement.COMPILATION_UNIT);
        break;
//      case PACKAGE:
//        id = DartCompareUtilities.getImageDescriptor(DartElement.PACKAGE_DECLARATION);
//        break;
//      case IMPORT:
//        id = DartCompareUtilities.getImageDescriptor(DartElement.IMPORT_DECLARATION);
//        break;
      case IMPORT_CONTAINER:
        id = DartCompareUtilities.getImageDescriptor(DartElement.IMPORT_CONTAINER);
        break;
      case CLASS:
        id = DartCompareUtilities.getTypeImageDescriptor();
//        id = DartCompareUtilities.getTypeImageDescriptor(true);
        break;
//      case INTERFACE:
//        id = DartCompareUtilities.getTypeImageDescriptor(false);
//        break;
//      case INIT:
//        id = DartCompareUtilities.getImageDescriptor(DartElement.INITIALIZER);
//        break;
//      case CONSTRUCTOR:
//      case METHOD:
//        id = DartCompareUtilities.getImageDescriptor(DartElement.METHOD);
//        break;
      case FIELD:
        id = DartCompareUtilities.getImageDescriptor(DartElement.FIELD);
        break;
      case ENUM:
        id = DartCompareUtilities.getEnumImageDescriptor();
        break;
      case ANNOTATION:
        id = DartCompareUtilities.getAnnotationImageDescriptor();
        break;
    }
    return DartToolsPlugin.getImageDescriptorRegistry().get(id);
  }

  public String getInitializerCount() {
    return Integer.toString(fInitializerCount++);
  }

  /**
   * Returns a name which is presented in the UI.
   * 
   * @see ITypedElement#getName()
   */
  @Override
  public String getName() {

    switch (getTypeCode()) {
      case INIT:
        return CompareMessages.DartNode_initializer;
      case IMPORT_CONTAINER:
        return CompareMessages.DartNode_importDeclarations;
      case CU:
        return CompareMessages.DartNode_compilationUnit;
      case PACKAGE:
        return CompareMessages.DartNode_packageDeclaration;
    }
    return getId().substring(1); // we strip away the type character
  }

  @Override
  public String getType() {
    return "dart2"; //$NON-NLS-1$
  }

  @Override
  public String toString() {
    return getType() + ": " + getName() //$NON-NLS-1$
        + "[" + getRange().offset + "+" + getRange().length + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
  }
}
