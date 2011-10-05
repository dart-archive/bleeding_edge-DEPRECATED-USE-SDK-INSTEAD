# Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
# for details. All rights reserved. Use of this source code is governed by a
# BSD-style license that can be found in the LICENSE file.

OUTPUT := out/dart.jar

all: $(OUTPUT)

out/dart.jar: Dart.g TestGrammar.java dart.mf Makefile
	@rm -rf out
	@mkdir -p out/classes
	@cp -p antlrworks.jar out
	java -cp antlrworks.jar org.antlr.Tool -o out/gensrc Dart.g
	javac -cp antlrworks.jar out/gensrc/*.java TestGrammar.java -d out/classes
	jar cfm $@ dart.mf -C out/classes com
