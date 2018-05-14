package com.ibisek.outlanded;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.ibisek.outlanded.net.UpdateQuery;
import com.ibisek.outlanded.utils.Configuration;

public class AboutActivity extends FragmentActivity {

	private TextView urlTextView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_about);

		// version text:
		TextView versionText = (TextView) findViewById(R.id.versionDateText);
		versionText.setText(Configuration.getAppVersion());

		// ibisek.com url:
		urlTextView = (TextView) findViewById(R.id.textViewUrl);
		urlTextView.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {

				String url = urlTextView.getText().toString();
				if (!url.startsWith("www."))
					url = "www." + url;
				if (!url.startsWith("http://") && !url.startsWith("https://"))
					url = "http://" + url;

				//url += "/#outlanded"; //.. navigate directly on that spot
				Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
				startActivity(myIntent);
			}

		});

		// update button:
		Button updateButton = (Button) findViewById(R.id.updateButton);
		updateButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				UpdateQuery x = new UpdateQuery(AboutActivity.this, Configuration.getVersionUrl(), Configuration.getAppVersion());
				x.execute();
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.about, menu);
		return true;
	}

}
