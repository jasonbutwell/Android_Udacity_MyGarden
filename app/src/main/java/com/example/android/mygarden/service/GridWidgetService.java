package com.example.android.mygarden.service;

import android.content.Intent;
import android.widget.RemoteViewsService;

/**
 * Created by J on 11/07/2017.
 */

public class GridWidgetService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new GridRemoteViewsFactory(this.getApplicationContext());
    }
}
