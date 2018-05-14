package com.ibisek.outlanded;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.ibisek.outlanded.components.ProximityDisplayComponent;
import com.ibisek.outlanded.navigation.OrientationSensorSource;
import com.ibisek.outlanded.navigation.POI;
import com.ibisek.outlanded.navigation.gps.GPSReader2;
import com.ibisek.outlanded.navigation.proximity.ProximityListener;
import com.ibisek.outlanded.navigation.proximity.ProximitySource;

/**
 * @author ibisek
 * @version 2013-09-20
 */
public class ProximityDisplayActivity extends Activity {

	private ProximitySource proximitySource;
	private MyProximityListener myProximityListener;
	private MyOrientationListener myOrientationListener;

	private MyListItemAdapter listItemAdapter;
	private boolean noValuesInList = true;
	private float deviceAzimuthDegrees = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_proximity_display);

		proximitySource = ProximitySource.getInstance();
		myProximityListener = new MyProximityListener();

		myOrientationListener = new MyOrientationListener();

		final ListView listView = (ListView) findViewById(R.id.listview);
		listView.setOnItemClickListener(new MyOnClickListener());
		listItemAdapter = new MyListItemAdapter(this);
		listView.setAdapter(listItemAdapter);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.proximity_display, menu);
		return true;
	}

	@Override
	public void onResume() {
		super.onResume();
		
		GPSReader2.getInstance(this).resumeUsingGps();

		// if not resuming on request from the MainActivity (which sometimes happens
		// on application start), navigate to MainActivity:
		if (getIntent().getExtras() == null || !getIntent().getExtras().containsKey(MainActivity.KEY_REQUEST_CODE)
				|| MainActivity.SELECT_PROXIMITY_ITEM_REQ_CODE != getIntent().getExtras().getInt(MainActivity.KEY_REQUEST_CODE)) {
			Intent mainIntent = new Intent(ProximityDisplayActivity.this, MainActivity.class);
			startActivity(mainIntent);
		}

		proximitySource.addListener(myProximityListener);
		OrientationSensorSource.getInstance(this).addListener(myOrientationListener);
		OrientationSensorSource.getInstance(this).resume();

		// update the view immediately:
		if (proximitySource.getNearestPointsList() != null)
			myProximityListener.notify(proximitySource.getNearestPointsList());
	}

	@Override
	public void onPause() {
		super.onPause();
		proximitySource.removeListener(myProximityListener);
		OrientationSensorSource.getInstance(this).removeListener(myOrientationListener);
		OrientationSensorSource.getInstance(this).pause(); // do not stop - used by the main screen
		
		GPSReader2.getInstance(this).stopUsingGPS();
	}

	public void onBackPressed() {
		// tell the main screen that NO POI has been selected (clear selectedPoi):
		Intent mainIntent = new Intent(this, MainActivity.class);
		setResult(RESULT_OK, mainIntent); 
		mainIntent.putExtra(MainActivity.KEY_SELECTED_POI_INDEX, -1);
		finish();
	}

	private class MyOnClickListener implements OnItemClickListener {

		@Override
		public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
			// tell the main screen which POI has the user selected:
			Intent mainIntent = new Intent(ProximityDisplayActivity.this, MainActivity.class);
			setResult(RESULT_OK, mainIntent); 
			mainIntent.putExtra(MainActivity.KEY_SELECTED_POI_INDEX, position);
			finish();
		}
	}

	private class MyProximityListener implements ProximityListener {

		@Override
		public void notify(List<POI> nearestPoints) {
			listItemAdapter.setValues(nearestPoints);
			listItemAdapter.notifyDataSetChanged();
		}

	}

	private class MyOrientationListener implements SensorEventListener {

		@Override
		public void onAccuracyChanged(Sensor arg0, int arg1) {
			// not used
		}

		@Override
		public void onSensorChanged(SensorEvent event) {
			float newHeading = event.values[0];
			if (!noValuesInList && Math.abs(deviceAzimuthDegrees - newHeading) > 1) {
				// at least one degree change
				deviceAzimuthDegrees = newHeading;
				listItemAdapter.notifyDataSetChanged();
			}
		}
	}

	private class MyListItemAdapter extends ArrayAdapter<String> {

		private Context context;
		private List<POI> values;

		public MyListItemAdapter(Context context) {
			// new ArrayList<String>(0)
			super(context, android.R.layout.simple_list_item_1, new ArrayList<String>(Arrays.asList(new String[] { "to display the wait message" })));
			this.context = context;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View rowView = inflater.inflate(R.layout.proximity_disp_list_item, parent, false);
			TextView line1 = (TextView) rowView.findViewById(R.id.firstLine);
			TextView line2 = (TextView) rowView.findViewById(R.id.secondLine);
			ImageView compass = (ImageView) rowView.findViewById(R.id.icon);
			ImageView sizeIcon = (ImageView) rowView.findViewById(R.id.sizeIcon);

			if (values != null && position < values.size()) {
				POI poi = values.get(position);

				// set icon:
				ProximityDisplayComponent.configureSizeIcon(sizeIcon, poi);

				// set text:
				line1.setText(ProximityDisplayComponent.getTextForLine(context, 1, poi));
				line2.setText(ProximityDisplayComponent.getTextForLine(context, 2, poi));

				compass.setImageResource(R.drawable.compass);

				// TODO overit, jestli ma vliv:
				if (rowView.getVisibility() == View.VISIBLE) {
					float heading = (360 + (poi.getBearingFromOrigin() - deviceAzimuthDegrees)) % 360;

					float rotateFrom = heading;
					float rotateTo = heading;

					RotateAnimation r = new RotateAnimation(rotateFrom, rotateTo, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
					r.setFillAfter(true); // keep the final rotation
					compass.startAnimation(r);
				}

			} else {
				line1.setText(context.getString(R.string.proximity_display_line1_default_text));
				line2.setText(context.getString(R.string.proximity_display_line2_default_text));

				float rotateFrom = 0;
				float rotateTo = 360;

				RotateAnimation r = new RotateAnimation(rotateFrom, rotateTo, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
				r.setDuration(2000);
				r.setRepeatCount(Animation.INFINITE);
				compass.startAnimation(r);
			}

			return rowView;
		}

		public void setValues(List<POI> values) {
			if (values != null && values.size() > 0) {
				noValuesInList = false;
				this.values = values;

				// pokud clear() vyhodi UnsupportedOperationException, tak je to tim, ze
				// kolekce/pole co je v kontruktoru super.ArrayAdapter-u nema na sobe
				// clear(!)
				super.clear();

				for (POI poi : values)
					super.add(poi.getName());

			} else {
				noValuesInList = true;
			}
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public boolean hasStableIds() {
			return true;
		}

	}
}
