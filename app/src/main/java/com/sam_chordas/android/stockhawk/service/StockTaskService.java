package com.sam_chordas.android.stockhawk.service;

import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.annotation.IntDef;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.TaskParams;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.URLEncoder;

/**
 * Created by sam_chordas on 9/30/15.
 * The GCMTask service is primarily for periodic tasks. However, OnRunTask can be called directly
 * and is used for the initialization and adding task as well.
 */
public class StockTaskService extends GcmTaskService {
    public static final int Stock_STATUS_OK = 0;
    public static final int Stock_STATUS_SERVER_DOWN = 1;
    public static final int Stock_STATUS_SERVER_INVALID = 2;
    public static final int Stock_STATUS_UNKNOWN = 3;
    public static final int Stock_INVALID_INPUT = 4;
    private String LOG_TAG = StockTaskService.class.getSimpleName();
    private OkHttpClient client = new OkHttpClient();
    private Context mContext;
    private StringBuilder mStoredSymbols = new StringBuilder();
    private boolean isUpdate;
    public StockTaskService() {
    }


    public StockTaskService(Context context) {
        mContext = context;
    }

    String fetchData(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .build();

        Response response = client.newCall(request).execute();
        if (response.isSuccessful())
            return response.body().string();
        else
            throw new IOException("BAD REQUEST:400");
    }

    @Override
    public int onRunTask(TaskParams params) {


        Cursor initQueryCursor;
        if (mContext == null) {
            mContext = this;
        }
        StringBuilder urlStringBuilder = new StringBuilder();
        try {
            // Base URL for the Yahoo query
            urlStringBuilder.append("https://query.yahooapis.com/v1/public/yql?q=");
            urlStringBuilder.append(URLEncoder.encode("select * from yahoo.finance.quotes where symbol "
                    + "in (", "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        if (params.getTag().equals("init") || params.getTag().equals("periodic") || params.getTag().equals("refresh")) {
            isUpdate = true;
            initQueryCursor = mContext.getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                    new String[]{"Distinct " + QuoteColumns.SYMBOL}, null,
                    null, null);
            if (initQueryCursor.getCount() == 0 || initQueryCursor == null) {
                // Init task. Populates DB with quotes for the symbols seen below
                try {
                    urlStringBuilder.append(
                            URLEncoder.encode("\"YHOO\",\"AAPL\",\"GOOG\",\"MSFT\")", "UTF-8"));
                    /* 1)when it's initializing or simply periodic checking, if it finds that the table is empty it will first create
                    the query according to the default stocks which are yahoo, google etc as given above in URLEncoder.encode("YH00",SDFWF,DF)
                   */
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            } else if (initQueryCursor != null) {

                initQueryCursor.moveToFirst();
                for (int i = 0; i < initQueryCursor.getCount(); i++) {
                    mStoredSymbols.append("\"" +
                            initQueryCursor.getString(initQueryCursor.getColumnIndex("symbol")) + "\",");
                    initQueryCursor.moveToNext();
                    /* but if it finds that the table is not empty it will append the symbols of all the stocks which are present there since
                    they will be those which the user has decided to keep...not necessarily yhoo google etc. (as shown in this for loop)*/
                }
                mStoredSymbols.replace(mStoredSymbols.length() - 1, mStoredSymbols.length(), ")");
                try {
                    urlStringBuilder.append(URLEncoder.encode(mStoredSymbols.toString(), "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        } else if (params.getTag().equals("add")) {
            /* function for when user clicks the floating action button that helps adding stocks
            according to symbols entered*/
            isUpdate = false;
            // get symbol from params.getExtra and build query
            String stockInput = params.getExtras().getString("symbol");
            try {
                urlStringBuilder.append(URLEncoder.encode("\"" + stockInput + "\")", "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        // finalize the URL for the API query.
        urlStringBuilder.append("&format=json&diagnostics=true&env=store%3A%2F%2Fdatatables."
                + "org%2Falltableswithkeys&callback=");

        String urlString;
        String getResponse;
        int result = GcmNetworkManager.RESULT_FAILURE;// initial setting to failure which can change according to how the request goes.

        if (urlStringBuilder != null) {
            urlString = urlStringBuilder.toString();
            try {
                getResponse = fetchData(urlString);
                result = GcmNetworkManager.RESULT_SUCCESS;

                ContentValues contentValues = new ContentValues();
                //update ISCURRENT to 0 (false) so new data is current
                if (isUpdate) {
                    contentValues.put(QuoteColumns.ISCURRENT, 0);
                    /*SETTING THE CURSOR LOADER ON AND OFF WAS REQUIRED HERE SINCE OTHERWISE THE SCREEN USED TO FLICKER
                    * WHILE SETTING OF THE DATA, DISPLAYING THE ERROR MESSAGE BEHIND IT, THE REASON WAS THAT AS SOON AS UPDATE
                    * WAS MADE THE ONLoadFinished() GOT A NULL RESPONSE*/
                    setLoaderStatus("off");
                    mContext.getContentResolver().update(QuoteProvider.Quotes.CONTENT_URI, contentValues,
                            null, null);
                }

                mContext.getContentResolver().applyBatch(QuoteProvider.AUTHORITY,
                        Utils.quoteJsonToContentVals(getResponse));
                setLoaderStatus("on");

                /*G:The annotations come in use here*/
                setStockServerStatus(Stock_STATUS_OK);


            } catch (IOException e) {
                setStockServerStatus(Stock_STATUS_SERVER_DOWN);
                result = GcmNetworkManager.RESULT_FAILURE;

            } catch (JSONException e) {
                setStockServerStatus(Stock_STATUS_SERVER_INVALID);
                result = GcmNetworkManager.RESULT_FAILURE;


            } catch (RemoteException | OperationApplicationException e) {


            } catch (NumberFormatException e) {
                setStockServerStatus(Stock_INVALID_INPUT);
                result = GcmNetworkManager.RESULT_FAILURE;
            }


        }
        return result;
    }

    private void setStockServerStatus(@ServerStatus int stockstatus) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
        SharedPreferences.Editor se = sp.edit();
        se.putInt(mContext.getString(R.string.server_status), stockstatus);
        se.commit();
    }

    private void setLoaderStatus(String toggle) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
        SharedPreferences.Editor se = sp.edit();
        se.putString(mContext.getString(R.string.loaders_switch), toggle);
        se.commit();
    }

    /*ANOOTATIONS FOR HANDLING SERVER BASED ERRORS*/
    @IntDef({Stock_STATUS_OK, Stock_STATUS_SERVER_DOWN, Stock_STATUS_SERVER_INVALID, Stock_STATUS_UNKNOWN, Stock_INVALID_INPUT})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ServerStatus {
    }

}
