/*
 * Copyright (c) 2014, the Dart project authors.
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
package com.google.dart.engine.source;

import com.google.dart.engine.internal.context.TimestampedData;
import com.google.dart.engine.utilities.general.ObjectUtilities;
import com.google.dart.engine.utilities.translation.DartOmit;

import java.net.URI;

/**
 * An implementation of an non-existing {@link Source}.
 * 
 * @coverage dart.engine.source
 */
public class NonExistingSource implements Source {
  private final String name;
  private final UriKind uriKind;

  public NonExistingSource(String name, UriKind uriKind) {
    this.name = name;
    this.uriKind = uriKind;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof NonExistingSource) {
      NonExistingSource other = (NonExistingSource) obj;
      return other.uriKind == uriKind && ObjectUtilities.equals(other.name, name);
    }
    return false;
  }

  @Override
  public boolean exists() {
    return false;
  }

  @Override
  public TimestampedData<CharSequence> getContents() throws Exception {
    throw new UnsupportedOperationException(name + "does not exist.");
  }

  @Override
  @Deprecated
  @DartOmit
  public void getContentsToReceiver(ContentReceiver receiver) throws Exception {
    throw new UnsupportedOperationException(name + "does not exist.");
  }

  @Override
  public String getEncoding() {
    throw new UnsupportedOperationException(name + "does not exist.");
  }

  @Override
  public String getFullName() {
    return name;
  }

  @Override
  public long getModificationStamp() {
    return 0;
  }

  @Override
  public String getShortName() {
    return name;
  }

  @Override
  public UriKind getUriKind() {
    return uriKind;
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  @Override
  public boolean isInSystemLibrary() {
    return false;
  }

  @Override
  public Source resolveRelative(URI relativeUri) {
    throw new UnsupportedOperationException(name + "does not exist.");
  }
}
