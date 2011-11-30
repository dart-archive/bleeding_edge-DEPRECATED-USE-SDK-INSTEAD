// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

class ClassElementParser extends PartialParser {
  ClassElementParser(Listener listener) : super(listener);

  Token parseClassBody(Token token) => fullParseClassBody(token);
}

class PartialClassElement extends ClassElement {
  final Token beginToken;
  final Token endToken;
  ClassNode node;
  Link<Element> members;

  PartialClassElement(SourceString name,
                      Token this.beginToken, Token this.endToken)
    : super(name);

  ClassNode parseNode(Canceler canceler, Logger logger) {
    NodeListener listener = new NodeListener(canceler, logger);
    Parser parser = new ClassElementParser(listener);
    Token token = parser.parseClass(beginToken);
    assert(token === endToken.next);
    ClassNode node = listener.popNode();
    assert(listener.nodes.isEmpty());
    assert(listener.topLevelElements.isEmpty());
    return node;
  }
}
