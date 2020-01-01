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

import jsmpl.entity.Entity;

/**
 * A class used to represent chords in music.
 */
public class Chord extends Note {
	private static final long serialVersionUID = 5740081589302226163L;
	private Note[] notes;
	
	/**
	 * @param notes an array of notes in the chord
	 * 
	 * Note that the super constructor is called such
	 * that:
	 * 	getFrequency() returns the frequency of the first note in the chord.
	 * 	getLength() returns the length of the first note in the chord.
	 * 	getVolume() returns the volume of teh first note in the chord.
	 * 
	 * An important PRECONDITION: all notes in the chord should have the same
	 * 	length.
	 */
	public Chord(Note... notes) {
		super(notes.length == 0 ? 0.0 : notes[0].getFrequency(),
				notes.length == 0 ? 0.0 : notes[0].getLength(),
				notes.length == 0 ? 0.0 : notes[0].getVolume());
		this.notes = notes;
	}
	
	public int getSize() {
		return notes.length;
	}
	
	/**
	 * Gets a note in the chord.
	 * 
	 * @param index the index of the note to retrieve
	 * @return the note at the index
	 */
	public Note getComponent(int index) {
		return notes[index];
	}
	
	/**
	 * @return if there is only one note in the chord, returns true,
	 * 	because this would mean that the chord is essentially a note.
	 */
	public boolean isNote() {
		return notes.length == 1;
	}
	
	public double[] produceData(Entity ent, double samplingRate) {
		int sampleCount = (int) (getLength() * samplingRate);
		double[] mltWaveData = new double[sampleCount];
		
		for (int i = 0; i < notes.length; i++) {
			double[] sngWaveData = notes[i].produceData(ent, samplingRate);
			for (int j = 0; j < sngWaveData.length; j++) {
				mltWaveData[j] = sngWaveData[j] * (1.0 / notes.length);
			}
		}
		
		return mltWaveData;
	}
	
	public boolean isRest() {
		return isNote() && super.isRest();
	}
	
	public boolean equals(Object o) {
		if (!(o instanceof Chord)) {
			return false;
		}
		
		Chord other = (Chord) o;
		if (this.getSize() != other.getSize()) {
			return false;
		} else {
			for (int i = 0; i < this.getSize(); i++) {
				if (!this.notes[i].equals(other.notes[i])) {
					return false;
				}
			}
			
			return true;
		}
	}
	
	public String toString() {
		StringBuilder stringArr = new StringBuilder();
		stringArr.append("[ ");
		
		for (Note n : notes) {
			stringArr.append(n.toString());
			stringArr.append(" ");
		}
		stringArr.append("]");
		
		return stringArr.toString();
	}
}
