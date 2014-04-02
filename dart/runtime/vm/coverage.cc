// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#include "vm/coverage.h"

#include "include/dart_api.h"

#include "vm/compiler.h"
#include "vm/isolate.h"
#include "vm/json_stream.h"
#include "vm/object.h"
#include "vm/object_store.h"

namespace dart {

DEFINE_FLAG(charp, coverage_dir, NULL,
            "Enable writing coverage data into specified directory.");


void CodeCoverage::CompileAndAdd(const Function& function,
                                 const JSONArray& hits_arr) {
  if (!function.HasCode()) {
    // If the function should not be compiled or if the compilation failed,
    // then just skip this method.
    // TODO(iposva): Maybe we should skip synthesized methods in general too.
    if (function.is_abstract() || function.IsRedirectingFactory()) {
      return;
    }
    if (function.IsNonImplicitClosureFunction() &&
        (function.context_scope() == ContextScope::null())) {
      // TODO(iposva): This can arise if we attempt to compile an inner function
      // before we have compiled its enclosing function or if the enclosing
      // function failed to compile.
      OS::Print("### Coverage skipped compiling: %s\n", function.ToCString());
      return;
    }
    const Error& err = Error::Handle(Compiler::CompileFunction(function));
    if (!err.IsNull()) {
      OS::Print("### Coverage failed compiling:\n%s\n", err.ToErrorCString());
      return;
    }
  }
  ASSERT(function.HasCode());

  Isolate* isolate = Isolate::Current();
  // Print the hit counts for all IC datas.
  const Script& script = Script::Handle(function.script());
  const Code& code = Code::Handle(function.unoptimized_code());
  const Array& ic_array = Array::Handle(code.ExtractTypeFeedbackArray());
  const PcDescriptors& descriptors = PcDescriptors::Handle(
      code.pc_descriptors());
  ICData& ic_data = ICData::Handle();

  for (int j = 0; j < descriptors.Length(); j++) {
    HANDLESCOPE(isolate);
    PcDescriptors::Kind kind = descriptors.DescriptorKind(j);
    // Only IC based calls have counting.
    if ((kind == PcDescriptors::kIcCall) ||
        (kind == PcDescriptors::kUnoptStaticCall)) {
      intptr_t deopt_id = descriptors.DeoptId(j);
      ic_data ^= ic_array.At(deopt_id);
      if (!ic_data.IsNull()) {
        intptr_t token_pos = descriptors.TokenPos(j);
        intptr_t line = -1;
        script.GetTokenLocation(token_pos, &line, NULL);
        hits_arr.AddValue(line);
        hits_arr.AddValue(ic_data.AggregateCount());
      }
    }
  }
}


void CodeCoverage::PrintClass(const Class& cls, const JSONArray& jsarr) {
  Isolate* isolate = Isolate::Current();
  Array& functions = Array::Handle(cls.functions());
  ASSERT(!functions.IsNull());
  Function& function = Function::Handle();
  Script& script = Script::Handle();
  String& saved_url = String::Handle();
  String& url = String::Handle();

  int i = 0;
  while (i < functions.Length()) {
    HANDLESCOPE(isolate);
    function ^= functions.At(i);
    JSONObject jsobj(&jsarr);
    script = function.script();
    saved_url = script.url();
    jsobj.AddProperty("source", saved_url.ToCString());
    jsobj.AddProperty("script", script);
    JSONArray hits_arr(&jsobj, "hits");

    // We stay within this loop while we are seeing functions from the same
    // source URI.
    while (i < functions.Length()) {
      function ^= functions.At(i);
      script = function.script();
      url = script.url();
      if (!url.Equals(saved_url)) {
        break;
      }
      CompileAndAdd(function, hits_arr);
      if (function.HasImplicitClosureFunction()) {
        function = function.ImplicitClosureFunction();
        CompileAndAdd(function, hits_arr);
      }
      i++;
    }
  }

  GrowableObjectArray& closures =
      GrowableObjectArray::Handle(cls.closures());
  if (!closures.IsNull()) {
    i = 0;
    // We need to keep rechecking the length of the closures array, as handling
    // a closure potentially adds new entries to the end.
    while (i < closures.Length()) {
      HANDLESCOPE(isolate);
      function ^= closures.At(i);
      JSONObject jsobj(&jsarr);
      script = function.script();
      saved_url = script.url();
      jsobj.AddProperty("source", saved_url.ToCString());
      jsobj.AddProperty("script", script);
      JSONArray hits_arr(&jsobj, "hits");

      // We stay within this loop while we are seeing functions from the same
      // source URI.
      while (i < closures.Length()) {
        function ^= closures.At(i);
        script = function.script();
        url = script.url();
        if (!url.Equals(saved_url)) {
          break;
        }
        CompileAndAdd(function, hits_arr);
        i++;
      }
    }
  }
}


void CodeCoverage::Write(Isolate* isolate) {
  if (FLAG_coverage_dir == NULL) {
    return;
  }

  Dart_FileOpenCallback file_open = Isolate::file_open_callback();
  Dart_FileWriteCallback file_write = Isolate::file_write_callback();
  Dart_FileCloseCallback file_close = Isolate::file_close_callback();
  if ((file_open == NULL) || (file_write == NULL) || (file_close == NULL)) {
    return;
  }

  JSONStream stream;
  PrintToJSONStream(isolate, &stream);

  const char* format = "%s/dart-cov-%" Pd "-%" Pd ".json";
  intptr_t pid = OS::ProcessId();
  intptr_t len = OS::SNPrint(NULL, 0, format,
                             FLAG_coverage_dir, pid, isolate->main_port());
  char* filename = Isolate::Current()->current_zone()->Alloc<char>(len + 1);
  OS::SNPrint(filename, len + 1, format,
              FLAG_coverage_dir, pid, isolate->main_port());
  void* file = (*file_open)(filename, true);
  if (file == NULL) {
    OS::Print("Failed to write coverage file: %s\n", filename);
    return;
  }
  (*file_write)(stream.buffer()->buf(), stream.buffer()->length(), file);
  (*file_close)(file);
}


void CodeCoverage::PrintToJSONStream(Isolate* isolate, JSONStream* stream) {
  const GrowableObjectArray& libs = GrowableObjectArray::Handle(
      isolate, isolate->object_store()->libraries());
  Library& lib = Library::Handle();
  Class& cls = Class::Handle();
  JSONObject coverage(stream);
  coverage.AddProperty("type", "CodeCoverage");
  coverage.AddProperty("id", "coverage");
  {
    JSONArray jsarr(&coverage, "coverage");
    for (int i = 0; i < libs.Length(); i++) {
      lib ^= libs.At(i);
      ClassDictionaryIterator it(lib, ClassDictionaryIterator::kIteratePrivate);
      while (it.HasNext()) {
        cls = it.GetNextClass();
        if (cls.EnsureIsFinalized(isolate) == Error::null()) {
          // Only classes that have been finalized do have a meaningful list of
          // functions.
          PrintClass(cls, jsarr);
        }
      }
    }
  }
}


}  // namespace dart
