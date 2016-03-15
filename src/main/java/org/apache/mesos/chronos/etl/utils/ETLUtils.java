package org.apache.mesos.chronos.etl.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.Iterator;
import java.util.HashMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONException;
import org.json.JSONObject;


/**
 * Description: Class for utility methods specifically used for ETL workflows.
 * Author: ppanwa1
 * Created: 2014/07/17
 */
public class ETLUtils
{
    private static final Logger log = Logger.getLogger("ETLUtils");
    public static final String GLOBAL_EXECUTOR_OPTIONS_KEY = "global";

    /**
     * Method to merge config returned (through TaskStatus) by Parent job with the existing Child job config.
     * Please note: Parent Job's returned config has higher priority over Child's job config.
     *
     * @param origConfig    Child Job's original config
     * @param newConfig     Config returned by Parent job
     * @return              Updated config of Child job
     */
    public static String overrideConfig(String origConfig, String newConfig)
    {
        String mergedConfigStr = "";
        try
        {
            JSONObject newJsonObj = new JSONObject(newConfig.trim());
            JSONObject origJsonObj = new JSONObject(origConfig.trim());

            log.info("ORIGINAL CONFIG = " + origJsonObj.toString());

            // Iterate over the keys of the new config
            Iterator<String> keys = newJsonObj.keys();
            while( keys.hasNext() )
            {
                String  key = (String) keys.next();
                String  value = newJsonObj.getString(key);

                // Overlay the new config data over the original config
                origJsonObj.put(key, value);
            }

            log.info("UPDATED CONFIG = " + origJsonObj.toString());

            mergedConfigStr = origJsonObj.toString();
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }

        return mergedConfigStr;

    }

    public static String mergeConfig(String origConfig, String returnedMap)
    {
        String mergedConfigStr = "";
        try
        {
            JSONObject newJsonObj = new JSONObject(returnedMap.trim());
            JSONObject origJsonObj = new JSONObject(origConfig.trim());

            log.info("ORIGINAL CONFIG = " + origJsonObj.toString());
            log.info("RETURNED MAP STR = " + newJsonObj.toString());

            if(origJsonObj.has(GLOBAL_EXECUTOR_OPTIONS_KEY)) {
                log.info(GLOBAL_EXECUTOR_OPTIONS_KEY + " key already present. Need to merge the returned map.");
                JSONObject globalJSONObject = origJsonObj.getJSONObject(GLOBAL_EXECUTOR_OPTIONS_KEY);

                // Iterate over the keys of the new config
                Iterator<String> keys = newJsonObj.keys();
                while( keys.hasNext() )
                {
                    String  key = (String) keys.next();
                    String  value = newJsonObj.getString(key);

                    // Overlay the new config data over the original config
                    globalJSONObject.put(key, value);
                }
            }
            else {
                log.info(GLOBAL_EXECUTOR_OPTIONS_KEY + " key not present. Creating one now.");

                HashMap<String,Object> globalMap = new ObjectMapper().readValue(returnedMap, HashMap.class);
                origJsonObj.put(GLOBAL_EXECUTOR_OPTIONS_KEY, globalMap);
            }

            mergedConfigStr = origJsonObj.toString();

            log.info("UPDATED CONFIG = " + mergedConfigStr);
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        return mergedConfigStr;
    }

    public static String getHostname(String json) {
        try {
            JSONObject jsonObj = new JSONObject(json.trim());
            if(jsonObj.has("hostname")) {
                return jsonObj.getString("hostname");
            }
            return "";
        } catch (Exception e) {
            log.info("Error while parsing JSON.");
            return "";
        }
    }

    public static List<String> getHostnames(String json) {
        List<String> hostnames = new ArrayList<String>();
        try {
            JSONObject jsonObj = new JSONObject(json.trim());
            if(jsonObj.has("hostnames")) {
                String hostnamesStr = jsonObj.getString("hostnames");
                if(hostnamesStr.length() > 0) {
                    for(String hostname : jsonObj.getString("hostnames").split(",")) {
                        hostnames.add(hostname.trim());
                    }
                }
            }
            return hostnames;
        } catch (Exception e) {
            log.info("Error while parsing JSON.");
            return hostnames;
        }
    }

    public static String getEnvironment(String json) {
        try {
            JSONObject jsonObj = new JSONObject(json.trim());
            if(jsonObj.has("environment")) {
                return jsonObj.getString("environment");
            }
            return "NONE";
        } catch (Exception e) {
            log.info("Error while parsing JSON.");
            return "NONE";
        }
    }

}
