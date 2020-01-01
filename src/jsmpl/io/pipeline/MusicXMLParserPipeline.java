/**
 * Copyright (C) 2019 Suhas Vittal
 *
 *  This file is part of Stoch-MPL.
 *
 *  Stoch-MPL is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Stoch-MPL is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License
 *  along with Stoch-MPL.  If not, see <https://www.gnu.org/licenses/>.
 * */

package jsmpl.io.pipeline;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jsmpl.direction.PiecewiseMusicalDirection;
import jsmpl.entity.Entity;
import jsmpl.musicxml.MusicXML;
import jsmpl.score.Instrument;
import jsmpl.score.Layer;
import jsmpl.score.Score;

/**
 * A pipeline class that creates objects that parse a text file (the src)
 * and puts the relevant information into a Score object (the destination).
 * 
 * The transferDataBetween requires the following keyword arguments:
 * 		entities (Type List<Entity>) - used to provide entities for each
 * 			instrument found in the XML file.
 * Make sure to pass a kwargs object containing the appropriate key-value
 * pairs to avoid errors.
 */
public class MusicXMLParserPipeline extends Pipeline<String, Score> {
	public static final Map<String, Class<?>> REQUIRED_KWARGS_AND_TYPES;
	
	static {
		Map<String, Class<?>> rkat = new HashMap<String, Class<?>>();
		rkat.put("entities", List.class);
		
		REQUIRED_KWARGS_AND_TYPES = rkat;
	}
	
	@Override
	public void transferDataBetween(Object src, Object dest, Map<String, Object> kwargs) {
		super.transferDataBetween(src, dest, kwargs); 
		// the Pipeline variant checks the types for us, so we just have to cast
		// the inputs. If the types are invalid, then a ClassCastException is 
		// thrown.
		
		transferDataBetween((String) src, (Score) dest, kwargs);
	}
	
	@SuppressWarnings("rawtypes")
	public void transferDataBetween(String src, Score dest, Map<String, Object> kwargs) {
		/*
		 * Required kwargs:
		 * 	"entities" (Type List<Entity>)
		 * */
		
		validateRequiredKwargs(REQUIRED_KWARGS_AND_TYPES, kwargs);
		
		List entities = (List) kwargs.get("entities");  // don't know type of individual elements
		
		try {
			Map<String, Map<String, Object>> scoreAttr = MusicXML.parseXML(new File(src), kwargs);
			int entityIndex = 0;
			
			for (String key : scoreAttr.keySet()) {
				Map<String, Object> attr = scoreAttr.get(key);
				String name = (String) attr.get("partName");
				PiecewiseMusicalDirection direction = (PiecewiseMusicalDirection) attr.get("directionData");
				Layer[] partData = (Layer[]) attr.get("partData");
			
				// now initialize objects
				// first initialize the instrument
				Object objEnt = entities.get(entityIndex++ % entities.size());
				// check if objEnt is an Entity; if not, throw an exception
				if (!(objEnt instanceof Entity)) {
					throw new ClassCastException(
							"Element of keyword \"entities\" is not an instance of class \"Entity\".");
				}
				Entity e = (Entity) objEnt;
				
				Instrument instrument = new Instrument(name, e, partData.length);
				// now initialize each voice of the instrument
				for (int i = 0; i < partData.length; i++) {
					instrument.setVoice(partData[i], i);
				}
				
				dest.add(instrument, direction);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
