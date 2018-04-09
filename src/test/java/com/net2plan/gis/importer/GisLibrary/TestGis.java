package com.net2plan.gis.importer.GisLibrary;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestGis {

	GisMultilayer gml;
	Map<Long, GisLayer> layers;
	
	@Before
	public void setUp()
	{
		gml = new GisMultilayer("Cartagena");
		
		//GisImporter gi = new GisImporter();
		List<File> files = new ArrayList<File>();
		
		File Edificios = new File("C:/Users/jlrg_/Desktop/UPCT/QGIS/OSM2QGIS/Edificios.geojson");
		File Carreteras = new File("C:/Users/jlrg_/Desktop/UPCT/QGIS/OSM2QGIS/Carreteras.geojson");
		File Luminarias = new File("C:/Users/jlrg_/Desktop/UPCT/QGIS/OSM2QGIS/Luminarias.geojson");

		// Cargamos un gml de prueba
		files.add(Edificios);
		files.add(Edificios);
		files.add(Carreteras);
		files.add(Luminarias);
		gml.buildFromGeoJson(Arrays.asList(Edificios , Edificios , Carreteras , Luminarias));
		layers = gml.getLayers();
	}
	

	@After
	public void tearDown() {
	}

	@Test
	public void testLayers() 
	{
		
		assert this.gml.getLayersByName("Edificios").size() == 2;			//More than 1 layer with the same name
		gml.setName("Murcia");	
		assert this.gml.getLayer(1L).getGml().getName() == "Murcia";				//Check if the change persists.
		assert this.gml.getLayer(1L).getObject(1L).getGml().getName() == "Murcia";	//

	}
	
	@Test
	public void testObjects() {

		Iterator<Long> gl_iterator = layers.keySet().iterator();
		while (gl_iterator.hasNext()) {
			Long key = gl_iterator.next();
			GisLayer gl = gml.getLayer(key);
			System.out.println("#### " + key + ": " + gl.getName() + " ####");
			Collection<GisObject> goc = gl.getObjects().values();
			Iterator<GisObject> object_iterator = goc.iterator();
			
			while (object_iterator.hasNext()) { // 3.1
				GisObject go = object_iterator.next();
				assert  go.getId() > 0;
				assert  go.getGeoJSONFeatures().geometry.coordinates != null;
				assert go.getGeometryType() != null;
			}
		}

	}

	@Test
	public void testBuildings() {

		Iterator<Long> gl_iterator = layers.keySet().iterator();
		while (gl_iterator.hasNext()) { // 2
			Long key = gl_iterator.next();
			GisLayer gl = gml.getLayer(key);
			System.out.println("#### " + key + ": " + gl.getName() + " ####");
			Collection<GisObject> goc = gl.getObjects().values();
			Iterator<GisObject> object_iterator = goc.iterator();
			if (gl.isBuildingsLayer()) {
				while (object_iterator.hasNext()) { // 3.1
					Building go = (Building) object_iterator.next();
					assert go.getRadius() != 0;
					assert go.getCenter() != null;
				}
			}
		}

	}
	
	@Test
	public void testRoads() {

		Iterator<Long> gl_iterator = layers.keySet().iterator();
		while (gl_iterator.hasNext()) { // 2
			Long key = gl_iterator.next();
			GisLayer gl = gml.getLayer(key);
			System.out.println("#### " + key + ": " + gl.getName() + " ####");
			Collection<GisObject> goc = gl.getObjects().values();
			Iterator<GisObject> object_iterator = goc.iterator();
			if (gl.isRoadsLayer()) {
				while (object_iterator.hasNext()) { // 3.1
					Road go = (Road) object_iterator.next();
					assert go.getOriginPoint() != null;
					assert go.getEndPoint() != null;
				}
			}
		}

	}
	
	@Test
	public void testLuminaires() {

		Iterator<Long> gl_iterator = layers.keySet().iterator();
		while (gl_iterator.hasNext()) { // 2
			Long key = gl_iterator.next();
			GisLayer gl = gml.getLayer(key);
			System.out.println("#### " + key + ": " + gl.getName() + " ####");
			Collection<GisObject> goc = gl.getObjects().values();
			Iterator<GisObject> object_iterator = goc.iterator();
			if (gl.isLuminairesLayer()) {
				while (object_iterator.hasNext()) { // 3.1
					Luminaire go = (Luminaire) object_iterator.next();
					assert go.getPoint() != null;
				}
			}
		}

	}
	
	
	
	

}
