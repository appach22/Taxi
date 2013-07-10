package com.taxisoft.taxi7lite;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.widget.Toast;

public class SmsReceiver extends BroadcastReceiver {
	
	public final static String NUMBER_VERIFIED = "com.taxisoft.taxi7lite.NumberVerified";

	@Override
	public void onReceive(Context context, Intent intent) 
	{
	    Bundle bundle = intent.getExtras();        
	    SmsMessage[] msgs = null;
	    String str = "";            
	    if (bundle != null)
	    {
	        Object[] pdus = (Object[]) bundle.get("pdus");
	        msgs = new SmsMessage[pdus.length];            
	        for (int i=0; i<msgs.length; i++)
	        {
	            msgs[i] = SmsMessage.createFromPdu((byte[])pdus[i]);  
	            if (msgs[i].getMessageBody().toString().contains(android.os.Build.DEVICE))
	            {
	                abortBroadcast();
	                str = "Msg from " + msgs[i].getOriginatingAddress() + " !";
	                Toast.makeText(context, str, Toast.LENGTH_LONG).show();
	                SharedPreferences settings = context.getSharedPreferences("TaxiLitePrefs", Context.MODE_PRIVATE);
	                SharedPreferences.Editor settingsEditor = settings.edit();
	                settingsEditor.putBoolean("numberIsVerified", true);
	                settingsEditor.commit();
	                Intent verified = new Intent(NUMBER_VERIFIED);
	                context.sendBroadcast(verified);
	            }    
	        }
	    }	
	}

}
