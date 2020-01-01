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
import jsmpl.function.Waveform;

/**
 * 
 * The LinearGreedyEntity class is a greedy variant of the Entity class
 * (as the name suggests). The "greediness" of the class is where the
 * waveform is linearly approximated between two different points, in 
 * order to save computation time as consecutive linear approximations
 * (for, say, 2 points) is constant.
 * 
 * However, note there is a drawback for skipping two many points: the
 * sound produced is more tinny and less perfect. To maintain a reasonable
 * amount of fidelity, skipping 5 points is sufficient to see a boost
 * in performance.
 * 
 * In benchmarks, this entity performs the least best amongst greedy 
 * entities but somewhat better than a normal entity.
 */
public class LinearGreedyEntity extends Entity {
	/**
	 * 
	 */
	private static final long serialVersionUID = -2118248338935841396L;
	private int greedIndex;
	
	/**
	 * @param wf the waveform
	 * @param attack the attack of the envelope
	 * @param decay the decay
	 * @param sustain the sustain
	 * @param release the release
	 * @param greed the greed parameter is a count of how many
	 * 	data points to skip over (that is, how many to approximate).
	 */
	public LinearGreedyEntity(Waveform wf, double attack, double decay,
			double sustain, double release, int greed)
	{
		super(wf, attack, decay, sustain, release);
		this.greedIndex = greed + 1;  
		// note that a greed of 1 means to skip 1;
		// we add one because adding 2 to an array index
		// is skipping exactly 1.
	}
	
	/**
	 * @param wf the waveform
	 * @param attack the attack of the envelope
	 * @param decay the decay
	 * @param sustain the sustain
	 * @param release the release
	 * @param adsr the adsr envelope function
	 * @param greed the greed parameter is a count of how many
	 * 	data points to skip over (that is, how many to approximate).
	 */
	public LinearGreedyEntity(Waveform wf, double attack, double decay, 
			double sustain, double release, ADSRFunction adsr, int greed) 
	{
		super(wf, attack, decay, sustain, release, adsr);
		this.greedIndex = greed + 1;
	}
	
	/**
	 * @param entity the entity to build the greedy entity from.
	 * 	Note that this does not allow the user to chain linear greedy
	 *  entities and other entities together; rather, it uses attributes
	 *  from another entity (such as waveform, attack, decay, etc.) and
	 *  creates a linear greedy entity.
	 * @param greed the greed parameter is a count of how many
	 * 	data points to skip over (that is, how many to approximate).
	 */
	public LinearGreedyEntity(Entity entity, int greed) {
		this(entity.getWaveform(), entity.getAttack(), entity.getDecay(),
				entity.getSustain(), entity.getRelease(), entity.getADSR(), greed);
	}
	
	public int getGreediness() {
		return greedIndex;
	}
	
	@Override
	public double[] playPitch(double frequency, double time, double amplitude, 
			double samplingRate) 
	{
		int numberOfSamples = (int) (time * samplingRate);
		double[] waveData = new double[numberOfSamples];
		
		waveData[0] = amplitude * super.waveFunction(frequency, 0.0, time);
		int index = 0;
		while (index < numberOfSamples) {
			index += greedIndex;
			
			// bound index in case it is greater than or equal to numberOfSamples
			if (index >= numberOfSamples) {
				index = numberOfSamples - 1;
			}
			
			waveData[index] = amplitude * super.waveFunction(frequency, 
					((double) index) / numberOfSamples * time, time);
			// approximate every value between (index - greedIndex) and index.
			
			int prev = index - greedIndex;
			double fs = waveData[index];
			double is = waveData[prev];
			double localDelta = (fs - is) / greedIndex;
			
			double value = localDelta * ((index - greedIndex) - prev) + is;
			
			for (int i = index - greedIndex; i < index; i++) {
				waveData[i] = value;
				value += localDelta;
			}
			
			index++;
		}
		
		return waveData;
	}
}
