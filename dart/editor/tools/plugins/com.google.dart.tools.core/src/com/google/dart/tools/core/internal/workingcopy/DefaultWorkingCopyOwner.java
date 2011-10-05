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
package com.google.dart.tools.core.internal.workingcopy;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.buffer.Buffer;
import com.google.dart.tools.core.model.SourceFileElement;
import com.google.dart.tools.core.workingcopy.WorkingCopyOwner;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;

/**
 * The unique instance of the class <code>DefaultWorkingCopyOwner</code> implement the default
 * working copy owner.
 */
public class DefaultWorkingCopyOwner extends WorkingCopyOwner {

  private static final String EXTENSION_POINT_ID = DartCore.PLUGIN_ID + ".workingCopyOwner";

  private static final DefaultWorkingCopyOwner UniqueInstance = new DefaultWorkingCopyOwner();

  /**
   * Return the unique instance of this class.
   * 
   * @return the unique instance of this class
   */
  public static DefaultWorkingCopyOwner getInstance() {
    return UniqueInstance;
  }

  private boolean initialized;
  private WorkingCopyOwner primaryBufferProvider;

  /**
   * Prevent the creation of instances of this class.
   */
  private DefaultWorkingCopyOwner() {
    super();
  }

  @Override
  public Buffer createBuffer(SourceFileElement<?> workingCopy) {
    if (!initialized) {
      initialize();
    }

    if (primaryBufferProvider != null) {
      return primaryBufferProvider.createBuffer(workingCopy);
    }

    return super.createBuffer(workingCopy);
  }

  @Override
  public String toString() {
    return "Primary owner"; //$NON-NLS-1$
  }

  /**
   * Scan the plugin registry for a contributed working copy owner. Note that even if the buffer
   * provider is a working copy owner, only its <code>createBuffer(CompilationUnit)</code> method is
   * used by the primary working copy owner. It doesn't replace the internal primary working owner.
   */
  private void initialize() {
    initialized = true;

    IExtensionRegistry registery = Platform.getExtensionRegistry();
    IExtensionPoint extensionPoint = registery.getExtensionPoint(EXTENSION_POINT_ID);
    IConfigurationElement[] elements = extensionPoint.getConfigurationElements();

    if (elements.length > 0) {
      if (elements.length > 1) {
        DartCore.logError("Error, more then one working copy owner replacement contributed", null);
      }

      IConfigurationElement element = elements[0];

      try {
        WorkingCopyOwner workingCopy = (WorkingCopyOwner) element.createExecutableExtension("class");

        primaryBufferProvider = workingCopy;
      } catch (Throwable t) {
        DartCore.logError(t);
      }
    }
  }

}
