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

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.ui.DartToolsPlugin;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.swt.widgets.Display;

import java.util.ArrayList;
import java.util.List;

/**
 * Decorator for files view contents.
 */
public class FilesViewLightweightDecorator implements ILightweightLabelDecorator,
    IResourceChangeListener {
  private static ImageDescriptor DESC_READ_ONLY = DartToolsPlugin.getImageDescriptor("icons/full/ovr16/lock_ovr.png");

  private List<ILabelProviderListener> listeners = new ArrayList<ILabelProviderListener>();

  public FilesViewLightweightDecorator() {
    ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
  }

  @Override
  public void addListener(ILabelProviderListener listener) {
    listeners.add(listener);
  }

  @Override
  public void decorate(Object element, IDecoration decoration) {
    if (element instanceof IFileStore) {
      IFileStore fileStore = (IFileStore) element;

      if (fileStore.fetchInfo().getAttribute(EFS.ATTRIBUTE_READ_ONLY)) {
        decoration.addOverlay(DESC_READ_ONLY, IDecoration.BOTTOM_RIGHT);
      }
    } else if (element instanceof IFile) {
      IFile file = (IFile) element;

      try {
        DartElement dartElement = DartCore.create(file);

        if (dartElement != null) {
          if (file.equals(dartElement.getDartProject().getCorrespondingResource())) {
            decoration.addOverlay(DESC_READ_ONLY, IDecoration.BOTTOM_RIGHT);
          }
        }
      } catch (CoreException exception) {
        //ignore
      }
    }
  }

  @Override
  public void dispose() {
    ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
  }

  @Override
  public boolean isLabelProperty(Object element, String property) {
    return false;
  }

  @Override
  public void removeListener(ILabelProviderListener listener) {
    listeners.remove(listener);
  }

  @Override
  public void resourceChanged(IResourceChangeEvent event) {
    if (event.getType() == IResourceChangeEvent.POST_CHANGE) {
      Display.getDefault().asyncExec(new Runnable() {
        @Override
        public void run() {
          fireLabelChangedEvent();
        }
      });
    }
  }

  private void fireLabelChangedEvent() {
    try {
      for (ILabelProviderListener listener : listeners) {
        listener.labelProviderChanged(new LabelProviderChangedEvent(this));
      }
    } catch (Throwable t) {
      DartToolsPlugin.log(t);
    }
  }

}
