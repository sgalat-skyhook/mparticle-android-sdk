package com.mparticle;

import android.content.Context;
import android.util.SparseArray;
import android.util.SparseBooleanArray;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by sdozor on 3/13/14.
 */
abstract class EmbeddedProvider implements IEmbeddedKit {

    final static String KEY_ID = "id";
    private final static String KEY_PROPERTIES = "as";
    private final static String KEY_FILTERS = "hs";
    private final static String KEY_EVENT_LIST = "eventList";
    private final static String KEY_ATTRIBUTE_LIST = "attributeList";
    private final static String KEY_EVENT_TYPES_FILTER = "et";
    private final static String KEY_EVENT_NAMES_FILTER = "ec";
    private final static String KEY_EVENT_ATTRIBUTES_FILTER = "ea";
    private final static String KEY_SCREEN_NAME_FILTER = "svec";
    private final static String KEY_SCREEN_ATTRIBUTES_FILTER = "svea";
    private final static String KEY_USER_IDENTITY_FILTER = "uid";
    private final static String KEY_USER_ATTRIBUTE_FILTER = "ua";


    //If set to true, our sdk honor user's optout wish. If false, we still collect data on opt-ed out users, but only for reporting
    private static final String HONOR_OPT_OUT = "honorOptOut";

    protected HashMap<String, String> properties = new HashMap<String, String>(0);
    protected SparseBooleanArray mTypeFilters = new SparseBooleanArray(0);
    protected SparseBooleanArray mNameFilters = new SparseBooleanArray(0);
    protected SparseBooleanArray mAttributeFilters = new SparseBooleanArray(0);
    protected SparseBooleanArray mScreenNameFilters = new SparseBooleanArray(0);
    protected SparseBooleanArray mScreenAttributeFilters = new SparseBooleanArray(0);
    protected SparseBooleanArray mUserIdentityFilters = new SparseBooleanArray(0);
    protected SparseBooleanArray mUserAttributeFilters = new SparseBooleanArray(0);
    protected HashSet<String> includedEvents, includedAttributes;

    protected Context context;

    public EmbeddedProvider(Context context) throws ClassNotFoundException{
        this.context = context;
    }

    protected EmbeddedProvider parseConfig(JSONObject json) throws JSONException {

        if (json.has(KEY_PROPERTIES)){
            JSONObject propJson = json.getJSONObject(KEY_PROPERTIES);
            for (Iterator<String> iterator = propJson.keys(); iterator.hasNext();) {
                String key = iterator.next();
                properties.put(key, propJson.getString(key));
            }
            if (propJson.has(KEY_EVENT_LIST)){
                try {
                    JSONArray inclusions = new JSONArray(propJson.getString(KEY_EVENT_LIST));
                    includedEvents = new HashSet<String>(inclusions.length());
                    for (int i = 0; i < inclusions.length(); i++){
                        includedEvents.add(inclusions.getString(i).toLowerCase());
                    }
                }catch (JSONException jse){

                }
            }
            if (propJson.has(KEY_ATTRIBUTE_LIST)){
                try {
                    JSONArray inclusions = new JSONArray(propJson.getString(KEY_ATTRIBUTE_LIST));
                    includedAttributes = new HashSet<String>(inclusions.length());
                    for (int i = 0; i < inclusions.length(); i++){
                        includedAttributes.add(inclusions.getString(i).toLowerCase());
                    }
                }catch (JSONException jse){

                }
            }
        }
        if (json.has(KEY_FILTERS)){
            if (json.has(KEY_EVENT_TYPES_FILTER)){
                mTypeFilters = convertToSparseArray(json.getJSONObject(KEY_EVENT_TYPES_FILTER));
            }else {
                mTypeFilters.clear();
            }
            if (json.has(KEY_EVENT_NAMES_FILTER)){
                mNameFilters = convertToSparseArray(json.getJSONObject(KEY_EVENT_NAMES_FILTER));
            }else{
                mNameFilters.clear();
            }
            if (json.has(KEY_EVENT_ATTRIBUTES_FILTER)){
                mAttributeFilters = convertToSparseArray(json.getJSONObject(KEY_EVENT_ATTRIBUTES_FILTER));
            }else{
                mAttributeFilters.clear();
            }
            if (json.has(KEY_SCREEN_NAME_FILTER)){
                mScreenNameFilters = convertToSparseArray(json.getJSONObject(KEY_SCREEN_NAME_FILTER));
            }else{
                mScreenNameFilters.clear();
            }
            if (json.has(KEY_SCREEN_ATTRIBUTES_FILTER)){
                mScreenAttributeFilters = convertToSparseArray(json.getJSONObject(KEY_SCREEN_ATTRIBUTES_FILTER));
            }else{
                mScreenAttributeFilters.clear();
            }
            if (json.has(KEY_USER_IDENTITY_FILTER)){
                mUserIdentityFilters = convertToSparseArray(json.getJSONObject(KEY_USER_IDENTITY_FILTER));
            }else{
                mUserIdentityFilters.clear();
            }
            if (json.has(KEY_USER_ATTRIBUTE_FILTER)){
                mUserAttributeFilters = convertToSparseArray(json.getJSONObject(KEY_USER_ATTRIBUTE_FILTER));
            }else{
                mUserAttributeFilters.clear();
            }
        }

        return this;
    }

