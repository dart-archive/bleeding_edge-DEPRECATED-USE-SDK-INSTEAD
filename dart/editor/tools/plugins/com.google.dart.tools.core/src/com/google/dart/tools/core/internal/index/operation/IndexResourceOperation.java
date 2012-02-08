/*
 * Copyright 2012, the Dart project authors.
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
package com.google.dart.tools.core.internal.index.operation;

import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.index.Resource;
import com.google.dart.tools.core.internal.index.contributor.IndexContributor;
import com.google.dart.tools.core.internal.index.store.IndexStore;
import com.google.dart.tools.core.internal.util.ResourceUtil;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;

import org.eclipse.core.resources.IFile;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Instances of the class <code>IndexResourceOperation</code> implement an operation that adds
 * data to the index based on the content of a specified resource.
 */
public class IndexResourceOperation implements IndexOperation {
  /**
   * The index store against which this operation is being run.
   */
  private IndexStore indexStore;

  /**
   * The resource being indexed.
   */
  private Resource resource;

  /**
   * The fully resolved AST structure representing the contents of the resource.
   */
  private DartUnit unit;

  /**
   * Initialize a newly created operation that will index the specified resource.
   * 
   * @param indexStore the index store against which this operation is being run
   * @param resource the resource being indexed
   * @param unit the fully resolved AST structure representing the contents of the resource
   */
  public IndexResourceOperation(IndexStore indexStore, Resource resource, DartUnit unit) {
    this.indexStore = indexStore;
    this.resource = resource;
    this.unit = unit;
  }

  @Override
  public void performOperation() {
    try {
      IFile file = ResourceUtil.getResource(new URI(resource.getResourceId()));
      DartElement element = DartCore.create(file);
      if (element instanceof CompilationUnit) {
        synchronized (indexStore) {
          indexStore.regenerateResource(resource);
          IndexContributor contributor = new IndexContributor(indexStore, (CompilationUnit) element);
          unit.accept(contributor);
        }
      }
    } catch (URISyntaxException exception) {
      DartCore.logError("Could not index resource, invalid URI: \"" + resource.getResourceId()
          + "\"", exception);
    }
  }
}
