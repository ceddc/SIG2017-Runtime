using Esri.ArcGISRuntime.Data;
using Esri.ArcGISRuntime.Geometry;
using Esri.ArcGISRuntime.Mapping;
using Esri.ArcGISRuntime.Symbology;
using Esri.ArcGISRuntime.UI;
using LiveCharts;
using LiveCharts.Configurations;
using LiveCharts.Defaults;
using LiveCharts.Wpf;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Data;
using System.Windows.Documents;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Media.Imaging;
using System.Windows.Navigation;
using System.Windows.Shapes;

namespace dotnet_portal_3D_profil
{
    /// <summary>
    /// </summary>
    public partial class MainWindow : Window
    {
        public MainWindow()
        {
            InitializeComponent();
            InitMap();
            InitChartConf();


        }
        FeatureLayer fLayer;
        Map myMap;
        Scene myScene;
        Surface sceneSurface;
       

        public static readonly DependencyProperty chartValuesProperty =
            DependencyProperty.Register("chartValues", typeof(ChartValues<ObservableChartMapPoint>), typeof(MainWindow));
        public ChartValues<ObservableChartMapPoint> chartValues
        {
            get { return (ChartValues<ObservableChartMapPoint>)GetValue(chartValuesProperty); }
            set { SetValue(chartValuesProperty, value); }
        }
        public static readonly DependencyProperty randoTitleProperty =
           DependencyProperty.Register("randoTitle", typeof(String), typeof(MainWindow));
        public String randoTitle
        {
            get { return (String)GetValue(randoTitleProperty); }
            set { SetValue(randoTitleProperty, value); }
        }



        public Func<double, string> XFormatter { get; set; } = val => val.ToString() + " km";
        public Func<double, string> YFormatter { get; set; } = val => val.ToString() + " m";
        public async void InitMap()
        {
            myMap = new Map(new Uri("http://cdespierre.maps.arcgis.com/home/item.html?id=cf7b87d2e0f34b4797a813bacb4a98e9"));
            myMap.Loaded += MyMap_Loaded;

            myScene = new Scene(Basemap.CreateTopographic());
            // ajout d'une source d'elevation
            var elevationSource = new ArcGISTiledElevationSource(new System.Uri("http://elevation3d.arcgis.com/arcgis/rest/services/WorldElevation3D/Terrain3D/ImageServer"));
            // creation d'une surface
            sceneSurface = new Surface();
            sceneSurface.ElevationSources.Add(elevationSource);
            sceneSurface.ElevationExaggeration = 5;
            myScene.BaseSurface = sceneSurface;

            //ajout des graphic overlay
            myMapView.GraphicsOverlays.Add(new GraphicsOverlay());
            mySceneView.GraphicsOverlays.Add(new GraphicsOverlay());

            // asignation des maps/scene
            myMapView.Map = myMap;
            mySceneView.Scene = myScene;



            myMapView.GeoViewTapped += MyMapView_GeoViewTapped;
            mySceneView.GeoViewTapped += MyMapView_GeoViewTapped;
        }
        public void InitChartConf()
        {
            LiveCharts.Charting.For<ObservableChartMapPoint>(Mappers.Xy<ObservableChartMapPoint>()
           .X((value, index) => value.X)
           .Y(value => value.Y));
        }

        private void MyMap_Loaded(object sender, EventArgs e)
        {
            // quand la webmap est load, on recup le featureLayer associé
            // on sait qu'i ly en a qu'un seul ici :-)
            var webMapLayer = myMap.OperationalLayers[0];
            // un featureLayer est associé a un seul objet.
            fLayer = webMapLayer.Clone() as FeatureLayer;
            myScene.OperationalLayers.Add(fLayer);
        }

        private async void MyMapView_GeoViewTapped(object sender, Esri.ArcGISRuntime.UI.Controls.GeoViewInputEventArgs e)
        {
            DataContext = this;
            // search
            var tapPoint = e.Location;
            var buffer = GeometryEngine.Buffer(tapPoint, 10);
            QueryParameters query = new QueryParameters();
            query.Geometry = buffer;
            var results = await fLayer.FeatureTable.QueryFeaturesAsync(query);
            if(results.Count()>0)
            {
                chartValues = new ChartValues<ObservableChartMapPoint>();
                var firstResult = results.First();
                var randoGeom = firstResult.Geometry as Esri.ArcGISRuntime.Geometry.Polyline;
                randoTitle = firstResult.Attributes["NOM"].ToString();


                Camera cam = new Camera(firstResult.Geometry.Extent.GetCenter(),4200, myMapView.MapRotation, 55, 0);
                var viewPoint = new Viewpoint(firstResult.Geometry, cam);
                //scen
                myMapView.SetViewpointAsync(viewPoint, System.TimeSpan.FromMilliseconds(4000));
                mySceneView.SetViewpointAsync(viewPoint, System.TimeSpan.FromMilliseconds(4000));
                var i = 0;
                double distance = 0;
                MapPoint lastPoint = null;  



                foreach (var part in randoGeom.Parts)
                {
                    foreach(var point in part.Points)
                    {
                        // si on est pas sur le premier point
                        if(i>0)
                        {
                            distance += GeometryEngine.DistanceGeodetic(lastPoint,point,LinearUnits.Kilometers,AngularUnits.Degrees,GeodeticCurveType.Geodesic).Distance;
                        }
                        // sauvegrde du point pour distance.
                        lastPoint = point;
                        // on ne prend pas tous les points.
                        if (i%2 == 0)
                        {
                            double elevation = await sceneSurface.GetElevationAsync(point);
                            chartValues.Add(new ObservableChartMapPoint(Math.Round(distance,4), elevation, point));
                        }
                        i++;
                    }
                }
                // charts
                
               


            }
            // zoom on both scene+mapview
        }

        private void CartesianChart_DataClick(object sender, ChartPoint chartPoint)
        {
            ChartPoint myPoint = chartPoint;
            var mapPoint = ((ObservableChartMapPoint)myPoint.Instance).point;
            Graphic graph = new Graphic(mapPoint, new SimpleMarkerSymbol(SimpleMarkerSymbolStyle.Circle, Colors.Red, 12));
            Graphic graph3d = new Graphic(mapPoint, new SimpleMarkerSymbol(SimpleMarkerSymbolStyle.Circle, Colors.Red, 12));
            myMapView.GraphicsOverlays[0].Graphics.Clear();
            myMapView.GraphicsOverlays[0].Graphics.Add(graph);
            mySceneView.GraphicsOverlays[0].Graphics.Clear();
            mySceneView.GraphicsOverlays[0].Graphics.Add(graph3d);
        }
    }
}
