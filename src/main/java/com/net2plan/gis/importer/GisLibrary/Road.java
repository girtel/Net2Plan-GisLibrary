package com.net2plan.gis.importer.GisLibrary;

import java.awt.geom.Point2D;
import java.util.List;

public class Road extends GisObject
{

	public Road(GeoJSONFeature jlf, GisLayer belongingLayer, long id/*, String idAccordingToGeoJsonFile, List<Point2D> track*/)
	{
		super(jlf, belongingLayer, id/*, idAccordingToGeoJsonFile, track*/);
	}
	
	
	public Point2D getOriginPoint () { return this.getTrack().get(0); }
	
	public Point2D getEndPoint () { return this.getTrack().get(this.getTrack().size()-1); }
	
	public boolean hasTrack () { return !this.getTrack().isEmpty(); } 
}


