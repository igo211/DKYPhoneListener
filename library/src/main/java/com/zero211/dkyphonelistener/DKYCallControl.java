package com.zero211.dkyphonelistener;

import android.content.Context;
import android.os.RemoteException;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.internal.telephony.ITelephony;

import java.lang.reflect.Method;

import static android.content.Context.TELEPHONY_SERVICE;

/**
 * Created by malloys on 9/7/2017.
 */

public class DKYCallControl
{
    private static final String LOGTAG = DKYCallControl.class.getCanonicalName();

    private ITelephony telService;

    public DKYCallControl(Context context)
    {
        TelephonyManager telManager = (TelephonyManager) (context.getSystemService(TELEPHONY_SERVICE));

        Method getITelMethod;
        try
        {
            getITelMethod = TelephonyManager.class.getDeclaredMethod("getITelephony");
            getITelMethod.setAccessible(true);
            telService = (ITelephony) getITelMethod.invoke(telManager);
        }
        catch (Throwable e)
        {
            Log.d(LOGTAG, "", e);
        }
    }

    public boolean endCall()
    {
        try
        {
            return telService.endCall();
        }
        catch (RemoteException e)
        {
            Log.d(LOGTAG,"endCall failed",e);
            return false;
        }
    }

    public void answerRingingCall()
    {
        // TODO: Find missing stub?
        // telService.answerRingingCall();
    }

    public void silenceRinger()
    {
        try
        {
            telService.silenceRinger();
        }
        catch (RemoteException e)
        {
            Log.d(LOGTAG,"silenceRinger failed",e);
        }
    }
}
