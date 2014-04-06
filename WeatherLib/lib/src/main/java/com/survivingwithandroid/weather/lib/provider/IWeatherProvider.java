/*
 * Copyright (C) 2014 Francesco Azzola
 *  Surviving with Android (http://www.survivingwithandroid.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.survivingwithandroid.weather.lib.provider;

import android.location.Location;

import com.survivingwithandroid.weather.lib.exception.ApiKeyRequiredException;
import com.survivingwithandroid.weather.lib.exception.WeatherLibException;
import com.survivingwithandroid.weather.lib.WeatherConfig;
import com.survivingwithandroid.weather.lib.model.City;
import com.survivingwithandroid.weather.lib.model.CurrentWeather;
import com.survivingwithandroid.weather.lib.model.WeatherForecast;

import java.util.List;

public interface IWeatherProvider {

    public CurrentWeather getCurrentCondition(String data) throws WeatherLibException;

    public WeatherForecast getForecastWeather(String data) throws WeatherLibException;

    public List<City> getCityResultList(String data) throws WeatherLibException;

    public String getQueryCityURL(String cityNamePattern) throws ApiKeyRequiredException;

    public String getQueryCurrentWeatherURL(String cityId) throws ApiKeyRequiredException;

    public String getQueryForecastWeatherURL(String cityId) throws ApiKeyRequiredException;

    public String getQueryCityURLByLocation(Location location) throws ApiKeyRequiredException;

    public void setConfig(WeatherConfig config);

    public void setWeatherCodeProvider(IWeatherCodeProvider codeProvider);

    public String getQueryImageURL(String weatherId) throws ApiKeyRequiredException;

}
