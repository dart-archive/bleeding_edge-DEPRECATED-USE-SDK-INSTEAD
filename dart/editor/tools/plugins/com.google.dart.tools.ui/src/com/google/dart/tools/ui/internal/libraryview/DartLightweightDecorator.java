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
package com.google.dart.tools.ui.internal.libraryview;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.ui.DartPluginImages;
import com.google.dart.tools.ui.DartToolsPlugin;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
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
 * An error decorator for Dart elements.
 */
public class DartLightweightDecorator implements ILightweightLabelDecorator,
    IResourceChangeListener {

  List<ILabelProviderListener> listeners;

  /**
   * Create a new DartLightweightDecorator.
   */
  public DartLightweightDecorator() {
    listeners = new ArrayList<ILabelProviderListener>(1);
    ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
  }

  @Override
  public void addListener(ILabelProviderListener listener) {
    listeners.add(listener);
  }

  @Override
  public void decorate(Object element, IDecoration decoration) {
    if (element instanceof IProject) {
      // IProject case:
      int maxSeverity = getProjectMaxSeverity((IProject) element);
      ImageDescriptor overlayImageDescriptor = getDecorationForSeverity(maxSeverity);
      if (overlayImageDescriptor != null) {
        decoration.addOverlay(overlayImageDescriptor, IDecoration.BOTTOM_LEFT);
      }
    } else if (element instanceof IFile) {
      // IFile case:
      IFile file = (IFile) element;
      if (isDartProject(file.getProject())) {
        ImageDescriptor overlayImageDescriptor = getDecorationForResource(file);

        if (overlayImageDescriptor != null) {
          decoration.addOverlay(overlayImageDescriptor, IDecoration.BOTTOM_LEFT);
        }
      }
    } else if (element instanceof DartElement) {
      // DartElement case:
      DartElement dartElement = (DartElement) element;
      try {
        if (dartElement.getElementType() == DartElement.LIBRARY) {
          // DartElement, DartLibrary case:
          int maxSeverity = getLibraryMaxSeverity((DartLibrary) dartElement);
          ImageDescriptor overlayImageDescriptor = getDecorationForSeverity(maxSeverity);
          if (overlayImageDescriptor != null) {
            decoration.addOverlay(overlayImageDescriptor, IDecoration.BOTTOM_LEFT);
          }
        } else {
          // DartElement, non-DartLibrary case:
          IResource resource = dartElement.getCorrespondingResource();
          if (resource != null) {
            ImageDescriptor overlayImageDescriptor = getDecorationForResource(resource);
            if (overlayImageDescriptor != null) {
              decoration.addOverlay(overlayImageDescriptor, IDecoration.BOTTOM_LEFT);
            }
          }
        }
      } catch (DartModelException dme) {
        // ignore
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

  private ImageDescriptor getDecorationForResource(IResource resource) {
    try {
      int severity = resource.findMaxProblemSeverity(IMarker.PROBLEM, true,
          IResource.DEPTH_INFINITE);

      return getDecorationForSeverity(severity);
    } catch (CoreException ce) {
      // if there are exceptions here, we don't want the user to see a flood of exceptions
      return null;
    }
  }

  private ImageDescriptor getDecorationForSeverity(int severity) {
    if (severity == IMarker.SEVERITY_ERROR) {
      return DartPluginImages.DESC_OVR_ERROR;
    } else if (severity == IMarker.SEVERITY_WARNING) {
      return DartPluginImages.DESC_OVR_WARNING;
    } else {
      return null;
    }
  }

  private int getLibraryMaxSeverity(DartLibrary dartLibrary) {
    int maxSeverity = -1;
    if (dartLibrary == null) {
      return maxSeverity;
    }

    try {
      IResource libraryResource = dartLibrary.getCorrespondingResource();
      if (libraryResource == null) {
        return maxSeverity;
      }
      // initialize the maxSeverity with the severity of the .lib or .app file
      maxSeverity = libraryResource.findMaxProblemSeverity(IMarker.PROBLEM, true,
          IResource.DEPTH_INFINITE);
      for (CompilationUnit cu : dartLibrary.getCompilationUnits()) {
        IResource resource = cu.getCorrespondingResource();
        if (resource == null) {
          continue;
        }
        try {
          // find the max problem severity for this file
          maxSeverity = Math.max(maxSeverity,
              resource.findMaxProblemSeverity(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE));
        } catch (CoreException ce) {
        }

        // if the severity is as high as it can get, return
        if (maxSeverity == IMarker.SEVERITY_ERROR) {
          return maxSeverity;
        }
        // otherwise, the severity of this file isn't a serious as a previously found issue,
        // do nothing
      }
    } catch (DartModelException dme) {
      // ignore
    } catch (CoreException ce) {
      // ignore
    }
    return maxSeverity;
  }

  private int getProjectMaxSeverity(IProject project) {
    try {
      return project.findMaxProblemSeverity(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
    } catch (CoreException ce) {
      return -1;
    }
  }

  private boolean isDartProject(IProject project) {
    try {
      return project.hasNature(DartCore.DART_PROJECT_NATURE);
    } catch (CoreException ce) {
      return false;
    }
  }

}
