import 'package:meta/meta.dart';
export 'platform_waiter.dart';
export 'dart:convert';

class FlutterPlatformDirection {
  static const String FROM = "FROM";
  static const String TO = "TO";
  static const String BIND = "BIND";
  static const String INJECT = "INJECT";
  static const String PARAM = "PARAM";
}

abstract class PlatformAnnotation {
  final String direction;
  final String name;
  const PlatformAnnotation(this.direction, this.name);
}

@immutable
class FlutterBridge {
  const FlutterBridge();
}

@immutable
class Wait extends PlatformAnnotation {
  const Wait(String name) : super(FlutterPlatformDirection.FROM, name);
}

@immutable
class Call extends PlatformAnnotation {
  const Call(String name) : super(FlutterPlatformDirection.TO, name);
}

@immutable
class Expose extends PlatformAnnotation {
  const Expose(String name) : super(FlutterPlatformDirection.BIND, name);
}

@immutable
class Param extends PlatformAnnotation {
  const Param(String name) : super(FlutterPlatformDirection.PARAM, name);
}

@immutable
class Retrieve extends PlatformAnnotation {
  const Retrieve({String channelName = null}) : super(FlutterPlatformDirection.INJECT, channelName);
}
