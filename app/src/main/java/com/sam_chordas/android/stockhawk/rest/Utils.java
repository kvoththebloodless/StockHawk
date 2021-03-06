package com.sam_chordas.android.stockhawk.rest;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.service.StockTaskService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by sam_chordas on 10/8/15.
 * Modified by Gourav Acharya
 */
public class Utils {

    public static final int CURSOR_LOADER_ID = 0;
    public static int Notificationid = 1001;
  public static boolean showPercent = true;
    public static boolean deleteStock = false;
    private static String LOG_TAG = Utils.class.getSimpleName();

  public static ArrayList quoteJsonToContentVals(String JSON) throws JSONException,IOException{
    ArrayList<ContentProviderOperation> batchOperations = new ArrayList<>();
    JSONObject jsonObject = null;
    JSONArray resultsArray = null;

      jsonObject = new JSONObject(JSON);

      if (jsonObject == null
              || jsonObject.length() == 0) {
          throw new IOException("length 0");// may look unnecessary, but I want this case to be handled just like the
          // IOException in StockTaskService
          //i.e, Server down
      }
      else{
        jsonObject = jsonObject.getJSONObject("query");
        int count = Integer.parseInt(jsonObject.getString("count"));
        if (count == 1){
          jsonObject = jsonObject.getJSONObject("results")
              .getJSONObject("quote");
          batchOperations.add(buildBatchOperation(jsonObject));
        } else{
          resultsArray = jsonObject.getJSONObject("results").getJSONArray("quote");

          if (resultsArray != null && resultsArray.length() != 0){
            for (int i = 0; i < resultsArray.length(); i++){
              jsonObject = resultsArray.getJSONObject(i);
              batchOperations.add(buildBatchOperation(jsonObject));
            }
          }
        }
      }



    return batchOperations;
  }

  public static String truncateBidPrice(String bidPrice)throws NumberFormatException{
    bidPrice = String.format("%.2f", Float.parseFloat(bidPrice));
    return bidPrice;
  }

  public static String truncateChange(String change, boolean isPercentChange){
    String weight = change.substring(0,1);
    String ampersand = "";
    if (isPercentChange){
      ampersand = change.substring(change.length() - 1, change.length());
      change = change.substring(0, change.length() - 1);
    }
    change = change.substring(1, change.length());
    double round = (double) Math.round(Double.parseDouble(change) * 100) / 100;
    change = String.format("%.2f", round);
    StringBuffer changeBuffer = new StringBuffer(change);
    changeBuffer.insert(0, weight);
    changeBuffer.append(ampersand);
    change = changeBuffer.toString();
    return change;
  }

public static boolean isConnected(Context mContext)
  {

      ConnectivityManager cm = (ConnectivityManager) (mContext.getSystemService(Context.CONNECTIVITY_SERVICE));

      NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
     boolean isconnected = activeNetwork != null &&
              activeNetwork.isConnectedOrConnecting();

      return  isconnected;

  }

    public static int getServerStatus(Context c)
        {
            SharedPreferences sp= PreferenceManager.getDefaultSharedPreferences(c);
            return(sp.getInt(c.getString(R.string.server_status),StockTaskService.Stock_STATUS_UNKNOWN));
        }
  public static ContentProviderOperation buildBatchOperation(JSONObject jsonObject) throws NumberFormatException{
    ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(
        QuoteProvider.Quotes.CONTENT_URI);
    try {
      String change = jsonObject.getString("Change");
      builder.withValue(QuoteColumns.SYMBOL, jsonObject.getString("symbol"));
      builder.withValue(QuoteColumns.BIDPRICE, truncateBidPrice(jsonObject.getString("Bid")));
      builder.withValue(QuoteColumns.PERCENT_CHANGE, truncateChange(
          jsonObject.getString("ChangeinPercent"), true));
      builder.withValue(QuoteColumns.CHANGE, truncateChange(change, false));
      builder.withValue(QuoteColumns.ISCURRENT, 1);
      if (change.charAt(0) == '-'){
        builder.withValue(QuoteColumns.ISUP, 0);
      }else{
        builder.withValue(QuoteColumns.ISUP, 1);
      }
        builder.withValue(QuoteColumns.NAME, jsonObject.getString("Name"));
    } catch (JSONException e){
      e.printStackTrace();
    }

    return builder.build();
  }
}
