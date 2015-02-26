// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#include "vm/globals.h"  // Needed here to get TARGET_ARCH_X64.
#if defined(TARGET_ARCH_X64)

#include "vm/cpu.h"
#include "vm/instructions.h"
#include "vm/object.h"

namespace dart {

intptr_t InstructionPattern::IndexFromPPLoad(uword start) {
  int32_t offset = *reinterpret_cast<int32_t*>(start);
  offset += kHeapObjectTag;
  return (offset - Array::data_offset()) / kWordSize;
}


intptr_t InstructionPattern::OffsetFromPPIndex(intptr_t index) {
  intptr_t offset = Array::element_offset(index);
  return offset - kHeapObjectTag;
}


bool InstructionPattern::TestBytesWith(const int* data, int num_bytes) const {
  ASSERT(data != NULL);
  const uint8_t* byte_array = reinterpret_cast<const uint8_t*>(start_);
  for (int i = 0; i < num_bytes; i++) {
    // Skip comparison for data[i] < 0.
    if ((data[i] >= 0) && (byte_array[i] != (0xFF & data[i]))) {
      return false;
    }
  }
  return true;
}


uword JumpPattern::TargetAddress() const {
  ASSERT(IsValid());
  int index = InstructionPattern::IndexFromPPLoad(start() + 3);
  return reinterpret_cast<uword>(object_pool_.At(index));
}


void JumpPattern::SetTargetAddress(uword target) const {
  ASSERT(IsValid());
  int index = InstructionPattern::IndexFromPPLoad(start() + 3);
  const Smi& smi = Smi::Handle(reinterpret_cast<RawSmi*>(target));
  object_pool_.SetAt(index, smi);
  // No need to flush the instruction cache, since the code is not modified.
}


const int* JumpPattern::pattern() const {
  //  07: 41 ff a7 imm32  jmpq [reg + off]
  static const int kJumpPattern[kLengthInBytes] =
      {0x41, 0xFF, -1, -1, -1, -1, -1};
  return kJumpPattern;
}


void ShortCallPattern::SetTargetAddress(uword target) const {
  ASSERT(IsValid());
  *reinterpret_cast<uint32_t*>(start() + 1) = target - start() - kLengthInBytes;
  CPU::FlushICache(start() + 1, kWordSize);
}


const int* ShortCallPattern::pattern() const {
  static const int kCallPattern[kLengthInBytes] = {0xE8, -1, -1, -1, -1};
  return kCallPattern;
}


const int* ReturnPattern::pattern() const {
  static const int kReturnPattern[kLengthInBytes] = { 0xC3 };
  return kReturnPattern;
}

}  // namespace dart

#endif  // defined TARGET_ARCH_X64
