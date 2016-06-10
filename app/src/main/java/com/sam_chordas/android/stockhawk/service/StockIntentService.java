package com.sam_chordas.android.stockhawk.service;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.TaskParams;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.squareup.okhttp.internal.Util;

/**
 * Created by sam_chordas on 10/1/15.
 */
public class StockIntentService extends IntentService {

  public StockIntentService(){
    super(StockIntentService.class.getName());
  }

  public StockIntentService(String name) {
    super(name);
  }

  @Override protected void onHandleIntent(Intent intent) {
    Log.d(StockIntentService.class.getSimpleName(), "Stock Intent Service");
    StockTaskService stockTaskService = new StockTaskService(this);
    Bundle args = new Bundle();
    if (intent.getStringExtra("tag").equals("add")){
      args.putString("symbol", intent.getStringExtra("symbol"));
    }

    // We can call OnRunTask from the intent service to force it to run immediately instead of
    // scheduling a task.

      int result = stockTaskService.onRunTask(new TaskParams(intent.getStringExtra("tag"), args));
      if (result == GcmNetworkManager.RESULT_FAILURE)// Returning failure only when stock fails to "add" as a visually impaired
          // user should be notified of it
          Toast.makeText(getApplicationContext(), getString(R.string.total_content_description, intent.getStringExtra("symbol"), "was not found", " "), Toast.LENGTH_LONG).show();
  }
}
