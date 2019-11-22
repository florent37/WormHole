import 'package:flutter_bridge/flutter_bridge.dart';
import 'package:flutter_project/question/model/question.dart';
import 'package:rxdart/rxdart.dart';

import 'model/answer.dart';

part 'question_bridge.g.dart';

@FlutterBridge()
class QuestionBridge {

  /* inspired from Bloc, used to display Question into widget */
  final _questionToDisplay = BehaviorSubject<Question>();
  Observable<Question> get questionToDisplay => _questionToDisplay.stream;

  /* created when we call question(question),
   * represents the future which will be returned to the native app
   */
  Completer<Answer> completableAnswer;

  @Expose("question")
  Future<Answer> question(Question question) async {
    //create a completer for the answer
    completableAnswer = Completer<Answer>();

    //push into my stream to be displayed into my view
    _questionToDisplay.add(question);

    //await until we push a value inside completableAnswer
    return completableAnswer.future;
  }

  void onAnswer(String answer) {
    /* now we can return a value to front */
    if(!completableAnswer.isCompleted) {
      completableAnswer.complete(Answer(answer));
    }
  }

  bind(channelName) => Bridge$QuestionBridge(channelName).bind(this);

  void dispose() {
    _questionToDisplay.close();
  }

}