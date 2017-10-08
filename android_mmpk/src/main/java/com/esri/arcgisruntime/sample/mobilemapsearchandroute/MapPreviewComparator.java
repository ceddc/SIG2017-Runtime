package com.esri.arcgisruntime.sample.mobilemapsearchandroute;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;

import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;

/**
 * Created by fgrataloup on 05/10/2017.
 */

public class MapPreviewComparator implements Comparator<MapPreview>
    {
        // Comparaison de livres
        public int compare(final MapPreview preview1,
                           final MapPreview preview2)
        {
            // Comparaison sur les propriétés des livres
            return Double.compare(preview1.getMapNum(),preview2.getMapNum());
        }

}
