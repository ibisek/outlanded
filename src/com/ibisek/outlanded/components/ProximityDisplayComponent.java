package com.ibisek.outlanded.components;

import android.content.Context;
import android.graphics.Rect;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ibisek.outlanded.R;
import com.ibisek.outlanded.navigation.OrientationSensorSource;
import com.ibisek.outlanded.navigation.POI;
import com.ibisek.outlanded.navigation.POI.TYPE;

/**
 * A proximity display element for the main activity. It uses same layout as the proximity display list.
 * 
 * The component consists of: (1) this class (3) res/layouts/proximity_disp_list_item.xml
 * 
 * @author ibisek
 * @version 2013-11-08
 */
public class ProximityDisplayComponent extends LinearLayout {
	public final static int PEAK_ICON = R.drawable.mountain;
	public final static int[] SIZE_ICONS = { R.drawable.size_1_house, R.drawable.size_2_town, R.drawable.size_3_city };

	private final static String TAG = ProximityDisplayComponent.class.getSimpleName();

	private Context context;

	private TextView firstLine, secondLine;
	private ImageView compass, sizeIcon;

	private POI poi;
	private float deviceAzimuthDegrees = 0;
	private MyOrientationListener myOrientationListener = new MyOrientationListener();

	public ProximityDisplayComponent(Context context) {
		this(context, null);
		this.context = context;
		Log.d(TAG, "constructor #1");
	}

	public ProximityDisplayComponent(Context context, AttributeSet attrs, int defStyle) {
		this(context, attrs);
		this.context = context;
		Log.d(TAG, "constructor #2");
	}

	public ProximityDisplayComponent(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.context = context;
		Log.d(TAG, "constructor #3");

		setClickable(true);
		Log.d(TAG, "editMode = " + isInEditMode());
		LayoutParams lp = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
		setLayoutParams(lp);

		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		inflater.inflate(R.layout.proximity_disp_list_item, this);
	}

	@Override
	protected void onFinishInflate() {
		Log.d(TAG, "onFinishInflate()");
		super.onFinishInflate();
		if (isInEditMode())
			return;

		init();
	}

	@Override
	protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
		super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
		Log.d(TAG, "onFocusChanged()");
		// zda se, ze toto se nikdy nevola
	}

	@Override
	public void onWindowFocusChanged(boolean hasWindowFocus) {
		super.onWindowFocusChanged(hasWindowFocus);
		Log.d(TAG, "onWindowFocusChanged(): hasWindowFocus="+hasWindowFocus);
		// activate/deactivate compass:

		if (hasWindowFocus) {
			OrientationSensorSource.getInstance(context).addListener(myOrientationListener);
			OrientationSensorSource.getInstance(context).resume();
			
		} else {
			OrientationSensorSource.getInstance(context).removeListener(myOrientationListener);
			OrientationSensorSource.getInstance(context).pause();
		}

	}

	@Override
	public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);

		// if (!isInEditMode()) {
		// super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		//
		// } else {
		// // setMeasuredDimension(LayoutParams.WRAP_CONTENT,
		// // LayoutParams.WRAP_CONTENT);
		// System.out.println(String.format("XXX onMeasure(): %s %s",
		// widthMeasureSpec, heightMeasureSpec));
		//
		// int width = 100;
		// int height = 100;
		// if (firstLine != null && secondLine != null && icon != null) {
		// int w1 = firstLine.getWidth();
		// int w2 = secondLine.getWidth();
		// width = (w1 > w2 ? w1 : w2) + icon.getWidth();
		//
		// int h1 = icon.getHeight();
		// int h2 = firstLine.getHeight() + secondLine.getHeight();
		// height = (h1 > h2 ? h1 : h2);
		// }
		//
		// setMeasuredDimension(width, height);
		// }
	}

	private void init() {
		Log.d(TAG, "init()");

		firstLine = (TextView) findViewById(R.id.firstLine);
		secondLine = (TextView) findViewById(R.id.secondLine);
		compass = (ImageView) findViewById(R.id.icon);
		sizeIcon = (ImageView) findViewById(R.id.sizeIcon);

		notifyDataChanged(); // to repaint the interface
	}

	public void updatePosition(POI poi) {
		this.poi = poi;
		OrientationSensorSource.getInstance(context).resume(); // in case it has been paused
		notifyDataChanged();
	}

	/**
	 * Sets icon type and size for habitable POIs.
	 * 
	 * @param sizeIcon
	 *          icon to be configured
	 * @param poi
	 */
	public static void configureSizeIcon(ImageView sizeIcon, POI poi) {
		POI.TYPE type = poi.getType();

		if (type == TYPE.PEAK) {
			sizeIcon.setVisibility(VISIBLE);
			sizeIcon.setImageResource(PEAK_ICON);

		} else if (type == TYPE.HABITABLE) {
			int size = poi.getSize() - 1;
			int iconIndex = (size < 0 ? 0 : (size >= SIZE_ICONS.length ? SIZE_ICONS.length - 1 : size));
			sizeIcon.setVisibility(VISIBLE);
			sizeIcon.setImageResource(SIZE_ICONS[iconIndex]);

		} else {
			sizeIcon.setVisibility(GONE);
		}
	}

	/**
	 * Used in {@link ProximityDisplayComponent} and array adapter.
	 * 
	 * @param context
	 * @param lineNumber
	 * @param poi
	 * @return formatted line of text
	 */
	public static String getTextForLine(Context context, int lineNumber, POI poi) {
		POI.TYPE type = poi.getType();

		if (lineNumber == 1) {
			if (type == POI.TYPE.PEAK && poi.getSize() > 0)
				return String.format("%s (%sm)", poi.getName(), poi.getSize());
			else
				return poi.getName();

		} else if (lineNumber == 2) {
			return String.format(context.getString(R.string.proximity_display_line2_template), poi.getDistanceToOrigin(), poi.getBearingFromOrigin());
		}

		return "Incorrect line number.";
	}

	private void notifyDataChanged() {
		if (poi != null) {
			// set icon:
			configureSizeIcon(sizeIcon, poi);

			// set text:
			firstLine.setText(getTextForLine(context, 1, poi));
			secondLine.setText(getTextForLine(context, 2, poi));

			compass.setImageResource(R.drawable.compass);

			float heading = (360 + (poi.getBearingFromOrigin() - deviceAzimuthDegrees)) % 360;

			float rotateFrom = heading;
			float rotateTo = heading;

			RotateAnimation r = new RotateAnimation(rotateFrom, rotateTo, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
			r.setFillAfter(true); // keep the final rotation
			compass.startAnimation(r);

		} else {
			firstLine.setText(context.getString(R.string.proximity_display_line1_default_text));
			secondLine.setText(context.getString(R.string.proximity_display_line2_default_text));

			float rotateFrom = 0;
			float rotateTo = 360;

			RotateAnimation r = new RotateAnimation(rotateFrom, rotateTo, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
			r.setDuration(2000);
			r.setRepeatCount(Animation.INFINITE);
			compass.startAnimation(r);
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
			if (poi != null && Math.abs(deviceAzimuthDegrees - newHeading) > 1) {
				// at least one degree change
				deviceAzimuthDegrees = newHeading;
				notifyDataChanged();
			}
		}
	}
}