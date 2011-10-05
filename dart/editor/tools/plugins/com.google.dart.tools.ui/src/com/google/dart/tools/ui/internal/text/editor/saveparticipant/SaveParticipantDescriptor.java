/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui.internal.text.editor.saveparticipant;

import org.eclipse.core.runtime.Assert;

/**
 * Describes a save participant contribution.
 */
public class SaveParticipantDescriptor {

  /** The listener */
  private final IPostSaveListener fPostSaveListener;
  /** The preference configuration block, if any */
  private ISaveParticipantPreferenceConfiguration fPreferenceConfiguration;

  /**
   * Creates a new descriptor which connects a {@link IPostSaveListener} with an
   * {@link ISaveParticipantPreferenceConfiguration}.
   * 
   * @param listener the listener
   */
  SaveParticipantDescriptor(IPostSaveListener listener) {
    Assert.isNotNull(listener);

    fPostSaveListener = listener;
  }

  /**
   * Creates a new preference configuration of the described save participant.
   * 
   * @return the preference configuration
   */
  public ISaveParticipantPreferenceConfiguration createPreferenceConfiguration() {
    return new AbstractSaveParticipantPreferenceConfiguration() {

      @Override
      protected String getPostSaveListenerId() {
        return fPostSaveListener.getId();
      }

      @Override
      protected String getPostSaveListenerName() {
        return fPostSaveListener.getName();
      }
    };
  }

  /**
   * Returns the identifier of the described save participant.
   * 
   * @return the non-empty id of this descriptor
   */
  public String getId() {
    return fPostSaveListener.getId();
  }

  /**
   * Returns the post save listener of the described save participant
   * 
   * @return the listener
   */
  public IPostSaveListener getPostSaveListener() {
    return fPostSaveListener;
  }

  /**
   * Returns the preference configuration of the described save participant.
   * 
   * @return the preference configuration
   */
  public ISaveParticipantPreferenceConfiguration getPreferenceConfiguration() {
    if (fPreferenceConfiguration == null) {
      fPreferenceConfiguration = createPreferenceConfiguration();
    }

    return fPreferenceConfiguration;
  }

}
