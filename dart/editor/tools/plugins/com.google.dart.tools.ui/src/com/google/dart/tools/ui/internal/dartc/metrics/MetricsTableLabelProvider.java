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
package com.google.dart.tools.ui.internal.dartc.metrics;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

/**
 * LableProvider for the {@link MetricsView} listViewer
 */
public class MetricsTableLabelProvider extends LabelProvider implements ITableLabelProvider {

  private MetricsManager manager;

  @Override
  public Image getColumnImage(Object element, int columnIndex) {
    return null;
  }

  @Override
  public String getColumnText(Object element, int columnIndex) {
    if (!(element instanceof String)) {
      return "ERROR: only Strings should be passed in the first column";
    }
    String title = (String) element;
    if (columnIndex == 0) {
      return (String) element;
    } else if (columnIndex == 1) {
      if (manager.getStat(title) != null) {
        return manager.getStat(title);
      } else {
        return "ERROR: no data here";
      }
    } else {
      return "ERROR: unhandled column passed";
    }
  }

  public void setManager(MetricsManager manager) {
    this.manager = manager;
  }
}
