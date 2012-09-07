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
package com.google.dart.tools.ui.internal.problemsview;

import com.google.dart.tools.ui.DartToolsPlugin;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.graphics.Image;

import java.util.HashMap;
import java.util.Map;

/**
 * A utility class to get images corresponding to markers.
 */
class AnnotationTypesExtManager {

  private static class ImageData {
    public String pluginId;
    public String iconPath;

    public ImageData(String pluginId, String iconPath) {
      this.pluginId = pluginId;
      this.iconPath = iconPath;
    }

    Image getImage() {
      return DartToolsPlugin.getImage("/" + pluginId + "/" + iconPath);
    }
  }

  private static AnnotationTypesExtManager SINGLETON;

  // org.eclipse.ui.editors.annotationTypes maps from markerType and severity to annotation ids

  // org.eclipse.ui.editors.markerAnnotationSpecification has all the details for annotation ids

  public static AnnotationTypesExtManager getModel() {
    if (SINGLETON == null) {
      SINGLETON = new AnnotationTypesExtManager();
    }

    return SINGLETON;
  }

  private Map<String, Image> imageDataForMarkerType = new HashMap<String, Image>();

  private AnnotationTypesExtManager() {
    parseExtensions();
  }

  public Image getImageForMarker(IMarker marker) {
    int severity = marker.getAttribute(IMarker.SEVERITY, -1);

    if (severity != -1) {
      try {
        String markerType = marker.getType();

        String key = markerType + "." + severity;

        if (imageDataForMarkerType.get(key) != null) {
          return imageDataForMarkerType.get(key);
        } else if (imageDataForMarkerType.get(markerType) != null) {
          return imageDataForMarkerType.get(markerType);
        }
      } catch (CoreException ex) {

      }
    }

    switch (severity) {
      case IMarker.SEVERITY_ERROR:
        return DartToolsPlugin.getImage("icons/full/misc/error_tsk.gif");
      case IMarker.SEVERITY_WARNING:
        return DartToolsPlugin.getImage("icons/full/misc/warn_tsk.gif");
      case IMarker.SEVERITY_INFO:
        return DartToolsPlugin.getImage("icons/full/misc/info_tsk.gif");
    }

    return DartToolsPlugin.getImage("icons/full/misc/info_tsk.gif");
  }

  private ImageData findIconDataForAnnotation(String annotationId) {
    IConfigurationElement[] elements = Platform.getExtensionRegistry().getConfigurationElementsFor(
        "org.eclipse.ui.editors.markerAnnotationSpecification");

    for (IConfigurationElement element : elements) {
      if ("specification".equals(element.getName())) {
        String annotationType = element.getAttribute("annotationType");
        String iconPath = element.getAttribute("icon");

        if (annotationId.equals(annotationType) && iconPath != null) {
          return new ImageData(element.getDeclaringExtension().getContributor().getName(), iconPath);
        }
      }
    }

    return null;
  }

  private void parseExtensions() {
    IConfigurationElement[] elements = Platform.getExtensionRegistry().getConfigurationElementsFor(
        "org.eclipse.ui.editors.annotationTypes");

    for (IConfigurationElement element : elements) {
      if ("type".equals(element.getName())) {

        String annotationId = element.getAttribute("name");

        String markerType = element.getAttribute("markerType");
        String markerSeverity = element.getAttribute("markerSeverity");

        if (annotationId != null && markerType != null) {
          ImageData data = findIconDataForAnnotation(annotationId);

          if (data != null) {
            if (markerSeverity != null) {
              imageDataForMarkerType.put(markerType + "." + markerSeverity, data.getImage());
            } else {
              imageDataForMarkerType.put(markerType, data.getImage());
            }
          }
        }
      }
    }
  }

}
