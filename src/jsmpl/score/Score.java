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

package jsmpl.score;

import java.util.List;
import java.util.TreeMap;

import jsmpl.direction.MusicalDirection;
import jsmpl.direction.PiecewiseMusicalDirection;
import jsmpl.function.Generator;

/**
 * The Score class represents a score in the traditional sense,
 * with a set of instruments that can be added or removed from.
 * The instruments are stored in a TreeMap, meaning that if the
 * user wants, they can add instruments before others; for example,
 * one can complete the second violin part before the first violin
 * part without much hassle.
 */
public class Score {
	private TreeMap<Integer, Instrument> instruments;
	private TreeMap<Integer, PiecewiseMusicalDirection> directions;
	
	public Score() {
		instruments = new TreeMap<Integer, Instrument>();
		directions = new TreeMap<Integer, PiecewiseMusicalDirection>();
	}
	
	public TreeMap<Integer, Instrument> getInstruments() {
		return instruments;
	}
	
	public Instrument getInstrument(int id) {
		return instruments.get(id);
	}
	
	public int getInstrumentCount() {
		return instruments.size();
	}
	
	public int add(Instrument instrument) {
		return add(instrument, null);
	}
	
	public int add(Instrument instrument, MusicalDirection direction) {
		return insert(instrument, direction, instruments.size() > 0 ? instruments.lastKey() + 1 : 0);
	}
	
	public int insert(Instrument instrument, int index) {
		return insert(instrument, null, index);
	}
	
	public int insert(Instrument instrument, MusicalDirection direction, int index) {
		if (instruments.containsKey(index)) {
			shift(index);
		}
		
		instruments.put(index, instrument);
		directions.put(index, conformToPiecewise(direction));
		
		return index;
	}
	
	public void setDirection(MusicalDirection direction, int index) {
		if (!instruments.containsKey(index)) {
			throw new IllegalArgumentException("There is no instrument at index " + index + ".");
		}
		
		directions.put(index, conformToPiecewise(direction));
	}
	
	public void copyDirection(int from, int to) {
		if (!instruments.containsKey(from)) {
			throw new IllegalArgumentException("There is no instrument at index " + from + ".");
		}
		
		directions.put(to, directions.get(from));
	}
	
	/**
	 * Creates a dictionary containing the developed samples for each instrument.
	 * 
	 * @param samplingRate the sampling rate
	 * @param verbose 1 to include console statements
	 * @return a dictionary containing the developed samples for each instrument.
	 */
	public TreeMap<Integer, List<Double[][]>> developAllInstruments(double samplingRate, int verbose) {
		TreeMap<Integer, List<Double[][]>> scoreDataMap = new TreeMap<Integer, List<Double[][]>>();
		
		Integer key = instruments.firstKey();
		while (key != null) {
			Instrument ins = instruments.get(key);
			PiecewiseMusicalDirection dir = directions.get(key);
			scoreDataMap.put(key, ins.developParts(dir, samplingRate, verbose));
			
			key = instruments.lowerKey(key);
		}
		
		return scoreDataMap;
	}
	
	/**
	 * This method is a memory-friendly option for developing instruments. Rather than
	 * developing all instruments and storing the samples in a TreeMap, this function
	 * stores a generator object such that when the object's generate method is called,
	 * the user will obtain the samples.
	 * 
	 * @param samplingRate the sampling rate
	 * @param verbose 1 to include console statements
	 * @return a dictionary containing generators that can generate samples for each instrument.
	 */
	public TreeMap<Integer, Generator<List<Double[][]>>> developInstrumentsAsReference(
			double samplingRate, int verbose) 
	{
		TreeMap<Integer, Generator<List<Double[][]>>> scoreDataRef = 
				new TreeMap<Integer, Generator<List<Double[][]>>>();
		
		if (instruments.size() == 0) {
			return scoreDataRef;
		}
		
		Integer key = instruments.firstKey();
		while (key != null) {
			Instrument ins = instruments.get(key);
			PiecewiseMusicalDirection dir = directions.get(key);
			scoreDataRef.put(key, new Generator<List<Double[][]>>() {

				@Override
				public List<Double[][]> generate() {
					return ins.developParts(dir, samplingRate, verbose);
				}
				
			});
			
			key = instruments.lowerKey(key);
		}
		
		return scoreDataRef;
	}
	
	/**
	 * Checks if a direction is Piecewise. If not, then casts to Piecewise.
	 * 
	 * @param direction the musical direction
	 * @return the new musical direction, casted.
	 */
	private PiecewiseMusicalDirection conformToPiecewise(MusicalDirection direction) {
		if (direction == null) {
			return null;
		} else if (!(direction instanceof PiecewiseMusicalDirection)) {
			return new PiecewiseMusicalDirection(direction);
		} else {
			return (PiecewiseMusicalDirection) direction;
		}
	}
	
	private void shift(int from) {
		int index = instruments.lastKey() + 1;
		
		while (index > from) {
			if (!instruments.containsKey(index)) {
				index--;
				continue;
			}
			
			instruments.put(index, instruments.get(index - 1));
			directions.put(index, directions.get(index - 1));
			index--;
		}
	}
}
