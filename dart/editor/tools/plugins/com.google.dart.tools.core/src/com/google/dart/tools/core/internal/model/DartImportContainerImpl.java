/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.google.dart.tools.core.internal.model;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.internal.model.info.DartElementInfo;
import com.google.dart.tools.core.internal.model.info.DartImportContainerInfo;
import com.google.dart.tools.core.internal.util.MementoTokenizer;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartImportContainer;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.workingcopy.WorkingCopyOwner;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;

import java.util.HashMap;

/**
 * Instances of the class <code>DartImportContainerImpl</code> implement a container for the imports
 * within a library or application file.
 */
public class DartImportContainerImpl extends DartElementImpl implements DartImportContainer {
  public DartImportContainerImpl(LibraryConfigurationFileImpl parent) {
    super(parent);
  }

  @Override
  public int getElementType() {
    return IMPORT_CONTAINER;
  }

  @Override
  public IResource getUnderlyingResource() throws DartModelException {
    return null;
  }

  @Override
  public IResource resource() {
    return null;
  }

  @Override
  protected DartElementInfo createElementInfo() {
    return new DartImportContainerInfo();
  }

  @Override
  protected void generateInfos(DartElementInfo info,
      HashMap<DartElement, DartElementInfo> newElements, IProgressMonitor pm)
      throws DartModelException {
    OpenableElementImpl openableParent = (OpenableElementImpl) getOpenableParent();
    if (openableParent == null) {
      return;
    }
    DartElementInfo openableParentInfo = DartModelManager.getInstance().getInfo(openableParent);
    if (openableParentInfo == null) {
      openableParent.generateInfos(openableParent.createElementInfo(), newElements, pm);
    }
  }

  @Override
  protected DartElement getHandleFromMemento(String token, MementoTokenizer tokenizer,
      WorkingCopyOwner owner) {
    if (token.charAt(0) == MEMENTO_DELIMITER_IMPORT) {
      if (!tokenizer.hasMoreTokens()) {
        return this;
      }
      DartCore.notYetImplemented();
      // TODO(brianwilkerson) I think that the second argument needs to be the
      // library file that is being imported.
      tokenizer.nextToken();
      return new DartImportImpl(this, null);
    }
    return null;
  }

  @Override
  protected char getHandleMementoDelimiter() {
    return MEMENTO_DELIMITER_IMPORT_CONTAINER;
  }

  @Override
  protected String getHandleMementoName() {
    // Because there is only one import container per library or application,
    // there is no need to specify a name for it.
    return "";
  }
}
