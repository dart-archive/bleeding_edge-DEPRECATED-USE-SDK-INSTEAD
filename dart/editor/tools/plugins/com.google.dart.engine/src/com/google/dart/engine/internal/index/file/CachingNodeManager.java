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

package com.google.dart.engine.internal.index.file;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.Uninterruptibles;
import com.google.dart.engine.context.AnalysisContext;

import java.util.concurrent.TimeUnit;

/**
 * A {@link NodeManager} that caches {@link IndexNode} accessed from another {@link NodeManager}.
 * <p>
 * By default up to 64 nodes are cached for 5 seconds.
 * 
 * @coverage dart.engine.index
 */
public class CachingNodeManager implements NodeManager {
  /**
   * A {@link Thread} that performs {@link #cache} clean up.
   */
  private class CleanUpThread extends Thread {
    public CleanUpThread() {
      super("IndexNode cache clean up thread");
      setDaemon(true);
    }

    @Override
    public void run() {
      while (true) {
        cache.cleanUp();
        Uninterruptibles.sleepUninterruptibly(1, TimeUnit.SECONDS);
      }
    }
  }

  private final NodeManager manager;
  private final Cache<String, IndexNode> cache;

  public CachingNodeManager(NodeManager manager) {
    this.manager = manager;
    {
      CacheBuilder<Object, Object> builder = CacheBuilder.newBuilder();
      cache = builder.maximumSize(64).expireAfterAccess(5, TimeUnit.SECONDS).build();
    }
    new CleanUpThread().start();
  }

  @Override
  public void clear() {
    manager.clear();
    cache.invalidateAll();
  }

  @Override
  public ContextCodec getContextCodec() {
    return manager.getContextCodec();
  }

  @Override
  public ElementCodec getElementCodec() {
    return manager.getElementCodec();
  }

  @Override
  public int getLocationCount() {
    return manager.getLocationCount();
  }

  @Override
  public IndexNode getNode(String name) {
    IndexNode node = cache.getIfPresent(name);
    if (node == null) {
      node = manager.getNode(name);
      cache.put(name, node);
    }
    return node;
  }

  @Override
  public StringCodec getStringCodec() {
    return manager.getStringCodec();
  }

  @Override
  public IndexNode newNode(AnalysisContext context) {
    return manager.newNode(context);
  }

  @Override
  public void putNode(String name, IndexNode node) {
    cache.put(name, node);
    manager.putNode(name, node);
  }

  @Override
  public void removeNode(String name) {
    cache.invalidate(name);
    manager.removeNode(name);
  }
}
