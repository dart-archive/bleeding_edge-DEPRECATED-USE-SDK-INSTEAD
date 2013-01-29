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
 * Create a sample web application using the Web UI (web_ui) package.
 */
public class WebUiSample extends AbstractSample {

  public WebUiSample() {
    super(
        "Web application (using the web_ui library)",
        "Create a sample web application using the Web UI (web_ui) library");

    List<String[]> templates = new ArrayList<String[]>();

    templates.add(new String[] {
        "pubspec.yaml",
        "name: {name}\ndescription: A sample WebUI application\n"
            + "dependencies:\n  browser: any\n  js: any\n  web_ui: any\n"});
    templates.add(new String[] {"build.dart", "@webui/build.dart"});
    templates.add(new String[] {"web/{name.lower}.dart", "@webui/webapp.dart"});
    templates.add(new String[] {"web/{name.lower}.html", "@webui/webapp.html"});
    templates.add(new String[] {"web/{name.lower}.css", "@webui/webapp.css"});
    templates.add(new String[] {"web/xclickcounter.dart", "@webui/xclickcounter.dart"});
    templates.add(new String[] {"web/xclickcounter.html", "@webui/xclickcounter.html"});

    setTemplates(templates);
    setMainFile("web/{name.lower}.html");
  }

}
