package com.net2plan.gis.importer.GisLibrary;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GeoJSONFeature{
    	public String type;
    	public Map<String,String> properties;
    	public GeoJSONGeometry geometry;
    	
    	/*public JoseLuisFeatures (@JsonProperty("type") String type, @JsonProperty("properties") Map<String,String> properties,
    						@JsonProperty("geometry") Map<String, Object> geometry){*/
    		
    	public GeoJSONFeature (@JsonProperty("type") String type, @JsonProperty("properties") Map<String,String> properties,
    			@JsonProperty("geometry") GeoJSONGeometry geometry){
    	
    		this.type = type;
    		this.properties = properties;
    		this.geometry = geometry;
    	}
    }