    private SparseBooleanArray convertToSparseArray(JSONObject json){
        SparseBooleanArray map = new SparseBooleanArray();
        for (Iterator<String> iterator = json.keys(); iterator.hasNext();) {
            try {
                String key = iterator.next();
                map.put(Integer.parseInt(key), json.getBoolean(key));
            }catch (JSONException jse){
                ConfigManager.log(MParticle.LogLevel.ERROR, "Issue while parsing embedded kit configuration: " + jse.getMessage());
            }
        }
        return map;
    }

    public boolean optedOut(){
        return Boolean.parseBoolean(properties.containsKey(HONOR_OPT_OUT) ? properties.get(HONOR_OPT_OUT) : "true")
                && !MParticle.getInstance().mConfigManager.getSendOoEvents();
    }

    private static int hash(String input) {
        int hash = 0;

        if (input == null || input.length() == 0)
            return hash;

        char[] chars = input.toLowerCase().toCharArray();

        for (char c : chars) {
            hash = ((hash << 5) - hash) + c;
        }

        return hash;
    }

    protected boolean shouldLogEvent(MParticle.EventType type, String name){
        int typeHash = hash(type.toString());
        int typeNameHash = hash(type.toString() + name);
        return mTypeFilters.get(typeHash, true) && mNameFilters.get(typeNameHash, true);
    }

    public boolean shouldLogScreen(String screenName) {
        int nameHash = hash("0" + screenName);
        if (mScreenNameFilters.size() > 0 && !mScreenNameFilters.get(nameHash, true)){
            return false;
        }
        return true;
    }

    protected Map<String, String> filterEventAttributes(String eventType, String eventName, SparseBooleanArray filter, Map<String, String> eventAttributes){
        if (eventAttributes != null && eventAttributes.size() > 0 && filter != null && filter.size() > 0) {
            Iterator<Map.Entry<String, String>> attIterator = eventAttributes.entrySet().iterator();
            Map<String, String> newAttributes = new HashMap<String, String>();
            while (attIterator.hasNext()) {
                Map.Entry<String, String> entry = attIterator.next();
                String key = entry.getKey();
                int hash = hash(eventType + eventName + key);
                if (filter.get(hash, true)) {
                    newAttributes.put(key, entry.getValue());
                }
            }
            return newAttributes;
        }else{
            return eventAttributes;
        }
    }

    public abstract String getName();

    public abstract boolean isOriginator(String uri);

    protected abstract EmbeddedProvider update();

    public JSONObject filterAttributes(SparseBooleanArray attributeFilters, JSONObject attributes) {
        if (attributes != null && attributeFilters != null && attributeFilters.size() > 0
                && attributes.length() > 0) {
            Iterator<String> attIterator = attributes.keys();
            JSONObject newAttributes = new JSONObject();
            while (attIterator.hasNext()) {
                String entry = attIterator.next();
                int hash = hash(entry);
                if (attributeFilters.get(hash, true)) {
                    try {
                        newAttributes.put(entry, attributes.getString(entry));
                    }catch (JSONException jse){

                    }
                }
            }
            return newAttributes;
        }else{
            return attributes;
        }
    }

    public boolean shouldSetIdentity(MParticle.IdentityType identityType) {
        return mUserIdentityFilters == null || mUserIdentityFilters.size() == 0 || mUserIdentityFilters.get(identityType.getValue(), true);
    }
}
