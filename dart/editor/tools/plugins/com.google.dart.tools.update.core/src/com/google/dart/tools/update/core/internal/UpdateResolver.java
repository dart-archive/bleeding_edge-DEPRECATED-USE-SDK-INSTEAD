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
package com.google.dart.tools.update.core.internal;

import com.google.dart.tools.update.core.Revision;
import com.google.dart.tools.update.core.UpdateCore;

import java.io.IOException;

/**
 * Subclasses implement a strategy for resolving revisions available for update.
 */
public abstract class UpdateResolver {

  /**
   * Infers a latest build from a directory listing fetched from a URL.
   */
  private static class DirectoryListingResolver extends UpdateResolver {

    private final String url;

    DirectoryListingResolver(String url) {
      this.url = url;
    }

    @Override
    public Revision getLatest() throws IOException {
      return UpdateUtils.getLatestRevision(url);
    }

  }

  /**
   * Create an update resolver for the continuous update channel.
   * 
   * @return an update resolver for the continuous update channel
   */
  public static UpdateResolver forIntegration() {
    return new DirectoryListingResolver(UpdateCore.getUpdateUrl());
  }

  /**
   * Resolve the latest available revision for update.
   * 
   * @return the latest revision, or <code>null</code> if none is found
   * @throws IOException if an exception occurred in retrieving the revision
   */
  public abstract Revision getLatest() throws IOException;

}
