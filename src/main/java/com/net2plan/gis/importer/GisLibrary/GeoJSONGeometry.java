package com.net2plan.gis.importer.GisLibrary;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GeoJSONGeometry
{
	public String type;
	public Object coordinates;
	
	public GeoJSONGeometry (@JsonProperty("type") String type, @JsonProperty("coordinates") Object coordinates){
		this.type = type;
		this.coordinates = coordinates;
	}
	
}