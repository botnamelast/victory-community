package com.victory.community.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class RootOverlayService extends Service {
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
