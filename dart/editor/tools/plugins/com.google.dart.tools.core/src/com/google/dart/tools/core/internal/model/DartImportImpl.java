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

import com.google.dart.compiler.LibrarySource;
import com.google.dart.tools.core.internal.model.info.DartElementInfo;
import com.google.dart.tools.core.internal.model.info.DartImportInfo;
import com.google.dart.tools.core.internal.util.MementoTokenizer;
import com.google.dart.tools.core.internal.util.ResourceUtil;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartImport;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.workingcopy.WorkingCopyOwner;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;

import java.util.HashMap;

/**
 * Instances of the class <code>DartImportImpl</code> implement an import within a library or
 * application file.
 */
public class DartImportImpl extends DartElementImpl implements DartImport {
  /**
   * The library being imported.
   */
  private LibrarySource library;

  public DartImportImpl(DartImportContainerImpl parent, LibrarySource library) {
    super(parent);
    this.library = library;
  }

  @Override
  public int getElementType() {
    return IMPORT;
  }

  /**
   * Return the name of the library that is being imported.
   * 
   * @return the name of the library that is being imported
   */
  public String getImportName() {
    return library.getName();
  }

  @Override
  public IResource getUnderlyingResource() {
    return ResourceUtil.getResource(library);
  }

  @Override
  public IResource resource() {
    return ResourceUtil.getResource(library);
  }

  @Override
  protected DartElementInfo createElementInfo() {
    return new DartImportInfo();
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
    // Import elements do not have any children.
    return this;
  }

  @Override
  protected char getHandleMementoDelimiter() {
    return MEMENTO_DELIMITER_IMPORT;
  }
}
