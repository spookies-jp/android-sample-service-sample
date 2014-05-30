package jp.co.spookies.android.onoue.servicesample;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;

public class ServiceSample extends Service {
    @SuppressWarnings("rawtypes")
    private static final Class[] mStartSignature = new Class[] { int.class, Notification.class };
    @SuppressWarnings("rawtypes")
    private static final Class[] mStopSignature = new Class[] { boolean.class };

    private NotificationManager mNM;
    private Method mStart;
    private Method mStop;
    private Object[] mStartArgs = new Object[2];
    private Object[] mStopArgs = new Object[1];

    /**
     * This is a wrapper around the new start method, using the older APIs if it
     * is not available.
     */
    void startCompat(int id, Notification notification) {
        // If we have the new start API, then use it.
        if (mStart != null) {
            mStartArgs[0] = Integer.valueOf(id);
            mStartArgs[1] = notification;
            try {
                mStart.invoke(this, mStartArgs);
            } catch (InvocationTargetException e) {
                // Should not happen.
                Log.w("ApiDemos", "Unable to invoke start", e);
            } catch (IllegalAccessException e) {
                // Should not happen.
                Log.w("ApiDemos", "Unable to invoke start", e);
            }
            return;
        }

        // Fall back on the old API.
        setForeground(true);
        mNM.notify(id, notification);
    }

    /**
     * This is a wrapper around the new stop method, using the older APIs if it
     * is not available.
     */
    void stopCompat(int id) {
        // If we have the new stop API, then use it.
        if (mStop != null) {
            mStopArgs[0] = Boolean.TRUE;
            try {
                mStop.invoke(this, mStopArgs);
            } catch (InvocationTargetException e) {
                // Should not happen.
                Log.w("ApiDemos", "Unable to invoke stop", e);
            } catch (IllegalAccessException e) {
                // Should not happen.
                Log.w("ApiDemos", "Unable to invoke stop", e);
            }
            return;
        }

        // Fall back on the old API. Note to cancel BEFORE changing the
        // state, since we could be killed at that point.
        mNM.cancel(id);
        setForeground(false);
    }

    @Override
    public void onCreate() {
        mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        try {
            mStart = getClass().getMethod("start", mStartSignature);
            mStop = getClass().getMethod("stop", mStopSignature);
        } catch (NoSuchMethodException e) {
            // Running on an older platform.
            mStart = mStop = null;
        }
    }

    @Override
    public void onDestroy() {
        // Make sure our notification is gone.
        stopCompat(R.string.service_started);
    }

    // This is the old onStart method that will be called on the pre-2.0
    // platform. On 2.0 or later we override onStartCommand() so this
    // method will not be called.
    @Override
    public void onStart(Intent intent, int startId) {
        handleCommand(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handleCommand(intent);
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    void handleCommand(Intent intent) {
        // In this sample, we'll use the same text for the ticker and the
        // expanded notification
        CharSequence text = getText(R.string.service_started);
        // Set the icon, scrolling text and timestamp
        Notification notification = new Notification(R.drawable.stat_sample, text, System.currentTimeMillis());
        // The PendingIntent to launch our activity if the user selects this
        // notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, Controller.class), 0);
        // Set the info for the views that show in the notification panel.
        notification.setLatestEventInfo(this, getText(R.string.service_label), text, contentIntent);
        startCompat(R.string.service_started, notification);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static class Controller extends Activity {
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.main);
            findViewById(R.id.start).setOnClickListener(mStartListener);
            findViewById(R.id.stop).setOnClickListener(mStopListener);
        }

        private OnClickListener mStartListener = new OnClickListener() {
            public void onClick(View v) {
                startService(new Intent(Controller.this, ServiceSample.class));
            }
        };

        private OnClickListener mStopListener = new OnClickListener() {
            public void onClick(View v) {
                stopService(new Intent(Controller.this, ServiceSample.class));
            }
        };
    }
}
