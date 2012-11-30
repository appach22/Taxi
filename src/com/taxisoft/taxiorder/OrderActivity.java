package com.taxisoft.taxiorder;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import com.taxisoft.taxiorder.MapActivity.TaxiData;

import android.opengl.Visibility;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
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
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.TimePicker;

public class OrderActivity extends Activity implements OnClickListener, OnCheckedChangeListener {

	private static final int REQUEST_CODE_CURRENT_LOCATION_START = 1;
	private static final int REQUEST_CODE_CURRENT_LOCATION_FINISH = 2;
	private static final int REQUEST_CODE_SHOW_START_ON_THE_MAP = 3;
	private static final int REQUEST_CODE_SHOW_FINISH_ON_THE_MAP = 4;

	Button mBtnCurrentLocationFrom;
	Button mBtnShowOnTheMapFrom;
	Button mBtnCurrentLocationTo;
	Button mBtnShowOnTheMapTo;
	
	TimePicker mTime;
	DatePicker mDate;

	public static final int GROUP_FROM = 0;
	public static final int GROUP_TO = 1;
	public static final int GROUP_OTHER = 2;
	
	final int groupCaptions[] = {R.string.from, R.string.to, R.string.time_and_contacts};
	final int groupViews[] = {R.layout.order_from, R.layout.order_to, R.layout.order_other};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_order);
		
		URL streetsUrl;
		String allStreets = "";
		try {
			streetsUrl = new URL("http://79.175.38.54:4481/get_streets.php");
			allStreets = parseStreetsResponse(streetsUrl.openStream()); 
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		PrepareOrderForm(allStreets.split("\\|"));
	}

	private void PrepareOrderForm(String[] streets)
	{
		View groupContent[] = new View[groupViews.length];
        LayoutInflater infalInflater = (LayoutInflater)getLayoutInflater();
	    ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, streets);
        
        // Start group
        groupContent[GROUP_FROM] = infalInflater.inflate(groupViews[GROUP_FROM], null);
	    AutoCompleteTextView textView = (AutoCompleteTextView)groupContent[GROUP_FROM].findViewById(R.id.edtStreetFrom);
	    textView.setAdapter(adapter);
	    textView.setThreshold(1);
	    mBtnCurrentLocationFrom = (Button)groupContent[GROUP_FROM].findViewById(R.id.btnCurrentLocationFrom); 
	    mBtnCurrentLocationFrom.setOnClickListener(this);
	    mBtnShowOnTheMapFrom = (Button)groupContent[GROUP_FROM].findViewById(R.id.btnShowOnTheMapFrom); 
	    mBtnShowOnTheMapFrom.setOnClickListener(this);
	    
        // Finish group
        groupContent[GROUP_TO] = infalInflater.inflate(groupViews[GROUP_TO], null);
	    textView = (AutoCompleteTextView)groupContent[GROUP_TO].findViewById(R.id.edtStreetTo);
	    textView.setAdapter(adapter);
	    textView.setThreshold(1);
	    mBtnCurrentLocationTo = (Button)groupContent[GROUP_TO].findViewById(R.id.btnCurrentLocationTo); 
	    mBtnCurrentLocationTo.setOnClickListener(this);
	    mBtnShowOnTheMapTo = (Button)groupContent[GROUP_TO].findViewById(R.id.btnShowOnTheMapTo); 
	    mBtnShowOnTheMapTo.setOnClickListener(this);
	    
	    //Other group
	    groupContent[GROUP_OTHER] = infalInflater.inflate(groupViews[GROUP_OTHER], null);
	    CheckBox cbNow = (CheckBox)groupContent[GROUP_OTHER].findViewById(R.id.cbNow);
	    cbNow.setOnCheckedChangeListener(this);
	    cbNow.setChecked(true);
	    mTime = (TimePicker)groupContent[GROUP_OTHER].findViewById(R.id.tpPickupTime);
	    mTime.setIs24HourView(true);
	    mDate = (DatePicker)groupContent[GROUP_OTHER].findViewById(R.id.dpPickupDate);
    	Field f[] = mDate.getClass().getDeclaredFields();
		for (Field field : f) {
			if (field.getName().equals("mYearPicker")) {
				field.setAccessible(true);
				Object yearPicker = new Object();
				try {
					yearPicker = field.get(mDate);
					((View)yearPicker).setVisibility(View.GONE);
				} catch (IllegalArgumentException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	    
	    String captions[] = new String[groupCaptions.length];
	    for (int i = 0; i < groupCaptions.length; ++i)
	    	captions[i] = getResources().getString(groupCaptions[i]);
		ExpandableListAdapter listAdapter = new OrderListAdapter(this, captions, groupContent);
		ExpandableListView lv = (ExpandableListView)findViewById(R.id.lvOrder);
		lv.setAdapter(listAdapter);
		
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
		getMenuInflater().inflate(R.layout.order_from, menu);
		return true;
	}

	@Override
	public void onClick(View view) {
		if (view == mBtnShowOnTheMapFrom || view == mBtnShowOnTheMapTo)
		{
			Intent intent = new Intent(this, MapActivity.class);
			intent. putExtra("Reason", MapActivity.INTENT_SHOW_ON_THE_MAP);
			if (view == mBtnShowOnTheMapFrom)
				startActivityForResult(intent, REQUEST_CODE_SHOW_START_ON_THE_MAP);
			else
				startActivityForResult(intent, REQUEST_CODE_SHOW_FINISH_ON_THE_MAP);
		}
		else if (view == mBtnCurrentLocationFrom || view == mBtnCurrentLocationTo)
		{
			if (MyLocationUtils.storedLocation != null)
			{
				Intent intent = new Intent(this, MapActivity.class);
				intent. putExtra("Reason", MapActivity.INTENT_CURRENT_LOCATION);
				if (view == mBtnCurrentLocationFrom)
					startActivityForResult(intent, REQUEST_CODE_CURRENT_LOCATION_START);
				else
					startActivityForResult(intent, REQUEST_CODE_CURRENT_LOCATION_FINISH);
			}
			else
			{
				// TODO: show error message
			}
		}
	}
	
	private void ShowStreetAddress(String street, int idStreet, int idHouse)
	{
		String addr[] = street.split(",");
		for (int i = 0; i < addr.length; ++i)
			addr[i] = addr[i].trim();
		TextView edtStreet = (TextView)findViewById(idStreet);
		TextView edtHouse = (TextView)findViewById(idHouse);
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