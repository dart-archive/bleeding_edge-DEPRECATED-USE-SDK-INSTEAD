// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#include "vm/stub_code.h"

#include "platform/assert.h"
#include "platform/globals.h"
#include "vm/assembler.h"
#include "vm/disassembler.h"
#include "vm/flags.h"
#include "vm/object_store.h"
#include "vm/virtual_memory.h"
#include "vm/visitor.h"

namespace dart {

DEFINE_FLAG(bool, disassemble_stubs, false, "Disassemble generated stubs.");

#define STUB_CODE_DECLARE(name)                                                \
  StubEntry* StubCode::name##_entry_ = NULL;
VM_STUB_CODE_LIST(STUB_CODE_DECLARE);
#undef STUB_CODE_DECLARE


StubEntry::StubEntry(const Code& code)
    : code_(code.raw()),
      entry_point_(code.EntryPoint()),
      size_(code.Size()),
      label_(code.EntryPoint()) {
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
  name##_entry_ = new StubEntry(code);


void StubCode::InitOnce() {
  // Generate all the stubs.
  Code& code = Code::Handle();
  VM_STUB_CODE_LIST(STUB_CODE_GENERATE);
}


void StubCode::GenerateBootstrapStubsFor(Isolate* init) {
  // Generate initial stubs.
  Code& code = Code::Handle();
  BOOTSTRAP_STUB_CODE_LIST(STUB_CODE_GENERATE);
}


void StubCode::GenerateStubsFor(Isolate* init) {
  // Generate all the other stubs.
  Code& code = Code::Handle();
  REST_STUB_CODE_LIST(STUB_CODE_GENERATE);
}

#undef STUB_CODE_GENERATE


void StubCode::InitBootstrapStubs(Isolate* isolate) {
  StubCode* stubs = new StubCode(isolate);
  isolate->set_stub_code(stubs);
  stubs->GenerateBootstrapStubsFor(isolate);
}


void StubCode::Init(Isolate* isolate) {
  isolate->stub_code()->GenerateStubsFor(isolate);
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
  return InInvocationStubForIsolate(Isolate::Current(), pc);
}


bool StubCode::InInvocationStubForIsolate(Isolate* isolate, uword pc) {
  StubCode* stub_code = isolate->stub_code();
  uword entry = stub_code->InvokeDartCodeEntryPoint();
  uword size = stub_code->InvokeDartCodeSize();
  return (pc >= entry) && (pc < (entry + size));
}


bool StubCode::InJumpToExceptionHandlerStub(uword pc) {
  uword entry = StubCode::JumpToExceptionHandlerEntryPoint();
  uword size = StubCode::JumpToExceptionHandlerSize();
  return (pc >= entry) && (pc < (entry + size));
}


RawCode* StubCode::GetAllocateArrayStub() {
  const Class& array_cls = Class::Handle(isolate_,
      isolate_->object_store()->array_class());
  return GetAllocationStubForClass(array_cls);
}


RawCode* StubCode::GetAllocationStubForClass(const Class& cls) {
  Isolate* isolate = Isolate::Current();
  const Error& error = Error::Handle(isolate, cls.EnsureIsFinalized(isolate));
  ASSERT(error.IsNull());
  Code& stub = Code::Handle(isolate, cls.allocation_stub());
  if (stub.IsNull()) {
    Assembler assembler;
    const char* name = cls.ToCString();
    uword patch_code_offset = 0;
    uword entry_patch_offset = 0;
    if (cls.id() == kArrayCid) {
      StubCode::GeneratePatchableAllocateArrayStub(
          &assembler, &entry_patch_offset, &patch_code_offset);
    } else {
      StubCode::GenerateAllocationStubForClass(
          &assembler, cls, &entry_patch_offset, &patch_code_offset);
    }
    stub ^= Code::FinalizeCode(name, &assembler);
    stub.set_owner(cls);
    cls.set_allocation_stub(stub);
    if (FLAG_disassemble_stubs) {
      LogBlock lb(Isolate::Current());
      ISL_Print("Code for allocation stub '%s': {\n", name);
      DisassembleToStdout formatter;
      stub.Disassemble(&formatter);
      ISL_Print("}\n");
    }
    stub.set_entry_patch_pc_offset(entry_patch_offset);
    stub.set_patch_code_pc_offset(patch_code_offset);
  }
  return stub.raw();
}


uword StubCode::UnoptimizedStaticCallEntryPoint(intptr_t num_args_tested) {
  switch (num_args_tested) {
    case 0:
      return ZeroArgsUnoptimizedStaticCallEntryPoint();
    case 1:
      return OneArgUnoptimizedStaticCallEntryPoint();
    case 2:
      return TwoArgsUnoptimizedStaticCallEntryPoint();
    default:
      UNIMPLEMENTED();
      return 0;
  }
}


RawCode* StubCode::Generate(const char* name,
                            void (*GenerateStub)(Assembler* assembler)) {
  Assembler assembler;
  GenerateStub(&assembler);
  const Code& code = Code::Handle(Code::FinalizeCode(name, &assembler));
  if (FLAG_disassemble_stubs) {
    LogBlock lb(Isolate::Current());
    ISL_Print("Code for stub '%s': {\n", name);
    DisassembleToStdout formatter;
    code.Disassemble(&formatter);
    ISL_Print("}\n");
  }
  return code.raw();
}


const char* StubCode::NameOfStub(uword entry_point) {
#define VM_STUB_CODE_TESTER(name)                                              \
  if ((name##_entry() != NULL) && (entry_point == name##EntryPoint())) {       \
    return ""#name;                                                            \
  }
  VM_STUB_CODE_LIST(VM_STUB_CODE_TESTER);

#define STUB_CODE_TESTER(name)                                                 \
  if ((isolate->stub_code()->name##_entry() != NULL) &&                        \
      (entry_point == isolate->stub_code()->name##EntryPoint())) {             \
    return ""#name;                                                            \
  }
  Isolate* isolate = Isolate::Current();
  if ((isolate != NULL) && (isolate->stub_code() != NULL)) {
    STUB_CODE_LIST(STUB_CODE_TESTER);
  }
#undef VM_STUB_CODE_TESTER
#undef STUB_CODE_TESTER
  return NULL;
}

}  // namespace dart
