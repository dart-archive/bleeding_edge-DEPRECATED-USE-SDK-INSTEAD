// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#include "vm/handles.h"

#include "platform/assert.h"
#include "platform/utils.h"
#include "vm/flags.h"
#include "vm/isolate.h"
#include "vm/os.h"
#include "vm/raw_object.h"
#include "vm/visitor.h"
#include "vm/zone.h"

#include "vm/handles_impl.h"

namespace dart {

DEFINE_FLAG(bool, verify_handles, false, "Verify handles.");
DEFINE_DEBUG_FLAG(bool, trace_handles,
                  false, "Traces allocation of handles.");


VMHandles::~VMHandles() {
#ifdef DEBUG
    if (FLAG_trace_handles) {
      OS::PrintErr("***   Handle Counts for 0x(%" Px
                   "):Zone = %d,Scoped = %d\n",
                   reinterpret_cast<intptr_t>(this),
                   CountZoneHandles(), CountScopedHandles());
      OS::PrintErr("*** Deleting VM handle block 0x%" Px "\n",
                   reinterpret_cast<intptr_t>(this));
    }
#endif
}


void VMHandles::VisitObjectPointers(ObjectPointerVisitor* visitor) {
  return Handles<kVMHandleSizeInWords,
                 kVMHandlesPerChunk,
                 kOffsetOfRawPtr>::VisitObjectPointers(visitor);
}


uword VMHandles::AllocateHandle(Zone* zone) {
  return Handles<kVMHandleSizeInWords,
                 kVMHandlesPerChunk,
                 kOffsetOfRawPtr>::AllocateHandle(zone);
}


uword VMHandles::AllocateZoneHandle(Zone* zone) {
  return Handles<kVMHandleSizeInWords,
                 kVMHandlesPerChunk,
                 kOffsetOfRawPtr>::AllocateZoneHandle(zone);
}


bool VMHandles::IsZoneHandle(uword handle) {
  return Handles<kVMHandleSizeInWords,
                 kVMHandlesPerChunk,
                 kOffsetOfRawPtr >::IsZoneHandle(handle);
}


int VMHandles::ScopedHandleCount() {
  Isolate* isolate = Isolate::Current();
  ASSERT(isolate->current_zone() != NULL);
  VMHandles* handles = isolate->current_zone()->handles();
  return handles->CountScopedHandles();
}


int VMHandles::ZoneHandleCount() {
  Isolate* isolate = Isolate::Current();
  ASSERT(isolate->current_zone() != NULL);
  VMHandles* handles = isolate->current_zone()->handles();
  return handles->CountZoneHandles();
}


HandleScope::HandleScope(Isolate* isolate) : StackResource(isolate) {
  ASSERT(isolate->no_handle_scope_depth() == 0);
  VMHandles* handles = isolate->current_zone()->handles();
  ASSERT(handles != NULL);
  saved_handle_block_ = handles->scoped_blocks_;
  saved_handle_slot_ = handles->scoped_blocks_->next_handle_slot();
#if defined(DEBUG)
  link_ = isolate->top_handle_scope();
  isolate->set_top_handle_scope(this);
#endif
}


HandleScope::~HandleScope() {
  ASSERT(isolate()->current_zone() != NULL);
  VMHandles* handles = isolate()->current_zone()->handles();
  ASSERT(handles != NULL);
  handles->scoped_blocks_ = saved_handle_block_;
  handles->scoped_blocks_->set_next_handle_slot(saved_handle_slot_);
#if defined(DEBUG)
  handles->VerifyScopedHandleState();
  handles->ZapFreeScopedHandles();
  ASSERT(isolate()->top_handle_scope() == this);
  isolate()->set_top_handle_scope(link_);
#endif
}


#if defined(DEBUG)
NoHandleScope::NoHandleScope(Isolate* isolate) : StackResource(isolate) {
  isolate->IncrementNoHandleScopeDepth();
}


NoHandleScope::NoHandleScope() : StackResource(Isolate::Current()) {
  isolate()->IncrementNoHandleScopeDepth();
}


NoHandleScope::~NoHandleScope() {
  isolate()->DecrementNoHandleScopeDepth();
}
#endif  // defined(DEBUG)

}  // namespace dart
