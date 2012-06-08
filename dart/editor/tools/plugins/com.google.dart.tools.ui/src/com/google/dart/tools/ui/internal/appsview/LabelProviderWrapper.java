package com.google.dart.tools.ui.internal.appsview;

import org.eclipse.jface.viewers.DecoratingStyledCellLabelProvider;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.IDecorationContext;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.StyledString;

public class LabelProviderWrapper extends DecoratingStyledCellLabelProvider implements
    IStyledLabelProvider, ILabelProvider {
  private AppLabelProvider labelProvider;

  public LabelProviderWrapper(AppLabelProvider labelProvider, ILabelDecorator decorator,
      IDecorationContext decorationContext) {
    super(labelProvider, decorator, decorationContext);
    this.labelProvider = labelProvider;
  }

  @Override
  public StyledString getStyledText(Object element) {
    return super.getStyledText(element);
  }

  @Override
  public String getText(Object element) {
    return labelProvider.getText(element);
  }
}
