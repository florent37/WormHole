// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'question_controller.dart';

// **************************************************************************
// FlutterWormHoleGenerator
// **************************************************************************

class Retrieve$QuestionController implements QuestionController {
  Retrieve$QuestionController(this.channelName) {
    this._bridge = flutterBridge().findOrCreate(channelName);
    this._bridge.addMethodCallHandler((MethodCall call) async {
      final name = call.method;
      final arg = call.arguments;
      _waiters[name]?.invoke(arg);
    });
  }

  BridgeManager _bridge;

  final String channelName;

  final Map<String, PlatformWaiter> _waiters = Map<String, PlatformWaiter>();

  @override
  void answer(Answer answer) async {
    _bridge.invokeMethod("answer", answer.toJson());
  }
}
