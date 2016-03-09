package com.example.mrmiguelini.reservacionhotel;

import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A placeholder fragment containing a simple view.
 */
public class ReservacionFragment extends Fragment {
    ArrayAdapter<String> arrayAdapter;
    TextView reservacion;

    public ReservacionFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View  vistaGenerada = inflater.inflate(R.layout.fragment_main, container, false);

        //Arreglo de informacion de prueba para probar la lista
        ArrayList<String> fakeDate = new ArrayList<String>();
        fakeDate.add("001 - Reservado 30Mts/3Ctos");
        fakeDate.add("002 - NO Reservado 50Mts/3Ctos");
        fakeDate.add("003 - NO Reservado 80Mts/4Ctos");
        fakeDate.add("004 - Reservado 60Mts/4Ctos");
        fakeDate.add("005 - NO Reservado 30Mts/3Ctos");
        fakeDate.add("006 - Reservado 40Mts/3Ctos");
        fakeDate.add("007 - NO Reservado 30Mts/3Ctos");
        fakeDate.add("008 - Reservado 50Mts/3Ctos");
        fakeDate.add("009 - Reservado 100Mts/10Ctos");
        fakeDate.add("010 - NO Reservado 50Mts/5Ctos");
        fakeDate.add("011 - NO Reservado 60Mts/6Ctos");

        arrayAdapter = new ArrayAdapter<String>(
                getActivity(), //CONTEXTO
                R.layout.list_reservacion_forecast,//id del list item
                R.id.list_item_forecast_textview,//id del textview dentro del list item
                fakeDate//La lista de datos
        );

        reservacion = (TextView) vistaGenerada.findViewById(R.id.reservacion);

        ListView lvForecasList = (ListView) vistaGenerada.findViewById(R.id.list_item_forecast_textview);
        lvForecasList.setAdapter(arrayAdapter);

        //Se instancia
        CargadorDeReservacion cdr = new CargadorDeReservacion();
        cdr.execute();//Se ejecuta el proceso asincrono como hilo

        return  vistaGenerada;
    }

    private class CargadorDeReservacion extends AsyncTask<Void,Void,String[]>{
        String reservado = "";
        @Override
        protected String[] doInBackground(Void... params) {

            String[] datos = consultarAPI();

            return datos;
        }

        @Override
        protected void onPostExecute(String[] hotel) {
            super.onPostExecute(hotel);

            List<String> listaDatos = Arrays.asList(hotel);
            arrayAdapter.clear();
            arrayAdapter.addAll(listaDatos);

            //reservacion.setText(reservado);
        }

        private String[] consultarAPI(){

            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String forecastJsonStr = null;
            String [] arregloReservacion = new String[0];
            try {
                // Construct the URL for the OpenWeatherMap query
                // Possible parameters are avaiable at OWM's forecast API page, at
                // http://openweathermap.org/API#forecast
                //String baseUrl = "http://hoteltel.ticcode.net/Reservacion/JsonIndex";
                //String apiKey = "&APPID=" + "dfb9632bd86e64831b1bc3814bde6a75";
                URL url = new URL("http://hoteltel.ticcode.net/Reservacion/JsonIndex");

                // Create the request to OpenWeatherMap, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                // Stream was empty.  No point in parsing.
                if (buffer.length() == 0) {
                    return null;
                }

                //EL JSON en forma de un String muy largo
                forecastJsonStr = buffer.toString();

                //Necesito PARSEAR el JSON para obtener un arreglo de Strings
                //donde se vea la informacion de cada dia
                ExtractorDeDatosJson v = new ExtractorDeDatosJson();

                try {
                    //arregloDias = v.getReservacionFromJson(forecastJsonStr, 7);
                    arregloReservacion = v.getReservacionFromJson(forecastJsonStr);

                } catch (JSONException e) {
                    e.printStackTrace();
                }

                Log.v("CargadorDePronostico","El JSON Recibido fue: "+forecastJsonStr);

                /*try {
                    ExtractorDeDatosJson.getLaReservacion(forecastJsonStr);
                } catch (JSONException e) {
                    e.printStackTrace();
                }*/


            } catch (IOException e) {
                Log.e("PlaceholderFragment", "Error ", e);
                // If the code didn't successfully get the weather data, there's no point in attemping
                // to parse it.
                return null;
            } finally{
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e("PlaceholderFragment", "Error closing stream", e);
                    }
                }
            }

            return arregloReservacion;
        }
    }
}

class ExtractorDeDatosJson{

    public String[] getReservacionFromJson(String strJSON) throws JSONException {
        JSONArray arrReservacion = new JSONArray(strJSON);

        String[] strHuesped = new String[arrReservacion.length()];
        for (int i = 0; i <arrReservacion.length() ; i++) {
            int reservacionID;
            String fechaDeIngreso;
            String fechaDeSalida;
            String numeroDeHabitacion;

            JSONObject jsonHuesped = arrReservacion.getJSONObject(i);
            reservacionID = jsonHuesped.getInt("reservacionID");
            fechaDeIngreso = jsonHuesped.getString("fechaDeIngreso");
            fechaDeSalida = jsonHuesped.getString("fechaDeSalida");
            numeroDeHabitacion = jsonHuesped.getString("numeroDeHabitacion");

            strHuesped[i] = reservacionID + " - " + fechaDeIngreso + " - " + fechaDeSalida + " - " + numeroDeHabitacion;


        }

        return strHuesped;
    }

