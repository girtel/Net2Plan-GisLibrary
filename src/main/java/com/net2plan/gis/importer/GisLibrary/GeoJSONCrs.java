package com.net2plan.gis.importer.GisLibrary;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GeoJSONCrs
{
	public String type;
	public Map<String,String> properties;
	
	public GeoJSONCrs (@JsonProperty("type") String type, @JsonProperty("properties") Map<String,String> properties){
		this.type = type;
		this.properties = properties;
	}
	
}