package com.taxisoft.taxi7lite;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Timer;
import java.util.TimerTask;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import com.example.taxi7.R;

import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.RelativeLayout;

public class MainActivity extends Activity implements OnClickListener {

    private static final String SERVER = "xml.tax7.ru";//"79.175.38.54";//
    private static final String PORT = "80";
    private static final String URL_ROOT = "http://" + SERVER + ":" + PORT + "/andr";
    
    private static final String URL_CHECK_NUMBER		= URL_ROOT + "/check_number.php";
    private static final String URL_PLACE_ORDER_LITE 	= URL_ROOT + "/place_order_lite.php";
    
    private static final int REQUEST_CODE_FILL_IN_DATA = 1;

	Button btnOrder;
	String mNumber;
	String mName;
	boolean mIsFirstTime;
	SharedPreferences mSettings;
	BroadcastReceiver mNumberVerifiedReceiver;
    XmlPullParser mXmlResponseParser = null;
	AlertDialog.Builder mDialog;
	CheckNumberTask mCheckNumberTask;

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		btnOrder = (Button) findViewById(R.id.btnOrder);
		btnOrder.setOnClickListener(this);
    	mSettings = getSharedPreferences("TaxiLitePrefs", MODE_PRIVATE);
    	mNumberVerifiedReceiver = null;
        XmlPullParserFactory factory;
		try {
			factory = XmlPullParserFactory.newInstance();
	        factory.setNamespaceAware(true);
	        mXmlResponseParser = factory.newPullParser();
		} catch (XmlPullParserException e) {
		}
		mDialog = new AlertDialog.Builder(this);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
	        case R.id.action_settings:
	            startActivity(new Intent(this, SettingsActivity.class));
	            return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}
	
	private void startPlacingOrder()
	{
		if (!mSettings.getBoolean("numberIsVerified", false))
		{
			//sendSMS(mNumber, android.os.Build.DEVICE);
			mCheckNumberTask = new CheckNumberTask(this);
			mCheckNumberTask.execute();
		}
		else
			new PlaceOrderTask(this).execute();
	}
	

	@Override
	public void onClick(View source) {
		if (source.equals(btnOrder))
		{
			if (mIsFirstTime)
			{
				Intent intent = new Intent(this, SettingsActivity.class);
				intent.putExtra("isFirstTime", true);
				intent.putExtra("startedForResult", true);
				startActivityForResult(intent, REQUEST_CODE_FILL_IN_DATA);
			}
			else
				startPlacingOrder();

			//Toast.makeText(this, mNumber, Toast.LENGTH_LONG).show();
		}
	}
	
    @Override
    protected void onResume() 
    {
    	super.onResume();
    	
    	mName = mSettings.getString("name", "");
    	mNumber = mSettings.getString("number", "").replaceAll("[^0-9+]", "");    	
    	mIsFirstTime = false;
    	if (mNumber.length() == 0)
    		mIsFirstTime = true;
    }
    
