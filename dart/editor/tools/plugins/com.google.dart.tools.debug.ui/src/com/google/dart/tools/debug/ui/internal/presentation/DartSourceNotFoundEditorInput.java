/*
 * Copyright (c) 2013, the Dart project authors.
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

package com.google.dart.tools.debug.ui.internal.presentation;

import com.google.dart.tools.debug.core.source.DartNoSourceFoundElement;

import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

/**
 * An IEditorInput for the "Source not found" editor.
 */
public class DartSourceNotFoundEditorInput extends PlatformObject implements IEditorInput {
  private DartNoSourceFoundElement sourceElement;

  protected DartSourceNotFoundEditorInput(DartNoSourceFoundElement sourceElement) {
    this.sourceElement = sourceElement;
  }

  @Override
  public boolean exists() {
    return false;
  }

  public String getDescription() {
    return sourceElement.getDescription();
  }

  @Override
  public ImageDescriptor getImageDescriptor() {
    return null;
  }

  public ILaunch getLaunch() {
    return sourceElement.getLaunch();
  }

  @Override
  public String getName() {
    return getDescription();
  }

  @Override
  public IPersistableElement getPersistable() {
    return null;
  }

  @Override
  public String getToolTipText() {
    return "Source not available";
  }
}
