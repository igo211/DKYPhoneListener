package com.zero211.dkyphonelistener;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.List;

import static com.zero211.dkyphonelistener.DKYPhoneListenerService.MODE_DND;
import static com.zero211.dkyphonelistener.DKYPhoneListenerService.PREF_DND_PRIOR_MODE;
import static com.zero211.dkyphonelistener.DKYPhoneListenerService.PREF_DND_PRIOR_MODE_DEFAULT;
import static com.zero211.dkyphonelistener.DKYPhoneListenerService.PREF_DND_SCHEDULE;
import static com.zero211.dkyphonelistener.DKYPhoneListenerService.PREF_DND_SCHEDULE_DEFAULT;
import static com.zero211.dkyphonelistener.DKYPhoneListenerService.PREF_DND_SCHEDULE_ONOFF;
import static com.zero211.dkyphonelistener.DKYPhoneListenerService.PREF_DND_SCHEDULE_ONOFF_DEFAULT;
import static com.zero211.dkyphonelistener.DKYPhoneListenerService.PREF_MODE;
import static com.zero211.dkyphonelistener.DKYPhoneListenerService.PREF_MODE_DEFAULT;

/**
 * Created by malloys on 12/11/17.
 */

public class DKYModeAlarmReceiver extends BroadcastReceiver
{
    private static final String LOGTAG = DKYModeAlarmReceiver.class.getCanonicalName();

    private static final String MODE_START_ALARM_ACTION = "com.zero211.dkyphonelistener.MODE_START_ALARM";
    private static final String MODE_END_ALARM_ACTION = "com.zero211.dkyphonelistener.MODE_END_ALARM";

    @Override
    public void onReceive(Context context, Intent intent)
    {
        Log.d(LOGTAG, ">>>>>>>>> Processing DND mode alarm event <<<<<<<<<");

        String action = intent.getAction();
        Log.d(LOGTAG, "Received a " + action + " action in the intent");

        this.setMode(context, action);

        this.setNextModeAlarm(context);
    }

    private void setMode(Context context, String modeEventType)
    {
        SharedPreferences prefs = this.getPrefs(context);

        String mode = prefs.getString(PREF_MODE, PREF_MODE_DEFAULT);
        Log.d(LOGTAG, "Mode is set to: '" + mode + "'");

        String prior_mode = prefs.getString(PREF_DND_PRIOR_MODE, PREF_DND_PRIOR_MODE_DEFAULT);
        Log.d(LOGTAG, "Mode prior to DND is set to: '" + prior_mode + "'");

        if (modeEventType.equals(MODE_START_ALARM_ACTION))
        {
            Log.d(LOGTAG, "Mode alarm action is START");
            if (!mode.equals(MODE_DND))
            {
                Log.d(LOGTAG, "Setting mode to: '" + MODE_DND + "'");
                prefs.edit().putString(PREF_MODE, MODE_DND).commit();
                Log.d(LOGTAG, "Setting dnd prior mode to: '" + mode + "'");
                prefs.edit().putString(PREF_DND_PRIOR_MODE, mode).commit();
                fireModeChangedEvent(context);
            }
            else
            {
                Log.d(LOGTAG, "Already in " + MODE_DND + " mode, so no need to set the mode.");
            }
        }
        else if (modeEventType.equals(MODE_END_ALARM_ACTION))
        {
            Log.d(LOGTAG, "Mode alarm action is END");
            if (mode.equals(MODE_DND))
            {
                Log.d(LOGTAG, "Setting mode to: '" + prior_mode + "'");
                prefs.edit().putString(PREF_MODE, prior_mode).commit();
                fireModeChangedEvent(context);
            }
            else
            {
                Log.d(LOGTAG, "Already out of " + MODE_DND + " mode, so no need to set the mode.");
            }
        }
        else
        {
            // Huh?  What action is this?
            Log.e(LOGTAG, "*************** Unknown mode alarm broadcast action: '" + modeEventType + "' *******************");
            return;
        }
    }

    private void fireModeChangedEvent(Context context)
    {
        DKYPhoneEvent modeChangedEvent = new DKYPhoneEvent(null, DKYPhoneEvent.EVENT_MODE_AUTO_CHANGED, null);
        DKYPhoneListenerService.firePhoneEvent(modeChangedEvent);
    }

