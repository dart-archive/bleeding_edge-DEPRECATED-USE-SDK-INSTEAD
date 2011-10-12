/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.google.dart.tools.core.mock;

import org.eclipse.core.resources.ICommand;

import java.util.Map;

@SuppressWarnings("rawtypes")
public class MockCommand implements ICommand {
  private Map args;
  private String builderName;
  private boolean[] building = new boolean[16];

  @SuppressWarnings("unchecked")
  @Override
  public Map getArguments() {
    return args;
  }

  @Override
  public String getBuilderName() {
    return builderName;
  }

  @Override
  public boolean isBuilding(int kind) {
    return building[kind];
  }

  @Override
  public boolean isConfigurable() {
    return false;
  }

  @Override
  public void setArguments(Map args) {
    this.args = args;
  }

  @Override
  public void setBuilderName(String builderName) {
    this.builderName = builderName;
  }

  @Override
  public void setBuilding(int kind, boolean value) {
    building[kind] = value;
  }
}
