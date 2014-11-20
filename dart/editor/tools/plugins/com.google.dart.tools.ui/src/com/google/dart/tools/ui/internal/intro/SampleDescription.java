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
package com.google.dart.tools.ui.internal.intro;

/**
 * A description of a Dart sample.
 */
public final class SampleDescription implements Comparable<SampleDescription> {
  public final String name;
  public final String description;
  public final String file;
  public final String url;
  public final boolean earlyAccess;
  public final String logoPath;

  public SampleDescription(String name, String description, String file, String url,
      boolean earlyAccess, String logoPath) {
    this.name = name;
    this.description = description;
    this.file = file;
    this.url = url;
    this.earlyAccess = earlyAccess;
    this.logoPath = logoPath;
  }

  @Override
  public int compareTo(SampleDescription other) {
    return name.compareToIgnoreCase(name);
  }

  @Override
  public String toString() {
    return name;
  }
}
