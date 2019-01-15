import 'dart:async';
import 'dart:convert';

import 'package:flutter/services.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';

/*class Flutterwebview {
  static const MethodChannel _channel =
      const MethodChannel('flutterwebview');

  static Future<String> get platformVersion async {
    final String version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }
}*/

typedef void FlutterwebviewCreatedCallback(FlutterwebviewController controller);
typedef Future<FlutterwebviewOnRequestResponse> FlutterwebviewOnRequestCallback(String url);

class FlutterwebviewOnRequestResponse {

  String mimeType;
  String content;
  List<String> header = [];
  String path;

  FlutterwebviewOnRequestResponse({
    this.mimeType = "text/plain",
    this.content,
    this.header = const [],
    this.path
  });

  Map<String, dynamic> toJson() {
    Map<String, dynamic> json = {
      'mimeType': mimeType,
      'headers': header,
    };

    if(this.content != null) {
      json['content'] = this.content;
    }
    else if(this.path != null) {
      json['path'] = this.path;
    }
    else {
      throw Exception("Either path or content must be specified!");
    }

    return json;
  }


}

class Flutterwebview extends StatefulWidget {

  final FlutterwebviewCreatedCallback onFlutterwebviewCreated;
  final FlutterwebviewOnRequestCallback onRequestCallback;
  final String javascriptInterface;
  final bool debugging;

  const Flutterwebview({
    Key key,
    this.onFlutterwebviewCreated,
    this.onRequestCallback,
    this.javascriptInterface,
    this.debugging = false
  }) : super(key: key);

  Map<String, dynamic> toJson() =>
      {
        'javascriptInterface': javascriptInterface,
        'debugging': debugging
      };

  @override
  State<StatefulWidget> createState() => _FlutterwebviewState();
}

class _FlutterwebviewState extends State<Flutterwebview> {
  @override
  Widget build(BuildContext context) {
    Map<String, dynamic> creationParams = {};

    if(widget.javascriptInterface != null) {
      creationParams['javascriptInterface'] = widget.javascriptInterface;
    }

    if (defaultTargetPlatform == TargetPlatform.android) {
      return AndroidView(
        // creationParams: creationParams.keys.length > 0 ? creationParams : null,
        creationParams: widget.toJson(),
        creationParamsCodec: StandardMessageCodec(),
        viewType: 'com.framr.flutterwebview.flutterwebview/flutterwebview',
        onPlatformViewCreated: _onPlatformViewCreated,
      );
    }
    return Text(
        '$defaultTargetPlatform is not yet supported by the flutterwebview plugin');
  }

  void _onPlatformViewCreated(int id) {
    if (widget.onFlutterwebviewCreated == null) {
      return;
    }
    widget.onFlutterwebviewCreated(new FlutterwebviewController.init(widget, id));
  }
}

class FlutterwebviewController {

  Flutterwebview _view;

  FlutterwebviewController.init(Flutterwebview view, int id) {
    _view = view;
    _channel = new MethodChannel('com.framr.flutterwebview.flutterwebview/flutterwebview_$id');
    _channel.setMethodCallHandler((MethodCall call) async {
      switch(call.method) {
        case "onRequest":
          if(_view.onRequestCallback != null) {
            dynamic response = await _view.onRequestCallback(call.arguments);
            if(response is FlutterwebviewOnRequestResponse) {
              return jsonEncode(response.toJson());
            }
          }
          return new Future.value("");

        default:
          return Future.value("");
      }
    });

    _onLoadingCompleteChannel = new EventChannel('com.framr.flutterwebview.flutterwebview/flutterwebview_loadingComplete_$id');
    _onRequestChannel = new EventChannel('com.framr.flutterwebview.flutterwebview/flutterwebview_onRequest_$id');
  }

  MethodChannel _channel;
  EventChannel _onLoadingCompleteChannel;
  EventChannel _onRequestChannel;

  Future<void> loadUrl(String url) async {
    assert(url != null);
    return _channel.invokeMethod('loadUrl', url);
  }

  Stream<String> get onLoadingComplete {
    var url = _onLoadingCompleteChannel
        .receiveBroadcastStream()
        .map<String>(
            (element) => element);
    return url;
  }

  Stream<String> get onRequest {
    var url = _onRequestChannel
        .receiveBroadcastStream()
        .map<String>(
            (element) => element);
    return url;
  }
}
