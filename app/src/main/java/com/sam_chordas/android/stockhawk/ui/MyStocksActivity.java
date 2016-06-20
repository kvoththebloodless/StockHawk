package com.sam_chordas.android.stockhawk.ui;

import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.InputType;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.PeriodicTask;
import com.google.android.gms.gcm.Task;
import com.melnykov.fab.FloatingActionButton;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.receivers.ConnectionChange;
import com.sam_chordas.android.stockhawk.rest.QuoteCursorAdapter;
import com.sam_chordas.android.stockhawk.rest.RecyclerViewItemClickListener;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.sam_chordas.android.stockhawk.service.StockIntentService;
import com.sam_chordas.android.stockhawk.service.StockTaskService;
import com.sam_chordas.android.stockhawk.touch_helper.SimpleItemTouchHelperCallback;


public class MyStocksActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>, SharedPreferences.OnSharedPreferenceChangeListener {

    boolean isConnected;
    RecyclerView recyclerView;
    TextView mNoStocks;
    Toolbar toolbar;
    RelativeLayout cont;
    BroadcastReceiver receiver;
    LinearLayout mainview;
    Button prestocks;
    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */


    private CharSequence mTitle;
    private Intent mServiceIntent;
    private ItemTouchHelper mItemTouchHelper;
    private QuoteCursorAdapter mCursorAdapter;
    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = this;

        isConnected = Utils.isConnected(getApplicationContext());
        setContentView(R.layout.activity_my_stocks);
        mainview = (LinearLayout) findViewById(R.id.mainview);
        toolbar = (Toolbar) findViewById(R.id.tooly);
        cont = (RelativeLayout) findViewById(R.id.hiddenpage);
        prestocks = (Button) findViewById(R.id.prestocks);

        setSupportActionBar(toolbar);


        mNoStocks = (TextView) findViewById(R.id.nostocks);
        // The intent service is for executing immediate pulls from the Yahoo API
        // GCMTaskService can only schedule tasks, they cannot execute immediately
        mServiceIntent = new Intent(this, StockIntentService.class);

        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        getLoaderManager().initLoader(Utils.CURSOR_LOADER_ID, null, this);

