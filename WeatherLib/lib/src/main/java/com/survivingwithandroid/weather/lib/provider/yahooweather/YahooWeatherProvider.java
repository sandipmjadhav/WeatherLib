/*
 * Copyright (C) 2014 Francesco Azzola - Surviving with Android (http://www.survivingwithandroid.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.survivingwithandroid.weather.lib.provider.yahooweather;

import android.location.Location;
import android.util.Log;

import com.survivingwithandroid.weather.lib.exception.ApiKeyRequiredException;
import com.survivingwithandroid.weather.lib.exception.WeatherLibException;
import com.survivingwithandroid.weather.lib.WeatherConfig;
import com.survivingwithandroid.weather.lib.model.City;
import com.survivingwithandroid.weather.lib.model.CurrentWeather;
import com.survivingwithandroid.weather.lib.model.DayForecast;
import com.survivingwithandroid.weather.lib.model.Weather;
import com.survivingwithandroid.weather.lib.model.WeatherForecast;
import com.survivingwithandroid.weather.lib.provider.IWeatherCodeProvider;
import com.survivingwithandroid.weather.lib.provider.IWeatherProvider;
import com.survivingwithandroid.weather.lib.util.WeatherUtility;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;


public class YahooWeatherProvider implements IWeatherProvider {

    private static String YAHOO_GEO_URL = "http://where.yahooapis.com/v1";
    private static String YAHOO_WEATHER_URL = "http://weather.yahooapis.com/forecastrss";
    private static final String YAHOO_IMG_URL = "http://l.yimg.com/a/i/us/we/52/";

    private WeatherConfig config;

    private Weather.WeatherUnit units = new Weather.WeatherUnit();;

    private IWeatherCodeProvider codeProvider;

    private WeatherForecast forecast = new WeatherForecast();

    @Override
    public List<City> getCityResultList(String data) throws WeatherLibException {
        List<City> result = new ArrayList<City>();

        try {
            // String query =makeQueryCityURL(data);
            //Log.d("Swa", "URL [" + query + "]");
            //yahooHttpConn= (HttpURLConnection) (new URL(query)).openConnection();
            //yahooHttpConn.connect();

            XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();

            parser.setInput(new StringReader(data));

            int event = parser.getEventType();

            City cty = null;
            String tagName = null;
            String currentTag = null;

            // We start parsing the XML
            while (event != XmlPullParser.END_DOCUMENT) {
                tagName = parser.getName();

                if (event == XmlPullParser.START_TAG) {
                   if (tagName.equals("place")) {
                      // place Tag Found so we create a new CityResult
                      cty = new City();
                     //  Log.d("Swa", "New City found");
                   }
                    currentTag = tagName;
                   // Log.d("Swa", "Tag ["+tagName+"]");
                }
                else if (event == XmlPullParser.TEXT) {
                    // We found some text. let's see the tagName to know the tag related to the text
                    if ("woeid".equals(currentTag))
                        cty.setId(parser.getText());
                    else if ("name".equals(currentTag))
                        cty.setName(parser.getText());
                    else if ("country".equals(currentTag))
                        cty.setCountry(parser.getText());

                    // We don't want to analyze other tag at the moment
                }
                else if (event == XmlPullParser.END_TAG) {
                    if ("place".equals(tagName))
                        result.add(cty);
                }

                event = parser.next();
            }
        }
        catch(Throwable t) {
            //t.printStackTrace();
            // Log.e("Error in getCityList", t.getMessage());
            throw new WeatherLibException(t);
        }

        return result;
    }

    @Override
    public CurrentWeather getCurrentCondition(String data) throws WeatherLibException {
       // Log.d("SwA", "Response ["+resp+"]");
        //Log.d("App", "Data [" + data + "]");
        CurrentWeather weather = new CurrentWeather();
        try {
            XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
            parser.setInput(new StringReader(data));

            String tagName = null;
            String currentTag = null;

            int event = parser.getEventType();
            boolean isFirstDayForecast = true;
            while (event != XmlPullParser.END_DOCUMENT) {
                tagName = parser.getName();

                if (event == XmlPullParser.START_TAG) {
                    if (tagName.equals("yweather:wind")) {
                       // Log.d("SwA", "Tag [Wind]");
                        weather.wind.setChill(Integer.parseInt(parser.getAttributeValue(null, "chill")));
                        weather.wind.setDeg(Integer.parseInt(parser.getAttributeValue(null, "direction")));
                        weather.wind.setSpeed(Float.parseFloat(parser.getAttributeValue(null, "speed")));
                    }
                    else if (tagName.equals("yweather:atmosphere")) {
                       // Log.d("SwA", "Tag [Atmos]");
                        weather.currentCondition.setHumidity(Integer.parseInt(parser.getAttributeValue(null, "humidity")));
                        weather.currentCondition.setVisibility(Float.parseFloat(parser.getAttributeValue(null, "visibility")));
                        weather.currentCondition.setPressure(Float.parseFloat(parser.getAttributeValue(null, "pressure")));
                        weather.currentCondition.setPressureTrend(Integer.parseInt(parser.getAttributeValue(null, "rising")));
                    }
                    else if (tagName.equals("yweather:forecast")) {
                      //  Log.d("SwA", "Tag [Fore]");
                        if (isFirstDayForecast) {
                            //weather.forecast.code = Integer.parseInt(parser.getAttributeValue(null, "code"));
                            weather.temperature.setMinTemp(Integer.parseInt(parser.getAttributeValue(null, "low")));
                            weather.temperature.setMaxTemp(Integer.parseInt(parser.getAttributeValue(null, "high")));
                            isFirstDayForecast = false;
                        }
                        else {
                            DayForecast df = new DayForecast();
                            df.forecastTemp.max = Integer.parseInt(parser.getAttributeValue(null, "high"));
                            df.forecastTemp.max = Integer.parseInt(parser.getAttributeValue(null, "low"));
                            df.weather.currentCondition.setWeatherId(Integer.parseInt(parser.getAttributeValue(null, "code")));

                            if (codeProvider != null)
                                df.weather.currentCondition.setWeatherCode(codeProvider.getWeatherCode(df.weather.currentCondition.getWeatherId()));

                            df.weather.currentCondition.setDescr(parser.getAttributeValue(null, "text"));
                            df.weather.currentCondition.setIcon("" + df.weather.currentCondition.getWeatherId());
                            forecast.addForecast(df);
                        }
                    }
                    else if (tagName.equals("yweather:condition")) {
                      //  Log.d("SwA", "Tag [Condition]");
                        weather.currentCondition.setWeatherId(Integer.parseInt(parser.getAttributeValue(null, "code")));
                        weather.currentCondition.setIcon("" + weather.currentCondition.getWeatherId());

                        // Convert the code
                        if (codeProvider != null)
                           weather.currentCondition.setWeatherCode(codeProvider.getWeatherCode(weather.currentCondition.getWeatherId()));

                        weather.currentCondition.setDescr(parser.getAttributeValue(null, "text"));
                        weather.temperature.setTemp(Integer.parseInt(parser.getAttributeValue(null, "temp")));
                        //result.condition.date = parser.getAttributeValue(null, "date");
                    }
                    else if (tagName.equals("yweather:units")) {
                     //   Log.d("SwA", "Tag [units]");
                        units.tempUnit = "°" + parser.getAttributeValue(null, "temperature");
                        units.pressureUnit = parser.getAttributeValue(null, "pressure");
                        units.distanceUnit = parser.getAttributeValue(null, "distance");
                        units.speedUnit = parser.getAttributeValue(null, "speed");
                        forecast.setUnit(units);
                    }
                    else if (tagName.equals("yweather:location")) {
                        weather.location.setCity(parser.getAttributeValue(null, "city"));
                        weather.location.setRegion(parser.getAttributeValue(null, "region"));
                        weather.location.setCountry(parser.getAttributeValue(null, "country"));
                    }
                    else if (tagName.equals("image"))
                        currentTag = "image";
                    else if (tagName.equals("url")) {
                        if (currentTag == null) {
                           // result.imageUrl = parser.getAttributeValue(null, "src");
                        }
                    }
                    else if (tagName.equals("lastBuildDate")) {
                       currentTag="update";
                    }
                    else if (tagName.equals("yweather:astronomy")) {
                        String val  = parser.getAttributeValue(null, "sunrise");
                        SimpleDateFormat sdf = new SimpleDateFormat("h:mm a");
                        if (val != null) {
                            java.util.Date d = sdf.parse(val);
                            weather.location.setSunrise(d.getTime());
                        }

                        val = parser.getAttributeValue(null, "sunset");
                        if (val != null) {
                            java.util.Date d = sdf.parse(val);
                            weather.location.setSunset(d.getTime());
                        }

                    }

                }
                else if (event == XmlPullParser.END_TAG) {
                    if ("image".equals(currentTag)) {
                       currentTag = null;
                    }
                }
                /*
                else if (event == XmlPullParser.TEXT) {
                    if ("update".equals(currentTag))
                        //result.lastUpdate = parser.getText();
                }
                */
                event = parser.next();
            }

        }
        catch(Throwable t) {
            t.printStackTrace();
           throw new WeatherLibException(t);
        }

        weather.setUnit(units);
        return weather;
    }

    @Override
    public String getQueryCityURL(String cityNamePattern) throws ApiKeyRequiredException  {

        if (config.ApiKey == null)
            throw new ApiKeyRequiredException();

        // We remove spaces in cityName
        cityNamePattern = cityNamePattern.replaceAll(" ", "%20");
        return YAHOO_GEO_URL + "/places.q(" + cityNamePattern + "%2A);count=" + config.maxResult + "?appid=" + config.ApiKey;
    }

    @Override
    public String getQueryCurrentWeatherURL(String cityId)  throws ApiKeyRequiredException{
        if (config.ApiKey == null)
            throw new ApiKeyRequiredException();

        return  YAHOO_WEATHER_URL + "?w=" + cityId + "&u=" + (WeatherUtility.isMetric(config.unitSystem) ? "c" : "f");
    }

    @Override
    public String getQueryForecastWeatherURL(String cityId)  throws ApiKeyRequiredException {
        if (config.ApiKey == null)
            throw new ApiKeyRequiredException();

        return  YAHOO_WEATHER_URL + "?w=" + cityId + "&u=" + (WeatherUtility.isMetric(config.unitSystem) ? "c" : "f");
    }

    @Override
    public String getQueryCityURLByLocation(Location location) throws ApiKeyRequiredException {
        if (config.ApiKey == null)
            throw new ApiKeyRequiredException();

        return YAHOO_GEO_URL + "/places.q(" + location.getLatitude() + "," + location.getLongitude() + ")?appid=" + config.ApiKey;
    }

    @Override
    public String getQueryImageURL(String icon) throws ApiKeyRequiredException {
        return YAHOO_IMG_URL + icon + ".gif";
    }

    @Override
    public WeatherForecast getForecastWeather(String data) throws WeatherLibException {
        return forecast;
    }


    @Override
    public void setConfig(WeatherConfig config) {
        this.config = config;
        units = WeatherUtility.createWeatherUnit(config.unitSystem);
    }

    @Override
    public void setWeatherCodeProvider(IWeatherCodeProvider codeProvider) {
        this.codeProvider = codeProvider;
    }
}
