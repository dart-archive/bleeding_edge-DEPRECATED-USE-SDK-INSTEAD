package com.google.dart.engine.services.completion;

import com.google.dart.engine.element.ElementKind;

public interface CompletionProposal {

  ElementKind getKind();

  int getLocation();

  String getName();

}
