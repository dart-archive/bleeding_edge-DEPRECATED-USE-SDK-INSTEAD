/*
 * Copyright 2011 Dart project authors.
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
package com.google.dart.tools.core.utilities.compiler;

import com.google.dart.tools.core.internal.builder.RootArtifactProvider;
import com.google.dart.tools.core.internal.compiler.TestCompilerListener;
import com.google.dart.tools.core.test.util.FileOperation;
import com.google.dart.tools.core.test.util.TestUtilities;

import junit.framework.TestCase;

import java.io.File;

public class DartCompilerWarmupTest extends TestCase {

  public void test_DartCompilerWarmup_testWarmUpCompiler() throws Exception {
    TestUtilities.runWithTempDirectory(new FileOperation() {

      @Override
      public void run(File tempDir) throws Exception {
        File cacheFile = new File(tempDir, "artifact.cache");

        // The first time that the compiler is warmed up,
        // there are no cached artifacts for bundled libraries on disk

        RootArtifactProvider provider = RootArtifactProvider.newInstanceForTesting();
        warmupCompiler(provider, 1);
        provider.saveCachedArtifacts(cacheFile);

        // The second and subsequent times that the compiler is warmed up,
        // it reads the cached artifacts for bundled libraries from disk

        provider = RootArtifactProvider.newInstanceForTesting();
        provider.loadCachedArtifacts(cacheFile);
        warmupCompiler(provider, 2);
      }
    });
  }

  private void warmupCompiler(RootArtifactProvider newInstanceForTesting, int count) {
    RootArtifactProvider provider = newInstanceForTesting;
    TestCompilerListener listener = new TestCompilerListener();
    long start = System.currentTimeMillis();
    DartCompilerWarmup.warmUpCompiler(provider, listener);
    long delta = System.currentTimeMillis() - start;
    System.out.println(getClass().getSimpleName() + " warmup compiler " + count + " : " + delta
        + " ms");
    listener.assertAllErrorsReported();
  }

}
