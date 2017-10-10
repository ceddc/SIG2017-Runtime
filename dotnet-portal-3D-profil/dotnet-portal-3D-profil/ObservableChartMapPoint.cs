using Esri.ArcGISRuntime.Geometry;
using LiveCharts.Defaults;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace dotnet_portal_3D_profil
{
    // extend observablePoint to add mappointValue
    public class ObservableChartMapPoint : ObservablePoint
    {
        public MapPoint point;

        public ObservableChartMapPoint(double x, double y, MapPoint point) : base (x,y)
        {
            this.point = point;
        }
    }
}
