package com.net2plan.gis.importer.test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import com.net2plan.gis.importer.GisLibrary.*;

public class Test {

	public static void main(String[] args) throws Throwable {
		
		List<GisMultilayer> gmlL = new ArrayList<GisMultilayer>();
		GisMultilayer gml_C = new GisMultilayer("Cartagena");
		GisMultilayer gml_L = new GisMultilayer("Lorca");
		gmlL.add(gml_C);
		gmlL.add(gml_L);

		//GisImporter gi = new GisImporter();
		List<File> files = new ArrayList<File>();
		
		File Edificios = new File("C:/Users/jlrg_/Desktop/UPCT/QGIS/OSM2QGIS/Edificios.geojson");
		File Carreteras = new File("C:/Users/jlrg_/Desktop/UPCT/QGIS/OSM2QGIS/Carreteras.geojson");
		File Luminarias = new File("C:/Users/jlrg_/Desktop/UPCT/QGIS/OSM2QGIS/Luminarias.geojson");

		// Cargamos un gml de prueba
		files.add(Edificios);
		files.add(Edificios);
		//files.add(path1);
		gml_C.buildFromGeoJson(gml_C, files);

		// Cargamos otro gml de prueba
		files.clear();
		files.add(Carreteras);
		files.add(Luminarias);
		gml_L.buildFromGeoJson(gml_L, files);

		ListIterator<GisMultilayer> gml_iterator = gmlL.listIterator();
		while (gml_iterator.hasNext()) { // 1
			GisMultilayer gml = gml_iterator.next();
			
			/*
			System.out.println("CARGAR GL A MANO");
			gml.addLayer(path1);
			System.out.println(gml.getLayerNames());
			System.out.println(gml.getLayer("Carreteras").name);
			gml.setName("Lorca");
			System.out.println(gml.getLayer("Carreteras").getGml().getName());
			*/
			
			System.out.println("/////////////////////////////////////////////");
			System.out.println("GML name: " + gml.getName());
			System.out.println("Layer names del gml " + gml.getLayerNames());
			System.out.println("Buildings layers loaded: " + gml.getLayersByName("Edificios").size());
			System.out.println("Roads layers loaded: " + gml.getLayersByName("Carreteras").size());
			System.out.println("layer name from id=1 : " + gml.getLayerName(1L));
			
			/*GisLayer layer = gml.getLayers().get(1L);
			System.out.println("Setting name to Totana...");
			gml.setName("Totana");
			System.out.println("GML name loaded from gml: "+gml.getName());
			System.out.println("GML name loaded from layer 1: "+layer.getGml().getName());*/

			Map<Long, GisLayer> layers = gml.getLayers();

			// Imprimimos el Map con un Iterador
			Iterator<Long> gl_iterator = layers.keySet().iterator();
			System.out.println("#/#/#/#/#/#/#/#/#/#/#/#/#/#/#/#/#/#/#/");
			while (gl_iterator.hasNext()) { // 2
				Long key = gl_iterator.next();
				GisLayer gl = gml.getLayer(key);
				System.out.println("#### " + key +": "+gl.getName() + " ####");
				Collection<GisObject> goc = gl.getObjects().values();
				Iterator<GisObject> object_iterator = goc.iterator();
				int counter = 0;
				if (gl.isBuildingsLayer()) {
					while (object_iterator.hasNext() && counter < 10) { // 3.1
						Building go = (Building) object_iterator.next();
						System.out.println("Object id: " + go.getId());
						System.out.println("Coordinates: " + go.getGeoJSONFeatures().geometry.coordinates);
						System.out.println("Radius: " + go.getRadius());
						System.out.println("Center: " + go.getCenter());
						counter++;
					}
				} else if (gl.isRoadsLayer()) {
					while (object_iterator.hasNext() && counter < 10) { // 3.2
						Road go = (Road) object_iterator.next();
						System.out.println("Object id: " + go.getId());
						System.out.println("Coordinates: " + go.getGeoJSONFeatures().geometry.coordinates);
						System.out.println("Origin point: " + go.getOriginPoint().toString());
						System.out.println("End point: " + go.getEndPoint());
						counter++;
					}
				}else if (gl.isLuminairesLayer()) {
					while (object_iterator.hasNext() && counter < 10) { // 3.2
						Luminaire go = (Luminaire) object_iterator.next();
						System.out.println("Object id: " + go.getId());
						System.out.println("Coordinates: " + go.getGeoJSONFeatures().geometry.coordinates);
						System.out.println("Point: " + go.getPoint());
						counter++;
					}
				}

				System.out.println("#*#*#*#*#*#*#*#*#*#*#*#*#*#*#*#*#*#*");
			}

		}

	}
}
