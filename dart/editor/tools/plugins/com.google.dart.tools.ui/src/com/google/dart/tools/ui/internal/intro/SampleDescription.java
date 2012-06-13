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

import java.io.File;

/**
 * Description for Dart sample.
 */
public final class SampleDescription implements Comparable<SampleDescription> {
  public final File directory;
  public final String file;
  public final String name;
  public final String description;

  public final File logo;

  private final String keywords;

  public SampleDescription(File directory, String file, String name, String description,
      String keywords, File logo) {
    this.directory = directory;
    this.file = file;
    this.name = name;
    this.description = description;
    this.keywords = keywords;
    this.logo = logo;
  }

  @Override
  public int compareTo(SampleDescription o) {
    return name.compareToIgnoreCase(o.name);
  }

  public String getKeywords() {
    return (keywords == null ? "" : keywords);
  }

}
