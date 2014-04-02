// Copyright (c) 2014, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#ifndef VM_CPU_ARM_H_
#define VM_CPU_ARM_H_

#include "vm/allocation.h"
#include "vm/simulator.h"

namespace dart {

// TargetCPUFeatures gives CPU features for the architecture that we are
// generating code for. HostCPUFeatures gives the CPU features for the
// architecture that we are actually running on. When the architectures
// are the same, TargetCPUFeatures will query HostCPUFeatures. When they are
// different (i.e. we are running in a simulator), HostCPUFeatures will
// additionally mock the options needed for the target architecture so that
// they may be altered for testing.

enum ARMVersion {
  ARMv6,
  ARMv7,
  ARMvUnknown,
};

class HostCPUFeatures: public AllStatic {
 public:
  static void InitOnce();
  static void Cleanup();
  static const char* hardware() {
    DEBUG_ASSERT(initialized_);
    return hardware_;
  }
  static bool integer_division_supported() {
    DEBUG_ASSERT(initialized_);
    return integer_division_supported_;
  }
  static bool neon_supported() {
    DEBUG_ASSERT(initialized_);
    return neon_supported_;
  }
  static ARMVersion arm_version() {
    DEBUG_ASSERT(initialized_);
    return arm_version_;
  }

#if !defined(HOST_ARCH_ARM)
  static void set_integer_division_supported(bool supported) {
    DEBUG_ASSERT(initialized_);
    integer_division_supported_ = supported;
  }
  static void set_neon_supported(bool supported) {
    DEBUG_ASSERT(initialized_);
    neon_supported_ = supported;
  }
  static void set_arm_version(ARMVersion version) {
    DEBUG_ASSERT(initialized_);
    arm_version_ = version;
  }
#endif  // !defined(HOST_ARCH_ARM)

 private:
  static const char* hardware_;
  static bool integer_division_supported_;
  static bool neon_supported_;
  static ARMVersion arm_version_;
#if defined(DEBUG)
  static bool initialized_;
#endif
};

class TargetCPUFeatures : public AllStatic {
 public:
  static void InitOnce() {
    HostCPUFeatures::InitOnce();
  }
  static void Cleanup() {
    HostCPUFeatures::Cleanup();
  }
  static bool double_truncate_round_supported() {
    return false;
  }
  static bool integer_division_supported() {
    return HostCPUFeatures::integer_division_supported();
  }
  static bool neon_supported() {
    return HostCPUFeatures::neon_supported();
  }
  static const char* hardware() {
    return HostCPUFeatures::hardware();
  }
  static ARMVersion arm_version() {
    return HostCPUFeatures::arm_version();
  }
};

}  // namespace dart

#endif  // VM_CPU_ARM_H_
