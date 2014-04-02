// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#include "vm/freelist.h"

#include <map>
#include <utility>

#include "vm/bit_set.h"
#include "vm/object.h"
#include "vm/raw_object.h"

namespace dart {


FreeListElement* FreeListElement::AsElement(uword addr, intptr_t size) {
  // Precondition: the (page containing the) header of the element is
  // writable.
  ASSERT(size >= kObjectAlignment);
  ASSERT(Utils::IsAligned(size, kObjectAlignment));

  FreeListElement* result = reinterpret_cast<FreeListElement*>(addr);

  uword tags = 0;
  tags = RawObject::SizeTag::update(size, tags);
  tags = RawObject::ClassIdTag::update(kFreeListElement, tags);

  result->tags_ = tags;
  if (size > RawObject::SizeTag::kMaxSizeTag) {
    *result->SizeAddress() = size;
  }
  result->set_next(NULL);
  return result;
  // Postcondition: the (page containing the) header of the element is
  // writable.
}


void FreeListElement::InitOnce() {
  ASSERT(sizeof(FreeListElement) == kObjectAlignment);
  ASSERT(OFFSET_OF(FreeListElement, tags_) == Object::tags_offset());
}


intptr_t FreeListElement::HeaderSizeFor(intptr_t size) {
  if (size == 0) return 0;
  return ((size > RawObject::SizeTag::kMaxSizeTag) ? 3 : 2) * kWordSize;
}


FreeList::FreeList() {
  Reset();
}


FreeList::~FreeList() {
  // Nothing to release.
}


uword FreeList::TryAllocate(intptr_t size, bool is_protected) {
  // Precondition: is_protected is false or else all free list elements are
  // in non-writable pages.

  // Postcondition: if allocation succeeds, the allocated block is writable.
  int index = IndexForSize(size);
  if ((index != kNumLists) && free_map_.Test(index)) {
    FreeListElement* element = DequeueElement(index);
    if (is_protected) {
      bool status =
          VirtualMemory::Protect(reinterpret_cast<void*>(element),
                                 size,
                                 VirtualMemory::kReadWrite);
      ASSERT(status);
    }
    return reinterpret_cast<uword>(element);
  }

  if ((index + 1) < kNumLists) {
    intptr_t next_index = free_map_.Next(index + 1);
    if (next_index != -1) {
      // Dequeue an element from the list, split and enqueue the remainder in
      // the appropriate list.
      FreeListElement* element = DequeueElement(next_index);
      if (is_protected) {
        // Make the allocated block and the header of the remainder element
        // writable.  The remainder will be non-writable if necessary after
        // the call to SplitElementAfterAndEnqueue.
        // If the remainder size is zero, only the element itself needs to
        // be made writable.
        intptr_t remainder_size = element->Size() - size;
        intptr_t region_size =
            size + FreeListElement::HeaderSizeFor(remainder_size);
        bool status =
            VirtualMemory::Protect(reinterpret_cast<void*>(element),
                                   region_size,
                                   VirtualMemory::kReadWrite);
        ASSERT(status);
      }
      SplitElementAfterAndEnqueue(element, size, is_protected);
      return reinterpret_cast<uword>(element);
    }
  }

  FreeListElement* previous = NULL;
  FreeListElement* current = free_lists_[kNumLists];
  while (current != NULL) {
    if (current->Size() >= size) {
      // Found an element large enough to hold the requested size. Dequeue,
      // split and enqueue the remainder.
      intptr_t remainder_size = current->Size() - size;
      intptr_t region_size =
          size + FreeListElement::HeaderSizeFor(remainder_size);
      if (is_protected) {
        // Make the allocated block and the header of the remainder element
        // writable.  The remainder will be non-writable if necessary after
        // the call to SplitElementAfterAndEnqueue.
        bool status =
            VirtualMemory::Protect(reinterpret_cast<void*>(current),
                                   region_size,
                                   VirtualMemory::kReadWrite);
        ASSERT(status);
      }

      if (previous == NULL) {
        free_lists_[kNumLists] = current->next();
      } else {
        // If the previous free list element's next field is protected, it
        // needs to be unprotected before storing to it and reprotected
        // after.
        bool target_is_protected = false;
        uword target_address = 0L;
        if (is_protected) {
          uword writable_start = reinterpret_cast<uword>(current);
          uword writable_end = writable_start + region_size - 1;
          target_address = previous->next_address();
          target_is_protected =
              !VirtualMemory::InSamePage(target_address, writable_start) &&
              !VirtualMemory::InSamePage(target_address, writable_end);
        }
        if (target_is_protected) {
          bool status =
              VirtualMemory::Protect(reinterpret_cast<void*>(target_address),
                                     kWordSize,
                                     VirtualMemory::kReadWrite);
          ASSERT(status);
        }
        previous->set_next(current->next());
        if (target_is_protected) {
          bool status =
              VirtualMemory::Protect(reinterpret_cast<void*>(target_address),
                                     kWordSize,
                                     VirtualMemory::kReadExecute);
          ASSERT(status);
        }
      }
      SplitElementAfterAndEnqueue(current, size, is_protected);
      return reinterpret_cast<uword>(current);
    }
    previous = current;
    current = current->next();
  }
  return 0;
}


void FreeList::Free(uword addr, intptr_t size) {
  // Precondition required by AsElement and EnqueueElement: the (page
  // containing the) header of the freed block should be writable.  This is
  // the case when called for newly allocated pages because they are
  // allocated as writable.  It is the case when called during GC sweeping
  // because the entire heap is writable.
  intptr_t index = IndexForSize(size);
  FreeListElement* element = FreeListElement::AsElement(addr, size);
  EnqueueElement(element, index);
  // Postcondition: the (page containing the) header is left writable.
}


void FreeList::Reset() {
  free_map_.Reset();
  for (int i = 0; i < (kNumLists + 1); i++) {
    free_lists_[i] = NULL;
  }
}


intptr_t FreeList::IndexForSize(intptr_t size) {
  ASSERT(size >= kObjectAlignment);
  ASSERT(Utils::IsAligned(size, kObjectAlignment));

  intptr_t index = size / kObjectAlignment;
  if (index >= kNumLists) {
    index = kNumLists;
  }
  return index;
}


void FreeList::EnqueueElement(FreeListElement* element, intptr_t index) {
  FreeListElement* next = free_lists_[index];
  if (next == NULL && index != kNumLists) {
    free_map_.Set(index, true);
  }
  element->set_next(next);
  free_lists_[index] = element;
}


FreeListElement* FreeList::DequeueElement(intptr_t index) {
  FreeListElement* result = free_lists_[index];
  FreeListElement* next = result->next();
  if (next == NULL && index != kNumLists) {
    free_map_.Set(index, false);
  }
  free_lists_[index] = next;
  return result;
}


intptr_t FreeList::Length(int index) const {
  ASSERT(index >= 0);
  ASSERT(index < kNumLists);
  intptr_t result = 0;
  FreeListElement* element = free_lists_[index];
  while (element != NULL) {
    ++result;
    element = element->next();
  }
  return result;
}


void FreeList::PrintSmall() const {
  int small_sizes = 0;
  int small_objects = 0;
  intptr_t small_bytes = 0;
  for (int i = 0; i < kNumLists; ++i) {
    if (free_lists_[i] == NULL) {
      continue;
    }
    small_sizes += 1;
    intptr_t list_length = Length(i);
    small_objects += list_length;
    intptr_t list_bytes = list_length * i * kObjectAlignment;
    small_bytes += list_bytes;
    OS::Print("small %3d [%8d bytes] : "
              "%8" Pd " objs; %8.1f KB; %8.1f cum KB\n",
              i,
              i * kObjectAlignment,
              list_length,
              list_bytes / static_cast<double>(KB),
              small_bytes / static_cast<double>(KB));
  }
}


void FreeList::PrintLarge() const {
  int large_sizes = 0;
  int large_objects = 0;
  intptr_t large_bytes = 0;
  std::map<intptr_t, intptr_t> sorted;
  std::map<intptr_t, intptr_t>::iterator it;
  FreeListElement* node;
  for (node = free_lists_[kNumLists]; node != NULL; node = node->next()) {
    it = sorted.find(node->Size());
    if (it != sorted.end()) {
      it->second += 1;
    } else {
      large_sizes += 1;
      sorted.insert(std::make_pair(node->Size(), 1));
    }
    large_objects += 1;
  }
  for (it = sorted.begin(); it != sorted.end(); ++it) {
    intptr_t size = it->first;
    intptr_t list_length = it->second;
    intptr_t list_bytes = list_length * size;
    large_bytes += list_bytes;
    OS::Print("large %3" Pd " [%8" Pd " bytes] : "
              "%8" Pd " objs; %8.1f KB; %8.1f cum KB\n",
              size / kObjectAlignment,
              size,
              list_length,
              list_bytes / static_cast<double>(KB),
              large_bytes / static_cast<double>(KB));
  }
}


void FreeList::Print() const {
  PrintSmall();
  PrintLarge();
}


void FreeList::SplitElementAfterAndEnqueue(FreeListElement* element,
                                           intptr_t size,
                                           bool is_protected) {
  // Precondition required by AsElement and EnqueueElement: either
  // element->Size() == size, or else the (page containing the) header of
  // the remainder element starting at element + size is writable.
  intptr_t remainder_size = element->Size() - size;
  if (remainder_size == 0) return;

  uword remainder_address = reinterpret_cast<uword>(element) + size;
  element = FreeListElement::AsElement(remainder_address, remainder_size);
  intptr_t remainder_index = IndexForSize(remainder_size);
  EnqueueElement(element, remainder_index);

  // Postcondition: when allocating in a protected page, the remainder
  // element is no longer writable unless it is in the same page as the
  // allocated element.  (The allocated element is still writable, and the
  // remainder element will be protected when the allocated one is).
  if (is_protected &&
      !VirtualMemory::InSamePage(remainder_address - 1, remainder_address)) {
    bool status =
        VirtualMemory::Protect(reinterpret_cast<void*>(remainder_address),
                               remainder_size,
                               VirtualMemory::kReadExecute);
    ASSERT(status);
  }
}

}  // namespace dart
