package com.framr.flutterwebview.flutterwebview;

import android.content.Context;
import android.util.Log;

import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.StandardMessageCodec;
import io.flutter.plugin.platform.PlatformView;
import io.flutter.plugin.platform.PlatformViewFactory;

public class FlutterwebviewFactory extends PlatformViewFactory {
    private final BinaryMessenger messenger;

    public FlutterwebviewFactory(BinaryMessenger messenger) {
        super(StandardMessageCodec.INSTANCE);
        this.messenger = messenger;
    }

    @Override
    public PlatformView create(Context context, int id, Object o) {
        return new Flutterwebview(context, messenger, id, o);
    }
}
