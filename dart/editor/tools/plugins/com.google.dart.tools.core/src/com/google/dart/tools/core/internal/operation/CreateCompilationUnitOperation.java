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

import com.google.dart.tools.core.buffer.Buffer;
import com.google.dart.tools.core.internal.model.CompilationUnitImpl;
import com.google.dart.tools.core.internal.model.DartModelStatusImpl;
import com.google.dart.tools.core.internal.model.delta.DartElementDeltaImpl;
import com.google.dart.tools.core.internal.util.Messages;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartConventions;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartElementDelta;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartModelStatus;
import com.google.dart.tools.core.model.DartModelStatusConstants;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.jobs.ISchedulingRule;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Instances of the class <code>CreateCompilationUnitOperation</code> implement an operation that
 * creates a compilation unit (CU). If the CU doesn't exist yet, a new compilation unit will be
 * created with the content provided. Otherwise the operation will override the contents of an
 * existing CU with the new content.
 * <p>
 * Note: It is possible to create a CU automatically when creating a class or interface. Thus, the
 * preferred method of creating a CU is to perform a create type operation rather than first
 * creating a CU and secondly creating a type inside the CU.
 * <p>
 * Required Attributes:
 * <ul>
 * <li>The folder in which to create the compilation unit.
 * <li>The name of the compilation unit. Do not include the <code>".dart"</code> suffix (ex.
 * <code>"Object"</code> - the <code>".dart"</code> will be added for the name of the compilation
 * unit.)
 * <li>
 * </ul>
 */
public class CreateCompilationUnitOperation extends DartModelOperation {
  /**
   * The name of the compilation unit being created.
   */
  private String name;

  /**
   * The source code to use when creating the element.
   */
  private String source = null;

  /**
   * When executed, this operation will create a compilation unit with the given name. The name
   * should have the ".dart" suffix.
   */
  public CreateCompilationUnitOperation(DartLibrary parentElement, String name, String source,
      boolean force) {
    super(null, new DartElement[] {parentElement}, force);
    this.name = name;
    this.source = source;
  }

  /**
   * Possible failures:
   * <ul>
   * <li>NO_ELEMENTS_TO_PROCESS - the package fragment supplied to the operation is
   * <code>null</code>.
   * <li>INVALID_NAME - the compilation unit name provided to the operation is <code>null</code> or
   * has an invalid syntax
   * <li>INVALID_CONTENTS - the source specified for the compilation unit is null
   * </ul>
   */
  @Override
  public DartModelStatus verify() {
    if (getParentElement() == null) {
      return new DartModelStatusImpl(DartModelStatusConstants.NO_ELEMENTS_TO_PROCESS);
    }
    // DartProject project = getParentElement().getDartProject();
    if (DartConventions.validateCompilationUnitName(name).getSeverity() == IStatus.ERROR) {
      return new DartModelStatusImpl(DartModelStatusConstants.INVALID_NAME, name);
    }
    if (source == null) {
      return new DartModelStatusImpl(DartModelStatusConstants.INVALID_CONTENTS);
    }
    return DartModelStatusImpl.VERIFIED_OK;
  }

  /**
   * Creates a compilation unit.
   * 
   * @throws DartModelException if unable to create the compilation unit
   */
  @Override
  protected void executeOperation() throws DartModelException {
    try {
      beginTask(Messages.operation_createUnitProgress, 2);
      DartElementDeltaImpl delta = newDartElementDelta();
      CompilationUnitImpl unit = (CompilationUnitImpl) getCompilationUnit();
      DartLibrary library = (DartLibrary) getParentElement();
      IContainer folder = (IContainer) library.getResource();
      worked(1);
      IFile compilationUnitFile = folder.getFile(new Path(name));
      if (compilationUnitFile.exists()) {
        // update the contents of the existing unit if fForce is true
        if (force) {
          Buffer buffer = unit.getBuffer();
          if (buffer == null) {
            return;
          }
          buffer.setContents(this.source);
          unit.save(new NullProgressMonitor(), false);
          resultElements = new DartElement[] {unit};
          if (/* !Util.isExcluded(unit) && */unit.getParent().exists()) {
            for (int i = 0; i < resultElements.length; i++) {
              delta.changed(resultElements[i], DartElementDelta.F_CONTENT);
            }
            addDelta(delta);
          }
        } else {
          throw new DartModelException(new DartModelStatusImpl(
              DartModelStatusConstants.NAME_COLLISION,
              Messages.bind(
                  Messages.status_nameCollision,
                  compilationUnitFile.getFullPath().toString())));
        }
      } else {
        try {
          String encoding = null;
          try {
            // get folder encoding as file is not accessible
            encoding = folder.getDefaultCharset();
          } catch (CoreException ce) {
            // use no encoding
          }
          InputStream stream = new ByteArrayInputStream(encoding == null ? this.source.getBytes()
              : this.source.getBytes(encoding));
          createFile(folder, unit.getElementName(), stream, this.force);
          resultElements = new DartElement[] {unit};
          if (/* !Util.isExcluded(unit) && */unit.getParent().exists()) {
            for (int i = 0; i < resultElements.length; i++) {
              delta.added(resultElements[i]);
            }
            addDelta(delta);
          }
        } catch (IOException e) {
          throw new DartModelException(e, DartModelStatusConstants.IO_EXCEPTION);
        }
      }
      worked(1);
    } finally {
      done();
    }
  }

  /**
   * @see CreateElementInCUOperation#getCompilationUnit()
   */
  protected CompilationUnit getCompilationUnit() {
    return ((DartLibrary) getParentElement()).getCompilationUnit(name);
  }

  @Override
  protected ISchedulingRule getSchedulingRule() {
    IResource resource = getCompilationUnit().getResource();
    IWorkspace workspace = resource.getWorkspace();
    if (resource.exists()) {
      return workspace.getRuleFactory().modifyRule(resource);
    } else {
      return workspace.getRuleFactory().createRule(resource);
    }
  }
}
