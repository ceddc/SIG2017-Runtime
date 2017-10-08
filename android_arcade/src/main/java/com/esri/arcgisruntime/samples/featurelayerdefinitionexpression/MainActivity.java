/* Copyright 2016 ESRI
 *
 * All rights reserved under the copyright laws of the United States
 * and applicable international laws, treaties, and conventions.
 *
 * You may freely redistribute and use this sample code, with or
 * without modification, provided you include the original copyright
 * notice and use restrictions.
 *
 * See the Sample code usage restrictions document for further information.
 *
 */

package com.esri.arcgisruntime.samples.featurelayerdefinitionexpression;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.esri.arcgisruntime.arcgisservices.LabelDefinition;
import com.esri.arcgisruntime.data.ServiceFeatureTable;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.SpatialReferences;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.view.MapView;

public class MainActivity extends AppCompatActivity {

    MapView mMapView;
    FeatureLayer mFeatureLayer;

    boolean applyActive;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // inflate MapView from layout
        mMapView = (MapView) findViewById(R.id.mapView);

        // create a map with the topographic basemap
        ArcGISMap map = new ArcGISMap(Basemap.createImagery());

        // create feature layer with its service feature table
        // create the service feature table
        ServiceFeatureTable serviceFeatureTable = new ServiceFeatureTable(getResources().getString(R.string.sample_service_url));

        // create the feature layer using the service feature table
        mFeatureLayer = new FeatureLayer(serviceFeatureTable);

        String labelDefinitionString = "{\n" +
                "  \"labelExpressionInfo\": \n" +
                "  {\n" +
                "    \"expression\": \"return Round(($feature.TEMP - 32)*5/9,2) + '°C';\"\n" +
                "  },\n" +
                "  \"labelPlacement\": \"esriServerPolygonPlacementAlwaysHorizontal\",\n" +
                "  \"symbol\": \n" +
                "  {\n" +
                "    \"color\": [255,255,255,255],\n" +
                "    \"font\": { \"size\": 16 },\n" +
                "    \"type\": \"esriTS\"\n" +
                "  }\n" +
                "}";

        LabelDefinition labelDefinition = LabelDefinition.fromJson(labelDefinitionString);
        mFeatureLayer.setLabelsEnabled(true);
        mFeatureLayer.getLabelDefinitions().add(labelDefinition);
        map.getOperationalLayers().add(mFeatureLayer);
        mMapView.setMap(map);

        // zoom to a view point of the FRA
        mMapView.setViewpointCenterAsync(new Point(341147, 5980251, SpatialReferences.getWebMercator()), 13500000);

    }

    @Override
    protected void onPause(){
        super.onPause();
        // pause MapView
        mMapView.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // resume MapView
        mMapView.resume();
    }
}