        mCursorAdapter = new QuoteCursorAdapter(this, null);
        recyclerView.addOnItemTouchListener(new RecyclerViewItemClickListener(this,
                new RecyclerViewItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View v, int position) {
                        String symbol = ((TextView) (v.findViewById(R.id.stock_symbol))).getText().toString();
                        if (Utils.deleteStock) {/*LOGIC FOR THE TOOLBAR DELETE BUTTON*/
                            mContext.getContentResolver().delete(QuoteProvider.Quotes.CONTENT_URI, QuoteColumns.SYMBOL + " = '" + symbol
                                    + "'", null);
                            Utils.deleteStock = false;
                            recyclerView.setFocusable(false);
                            Toast.makeText(getApplicationContext(), getString(R.string.toast_stock_removed) + symbol, Toast.LENGTH_SHORT).show();
                        } else {
                            Intent in = new Intent(getApplicationContext(), StockGraphActivity.class);
                            in.putExtra("Stockname", ((QuoteCursorAdapter.ViewHolder) recyclerView.getChildViewHolder(v)).name); //required for the next activity
                            in.putExtra("Quote", ((QuoteCursorAdapter.ViewHolder) recyclerView.getChildViewHolder(v)).quote);

                            startActivity(in);
                        }
                    }
                }));
        recyclerView.setAdapter(mCursorAdapter);
        /*THE BUTTON WHICH RETURNS BACK THE LOCAL DATA AFTER THE ERROR MESSAGES ARE DISPLAYED*/
        prestocks.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Cursor c = callContent();
                if (c != null && c.getCount() != 0) {
                    cont.setVisibility(View.GONE);
                    mCursorAdapter.swapCursor(callContent());

                } else {
                    Toast.makeText(getApplicationContext(), R.string.no_offline_Stocks, Toast.LENGTH_SHORT).show();
                }
            }
        });


        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.attachToRecyclerView(recyclerView);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Utils.isConnected(getApplicationContext())) {
                    new MaterialDialog.Builder(mContext).title(R.string.symbol_search)
                            .content(R.string.content_test)
                            .inputType(InputType.TYPE_CLASS_TEXT)
                            .input(R.string.input_hint, R.string.input_prefill, new MaterialDialog.InputCallback() {
                                @Override
                                public void onInput(MaterialDialog dialog, CharSequence input) {
                                    // On FAB click, receive user input. Make sure the stock doesn't already exist
                                    // in the DB and proceed accordingly
                                    if (input.toString().trim().equals("")) {/*IF THIS EMPTY INPUT CONDITION IS NOT HANDLED THEN THE ERROR MESSAGE SET IS THE SERVER_INVALID
                                         INSTEAD OF INPUT_INVALID*/
                                        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                                        SharedPreferences.Editor se = sp.edit();
                                        se.putInt(mContext.getString(R.string.server_status), StockTaskService.Stock_INVALID_INPUT);
                                        se.apply();
                                        return;
                                    }

                                    Cursor c = getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                                            new String[]{QuoteColumns.SYMBOL}, QuoteColumns.SYMBOL + "= ?",
                                            new String[]{input.toString()}, null);
                                    if (c.getCount() != 0) {
                                        Toast toast =
                                                Toast.makeText(MyStocksActivity.this, getString(R.string.preexistingstock),
                                                        Toast.LENGTH_LONG);
                                        toast.setGravity(Gravity.CENTER, Gravity.CENTER, 0);
                                        toast.show();

                                    } else {
                                        // Add the stock to DB
                                        mServiceIntent.putExtra("tag", "add");
                                        mServiceIntent.putExtra("symbol", input.toString());
                                        startService(mServiceIntent);

                                    }
                                }

                            })
                            .show();
                } else {
                    networkToast();
                }

            }
        });

        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(mCursorAdapter);
        mItemTouchHelper = new ItemTouchHelper(callback);
        mItemTouchHelper.attachToRecyclerView(recyclerView);

        mTitle = getTitle();
        if (isConnected) {
            long period = 36000L;
            long flex = 10L;
            String periodicTag = "periodic";

            // create a periodic task to pull stocks once every hour after the app has been opened. This
            // is so Widget data stays up to date.
            PeriodicTask periodicTask = new PeriodicTask.Builder()
                    .setService(StockTaskService.class)
                    .setPeriod(period)
                    .setFlex(flex)
                    .setTag(periodicTag)
                    .setRequiredNetwork(Task.NETWORK_STATE_CONNECTED)
                    .setRequiresCharging(false)
                    .build();
            // Schedule task with tag "periodic." This ensure that only the stocks present in the DB
            // are updated.
            GcmNetworkManager.getInstance(this).schedule(periodicTask);
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        sp.registerOnSharedPreferenceChangeListener(this);
        getLoaderManager().restartLoader(Utils.CURSOR_LOADER_ID, null, this);
        /*REGISTERING THE RECEIVER FOR THE CONNECTIVITY CHANGE BROADCAST*/
        IntentFilter filter = new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE");
        receiver = new ConnectionChange();
        registerReceiver(receiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        sp.unregisterOnSharedPreferenceChangeListener(this);
        getLoaderManager().destroyLoader(Utils.CURSOR_LOADER_ID);
        /*UNREGISTERING THE CONNECTIVITY CHANGE BROADCAST*/
        unregisterReceiver(receiver);
    }

    public void networkToast() {
        Toast.makeText(mContext, getString(R.string.network_toast), Toast.LENGTH_SHORT).show();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.my_stocks, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        if (id == R.id.delete && mCursorAdapter != null) {
            Utils.deleteStock = true;
            Toast.makeText(getApplicationContext(), getString(R.string.toast_select_stock2_delete), Toast.LENGTH_SHORT).show();
        }
        if (id == R.id.menu_refresh) {
            refresh();
        }
        if (id == R.id.action_change_units) {
            // this is for changing stock changes from percent value to dollar value
            if (Utils.showPercent) {
                item.setTitle(getString(R.string.percentrep));
                item.setIcon(R.drawable.ic_percent_white_24dp);

            } else {
                item.setTitle(getString(R.string.dollarrep));
                item.setIcon(R.drawable.ic_attach_money_white_24dp);
            }
            Utils.showPercent = !Utils.showPercent;
            this.getContentResolver().notifyChange(QuoteProvider.Quotes.CONTENT_URI, null);
        }

        return super.onOptionsItemSelected(item);
    }

    private void refresh() {
        if (Utils.isConnected(getApplicationContext())) {

            mServiceIntent.putExtra("tag", "refresh");
            startService(mServiceIntent);
        } else {
            Toast.makeText(getApplicationContext(), getString(R.string.internet_off), Toast.LENGTH_LONG).show();
        }
    }

    public void updateErrorView() {
        /*THE ERROR CODES AND THEIR HANDLING*/

        switch (Utils.getServerStatus(getApplicationContext())) {
            case StockTaskService.Stock_STATUS_SERVER_DOWN:
                mNoStocks.setText(getString(R.string.server_down));
                cont.setVisibility(View.VISIBLE);
                break;
            case StockTaskService.Stock_STATUS_SERVER_INVALID:
                mNoStocks.setText(getString(R.string.server_invalid));
                cont.setVisibility(View.VISIBLE);
                break;
            case StockTaskService.Stock_STATUS_OK:
                cont.setVisibility(View.GONE);
                break;
            case StockTaskService.Stock_STATUS_UNKNOWN:
                cont.setVisibility(View.VISIBLE);
                mNoStocks.setText(getString(R.string.server_unknown));
                break;
            case StockTaskService.Stock_INVALID_INPUT:
                cont.setVisibility(View.VISIBLE);
                mNoStocks.setText(getString(R.string.server_invalid_input));
                break;


            default:
                cont.setVisibility(View.VISIBLE);
                mNoStocks.setText(getString(R.string.empty_db_msg));

        }

        resetSharedPref();
    }


    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        // This narrows the return to only the stocks that are most current.
        return new CursorLoader(this, QuoteProvider.Quotes.CONTENT_URI,
                new String[]{QuoteColumns._ID, QuoteColumns.SYMBOL, QuoteColumns.BIDPRICE,
                        QuoteColumns.PERCENT_CHANGE, QuoteColumns.CHANGE, QuoteColumns.ISUP, QuoteColumns.NAME},
                QuoteColumns.ISCURRENT + " = 1 ",
                null,
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data != null && data.getCount() != 0) {
            mCursorAdapter.swapCursor(data);

            cont.setVisibility(View.GONE);
        } else {

            if (!Utils.isConnected(getApplicationContext()))
                mNoStocks.setText(getString(R.string.internet_off));
            else {
                mNoStocks.setText(getString(R.string.empty_db_msg));
                if (Utils.getServerStatus(getApplicationContext()) != StockTaskService.Stock_STATUS_OK)
                    updateErrorView();
            }
            cont.setVisibility(View.VISIBLE);

        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mCursorAdapter.swapCursor(null);
    }

    private void resetSharedPref() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        sp.unregisterOnSharedPreferenceChangeListener(this);
        SharedPreferences.Editor se = sp.edit();
        se.putInt(mContext.getString(R.string.server_status), StockTaskService.Stock_STATUS_OK);
        se.apply();
        sp.registerOnSharedPreferenceChangeListener(this);

    }

    public Cursor callContent() {/*USED BY THE RETURN BUTTON*/
        return getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                new String[]{QuoteColumns._ID, QuoteColumns.SYMBOL, QuoteColumns.BIDPRICE,
                        QuoteColumns.PERCENT_CHANGE, QuoteColumns.CHANGE, QuoteColumns.ISUP, QuoteColumns.NAME},
                QuoteColumns.ISCURRENT + " = 1 ",
                null,
                null);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        if (key.equals(getString(R.string.server_status)))
            updateErrorView();

        if (key.equals(getString(R.string.loaders_switch))) {
            if (sharedPreferences.getString(getString(R.string.loaders_switch), "null").equals("on"))
                getLoaderManager().restartLoader(Utils.CURSOR_LOADER_ID, null, this);


            else
                getLoaderManager().destroyLoader(Utils.CURSOR_LOADER_ID);

        }

    }
}
