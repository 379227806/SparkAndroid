package com.sparkplatform.ui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.codehaus.jackson.JsonNode;

import android.app.ListActivity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.widget.ListAdapter;
import android.widget.SimpleAdapter;

import com.sparkplatform.api.ApiParameter;
import com.sparkplatform.api.FlexmlsApiClientException;
import com.sparkplatform.api.Response;
import com.sparkplatform.api.SparkClient;
import com.sparkplatform.api.models.Listing;
import com.sparkplatform.api.models.StandardField;
import com.sparkplatform.utils.ListingFormatter;

public class ViewListingActivity extends ListActivity {

	// class vars *************************************************************
	
	private static final String TAG = "ViewListingActivity";
	
	// instance vars **********************************************************
	
	private Listing summaryListing;
	private JsonNode listing;
	private Collection<StandardField.Field> sortedStandardFields;
	
	// interface **************************************************************
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_view_listing);
		
		Intent i = getIntent();
		summaryListing = (Listing)i.getSerializableExtra(UIConstants.EXTRA_LISTING);		
    	Log.d(TAG, "summaryListing>" + summaryListing);

		 new ViewListingTask().execute();
		 new StandardFieldsTask().execute();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_view_listing, menu);
		return true;
	}

	private void loadViewListing()
	{
    	Log.d(TAG, "loadViewListing");
    	
    	// set summary line
		 List<Map<String,String>> list = new ArrayList<Map<String,String>>();
		 
		 ActivityHelper.addListLine(list, ListingFormatter.getListingSubtitle(summaryListing), ListingFormatter.getListingTitle(summaryListing));
		 
		 // set standard fields
		 JsonNode standardFields = listing.get("StandardFields");
		 for(StandardField.Field field : sortedStandardFields)
		 {
			 String key = field.getResourceUri().substring("/v1/standardfields/".length());
			 JsonNode value = standardFields.get(key);
			 if(value != null)
				 ActivityHelper.addListLine(list, field.getLabel(), value.getValueAsText());
		 }

		 ListAdapter adapter = new SimpleAdapter(getApplicationContext(), 
				 list,
				 android.R.layout.two_line_list_item, 
				 new String[] {"line1", "line2"}, 
				 new int[] {android.R.id.text1, android.R.id.text2});
		 setListAdapter(adapter);
	}
	
	 private class ViewListingTask extends AsyncTask<Void, Void, Response> {
	     protected Response doInBackground(Void... v) {
				   
	    	 Map<ApiParameter,String> parameters = new HashMap<ApiParameter,String>();
	    	 parameters.put(ApiParameter._limit, "1");
	    	 parameters.put(ApiParameter._expand, "Photos");
	    	 parameters.put(ApiParameter._filter, "ListingId Eq '" + summaryListing.getStandardFields().getListingId() + "'");
	    	 
	    	 Response r = null;
	    	 try
	    	 {
	    		 r = SparkClient.getInstance().get("/listings",parameters);
	    		 Log.d(TAG, "success>" + r.isSuccess());
	    	 }
	    	 catch(FlexmlsApiClientException e)
	    	 {
	    		 Log.e(TAG, "/listings exception>", e);
	    	 }
	    	 
	    	 return r;
	     }
	     
	     protected void onPostExecute(Response r) {
	    	 Log.d(TAG,"/listings>" + r.getResultsJSONString());
	    	 
	    	 listing = r.getFirstResult();

	    	 if(listing != null && sortedStandardFields != null)
	    		 loadViewListing();
		 }
	 }
	 
	 private class StandardFieldsTask extends AsyncTask<Void, Void, Response> {
		 protected Response doInBackground(Void... v) {

			 Response r = null;
			 try
			 {
				 r = SparkClient.getInstance().get("/standardfields", null);
				 Log.d(TAG, "success>" + r.isSuccess());
			 }
			 catch(FlexmlsApiClientException e)
			 {
				 Log.e(TAG, "/standardfields exception>", e);
			 }

			 return r;
		 }

		 protected void onPostExecute(Response r) {
			 Log.d(TAG,"/standardfields>" + r.getResultsJSONString());
			 
    		 try {
				List<StandardField> standardFields = r.getResults(StandardField.class);
				
				Log.d(TAG, standardFields.toString()); 
				
				if(standardFields != null && standardFields.size() > 0)
				{
					StandardField standardField = standardFields.get(0);
					// iterate over map to build
				    Map<String,StandardField.Field> sortedMap = new TreeMap<String,StandardField.Field>();
				    Map<String,StandardField.Field> fieldMap = standardField.getFieldMap();
					for(String key : fieldMap.keySet())
					{
						StandardField.Field field = fieldMap.get(key);
						sortedMap.put(field.getLabel(), field);
					}
					
					sortedStandardFields = sortedMap.values();
				}
				
				// sort standard fields map by label
				
				if(listing != null && sortedStandardFields != null)
					loadViewListing();
			} catch (FlexmlsApiClientException e) {
	    		 Log.e(TAG,"StandardField JSON binding exception", e);
			}
		 }
	 }

}
