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
package com.google.dart.tools.ui.internal.problemsview;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;

import java.util.HashMap;
import java.util.Map;

/**
 * A manager class to gather information about registered marker types from the plugin registry.
 */
class MarkersExtManager {

  private static MarkersExtManager INSTANCE;

  public static MarkersExtManager getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new MarkersExtManager();
    }

    return INSTANCE;
  }

  private Map<String, String> idToLabelMap = new HashMap<String, String>();

  private MarkersExtManager() {
    readTypes();
  }

  public String getLabelForMarkerType(IMarker marker) {
    try {
      return getLabelforTypeId(marker.getType());
    } catch (CoreException ce) {
      return "";
    }
  }

  public String getLabelforTypeId(String typeId) {
    if (typeId == null) {
      return "";
    } else {
      return idToLabelMap.get(typeId);
    }
  }

  private void readTypes() {
    idToLabelMap.put(IMarker.PROBLEM, "Problem");
    idToLabelMap.put(IMarker.TASK, "Task");

    IExtensionPoint point = Platform.getExtensionRegistry().getExtensionPoint(
        ResourcesPlugin.PI_RESOURCES, ResourcesPlugin.PT_MARKERS);

    if (point != null) {
      // Gather all registered marker types.
      IExtension[] extensions = point.getExtensions();

      for (int i = 0; i < extensions.length; ++i) {
        IExtension ext = extensions[i];
        String id = ext.getUniqueIdentifier();
        String label = ext.getLabel();
        if (!label.equals("")) {
          idToLabelMap.put(id, label);
        }
      }
    }
  }

}
