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
package com.google.dart.tools.update.core;

import com.google.dart.tools.update.core.internal.UpdateUtils;

import org.eclipse.core.runtime.IPath;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * A build revision number.
 */
public class Revision implements Comparable<Revision> {

  /**
   * A null object to signify the revision is unknown.
   * <p>
   * NOTE: this will be true in case a revision is requested for a runtime workbench instance of the
   * editor.
   */
  public static final Revision UNKNOWN = new Revision(-1);

  /**
   * Create a revision for the given (numeric) string value.
   * 
   * @param revision the numeric value
   * @return a new revision or {@link Revision#UNKNOWN} if the numeric value cannot be parsed
   */
  public static Revision forValue(String revision) {
    try {
      int intValue = Integer.parseInt(revision);
      return new Revision(intValue);
    } catch (NumberFormatException e) {
      return UNKNOWN;
    }
  }

  private final Integer revisionNumber;

  /**
   * Create a revision with the given number.
   * 
   * @param revisionNumber the revision number
   */
  public Revision(int revisionNumber) {
    this.revisionNumber = revisionNumber;
  }

  @Override
  public int compareTo(Revision o) {
    return revisionNumber.compareTo(o.revisionNumber);
  }

  /**
   * Get a path describing where this revision is (or will be) staged in the editor's updates/
   * directory in the local file system.
   * 
   * @return the local path
   */
  public IPath getLocalPath() {
    return UpdateUtils.getPath(this);
  }

  /**
   * Get a platform-aware download URL for this revision.
   * 
   * @return the download URL
   * @throws MalformedURLException
   */
  public URL getUrl() throws MalformedURLException {
    return UpdateUtils.getUrl(this);
  }

  /**
   * Check to see if this revision is equal to another.
   * 
   * @param rev the revision to compare to
   * @return <code>true</code> if equal, <code>false</code> otherwise
   */
  public boolean isEqualTo(Revision rev) {
    return compareTo(rev) == 0;
  }

  /**
   * Check to see if the current revision is more current than another.
   * 
   * @param rev the revision to compare against
   * @return <code>true</code> if the current revision is more current, <code>false</code> otherwise
   */
  public boolean isMoreCurrentThan(Revision rev) {
    return compareTo(rev) > 0;
  }

  @Override
  public String toString() {
    return revisionNumber.toString();
  }

}
