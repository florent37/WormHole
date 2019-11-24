import 'package:flutter_bridge/flutter_bridge.dart';
import 'package:source_gen_test/annotations.dart';

@ShouldGenerate(
  r'''
class Bridge_QuestionBinding {
  Bridge_QuestionBinding(this.channelName) {
    this._bridge = flutterBridge().findOrCreate(channelName);
    this._bridge.addMethodCallHandler(_bridge_handleMessage);
  }

  BridgeManager _bridge;

  final String channelName;

  QuestionBinding target;

  void bind(QuestionBinding target) {
    this.target = target;
    target.bridge = QuestionBridge("test");
  }

  Future<dynamic> _bridge_handleMessage(MethodCall call) async {
    final name = call.method;
    final input = call.arguments as String;
    if (name == "answer") {
      final value = Answer.fromJson(json.decode(input) as Map<String, dynamic>);
      target.displayTask(value);
      return null;
    }
  }
}

''',
)
@FlutterBridge()
class QuestionBinding {

  @Retrieve(channelName: "test") QuestionBridge bridge;

  @Expose("answer")
  void displayTask(Answer answer) {
    print(answer);
  }

  void bind(String channelName) {
    //Bridge_QuestionBinding(channelName).bind(this);
  }
}

@ShouldGenerate(r'''
class _QuestionBridge implements QuestionBridge {
  _QuestionBridge(this.channelName) {
    this._bridge = flutterBridge().findOrCreate(channelName);
    this._bridge.addMethodCallHandler((MethodCall call) async {
      final name = call.method;
      final json = call.arguments as String;
      _waiters[name]?.invoke(json);
    });
  }

  BridgeManager _bridge;

  final String channelName;

  final Map<String, PlatformWaiter> _waiters = Map<String, PlatformWaiter>();

  @override
  void question(Question question) async {
    _bridge.invokeMethod("question", json.encode(question.toJson()));
  }

  @override
  Future<Answer> questionAndWait(Question question) async {
    final String jsonString = await _bridge.invokeMethod(
        "question", json.encode(question.toJson())) as String;
    final jsonMap = json.decode(jsonString) as Map<String, dynamic>;
    return Answer.fromJson(jsonMap);
  }

  @override
  Future<Answer> answer() async {
    //create the waiter
    if (_waiters["answer"] == null) {
      final waiter = PlatformWaiter<Answer>();
      _waiters["answer"] = waiter;
      waiter.converter = (input) {
        return Answer.fromJson(json.decode(input) as Map<String, dynamic>);
      };
    }
//add our futur
    final completer = Completer<Answer>();
    _waiters["answer"].add(completer);
    return completer.future;
  }
}
''', contains: true)
@FlutterBridge()
abstract class QuestionBridge {
  //factory QuestionBridge(String channelName) = _QuestionBridge;

  @Wait("answer")
  Future<Answer> answer();

  @Call("question")
  void question(Question question);

  @Call("question")
  Future<Answer> questionAndWait(Question question);
}

class Question{}
class Answer{}