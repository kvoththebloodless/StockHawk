package com.sam_chordas.android.stockhawk.receivers;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.NotificationCompat;
import android.widget.Toast;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.sam_chordas.android.stockhawk.service.StockIntentService;
import com.sam_chordas.android.stockhawk.ui.MyStocksActivity;

/**
 * Created by Gourav on 6/14/2016.
 * <p/>
 * A Receiver CLASS FOR REPOPULATING OR UPDATING THE STOCKLIST UPON NETWORK CONNECTED
 */
public class ConnectionChange extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        NotificationCompat.Builder mBuilder =
                (NotificationCompat.Builder) new NotificationCompat.Builder(context)
                        .setSmallIcon(R.drawable.logo)
                        .setContentTitle(context.getString(R.string.notification_network));

        if (!(Utils.isConnected(context))) {
            Toast.makeText(context, context.getString(R.string.toast_offline_Stocks), Toast.LENGTH_LONG).show();
            mBuilder.setContentText(context.getString(R.string.connectivity_lost));
            ((MyStocksActivity) context).getLoaderManager().restartLoader(Utils.CURSOR_LOADER_ID, null, (MyStocksActivity) context);
        } else if (Utils.isConnected(context)) {
            mBuilder.setContentText(context.getString(R.string.connectivity_gained));
            Intent mServiceIntent = new Intent(context, StockIntentService.class);

            mServiceIntent.putExtra("tag", "refresh");
            context.startService(mServiceIntent);
        }
        NotificationManager mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        mNotificationManager.notify(Utils.Notificationid, mBuilder.build());

    }

}


