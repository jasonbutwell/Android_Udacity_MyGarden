package com.example.android.mygarden.service;


import android.app.IntentService;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.Nullable;

import com.example.android.mygarden.PlantWidgetProvider;
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

    public static final String ACTION_WATER_PLANTS =
            "com.example.android.mygarden.action.water_plants";

    // Set another action for updating the widgets

    public static final String ACTION_UPDATE_PLANT_WIDGETS =
            "com.example.android.mygarden.action.update_plant_widgets";

    // Next we will create a static method called startActionWaterPlants that allows explicitly triggering the Service to perform this action,
    // inside simply create an intent that refers to the same class and set the action to ACTION_WATER_PLANTS and call start service

    public static void startActionWaterPlants(Context context) {
        Intent intent = new Intent(context, PlantWateringService.class);
        intent.setAction(ACTION_WATER_PLANTS);
        context.startService(intent);
    }

    // For updating the plant widgets

    public static void startActionUpdatePlantWidgets(Context context) {
        Intent intent = new Intent(context, PlantWateringService.class);
        intent.setAction(ACTION_UPDATE_PLANT_WIDGETS);
        context.startService(intent);
    }

    public PlantWateringService() {
        super(serviceName);
    }

    // To handle this action we need to override onHandleIntent, where you can extract the action and handle each action type separately

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {

            final String action = intent.getAction();

            if (ACTION_WATER_PLANTS.equals(action)) {
                handleActionWaterPlants();
            } else if (ACTION_UPDATE_PLANT_WIDGETS.equals(action)) {
                handleActionUpdatePlantWidgets();
            }
        }
    }

    // Then finally we implement the handleActionWaterPlants method. To water all plants we just run an update query setting the last watered time to now, but only
    // for those plants that are still alive. To check if a plant is still alive, you can compare the last watered time with the time now and if the
    // difference is larger than MAX_AGE_WITHOUT_WATER, then the plant is dead!

    private void handleActionWaterPlants() {
        Uri PLANTS_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_PLANTS).build();
        ContentValues contentValues = new ContentValues();

        long timeNow = System.currentTimeMillis();

        contentValues.put(PlantContract.PlantEntry.COLUMN_LAST_WATERED_TIME, timeNow);

        // Update only plants that are still alive
        getContentResolver().update(
                PLANTS_URI,
                contentValues,
                PlantContract.PlantEntry.COLUMN_LAST_WATERED_TIME+">?",
                new String[]{String.valueOf(timeNow - PlantUtils.MAX_AGE_WITHOUT_WATER)});
    }

    private void handleActionUpdatePlantWidgets() {

//        Get plant that is most in need of water (close to dying)

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

            // Get appropriate image resource to be displayed in the widget
            imgRes = PlantUtils.getPlantImageRes(this, timeNow-createdAt, timeNow-wateredAt, plantType);
        }

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        int [] appWidgetIDs = appWidgetManager.getAppWidgetIds(new ComponentName(this, PlantWidgetProvider.class));

        // Now update all widgets
        PlantWidgetProvider.updatePlantWidgets(this, appWidgetManager, imgRes, appWidgetIDs);

    }
}
