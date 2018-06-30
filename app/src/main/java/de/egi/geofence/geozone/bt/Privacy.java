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

package de.egi.geofence.geozone.bt;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import de.egi.geofence.geozone.bt.utils.Utils;

public class Privacy extends AppCompatActivity {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Utils.onActivityCreateSetDialogTheme(this);
        setContentView(R.layout.privacy);
        try {
            PackageInfo pi = getPackageManager().getPackageInfo("de.egi.geofence.geozone.bt", PackageManager.GET_CONFIGURATIONS);
            String v = pi.versionName;
            setTitle(getTitle() + " - EgiGeoZoneBT: " + v);
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
    }
}
