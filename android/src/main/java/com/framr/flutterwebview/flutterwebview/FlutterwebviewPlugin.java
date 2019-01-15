package com.framr.flutterwebview.flutterwebview;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

/** FlutterwebviewPlugin */
public class FlutterwebviewPlugin {
      /** Plugin registration. */
      public static void registerWith(Registrar registrar) {
          registrar.platformViewRegistry().registerViewFactory(
                          "com.framr.flutterwebview.flutterwebview/flutterwebview", new FlutterwebviewFactory(registrar.messenger()));
      }
}
