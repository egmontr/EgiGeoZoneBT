/*
* Copyright 2014 - 2015 Egmont R. (egmontr@gmail.com)
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/		

package de.egi.geofence.geozone.bt.gcm;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import de.egi.geofence.geozone.bt.GcmLog;
import de.egi.geofence.geozone.bt.utils.NotificationUtil;

/**
 * This {@code IntentService} does the actual handling of the GCM message.
 * {@code GcmBroadcastReceiver} (a {@code WakefulBroadcastReceiver}) holds a
 * partial wake lock for this service while the service does its work. When the
 * service is finished, it calls {@code completeWakefulIntent()} to release the
 * wake lock.
 */
@SuppressLint("Registered")
public class GcmIntentService extends IntentService {
    // --Commented out by Inspection (17.08.2016 08:24):public static final int NOTIFICATION_ID = 1;
// --Commented out by Inspection START (17.08.2016 08:24):
////    private NotificationManager mNotificationManager;
//    NotificationCompat.Builder builder;
// --Commented out by Inspection STOP (17.08.2016 08:24)

    public GcmIntentService() {
        super("GcmIntentService");
    }
    private static final String TAG = "EgiGeoZone GCM";

    @SuppressWarnings("deprecation")
	@Override
    protected void onHandleIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        // The getMessageType() intent parameter must be the intent you received
        // in your BroadcastReceiver.
        String messageType = gcm.getMessageType(intent);

        if (!extras.isEmpty()) {  // has effect of unparcelling Bundle
            /*
             * Filter messages based on message type. Since it is likely that GCM will be
             * extended in the future with new message types, just ignore any message types you're
             * not interested in, or that you don't recognize.
             */
			if (GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR.equals(messageType)) {
            	systemMessage("Send error: " + extras.toString());
            } else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED.equals(messageType)) {
            	systemMessage("Deleted messages on server: " + extras.toString());
            // If it's a regular GCM message, do some work.
            } else if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
                Log.i(TAG, "Received: " + extras.toString());
				if (!extras.containsKey("type")	|| !extras.containsKey("source")) {
					Log.i(TAG, "received GCM message, but doesn't fit required fields");
					return;
				}
				String type = extras.getString("type");
				
				// normale Benachrichtigungen				
				if ("message".equalsIgnoreCase(type)) {
					handleMessage(extras);
				} else if ("notify".equalsIgnoreCase(type) || (type == null || type.trim().equals(""))) {
					// Spezielle FHEM-Benachrichtigungen über Geräte-Status-Änderungen
					handleNotify(extras);
				} else {
					handleOthers(extras);
				}
            
                // source = gcmsend_fhem
                // from = 1111111
                // type = notify oder message (notify = fhem-notify und message = benachrichtigung 
                // vibrate = false
                // changes = sensor_value:21.9
                // deviceName = thHeizung
                // android.support.content.wakelockid = 5
                // collapse_key = do_not_collapse
                
            }
        }
        // Release the wake lock provided by the WakefulBroadcastReceiver.
        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }

    
    private void systemMessage(String extras) {
		int notifyId = 99;
		Intent openIntent = new Intent(this, GcmLog.class);
		openIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		PendingIntent pendingIntent = PendingIntent.getActivity(this, notifyId,	openIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		NotificationUtil.notify(this, notifyId, pendingIntent,
				"EgiGeoZone GCM message",
				extras,
				"EgiGeoZone GCM message", 
				true, true, de.egi.geofence.geozone.bt.R.drawable.cloud_email);
	}

    
	private void handleMessage(Bundle extras) {
		int notifyId = 100;
		try {
			if (extras.containsKey("notifyId")) {
				notifyId = Integer.valueOf(extras.getString("notifyId"));
			}
		} catch (Exception e) {
			Log.e(TAG, "invalid notify id: " + extras.getString("notifyId"));
		}
		Intent openIntent = new Intent(this, GcmLog.class);
		openIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		PendingIntent pendingIntent = PendingIntent.getActivity(this, notifyId,	openIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		NotificationUtil.notify(this, notifyId, pendingIntent,
				extras.getString("contentTitle"),
				extras.getString("contentText"),
				extras.getString("tickerText"), shouldVibrate(extras), shouldPlaySound(extras), de.egi.geofence.geozone.bt.R.drawable.cloud_email);
	}

	private boolean shouldVibrate(Bundle extras) {
		return extras.containsKey("vibrate") && "true".equalsIgnoreCase(extras.getString("vibrate"));
	}

	private boolean shouldPlaySound(Bundle extras) {
		return extras.containsKey("playSound") && "true".equalsIgnoreCase(extras.getString("playSound"));
	}

	
	private void handleNotify(Bundle extras) {
		int notifyId = 101;
		Intent openIntent = new Intent(this, GcmLog.class);
		openIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		PendingIntent pendingIntent = PendingIntent.getActivity(this, notifyId,	openIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		NotificationUtil.notify(this, notifyId, pendingIntent,
				"EgiGeoZone GCM notify message",
				"From " + extras.getString("source") + "(" + extras.getString("from") + ")\n" + extras.getString("deviceName") + "-->" + extras.getString("changes"),
				extras.getString("deviceName") + "-->" + extras.getString("changes"), 
				shouldVibrate(extras), shouldPlaySound(extras), de.egi.geofence.geozone.bt.R.drawable.cloud_email);
	}

	private void handleOthers(Bundle extras) {
		int notifyId = 102;
		Intent openIntent = new Intent(this, GcmLog.class);
		openIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		PendingIntent pendingIntent = PendingIntent.getActivity(this, notifyId,	openIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		NotificationUtil.notify(this, notifyId, pendingIntent,
				"EgiGeoZone other GCM message",
				extras.toString(), "", 
				shouldVibrate(extras), shouldPlaySound(extras), de.egi.geofence.geozone.bt.R.drawable.cloud_email);
	}

}
