// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.
// Classes that describe assembly patterns as used by inline caches.

#ifndef VM_INSTRUCTIONS_X64_H_
#define VM_INSTRUCTIONS_X64_H_

#ifndef VM_INSTRUCTIONS_H_
#error Do not include instructions_ia32.h directly; use instructions.h instead.
#endif

#include "vm/allocation.h"
#include "vm/object.h"

namespace dart {

// Forward declarations.
class RawClass;
class Immediate;
class RawObject;

// Abstract class for all instruction pattern classes.
class InstructionPattern : public ValueObject {
 public:
  explicit InstructionPattern(uword pc) : start_(pc) {
    ASSERT(pc != 0);
  }
  virtual ~InstructionPattern() {}

  // Call to check if the instruction pattern at 'pc' match the instruction.
  virtual bool IsValid() const {
    return TestBytesWith(pattern(), pattern_length_in_bytes());
  }

  // 'pattern' returns the expected byte pattern in form of an integer array
  // with length of 'pattern_length_in_bytes'. A '-1' element means 'any byte'.
  virtual const int* pattern() const = 0;
  virtual int pattern_length_in_bytes() const = 0;

  static intptr_t IndexFromPPLoad(uword start);
  static intptr_t OffsetFromPPIndex(intptr_t index);

 protected:
  uword start() const { return start_; }

 private:
  // Returns true if the 'num_bytes' bytes at 'start_' correspond to
  // array of integers 'data'. 'data' elements are either a byte or -1, which
  // represents any byte.
  bool TestBytesWith(const int* data, int num_bytes) const;

  const uword start_;

  DISALLOW_COPY_AND_ASSIGN(InstructionPattern);
};


class JumpPattern : public InstructionPattern {
 public:
  JumpPattern(uword pc, const Code& code)
      : InstructionPattern(pc),
        object_pool_(Array::Handle(code.ObjectPool())) {}
  static int InstructionLength() {
    return kLengthInBytes;
  }
  uword TargetAddress() const;
  void SetTargetAddress(uword new_target) const;
  virtual int pattern_length_in_bytes() const {
    return kLengthInBytes;
  }

  static const int kLengthInBytes = 7;
 private:
  virtual const int* pattern() const;
  const Array& object_pool_;

  DISALLOW_COPY_AND_ASSIGN(JumpPattern);
};


// 5 byte call instruction.
class ShortCallPattern : public InstructionPattern {
 public:
  explicit ShortCallPattern(uword pc) : InstructionPattern(pc) {}
  static int InstructionLength() {
    return kLengthInBytes;
  }

  virtual int pattern_length_in_bytes() const {
    return kLengthInBytes;
  }

  void SetTargetAddress(uword new_target) const;

 private:
  static const int kLengthInBytes = 5;
  virtual const int* pattern() const;

  DISALLOW_COPY_AND_ASSIGN(ShortCallPattern);
};


class ReturnPattern : public InstructionPattern {
 public:
  explicit ReturnPattern(uword pc) : InstructionPattern(pc) {}

  virtual const int* pattern() const;
  virtual int pattern_length_in_bytes() const { return kLengthInBytes; }

 private:
  static const int kLengthInBytes = 1;
};

}  // namespace dart

#endif  // VM_INSTRUCTIONS_X64_H_
