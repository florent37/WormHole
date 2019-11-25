import 'dart:async';
export 'dart:async';

class PlatformWaiter<T> {
  final List<Completer<T>> completers = List<Completer<T>>();

  T Function(dynamic) converter;

  void invoke(dynamic argument){
    final T value = converter(argument);
    for (var completer in completers) {
      completer.complete(value);
    }
  }

  void add(Completer<T> completer) {
    completers.add(completer);
  }
}