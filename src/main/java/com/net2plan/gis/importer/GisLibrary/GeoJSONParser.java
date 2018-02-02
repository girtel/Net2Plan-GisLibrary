package com.net2plan.gis.importer.GisLibrary;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;

public class GeoJSONParser
    {
    	//cada una de las partes del geojson
    	//	{"type": "_", "name":"_", "crs": {"type":"_", "properties": { _ } }, "features": [{}, {}, {}] }
        public String type;
        public String name;
        public GeoJSONCrs crs;
        public List<GeoJSONFeature> features;
        
       public GeoJSONParser(@JsonProperty("type") String type, @JsonProperty("name") String name, 
    		   @JsonProperty("crs") GeoJSONCrs crs, @JsonProperty("features") List<GeoJSONFeature> features)
        {
			this.type = type;
			this.name = name;
			this.crs = crs;
			this.features = features;
		}

    }