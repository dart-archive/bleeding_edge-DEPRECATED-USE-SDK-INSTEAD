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

package com.google.dart.server.internal.local.operation;

/**
 * The enumeration {@code ServerOperationPriority} defines the priority levels used to organize
 * {@link ServerOperation}s in an optimal order. A smaller ordinal value equates to a higher
 * priority.
 * 
 * @coverage dart.server.local
 */
public enum ServerOperationPriority {
  SHUTDOWN,
  SERVER,
  CONTEXT_CHANGE,
  CONTEXT_NOTIFICATION,
  CONTEXT_ANALYSIS_PRIORITY_CONTINUE,
  CONTEXT_ANALYSIS_PRIORITY,
  CONTEXT_ANALYSIS_CONTINUE,
  CONTEXT_ANALYSIS,
  REFACTORING
}
