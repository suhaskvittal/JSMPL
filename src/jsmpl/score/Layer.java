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
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

/**
 * The Layer class represents a voice of an instrument, or a line
 * in an instrument. Each layer is backed by a stack of chords.
 *
 */
public class Layer {
	private List<Chord> chords;
	private double length;
	private int count;
	
	public Layer() {
		chords = new ArrayList<Chord>();
		length = 0.0;
		count = 0;
	}
	
	public Chord peek() {
		if (length == 0) {
			throw new NoSuchElementException("There are no chords in the Layer");
		}
		
		return chords.get(count);
	}
	
	public Chord pop() {
		if (length == 0) {
			throw new NoSuchElementException("There are no chords in the Layer");
		}
		Chord c = chords.remove(count--);
		length -= c.getLength();
		count--;
		return c;
	}
	
	public void add(Note note) {
		add(new Chord(note));
	}
	
	public void add(Chord chord) {
		length += chord.getLength();
		count++;
		chords.add(chord);
	}
	
	public Chord remove(int index) {
		if (length == 0) {
			throw new NoSuchElementException("There are no chords in the Layer");
		}
		
		Chord c = chords.remove(index);
		length -= c.getLength();
		count--;
		
		return c;
	}
	
	public void insert(Note note, int index) {
		insert(new Chord(note), index);
	}
	
	public void insert(Chord chord, int index) {
		length += chord.getLength();
		count++;
		chords.add(index, chord);
	}
	
	/**
	 * Modifies this layer to include all the chords in another layer
	 * 
	 * @param other the layer to concatenate with this layer
	 */
	public void concatenate(Layer other) {
		for (int i = 0; i < other.count; i++) {
			chords.add(other.chords.get(i));
		}
		count += other.count;
		length += other.length;
	}
	
	/**
	 * @return a stream of chords in the layer
	 */
	public Stream<Chord> getChordStream() {
		return chords.stream();
	}
	
	public double getSize() {
		return length;
	}
	
	/**
	 * This empty method runs in O(1). For quick checks, this is
	 * fine. However, for deep checks, such as checking if the 
	 * chords are also all empty, use isEffectivelyEmpty().
	 * 
	 * @return true if the length of the layer is 0
	 */
	public boolean isEmpty() {
		return length == 0;
	}
	
	/**
	 * @return true if isEmpty or all chords are empty
	 */
	public boolean isEffectivelyEmpty() {
		if (isEmpty()) {
			return true;
		}
		
		for (int i = 0; i < chords.size(); i++) {
			if (!chords.get(i).isRest()) {
				return false;
			}
		}
		
		return true;
	}
	
	public boolean equals(Object o) {
		if (!(o instanceof Layer)) {
			return false;
		} else if (this.count != ((Layer) o).count) {
			return false;
		} else {
			Layer other = (Layer) o;
			
			for (int i = 0; i < this.count; i++) {
				if (!this.chords.get(i).equals(other.chords.get(i))) {
					return false;
				}
			}
			
			return true;
		}
	}
	
	public int hashCode() {
		return count > 0 ? chords.get(0).hashCode() * count : 0;
	}
	
	public String toString() {
		StringBuilder s = new StringBuilder("[");
		for (int i = 0; i < chords.size(); i++) {
			s.append(chords.get(i));
		}
		
		return s.toString();
	}
}
