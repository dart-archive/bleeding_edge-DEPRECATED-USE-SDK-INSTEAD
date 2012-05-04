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
package com.google.dart.tools.core.internal.operation;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.internal.model.DartModelStatusImpl;
import com.google.dart.tools.core.internal.util.Messages;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartModelStatusConstants;
import com.google.dart.tools.core.model.OpenableElement;

import org.eclipse.core.resources.IResource;

/**
 * Instances of the class <code>DeleteResourceElementsOperation</code> implement an operation that
 * deletes a collection of resources and all of their children. It does not delete resources which
 * do not belong to the Dart Model such as GIF files.
 */
public class DeleteResourceElementsOperation extends MultiOperation {
  /**
   * When executed, this operation will delete the given elements. The elements to delete cannot be
   * <code>null</code> or empty, and must have a corresponding resource.
   */
  protected DeleteResourceElementsOperation(DartElement[] elementsToProcess, boolean force) {
    super(elementsToProcess, force);
  }

  /**
   * Deletes the direct children of <code>frag</code> corresponding to its kind (K_SOURCE or
   * K_BINARY), and deletes the corresponding folder if it is then empty.
   */
  // private void deletePackageFragment(IPackageFragment frag)
  // throws DartModelException {
  // IResource res = ((DartElementImpl) frag).resource();
  // if (res != null) {
  // // collect the children to remove
  // DartElement[] childrenOfInterest = frag.getChildren();
  // if (childrenOfInterest.length > 0) {
  // IResource[] resources = new IResource[childrenOfInterest.length];
  // // remove the children
  // for (int i = 0; i < childrenOfInterest.length; i++) {
  // resources[i] = ((DartElementImpl) childrenOfInterest[i]).resource();
  // }
  // deleteResources(resources, this.force);
  // }
  //
  // // Discard non-java resources
  // Object[] nonJavaResources = frag.getNonJavaResources();
  // int actualResourceCount = 0;
  // for (int i = 0, max = nonJavaResources.length; i < max; i++){
  // if (nonJavaResources[i] instanceof IResource) actualResourceCount++;
  // }
  // IResource[] actualNonJavaResources = new IResource[actualResourceCount];
  // for (int i = 0, max = nonJavaResources.length, index = 0; i < max; i++){
  // if (nonJavaResources[i] instanceof IResource)
  // actualNonJavaResources[index++] = (IResource)nonJavaResources[i];
  // }
  // deleteResources(actualNonJavaResources, this.force);
  //
  // // delete remaining files in this package (.class file in the case where
  // Proj=src=bin)
  // IResource[] remainingFiles;
  // try {
  // remainingFiles = ((IContainer) res).members();
  // } catch (CoreException ce) {
  // throw new DartModelException(ce);
  // }
  // boolean isEmpty = true;
  // for (int i = 0, length = remainingFiles.length; i < length; i++) {
  // IResource file = remainingFiles[i];
  // if (file instanceof IFile && Util.isClassFileName(file.getName())) {
  // deleteResource(file, IResource.FORCE | IResource.KEEP_HISTORY);
  // } else {
  // isEmpty = false;
  // }
  // }
  // if (isEmpty && !frag.isDefaultPackage()/*don't delete default package's
  // folder: see https://bugs.eclipse.org/bugs/show_bug.cgi?id=38450*/) {
  // // delete recursively empty folders
  // IResource fragResource = ((DartElementImpl) frag).resource();
  // if (fragResource != null) {
  // deleteEmptyPackageFragment(frag, false, fragResource.getParent());
  // }
  // }
  // }
  // }

  @Override
  protected String getMainTaskName() {
    return Messages.operation_deleteResourceProgress;
  }

  /**
   * @see MultiOperation This method delegate to <code>deleteResource</code> or
   *      <code>deletePackageFragment</code> depending on the type of <code>element</code>.
   */
  @Override
  protected void processElement(DartElement element) throws DartModelException {
    DartCore.notYetImplemented();
    switch (element.getElementType()) {
    // case DartElement.CLASS_FILE :
      case DartElement.COMPILATION_UNIT:
        deleteResource(element.getResource(), this.force ? IResource.FORCE | IResource.KEEP_HISTORY
            : IResource.KEEP_HISTORY);
        break;
      // case DartElement.PACKAGE_FRAGMENT :
      // deletePackageFragment((IPackageFragment) element);
      // break;
      default:
        throw new DartModelException(new DartModelStatusImpl(
            DartModelStatusConstants.INVALID_ELEMENT_TYPES,
            element));
    }
    // ensure the element is closed
    if (element instanceof OpenableElement) {
      ((OpenableElement) element).close();
    }
  }

  @Override
  protected void verify(DartElement element) throws DartModelException {
    if (element == null || !element.exists()) {
      error(DartModelStatusConstants.ELEMENT_DOES_NOT_EXIST, element);
    }
    DartCore.notYetImplemented();
    // int type = element.getElementType();
    // if (type <= DartElement.PACKAGE_FRAGMENT_ROOT || type >
    // DartElement.COMPILATION_UNIT) {
    // error(DartModelStatusConstants.INVALID_ELEMENT_TYPES, element);
    // } else if (type == DartElement.PACKAGE_FRAGMENT && element instanceof
    // JarPackageFragment) {
    // error(DartModelStatusConstants.INVALID_ELEMENT_TYPES, element);
    // }
    // IResource resource = ((DartElementImpl) element).resource();
    // if (resource instanceof IFolder) {
    // if (resource.isLinked()) {
    // error(DartModelStatusConstants.INVALID_RESOURCE, element);
    // }
    // }
  }
}
