package com.zero211.dkyphonelistener;

import android.Manifest;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.support.annotation.DrawableRes;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.PhoneStateListener;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DKYPhoneListenerService extends Service
{
    private static final String LOGTAG = DKYPhoneListenerService.class.getCanonicalName();

    private static final String SEP_ITEM = ",";
    private static final String SEP_TUPLE = ":";

    public static final String CONTACT_ID_PREFIX = "ID#";

    public static final String ACTION_SHUTDOWN = "com.zero211.dky.SHUTDOWN";
    public static final String ACTION_STOP_SERVICE = "com.zero211.dky.START_SERVICE";
    public static final String ACTION_START_SERVICE = "com.zero211.dky.STOP_SERVICE";

    public static final String NOTIF_CHANNEL_ID_CONTROL = "Control";
    public static final String NOTIF_CHANNEL_NAME_CONTROL = "Control (persistent)";
    public static final String NOTIF_CHANNEL_DESCR_CONTROL = "Includes UI fast launch and Exit";

    private static final int NOTIF_ID_FOR_APP = 77777;

    public static final String EXTRA_ACCENT_COLOR = "AccentColor";
    public static final String EXTRA_SETTINGS_PACKAGE_PATH = "SettingsPackagePath";
    public static final String EXTRA_SETTINGS_CLASS_NAME = "SettingsClassName";
    public static final String EXTRA_SETTINGS_ACTION = "SettingsAction";

//    public static final String EXTRA_STOPME_PACKAGE_PATH = "StopMePackagePath";
//    public static final String EXTRA_STOPME_CLASS_NAME = "StopMeClassName";
//    public static final String EXTRA_STOPME_ACTION = "StopMeAction";

    public static final String DESCR_CALL_STATE_RINGING = "CALL_STATE_RINGING";
    public static final String DESCR_CALL_STATE_OFFHOOK = "CALL_STATE_OFFHOOK";
    public static final String DESCR_CALL_STATE_IDLE = "CALL_STATE_IDLE";
    public static final String DESCR_CALL_STATE_UNKNOWN = "CALL_STATE_IDLE";

    public static final String MODE_DND = "DND";
    public static final String MODE_BLOCK_ALL = "BLOCK_ALL";
    public static final String MODE_ALLOW_ALL = "ALLOW_ALL";

    public static final String PREF_MODE = "MODE";
    public static final String PREF_MODE_DEFAULT = MODE_BLOCK_ALL;

    public static final String PREF_DND_SCHEDULE_ONOFF = "DND_SCHEDULE_ONOFF";
    public static final boolean PREF_DND_SCHEDULE_ONOFF_DEFAULT = false;

    public static final String PREF_DND_SCHEDULE = "DND_SCHEDULE";
    public static final String PREF_DND_SCHEDULE_DEFAULT = "";

    public static final String PREF_DND_PRIOR_MODE = "DND_PRIOR_MODE";
    public static final String PREF_DND_PRIOR_MODE_DEFAULT = PREF_MODE_DEFAULT;

    public static final String PREF_PROCESSED_CALL_LOG = "PROCESSED_CALL_LOG";
    public static final String PREF_PROCESSED_CALL_LOG_DEFAULT = "";

    public static final String PREF_PROCESSED_CALL_LOG_MAX_SIZE = "PROCESSED_CALL_LOG_MAX_SIZE";
    public static final int PREF_PROCESSED_CALL_LOG_MAX_SIZE_DEFAULT = 100;

    public static final String PREF_DND_EXCEPTIONS = "DND_EXCEPTIONS";
    public static final String PREF_DND_EXCEPTIONS_DEFAULT = "";

    public static final String PREF_DND_EXCEPT_PERSISTENTS = "DND_EXCEPT_PERSISTENTS";
    public static final boolean PREF_DND_EXCEPT_PERSISTENTS_DEFAULT = true;

    public static final String PREF_DND_PERSISTENT_TIMES = "PERSISTENT_TIMES";
    public static final int PREF_DND_PERSISTENT_TIMES_DEFAULT = 3;

    public static final String PREF_DND_PERSISTENT_MINUTES = "PERSISTENT_MINUTES";
    public static final int PREF_DND_PERSISTENT_MINUTES_DEFAULT = 10;

    public static final String PREF_DND_SMS_BLOCK_SEND = "DND_SMS_BLOCK_SEND";
    public static final boolean PREF_DND_SMS_BLOCK_SEND_DEFAULT = false;

    public static final String PREF_DND_SMS_BLOCK_MSG = "DND_SMS_BLOCK_MSG";
    public static final String PREF_DND_SMS_BLOCK_MSG_DEFAULT = "This is an auto-response to your phone call.  Your call was prevented from ringing thru because your number is not in my ring-thru list.  If you did not leave a voicemail, or respond to this text, I will not respond back to your call.";

    public static final String PREF_BLOCK_ALL_EXCEPTIONS = "BLOCK_ALL_EXCEPTIONS";
    public static final String PREF_BLOCK_ALL_EXCEPTIONS_DEFAULT = "";

    public static final String PREF_BLOCK_ALL_EXCEPT_CONTACTS = "BLOCK_ALL_EXCEPT_CONTACTS";
    public static final boolean PREF_BLOCK_ALL_EXCEPT_CONTACTS_DEFAULT = true;

    public static final String PREF_BLOCK_ALL_EXCEPT_OUTGOING = "BLOCK_ALL_EXCEPT_OUTGOING";
    public static final boolean PREF_BLOCK_ALL_EXCEPT_OUTGOING_DEFAULT = true;

    public static final String PREF_BLOCK_ALL_SMS_BLOCK_SEND = "BLOCK_ALL_SMS_BLOCK_SEND";
    public static final boolean PREF_BLOCK_ALL_SMS_BLOCK_SEND_DEFAULT = false;

    public static final String PREF_BLOCK_ALL_SMS_BLOCK_MSG = "BLOCK_ALL_SMS_BLOCK_MSG";
    public static final String PREF_BLOCK_ALL_SMS_BLOCK_MSG_DEFAULT = "This is an auto-response to your phone call.  Your call was prevented from ringing thru because your number is not in my ring-thru list.  If you did not leave a voicemail, or respond to this text, I will not respond back to your call.";

    public static final String PREF_ALLOW_ALL_EXCEPTIONS = "ALLOW_ALL_EXCEPTIONS";
    public static final String PREF_ALLOW_ALL_EXCEPTIONS_DEFAULT = "";

    public static final String PREF_ALLOW_ALL_SMS_BLOCK_SEND = "ALLOW_ALL_SMS_BLOCK_SEND";
    public static final boolean PREF_ALLOW_ALL_SMS_BLOCK_SEND_DEFAULT = false;

    public static final String PREF_ALLOW_ALL_SMS_BLOCK_MSG = "ALLOW_ALL_SMS_BLOCK_MSG";
    public static final String PREF_ALLOW_ALL_SMS_BLOCK_MSG_DEFAULT = "This is an auto-response to your phone call.  Your call was prevented from ringing thru because your are on my blocked list.  If you did not leave a voicemail, or respond to this text, I will not respond back to your call.";

    private static ArrayList<DKYPhoneEventListener> listeners  = new ArrayList<DKYPhoneEventListener>();

    private SharedPreferences prefs;

    private DKYStateListener dkyStateListener;
    private TelephonyManager telManager;
    private DKYCallControl callControl;
    private DKYPhoneListenerShutdownReceiver shutdownReceiver;

    private String settingsPackagePath;
    private String settingsClassName;

//    private String stopmePackagePath;
//    private String stopmeClassName;


    public static void firePhoneEvent(DKYPhoneEvent event)
    {
        for (DKYPhoneEventListener listener : listeners)
        {
            listener.phoneEventProcessed(event);
        }
    }

    public static boolean addPhoneEventListener(DKYPhoneEventListener listener)
    {
        return listeners.add(listener);
    }

    public static boolean removePhoneEventListener(DKYPhoneEventListener listener)
    {
        return listeners.remove(listener);
    }


    public static Bitmap getBitmapFromDrawableResource(Context context, @DrawableRes int drawableId, int accentColor)
    {
        Drawable drawable = ContextCompat.getDrawable(context, drawableId);
        ColorFilter colorFilter = new LightingColorFilter(Color.BLACK, accentColor);
        drawable.setColorFilter(colorFilter);

        if (drawable instanceof BitmapDrawable)
        {
            return ((BitmapDrawable) drawable).getBitmap();
        }
        //else if (drawable instanceof VectorDrawableCompat || drawable instanceof VectorDrawable)
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            if (drawable instanceof VectorDrawable)
            {
                Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                drawable.draw(canvas);

                return bitmap;
            }
            else
            {
                throw new IllegalArgumentException("unsupported drawable type");
            }
        }
        else
        {
            throw new IllegalArgumentException("unsupported drawable type");
        }
    }

    public static String getStringFromTuples(List<List<String>> tuples)
    {
        StringBuilder sb = new StringBuilder();
        for (List<String> tuple : tuples)
        {
            if (sb.length() != 0)
            {
                sb.append(SEP_TUPLE);
            }

            String rawTupleStr = getStringFromTuple(tuple);
            sb.append(rawTupleStr);
        }

        return sb.toString();
    }

    public static String getStringFromTuple(List<String> tuple)
    {
        StringBuilder sb = new StringBuilder();
        for (String item : tuple)
        {
            if (sb.length() != 0)
            {
                sb.append(SEP_ITEM);
            }

            sb.append(item);
        }

        String tupleStr = sb.toString();

        return tupleStr;
    }

    public static List<List<String>> getTuplesFromString(String csv)
    {
        if (csv == null)
        {
            return null;
        }

        List<List<String>> tuples = new ArrayList<>();
        String[] rawTuples = csv.split(SEP_TUPLE);

        for (String rawTuple : rawTuples)
        {
            String[] tupleArr = rawTuple.split(SEP_ITEM);

            List<String> tuple = Arrays.asList(tupleArr);
            tuples.add(tuple);
        }

        return tuples;
    }

    public boolean isServiceRunning(String serviceClassName)
    {
        final ActivityManager activityManager = (ActivityManager) this.getSystemService(Context.ACTIVITY_SERVICE);
        final List<ActivityManager.RunningServiceInfo> services = activityManager.getRunningServices(Integer.MAX_VALUE);

        for (ActivityManager.RunningServiceInfo runningServiceInfo : services)
        {
            if (runningServiceInfo.service.getClassName().equals(serviceClassName))
            {
                return true;
            }
        }
        return false;
    }

    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Log.d(LOGTAG, "In onStartCommand");

        int accentColor = intent.getIntExtra(EXTRA_ACCENT_COLOR, 0xFFFF0000);
        Log.d(LOGTAG, "Accent color is: '" + accentColor + "'");

        settingsPackagePath = intent.getStringExtra(EXTRA_SETTINGS_PACKAGE_PATH);
        Log.d(LOGTAG, "settingsPackagePath is: '" + settingsPackagePath + "'");

        settingsClassName = intent.getStringExtra(EXTRA_SETTINGS_CLASS_NAME);
        Log.d(LOGTAG, "settingsClassName is: '" + settingsClassName + "'");

        Log.d(LOGTAG, "Getting the application context");
        Context appContext = this.getApplicationContext();

        Log.d(LOGTAG, "Getting the sharedPreferences");
        prefs = PreferenceManager.getDefaultSharedPreferences(appContext);

        Log.d(LOGTAG, "Creating the callControl Object");
        callControl = new DKYCallControl(this);

        Log.d(LOGTAG, "Creating the shutdown receiver");
        shutdownReceiver = new DKYPhoneListenerShutdownReceiver(this);

        Log.d(LOGTAG, "Registering the shutdown receiver");
        IntentFilter shutdownIntentFilter = new IntentFilter(DKYPhoneListenerService.ACTION_SHUTDOWN);
        this.registerReceiver(shutdownReceiver, shutdownIntentFilter);

