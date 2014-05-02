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
package com.google.dart.tools.core.generator;

import java.util.ArrayList;
import java.util.List;

/**
 * Create a sample web application with Pub support.
 * 
 * @coverage dart.tools.core.generator
 */
public class WebAppSample extends AbstractSample {

  public WebAppSample() {
    super("Web application", "Create a sample web application with Pub support");

    List<String[]> templates = new ArrayList<String[]>();

    templates.add(new String[] {
        "pubspec.yaml",
        "name: {name}\ndescription: A sample web application\ndependencies:\n  browser: any\n"});

    templates.add(new String[] {"web/{name.lower}.dart", "@web/webapp.dart"});
    templates.add(new String[] {"web/{name.lower}.html", "@web/webapp.html"});
    templates.add(new String[] {"web/{name.lower}.css", "@web/webapp.css"});

    setTemplates(templates);
    setMainFile("web/{name.lower}.dart");
  }

  @Override
  public boolean isValidProjectName(String name) {
    return !name.equalsIgnoreCase("browser");
  }

  @Override
  public boolean shouldBeDefault() {
    return true;
  }

}
