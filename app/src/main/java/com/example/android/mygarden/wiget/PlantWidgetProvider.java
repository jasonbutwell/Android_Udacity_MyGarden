package com.example.android.mygarden.wiget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.example.android.mygarden.R;
import com.example.android.mygarden.provider.PlantContract;
import com.example.android.mygarden.service.GridWidgetService;
import com.example.android.mygarden.service.PlantWateringService;
import com.example.android.mygarden.ui.MainActivity;
import com.example.android.mygarden.ui.PlantDetailActivity;

/**
 * Implementation of App Widget functionality.
 */
public class PlantWidgetProvider extends AppWidgetProvider {

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int imgRes, long plantId, boolean canWater, int appWidgetId) {
        
        // Get current width to decide on single plant vs garden grid view
        Bundle options = appWidgetManager.getAppWidgetOptions(appWidgetId);
        int width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
        
        RemoteViews views;
        
        if ( width < 300 )
            views = getSinglePlantRemoteView(context, imgRes, plantId, canWater);
        else
            views = getGardenGridRemoteView(context);
        
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    private static RemoteViews getSinglePlantRemoteView(Context context, int imgRes, long plantId, boolean canWater) {

        Intent intent;

        // If ID is invalid then just open the main activity
        if ( plantId == PlantContract.INVALID_PLANT_ID) {
            intent = new Intent(context, MainActivity.class);
        } else {
            Log.d(PlantWidgetProvider.class.getSimpleName(),"PlantID="+plantId);
            intent = new Intent(context, PlantDetailActivity.class);
            intent.putExtra(PlantDetailActivity.EXTRA_PLANT_ID, plantId);
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.plant_widget_provider);      // Construct the RemoteViews object

        views.setImageViewResource(R.id.widget_plant_image, imgRes);                                        // update image

        views.setTextViewText(R.id.widget_plant_name, String.valueOf(plantId));                             // set the plant name

        // Set the visibility of the water button accordingly

        if ( canWater )
            views.setViewVisibility(R.id.widget_water_button, View.VISIBLE);
        else
            views.setViewVisibility(R.id.widget_water_button, View.INVISIBLE);

        // Widgets allow click handlers to only launch pending intents

        views.setOnClickPendingIntent(R.id.widget_plant_image, pendingIntent);

        // Add the wateringservice click handler
        Intent wateringIntent = new Intent(context, PlantWateringService.class);
        wateringIntent.setAction(PlantWateringService.ACTION_WATER_PLANT);
        wateringIntent.putExtra(PlantWateringService.EXTRA_PLANT_ID, plantId);

        PendingIntent wateringPendingIntent = PendingIntent.getService(
                context,
                0,
                wateringIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        views.setOnClickPendingIntent(R.id.widget_water_button, wateringPendingIntent);

        return views;
    }

    private static RemoteViews getGardenGridRemoteView(Context context) {

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_grid_view);

        // Set the GridWidgetService intent to act as the adapter for the GridView

        Intent intent = new Intent(context, GridWidgetService.class);

        views.setRemoteAdapter(R.id.widget_grid_view, intent);

        // Set the PlantDetailActivity to launch when clicked

        Intent appIntent = new Intent(context, PlantDetailActivity.class);

        PendingIntent appPendingIntent = PendingIntent.getActivity(context, 0, appIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        views.setPendingIntentTemplate(R.id.widget_grid_view, appPendingIntent);

        // Handle empty gardens

        views.setEmptyView(R.id.widget_grid_view, R.id.empty_view);

        return views;
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        PlantWateringService.startActionUpdatePlantWidgets(context);
    }

    // This is a static method - Which means we can call it from anywhere and pass parameters to it

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public static void updatePlantWidgets(Context context, AppWidgetManager appWidgetManager, int imgRes, long plantId, boolean canWater, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds ) {
            updateAppWidget( context, appWidgetManager, imgRes, plantId, canWater, appWidgetId );
        }
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        PlantWateringService.startActionUpdatePlantWidgets(context);
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        // Perform any action when one or more AppWidget instances have been deleted
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }
}
