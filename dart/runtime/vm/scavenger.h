// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#ifndef VM_SCAVENGER_H_
#define VM_SCAVENGER_H_

#include "platform/assert.h"
#include "platform/utils.h"
#include "vm/flags.h"
#include "vm/globals.h"
#include "vm/raw_object.h"
#include "vm/virtual_memory.h"
#include "vm/visitor.h"

namespace dart {

// Forward declarations.
class Heap;
class Isolate;
class JSONObject;
class ScavengerVisitor;

DECLARE_FLAG(bool, gc_at_alloc);

class Scavenger {
 public:
  Scavenger(Heap* heap, intptr_t max_capacity_in_words, uword object_alignment);
  ~Scavenger();

  // Check whether this Scavenger contains this address.
  // During scavenging both the to and from spaces contain "legal" objects.
  // During a scavenge this function only returns true for addresses that will
  // be part of the surviving objects.
  bool Contains(uword addr) const {
    // No reasonable algorithm should be checking for objects in from space. At
    // least unless it is debugging code. This might need to be relaxed later,
    // but currently it helps prevent dumb bugs.
    ASSERT(!from_->Contains(addr));
    return to_->Contains(addr);
  }

  RawObject* FindObject(FindObjectVisitor* visitor) const;

  uword TryAllocate(intptr_t size) {
    ASSERT(Utils::IsAligned(size, kObjectAlignment));
#if defined(DEBUG)
    if (FLAG_gc_at_alloc && !scavenging_) {
      Scavenge();
    }
#endif
    uword result = top_;
    intptr_t remaining = end_ - top_;
    if (remaining < size) {
      return 0;
    }
    ASSERT(to_->Contains(result));
    ASSERT((result & kObjectAlignmentMask) == object_alignment_);

    top_ += size;
    ASSERT(to_->Contains(top_) || (top_ == to_->end()));
    return result;
  }

  // Collect the garbage in this scavenger.
  void Scavenge();
  void Scavenge(bool invoke_api_callbacks);

  // Accessors to generate code for inlined allocation.
  uword* TopAddress() { return &top_; }
  uword* EndAddress() { return &end_; }
  static intptr_t top_offset() { return OFFSET_OF(Scavenger, top_); }
  static intptr_t end_offset() { return OFFSET_OF(Scavenger, end_); }

  intptr_t UsedInWords() const {
    return (top_ - FirstObjectStart()) >> kWordSizeLog2;
  }
  intptr_t CapacityInWords() const {
    return (end_ - FirstObjectStart()) >> kWordSizeLog2;
  }
  intptr_t ExternalInWords() const {
    return external_size_ >> kWordSizeLog2;
  }

  void VisitObjects(ObjectVisitor* visitor) const;
  void VisitObjectPointers(ObjectPointerVisitor* visitor) const;

  void StartEndAddress(uword* start, uword* end) const {
    *start = to_->start();
    *end = to_->end();
  }

  // Returns true if the last scavenge had a promotion failure.
  bool HadPromotionFailure() {
    return had_promotion_failure_;
  }

  void WriteProtect(bool read_only);

  void AddGCTime(int64_t micros) {
    gc_time_micros_ += micros;
  }

  int64_t gc_time_micros() const {
    return gc_time_micros_;
  }

  void IncrementCollections() {
    collections_++;
  }

  intptr_t collections() const {
    return collections_;
  }

  void PrintToJSONObject(JSONObject* object);

  void AllocateExternal(intptr_t size);
  void FreeExternal(intptr_t size);

 private:
  // Ids for time and data records in Heap::GCStats.
  enum {
    // Time
    kVisitIsolateRoots = 0,
    kIterateStoreBuffers = 1,
    kProcessToSpace = 2,
    kIterateWeaks = 3,
    // Data
    kStoreBufferEntries = 0,
    kStoreBufferVisited = 1,
    kStoreBufferPointers = 2,
    kToKBAfterStoreBuffer = 3
  };

  uword FirstObjectStart() const { return to_->start() | object_alignment_; }
  void Prologue(Isolate* isolate, bool invoke_api_callbacks);
  void IterateStoreBuffers(Isolate* isolate, ScavengerVisitor* visitor);
  void IterateObjectIdTable(Isolate* isolate, ScavengerVisitor* visitor);
  void IterateRoots(Isolate* isolate,
                    ScavengerVisitor* visitor,
                    bool visit_prologue_weak_persistent_handles);
  void IterateWeakProperties(Isolate* isolate, ScavengerVisitor* visitor);
  void IterateWeakReferences(Isolate* isolate, ScavengerVisitor* visitor);
  void IterateWeakRoots(Isolate* isolate,
                        HandleVisitor* visitor,
                        bool visit_prologue_weak_persistent_handles);
  void ProcessToSpace(ScavengerVisitor* visitor);
  uword ProcessWeakProperty(RawWeakProperty* raw_weak,
                            ScavengerVisitor* visitor);
  void Epilogue(Isolate* isolate,
                ScavengerVisitor* visitor,
                bool invoke_api_callbacks);

  bool IsUnreachable(RawObject** p);

  // During a scavenge we need to remember the promoted objects.
  // This is implemented as a stack of objects at the end of the to space. As
  // object sizes are always greater than sizeof(uword) and promoted objects do
  // not consume space in the to space they leave enough room for this stack.
  void PushToPromotedStack(uword addr) {
    ASSERT(scavenging_);
    end_ -= sizeof(addr);
    ASSERT(end_ > top_);
    *reinterpret_cast<uword*>(end_) = addr;
  }
  uword PopFromPromotedStack() {
    ASSERT(scavenging_);
    uword result = *reinterpret_cast<uword*>(end_);
    end_ += sizeof(result);
    ASSERT(end_ <= to_->end());
    return result;
  }
  bool PromotedStackHasMore() const {
    ASSERT(scavenging_);
    return end_ < to_->end();
  }

  void ProcessWeakTables();

  VirtualMemory* space_;
  MemoryRegion* to_;
  MemoryRegion* from_;

  Heap* heap_;

  // Current allocation top and end. These values are being accessed directly
  // from generated code.
  uword top_;
  uword end_;

  // A pointer to the first unscanned object.  Scanning completes when
  // this value meets the allocation top.
  uword resolved_top_;

  // Objects below this address have survived a scavenge.
  uword survivor_end_;

  // All object are aligned to this value.
  uword object_alignment_;

  // Keep track whether a scavenge is currently running.
  bool scavenging_;
  // Keep track whether the scavenge had a promotion failure.
  bool had_promotion_failure_;

  int64_t gc_time_micros_;
  intptr_t collections_;

  // The total size of external data associated with objects in this scavenger.
  intptr_t external_size_;

  friend class ScavengerVisitor;
  friend class ScavengerWeakVisitor;

  DISALLOW_COPY_AND_ASSIGN(Scavenger);
};

}  // namespace dart

#endif  // VM_SCAVENGER_H_
