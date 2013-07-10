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
	    //String str = "";            
	    if (bundle != null)
	    {
	        Object[] pdus = (Object[]) bundle.get("pdus");
	        msgs = new SmsMessage[pdus.length];            
	        for (int i=0; i<msgs.length; i++)
	        {
	            msgs[i] = SmsMessage.createFromPdu((byte[])pdus[i]);  
	            String messageText = msgs[i].getMessageBody().toString();
	            if (messageText.contains(android.os.Build.DEVICE))
	            {
	                abortBroadcast();
	                System.out.println("got message " + messageText);
	                //str = "Msg from " + msgs[i].getOriginatingAddress() + " !";
	                //Toast.makeText(context, str, Toast.LENGTH_LONG).show();
	                String[] parts = messageText.split("_");
	                String number = "";
	                if (parts.length > 1)
	                	number = parts[1];
	                System.out.println("found number " + number);
	                Intent verified = new Intent(NUMBER_VERIFIED);
	                verified.putExtra("number", number);
	                context.sendBroadcast(verified);
	            }    
	        }
	    }	
	}

}
