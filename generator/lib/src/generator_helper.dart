import 'package:analyzer/dart/element/element.dart';
import 'package:analyzer/dart/element/type.dart';
import 'package:built_collection/built_collection.dart';
import 'package:code_builder/code_builder.dart';
import 'package:source_gen/source_gen.dart';

TypeChecker typeChecker(Type type) => new TypeChecker.fromRuntime(type);

ConstantReader getMethodAnnotation(MethodElement method, List _methodsAnnotations) {
  //print("searching annotation into ${method.name}");
  for (final type in _methodsAnnotations) {
    //print("searching for ${type}");
    final annot = typeChecker(type).firstAnnotationOf(method, throwOnUnresolved: false);
    if (annot != null) {
      //print("found");
      return new ConstantReader(annot);
    }
  }
  return null;
}

ConstantReader getParameterAnnotation(ParameterElement parameter, List _parametersAnnotations) {
  //print("searching annotation into ${method.name}");
  for (final type in _parametersAnnotations) {
    //print("searching for ${type}");
    final annot = typeChecker(type).firstAnnotationOf(parameter, throwOnUnresolved: false);
    if (annot != null) {
      //print("found");
      return new ConstantReader(annot);
    }
  }
  return null;
}

ConstantReader getFieldAnnotation(FieldElement method, List _fieldsAnnotations) {
  //print("searching annotation into ${method.name}");
  for (final type in _fieldsAnnotations) {
    //print("searching for ${type}");
    final annot = typeChecker(type).firstAnnotationOf(method, throwOnUnresolved: false);
    if (annot != null) {
      //print("found");
      return new ConstantReader(annot);
    }
  }
  return null;
}

DartType genericOf(DartType type) {
  return type is InterfaceType && type.typeArguments.isNotEmpty ? type.typeArguments.first : null;
}

DartType getResponseType(DartType type) {
  return genericOf(type);
}

DartType getResponseInnerType(DartType type) {
  final generic = genericOf(type);

  if (generic == null || typeChecker(Map).isExactlyType(type) || typeChecker(BuiltMap).isExactlyType(type)) return type;

  if (generic.isDynamic) return null;

  if (typeChecker(List).isExactlyType(type) || typeChecker(BuiltList).isExactlyType(type)) return generic;

  return getResponseInnerType(generic);
}

String futureOf(DartType type) {
  return "Future<${type.toString()}>";
}