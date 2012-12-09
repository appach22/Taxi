package com.taxisoft.taxiorder;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;

public class OrderStateService extends Service
{
	ArrayList<String> mOrders;
    XmlPullParser mStateResponseParser;
    URL mStateUrl;

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
			mStateResponseParser.setInput(mStateUrl.openStream(), "UTF-8");
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
		for (int i = 0; i < mOrders.size(); ++i)
		{
			int state = getOrderState(mOrders.get(i));
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
			mOrders.add(orders[i]);

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
    			try
				{
					TimeUnit.SECONDS.sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
    			getAllOrdersState();
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
		Log.d(LOG_TAG, String.format("OrderStateService onStartCommand intent=%s", intent.toString()));
		synchronized(this) {
			mOrders.add(intent.getStringExtra("id"));
		}
		return START_STICKY;
	}
	
}
