import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter_project/question/model/question.dart';
import 'package:flutter_project/question/question_bloc.dart';

class QuestionWidget extends StatefulWidget {
  QuestionWidget();

  @override
  _QuestionWidgetState createState() => _QuestionWidgetState();
}

class _QuestionWidgetState extends State<QuestionWidget> {
  final _fieldController = TextEditingController();

  @override
  Widget build(BuildContext context) {
    final questionBloc = BlocProvider.of<QuestionBloc>(context);

    return StreamBuilder<Question>(
        stream: questionBloc.questionToDisplay,
        builder: (context, snap) {
          if (snap.hasData) {
            final Question question = snap.data;
            return Container(
              color: Colors.white,
              child: Center(
                child: Column(
                  children: <Widget>[
                    Text(
                      question.sentence,
                      style: TextStyle(color: Colors.blueAccent),
                    ),
                    Text("your answer"),
                    TextField(
                      controller: this._fieldController,
                      style: TextStyle(),
                      decoration: InputDecoration(hintText: "Your response"),
                    ),
                    RaisedButton(
                      onPressed: () {
                        questionBloc.onAnswer(_fieldController.text);
                      },
                      color: Colors.blueAccent,
                      child: Text(
                        "Submit",
                        style: TextStyle(color: Colors.white),
                      ),
                    ),
                  ],
                ),
              ),
            );
          } else {
            return SizedBox();
          }
        });
  }
}
