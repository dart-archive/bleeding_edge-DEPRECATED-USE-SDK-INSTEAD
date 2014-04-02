// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#include "vm/virtual_memory.h"

#include "platform/assert.h"
#include "platform/utils.h"

namespace dart {

bool VirtualMemory::InSamePage(uword address0, uword address1) {
  return (Utils::RoundDown(address0, PageSize()) ==
          Utils::RoundDown(address1, PageSize()));
}


VirtualMemory* VirtualMemory::ReserveAligned(intptr_t size,
                                             intptr_t alignment) {
  ASSERT((size & (PageSize() - 1)) == 0);
  ASSERT(Utils::IsPowerOfTwo(alignment));
  ASSERT(alignment >= PageSize());
  VirtualMemory* result = VirtualMemory::Reserve(size + alignment);
  if (result == NULL) {
    FATAL("Out of memory.\n");
  }
  uword start = result->start();
  uword real_start = (start + alignment - 1) & ~(alignment - 1);
  result->Truncate(real_start, size);
  return result;
}


void VirtualMemory::Truncate(uword new_start, intptr_t new_size) {
  ASSERT(new_start >= start());
  ASSERT((new_size & (PageSize() - 1)) == 0);
  if (new_start > start()) {
    uword split = new_start - start();
    ASSERT((split & (PageSize() - 1)) == 0);
    FreeSubSegment(address(), split);
    region_.Subregion(region_, split, size() - split);
  }
  ASSERT(new_size <= size());
  FreeSubSegment(reinterpret_cast<void*>(start() + new_size),
                 size() - new_size);
  region_.Subregion(region_, 0, new_size);
}

}  // namespace dart
