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

import com.google.dart.tools.core.dart2js.ProcessRunner;

import org.eclipse.core.resources.IContainer;

import java.util.Map;

/**
 * PubJobTestRunner should be used in conjunction with {@link PubPackageServer} for all tests that
 * call out to pub. The environment variable PUB_HOSTED_URL is used to direct all of pub's request
 * to the server that is started by the {@link PubPackageServer} instead of to pub.dartlang.org
 */
public class PubJobRunner extends RunPubJob {

  public PubJobRunner(IContainer container, String command) {
    super(container, command, false);

  }

  @Override
  protected ProcessRunner newProcessRunner(ProcessBuilder builder) {
    Map<String, String> env = builder.environment();
    env.put("PUB_HOSTED_URL", "http://localhost:" + PubPackageServer.pubServerPort);
    return super.newProcessRunner(builder);
  }

}
