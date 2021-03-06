package com.taxisoft.taxi7;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;


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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.googlecode.androidannotations.annotations.AfterViews;
import com.googlecode.androidannotations.annotations.Click;
import com.googlecode.androidannotations.annotations.EActivity;
import com.googlecode.androidannotations.annotations.ViewById;

@EActivity(R.layout.activity_order)
public class OrderActivity extends Activity implements OnClickListener, OnCheckedChangeListener {
	
	private static final int REQUEST_CODE_CURRENT_LOCATION_START = 1;
	private static final int REQUEST_CODE_CURRENT_LOCATION_FINISH = 2;
	private static final int REQUEST_CODE_SHOW_START_ON_THE_MAP = 3;
	private static final int REQUEST_CODE_SHOW_FINISH_ON_THE_MAP = 4;

	Button btnCurrentLocationFrom;
	Button btnShowOnTheMapFrom;
	Button btnCurrentLocationTo;
	Button btnShowOnTheMapTo;
	
	@ViewById
	ExpandableListView lvOrder;
	@ViewById
	Button btnPlaceOrder;
	
	TimePicker mTime;
	DatePicker mDate;

	String mCity;

	AutoCompleteTextView mStreetFrom = null;
	AutoCompleteTextView mStreetTo = null;
	
	Order mOrder = null;
	
	AlertDialog.Builder mDialog;
	
	BroadcastReceiver mOrderStateReceiver;

	public static final int GROUP_FROM = 0;
	public static final int GROUP_TO = 1;
	public static final int GROUP_OTHER = 2;
	
	final int mGroupCaptions[] = {R.string.from, R.string.to, R.string.time_and_contacts};
	final int mGroupViews[] = {R.layout.order_from, R.layout.order_to, R.layout.order_other};
	View mGroupContents[] = new View[mGroupViews.length];

	final long MILLISECONDS_IN_MONTH = 30L * 24 * 3600 * 1000;

	
	private class LoadStreetsTask extends AsyncTask<Void, Void, Void>
	{
		private Context context;
		private String allStreets;
		
		public LoadStreetsTask(Context ctx)
		{
			context = ctx;
		}
		
		protected Void doInBackground(Void... params)
		{
    		allStreets = "";
        	SharedPreferences settings = getSharedPreferences("TaxiPrefs", MODE_PRIVATE);
        	Date lastStreetsSyncDate = new Date(settings.getLong("StreetsSyncTime", 0));
        	if ((new Date()).getTime() - lastStreetsSyncDate.getTime() < MILLISECONDS_IN_MONTH)
        		allStreets = settings.getString("Streets", "");	
        	else
        	{
    			URL streetsUrl;
    			try {
    				streetsUrl = new URL(MapActivity.URL_STREETS);
    				allStreets = parseStreetsResponse(streetsUrl.openStream());
    				SharedPreferences.Editor editor = settings.edit();
    				editor.putLong("StreetsSyncTime", (new Date()).getTime());
    				editor.putString("Streets", allStreets);
    				editor.commit();
    			} catch (MalformedURLException e) {
    				e.printStackTrace();
    			} catch (IOException e) {
    				e.printStackTrace();
    			}
        	}
			return null;
		}
		
