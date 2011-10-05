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
package com.google.dart.tools.core.internal.operation;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.buffer.Buffer;
import com.google.dart.tools.core.internal.model.CompilationUnitImpl;
import com.google.dart.tools.core.internal.model.DartModelStatusImpl;
import com.google.dart.tools.core.internal.model.ExternalDartProject;
import com.google.dart.tools.core.internal.model.delta.DartElementDeltaBuilder;
import com.google.dart.tools.core.internal.util.Messages;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartConventions;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartModelStatus;
import com.google.dart.tools.core.model.DartModelStatusConstants;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.jobs.ISchedulingRule;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;

/**
 * Instances of the class <code>CommitWorkingCopyOperation</code> implement an operation that
 * commits the contents of a working copy compilation unit to its original element and resource,
 * bringing the Dart Model up-to-date with the current contents of the working copy.
 * <p>
 * It is possible that the contents of the original resource have changed since the working copy was
 * created, in which case there is an update conflict. This operation allows for two settings to
 * resolve conflict set by the <code>force</code> flag:
 * <ul>
 * <li>force flag is <code>false</code> - in this case a <code>DartModelException</code> is thrown</li>
 * <li>force flag is <code>true</code> - in this case the contents of the working copy are applied
 * to the underlying resource even though the working copy was created before a subsequent change in
 * the resource</li>
 * </ul>
 * <p>
 * The default conflict resolution setting is the force flag is <code>false</code> A
 * DartModelOperation exception is thrown either if the commit could not be performed or if the new
 * content of the compilation unit violates some Dart Model constraint.
 */
public class CommitWorkingCopyOperation extends DartModelOperation {
  /**
   * Constructs an operation to commit the contents of a working copy to its original compilation
   * unit.
   */
  public CommitWorkingCopyOperation(CompilationUnit element, boolean force) {
    super(new DartElement[] {element}, force);
  }

  /**
   * Possible failures:
   * <ul>
   * <li>INVALID_ELEMENT_TYPES - the compilation unit supplied to this operation is not a working
   * copy
   * <li>ELEMENT_NOT_PRESENT - the compilation unit the working copy is based on no longer exists.
   * <li>UPDATE_CONFLICT - the original compilation unit has changed since the working copy was
   * created and the operation specifies no force
   * <li>READ_ONLY - the original compilation unit is in read-only mode
   * </ul>
   */
  @Override
  public DartModelStatus verify() {
    CompilationUnit cu = getCompilationUnit();
    if (!cu.isWorkingCopy()) {
      return new DartModelStatusImpl(DartModelStatusConstants.INVALID_ELEMENT_TYPES, cu);
    }
    if (cu.hasResourceChanged() && !force) {
      return new DartModelStatusImpl(DartModelStatusConstants.UPDATE_CONFLICT);
    }
    // no read-only check, since some repository adapters can change the flag on
    // save operation.
    return DartModelStatusImpl.VERIFIED_OK;
  }

  /**
   * @exception DartModelException if setting the source of the original compilation unit fails
   */
  @Override
  protected void executeOperation() throws DartModelException {
    DartCore.notYetImplemented();
    try {
      beginTask(Messages.operation_workingCopy_commit, 2);
      CompilationUnitImpl workingCopy = getCompilationUnit();

      if (ExternalDartProject.EXTERNAL_PROJECT_NAME.equals(workingCopy.getDartProject().getElementName())) {
        // case of a working copy without a resource
        workingCopy.getBuffer().save(progressMonitor, force);
        return;
      }

      CompilationUnit primary = workingCopy.getPrimary();
      boolean isPrimary = workingCopy.isPrimary();

      DartElementDeltaBuilder deltaBuilder = null;
      // PackageFragmentRoot root = (PackageFragmentRoot)
      // workingCopy.getAncestor(DartElement.PACKAGE_FRAGMENT_ROOT);
      boolean isIncluded = true; // !Util.isExcluded(workingCopy);
      IFile resource = (IFile) workingCopy.getResource();
      // DartProject project = workingCopy.getDartProject();
      if (isPrimary
          || (/* root.validateOnClasspath().isOK() && */isIncluded && resource.isAccessible() && DartConventions.validateCompilationUnitName(
              workingCopy.getElementName()).isOK())) {

        // force opening so that the delta builder can get the old info
        if (!isPrimary && !primary.isOpen()) {
          primary.open(null);
        }

        // creates the delta builder (this remembers the content of the cu) if:
        // - it is not excluded
        // - and it is not a primary or it is a non-consistent primary
        if (isIncluded && (!isPrimary || !workingCopy.isConsistent())) {
          deltaBuilder = new DartElementDeltaBuilder(primary);
        }

        // save the cu
        Buffer primaryBuffer = primary.getBuffer();
        if (!isPrimary) {
          if (primaryBuffer == null) {
            return;
          }
          char[] primaryContents = primaryBuffer.getCharacters();
          boolean hasSaved = false;
          try {
            Buffer workingCopyBuffer = workingCopy.getBuffer();
            if (workingCopyBuffer == null) {
              return;
            }
            primaryBuffer.setContents(workingCopyBuffer.getCharacters());
            primaryBuffer.save(progressMonitor, force);
            primary.makeConsistent(this);
            hasSaved = true;
          } finally {
            if (!hasSaved) {
              // restore original buffer contents since something went wrong
              primaryBuffer.setContents(primaryContents);
            }
          }
        } else {
          // for a primary working copy no need to set the content of the buffer
          // again
          primaryBuffer.save(progressMonitor, force);
          primary.makeConsistent(this);
        }
      } else {
        // working copy on cu outside classpath OR resource doesn't exist yet
        String encoding = null;
        try {
          encoding = resource.getCharset();
        } catch (CoreException ce) {
          // use no encoding
        }
        String contents = workingCopy.getSource();
        if (contents == null) {
          return;
        }
        try {
          byte[] bytes = encoding == null ? contents.getBytes() : contents.getBytes(encoding);
          ByteArrayInputStream stream = new ByteArrayInputStream(bytes);
          if (resource.exists()) {
            resource.setContents(stream, force ? IResource.FORCE | IResource.KEEP_HISTORY
                : IResource.KEEP_HISTORY, null);
          } else {
            resource.create(stream, force, progressMonitor);
          }
        } catch (CoreException e) {
          throw new DartModelException(e);
        } catch (UnsupportedEncodingException e) {
          throw new DartModelException(e, DartModelStatusConstants.IO_EXCEPTION);
        }

      }

      setAttribute(HAS_MODIFIED_RESOURCE_ATTR, TRUE);

      // make sure working copy is in sync
      workingCopy.updateTimeStamp((CompilationUnitImpl) primary);
      workingCopy.makeConsistent(this);
      worked(1);

      // build the deltas
      if (deltaBuilder != null) {
        deltaBuilder.buildDeltas();

        // add the deltas to the list of deltas created during this operation
        if (deltaBuilder.delta != null) {
          addDelta(deltaBuilder.delta);
        }
      }
      worked(1);
    } finally {
      done();
    }
  }

  /**
   * Returns the compilation unit this operation is working on.
   */
  protected CompilationUnitImpl getCompilationUnit() {
    return (CompilationUnitImpl) getElementToProcess();
  }

  @Override
  protected ISchedulingRule getSchedulingRule() {
    IResource resource = getElementToProcess().getResource();
    if (resource == null) {
      return null;
    }
    IWorkspace workspace = resource.getWorkspace();
    if (resource.exists()) {
      return workspace.getRuleFactory().modifyRule(resource);
    } else {
      return workspace.getRuleFactory().createRule(resource);
    }
  }
}
