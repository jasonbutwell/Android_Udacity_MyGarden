package com.example.android.mygarden.service;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.example.android.mygarden.R;
import com.example.android.mygarden.provider.PlantContract;
import com.example.android.mygarden.ui.PlantDetailActivity;
import com.example.android.mygarden.utils.PlantUtils;

import static com.example.android.mygarden.provider.PlantContract.BASE_CONTENT_URI;
import static com.example.android.mygarden.provider.PlantContract.PATH_PLANTS;

/**
 * Created by J on 11/07/2017.
 */

class GridRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {

    Context mContext;   // To access the content resolver
    Cursor mCursor;     // To obtain plant data from database

    GridRemoteViewsFactory(Context applicationContext) {
        mContext = applicationContext;
    }

    @Override
    public void onCreate() {
    }

    // Called at the start and when notifyAppWidgetViewDataChanged is called

    @Override
    public void onDataSetChanged() {
        // Get all plant info ordered by creation time
        Uri PLANT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_PLANTS).build();

        // close the previous cursor obtained if it is not null

        if (mCursor != null) mCursor.close();

        // execute query to obtain plants in database and order by creation time

        mCursor = mContext.getContentResolver().query(
                PLANT_URI,
                null,
                null,
                null,
                PlantContract.PlantEntry.COLUMN_CREATION_TIME
        );
    }

    // Close the cursor when destroyed

    @Override
    public void onDestroy() {
        mCursor.close();
    }

    // Return the number of items

    @Override
    public int getCount() {
        return (mCursor == null) ? 0 : mCursor.getCount();
    }

    @Override
    public RemoteViews getViewAt(int position) {

        // Return null if nothing in cursor

        if (mCursor == null || mCursor.getCount() == 0) return null;

        // Move to first item in Cursor

        mCursor.moveToFirst();

        // Column Indexes
        int idIndex = mCursor.getColumnIndex(PlantContract.PlantEntry._ID);
        int createTimeIndex = mCursor.getColumnIndex(PlantContract.PlantEntry.COLUMN_CREATION_TIME);
        int waterTimeIndex = mCursor.getColumnIndex(PlantContract.PlantEntry.COLUMN_LAST_WATERED_TIME);
        int plantTypeIndex = mCursor.getColumnIndex(PlantContract.PlantEntry.COLUMN_PLANT_TYPE);

        // Retrieve column values from indexes
        long plantId = mCursor.getLong(idIndex);
        int plantType = mCursor.getInt(plantTypeIndex);
        long createdAt = mCursor.getLong(createTimeIndex);
        long wateredAt = mCursor.getLong(waterTimeIndex);
        long timeNow = System.currentTimeMillis();

        // Create the remote views object to return

        RemoteViews views = new RemoteViews(mContext.getPackageName(), R.layout.plant_widget_provider);

        // Update the plant image
        int imgRes = PlantUtils.getPlantImageRes(mContext, timeNow-createdAt, timeNow-wateredAt, plantType);

        views.setImageViewResource(R.id.widget_plant_image,imgRes);             // set the plan image
        views.setTextViewText(R.id.widget_plant_name,String.valueOf(plantId));  // set the plants ID

        views.setViewVisibility(R.id.widget_water_button, View.GONE);           // Hide the watering button

        // Fill in onClickPendingItent Template

        Bundle extras = new Bundle();
        extras.putLong(PlantDetailActivity.EXTRA_PLANT_ID, plantId);

        Intent fillInIntent = new Intent();
        fillInIntent.putExtras(extras);

        // Set the OnClick FillIn Intent

        views.setOnClickFillInIntent(R.id.widget_plant_image, fillInIntent);

        // Return the RemoteViews object

        return views;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }
}
