/*
 * Copyright (c) 2013, the Dart project authors.
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
package com.google.dart.engine.internal.context;

import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceKind;

import java.util.ArrayList;

/**
 * Instances of the class {@code SourceInfo} maintain the information known by an analysis context
 * about an individual source.
 */
public class SourceInfo {
  /**
   * The source about which information is being maintained.
   */
//  private Source source;

  /**
   * The kind of the source.
   */
  private SourceKind kind;

  /**
   * The sources for the defining compilation units for the libraries containing the source, or
   * {@code null} if the libraries containing the source are not yet known.
   */
  private ArrayList<Source> librarySources = null;

  public SourceInfo(Source source, SourceKind kind) {
//    this.source = source;
    this.kind = kind;
  }

  /**
   * Initialize a newly created information holder to hold the same information as the given holder.
   * 
   * @param info the information holder used to initialize this holder
   */
  public SourceInfo(SourceInfo info) {
//  source = info.source;
    kind = info.kind;
    librarySources = new ArrayList<Source>(info.librarySources);
  }

  /**
   * Add the given source to the list of sources for the defining compilation units for the
   * libraries containing this source.
   * 
   * @param source the source to be added to the list
   */
  public void addLibrarySource(Source source) {
    if (librarySources == null) {
      librarySources = new ArrayList<Source>();
    }
    librarySources.add(source);
  }

  /**
   * Return the kind of the source.
   * 
   * @return the kind of the source
   */
  public SourceKind getKind() {
    return kind;
  }

  /**
   * Return the sources for the defining compilation units for the libraries containing this source.
   * 
   * @return the sources for the defining compilation units for the libraries containing this source
   */
  public Source[] getLibrarySources() {
    if (librarySources == null) {
      return Source.EMPTY_ARRAY;
    }
    return librarySources.toArray(new Source[librarySources.size()]);
  }

  /**
   * Remove the given source from the list of sources for the defining compilation units for the
   * libraries containing this source.
   * 
   * @param source the source to be removed to the list
   */
  public void removeLibrarySource(Source source) {
    librarySources.remove(source);
    if (librarySources.isEmpty()) {
      librarySources = null;
    }
  }

  /**
   * Set the kind of the source to the given kind.
   * 
   * @param kind the kind of the source
   */
  public void setKind(SourceKind kind) {
    this.kind = kind;
  }
}
