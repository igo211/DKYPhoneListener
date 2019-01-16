package com.zero211.dkyphonelistener;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Process;
import android.util.Log;

/**
 * Created by malloys on 10/24/2017.
 */

public class DKYPhoneListenerShutdownReceiver extends BroadcastReceiver
{
    private static final String LOGTAG = DKYPhoneListenerShutdownReceiver.class.getCanonicalName();

    private DKYPhoneListenerService service;

    public DKYPhoneListenerShutdownReceiver(DKYPhoneListenerService service)
    {
        this.service = service;
    }

    @Override
    public void onReceive(Context context, Intent intent)
    {
        String actionStr = intent.getAction();
        Log.d(LOGTAG, "Received " + actionStr + " broadcast intent");

        switch (actionStr)
        {
            case DKYPhoneListenerService.ACTION_SHUTDOWN:
                break;
            default:
                Log.e(LOGTAG, "Received unknown intent action: '" + actionStr + "'");
        }

        this.service.stopSelf();
        Process.killProcess(Process.myPid());
    }
}
