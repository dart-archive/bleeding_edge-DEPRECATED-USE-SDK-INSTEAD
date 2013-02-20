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
package com.google.dart.tools.ui.internal.filesview;

import com.google.dart.tools.core.DartCoreDebug;
import com.google.dart.tools.core.analysis.model.ProjectListener;
import com.google.dart.tools.core.model.ElementChangedEvent;
import com.google.dart.tools.core.model.ElementChangedListener;

import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;

/**
 * Label provider for resources in the {@link FilesView}.
 */
public abstract class ResourceLabelProvider implements IStyledLabelProvider, ILabelProvider,
    ElementChangedListener, ProjectListener {

  /**
   * Get a resource label provider instance.
   */
  public static ResourceLabelProvider createInstance() {
    return DartCoreDebug.ENABLE_NEW_ANALYSIS ? new NewResourceLabelProvider()
        : new OldResourceLabelProvider();
  }

  @Override
  public void elementChanged(ElementChangedEvent event) {
    //legacy method to be removed
    //(implemented in legacy "old" label provider subclass)
  }

}
