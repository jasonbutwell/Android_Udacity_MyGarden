package com.example.android.mygarden.service;


import android.app.IntentService;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.RequiresApi;

import com.example.android.mygarden.wiget.PlantWidgetProvider;
import com.example.android.mygarden.R;
import com.example.android.mygarden.provider.PlantContract;
import com.example.android.mygarden.utils.PlantUtils;

import static com.example.android.mygarden.provider.PlantContract.BASE_CONTENT_URI;
import static com.example.android.mygarden.provider.PlantContract.PATH_PLANTS;

/**
 * Created by J on 08/07/2017.
 */

// In this exercise we will start by creating an IntentService class called PlantWateringService that extends from IntentService.

public class PlantWateringService extends IntentService {
    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */

    public static final String serviceName = "PlantWateringService";

    // To keep things organized, it’s best to define the actions that the IntentService can handle, we will start by defining our first action as ACTION_WATER_PLANTS

    // Add action to water a single plant only
    public static final String ACTION_WATER_PLANT =
            "com.example.android.mygarden.action.water_plant";

    // Set another action for updating the widgets
    public static final String ACTION_UPDATE_PLANT_WIDGETS =
            "com.example.android.mygarden.action.update_plant_widgets";

    // Set the ID to pass in the plant ID as an extra
    public static final String EXTRA_PLANT_ID = "com.example.android.mygarden.extra.PLANT_ID";

    public PlantWateringService() {
        super(serviceName);
    }

    public static void startActionWaterPlant(Context context, long plantId) {
        Intent intent = new Intent(context, PlantWateringService.class);
        intent.setAction(ACTION_WATER_PLANT);       // Pass in action to water one plant
        intent.putExtra(EXTRA_PLANT_ID, plantId);   // Pass in the ID of the plant as an extra
        context.startService(intent);
    }

    // For updating the plant widgets

    public static void startActionUpdatePlantWidgets(Context context) {
        Intent intent = new Intent(context, PlantWateringService.class);
        intent.setAction(ACTION_UPDATE_PLANT_WIDGETS);
        context.startService(intent);
    }

    // To handle this action we need to override onHandleIntent, where you can extract the action and handle each action type separately

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {

            final String action = intent.getAction();

            if (ACTION_WATER_PLANT.equals(action)) {

                // Gets the id of the plant passed in
                final long plantId = intent.getLongExtra(EXTRA_PLANT_ID, PlantContract.INVALID_PLANT_ID);

                handleActionWaterPlant(plantId);

            } else if (ACTION_UPDATE_PLANT_WIDGETS.equals(action)) {
                handleActionUpdatePlantWidgets();
            }
        }
    }

    // A method to water one plant only

    private void handleActionWaterPlant(long plantId) {

        Uri SINGLE_PLANT_URI = ContentUris.withAppendedId(
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_PLANTS).build(), plantId);

        long timeNow = System.currentTimeMillis();

        ContentValues contentValues = new ContentValues();
        contentValues.put(PlantContract.PlantEntry.COLUMN_LAST_WATERED_TIME, timeNow);

        getContentResolver().update(
                SINGLE_PLANT_URI,
                contentValues,
                PlantContract.PlantEntry.COLUMN_LAST_WATERED_TIME + ">?",
                new String[]{String.valueOf(timeNow - PlantUtils.MAX_AGE_WITHOUT_WATER)});

        startActionUpdatePlantWidgets(this);    // update the widgets after data change
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private void handleActionUpdatePlantWidgets() {

        boolean canWater = false; // Default to hide the water drop button
        long plantId = PlantContract.INVALID_PLANT_ID;

//      Get plant that is most in need of water (close to dying)

        Uri PLANT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_PLANTS).build();
        Cursor cursor = getContentResolver().query(
                PLANT_URI,
                null,
                null,
                null,
                PlantContract.PlantEntry.COLUMN_LAST_WATERED_TIME
        );

        // Extract the plant details
        int imgRes = R.drawable.grass; // Default image in case garden is empty

        // Check that the cursor actually returned something from the query
        if ( cursor != null && cursor.getCount() > 0 ) {
            cursor.moveToFirst();

            // column indexes

            int idIndex = cursor.getColumnIndex(PlantContract.PlantEntry._ID);  // gets the plant ID
            plantId = cursor.getLong(idIndex);

            int createTimeIndex = cursor.getColumnIndex(PlantContract.PlantEntry.COLUMN_CREATION_TIME);
            int waterTimeIndex = cursor.getColumnIndex(PlantContract.PlantEntry.COLUMN_LAST_WATERED_TIME);
            int plantTypeIndex = cursor.getColumnIndex(PlantContract.PlantEntry.COLUMN_PLANT_TYPE);

            // get values from column indexes
            long timeNow = System.currentTimeMillis();
            long wateredAt = cursor.getLong(waterTimeIndex);
            long createdAt = cursor.getLong(createTimeIndex);
            int plantType = cursor.getInt(plantTypeIndex);

            // close the cursor after use
            cursor.close();

            // Can this plant be watered or not?
            canWater = (timeNow - wateredAt) > PlantUtils.MIN_AGE_BETWEEN_WATER &&
                    (timeNow - wateredAt) < PlantUtils.MAX_AGE_WITHOUT_WATER;

            // Get appropriate image resource to be displayed in the widget
            imgRes = PlantUtils.getPlantImageRes(this, timeNow-createdAt, timeNow-wateredAt, plantType);
        }

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        int [] appWidgetIDs = appWidgetManager.getAppWidgetIds(new ComponentName(this, PlantWidgetProvider.class));

        // Trigger data update to handle the GridView widgets and force a data refresh
        // Notifies the GridView that the data has been changed

        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIDs, R.id.widget_grid_view);

        // Now update all widgets   - pass in the plant ID and the canWater boolean
        PlantWidgetProvider.updatePlantWidgets(this, appWidgetManager, imgRes, plantId, canWater, appWidgetIDs);
    }
}
