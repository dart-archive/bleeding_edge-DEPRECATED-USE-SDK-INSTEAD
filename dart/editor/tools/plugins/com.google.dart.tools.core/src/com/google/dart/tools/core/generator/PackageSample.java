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
package com.google.dart.tools.core.generator;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates a structure for a pub package with directories and files as a starting point for the
 * user.
 * 
 * @coverage dart.tools.core.generator
 */
public class PackageSample extends AbstractSample {

  public PackageSample() {
    super("Package Template", "Create a Pub package structure");

    List<String[]> templates = new ArrayList<String[]>();

    templates.add(new String[] {"pubspec.yaml", "name: {name}\ndescription: A pub package \n"});

    templates.add(new String[] {"lib/{name.lower}.dart", "@pub/lib.dart"});

    setTemplates(templates);
    setMainFile("lib/{name.lower}.dart");
  }

}
