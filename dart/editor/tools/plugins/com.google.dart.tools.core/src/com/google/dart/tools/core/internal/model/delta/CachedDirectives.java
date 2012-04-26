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
package com.google.dart.tools.core.internal.model.delta;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * This class is a wrapper for the {@link DeltaProcessor} to contain three {@link Set}s of
 * {@link String}s, one for each of the <code>imports</code>, <code>sources</code> and
 * <code>resources</code>.
 */
public class CachedDirectives {

  public static final Set<CachedLibraryImport> EMPTY_IMP_SET = Collections.unmodifiableSet(new HashSet<CachedLibraryImport>(
      0));
  public static final Set<String> EMPTY_STR_SET = Collections.unmodifiableSet(new HashSet<String>(0));

  private final String libraryName;

  private final Set<CachedLibraryImport> imports;
  private final Set<String> sources;
  private final Set<String> resources;

  /**
   * The empty constructor for {@link CachedDirectives} creates three empty sets.
   */
  public CachedDirectives() {
    this("", EMPTY_IMP_SET, EMPTY_STR_SET, EMPTY_STR_SET);
  }

  /**
   * This is the only constructor for a {@link CachedDirectives} object which actually has content
   * to return.
   * 
   * @param imports some set of "import" {@link CachedLibraryImport}s
   * @param sources some set of "source" {@link String}s
   * @param resources some set of "resource" {@link String}s
   */
  public CachedDirectives(String libraryName, Set<CachedLibraryImport> imports,
      Set<String> sources, Set<String> resources) {
    this.libraryName = libraryName;
    this.imports = Collections.unmodifiableSet(imports);
    this.sources = Collections.unmodifiableSet(sources);
    this.resources = Collections.unmodifiableSet(resources);
  }

  /**
   * Returns an unmodifiable {@link Set} which was created using the <code>imports</code>
   * constructor argument.
   */
  public Set<CachedLibraryImport> getImports() {
    return imports;
  }

  /**
   * Returns the library name which was created using the <code>libraryName</code> constructor
   * argument.
   */
  public String getLibraryName() {
    return libraryName;
  }

  /**
   * Returns an unmodifiable {@link Set} which was created using the <code>resources</code>
   * constructor argument.
   */
  public Set<String> getResources() {
    return resources;
  }

  /**
   * Returns an unmodifiable {@link Set} which was created using the <code>sources</code>
   * constructor argument.
   */
  public Set<String> getSources() {
    return sources;
  }
}
