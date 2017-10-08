/* Copyright 2017 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.esri.arcgisruntime.sample.mobilemapsearchandroute;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.TextView;

import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.loadable.LoadStatus;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.MobileMapPackage;
import com.esri.arcgisruntime.mapping.view.Callout;
import com.esri.arcgisruntime.mapping.view.DefaultMapViewOnTouchListener;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.IdentifyGraphicsOverlayResult;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.symbology.CompositeSymbol;
import com.esri.arcgisruntime.symbology.SimpleLineSymbol;
import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol;
import com.esri.arcgisruntime.symbology.Symbol;
import com.esri.arcgisruntime.symbology.TextSymbol;
import com.esri.arcgisruntime.tasks.geocode.GeocodeResult;
import com.esri.arcgisruntime.tasks.geocode.LocatorTask;
import com.esri.arcgisruntime.tasks.geocode.ReverseGeocodeParameters;
import com.esri.arcgisruntime.tasks.networkanalysis.DirectionManeuver;
import com.esri.arcgisruntime.tasks.networkanalysis.Route;
import com.esri.arcgisruntime.tasks.networkanalysis.RouteParameters;
import com.esri.arcgisruntime.tasks.networkanalysis.RouteResult;
import com.esri.arcgisruntime.tasks.networkanalysis.RouteTask;
import com.esri.arcgisruntime.tasks.networkanalysis.Stop;
import com.google.common.collect.Lists;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static com.esri.arcgisruntime.sample.mobilemapsearchandroute.R.layout.callout;

/**
 * This class demonstrates offline functionality through the use of a mobile map package (mmpk).
 *
 * This (main) activity handles:
 * loading of map package,
 * loading of maps and map previews from map packages,
 * searching (ie reverse geocoding), and
 * routing.
 */
public class MobileMapViewActivity extends AppCompatActivity {
    private static final String TAG = "MMVA";
    private MobileMapPackage mMobileMapPackage;
    private MapView mMapView;
    private String mMMPkTitle;
    private List <MapPreview> mMapPreviews = new ArrayList<>();
    private LocatorTask mLocatorTask;
    private Callout mCallout;
    private static GraphicsOverlay mMarkerGraphicsOverlay;
    private static GraphicsOverlay mRouteGraphicsOverlay;
    private static RouteTask mRouteTask;
    private static RouteParameters mRouteParameters;
    private ReverseGeocodeParameters mReverseGeocodeParameters;
    private double distanceTotale = 0;
    private double tempsTotale = 0;
    private int currentMapIndex = 0;
    private final String[] reqPermission = new String[] {
            WRITE_EXTERNAL_STORAGE
    };
    private String path;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //initialize reverse geocode params
        mReverseGeocodeParameters = new ReverseGeocodeParameters();
        mReverseGeocodeParameters.setMaxResults(1);
        mReverseGeocodeParameters.getResultAttributeNames().add("*");
        //retrieve the MapView from layout
        mMapView = (MapView) findViewById(R.id.mapView);
        //add route and marker overlays to map view
        mMarkerGraphicsOverlay = new GraphicsOverlay();
        mRouteGraphicsOverlay = new GraphicsOverlay();
        mMapView.getGraphicsOverlays().add(mRouteGraphicsOverlay);
        mMapView.getGraphicsOverlays().add(mMarkerGraphicsOverlay);
        // build the file path to access the mobile map package
        String filePathMMPk = buildMMPkPath();

