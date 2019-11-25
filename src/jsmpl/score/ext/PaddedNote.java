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


package jsmpl.score.ext;

import jsmpl.entity.Entity;
import jsmpl.score.Note;
import jsmpl.util.Dynamics;

/**
 * The PaddedNote class extends the Note class
 * by adding padding in front and behind the Note.
 * This class is mostly used with parsing, and has
 * no real practical use in composition.
 */
public class PaddedNote extends Note {
	private Note body;
	private double headPadding, tailPadding;
	
	public PaddedNote(double frequency, double length) {
		this(frequency, length, Dynamics.MF.value, 0.0, 0.0);
	}
	
	public PaddedNote(double frequency, double length, double volume) {
		this(frequency, length, volume, 0.0, 0.0);
	}
	
	public PaddedNote(double frequency, double length, double volume, double headPadding, double tailPadding) {
		super(frequency, headPadding + length + tailPadding, volume);
		
		this.body = new Note(frequency, length, volume);
		this.headPadding = headPadding;
		this.tailPadding = tailPadding;
	}
	
	public double[] produceDate(Entity entity, double samplingRate) {
		double[] samples = new double[(int) (getLength() * samplingRate)];
		
		int start = (int) (headPadding * samplingRate);
		double[] bodySamples = body.produceData(entity, samplingRate);
		for (int i = 0; i < bodySamples.length; i++) {
			samples[i + start] = bodySamples[i];
		}
		
		return samples;
	}
	
	public double getHeadPadding() {
		return headPadding;
	}
	
	public double getTailPadding() {
		return tailPadding;
	}
}
