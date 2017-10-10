using Esri.ArcGISRuntime.Data;
using Esri.ArcGISRuntime.Geometry;
using Esri.ArcGISRuntime.Mapping;
using Esri.ArcGISRuntime.UI;
using LiveCharts;
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

           
        }
        FeatureLayer fLayer;
        Map myMap;
        Scene myScene;
        Surface sceneSurface;
     

        public static readonly DependencyProperty chartValuesProperty =
            DependencyProperty.Register("chartValues", typeof(ChartValues<ObservablePoint>), typeof(MainWindow));
        public ChartValues<ObservablePoint> chartValues
        {
            get { return (ChartValues<ObservablePoint>)GetValue(chartValuesProperty); }
            set { SetValue(chartValuesProperty, value); }
        }




        public Func<double, string> XFormatter { get; set; }
        public Func<double, string> YFormatter { get; set; }
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
            // asignation des maps/scene
            myMapView.Map = myMap;
            mySceneView.Scene = myScene;
            myMapView.GeoViewTapped += MyMapView_GeoViewTapped;
            mySceneView.GeoViewTapped += MyMapView_GeoViewTapped;
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
                chartValues = new ChartValues<ObservablePoint>();
                var firstResult = results.First();
                var randoGeom = firstResult.Geometry as Esri.ArcGISRuntime.Geometry.Polyline;
                Camera cam = new Camera(firstResult.Geometry.Extent.GetCenter(),3200, myMapView.MapRotation, 55, 0);
                var viewPoint = new Viewpoint(firstResult.Geometry, cam);
                //scen
                myMapView.SetViewpointAsync(viewPoint, System.TimeSpan.FromMilliseconds(3000));
                mySceneView.SetViewpointAsync(viewPoint, System.TimeSpan.FromMilliseconds(3000));
                var i = 0;
                var distance = 0;
                MapPoint lastPoint = null;  

                foreach (var part in randoGeom.Parts)
                {
                    foreach(var point in part.Points)
                    {
                        // si on est pas sur le premier point
                        if(i>0)
                        {
                           // distance += GeometryEngine.DistanceGeodetic();
                        }
                        // on ne prend pas tous les points.
                        if (i%5 == 0)
                        {
                            var elevation = await sceneSurface.GetElevationAsync(point);
                            chartValues.Add(new ObservablePoint(i, elevation));
                        }
                        i++;
                    }
                }
                // charts
                
                XFormatter = val => val.ToString();
                YFormatter = val => val.ToString();


            }
            // zoom on both scene+mapview
        }
    }
}
