package com.taxisoft.taxiorder;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.TextView;

public class OrderListAdapter extends BaseExpandableListAdapter {

	private Context mContext;
	private String mCaptions[];
	private View mContent[];

	public OrderListAdapter(Context ctx, String[] captions, View[] content)
	{
		mContext = ctx;
		mCaptions = captions;
		mContent = content;
	}
	
	@Override
	public Object getChild(int groupPosition, int childPosition) {
		return mContent[childPosition];
	}

	@Override
	public long getChildId(int groupPosition, int childPosition) {
		return childPosition;
	}

	@Override
	public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
            View convertView, ViewGroup parent) {
		convertView = mContent[groupPosition];
		return convertView;
	}

    public TextView getGenericView() {
        // Layout parameters for the ExpandableListView
        AbsListView.LayoutParams lp = new AbsListView.LayoutParams(
                ViewGroup.LayoutParams.FILL_PARENT, 64);

        TextView textView = new TextView(mContext);
        textView.setLayoutParams(lp);
        // Center the text vertically
        textView.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
        // Set the text starting position
        textView.setPadding(64, 0, 0, 0);
        return textView;
    }

	@Override
	public int getChildrenCount(int groupPosition) {
		return 1;
	}

	@Override
	public Object getGroup(int groupPosition) {
		return mCaptions[groupPosition];
	}

	@Override
	public int getGroupCount() {
		return mCaptions.length;
	}

	@Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

	@Override
	public View getGroupView(int groupPosition, boolean isExpanded, 
							 View convertView, ViewGroup parent) {
        TextView textView = getGenericView();
        textView.setText(getGroup(groupPosition).toString());
        return textView;
	}

	@Override
	public boolean hasStableIds() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isChildSelectable(int arg0, int arg1) {
		// TODO Auto-generated method stub
		return false;
	}

}