//    private void sendSMS(String number, String message)
//    {
//        String SENT = "SMS_SENT";
//        String DELIVERED = "SMS_DELIVERED";
//
//        PendingIntent sentPI = PendingIntent.getBroadcast(this, 0,
//            new Intent(SENT), 0);
//
//        PendingIntent deliveredPI = PendingIntent.getBroadcast(this, 0,
//            new Intent(DELIVERED), 0);
//
//        //---when the SMS has been sent---
//        registerReceiver(new BroadcastReceiver(){
//            @Override
//            public void onReceive(Context arg0, Intent arg1) {
//                switch (getResultCode())
//                {
//                    case Activity.RESULT_OK:
//                        break;
//                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
//                        Toast.makeText(getBaseContext(), "Generic failure", 
//                                Toast.LENGTH_SHORT).show();
//                        break;
//                    case SmsManager.RESULT_ERROR_NO_SERVICE:
//                        Toast.makeText(getBaseContext(), "No service", 
//                                Toast.LENGTH_SHORT).show();
//                        break;
//                    case SmsManager.RESULT_ERROR_NULL_PDU:
//                        Toast.makeText(getBaseContext(), "Null PDU", 
//                                Toast.LENGTH_SHORT).show();
//                        break;
//                    case SmsManager.RESULT_ERROR_RADIO_OFF:
//                        Toast.makeText(getBaseContext(), "Radio off", 
//                                Toast.LENGTH_SHORT).show();
//                        break;
//                }
//            }
//        }, new IntentFilter(SENT));
//
//        //---when the SMS has been delivered---
//        registerReceiver(new BroadcastReceiver(){
//            @Override
//            public void onReceive(Context arg0, Intent arg1) {
//                switch (getResultCode())
//                {
//                    case Activity.RESULT_OK:
//                        break;
//                    case Activity.RESULT_CANCELED:
//                        Toast.makeText(getBaseContext(), "SMS not delivered", 
//                                Toast.LENGTH_SHORT).show();
//                        break;                        
//                }
//            }
//        }, new IntentFilter(DELIVERED));
//        
//        SmsManager sms = SmsManager.getDefault();
//        sms.sendTextMessage(mNumber, null, android.os.Build.DEVICE, sentPI, deliveredPI);            	
//    }

    @Override
    protected void onDestroy() {
    	super.onDestroy();
    	// выключаем BroadcastReceiver
    	if (mNumberVerifiedReceiver != null)
    		unregisterReceiver(mNumberVerifiedReceiver);
    }
    
    private boolean checkNumber()
    {
    	boolean result = false;
    	
        mNumberVerifiedReceiver = new BroadcastReceiver() 
		{
			public void onReceive(Context context, Intent intent) 
			{
				if (mCheckNumberTask.isRunning())
				{
					mCheckNumberTask.stop();
					String number = intent.getStringExtra("number");
					System.out.println("Comparing " + mNumber + " to " + number);
					if (mNumber.equals(number))
					{
						SharedPreferences.Editor settingsEditor = mSettings.edit();
						settingsEditor.putBoolean("numberIsVerified", true);
						settingsEditor.commit();
						new PlaceOrderTask(context).execute();
					}
				}
			}
	    };
	    // создаем фильтр для BroadcastReceiver
	    IntentFilter intFilt = new IntentFilter(SmsReceiver.NUMBER_VERIFIED);
	    // регистрируем BroadcastReceiver
	    registerReceiver(mNumberVerifiedReceiver, intFilt);

		try
		{
		    System.out.println(mNumber);
		    String requestString = String.format("?number=%s&content=%s", URLEncoder.encode("+7" + mNumber, "UTF-8"), URLEncoder.encode(android.os.Build.DEVICE + "_" + mNumber, "UTF-8"));
		    System.out.println(requestString);
			URL placeOrderUrl = new URL(URL_CHECK_NUMBER + requestString);
			result = parseResponse(placeOrderUrl.openStream());
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return result;
    }

    private boolean placeOrder()
    {
    	boolean result = false;
    	
		try
		{
		    System.out.println(mNumber);
	    	String requestString = String.format("?number=%s&name=%s", URLEncoder.encode("+7" + mNumber, "UTF-8"), URLEncoder.encode(mName, "UTF-8"));
		    System.out.println(URL_PLACE_ORDER_LITE + requestString);
			URL placeOrderUrl = new URL(URL_PLACE_ORDER_LITE + requestString);
			result = parseResponse(placeOrderUrl.openStream());
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return result;
    }

	private boolean parseResponse(InputStream response) throws XmlPullParserException, IOException
	{
		mXmlResponseParser.setInput(response, "utf-8");
        int eventType = mXmlResponseParser.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) 
        {
        	if(eventType == XmlPullParser.START_TAG) 
        	{
        		String name = mXmlResponseParser.getName();
        		if (name.equalsIgnoreCase("result"))
        		{
        			for (int i = 0; i < mXmlResponseParser.getAttributeCount(); ++i)
        				if (mXmlResponseParser.getAttributeName(i).equalsIgnoreCase("error"))
        				{
        					int code = Integer.parseInt(mXmlResponseParser.getAttributeValue(i));
        					// TODO: проверять и возвращать код ошибки
        					if (code != 0)
        						return false;
        					//mID = Integer.parseInt(mXmlResponseParser.nextText());
        				}
        		}
        	}
        	eventType = mXmlResponseParser.next();
        }
        System.out.println("Response ok");
		return true;
	}

	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		if (resultCode != RESULT_OK)
			return;
		if (requestCode == REQUEST_CODE_FILL_IN_DATA)
		{
			startPlacingOrder();
		}
	}

	
