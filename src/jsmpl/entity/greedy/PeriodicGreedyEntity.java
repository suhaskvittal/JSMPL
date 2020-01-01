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

package jsmpl.entity.greedy;

import jsmpl.entity.Entity;
import jsmpl.function.ADSRFunction;
import jsmpl.function.PeriodicWaveform;

/**
 * The PeriodicGreedyEntity class implements dynamic programming
 * by memorizing a period of a waveform, and then using a lookup
 * table to compute subsequent iterations of the period. For example,
 * if we want a sound of length 8 and our waveform has a period of 2,
 * we record the waveform from 0 <= t < 2, and reference our records
 * for t >= 2.
 * 
 * As a result, the PeriodicGreedyEntity was the best performer in 
 * benchmarks and does not diminish the quality of the sound in any
 * way. However, the class is not compatible with non-periodic 
 * waveforms (i.e. noisy waveforms). But for the basic waveforms, such
 * as sine, triangle, sawtooth, and square, the class synergizes well.
 */
public class PeriodicGreedyEntity extends Entity {
	/**
	 * 
	 */
	private static final long serialVersionUID = 7270935762843423756L;
	private double period;
	
	/**
	 * @param wf The waveform function that the Entity emits.
	 * @param attack a double from 0.0 to 1.0 that is the proportion of time spent in attack
	 * @param decay a double from 0.0 to 1.0 that is the proportion of time spent in decay 
	 * @param sustain a double from 0.0 to 1.0 that is the amplitude for sustain
	 * @param release a double from 0.0 to 1.0 that is the proportion of time spent in release.
	 */
	public PeriodicGreedyEntity(PeriodicWaveform wf, double attack, double decay,
			double sustain, double release)
	{
		super(wf, attack, decay, sustain, release);
		this.period = wf.getPeriod();
	}
	
	/**
	 * @param wf The waveform function that the Entity emits.
	 * @param attack a double from 0.0 to 1.0 that is the proportion of time spent in attack
	 * @param decay a double from 0.0 to 1.0 that is the proportion of time spent in decay 
	 * @param sustain a double from 0.0 to 1.0 that is the amplitude for sustain
	 * @param release a double from 0.0 to 1.0 that is the proportion of time spent in release
	 * @param adsr the envelope function
	 */
	public PeriodicGreedyEntity(PeriodicWaveform wf, double attack, double decay, 
			double sustain, double release, ADSRFunction adsr) 
	{
		super(wf, attack, decay, sustain, release, adsr);
		this.period = wf.getPeriod();
	}
	
	/**
	 * @param e an Entity to build from. Note that the waveform of the Entity
	 * must implement PeriodicWaveform; otherwise, there will be a error.
	 */
	public PeriodicGreedyEntity(Entity e) {
		this((PeriodicWaveform) e.getWaveform(), e.getAttack(), e.getDecay(),
				e.getSustain(), e.getRelease(), e.getADSR());
	}
	
	public double[] playPitch(double frequency, double time, double amplitude, 
			double samplingRate) 
	{	
		int numberOfSamples = (int) (time * samplingRate);
		double[] waveData = new double[numberOfSamples];
		
		if (frequency == 0.0) {
			return waveData;
		}
		
		double adjustedPeriod = period / (2 * Math.PI * frequency);
		int numberOfSamplesPerPeriod = (int) (adjustedPeriod * samplingRate);
		
		double[] lookupTable = new double[numberOfSamplesPerPeriod];
		
		for (int i = 0; i < numberOfSamples; i++) {
			double v;
			if (i < numberOfSamplesPerPeriod) {
				v = rawWaveFunction(frequency, ((double) i) / numberOfSamples * time);
				lookupTable[i] = v;
			} else {
				v = lookupTable[i % numberOfSamplesPerPeriod];
			}
			waveData[i] = v * getADSR().f(((double) i) / numberOfSamples,
					getAttack(), getDecay(), getSustain(), getRelease());
		}
		
		return waveData;
	}
	
	protected double rawWaveFunction(double frequency, double currentTime) {
		return getWaveform().f(2 * Math.PI * frequency * currentTime);
	}
}
