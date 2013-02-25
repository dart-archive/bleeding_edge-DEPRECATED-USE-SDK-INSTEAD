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
package com.google.dart.indexer.index.updating;

import com.google.dart.indexer.exceptions.IndexRequestFailed;
import com.google.dart.indexer.exceptions.IndexRequiresFullRebuild;
import com.google.dart.indexer.exceptions.IndexTemporarilyNonOperational;
import com.google.dart.indexer.locations.Location;
import com.google.dart.indexer.source.IndexableSource;

import org.eclipse.core.resources.IFile;

public interface DependenciesBuilder {
  public void currentFileAffectsLocationOfCurrentLayer(Location location)
      throws IndexRequiresFullRebuild, IndexTemporarilyNonOperational, IndexRequestFailed;

  @Deprecated
  public void currentLocationDependsOnFile(IFile file) throws IndexRequiresFullRebuild,
      IndexTemporarilyNonOperational, IndexRequestFailed;

  public void currentLocationDependsOnFile(IndexableSource source) throws IndexRequiresFullRebuild,
      IndexTemporarilyNonOperational, IndexRequestFailed;
}
