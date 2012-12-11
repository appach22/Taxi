package com.taxisoft.taxiorder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class OrderStateService extends Service
{
	ArrayList<String> mOrders;
    XmlPullParser mStateResponseParser;
    URL mStateUrl;
    int mCounter;
    NotificationManager mNotificationManager;

    public final static String BROADCAST_ACTION = "com.taxisoft.taxiorder.OrderStateService"; 
    		
    		
	@Override
	public IBinder onBind(Intent arg0)
	{
		// TODO Auto-generated method stub
		return null;
	}

	final String LOG_TAG = "OrderStateService";
	
	private int getOrderState(String id)
	{
		try
		{
			//mStateResponseParser.setInput(mStateUrl.openStream(), "UTF-8");
			mStateResponseParser.setInput(new ByteArrayInputStream(("<?xml version=\"1.0\" standalone=\"yes\"?><response><state>" + Integer.toString(mCounter - 1) + "</state></response>").getBytes()), "UTF-8");
	        int eventType = mStateResponseParser.getEventType();
	        while (eventType != XmlPullParser.END_DOCUMENT) {
	        	if(eventType == XmlPullParser.START_TAG) {
	        		if (mStateResponseParser.getName().equalsIgnoreCase("state"))
	        			return Integer.parseInt(mStateResponseParser.nextText());
	        	}
	        	eventType = mStateResponseParser.nextTag();
	        }

		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (XmlPullParserException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return -1;
	}
	
	private void getAllOrdersState()
	{
		Intent intent = new Intent(BROADCAST_ACTION);
		for (int i = 0; i < mOrders.size(); ++i)
		{
			intent.putExtra("id", mOrders.get(i));
			int state = getOrderState(mOrders.get(i));
			intent.putExtra("state", state);
			sendBroadcast(intent);
			Context ctx = getApplicationContext();
			Resources res = ctx.getResources();
			Notification notification = new NotificationCompat.Builder(getApplicationContext())
	         .setContentTitle("Order state")
	         .setContentText("Order state is " + String.valueOf(state))
	         .setSmallIcon(R.drawable.ic_launcher)
	         .setLargeIcon(BitmapFactory.decodeResource(res, R.drawable.ic_launcher))
	         .setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, MapActivity_.class), 0))
	         .build();	
			mNotificationManager.notify(Integer.parseInt(mOrders.get(i)), notification);
			switch (state)
			{
				case Order.STATE_UNDEFINED :
					break;
				case Order.STATE_NO_SUCH_ORDER :
					break;
				case Order.STATE_ACCEPTED :
					break;
				case Order.STATE_LOOKING_FOR_CAR :
					break;
				case Order.STATE_CAR_ASSIGNED :
					break;
				case Order.STATE_CAR_APPROACHES :
					break;
				case Order.STATE_CAR_ARRIVED :
					break;
				case Order.STATE_ON_THE_WAY :
					break;
				case Order.STATE_CLOSED :
					break;
				case Order.STATE_CAR_NOT_FOUND :
					break;
				case Order.STATE_CANCELLED :
					break;
				case Order.STATE_EDITED :
					break;
				default :
					break;
			}
		}
		synchronized(this) {
		}
	}

	public void onCreate() 
	{
	    super.onCreate();
	    Log.d(LOG_TAG, "OrderStateService onCreate");
		SharedPreferences settings = getSharedPreferences("TaxiPrefs", MODE_PRIVATE);
		String orders[] = settings.getString("CurrentOrders", "").split(",");
		mOrders = new ArrayList<String>();
		for (int i = 0; i < orders.length; ++i)
		{
			Log.d(LOG_TAG, "orders[i]=" + orders[i]);
			if (!orders[i].equals("null") && !orders[i].equals(""))
			{
				Log.d(LOG_TAG, "Adding " + orders[i]);
				mOrders.add(orders[i]);
			}
		}

		mNotificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		
		mCounter = 0;
		
		try
		{
			mStateUrl = new URL(MapActivity.URL_ORDER_STATE);
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
		}

		XmlPullParserFactory factory;
		try {
			factory = XmlPullParserFactory.newInstance();
	        factory.setNamespaceAware(true);
	        mStateResponseParser = factory.newPullParser();
		} catch (XmlPullParserException e) {
		}

    	new Thread(new Runnable(){ 
    		public void run(){
    			while (true)
    			{
	    			try
					{
						TimeUnit.SECONDS.sleep(10);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
	    			mCounter = ++mCounter % 12;
	    			getAllOrdersState();
    			}
    		} 
    	}).start();
	}

	public void onDestroy() 
	{
	    super.onDestroy();
	    Log.d(LOG_TAG, "OrderStateService onDestroy");
	}

	public int onStartCommand(Intent intent, int flags, int startId) 
	{
		Log.d(LOG_TAG, "onStartCommand");
		if (intent != null)
		{
			String newID = intent.getStringExtra("id");
			Log.d(LOG_TAG, String.format("OrderStateService onStartCommand, id=%s intent=%s", newID, intent.toString()));
			if (!mOrders.contains(newID))
			{
				synchronized(this) {
					mOrders.add(newID);
				}
		    	SharedPreferences settings = getSharedPreferences("TaxiPrefs", MODE_PRIVATE);
		    	SharedPreferences.Editor prefEditor = settings.edit();
		    	String param = "";
		    	for (int i = 0; i < mOrders.size(); ++i)
		    	{
		    		if (i > 0)
		    			param += ",";
		    		param += mOrders.get(i);
		    	}
		    	prefEditor.putString("CurrentOrders", param);
		    	prefEditor.commit();
			}
		}
		
		return START_STICKY;
	}
	
}
