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

package com.google.dart.tools.debug.core.webkit;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * A WIP script object.
 */
public class WebkitScript {

  public static WebkitScript createFrom(JSONObject params) throws JSONException {
    WebkitScript script = new WebkitScript();

    script.scriptId = JsonUtils.getString(params, "scriptId");
    script.url = JsonUtils.getString(params, "url");
    script.startLine = JsonUtils.getInt(params, "startLine", -1);
    script.startColumn = JsonUtils.getInt(params, "startLine", -1);
    script.endLine = JsonUtils.getInt(params, "endLine", -1);
    script.endColumn = JsonUtils.getInt(params, "endColumn", -1);
    script.isContentScript = JsonUtils.getBoolean(params, "isContentScript");
    script.sourceMapURL = JsonUtils.getString(params, "sourceMapURL");

    script.patchupScriptUrl();

    return script;
  }

  private String sourceMapURL;

  private boolean isContentScript;

  private int endLine;

  private int endColumn;

  private int startColumn;

  private int startLine;

  private String url;

  private String scriptId;

  private String scriptSource;

  private Object privateData;

  private WebkitScript() {

  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }

    if (obj instanceof WebkitScript) {
      WebkitScript other = (WebkitScript) obj;

      return scriptId.equals(other.getScriptId());
    }

    return false;
  }

  public int getEndColumn() {
    return endColumn;
  }

  public int getEndLine() {
    return endLine;
  }

  public Object getPrivateData() {
    return privateData;
  }

  public String getScriptId() {
    return scriptId;
  }

  public String getScriptSource() {
    return scriptSource;
  }

  public String getSourceMapURL() {
    return sourceMapURL;
  }

  public int getStartColumn() {
    return startColumn;
  }

  public int getStartLine() {
    return startLine;
  }

  public String getUrl() {
    return url;
  }

  @Override
  public int hashCode() {
    return scriptId.hashCode();
  }

  public boolean hasScriptSource() {
    return scriptSource != null;
  }

  public boolean isContentScript() {
    return isContentScript;
  }

  public boolean isSystemScript() {
    if (url == null) {
      return false;
    }

    if (url.startsWith("dart:")) {
      return true;
    }

    // TODO(devoncarew): this hack has to do with how Dartium is built, and will go away at some point
    if (url.startsWith("/Volumes/data/b/build/") || url.startsWith("e:\\b\\build\\slave")) {
      return true;
    }

    return false;
  }

  public void setPrivateData(Object object) {
    this.privateData = object;
  }

  public void setScriptSource(String source) {
    this.scriptSource = source;
  }

  @Override
  public String toString() {
    return "[" + url + "," + scriptId + "]";
  }

  private void patchupScriptUrl() {
    // TODO(devoncarew): this method converts unqualified library urls to slightly better formed urls.
    if (url != null) {
      // bootstrap_impl ==> dart:bootstrap_impl
      if (url.equals("bootstrap_impl")) {
        url = "dart:" + url;
      }
    }
  }

}
