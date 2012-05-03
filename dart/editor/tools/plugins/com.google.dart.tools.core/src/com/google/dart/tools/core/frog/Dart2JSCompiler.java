/*
 * Copyright 2012 Dart project authors.
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

package com.google.dart.tools.core.frog;

import org.eclipse.core.runtime.IPath;

import java.util.ArrayList;
import java.util.List;

/**
 * Launch the dart2js process and collect stdout, stderr, and exit code information.
 */
public class Dart2JSCompiler extends FrogCompiler {

  /**
   * Create a new Dart2JSCompiler.
   */
  public Dart2JSCompiler() {

  }

  @Override
  public String getName() {
    return "dart2js";
  }

  @Override
  protected List<String> getCompilerArguments(IPath inputPath, IPath outputPath) {
    List<String> args = new ArrayList<String>();

    args.add("dart2js/lib/compiler/implementation/dart2js.dart");
    args.add("--no-colors");
    args.add("--suppress-warnings");
    //args.add("--library-root=" + DartSdk.getInstance().getLibraryDirectory().getPath());
    args.add("--out=" + outputPath.toOSString());
    args.add(inputPath.toOSString());

    return args;
  }

}
