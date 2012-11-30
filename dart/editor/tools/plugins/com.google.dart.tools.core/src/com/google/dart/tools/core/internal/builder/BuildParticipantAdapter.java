/*
 * Copyright 2012 Dart project authors.
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
package com.google.dart.tools.core.internal.builder;

import com.google.dart.tools.core.builder.BuildParticipant;
import com.google.dart.tools.core.builder.DartBuildParticipant;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import java.util.Map;

/**
 * Used to adapt {@link BuildParticipant} to {@link DartBuildParticipant}. This will be removed once
 * {@link DartBuildParticipant} is removed.
 */
public class BuildParticipantAdapter implements DartBuildParticipant {

  private final BuildParticipant participant;

  public BuildParticipantAdapter(BuildParticipant participant) {
    this.participant = participant;
  }

  @Override
  public void build(int kind, Map<String, String> args, IResourceDelta delta,
      IProgressMonitor monitor) throws CoreException {
    throw new RuntimeException("call getParticipant() and access directly");
  }

  @Override
  public void clean(IProject project, IProgressMonitor monitor) throws CoreException {
    throw new RuntimeException("call getParticipant() and access directly");
  }

  public BuildParticipant getParticipant() {
    return participant;
  }
}
