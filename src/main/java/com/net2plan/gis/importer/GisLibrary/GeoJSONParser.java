package com.net2plan.gis.importer.GisLibrary;

import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GeoJSONParser {
	// cada una de las partes del geojson
	// {"type": "_", "name":"_", "crs": {"type":"_", "properties": { _ } },
	// "features": [{}, {}, {}] }
	public String type;
	public String name;
	public GeoJSONCrs crs;
	public List<GeoJSONFeature> features;

	public GeoJSONParser(@JsonProperty("type") String type, @JsonProperty("name") String name,
			@JsonProperty("crs") GeoJSONCrs crs, @JsonProperty("features") List<GeoJSONFeature> features) {
		this.type = type;
		this.name = name;
		this.crs = crs;
		this.features = features;
	}
	
	public static class GeoJSONCrs
	{
		public String type;
		public SortedMap<String,String> properties;
		
		public GeoJSONCrs (@JsonProperty("type") String type, @JsonProperty("properties") SortedMap<String,String> properties){
			this.type = type;
			this.properties = properties;
		}
		
	}
	
	public static class GeoJSONFeature{
    	public String type;
    	public SortedMap<String,Object> properties;
    	public GeoJSONGeometry geometry;
    	
    	/*public JoseLuisFeatures (@JsonProperty("type") String type, @JsonProperty("properties") Map<String,String> properties,
    						@JsonProperty("geometry") Map<String, Object> geometry){*/
    		
    	public GeoJSONFeature (@JsonProperty("type") String type, @JsonProperty("properties") SortedMap<String,Object> properties,
    			@JsonProperty("geometry") GeoJSONGeometry geometry){
    	
    		this.type = type;
    		this.properties = properties;
    		this.geometry = geometry;
    	}
    }
	
	public static class GeoJSONGeometry
	{
		public String type;
		public Object coordinates;
		
		public GeoJSONGeometry (@JsonProperty("type") String type, @JsonProperty("coordinates") Object coordinates){
			this.type = type;
			this.coordinates = coordinates;
		}
		
	}
}

