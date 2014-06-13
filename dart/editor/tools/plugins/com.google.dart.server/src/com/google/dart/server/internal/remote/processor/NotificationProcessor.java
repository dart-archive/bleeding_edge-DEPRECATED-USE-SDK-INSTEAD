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

import com.google.common.annotations.VisibleForTesting;
import com.google.dart.server.AnalysisServerListener;
import com.google.dart.server.Element;
import com.google.dart.server.ElementKind;
import com.google.dart.server.internal.local.computer.ElementImpl;
import com.google.gson.JsonObject;

/**
 * Abstract processor class which holds the {@link AnalysisServerListener} for all processors.
 * 
 * @coverage dart.server.remote
 */
public abstract class NotificationProcessor {
  /**
   * Return the {@link ElementKind} code for the given name. If the passed name cannot be found, an
   * {@link IllegalArgumentException} is thrown.
   */
  @VisibleForTesting
  public static ElementKind getElementKind(String kindName) {
    return ElementKind.valueOf(kindName);
  }

  private final AnalysisServerListener listener;

  public NotificationProcessor(AnalysisServerListener listener) {
    this.listener = listener;
  }

  /**
   * Process the given {@link JsonObject} notification and notify {@link #listener}.
   */
  public abstract void process(JsonObject response) throws Exception;

  protected Element computeElement(JsonObject elementObject) {
    ElementKind kind = getElementKind(elementObject.get("kind").getAsString());
    String name = elementObject.get("name").getAsString();
    int offset = elementObject.get("offset").getAsInt();
    int length = elementObject.get("length").getAsInt();
    int flags = elementObject.get("flags").getAsInt();
    // prepare parameters
    String parameters = null;
    if (elementObject.has("parameters")) {
      parameters = elementObject.get("parameters").getAsString();
    }
    // prepare return type
    String returnType = null;
    if (elementObject.has("returnType")) {
      returnType = elementObject.get("returnType").getAsString();
    }
    return new ElementImpl(kind, name, offset, length, flags, parameters, returnType);
  }

  protected AnalysisServerListener getListener() {
    return listener;
  }
}