        // add the map from the mobile map package to the MapView
        loadMobileMapPackage(filePathMMPk);
        mMapView.setOnTouchListener(new DefaultMapViewOnTouchListener(this, mMapView) {

            @Override
            public boolean onSingleTapConfirmed(MotionEvent motionEvent) {
                Log.d(TAG, "onSingleTapConfirmed: " + motionEvent.toString());
                // get the point that was clicked and convert it to a point in map coordinates
                android.graphics.Point screenPoint = new android.graphics.Point(
                        Math.round(motionEvent.getX()),
                        Math.round(motionEvent.getY()));
                // create a map point from screen point
                Point mapPoint = mMapView.screenToLocation(screenPoint);
                geoView(screenPoint, mapPoint);
                return true;
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //create button in action bar to allow user to access MapChooserActivity
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.map_preview_list, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int MAP_CHOSEN_RESULT = 1;
        Intent mapChooserIntent = new Intent(getApplicationContext(), MapChooserActivity.class);
        //pass the list of mapPreviews
        mapChooserIntent.putExtra("map_previews", Lists.newArrayList(MapPreviewUtils.orderMapPreviews(mMapPreviews)));
        //pass the mobile map package title
        mapChooserIntent.putExtra("MMPk_title", mMMPkTitle);
        //start MapChooserActivity to determine user's chosen map number
        startActivityForResult(mapChooserIntent, MAP_CHOSEN_RESULT);
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data != null) {
            //get the map number chosen in MapChooserActivity and load that map
            currentMapIndex = data.getIntExtra("map_num", -1);
            loadMap(currentMapIndex);
            //dismiss any callout boxes
            if (mCallout != null) {
                mCallout.dismiss();
            }
            //clear any existing graphics
            mMarkerGraphicsOverlay.getGraphics().clear();
            mRouteGraphicsOverlay.getGraphics().clear();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMapView.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapView.resume();
    }

    /**
     * Builds the path to the mobile map package on the device
     * @return the assembled path
     */
    private String buildMMPkPath() {
        // get sdcard resource name
        return new File("/sdcard/ArcGIS/samples/MapPackage/"+this.getString(R.string.config_mmpk_name)+".mmpk").getAbsolutePath();
    }

    /**
     * Handles read/write external storage permissions (for API 23+) and loads mobile map package
     * @param path to location of mobile map package on device
     */
    private void loadMobileMapPackage(String path) {
        this.path = path;
        //for API level 23+ request permission at runtime
        if (ContextCompat.checkSelfPermission(getApplicationContext(),
                reqPermission[0]) == PackageManager.PERMISSION_GRANTED) {
            loadMobileMapPackageImpl(path);
        }
        else{
            //request permission
            int requestCode = 2;
            ActivityCompat.requestPermissions(
                    MobileMapViewActivity.this, reqPermission, requestCode);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {

        switch (requestCode) {
            case 2: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    loadMobileMapPackageImpl(this.path);
                }
            }
        }
    }

    private void loadMobileMapPackageImpl(String path) {

        //create the mobile map package
        mMobileMapPackage = new MobileMapPackage(path);
        //load the mobile map package asynchronously
        mMobileMapPackage.loadAsync();
        //add done listener which will load when package has maps
        mMobileMapPackage.addDoneLoadingListener(new Runnable() {
            @Override
            public void run() {
                //check load status and that the mobile map package has maps
                if (mMobileMapPackage.getLoadStatus() == LoadStatus.LOADED &&
                        mMobileMapPackage.getMaps().size() > 0) {
                    mLocatorTask = mMobileMapPackage.getLocatorTask();
                    //default to display of first map in package
                    loadMap(currentMapIndex);
                    loadMapPreviews();
                } else {
                    //log an issue if the mobile map package fails to load
                    Log.e(TAG, mMobileMapPackage.getLoadError().getMessage());
                }
            }
        });
    }

    /**
     * Loads map from the mobile map package for a given index
     * @param mapNum index of map in mobile map package
     */
    private void loadMap(int mapNum) {
        ArcGISMap map = mMobileMapPackage.getMaps().get(mapNum);
        //if map contains transport network setup route task
        if (map.getTransportationNetworks().size() > 0) {
            setupRouteTask(map);
        } else {
            //only allow routing on map with transport networks
            mRouteTask = null;
        }
        mMapView.setMap(map);
    }
    /**
     * generates and populates the mapPreview models from information in the mobile map package
     */
    private void loadMapPreviews() {
        //set mobile map package title
        mMMPkTitle = mMobileMapPackage.getItem().getTitle();
        //for each map in the mobile map package, pull out relevant thumbnail information
        for (int i = 0; i < mMobileMapPackage.getMaps().size(); i++) {
            ArcGISMap currMap = mMobileMapPackage.getMaps().get(i);
            final MapPreview mapPreview = new MapPreview();
            //set map number
            mapPreview.setMapNum(i);
            //set map title. If null use the index of the list of maps to name each map Map #
            if (currMap.getItem() != null && currMap.getItem().getTitle() != null) {
                mapPreview.setTitle(currMap.getItem().getTitle());
            } else {
                mapPreview.setTitle("Map " + i);
            }
            //set map description. If null use package description instead
            if (currMap.getItem() != null && currMap.getItem().getDescription() != null) {
                mapPreview.setDesc(currMap.getItem().getDescription());
            } else {
                mapPreview.setDesc(mMobileMapPackage.getItem().getDescription());
            }
            //check if map has transport data
            if (currMap.getTransportationNetworks().size() > 0) {
                mapPreview.setTransportNetwork(true);
            }
            //check if map has geocoding data
            if (mMobileMapPackage.getLocatorTask() != null) {
                mapPreview.setGeocoding(true);
            }
            //set map preview thumbnail
            final ListenableFuture<byte[]> thumbnailAsync;
            if (currMap.getItem() != null && currMap.getItem().fetchThumbnailAsync() != null) {
                thumbnailAsync = currMap.getItem().fetchThumbnailAsync();
            } else {
                thumbnailAsync = mMobileMapPackage.getItem().fetchThumbnailAsync();
            }
            thumbnailAsync.addDoneListener(new Runnable() {
                @Override
                public void run() {
                    if (thumbnailAsync.isDone()) {
                        try {
                            mapPreview.setThumbnailByteStream(thumbnailAsync.get());
                            mMapPreviews.add(mapPreview);
                            mMapPreviews = Lists.newArrayList(MapPreviewUtils.orderMapPreviews(mMapPreviews));
                        } catch (InterruptedException | ExecutionException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        }
    }

    /**
     * Defines a graphic symbol which represents geocoded locations
     * @return the stop graphic
     */
    private SimpleMarkerSymbol simpleSymbolForStopGraphic() {
        SimpleMarkerSymbol simpleMarkerSymbol = new SimpleMarkerSymbol(
                SimpleMarkerSymbol.Style.CIRCLE, Color.RED, 12);
        simpleMarkerSymbol.setLeaderOffsetY(5);
        return simpleMarkerSymbol;
    }

    /**
     *  Defines a composite symbol consisting of the SimpleMarkerSymbol and a text symbol
     *  representing the index of a stop in a route
     * @param simpleMarkerSymbol a SimpleMarkerSymbol which represents the background of the
     *                           composite symbol
     * @param index number which corresponds to the stop number in a route
     * @return the composite symbol
     */
    private CompositeSymbol compositeSymbolForStopGraphic(
            SimpleMarkerSymbol simpleMarkerSymbol, Integer index) {
        TextSymbol textSymbol = new TextSymbol(12, index.toString(), Color.BLACK,
                TextSymbol.HorizontalAlignment.CENTER, TextSymbol.VerticalAlignment.MIDDLE);
        List<Symbol> compositeSymbolList = new ArrayList<>();
        compositeSymbolList.addAll(Arrays.asList(simpleMarkerSymbol, textSymbol));
        return new CompositeSymbol(compositeSymbolList);
    }

    /**
     * For a given point, returns a graphic
     * @param point map point
     * @param isIndexRequired true if used in a route
     * @param index stop number in a route
     * @return a Graphic at point with either a simple or composite symbol
     */
    private Graphic graphicForPoint(Point point, boolean isIndexRequired, Integer index) {
        //make symbol composite if an index is required
        Symbol symbol;
        if (isIndexRequired && index != null) {
            symbol = compositeSymbolForStopGraphic(simpleSymbolForStopGraphic(), index);
        } else {
            symbol = simpleSymbolForStopGraphic();
        }
        return new Graphic(point, symbol);
    }

    /**
     * Shows the callout for a given graphic
     * @param graphic the graphic selected by the user
     * @param tapLocation the location selected at a Point
     */
    private void showCalloutForGraphic(Graphic graphic, Point tapLocation) {
        TextView calloutTextView = (TextView) getLayoutInflater().inflate(callout, null);
        String calloutText = "";
        if(graphic.getAttributes().containsKey("Match_addr")) {
            calloutText += graphic.getAttributes().get("Match_addr").toString()+"\n\n";
        }
        if(graphic.getAttributes().containsKey("Route")) {
            calloutText += graphic.getAttributes().get("Route").toString()+"\n";
        }
        calloutTextView.setText(calloutText);
        mCallout = mMapView.getCallout();
        mCallout.setLocation(tapLocation);
        mCallout.setContent(calloutTextView);
        mCallout.show();
    }

    /**
     * Shows the callout for a given graphic
     * @param route the graphic selected by the user
     * @param point the location selected at a Point
     */
    private void showCalloutForRoute(Route route, Point point,Graphic graphic, Boolean isGeocodeEnable) {
        distanceTotale += route.getTotalLength();
        tempsTotale += route.getTotalTime();
        String texte = "Distance intermédiaire : "+ NumberUtils.formatDecimal(route.getTotalLength())+" mètres\n";
        texte += "Temps intermédiaire : "+ NumberUtils.formatDecimal(route.getTotalTime())+" heures\n";
        texte += "Distance totale : "+ NumberUtils.formatDecimal(distanceTotale)+" mètres\n";
        texte += "Temps total : "+ NumberUtils.formatDecimal(tempsTotale)+" heures\n";
        graphic.getAttributes().put("Route", texte);
        if(isGeocodeEnable){
            reverseGeocode(graphic,point);
        }
        else{
            showCalloutForGraphic(graphic, point);
        }
    }

    /**
     * Adds a graphic at a given point to GraphicsOverlay in the MapView. If RouteTask is not null
     * get index for stop symbol. If identifyGraphicsOverlayAsync returns no graphics, call
     * reverseGeocode and route, otherwise just call reverseGeocode.
     * @param screenPoint point on the screen which the user selected
     * @param mapPoint point on the map which the user selected
     */
    private void geoView(android.graphics.Point screenPoint, final Point mapPoint) {
        if (mRouteTask != null || mLocatorTask != null) {
            if (mRouteTask == null) {
                mMarkerGraphicsOverlay.getGraphics().clear();
            }
            final ListenableFuture<IdentifyGraphicsOverlayResult> result =
                    mMapView.identifyGraphicsOverlayAsync(
                            mMarkerGraphicsOverlay, screenPoint, 12, false);
            result.addDoneListener(new Runnable() {
                public void run() {
                    try {
                        Graphic graphic;
                        if (result.isDone() && result.get().getGraphics().size() == 0) {
                            if (mRouteTask != null) {
                                int index = mMarkerGraphicsOverlay.getGraphics().size() + 1;
                                graphic = graphicForPoint(mapPoint, true, index);
                            } else {
                                graphic = graphicForPoint(mapPoint, false, null);
                            }
                            mMarkerGraphicsOverlay.getGraphics().add(graphic);
                            if(!mMapPreviews.get(currentMapIndex).hasTransportNetwork()) {
                                reverseGeocode(graphic,mapPoint);
                            }else {
                                route(mapPoint, graphic, true);
                            }
                        } else if (result.isDone()){
                            //if graphic exists within screenPoint tolerance, show callout
                            //information of clicked graphic
                            if(mMapPreviews.get(currentMapIndex).hasGeocoding()) {
                                reverseGeocode(result.get().getGraphics().get(0),mapPoint);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    /**
     *  Calls reverseGeocode on a Locator Task and, if there is a result, passes the result to a
     *  Callout method
     * @param point user generated map point
     * @param graphic used for marking the point on which the user touched
     */
    private void reverseGeocode(final Graphic graphic, final Point point) {
        if (mLocatorTask != null) {
            final ListenableFuture<List<GeocodeResult>> results =
                    mLocatorTask.reverseGeocodeAsync(point, mReverseGeocodeParameters);
            results.addDoneListener(new Runnable() {
                public void run() {
                    try {
                        List<GeocodeResult> geocodeResult = results.get();
                        if (geocodeResult.size() > 0) {
                            String matchAdrr = "";
                            if("logementssociaux".equals(getString(R.string.config_mmpk_name))) {
                                String adresse = geocodeResult.get(0).getAttributes().get("Address").toString();
                                String city = geocodeResult.get(0).getAttributes().get("City").toString();
                                String postal = geocodeResult.get(0).getAttributes().get("Postal").toString();
                                matchAdrr = adresse + ", " + postal + ", " + city;
                            }
                            else{
                                matchAdrr = geocodeResult.get(0).getAttributes().get("Match_addr").toString();
                            }
                            graphic.getAttributes().put(
                                    "Match_addr", matchAdrr);
                            showCalloutForGraphic(graphic, point);
                        } else {
                            //no result was found
                            mMapView.getCallout().dismiss();
                        }
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    /**
     * Given an ArcGISMap with a transport network, create a new RouteTask
     * @param map a map with a transport network
     */
    private void setupRouteTask(ArcGISMap map) {
        mRouteTask = new RouteTask(this, map.getTransportationNetworks().get(0));
        try {
            getDefaultParameters();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     *  Get default RouteTask parameters
     */
    private void getDefaultParameters() {
        try {
            mRouteParameters = mRouteTask.createDefaultParametersAsync().get();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Uses the last two markers drawn to calculate a route between them
     */
    private void route(final Point point, final Graphic graphic, final Boolean isGeocodeEnable) {
        if (mMarkerGraphicsOverlay.getGraphics().size() > 1 && mRouteParameters != null) {
            //create stops for last and second to last graphic
            int size = mMarkerGraphicsOverlay.getGraphics().size();
            List<Graphic> graphics = new ArrayList<>();
            Graphic lastGraphic = mMarkerGraphicsOverlay.getGraphics().get(size - 1);
            graphics.add(lastGraphic);
            Graphic secondLastGraphic = mMarkerGraphicsOverlay.getGraphics().get(size - 2);
            graphics.add(secondLastGraphic);
            List stops = stopsForGraphics(graphics);
            //add stops to the parameters
            mRouteParameters.getStops().clear();
            mRouteParameters.getStops().addAll(stops);
            //route
            final ListenableFuture<RouteResult> routeResult =
                    mRouteTask.solveRouteAsync(mRouteParameters);
            routeResult.addDoneListener(new Runnable() {
                public void run() {
                    try {
                        Route route = routeResult.get().getRoutes().get(0);
                        Graphic routeGraphic = new Graphic(route.getRouteGeometry(),
                                new SimpleLineSymbol(
                                        SimpleLineSymbol.Style.SOLID, Color.BLUE, 5.0f));
                        mRouteGraphicsOverlay.getGraphics().add(routeGraphic);
                        showCalloutForRoute(route,point,graphic,isGeocodeEnable);
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                        //if routing is interrupted, remove last graphic
                        Log.e(TAG, "Routing interrupted. Removing last graphic");
                        mMarkerGraphicsOverlay.getGraphics().remove(
                                mMarkerGraphicsOverlay.getGraphics().size() - 1);
                    }
                }
            });
        }
    }

    /**
     * Converts a given list of graphics into a list of stops
     * @param graphics
     * @return a list of stops
     */
    private List stopsForGraphics(List<Graphic> graphics) {
        List<Stop> stops = new ArrayList<>();
        for (Graphic graphic : graphics) {
            Stop stop = new Stop((Point)graphic.getGeometry());
            stops.add(stop);
        }
        return stops;
    }
}