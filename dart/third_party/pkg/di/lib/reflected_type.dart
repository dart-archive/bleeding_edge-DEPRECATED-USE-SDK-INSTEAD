library di.src.reflected_type;

import 'dart:mirrors' show ClassMirror;

Map<ClassMirror, Type> _cache = <ClassMirror, Type>{};

// Removed the horrible hack to work around bug 
// http://dartbug.com/12607 as it is no longer needed, nor does
// it work with the new MirrorsUsed tracking that disallows 
// reflection on internal libraries. This will go away with an
// update of DI.
Type getReflectedTypeWorkaround(ClassMirror cls) {
  return cls.reflectedType;
}
