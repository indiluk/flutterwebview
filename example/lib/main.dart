import 'package:flutter/material.dart';
import 'dart:async';
import 'dart:io' as Io;
import 'package:flutter_cache_manager/flutter_cache_manager.dart';
import 'package:path_provider/path_provider.dart';

import 'package:flutterwebview/flutterwebview.dart';

void main() => runApp(MyApp());

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {

  FlutterwebviewController _controller;

  @override
  void initState() {
    super.initState();
  }

  Future<String> get _localPath async {
    final directory = await getApplicationDocumentsDirectory();
    return directory.path;
  }

  Future<Io.File> getImageFromNetwork(String url) async {
    var cacheManager = await CacheManager.getInstance();
    Io.File file = await cacheManager.getFile(url);
    return file;
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: Flutterwebview(
          javascriptInterface: "flutterwebview",
          onRequestCallback: (url) {

            // print(url);
            Uri uri = Uri.parse(url);
            if(uri.path.split('.').last.toLowerCase() == 'png') {
              return getImageFromNetwork('https://cdn.pixabay.com/photo/2017/08/22/16/23/cat-2669554_960_720.png').then((file) {
                return Future.value(FlutterwebviewOnRequestResponse(
                    path: file.path,
                    mimeType: "image/png"
                ));
              });
            }

            switch(uri.host) {
              case "localhost":
                switch(uri.path) {
                  case "/script.js":
                    return Future.value(FlutterwebviewOnRequestResponse(
                        content: 'console.log("hello world!");',
                        mimeType: "text/javascript"
                    ));
                }
                break;
            }

            return null;
          },
          onFlutterwebviewCreated: (controller) {
            _controller = controller;
            _controller.loadUrl('https://google.com');
            _controller.onLoadingComplete.listen((url) {
              print("Finished loading URL: $url");
            });
            _controller.onRequest.listen((url) {
              print("Requested URL: $url");
            });
          },
        ),
      ),
    );
  }
}
