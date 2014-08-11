
/**
 * TODO(scheglov) invalid implementation
 */
class WeakReference<T> {
  final T value;
  WeakReference(this.value);
  T get() => value;
}
