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
package com.google.dart.tools.core.pub;

import com.google.dart.tools.core.DartCore;

import junit.framework.TestCase;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import java.util.List;

public class RunPubCacheListJobTest extends TestCase {

  public void test_run() throws Exception {
    RunPubCacheListJob job = new RunPubCacheListJob() {

      @Override
      public IStatus run(IProgressMonitor monitor) {
        List<String> args = buildCacheListCommand();
        assertNotNull(args);
        assertEquals(3, args.size());
        assertEquals(RunPubCacheListJob.CACHE_COMMAND, args.get(1));
        assertEquals(RunPubCacheListJob.CACHELIST_COMMAND, args.get(2));
        return new Status(IStatus.OK, DartCore.PLUGIN_ID, "{\"packages\":{\"analyzer\":"
            + "{\"0.5.0+1\":\"location\":"
            + "\"/.pub-cache/hosted/pub.dartlang.org/analyzer-0.5.0+1\"}}}");
      }
    };

    IStatus status = job.run(null);
    assertNotNull(status);
    assertTrue(status.isOK());
    assertTrue(status.getMessage().startsWith("{\"packages\":"));
  }

}
