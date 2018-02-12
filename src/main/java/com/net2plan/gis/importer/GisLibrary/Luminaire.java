package com.net2plan.gis.importer.GisLibrary;

import java.awt.geom.Point2D;
import java.util.List;

public class Luminaire extends GisObject
{

	public Luminaire(GeoJSONParser.GeoJSONFeature geoJSONF, GisLayer belongingLayer, long id/*, String idAccordingToGeoJsonFile, List<Point2D> track*/)
	{
		super(geoJSONF, belongingLayer, id/*, idAccordingToGeoJsonFile, track*/);
	}
	
	public Point2D getPoint () { return this.getTrack().get(0); }

}