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

package com.google.dart.tools.core.generator;

import java.util.ArrayList;
import java.util.List;

/**
 * Create a sample web application using the Polymer (polymer) package.
 * 
 * @coverage dart.tools.core.generator
 */
public class PolymerSample extends AbstractSample {

  public PolymerSample() {
    super(
        "Sample web application using the polymer library [mobile friendly]",
        "Create a sample web application using the Polymer (polymer) library");

    List<String[]> templates = new ArrayList<String[]>();

    templates.add(new String[] {
        "pubspec.yaml",
        "name: {name}\n"
            + "description: A sample Polymer application\n"
            + "dependencies:\n"
            + "    polymer: \">=0.12.0 <0.13.0\"\n"
            + "transformers:\n"
            + "- polymer:\n"
            + "    entry_points: web/{name.lower}.html\n"});
    templates.add(new String[] {"build.dart", "@webui/build.dart"});
    templates.add(new String[] {"web/{name.lower}.html", "@webui/webapp.html"});
    templates.add(new String[] {"web/{name.lower}.css", "@webui/webapp.css"});
    templates.add(new String[] {"web/clickcounter.dart", "@webui/clickcounter.dart"});
    templates.add(new String[] {"web/clickcounter.html", "@webui/clickcounter.html"});

    setTemplates(templates);
    setMainFile("web/{name.lower}.html");
  }

  @Override
  public boolean isValidProjectName(String name) {
    return !name.equalsIgnoreCase("polymer");
  }

}
