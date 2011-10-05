/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui.internal.text.editor.saveparticipant;

import com.google.dart.tools.core.model.CompilationUnit;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IRegion;

/**
 * This <code>IPostSaveListener</code> is informed when a compilation unit is saved through the
 * {@link org.eclipse.wst.jsdt.internal.ui.javaeditor.CompilationUnitDocumentProvider} .
 * <p>
 * In oder to get notified the listener must be registered with the {@link SaveParticipantRegistry}
 * and be enabled on the save participant preference page.
 * </p>
 * <p>
 * The notification order of post save listeners is unspecified.
 * </p>
 * <p>
 * Clients may implement this interface.
 * </p>
 * 
 * @see SaveParticipantDescriptor
 * @see org.eclipse.wst.jsdt.internal.ui.javaeditor.CompilationUnitDocumentProvider
 */
public interface IPostSaveListener {

  /**
   * The unique id of this listener.
   * 
   * @return a non-empty id
   */
  String getId();

  /**
   * A human readable name of this listener.
   * 
   * @return the name
   */
  String getName();

  void saved(CompilationUnit unit, IProgressMonitor monitor) throws CoreException;

  /**
   * Informs this post save listener that the given <code>compilationUnit</code> has been saved by
   * the {@link org.eclipse.wst.jsdt.internal.ui.javaeditor.CompilationUnitDocumentProvider} . The
   * listener is allowed to modify the given compilation unit and to open a dialog.
   * <p>
   * <em>Every implementor of this method must strictly obey these rules:</em>
   * <ul>
   * <li>not touch any file other than the given <code>compilationUnit</code> which is already
   * locked by a scheduling rule</li>
   * <li>changing the scheduling rule or posting a new job is not allowed</li>
   * <li>it is not allowed to save the given <code>compilationUnit</code></li>
   * <li>it must be able to deal with unsaved resources and with compilation units which are not on
   * the Java build path</li>
   * <li>must not assume to be called in the UI thread</li>
   * <li>should be as fast as possible since this code is executed every time the
   * <code>compilationUnit</code> is saved</li>
   * </ul>
   * The compilation unit document provider can disable a listener that violates any of the above
   * rules.
   * </p>
   * 
   * @param compilationUnit the compilation unit which was saved
   * @param monitor the progress monitor for reporting progress
   * @see org.eclipse.wst.jsdt.internal.ui.javaeditor.CompilationUnitDocumentProvider
   */
  void saved(CompilationUnit compilationUnit, IRegion[] changedRegions, IProgressMonitor monitor)
      throws CoreException;
}
