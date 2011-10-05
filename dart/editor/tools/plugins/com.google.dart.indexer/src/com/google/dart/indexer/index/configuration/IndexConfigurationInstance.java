/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.google.dart.indexer.index.configuration;

import com.google.dart.indexer.index.layers.Layer;
import com.google.dart.indexer.index.layers.LayerId;

import org.eclipse.core.resources.IFile;

/**
 * Holds an immutable set of options required to build the index. Currently this set consists only
 * of the registered processors and contributors.
 * <p>
 * The clients should create configurations using the <code>IndexConfigurationBuilder</code> class.
 */
public interface IndexConfigurationInstance {
  public String describe();

  /**
   * Return an array containing all of the processors that should be given an opportunity to process
   * the given file.
   * 
   * @return the processors that should be given an opportunity to process the given file
   */
  public Processor[] findProcessors(IFile file);

  public long gatherTimeSpentParsing();

  /**
   * Return an array containing all of the processors that have been created.
   * 
   * @return an array containing all of the processors that have been created
   */
  public Processor[] getKnownProcessors();

  public Layer getLayer(int ordinal);

  public Layer getLayer(LayerId layerId);

  public Layer[] getLayers();

  public boolean isIndexedFile(IFile file);
}
