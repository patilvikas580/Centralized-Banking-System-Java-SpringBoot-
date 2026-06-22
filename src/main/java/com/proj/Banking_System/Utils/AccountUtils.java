package com.proj.Banking_System.Utils;

import java.time.Year;

public class AccountUtils {
    public static String generateAccountNumber(){
        Year currentYear = Year.now();
        int min=1000000;
        int max=9999999;

        int randNumber= (int) Math.floor(Math.random()*(max-min+1)+min);

        return currentYear.toString()+Integer.toString(randNumber);

    }
}
