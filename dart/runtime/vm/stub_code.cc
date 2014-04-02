// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#include "vm/stub_code.h"

#include "platform/assert.h"
#include "platform/globals.h"
#include "vm/assembler.h"
#include "vm/disassembler.h"
#include "vm/flags.h"
#include "vm/virtual_memory.h"
#include "vm/visitor.h"

namespace dart {

DEFINE_FLAG(bool, disassemble_stubs, false, "Disassemble generated stubs.");

#define STUB_CODE_DECLARE(name)                                                \
  StubEntry* StubCode::name##_entry_ = NULL;
VM_STUB_CODE_LIST(STUB_CODE_DECLARE);
#undef STUB_CODE_DECLARE


StubEntry::StubEntry(const char* name, const Code& code)
    : code_(code.raw()),
      entry_point_(code.EntryPoint()),
      size_(code.Size()),
      label_(name, code.EntryPoint()) {
}


// Visit all object pointers.
void StubEntry::VisitObjectPointers(ObjectPointerVisitor* visitor) {
  ASSERT(visitor != NULL);
  visitor->VisitPointer(reinterpret_cast<RawObject**>(&code_));
}


StubCode::~StubCode() {
#define STUB_CODE_DELETER(name)                                                \
  delete name##_entry_;
  STUB_CODE_LIST(STUB_CODE_DELETER);
#undef STUB_CODE_DELETER
}


#define STUB_CODE_GENERATE(name)                                               \
  code ^= Generate("_stub_"#name, StubCode::Generate##name##Stub);             \
  name##_entry_ = new StubEntry("_stub_"#name, code);


void StubCode::InitOnce() {
  // TODO(zra): ifndef to be removed when ARM64 port is ready.
#if !defined(TARGET_ARCH_ARM64)
  // Generate all the stubs.
  Code& code = Code::Handle();
  VM_STUB_CODE_LIST(STUB_CODE_GENERATE);
#endif
}


void StubCode::GenerateFor(Isolate* init) {
  // Generate all the stubs.
  Code& code = Code::Handle();
  STUB_CODE_LIST(STUB_CODE_GENERATE);
}

#undef STUB_CODE_GENERATE


void StubCode::Init(Isolate* isolate) {
  // TODO(zra): ifndef to be removed when ARM64 port is ready.
#if !defined(TARGET_ARCH_ARM64)
  StubCode* stubs = new StubCode();
  isolate->set_stub_code(stubs);
  stubs->GenerateFor(isolate);
#endif
}


void StubCode::VisitObjectPointers(ObjectPointerVisitor* visitor) {
  // The current isolate is needed as part of the macro.
  Isolate* isolate = Isolate::Current();
  StubCode* stubs = isolate->stub_code();
  if (stubs == NULL) return;
  StubEntry* entry;
#define STUB_CODE_VISIT_OBJECT_POINTER(name)                                   \
  entry = stubs->name##_entry();                                               \
  if (entry != NULL) {                                                         \
    entry->VisitObjectPointers(visitor);                                       \
  }

  STUB_CODE_LIST(STUB_CODE_VISIT_OBJECT_POINTER);
#undef STUB_CODE_VISIT_OBJECT_POINTER
}


bool StubCode::InInvocationStub(uword pc) {
  return ((pc >= InvokeDartCodeEntryPoint()) &&
          (pc < (InvokeDartCodeEntryPoint() + InvokeDartCodeSize())));
}


RawCode* StubCode::GetAllocationStubForClass(const Class& cls) {
  Isolate* isolate = Isolate::Current();
  const Error& error = Error::Handle(isolate, cls.EnsureIsFinalized(isolate));
  ASSERT(error.IsNull());
  Code& stub = Code::Handle(isolate, cls.allocation_stub());
  if (stub.IsNull()) {
    Assembler assembler;
    const char* name = cls.ToCString();
    StubCode::GenerateAllocationStubForClass(&assembler, cls);
    stub ^= Code::FinalizeCode(name, &assembler);
    stub.set_owner(cls);
    cls.set_allocation_stub(stub);
    if (FLAG_disassemble_stubs) {
      OS::Print("Code for allocation stub '%s': {\n", name);
      Disassembler::Disassemble(stub.EntryPoint(),
                                stub.EntryPoint() + assembler.CodeSize());
      OS::Print("}\n");
    }
  }
  return stub.raw();
}


RawCode* StubCode::Generate(const char* name,
                            void (*GenerateStub)(Assembler* assembler)) {
  Assembler assembler;
  GenerateStub(&assembler);
  const Code& code = Code::Handle(Code::FinalizeCode(name, &assembler));
  if (FLAG_disassemble_stubs) {
    OS::Print("Code for stub '%s': {\n", name);
    Disassembler::Disassemble(code.EntryPoint(),
                              code.EntryPoint() + assembler.CodeSize());
    OS::Print("}\n");
  }
  return code.raw();
}


const char* StubCode::NameOfStub(uword entry_point) {
#define STUB_CODE_TESTER(name) \
  if ((name##_entry() != NULL) && (entry_point == name##EntryPoint())) {       \
    return ""#name;                                                            \
  }

  VM_STUB_CODE_LIST(STUB_CODE_TESTER);
  Isolate* isolate = Isolate::Current();
  if ((isolate != NULL) && (isolate->stub_code() != NULL)) {
    STUB_CODE_LIST(STUB_CODE_TESTER);
  }
#undef STUB_CODE_TESTER
  return NULL;
}

}  // namespace dart