    public void setNextModeAlarm(Context context)
    {
        this.cancelModeAlarms(context);

        Log.d(LOGTAG, "Start of setting the next alarm");
        PendingIntent startPI = this.getModeStartAlarmPendingIntent(context);
        PendingIntent endPI = this.getModeEndAlarmPendingIntent(context);

        SharedPreferences prefs = this.getPrefs(context);

        boolean dnd_sched_on = prefs.getBoolean(PREF_DND_SCHEDULE_ONOFF, PREF_DND_SCHEDULE_ONOFF_DEFAULT);
        Log.d(LOGTAG, "DND schedule on flag is set to: '" + dnd_sched_on + "'");

        if (dnd_sched_on)
        {

            String mode = prefs.getString(PREF_MODE, PREF_MODE_DEFAULT);
            Log.d(LOGTAG, "Mode is set to: '" + mode + "'");

            String prior_mode = prefs.getString(PREF_DND_PRIOR_MODE, PREF_DND_PRIOR_MODE_DEFAULT);
            Log.d(LOGTAG, "Mode prior to DND is set to: '" + prior_mode + "'");

            // Read in the DND schedule
            String dndSchedStr = prefs.getString(PREF_DND_SCHEDULE, PREF_DND_SCHEDULE_DEFAULT);
            List<List<String>> dndSchedTuples = DKYPhoneListenerService.getTuplesFromString(dndSchedStr);
            Log.d(LOGTAG, "Got the DND schedule list for processing...");

            // Get the now/today calendar instance and calculate the offset from this week's Sunday midnight.

            long triggerAtMillis;

            Calendar currCal = Calendar.getInstance();

            int minutesInADay = 24 * 60;
            int minutesInAWeek = 7 * minutesInADay;

            // Determine current day of week (1-7)
            int cDow = currCal.get(Calendar.DAY_OF_WEEK);
            Log.d(LOGTAG, "Current day of the week number is: '" + (cDow - 1) + "'");

            // Determine current hour of day (0-23)
            int cHour = currCal.get(Calendar.HOUR_OF_DAY);
            Log.d(LOGTAG, "Current hour is: '" + cHour + "'");

            // Determine current minute of hour (0-59)
            int cMin = currCal.get(Calendar.MINUTE);
            Log.d(LOGTAG, "Current minute is: '" + cMin + "'");

            // Determine minute offset from dow midnight (0-1439)
            int cMinFromMid = (cHour * 60) + cMin;
            Log.d(LOGTAG, "Current minute since Midnight is: '" + cMinFromMid + "'");

            // Determine minute offset from Sunday midnight (0-10073)
            int cMinFromSun = (((cDow - 1) * minutesInADay) + cMinFromMid);
            Log.d(LOGTAG, "Current minute since Sunday midnight is: '" + cMinFromSun + "'");

            // Find next schedule time, (start or end time)  after now, in the DND schedule

            PendingIntent pi = endPI;
            int schedMinsSinceSun = Integer.MAX_VALUE;
            String nextItem = "unknown";

            Log.d(LOGTAG, "Processing DND schedule...");
            for (List<String> dndSchedTuple : dndSchedTuples)
            {
                String startMinsStr = dndSchedTuple.get(0);
                Log.d(LOGTAG, "Start mins since midnight is: ' " + startMinsStr + "'");
                String endMinStr = dndSchedTuple.get(1);
                Log.d(LOGTAG, "End mins since midnight is: ' " + endMinStr + "'");
                String daysSelectedStr = dndSchedTuple.get(2);
                Log.d(LOGTAG, "Day of Week selector string is: '" + daysSelectedStr + "'");

                for (int i = 0; i < 7; i++)
                {
                    String dowStr = daysSelectedStr.substring(i, i + 1);
                    Log.d(LOGTAG, "Current day of week selector value is: '" + dowStr + "'");
                    if (dowStr.trim().equals("1"))
                    {
                        int adjustedDow = i;

                        int startMins = Integer.parseInt(startMinsStr);
                        int startMinsSinceSun = (adjustedDow * minutesInADay) + startMins;
                        Log.d(LOGTAG, "Start mins since Sunday midnight is: ' " + startMinsSinceSun + "'");

                        if (startMinsSinceSun <= cMinFromSun)
                        {
                            // The start is earlier in the calendar week, so for this calculation, it needs to represent next week...
                            startMinsSinceSun = startMinsSinceSun + minutesInAWeek;
                            Log.d(LOGTAG, "Adjusting Start mins since Sunday midnight into next week to: '" + startMinsSinceSun + "' since it's before the current.");
                        }

                        if (startMinsSinceSun < schedMinsSinceSun)
                        {
                            Log.d(LOGTAG, "This item's start is the new contender for next schedule item");
                            schedMinsSinceSun = startMinsSinceSun;
                            pi = startPI;
                            nextItem = startMinsStr + "," + endMinStr + "," + daysSelectedStr;
                            continue;
                        }


                        int endMins = Integer.parseInt(endMinStr);
                        int endMinsSinceSun;
                        if (endMins < startMins)
                        {
                            // means the end is really in the next day...
                            adjustedDow = i + 1;
                            Log.d(LOGTAG, "Adjusting End day of week up one since start is after end");
                        }
                        endMinsSinceSun = (adjustedDow * minutesInADay) + endMins;
                        Log.d(LOGTAG, "End mins since Sunday midnight is: ' " + endMinsSinceSun + "'");

                        if (endMinsSinceSun <= cMinFromSun)
                        {
                            // The end is earlier in the calendar week, so for this calculation, it needs to represent next week...
                            endMinsSinceSun = endMinsSinceSun + minutesInAWeek;
                            Log.d(LOGTAG, "Adjusting End mins since Sunday midnight into next week to: '" + endMinsSinceSun + "' since it's before the current.");
                        }

                        if (endMinsSinceSun < schedMinsSinceSun)
                        {
                            Log.d(LOGTAG, "This item's end is the new contender for next schedule item");
                            schedMinsSinceSun = endMinsSinceSun;
                            pi = endPI;
                            nextItem = startMinsStr + "," + endMinStr + "," + daysSelectedStr;
                        }
                    }
                }
            }

            if (schedMinsSinceSun == Integer.MAX_VALUE)
            {
                // No future schedule item found....
                Log.d(LOGTAG, "......... No future DND mode schedule item found, so not setting an alarm .........");
            }
            else
            {
                if ((!mode.equals(MODE_DND)) && (pi == endPI))
                {
                    // We are currently inside of this DND schedule item, so why is the mode not DND?

                    // Could be that the user manually changed from DND to other mode while inside a scheduled DND,
                    // then made an unrelated change to to schedule, thereby causing this method to be called.
                    // OR... they added this specific new DND schedule item when they were already inside the window...
                    // So, do we set it (or change it back) for them, effectively enforcing the currently scheduled mode?
                    // (i.e. Do we set the mode to DND by send a startPI to setMode()?)
                    // Hard call, since we have no way of knowing which change/cause without versioning of the schedule or some way of tracking the schedule change specifics.
                }

                // Convert schedule minutes since Sunday midnight to a calendar instance

                // Start with today/now..
                Calendar schedCal = Calendar.getInstance();

                // Adjust to Sunday of this week...
                schedCal.set(Calendar.DAY_OF_WEEK, 1);

                // Adjust to midnight...
                schedCal.set(Calendar.HOUR_OF_DAY, 0);
                schedCal.set(Calendar.MINUTE, 0);
                schedCal.set(Calendar.SECOND, 0);
                schedCal.set(Calendar.MILLISECOND, 0);

                // Now add the above calculated minutes since Sunday midnight from the next schedule item.
                schedCal.add(Calendar.MINUTE, schedMinsSinceSun);

                DateFormat df = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG);
                String formattedDate = df.format(schedCal.getTime());


                // Set triggerAtMillis to above result
                triggerAtMillis = schedCal.getTimeInMillis();

                AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

                if ((System.currentTimeMillis() + 1000) < triggerAtMillis)
                {
                    Log.d(LOGTAG, "............ Setting alarm for next schedule item: '" + nextItem + "' i.e.: '" + formattedDate + "' .............");
                    am.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi);
                }
                else
                {
                    Log.w(LOGTAG, "*********** Just missed the window for alarm time: '" + formattedDate + "'.  re-calling to set for next mode alarm *********");
                    this.setNextModeAlarm(context);
                }
            }
        }
        else
        {
            Log.d(LOGTAG, "DND schedule processing is not on so, not doing anything more with alarms or schedule processing for now.");
        }

    }

    public void cancelModeAlarms(Context context)
    {
        Log.d(LOGTAG, "Cancelling Mode alarms...");
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        PendingIntent startPI = this.getModeStartAlarmPendingIntent(context);
        alarmManager.cancel(startPI);

        PendingIntent endPI = this.getModeEndAlarmPendingIntent(context);
        alarmManager.cancel(endPI);
    }

    private SharedPreferences getPrefs(Context context)
    {
        Context appContext = context.getApplicationContext();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(appContext);
        return prefs;
    }

    private PendingIntent getModeStartAlarmPendingIntent(Context context)
    {
        Intent i = new Intent(MODE_START_ALARM_ACTION);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);
        return pi;
    }

    private PendingIntent getModeEndAlarmPendingIntent(Context context)
    {
        Intent i = new Intent(MODE_END_ALARM_ACTION);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);
        return pi;
    }
}