//        stopmePackagePath = intent.getStringExtra(EXTRA_STOPME_PACKAGE_PATH);
//        stopmeClassName = intent.getStringExtra(EXTRA_STOPME_CLASS_NAME);

        // Setup the telService var to allow for call control
        Log.d(LOGTAG, "Getting the TelephonyManager");
        telManager = (TelephonyManager) (getSystemService(TELEPHONY_SERVICE));

        Log.d(LOGTAG, "Creating the PhoneStateListener");
        dkyStateListener = new DKYStateListener();

        Log.d(LOGTAG, "Applying the PhoneStateListener via the TelephonyManager");
        telManager.listen(dkyStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        Log.d(LOGTAG, "DKYStateListener listening");

        int requestCode = 1;

        Intent settingsIntent = new Intent().setClassName(settingsPackagePath, settingsClassName).putExtra(EXTRA_SETTINGS_ACTION, true);
        PendingIntent settingsPendingIntent = PendingIntent.getActivity(this, requestCode, settingsIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Action settingsAction = new NotificationCompat.Action.Builder(R.drawable.ic_settings_32dp, getString(R.string.action_settings), settingsPendingIntent).build();

        Intent exitIntent = new Intent(DKYPhoneListenerService.ACTION_SHUTDOWN);
        PendingIntent exitPendingIntent = PendingIntent.getBroadcast(this, requestCode, exitIntent, PendingIntent.FLAG_ONE_SHOT);
        NotificationCompat.Action exitAction = new NotificationCompat.Action.Builder(R.drawable.ic_exit_32dp, getString(R.string.action_exit), exitPendingIntent).build();

        PendingIntent notifPendingIntent = settingsPendingIntent;


        String NL = System.lineSeparator();
        boolean notifSticky = true;
        boolean notifLongRunning = true;
        String notifTitle = this.getString(R.string.app_name);
        String notifShort = this.getString(R.string.filtering_inbound_calls);
        String notifLong = this.getString(R.string.more_options);

        Log.d(LOGTAG, "Creating the sticky DKY notification");
        Notification notif = getNotification(accentColor, false, notifPendingIntent, notifSticky, notifLongRunning, notifTitle, notifShort, notifLong, exitAction);

        Log.d(LOGTAG, "Calling DKYModeAlarmReceiver.setNextModeAlarm from DKYPhoneListenerService.onStartCommand()");
        DKYModeAlarmReceiver alarmReceiver = new DKYModeAlarmReceiver();
        alarmReceiver.setNextModeAlarm(this);

        Log.d(LOGTAG, "Restarting as a foreground service with the sticky notification");
        this.startForeground(NOTIF_ID_FOR_APP, notif);
        return START_STICKY;
    }

    public void onDestroy()
    {
        this.unregisterReceiver(shutdownReceiver);

        NotificationManager notificationManager = (NotificationManager) (getSystemService(NOTIFICATION_SERVICE));
        notificationManager.cancelAll();

        DKYModeAlarmReceiver alarmReceiver = new DKYModeAlarmReceiver();
        alarmReceiver.cancelModeAlarms(this);
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException(this.getString(R.string.not_yet));
    }


//    private void askToStopAllOfMe()
//    {
//        final String skipPrefName = "exit_confirm_skipper";
//
//        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
//        boolean skipIt = settings.getBoolean(skipPrefName, false);
//
//        if (!skipIt)
//        {
//            AlertDialog.Builder adb = new AlertDialog.Builder(this);
//            LayoutInflater adbInflater = LayoutInflater.from(this);
//            final View checkBoxView = adbInflater.inflate(R.layout.dontaskagain, null);
//            adb.setTitle(this.getString(R.string.exit_app));
//            adb.setMessage(this.getString(R.string.are_you_sure_exit));
//            adb.setView(checkBoxView);
//
//            adb.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener()
//            {
//                @Override
//                public void onClick(DialogInterface dialog, int which)
//                {
//                    CheckBox checkBox = (CheckBox) checkBoxView.findViewById(R.id.skip);
//                    settings.edit().putBoolean(skipPrefName, checkBox.isChecked()).commit();
//                    stopAllOfMe();
//                }
//            });
//
//            adb.setNegativeButton(R.string.no, new DialogInterface.OnClickListener()
//            {
//                @Override
//                public void onClick(DialogInterface dialog, int which)
//                {
//                    CheckBox checkBox = (CheckBox) checkBoxView.findViewById(R.id.skip);
//                    settings.edit().putBoolean(skipPrefName, checkBox.isChecked()).commit();
//                }
//            });
//
//            adb.show();
//        }
//        else
//        {
//            stopAllOfMe();
//        }
//    }

    private void stopAllOfMe()
    {
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
        localBroadcastManager.sendBroadcast(new Intent(DKYPhoneListenerService.ACTION_SHUTDOWN));

//        Intent exitIntent = new Intent().setClassName(stopmePackagePath, stopmeClassName).putExtra(EXTRA_SETTINGS_ACTION, true);
//        this.startActivity(exitIntent);

//        this.stopSelf();
    }

    private Notification getNotification(int accentColor, boolean showOnLockScreen, PendingIntent notifIntent, boolean noClear, boolean longRunning, String notificationTitle, String notificationShortText, String notificationLongText, NotificationCompat.Action... actions)
    {
        Bitmap largeIcon = getBitmapFromDrawableResource(this, R.drawable.ic_phone_forwarded_black_48dp, accentColor);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
        {
            NotificationChannel notifChannel = null;
            notifChannel = new NotificationChannel(NOTIF_CHANNEL_ID_CONTROL, NOTIF_CHANNEL_NAME_CONTROL, NotificationManager.IMPORTANCE_DEFAULT);
            notifChannel.setDescription(NOTIF_CHANNEL_DESCR_CONTROL);
            notifChannel.setLightColor(Color.TRANSPARENT);
            notifChannel.setLockscreenVisibility(Notification.VISIBILITY_SECRET);

            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(notifChannel);
        }

        NotificationCompat.Builder notifBuilder = new NotificationCompat.Builder(this, NOTIF_CHANNEL_ID_CONTROL)
                .setContentIntent(notifIntent)
                .setContentTitle(notificationTitle)
                .setContentText(notificationShortText)
                .setColor(accentColor)
                .setSmallIcon(R.drawable.ic_phone_forwarded_black_24dp)
                .setLargeIcon(largeIcon);

        if ((notificationLongText != null) && (notificationLongText.trim().length() > 0))
        {
            notifBuilder
                    .setStyle(new NotificationCompat.BigTextStyle()
                            .bigText(notificationLongText)
                    )
            ;
        }
        else
        {

        }

        if (showOnLockScreen)
        {
            notifBuilder
                    .setVisibility(Notification.VISIBILITY_PUBLIC)
            ;
        }
        else
        {
            notifBuilder
                    .setVisibility(Notification.VISIBILITY_SECRET)
            ;
        }

        for (NotificationCompat.Action tAction : actions)
        {
            if (tAction != null)
            {
                notifBuilder.addAction(tAction);
            }
        }

        Notification notification = notifBuilder.build();

        if (noClear)
        {
            notification.flags |= Notification.FLAG_NO_CLEAR;
        }

        if (longRunning)
        {
            notification.flags |= Notification.FLAG_ONGOING_EVENT;
        }

        return notification;
    }


    public class DKYStateListener extends PhoneStateListener
    {

        private int previousState = -1;

        DKYStateListener()
        {
            Log.d("DKYMain", "DKYStateListener created");

        }

        @Override
        public void onCallStateChanged(int state, String incomingNumber)
        {
            Log.d(LOGTAG, "******************************************************");
            Log.d(LOGTAG, "******************************************************");
            Log.d(LOGTAG, " ");

            Log.d(LOGTAG, "State = '" + state + "' incomingNumber = '" + incomingNumber + "'");

            super.onCallStateChanged(state, incomingNumber);

            String stateStr;

            switch (state)
            {
                case TelephonyManager.CALL_STATE_RINGING:
                    stateStr = DESCR_CALL_STATE_RINGING;
                    Log.d(LOGTAG, "Phone is ringing with incoming number: '" + incomingNumber + "'");
                    switch (previousState)
                    {
                        case TelephonyManager.CALL_STATE_OFFHOOK:
                            Log.d(LOGTAG, "Transition from offhook");
                            // CallWaiting/Call coming in during another active call...
                            // Can't reall do anything here since there is no separate
                            // API to end the new incoming call without ending the current call
                            callControl.silenceRinger(); // Not sure if silenceRinger will work in this state
                            break;
                        case TelephonyManager.CALL_STATE_IDLE:
                            Log.d(LOGTAG, "Transition from idle");
                            processRingingCall(incomingNumber);
                            break;
                        default:
                            Log.d(LOGTAG, "Transition from unknown");
                            processRingingCall(incomingNumber);
                    }
                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    stateStr = DESCR_CALL_STATE_OFFHOOK;
                    Log.d(LOGTAG, "Phone is offhook");
                    break;
                case TelephonyManager.CALL_STATE_IDLE:
                    stateStr = DESCR_CALL_STATE_IDLE;
                    Log.d(LOGTAG, "Phone is idle");
                    break;
                default:
                    stateStr = DESCR_CALL_STATE_UNKNOWN;
                    Log.d(LOGTAG, "Phone is in unknown state");
                    break;
            }

            Log.d(LOGTAG, stateStr);

            previousState = state;

            Log.d(LOGTAG, " ");
        }

        /**
         * This is where the ringing call processing logic happens in all its glory
         *
         * @param incomingNumber
         */
        private void processRingingCall(String incomingNumber)
        {
            Log.d(LOGTAG, "Processing ringing call from: '" + incomingNumber + "'");

            // Apply fixes for mystery caller number string mistakes...

            // Strip out ;oli=xx garbage passed in by some poorly implemented WiFi/SIP network equipment

            int oliGarbageIndex = incomingNumber.indexOf(";oli=");
            if (oliGarbageIndex == 0)
            {
                // Unlikely, but just in case...
                Log.d(LOGTAG, "Strange, but incomingNumber = '" + incomingNumber + "'");
                return;
            }

            if (oliGarbageIndex > 0)
            {
                incomingNumber = incomingNumber.substring(0, oliGarbageIndex);
                Log.d(LOGTAG, "Stripped out oli garbage: '" + incomingNumber + "'");
            }

            boolean shouldEndCall;
            String fullCallResultReason = "";
            String shortCallResultReason = "";
            String smsTextMsg = "";
            boolean smsWasSent = false;

            String normalizedIncomingNumber = this.normalizeNumberStr(incomingNumber);
            Log.d(LOGTAG, "normalized number is : '" + normalizedIncomingNumber + "'");

            String mode = prefs.getString(PREF_MODE, PREF_MODE_DEFAULT);
            Log.d(LOGTAG, "Mode is set to: '" + mode + "'");

            String processed_call_log = prefs.getString(PREF_PROCESSED_CALL_LOG, PREF_PROCESSED_CALL_LOG_DEFAULT);
            Log.d(LOGTAG, "Processed call log is set to: '" + processed_call_log + "'");

            int processed_call_log_max_size = prefs.getInt(PREF_PROCESSED_CALL_LOG_MAX_SIZE, PREF_PROCESSED_CALL_LOG_MAX_SIZE_DEFAULT);
            Log.d(LOGTAG, "Processed call log size is set to: '" + processed_call_log_max_size + "'");

            boolean dndExceptPersistent = prefs.getBoolean(PREF_DND_EXCEPT_PERSISTENTS, PREF_DND_EXCEPT_PERSISTENTS_DEFAULT);
            Log.d(LOGTAG, "DND except persistents is set to: '" + dndExceptPersistent + "'");

            int dndExceptPersistentTimes = prefs.getInt(PREF_DND_PERSISTENT_TIMES, PREF_DND_PERSISTENT_TIMES_DEFAULT);
            Log.d(LOGTAG, "DND persistent times is set to: '" + dndExceptPersistentTimes + "'");

            int dndExceptPersistentMinutes = prefs.getInt(PREF_DND_PERSISTENT_MINUTES, PREF_DND_PERSISTENT_MINUTES_DEFAULT);
            Log.d(LOGTAG, "DND persistent minutes is set to: '" + dndExceptPersistentMinutes + "'");

            String dndExceptions = prefs.getString(PREF_DND_EXCEPTIONS, PREF_DND_EXCEPTIONS_DEFAULT);
            Log.d(LOGTAG, "DND exceptions list is: '" + dndExceptions + "'");

            boolean dndSMSSend = prefs.getBoolean(PREF_DND_SMS_BLOCK_SEND, PREF_DND_SMS_BLOCK_SEND_DEFAULT);
            Log.d(LOGTAG, "DND SMS send is set to: '" + dndSMSSend + "'");

            String dndSMSMsg = prefs.getString(PREF_DND_SMS_BLOCK_MSG, PREF_DND_SMS_BLOCK_MSG_DEFAULT);
            Log.d(LOGTAG, "DND SMS msg is set to: '" + dndSMSMsg + "'");

            boolean blockAllExceptOutgoing = prefs.getBoolean(PREF_BLOCK_ALL_EXCEPT_OUTGOING, PREF_BLOCK_ALL_EXCEPT_OUTGOING_DEFAULT);
            Log.d(LOGTAG, "Block all except outgoing is: '" + blockAllExceptOutgoing + "'");

            boolean blockAllExceptContacts = prefs.getBoolean(PREF_BLOCK_ALL_EXCEPT_CONTACTS, PREF_BLOCK_ALL_EXCEPT_CONTACTS_DEFAULT);
            Log.d(LOGTAG, "Block all except contacts is set to: '" + blockAllExceptContacts + "'");

            String blockAllExceptions = prefs.getString(PREF_BLOCK_ALL_EXCEPTIONS, PREF_BLOCK_ALL_EXCEPTIONS_DEFAULT);
            Log.d(LOGTAG, "Block all exceptions is: '" + blockAllExceptions + "'");

            boolean blockAllSMSSend = prefs.getBoolean(PREF_BLOCK_ALL_SMS_BLOCK_SEND, PREF_BLOCK_ALL_SMS_BLOCK_SEND_DEFAULT);
            Log.d(LOGTAG, "Block All SMS send is set to: '" + blockAllSMSSend + "'");

            String blockAllSMSMsg = prefs.getString(PREF_BLOCK_ALL_SMS_BLOCK_MSG, PREF_BLOCK_ALL_SMS_BLOCK_MSG_DEFAULT);
            Log.d(LOGTAG, "Block All SMS msg is set to: '" + blockAllSMSMsg + "'");

            String allowAllExceptions = prefs.getString(PREF_ALLOW_ALL_EXCEPTIONS, PREF_ALLOW_ALL_EXCEPTIONS_DEFAULT);
            Log.d(LOGTAG, "Allow all exceptions is: '" + allowAllExceptions + "'");

            boolean allowAllSMSSend = prefs.getBoolean(PREF_ALLOW_ALL_SMS_BLOCK_SEND, PREF_ALLOW_ALL_SMS_BLOCK_SEND_DEFAULT);
            Log.d(LOGTAG, "Allow All SMS send is set to: '" + allowAllSMSSend + "'");

            String allowAllSMSMsg = prefs.getString(PREF_ALLOW_ALL_SMS_BLOCK_MSG, PREF_ALLOW_ALL_SMS_BLOCK_MSG_DEFAULT);
            Log.d(LOGTAG, "Allow All SMS msg is set to: '" + allowAllSMSMsg + "'");

            switch (mode)
            {
                case MODE_DND:
                    fullCallResultReason = "no allow options matched";
                    shortCallResultReason = "no allows";
                    shouldEndCall = true; // The default is to end the call in DND mode

                    if (dndExceptPersistent)
                    {
                        boolean isPersistent = isPersistentCaller(normalizedIncomingNumber, dndExceptPersistentTimes, dndExceptPersistentMinutes);
                        if (isPersistent)
                        {
                            fullCallResultReason = incomingNumber + " is a persistent caller";
                            shortCallResultReason = "peristent caller";
                            Log.d(LOGTAG, fullCallResultReason);
                            shouldEndCall = false;
                        }
                    }

                    if (dndExceptions.length() > 0)
                    {
                        boolean isInExceptions = this.isNumberInRawListStr(incomingNumber, dndExceptions);
                        if (isInExceptions)
                        {
                            fullCallResultReason = incomingNumber + " is in the " + MODE_DND + " exceptions list";
                            shortCallResultReason = "exceptions list";
                            Log.d(LOGTAG, fullCallResultReason);
                            shouldEndCall = false;
                        }
                    }

                    // Lastly, set smsTextMsg if the call will be ended
                    if (shouldEndCall && dndSMSSend)
                    {
                        smsTextMsg = dndSMSMsg;
                    }

                    break;
                case MODE_BLOCK_ALL:
                    fullCallResultReason = "no allow options matched";
                    shortCallResultReason = "no allows";
                    shouldEndCall = true; // The default is to end the call in Block All mode

                    if (blockAllExceptOutgoing)
                    {
                        boolean isOutgoingCallCaller = isOutgoingCallCaller(incomingNumber);
                        if (isOutgoingCallCaller)
                        {
                            fullCallResultReason = incomingNumber + " is a previous outgoing call caller";
                            shortCallResultReason = "previous outgoing caller";
                            Log.d(LOGTAG, fullCallResultReason);
                            shouldEndCall = false;
                        }
                    }

                    if (blockAllExceptContacts)
                    {
                        String contactID = getContactID(incomingNumber);
                        if (contactID != null)
                        {
                            fullCallResultReason = incomingNumber + " is a contact";
                            shortCallResultReason = "contact";
                            Log.d(LOGTAG, fullCallResultReason);
                            shouldEndCall = false;
                        }
                    }

                    if (blockAllExceptions.length() > 0)
                    {
                        boolean isInExceptions = this.isNumberInRawListStr(incomingNumber, blockAllExceptions);
                        if (isInExceptions)
                        {
                            fullCallResultReason = incomingNumber + " is in the " + MODE_BLOCK_ALL + " exceptions list";
                            shortCallResultReason = "exceptions list";
                            Log.d(LOGTAG, fullCallResultReason);
                            shouldEndCall = false;
                        }
                    }

                    // Lastly, set smsTextMsg if the call will be ended
                    if (shouldEndCall && blockAllSMSSend)
                    {
                        smsTextMsg = blockAllSMSMsg;
                    }

                    break;
                case MODE_ALLOW_ALL:
                    fullCallResultReason = "no block options matched";
                    shortCallResultReason = "no blocks";
                    shouldEndCall = false; // The default is to allow the call in Allow All mode

                    if (allowAllExceptions.length() > 0)
                    {
                        boolean isInExceptions = this.isNumberInRawListStr(incomingNumber, allowAllExceptions);
                        if (isInExceptions)
                        {
                            fullCallResultReason = incomingNumber + " is in the " + MODE_ALLOW_ALL + " exceptions list";
                            shortCallResultReason = "exceptions list";
                            Log.d(LOGTAG, fullCallResultReason);
                            shouldEndCall = true;
                        }
                    }


                    // Lastly, set smsTextMsg if the call will be ended
                    if (shouldEndCall && allowAllSMSSend)
                    {
                        smsTextMsg = allowAllSMSMsg;
                    }

                    break;
                default:
                    fullCallResultReason = "Unknown mode set: " + mode;
                    shortCallResultReason = "Unknown mode set: " + mode;
                    shouldEndCall = false;
                    Log.e(LOGTAG, fullCallResultReason);
            }

            String eventType;

            if (shouldEndCall)
            {
                Log.d(LOGTAG, "Ending call in '" + mode + "' mode because: '" + fullCallResultReason + "'");
                callControl.endCall();
                eventType = DKYPhoneEvent.EVENT_CALL_ENDED;

                if ((smsTextMsg != null) && (smsTextMsg.trim().length() > 0))
                {
                    this.sendSMS(incomingNumber, smsTextMsg);
                    smsWasSent = true; // TODO: Probably should determine this from sent intentions in the sendSMS method, but for now... assume no issues.
                }
            }
            else
            {
                Log.d(LOGTAG, "Allowed call in '" + mode + "' mode because: '" + fullCallResultReason + "'");
                eventType = DKYPhoneEvent.EVENT_CALL_ALLOWED;
            }

            // Add the call to the processed call log stored pref

            long dateTimeLong = Calendar.getInstance().getTimeInMillis();
            String dateTimeLongStr = String.valueOf(dateTimeLong);

            String newCallLogEntry = incomingNumber + SEP_ITEM + dateTimeLongStr + SEP_ITEM + eventType + SEP_ITEM + mode + SEP_ITEM + shortCallResultReason + SEP_ITEM + smsWasSent;
            processed_call_log = newCallLogEntry + SEP_TUPLE + processed_call_log;

            // Truncate call log to max items

            List<List<String>> processed_call_log_tuples = getTuplesFromString(processed_call_log);
            List<List<String>> truncated_processed_call_log_tuples = new ArrayList<>();
            int i=0;
            for (List<String> processed_call_log_tuple : processed_call_log_tuples)
            {
                if (i>=processed_call_log_max_size)
                {
                    break;
                }
                truncated_processed_call_log_tuples.add(processed_call_log_tuple);
                i++;
            }

            String truncated_processed_call_log = getStringFromTuples(truncated_processed_call_log_tuples);

            prefs.edit().putString(PREF_PROCESSED_CALL_LOG, truncated_processed_call_log).commit();

            DKYPhoneEvent event = new DKYPhoneEvent(incomingNumber, eventType, fullCallResultReason);

            firePhoneEvent(event);

            Log.d(LOGTAG, " ");
        }

        /**
         * @param incomingNumber
         * @return true if the incomingNumber corresponds to an existing contact
         */
        private String getContactID(String incomingNumber)
        {
            Uri lookupUri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(incomingNumber));
            Log.d(LOGTAG, "Contacts URI is: '" + lookupUri.toString() + "' and incoming number is: '" + incomingNumber + "'");
            String[] phoneNumberProjection = {ContactsContract.PhoneLookup._ID, ContactsContract.PhoneLookup.DISPLAY_NAME};
            Cursor cur = DKYPhoneListenerService.this.getContentResolver().query(lookupUri, phoneNumberProjection, null, null, null);
            try
            {
                if ((cur != null) && (cur.moveToFirst()))
                {
                    String contactID = cur.getString(0);
                    String displayName = cur.getString(1);
                    Log.d(LOGTAG, "Contact found: '" + displayName + "'");
                    return contactID;
                }
                else
                {
                    Log.d(LOGTAG, "Contact not found for '" + incomingNumber + "'");
                    return null;
                }
            }
            finally
            {
                if (cur != null)
                {
                    cur.close();
                }
            }

        }

        /**
         * @param incomingNumber
         * @return true is caller is considered a 'persistent caller' - i.e. calls x times within the last y minutes
         */
        private boolean isPersistentCaller(String incomingNumber, int targetTimes, int targetMinutes)
        {
            if (ActivityCompat.checkSelfPermission(DKYPhoneListenerService.this, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED)
            {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                // return TODO;
            }

            Calendar nowCal = Calendar.getInstance();
            long nowMillis = nowCal.getTimeInMillis();
            String nowMillisStr = String.valueOf(nowMillis);

            long minutesSeconds = targetMinutes * 60;
            long minutesMillis = minutesSeconds * 1000;

            long backTimeMillis = nowMillis - minutesMillis;
            String backTimeMillisStr = String.valueOf(backTimeMillis);

            Uri lookupUri = CallLog.Calls.CONTENT_URI;
            String[] projection = null;

            String numberPart = CallLog.Calls.NUMBER + " = ?";
            String typePart = CallLog.Calls.TYPE + " IN ( " + CallLog.Calls.BLOCKED_TYPE + "," + CallLog.Calls.REJECTED_TYPE + "," + CallLog.Calls.VOICEMAIL_TYPE + "," + CallLog.Calls.MISSED_TYPE + ")";
            String datePart = CallLog.Calls.DATE + " BETWEEN ? AND ?";
            String whereClause = numberPart + " AND " + typePart + " AND " + datePart;
            String[] whereClauseArgs = {incomingNumber, backTimeMillisStr, nowMillisStr};

            String sortOrder = android.provider.CallLog.Calls.DATE + " DESC";

            Cursor cur = DKYPhoneListenerService.this.getContentResolver().query(lookupUri, projection, whereClause, whereClauseArgs, sortOrder);
            try
            {
                if ((cur != null) && (cur.moveToFirst()))
                {
                    int actualTimes = cur.getCount();

                    // Note, we subtract 1 from the targetTimes because the current call is
                    // not in the call log yet - it will be after this call has been handled.
                    if (actualTimes >= (targetTimes -1))
                    {
                        String displayName = cur.getString(1);
                        Log.d(LOGTAG, "Contact found: '" + displayName + "' with " + actualTimes + " calls within " + targetMinutes + " minutes.");
                        return true;
                    }
                    else
                    {
                        return false;
                    }
                }
                else
                {
                    Log.d(LOGTAG, "No previous outgoing calls in the last " + targetMinutes + " minutes found for '" + incomingNumber + "' ");
                    return false;
                }
            }
            finally
            {
                if (cur != null)
                {
                    cur.close();
                }
            }
        }

        /**
         * @param incomingNumber
         * @return
         */
        private boolean isOutgoingCallCaller(String incomingNumber)
        {
            if (ActivityCompat.checkSelfPermission(DKYPhoneListenerService.this, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED)
            {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                // return TODO;
            }

            Uri lookupUri = CallLog.Calls.CONTENT_URI;
            String[] projection = null;

            String whereClause = CallLog.Calls.NUMBER + " = ? AND " + CallLog.Calls.TYPE + " = " + CallLog.Calls.OUTGOING_TYPE;
            String[] whereClauseArgs = {incomingNumber};

            String sortOrder = android.provider.CallLog.Calls.DATE + " DESC";


            Cursor cur = DKYPhoneListenerService.this.getContentResolver().query(lookupUri, projection, whereClause, whereClauseArgs, sortOrder);
            try
            {
                if ((cur != null) && (cur.moveToFirst()))
                {
                    return true;
                }
                else
                {
                    Log.d(LOGTAG, "No previous outgoing calls found for '" + incomingNumber + "'");
                    return false;
                }
            }
            finally
            {
                if (cur != null)
                {
                    cur.close();
                }
            }
        }




        private boolean isNumberInRawListStr(String incomingNumberStr, String rawListStr)
        {
            String normalizedIncomingNumStr = this.normalizeNumberStr(incomingNumberStr);
            String contactID = this.getContactID(incomingNumberStr);

            List<List<String>> tuples = getTuplesFromString(rawListStr);
            for (List<String> tuple : tuples)
            {
                String numGlobOrCIDStr = tuple.get(0);

                if (numGlobOrCIDStr.startsWith(CONTACT_ID_PREFIX))
                {
                    if (contactID != null)
                    {
                        String cidStr = numGlobOrCIDStr.substring(CONTACT_ID_PREFIX.length());
                        if (cidStr.equals(contactID))
                        {
                            return true;
                        }
                    }
                }
                else
                {
                    String normalizedNumRegexStr = normalizeNumberGlobStrToRegexStr(numGlobOrCIDStr);

                    Pattern p = Pattern.compile(normalizedNumRegexStr);
                    Matcher m = p.matcher(normalizedIncomingNumStr);
                    boolean b = m.matches();

                    if (b)
                    {
                        return true;
                    }
                }
            }

            return false;
        }

        private String normalizeNumberGlobStrToRegexStr(String numberGlobStr)
        {

            String regexStr = "^";
            for (int i = 0; i < numberGlobStr.length(); ++i)
            {
                final char c = numberGlobStr.charAt(i);
                switch (c)
                {
                    case '*':
                        if (regexStr.endsWith(".*"))
                        {
                            // do nothing since another ".*" won't add anything
                        }
                        else if (regexStr.endsWith("."))
                        {
                            // just add a "*" since "." + ".*" = ".*"
                            regexStr += "*";
                        }
                        else
                        {
                            regexStr += ".*";
                        }
                        break;
                    case '?':
                        if (regexStr.endsWith(".*"))
                        {
                            // do nothing since ".* + "." = ".*"
                        }
                        else
                        {
                            regexStr += '.';
                        }
                        break;
                    default:
                        if (Character.isDigit(c))
                        {
                            regexStr += c;
                        }
                        else
                        {
                            // Not a glob character and not a digit, so don't add char to the regex
                        }
                }
            }
            regexStr += '$';

            return regexStr;
        }

        private String normalizeNumberStr(String numberStr)
        {
            // TODO: Should we call the wrapped google phonenumberUtils library instead?
            return numberStr.replaceAll("\\D+", "");
        }


        private void sendSMS(String phoneNumber, String message)
        {
            SmsManager smsManager = SmsManager.getDefault();
            Log.d(LOGTAG, "Prepping to send SMS text to: '" + phoneNumber + "' with before processing msg: '" + message + "'");

            // Replace all instances of special sms message variables...

            // Replace all "<times>" with the persistent caller times value
            int dndExceptPersistentTimes = prefs.getInt(PREF_DND_PERSISTENT_TIMES, PREF_DND_PERSISTENT_TIMES_DEFAULT);
            message = message.replaceAll("<times>", String.valueOf(dndExceptPersistentTimes));

            // Replace all "<minutes>" with persistent caller minutes value
            int dndExceptPersistentMinutes = prefs.getInt(PREF_DND_PERSISTENT_MINUTES, PREF_DND_PERSISTENT_MINUTES_DEFAULT);
            message = message.replaceAll("<minutes>", String.valueOf(dndExceptPersistentMinutes));

            Log.d(LOGTAG, "Sending SMS text to: '" + phoneNumber + "' with processed msg: '" + message + "'");


            ArrayList<String> msgParts = smsManager.divideMessage(message);

            // TODO: create sendIntent & deliveryIntent?
            smsManager.sendMultipartTextMessage(phoneNumber, null, msgParts, null, null);

            // Individual, non-multipart, SMS message max is 140 bytes - e.g.: 160 7-bit characters, 140 8-bit characters, or 70 16-bit characters
            //sms.sendTextMessage(phoneNumber, null, message, null, null);
        }
    }
}
