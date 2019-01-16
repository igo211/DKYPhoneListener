package com.zero211.dkyphonelistener;

/**
 * Created by malloys on 10/27/2017.
 */

public class DKYPhoneEvent
{
    public static String EVENT_CALL_ALLOWED = "CALL_ALLOWED";
    public static String EVENT_CALL_ENDED = "CALL_ENDED";
    public static String EVENT_CALL_SILENCED = "CALL_SILENCED";
    public static String EVENT_CALL_HUNG_UP = "CALL_HUNGUP";
    public static String EVENT_MODE_AUTO_CHANGED = "MODE_AUTO_CHANGED";

    private String number;
    private String eventType;
    private String callResultReason;

    public DKYPhoneEvent(String number, String eventType, String callResultReason)
    {
        this.number = number;
        this.eventType = eventType;
        this.callResultReason = callResultReason;
    }

    public String getNumber()
    {
        return number;
    }

    public String getEventType()
    {
        return this.eventType;
    }

    public String getCallResultReason()
    {
        return callResultReason;
    }
}
