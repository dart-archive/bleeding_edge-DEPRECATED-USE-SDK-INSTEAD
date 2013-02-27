#!/usr/bin/python
#
# Copyright (C) 2011 Google Inc. All rights reserved.
#
# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions are
# met:
#
#     * Redistributions of source code must retain the above copyright
# notice, this list of conditions and the following disclaimer.
#     * Redistributions in binary form must reproduce the above
# copyright notice, this list of conditions and the following disclaimer
# in the documentation and/or other materials provided with the
# distribution.
#     * Neither the name of Google Inc. nor the names of its
# contributors may be used to endorse or promote products derived from
# this software without specific prior written permission.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
# "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
# LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
# A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
# OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
# SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
# LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
# DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
# THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
# (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
# OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
#
# Copyright (c) 2011 The Chromium Authors. All rights reserved.
# Use of this source code is governed by a BSD-style license that can be
# found in the LICENSE file.

# build_dart_snapshot.py generates two C++ files: DartSnapshot.cpp
# with a constant which is a snapshot of major DOM libs an
# DartResolver.cpp which is a resolver for dart:html library.

import re
import os.path
import sys


def main(args):
    _, inputDir, outputDir, tag = args

    def massage(ext, repls):
        input = open(os.path.join(inputDir, 'V8%sElementWrapperFactory.%s' % (tag, ext)), 'r')
        output = open(os.path.join(outputDir, 'Dart%sElementWrapperFactory.%s' % (tag, ext)), 'w')
        for line in input:
            for regexp, repl in repls:
                line = regexp.sub(repl, line)
            output.write(line)

        input.close()
        output.close()

    massage('h', [
        (re.compile(r'v8\.h'), 'dart_api.h'),
        (re.compile(r', bool'), ''),
        (re.compile(r', v8::Isolate\*( isolate)?'), ''),
        (re.compile(r', isolate'), ''),
        (re.compile(r', v8::Handle<v8::Object> creationContext'), ''),
        (re.compile(r', creationContext'), ''),
        (re.compile(r'createWrapper'), 'toDart'),
        (re.compile(r'v8::Handle<v8::Object>'), 'Dart_Handle'),
        (re.compile(r'V8'), 'Dart'),
    ])
    massage('cpp', [
        (re.compile(r'^#include "V8'), '#include "Dart'),
        (re.compile(r'v8\.h'), 'dart_api.h'),
        (re.compile(
            r'return wrap\(static_cast<(\w+)\*>\(element\), creationContext, isolate\);'),
            r'return Dart\1::toDart(static_cast<\1*>(element));'),
        (re.compile(
            r'return createWrapperFunction\(element, creationContext, isolate\)'),
            r'return createWrapperFunction(element)'),
        (re.compile(
            r'return V8%sElement::createWrapper\(element, creationContext, isolate\);' % tag),
            r'return DartDOMWrapper::toDart<Dart%sElement>(element);' % tag),
        (re.compile(
            r'return V8HTMLCustomElement::wrap\(element, creationContext, isolate\);'),
            r'return DartHTMLCustomElement::toDart(element);'),
        (re.compile(
            r'return createV8HTMLFallbackWrapper\(toHTMLUnknownElement\(element\), creationContext, isolate\);'),
            r'return DartHTMLUnknownElement::toDart(toHTMLUnknownElement(element));'),
        (re.compile(
            r'return createV8HTMLDirectWrapper\(element, creationContext, isolate\);'),
            r'return DartDOMWrapper::toDart<DartHTMLElement>(element);'),
        (re.compile(r'createV8%sWrapper' % tag), r'createDart%sWrapper' % tag),
        (re.compile(r', v8::Isolate\*( isolate)?'), ''),
        (re.compile(r', v8::Handle<v8::Object> creationContext'), ''),
        (re.compile(r'v8::Handle<v8::Object>'), 'Dart_Handle'),
    ])

    return 0

if __name__ == '__main__':
    sys.exit(main(sys.argv))
