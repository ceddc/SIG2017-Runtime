package com.esri.arcgisruntime.sample.mobilemapsearchandroute;

import com.google.common.collect.Ordering;

import java.util.Comparator;
import java.util.List;

/**
 * Created by fgrataloup on 06/10/2017.
 */

public class MapPreviewUtils {

    public static List<MapPreview> orderMapPreviews(List<MapPreview> mapPreviews){
        Comparator mapPreviewComp =
                Ordering.from(new MapPreviewComparator());

        List<MapPreview> mapPreviewRes =
                Ordering.from(mapPreviewComp).sortedCopy(mapPreviews);
        return mapPreviewRes;

    }

}
