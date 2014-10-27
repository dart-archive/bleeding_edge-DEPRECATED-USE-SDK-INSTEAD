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

package com.google.dart.tools.ui.internal.projects;

import com.google.dart.tools.core.generator.AbstractSample;

import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.graphics.Image;

/**
 * A styled label provider for samples.
 * 
 * @see AbstractSample
 */
public class SamplesLabelProvider extends DelegatingStyledCellLabelProvider {

  private static class SamplesStyledLabelProvider implements IStyledLabelProvider {
    @Override
    public void addListener(ILabelProviderListener listener) {

    }

    @Override
    public void dispose() {

    }

    @Override
    public Image getImage(Object element) {
      return null;
    }

    @Override
    public StyledString getStyledText(Object element) {
      AbstractSample sample = (AbstractSample) element;

      StyledString str = new StyledString(sample.getTitle());

      if (sample.getDescription() != null) {
        str.append(" " + sample.getDescription(), StyledString.QUALIFIER_STYLER);
      }

      return str;
    }

    @Override
    public boolean isLabelProperty(Object element, String property) {
      return false;
    }

    @Override
    public void removeListener(ILabelProviderListener listener) {

    }
  }

  public SamplesLabelProvider() {
    super(new SamplesStyledLabelProvider());
  }
}
