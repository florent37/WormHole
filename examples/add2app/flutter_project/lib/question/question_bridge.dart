import 'package:flutter_bridge/flutter_bridge.dart';
import 'package:flutter_project/question/model/question.dart';

import 'model/answer.dart';

part 'question_bridge.g.dart';

@FlutterBridge()
class QuestionBridge {
  @Expose("question")
  Future<Answer> question(@Param("toto") Question question) {
    return null;
  }

  bind(channelName) => null
}