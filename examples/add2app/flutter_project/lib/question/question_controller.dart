import 'package:flutter_bridge/flutter_bridge.dart';

import 'model/answer.dart';

part 'question_controller.g.dart';

@FlutterBridge()
abstract class QuestionController {

  factory QuestionController(channelName) => Retrieve$QuestionController(channelName);

  @Call("answer")
  void answer(Answer answer);

}
