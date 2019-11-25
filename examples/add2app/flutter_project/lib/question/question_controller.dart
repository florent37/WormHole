import 'package:wormhole/wormhole.dart';

import 'model/answer.dart';

part 'question_controller.g.dart';

@WormHole()
abstract class QuestionController {

  factory QuestionController(channelName) => Retrieve$QuestionController(channelName);

  @Call("answer")
  void answer(Answer answer);

}
