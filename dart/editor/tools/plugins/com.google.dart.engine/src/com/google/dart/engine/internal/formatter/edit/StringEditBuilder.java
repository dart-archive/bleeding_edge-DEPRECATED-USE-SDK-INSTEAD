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
package com.google.dart.engine.internal.formatter.edit;

import com.google.dart.engine.formatter.edit.Edit;
import com.google.dart.engine.formatter.edit.EditBuilder;
import com.google.dart.engine.formatter.edit.EditOperation;

import java.util.List;

/**
 * An edit builder that produces simple string edit operations.
 */
public class StringEditBuilder implements EditBuilder<String, String> {

  @Override
  public EditOperation<String, String> buildEdit(List<Edit> edits) {
    return new StringEditOperation(edits);
  }

}