		protected void onPostExecute(Void result)
		{
		    ArrayAdapter<String> adapter = new ArrayAdapter<String>(context, android.R.layout.simple_dropdown_item_1line, allStreets.split("\\|"));
		    while (mStreetFrom == null || mStreetTo == null)
				try {
					synchronized(this){
	                    wait(10);
	                }
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
        	mStreetFrom.setAdapter(adapter);
        	mStreetTo.setAdapter(adapter);
		}
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mCity = getResources().getString(R.string.kursk);
		mDialog = new AlertDialog.Builder(this);
		new LoadStreetsTask(this).execute();

		mOrderStateReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent)
			{
				Toast.makeText(context, "state of " + intent.getStringExtra("id") + " is " + String.valueOf(intent.getIntExtra("state", 0)), Toast.LENGTH_SHORT).show();
			}
		};
	    IntentFilter filter = new IntentFilter(OrderStateService.BROADCAST_ACTION);
	    registerReceiver(mOrderStateReceiver, filter);
	}

	@Override
	protected void onDestroy() {
	    super.onDestroy();
	    unregisterReceiver(mOrderStateReceiver);
	}
	
	@AfterViews
	void prepareOrderForm()
	{
        LayoutInflater infalInflater = (LayoutInflater)getLayoutInflater();
        
        // Start group
        mGroupContents[GROUP_FROM] = infalInflater.inflate(mGroupViews[GROUP_FROM], null);
	    mStreetFrom = (AutoCompleteTextView)mGroupContents[GROUP_FROM].findViewById(R.id.edtStreetFrom);
	    mStreetFrom.setThreshold(1);
	    btnCurrentLocationFrom = (Button)mGroupContents[GROUP_FROM].findViewById(R.id.btnCurrentLocationFrom); 
	    btnCurrentLocationFrom.setOnClickListener(this);
	    btnShowOnTheMapFrom = (Button)mGroupContents[GROUP_FROM].findViewById(R.id.btnShowOnTheMapFrom); 
	    btnShowOnTheMapFrom.setOnClickListener(this);
	    
        // Finish group
        mGroupContents[GROUP_TO] = infalInflater.inflate(mGroupViews[GROUP_TO], null);
        mStreetTo = (AutoCompleteTextView)mGroupContents[GROUP_TO].findViewById(R.id.edtStreetTo);
        mStreetTo.setThreshold(1);
	    btnCurrentLocationTo = (Button)mGroupContents[GROUP_TO].findViewById(R.id.btnCurrentLocationTo); 
	    btnCurrentLocationTo.setOnClickListener(this);
	    btnShowOnTheMapTo = (Button)mGroupContents[GROUP_TO].findViewById(R.id.btnShowOnTheMapTo); 
	    btnShowOnTheMapTo.setOnClickListener(this);
	    
	    //Other group
	    mGroupContents[GROUP_OTHER] = infalInflater.inflate(mGroupViews[GROUP_OTHER], null);
	    CheckBox cbNow = (CheckBox)mGroupContents[GROUP_OTHER].findViewById(R.id.cbNow);
	    cbNow.setOnCheckedChangeListener(this);
	    cbNow.setChecked(true);
	    mTime = (TimePicker)mGroupContents[GROUP_OTHER].findViewById(R.id.tpPickupTime);
	    mTime.setIs24HourView(true);
	    mDate = (DatePicker)mGroupContents[GROUP_OTHER].findViewById(R.id.dpPickupDate);
    	Field f[] = mDate.getClass().getDeclaredFields();
		for (Field field : f) {
			if (field.getName().equals("mYearPicker")) {
				field.setAccessible(true);
				Object yearPicker = new Object();
				try {
					yearPicker = field.get(mDate);
					((View)yearPicker).setVisibility(View.GONE);
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
			}
		}
	    
	    String captions[] = new String[mGroupCaptions.length];
	    for (int i = 0; i < mGroupCaptions.length; ++i)
	    	captions[i] = getResources().getString(mGroupCaptions[i]);
		ExpandableListAdapter listAdapter = new OrderListAdapter(this, captions, mGroupContents);
		lvOrder.setAdapter(listAdapter);		
	}
	
    private String parseStreetsResponse(InputStream response)
    {
    	boolean inStreets = false;
        XmlPullParserFactory factory;
    	String streets = "";
    	
    	try {
			factory = XmlPullParserFactory.newInstance();
	        factory.setNamespaceAware(true);
	        XmlPullParser streetsResponseParser = factory.newPullParser();
    		streetsResponseParser.setInput(response, "utf-8");
	        int eventType = streetsResponseParser.getEventType();
	        while (eventType != XmlPullParser.END_DOCUMENT) {
	        	if (streets.length() > 0)
	        		streets += "|";
	        	if(eventType == XmlPullParser.START_TAG) {
	        		if (streetsResponseParser.getName().equalsIgnoreCase("streets"))
	        			inStreets = true;
	        		else if (inStreets && streetsResponseParser.getName().equalsIgnoreCase("street"))
	        			streets += streetsResponseParser.nextText();
	        	}
	        	eventType = streetsResponseParser.nextTag();
	        }
						
		} catch (XmlPullParserException e) {
		} catch (IOException e) {
		}
    	return streets;
    }
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		//getMenuInflater().inflate(R.layout.order_from, menu);
		//return true;
		return false;
	}

	@Override
	public void onClick(View view) {
		if (view == btnShowOnTheMapFrom || view == btnShowOnTheMapTo)
		{
			Intent intent = new Intent(this, MapActivity_.class);
			intent.putExtra("Reason", MapActivity.INTENT_SHOW_ON_THE_MAP);
			if (view == btnShowOnTheMapFrom)
				startActivityForResult(intent, REQUEST_CODE_SHOW_START_ON_THE_MAP);
			else
				startActivityForResult(intent, REQUEST_CODE_SHOW_FINISH_ON_THE_MAP);
		}
		else if (view == btnCurrentLocationFrom || view == btnCurrentLocationTo)
		{
			if (MyLocationUtils.storedLocation != null)
			{
				Intent intent = new Intent(this, MapActivity_.class);
				intent.putExtra("Reason", MapActivity.INTENT_CURRENT_LOCATION);
				if (view == btnCurrentLocationFrom)
					startActivityForResult(intent, REQUEST_CODE_CURRENT_LOCATION_START);
				else
					startActivityForResult(intent, REQUEST_CODE_CURRENT_LOCATION_FINISH);
			}
			else
			{
				Toast.makeText(this, R.string.location_is_unknown, Toast.LENGTH_SHORT).show();
			}
		}		
	}
	

	private class PlaceOrderTask extends AsyncTask<Void, Void, Boolean>
	{
		private Context context;
		private Order mOrder;
		private ProgressDialog mSubmittingOrderDialog;
		
		public PlaceOrderTask(Context ctx, Order order)
		{
			context = ctx;
			mOrder = order;
		}
		
		protected void onPreExecute()
		{
			mSubmittingOrderDialog = ProgressDialog.show(context, "", context.getResources().getString(R.string.wait_for_order_submit), true);
		}
		
		protected Boolean doInBackground(Void... params)
		{
			return mOrder.submit();
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
				startService((new Intent(context, OrderStateService.class)).putExtra("id", String.valueOf(mOrder.getID())));
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
	
	
	@Click
	void btnPlaceOrder()
	{
		if (mOrder == null)
			mOrder = new Order();
		
		mOrder.mCity = mCity;
		mOrder.mCustomerName = ((EditText)mGroupContents[GROUP_OTHER].findViewById(R.id.edtClientName)).getText().toString().trim();
		mOrder.mStreetFrom = ((EditText)mGroupContents[GROUP_FROM].findViewById(R.id.edtStreetFrom)).getText().toString().trim();
		mOrder.mHouseFrom = ((EditText)mGroupContents[GROUP_FROM].findViewById(R.id.edtHouseFrom)).getText().toString().trim();
		mOrder.mEntranceFrom = ((EditText)mGroupContents[GROUP_FROM].findViewById(R.id.edtEntranceFrom)).getText().toString().trim();
		mOrder.mLandmarkFrom = ((EditText)mGroupContents[GROUP_FROM].findViewById(R.id.edtOtherLandmarkFrom)).getText().toString().trim();
		mOrder.mStreetTo = ((EditText)mGroupContents[GROUP_TO].findViewById(R.id.edtStreetTo)).getText().toString().trim();
		mOrder.mHouseTo = ((EditText)mGroupContents[GROUP_TO].findViewById(R.id.edtHouseTo)).getText().toString().trim();
		mOrder.mEntranceTo = ((EditText)mGroupContents[GROUP_TO].findViewById(R.id.edtEntranceTo)).getText().toString().trim();
		mOrder.mLandmarkTo = ((EditText)mGroupContents[GROUP_TO].findViewById(R.id.edtOtherLandmarkTo)).getText().toString().trim();
		mOrder.mTime.set(mDate.getYear(), mDate.getMonth(), mDate.getDayOfMonth(), mTime.getCurrentHour(), mTime.getCurrentMinute());
		mOrder.mPhoneNumber = ((EditText)mGroupContents[GROUP_OTHER].findViewById(R.id.edtPhoneNumber)).getText().toString().trim(); 
		
		// TODO: втащить валидацию в AcyncTask. Показывать прогресс...
		if (!mOrder.validate())
		{
			mDialog.setTitle(R.string.title_error);
			mDialog.setMessage(R.string.msg_order_data_incomplete);
			mDialog.setPositiveButton(android.R.string.ok, null);
			mDialog.show();
			return;
		}

		new PlaceOrderTask(this, mOrder).execute();
		
	}
	
	private void ShowStreetAddress(String street, int idStreet, int idHouse)
	{
		String addr[] = street.split(",");
		for (int i = 0; i < addr.length; ++i)
			addr[i] = addr[i].trim();
		EditText edtStreet = (EditText)findViewById(idStreet);
		EditText edtHouse = (EditText)findViewById(idHouse);
		if (addr.length > 0)
			edtStreet.setText(addr[0]);
		if (addr.length > 1)
			edtHouse.setText(addr[1]);
		else
			edtHouse.setText("");
		
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		if (resultCode != RESULT_OK)
			return;
		if (requestCode == REQUEST_CODE_SHOW_START_ON_THE_MAP ||
			requestCode == REQUEST_CODE_CURRENT_LOCATION_START)
		{
			String street = data.getStringExtra("Street");
			ShowStreetAddress(street, R.id.edtStreetFrom, R.id.edtHouseFrom);
		}
		else if (requestCode == REQUEST_CODE_SHOW_FINISH_ON_THE_MAP ||
			     requestCode == REQUEST_CODE_CURRENT_LOCATION_FINISH)
		{
			String street = data.getStringExtra("Street");
			ShowStreetAddress(street, R.id.edtStreetTo, R.id.edtHouseTo);
		}
	}

	@Override
	public void onCheckedChanged(CompoundButton view, boolean checked) {
		//CheckBox cbNow = (CheckBox)view;
		if (checked)
		{
			mTime.setVisibility(View.GONE);
			mDate.setVisibility(View.GONE);
		}
		else
		{
			mTime.setVisibility(View.VISIBLE);
			mDate.setVisibility(View.VISIBLE);
		}
	}

}
