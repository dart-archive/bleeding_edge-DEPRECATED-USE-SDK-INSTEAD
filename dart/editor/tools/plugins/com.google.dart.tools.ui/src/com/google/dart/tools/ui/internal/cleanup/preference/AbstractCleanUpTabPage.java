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
package com.google.dart.tools.ui.internal.cleanup.preference;

import com.google.dart.tools.ui.cleanup.CleanUpOptions;
import com.google.dart.tools.ui.internal.cleanup.AbstractCleanUp;
import com.google.dart.tools.ui.internal.cleanup.MapCleanUpOptions;

import java.util.Map;

public abstract class AbstractCleanUpTabPage extends CleanUpTabPage {

  private AbstractCleanUp[] fPreviewCleanUps;
  private Map<String, String> fValues;

  public AbstractCleanUpTabPage() {
    super();
  }

  /*
   * @see org.eclipse.jdt.internal.ui.preferences.cleanup.ICleanUpTabPage#getPreview()
   */
  @Override
  public String getPreview() {
    if (fPreviewCleanUps == null) {
      fPreviewCleanUps = createPreviewCleanUps(fValues);
    }

    StringBuffer buf = new StringBuffer();
    for (int i = 0; i < fPreviewCleanUps.length; i++) {
      buf.append(fPreviewCleanUps[i].getPreview());
      buf.append("\n"); //$NON-NLS-1$
    }
    return buf.toString();
  }

  /*
   * @see
   * org.eclipse.jdt.internal.ui.preferences.cleanup.ICleanUpTabPage#setOptions(org.eclipse.jdt.
   * internal.ui.fix.CleanUpOptions)
   */
  @Override
  public void setOptions(CleanUpOptions options) {
  }

  /*
   * @see
   * org.eclipse.jdt.internal.ui.preferences.cleanup.CleanUpTabPage#setWorkingValues(java.util.Map)
   */
  @Override
  public void setWorkingValues(Map<String, String> workingValues) {
    super.setWorkingValues(workingValues);
    fValues = workingValues;
    setOptions(new MapCleanUpOptions(workingValues));
  }

  protected abstract AbstractCleanUp[] createPreviewCleanUps(Map<String, String> values);

}
