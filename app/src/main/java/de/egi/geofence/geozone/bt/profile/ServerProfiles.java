/*
* Copyright 2014 - 2015 Egmont R. (egmontr@gmail.com)
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/		

package de.egi.geofence.geozone.bt.profile;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import de.egi.geofence.geozone.bt.db.DbContract;
import de.egi.geofence.geozone.bt.db.DbServerHelper;
import de.egi.geofence.geozone.bt.utils.Constants;
import de.egi.geofence.geozone.bt.utils.Utils;

@SuppressWarnings("deprecation")
public class ServerProfiles extends AppCompatActivity implements OnItemClickListener{
	private ListView list;
	private DbServerHelper datasource;
	private Cursor cursorMerk = null;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Utils.onActivityCreateSetTheme(this);
		setContentView(de.egi.geofence.geozone.bt.R.layout.profile_alle);

		Toolbar toolbar = (Toolbar) findViewById(de.egi.geofence.geozone.bt.R.id.toolbar);
		setSupportActionBar(toolbar);
		Utils.changeBackGroundToolbar(this, toolbar);

		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(true);
			actionBar.setHomeButtonEnabled(true);
		}
		FloatingActionButton fab = (FloatingActionButton) findViewById(de.egi.geofence.geozone.bt.R.id.fab_profiles);
		fab.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Intent i = new Intent(ServerProfiles.this, ServerProfile.class);
				i.putExtra("action", "new");
				startActivityForResult(i, 4711);
			}
		});


		datasource = new DbServerHelper(this);
		
	    list = (ListView) findViewById (de.egi.geofence.geozone.bt.R.id.list);
        registerForContextMenu(list);
        fillList();
	}
	
	private void fillList(){
		final Cursor cursor = datasource.getCursorAllServerSorted();
		cursorMerk = cursor;

		ListAdapter adapter = new SimpleCursorAdapter(this, de.egi.geofence.geozone.bt.R.layout.profile_list_item, cursor,
				new String[]{DbContract.ServerEntry.CN_NAME, DbContract.ServerEntry.CN_URL_FHEM},
				new int[]{de.egi.geofence.geozone.bt.R.id.profName, de.egi.geofence.geozone.bt.R.id.profWert}, 0) {
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				final View row = super.getView(position, convertView, parent);

				if (position % 2 == 0)
					row.setBackgroundColor(Color.parseColor("#F7F7F7"));
				else
					row.setBackgroundColor(Color.parseColor("#E7E7E7"));
				return row;
			}
		};
		  
		  list.setAdapter(adapter);
		  list.setOnItemClickListener(this);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,	long id) {
    	// Eintrag evtl. ändern
		cursorMerk.moveToPosition(position);
		String ind = cursorMerk.getString(cursorMerk.getColumnIndex(DbContract.ServerEntry._ID));
		
		Intent is = new Intent(this, ServerProfile.class);
		is.putExtra("action", "update");
		is.putExtra("ind", ind);
		startActivityForResult(is, 4711);
	}

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        // Choose what to do based on the request code
        switch (requestCode) {
            case 4711 :
                fillList();
                break;
            // If any other request code was received
            default:
               // Report that this Activity received an unknown requestCode
               Log.d(Constants.APPTAG, getString(de.egi.geofence.geozone.bt.R.string.unknown_activity_request_code, requestCode));
               break;
        }
    }
}

















