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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jsmpl.entity.Entity;
import jsmpl.util.Dynamics;


/**
 * A class used to represent Notes in music. 
 * Each note has three fields:
 * 	1.) a Frequency
 * 	2.) a Length
 * 	3.) a Volume
 * */
public class Note {
	private double frequency;
	private double length;
	private double volume;
	
	/**
	 * @param frequency the frequency of a pitch as a double
	 * @param length the length of the note
	 */
	public Note(double frequency, double length) {
		this(frequency, length, Dynamics.MF.value);
	}
	
	/**
	 * @param frequency the frequency of a pitch as a string.
	 * 	The pitch should be of the form "<pitch><octave>". For
	 * 	example, middle C would be "C4", and the C# directly above
	 * 	that would be "C+4". The B-flat below that would be 
	 * 	"B-3".
	 * @param length the length of the note.
	 */
	public Note(String frequency, double length) {
		this(fStringToFloat(frequency), length);
	}
	
	/**
	 * @param frequency the frequency of the pitch as a double
	 * @param length the length of the note
	 * @param volume the volume of the note as a double
	 */
	public Note(double frequency, double length, double volume) {
		this.frequency = frequency;
		this.length = length;
		this.volume = volume;
	}
	
	/**
	 * @param frequency the frequency of a pitch as a string.
	 * 	The pitch should be of the form "<pitch><octave>". For
	 * 	example, middle C would be "C4", and the C# directly above
	 * 	that would be "C+4". The B-flat below that would be 
	 * 	"B-3".
	 * @param length the length of the note
	 * @param volume the length of the volume as a double
	 */
	public Note(String frequency, double length, double volume) {
		this(fStringToFloat(frequency), length, volume);
	}
	
	/**
	 * @param frequency the frequency of a pitch as a string.
	 * 	The pitch should be of the form "<pitch><octave>". For
	 * 	example, middle C would be "C4", and the C# directly above
	 * 	that would be "C+4". The B-flat below that would be 
	 * 	"B-3".
	 * @param length the length of the note
	 * @param volume the volume of the note as a string. The pitch
	 * 	can be represented as any value from "ppp" to "fff".
	 */
	public Note(String frequency, double length, String volume) {
		this(fStringToFloat(frequency), length, Dynamics.valueOf(volume).value);
	}
	
	/**
	 * @return true if the frequency or volume of the note is 0, false otherwise
	 */
	public boolean isRest() {
		return frequency == 0.0 || volume == 0.0;
	}
	
	/**
	 * @param ent the Entity to play the note with
	 * @param samplingRate the sampling rate of the resulting samples
	 * @return a array of sample data represented as doubles
	 */
	public double[] produceData(Entity ent, double samplingRate) {
		return ent.playPitch(frequency, length, volume, samplingRate);
	}
	
	public double getFrequency() {
		return frequency;
	}
	
	public double getLength() {
		return length;
	}
	
	public double getVolume() {
		return volume;
	}
	
	public boolean equals(Object other) {
		if (!(other instanceof Note)) {
			return false;
		}
		
		Note otherNote = (Note) other;
		return this.frequency == otherNote.frequency 
				&& this.length == otherNote.length
				&& this.volume == otherNote.volume;
	}
	
	public int hashCode() {
		return (int) (frequency + length + volume);
	}
	
	public String toString() {
		return "[F=" + frequency + ", L=" + length + "]";
	}
	
	private static enum Pitch {
		A (0),
		B (2),
		C (-9),
		D (-7),
		E (-5),
		F (-4),
		G (-2);
		
		public final int diff;
		
		private Pitch(int d) {
			diff = d;
		}
	}
	
	private static double fStringToFloat(String fstr) {
		String fsRegex = "([A-G][+-]?)(\\d+?)";
		Pattern pattern = Pattern.compile(fsRegex);
		Matcher match = pattern.matcher(fstr);
		
		if (match.matches()) {
			String note = match.group(1);
			int octave = Integer.parseInt(match.group(2));
			
			char letter = note.charAt(0);
			int adjust = note.length() > 1 ? (note.charAt(1) == '+' ? 1 : -1) : 0;
			
			double freqPow = (Pitch.valueOf(letter + "").diff + adjust) / 12.0 + (octave - 4);
			return 440.0 * Math.pow(2.0, freqPow);
		} else {
			return 0.0;
		}
	}
}
