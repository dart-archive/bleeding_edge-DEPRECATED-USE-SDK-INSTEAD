// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// Separate entrypoint for the frog compiler with experimental support for the
// 'await' keyword.

#import('../lang.dart');
#import('../minfrog.dart', prefix:'minfrog');
#source('nodeset.dart');
#source('checker.dart');
#source('normalizer.dart');
#source('transformation.dart');

/**
 * The main entry point method - needed to ensure we handle correctly await
 * expressions within main.
 */
Member _mainMethod;

/**
 * A phase in the compilation process that processes the entire AST and
 * desugars await expressions.
 */
awaitTransformation() {
  _mainMethod =
      world.findMainMethod(world.getOrAddLibrary(options.dartScript));
  for (var lib in world.libraries.getValues()) {
    for (var type in lib.types.getValues()) {
      for (var member in type.members.getValues()) {
        _process(member);
      }
      for (var member in type.constructors.getValues()) {
        _process(member);
      }
    }
  }
}

/** Analyze and transform a single member (method or property). */
_process(Member member) {
  if (member.isConstructor || member.isMethod) {
    _processFunction(member.definition);
  } else if (member.isProperty) {
    PropertyMember p = member;
    if (p.getter != null) _process(p.getter);
    if (p.setter != null) _process(p.setter);
  }
}

/** Analyze and transform a function definition and nested definitions. */
_processFunction(FunctionDefinition func) {
  AwaitChecker checker = new AwaitChecker();

  // Run checker that collects nested functions and which nodes may contain
  // await expressions.
  func.visit(checker);

  // Rewrite nested asynchronous functions first.
  for (FunctionDefinition f in checker.nestedFunctions) {
    _processFunction(f);
  }

  if (checker.haveAwait.contains(func)) {
    // Normalize
    func.visit(new AwaitNormalizer(checker.haveAwait));
    // Re-run the checker to derive appropriate node information (this
    // makes the normalizer simpler as we don't have to keep track of this
    // information).
    checker = new AwaitChecker();
    func.visit(checker);

    // Transform the code.
    func.visit(new AwaitProcessor(checker.haveAwait));
  }
}

/** Run frog, setting the await transformation correctly. */
void main() {
  experimentalAwaitPhase = () {
    world.withTiming('remove await expressions', awaitTransformation);
  };
  minfrog.main();
}
