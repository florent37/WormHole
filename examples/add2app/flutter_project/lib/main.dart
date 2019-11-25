import 'dart:ui';

import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:flutter_project/question/question_bloc.dart';
import 'package:flutter_project/question/question_widget.dart';

void main() => runApp(
      MaterialApp(
        debugShowCheckedModeBanner: false,
        home: Scaffold(
          body: _widgetForRoute(
            window.defaultRouteName,
          ),
        ),
      ),
    );

Widget _widgetForRoute(String route) {
  switch (route) {
    case "/questions":
      return BlocProvider<QuestionBloc>(
          creator: (_context, _bag) => QuestionBloc(),
          child: QuestionWidget());
    default:
      return Center(
        child: Text('Unknown route: $route', textDirection: TextDirection.ltr),
      );
  }
}