package com.esri.arcgisruntime.sample.mobilemapsearchandroute;

import java.text.DecimalFormat;

/**
 * Created by Administrateur on 24/03/2017.
 */
public class NumberUtils {

    public static double parseDouble(String strNumber) {
        if (strNumber != null && strNumber.length() > 0) {
            strNumber = strNumber.replace(",",".");
            try {
                return Double.parseDouble(strNumber);
            } catch(Exception e) {
                return -1;   // or some value to mark this field is wrong. or make a function validates field first ...
            }
        }
        else return 0;
    }

    public static int parseInt(String strNumber) {
        if (strNumber != null && strNumber.length() > 0) {
            try {
                return Integer.parseInt(strNumber);
            } catch(Exception e) {
                return -1;   // or some value to mark this field is wrong. or make a function validates field first ...
            }
        }
        else return 0;
    }

    public static double formatDecimal(double value){
        DecimalFormat df = new DecimalFormat("#.000");
        return parseDouble(df.format(value));
    }

}
