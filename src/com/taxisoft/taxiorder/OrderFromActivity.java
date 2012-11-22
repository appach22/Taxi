package com.taxisoft.taxiorder;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

public class OrderFromActivity extends Activity implements OnClickListener {

	private static final String[] COUNTRIES = new String[] {
        "Belgium", "France", "Italy", "Germany", "Spain", "Finland"
    };
	
	private static final int REQUEST_CODE_CURRENT_LOCATION = 1;
	private static final int REQUEST_CODE_SHOW_ON_THE_MAP = 2;
	
	Button mBtnCurrentLocation;
	Button mBtnShowOnTheMap;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_order_from);
		
	    ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, COUNTRIES);
	    AutoCompleteTextView textView = (AutoCompleteTextView)findViewById(R.id.edtStreetFrom);
	    textView.setAdapter(adapter);
	    textView.setThreshold(1);

	    mBtnCurrentLocation = (Button)findViewById(R.id.btnCurrentLocation); 
	    mBtnCurrentLocation.setOnClickListener(this);
	    mBtnShowOnTheMap = (Button)findViewById(R.id.btnShowOnTheMap); 
	    mBtnShowOnTheMap.setOnClickListener(this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.layout.activity_order_from, menu);
		return true;
	}

	@Override
	public void onClick(View view) {
		if (view == mBtnShowOnTheMap)
		{
			Intent intent = new Intent(this, MapActivity.class);
			intent. putExtra("Reason", MapActivity.INTENT_SHOW_ON_THE_MAP);
			startActivityForResult(intent, REQUEST_CODE_SHOW_ON_THE_MAP);
		}
		else if (view == mBtnCurrentLocation)
		{
			Intent intent = new Intent(this, MapActivity.class);
			intent. putExtra("Reason", MapActivity.INTENT_CURRENT_LOCATION);
			startActivityForResult(intent, REQUEST_CODE_CURRENT_LOCATION);
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		if (resultCode != RESULT_OK)
			return;
		if (requestCode == REQUEST_CODE_SHOW_ON_THE_MAP ||
			requestCode == REQUEST_CODE_CURRENT_LOCATION)
		{
			String street = data.getStringExtra("Street");
			String addr[] = street.split(",");
			for (int i = 0; i < addr.length; ++i)
				addr[i] = addr[i].trim();
			TextView edtStreet = (TextView)findViewById(R.id.edtStreetFrom);
			TextView edtHouse = (TextView)findViewById(R.id.edtHouse);
			if (addr.length > 0)
				edtStreet.setText(addr[0]);
			if (addr.length > 1)
				edtHouse.setText(addr[1]);
			else
				edtHouse.setText("");
		}
	}

}
