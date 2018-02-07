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
		
		File path = new File("C:/Users/jlrg_/Desktop/UPCT/QGIS/OSM2QGIS/Edificios.geojson");
		File path1 = new File("C:/Users/jlrg_/Desktop/UPCT/QGIS/OSM2QGIS/Carreteras.geojson");

		// Cargamos un gml de prueba
		files.add(path);
		//files.add(path1);
		gml_C.buildFromGeoJson(files);

		// Cargamos otro gml de prueba
		files.clear();
		files.add(path1);
		gml_L.buildFromGeoJson(files);

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
			Map<String, GisLayer> layers = gml.getLayers();

			// Imprimimos el Map con un Iterador
			Iterator<String> gl_iterator = layers.keySet().iterator();
			System.out.println("#/#/#/#/#/#/#/#/#/#/#/#/#/#/#/#/#/#/#/");
			while (gl_iterator.hasNext()) { // 2
				String key = gl_iterator.next();
				System.out.println("#### " + key + " ####");
				GisLayer gl = gml.getLayer(key);
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

					System.out.println("#/#/#/#/#/#/#/#/#/#/#/#/#/#/#/#/#/#/#/");
				}

				System.out.println("#*#*#*#*#*#*#*#*#*#*#*#*#*#*#*#*#*#*");
			}

		}

	}
}