// =====================================================================================================================	
// ========================================= Asynchronous tasks ========================================================	
// =====================================================================================================================	
	
	private class PlaceOrderTask extends AsyncTask<Void, Void, Boolean>
	{
		private Context mCtx;
		private ProgressDialog mSubmittingOrderDialog;
		
		public PlaceOrderTask(Context ctx)
		{
			mCtx = ctx;
		}
		
		protected void onPreExecute()
		{
			mSubmittingOrderDialog = ProgressDialog.show(mCtx, "", mCtx.getResources().getString(R.string.wait_for_order_submit), true);
		}
		
		protected Boolean doInBackground(Void... params)
		{
			return placeOrder();
		}
		
		protected void onPostExecute(Boolean result)
		{
			//super.onPostExecute(result);
			mSubmittingOrderDialog.dismiss();
			if (result)
			{
				// Показываем диалог, что все ок
				mDialog.setTitle(R.string.title_success);
				mDialog.setMessage(R.string.msg_order_placed_successfully);
				mDialog.setPositiveButton(android.R.string.ok, null);
				mDialog.show();
			}
			else
			{
				// Показываем диалог, что все плохо
				mDialog.setTitle(R.string.title_error);
				mDialog.setMessage(R.string.msg_order_place_error);
				mDialog.setPositiveButton(android.R.string.ok, null);
				mDialog.show();
			}
		}
	}

	private class CheckNumberTask extends AsyncTask<Void, Void, Boolean>
	{
		private Context mCtx;
		private ProgressDialog mCheckingNumberDialog;
		private Timer mCheckNumberTimer;
		private boolean mIsRunning;
		
		public CheckNumberTask(Context ctx)
		{
			mCtx = ctx;
		}
		
		protected void onPreExecute()
		{
			mCheckNumberTimer = new Timer();
			mCheckNumberTimer.schedule(new TimerTask() {
	            @Override
	            public void run() {
	            	runOnUiThread(new Runnable() { public void run(){
		            	// Слишком долго ожидали ответной СМС-ки
		            	mCheckingNumberDialog.dismiss();
		    			mIsRunning = false;
						// Показываем диалог, что все плохо
						mDialog.setTitle(R.string.title_error);
						mDialog.setMessage(R.string.msg_number_check_error);
						mDialog.setPositiveButton(android.R.string.ok, null);
						mDialog.show();	            		
	            	}});
	            }
	    	}, 15000);
			
			mIsRunning = true;
			
			mCheckingNumberDialog = ProgressDialog.show(mCtx, "", mCtx.getResources().getString(R.string.wait_for_number_check), true);
		}
		
		protected Boolean doInBackground(Void... params)
		{
			return checkNumber();
		}
		
		protected void onPostExecute(Boolean result)
		{
			if (!result)
			{
				// Не получили подтверждение от сервера
				mCheckNumberTimer.cancel();
				mCheckingNumberDialog.dismiss();
				// Показываем диалог, что все плохо
				mDialog.setTitle(R.string.title_error);
				mDialog.setMessage(R.string.msg_number_check_error_1);
				mDialog.setPositiveButton(android.R.string.ok, null);
				mDialog.show();
			}
		}
		
		// Вызывается извне при успешном получении ответной СМС
		public void stop()
		{
			mIsRunning = false;
			mCheckNumberTimer.cancel();
			mCheckingNumberDialog.dismiss();
		}
		
		public boolean isRunning()
		{
			return mIsRunning;
		}
	}

}


