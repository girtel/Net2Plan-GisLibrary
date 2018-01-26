package com.net2plan.gis.importer;

import java.awt.geom.Point2D;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class Building extends GisObjectPablo
{
	private String type;
	
	public Point2D getCenter () 
	{ 
		final int numPoints = this.getTrack().size();
		final double xAccum = this.getTrack().stream().mapToDouble(p->p.getX()).sum();
		final double yAccum = this.getTrack().stream().mapToDouble(p->p.getY()).sum();
		return new Point2D.Double(xAccum / numPoints , yAccum / numPoints);
	}
	public double getRadius () 
	{ 
		final Point2D center = getCenter ();
		final double maxdis_x = this.getTrack().stream().mapToDouble(p->Math.abs(p.getX() - center.getX())).max ().orElse(0);
		final double maxdis_y = this.getTrack().stream().mapToDouble(p->Math.abs(p.getY() - center.getY())).max ().orElse(0);
		return Math.max(maxdis_x, maxdis_y);
	}
	
	
	
	
	
}
