package com.zero211.dkyphonelistener;

/**
 * Created by malloys on 10/27/2017.
 */

public interface DKYPhoneEventListener
{
    /**
     * Invoked when the DKYPhoneListenerService processes a call or sms message.
     * @param event The DKYPhoneEvent which occured.
     */
    public void phoneEventProcessed(DKYPhoneEvent event);
}
