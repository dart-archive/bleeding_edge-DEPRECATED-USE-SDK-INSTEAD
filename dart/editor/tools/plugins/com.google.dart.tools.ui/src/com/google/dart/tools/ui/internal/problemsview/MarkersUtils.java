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
import org.eclipse.core.runtime.CoreException;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A utility class for dealing with IMarkers.
 */
class MarkersUtils {

  private static MarkersUtils INSTANCE;

  private static final String TASK_CATEGORY = "task";

  private static final String WARNING_CATEGORY = "warning";

  private static final String ERROR_CATEGORY = "problem";

  public static MarkersUtils getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new MarkersUtils();
    }

    return INSTANCE;
  }

  private static String pluralize(String word, int count) {
    return (count == 1 ? word : word + "s");
  }

  private List<String> markerIds = new ArrayList<String>();
  private static final String INFO_CATEGORY = "info";

  private MarkersUtils() {
    markerIds.add(IMarker.PROBLEM);
    markerIds.add(IMarker.TASK);
  }

  public String[] getErrorsViewMarkerIds() {
    return markerIds.toArray(new String[markerIds.size()]);
  }

  public String summarizeMarkers(List<IMarker> markers) {
    Map<String, Integer> counts = getMarkerCounts(markers);

    StringBuilder builder = new StringBuilder();

    for (String category : counts.keySet()) {
      Integer count = counts.get(category);

      if (count > 0) {
        if (builder.length() > 0) {
          builder.append(", ");
        }

        builder.append(NumberFormat.getIntegerInstance().format(count));
        builder.append(" ");
        builder.append(pluralize(category, count));
      }
    }

    String str = builder.toString();

    return str.length() == 0 ? "no items" : str;
  }

  private Map<String, Integer> getMarkerCounts(List<IMarker> markers) {
    Map<String, int[]> counts = new LinkedHashMap<String, int[]>();

    final String[] catLookup = {INFO_CATEGORY, TASK_CATEGORY, WARNING_CATEGORY, ERROR_CATEGORY};

    counts.put(ERROR_CATEGORY, new int[1]);
    counts.put(WARNING_CATEGORY, new int[1]);
    counts.put(TASK_CATEGORY, new int[1]);
    counts.put(INFO_CATEGORY, new int[1]);

    for (IMarker marker : markers) {
      int severity = marker.getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO);

      if (severity == IMarker.SEVERITY_INFO) {
        try {
          if (marker.isSubtypeOf(IMarker.TASK)) {
            severity = 1;
          }
        } catch (CoreException ce) {
          // ignore

        }
      } else if (severity == IMarker.SEVERITY_WARNING) {
        severity = 2;
      } else if (severity == IMarker.SEVERITY_ERROR) {
        severity = 3;
      }

      String category = catLookup[severity];

      counts.get(category)[0]++;
    }

    Map<String, Integer> result = new LinkedHashMap<String, Integer>();

    for (String category : counts.keySet()) {
      int count = counts.get(category)[0];

      result.put(category, count);
    }

    return result;
  }

}
