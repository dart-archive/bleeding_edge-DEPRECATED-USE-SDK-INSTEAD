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
package com.google.dart.server.internal.remote.processor;

import com.google.dart.server.AnalysisServerListener;
import com.google.dart.server.generated.types.AnalysisStatus;
import com.google.gson.JsonObject;

/**
 * Processor for "server.status" notification.
 * 
 * @coverage dart.server.remote
 */
public class NotificationServerStatusProcessor extends NotificationProcessor {

  public NotificationServerStatusProcessor(AnalysisServerListener listener) {
    super(listener);
  }

  @Override
  public void process(JsonObject response) throws Exception {
    JsonObject paramsObject = response.get("params").getAsJsonObject();
    JsonObject analysisObject = paramsObject.get("analysis").getAsJsonObject();
    boolean isAnalyzing = analysisObject.get("isAnalyzing").getAsBoolean();
    String analysisTarget = safelyGetAsString(analysisObject, "analysisTarget");
    // notify listener
    getListener().serverStatus(new AnalysisStatus(isAnalyzing, analysisTarget));
  }
}
