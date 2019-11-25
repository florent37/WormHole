// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'question_bloc.dart';

// **************************************************************************
// FlutterWormHoleGenerator
// **************************************************************************

class Expose$QuestionBloc {
  Expose$QuestionBloc(this.channelName) {
    this._bridge = flutterBridge().findOrCreate(channelName);
    this._bridge.addMethodCallHandler($bridge$handleMessage);
  }

  BridgeManager _bridge;

  final String channelName;

  QuestionBloc _target;

  QuestionBloc expose(QuestionBloc target) {
    this._target = target;
    _target.questionBridge = QuestionController(channelName);
    return target;
  }

  Future<dynamic> $bridge$handleMessage(MethodCall call) async {
    final name = call.method;
    final List<dynamic> returns = List();
    if (name == "question") {
      try {
        if (call.arguments != null && call.arguments is Question) {
          final $parameter$$ = call.arguments as Question;
          _target.question($parameter$$);
        } else if (call.arguments != null &&
            call.arguments is Map<dynamic, dynamic>) {
          final $input$$ = Map<String, dynamic>.from(
              call.arguments as Map<dynamic, dynamic>);
          dynamic $parameter$$;
          try {
/* trying to decode the map directly to our object */
            $parameter$$ =
                Question.fromJson(Map<String, dynamic>.from($input$$));
          } catch (t) {
/* look inside the map to fetch the object, using @Param */
            if ((call.arguments as Map<dynamic, dynamic>).length >= 1) {
              $parameter$$ = Question.fromJson(Map<String, dynamic>.from(
                  $input$$["question"] as Map<dynamic, dynamic>));
            } else {
              print(
                  "cannot retrieve a Question from arguments, are you sure the native implementation method send at lease one argument ?");
            }
          }
          _target.question($parameter$$);
        }
      } catch (e) {
        print("error while calling question");
        print(e);
      }
    }
    return firstNotNull(returns);
  }
}
