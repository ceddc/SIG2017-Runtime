using Esri.ArcGISRuntime.Data;
using Esri.ArcGISRuntime.Geometry;
using Esri.ArcGISRuntime.Mapping;
using Esri.ArcGISRuntime.UI;
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
        public async void InitMap()
        {
            Map myMap = new Map(new Uri("http://cdespierre.maps.arcgis.com/home/item.html?id=cf7b87d2e0f34b4797a813bacb4a98e9"));
            myMap.Loaded += MyMap_Loaded;
            Scene myScene = new Scene(Basemap.CreateTopographic());
           // fLayer = new FeatureLayer(new Uri("https://services1.arcgis.com/iNWpti7T6X4xmP8G/arcgis/rest/services/Circuits_de_rando_v2/FeatureServer/0"));
            myScene.OperationalLayers.Add(fLayer);
            myMapView.Map = myMap;
            mySceneView.Scene = myScene;
            myMapView.GeoViewTapped += MyMapView_GeoViewTapped;
            mySceneView.GeoViewTapped += MyMapView_GeoViewTapped;
        }

        private void MyMap_Loaded(object sender, EventArgs e)
        {
            // quand la webmap est load, on recup le featureLayer associé
        }

        private async void MyMapView_GeoViewTapped(object sender, Esri.ArcGISRuntime.UI.Controls.GeoViewInputEventArgs e)
        {
            // search
            var tapPoint = e.Location;
            var buffer = GeometryEngine.Buffer(tapPoint, 10);
            QueryParameters query = new QueryParameters();
            query.Geometry = buffer;
            var results = await fLayer.FeatureTable.QueryFeaturesAsync(query);
            if(results.Count()>0)
            {
                var firstResult = results.First();
                var extent = firstResult.Geometry.Extent;
            }
            // zoom on both scene+mapview
        }
    }
}