    /*Nos arroja el numero de habitacion reservada, los metors de
    la habitacion y cuantos cuartos tiene
     */
    /*public static String getReservacion (String strJson) throws JSONException{
        String numRservacion = new JSONObject(strJson).
                getJSONObject("reservacion").
                getString("reservacionID");
        return  numRservacion;
    }*/

    /*public static void getLaReservacion (String strJSON)throws JSONException
    {
        JSONObject jso = new JSONObject(strJSON);
        JSONArray jsArregloDias = jso.getJSONArray("list");

        for (int i = 0; i < jsArregloDias.length(); i++) {
            double Reservado = jsArregloDias.getJSONObject(i).
                    getJSONObject("fechaDeIngreso").
                    getDouble("fechaDeSalida");

            Log.d("ExtractorDeDatosJson", "Estados de la Reservacio  "+(i+1)+": "+Reservado);
        }
    }
    /* The date/time conversion code is going to be moved outside the asynctask later,
       * so for convenience we're breaking it out into its own method now.
       */
    /*private String getReadableDateString(long time){
        // Because the API returns a unix timestamp (measured in seconds),
        // it must be converted to milliseconds in order to be converted to valid date.
        SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE MMM dd");
        return shortenedDateFormat.format(time);
    }
    /**
     * Prepare the weather high/lows for presentation.
     */
    /*private String formatHighLows(double high, double low) {
        // For presentation, assume the user doesn't care about tenths of a degree.
        long roundedHigh = Math.round(high);
        long roundedLow = Math.round(low);

        String highLowStr = roundedHigh + "/" + roundedLow;
        return highLowStr;
    }*/


    /**
     * Take the String representing the complete forecast in JSON Format and
     * pull out the data we need to construct the Strings needed for the wireframes.
     *
     * Fortunately parsing is easy:  constructor takes the JSON string and converts it
     * into an Object hierarchy for us.
     */
    /*public String[] getWeatherDataFromJson(String forecastJsonStr, int numDays)
            throws JSONException {

        // These are the names of the JSON objects that need to be extracted.
        final String HOTEL_RESERVACION = "reservacionID";
        //final String HOTEL_NOMBRE = "min";
        final String HOTEL_INGRESO = "fechaDeIngreso";
        final String HOTEL_SALIDA = "fechaDeSalida";
        final String HOTEL_NUMERO = "numeroDeHabitacion";

        JSONObject forecastJson = new JSONObject(forecastJsonStr);
        JSONArray reservacionIDArray = forecastJson.getJSONArray(HOTEL_RESERVACION);

        // OWM returns daily forecasts based upon the local time of the city that is being
        // asked for, which means that we need to know the GMT offset to translate this data
        // properly.

        // Since this data is also sent in-order and the first day is always the
        // current day, we're going to take advantage of that to get a nice
        // normalized UTC date for all of our weather.

        Time dayTime = new Time();
        dayTime.setToNow();

        // we start at the day returned by local time. Otherwise this is a mess.
        int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);

        // now we work exclusively in UTC
        dayTime = new Time();

        String[] resultStrs = new String[numDays];
        for(int i = 0; i <reservacionIDArray.length(); i++) {
            // For now, using the format "Day, description, hi/low"
            String day;
            String description;
            String highAndLow;

            // Get the JSON object representing the day
            JSONObject dayForecast = reservacionIDArray.getJSONObject(i);

            // The date/time is returned as a long.  We need to convert that
            // into something human-readable, since most people won't read "1400356800" as
            // "this saturday".
            long dateTime;
            // Cheating to convert this to UTC time, which is what we want anyhow
            dateTime = dayTime.setJulianDay(julianStartDay+i);
            day = getReadableDateString(dateTime);

            // description is in a child array called "weather", which is 1 element long.
            JSONObject weatherObject = dayForecast.getJSONArray(HOTEL_RESERVACION).getJSONObject(0);
            description = weatherObject.getString(HOTEL_NUMERO);

            // Temperatures are in a child object called "temp".  Try not to name variables
            // "temp" when working with temperature.  It confuses everybody.
            JSONObject temperatureObject = dayForecast.getJSONObject(HOTEL_RESERVACION);
            double high = temperatureObject.getDouble(HOTEL_INGRESO);
            double low = temperatureObject.getDouble(HOTEL_SALIDA);

            highAndLow = formatHighLows(high, low);
            resultStrs[i] = day + " - " + description + " - " + highAndLow;
        }

        for (String s : resultStrs) {
            Log.v("getWeatherDataFromJson", "Forecast entry: " + s);
        }
        return resultStrs;*/

    }

