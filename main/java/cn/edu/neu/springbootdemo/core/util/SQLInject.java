package cn.edu.neu.springbootdemo.core.util;

import java.net.*;
import java.io.*;
import java.sql.*;
import java.text.*;
import java.lang.String; 
 
public class SQLInject {
	public static boolean sql_inj(String str)
	{
	    String injStr = "'|and|exec|insert|select|delete|update|drop|count|*|%|chr|mid|master|truncate|char|declare|;|or|-|+|,";
		//这里的东西还可以自己添加
		String[] injStra=injStr.split("\\|");
	    for (int i=0 ; i < injStra.length ; i++ )
	    {
	        if (str.indexOf(injStra[i])>=0)
	        {
	            return true;
	        }
	    }
	    return false;
	}
}
