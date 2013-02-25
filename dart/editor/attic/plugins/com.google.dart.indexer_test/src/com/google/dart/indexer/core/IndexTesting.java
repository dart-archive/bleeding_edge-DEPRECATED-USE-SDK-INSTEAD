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
package com.google.dart.indexer.core;

import com.google.dart.indexer.IndexerPlugin;
import com.google.dart.indexer.exceptions.IndexRequestFailed;
import com.google.dart.indexer.index.IndexSession;
import com.google.dart.indexer.index.IndexTransaction;
import com.google.dart.indexer.index.readonly.Index;
import com.google.dart.indexer.workspace.index.IndexingTarget;

import java.io.File;

public class IndexTesting {
  public static Index buildIndex(IndexSession session, IndexingTarget target)
      throws IndexRequestFailed {
    Index index = session.createEmptyRegularIndex();
    IndexTesting.updateIndex(index, session, target);
    session.getStorage().checkpoint();
    File metadata = new File(getWorkspaceIndexMetadataLocation());
    return session.createRegularIndex(metadata);
  }

  public static void updateIndex(Index index, IndexSession session, IndexingTarget target)
      throws IndexRequestFailed {
    IndexTransaction transaction = session.createTransaction(index);
    transaction.indexTarget(target);
    transaction.close();
  }

  /**
   * Copied from WorkspaceIndexer.
   */
  private static String getWorkspaceIndexMetadataLocation() {
    return IndexerPlugin.getDefault().getStateLocation().toFile().getAbsolutePath();
  }
}
