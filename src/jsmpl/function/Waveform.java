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

package jsmpl.function;

/**
 * An interface for representing waveform functions, such as
 * sine waves or sawtooth waves.
 * */
public interface Waveform {
	public static final Waveform SINE = new Waveform() {
		@Override
		public double f(double t) {
			return Math.sin(t);
		}
	};
	
	public static final Waveform TRIANGLE = new Waveform() {
		@Override
		public double f(double t) {
			double modt = (t + Math.PI / 2) % (2 * Math.PI);  // shift by pi/2 for easy computations
			if (modt < 0) {
				modt += 2 * Math.PI;
			}
			
			if (modt <= Math.PI) {
				return (2 / Math.PI) * modt - 1;
			} else {
				return (-2 / Math.PI) * (modt - Math.PI) + 1;
			}
		}
	};
	
	public static final Waveform SQUARE = new Waveform() {
		@Override
		public double f(double t) {
			double modt = t % (2 * Math.PI);
			if (modt < 0) {
				modt += 2 * Math.PI;
			} 
			
			if (t <= Math.PI) {
				return 1;
			} else {
				return -1;
			}
		}
	};
	
	public static final Waveform SAWTOOTH = new Waveform() {
		@Override
		public double f(double t) {
			double modt = (t + Math.PI) % (2 * Math.PI);  // shift by pi by for easy computation
			if (modt < 0) {
				modt += 2 * Math.PI;
			}
			
			return (1 / Math.PI) * modt - 1;
		}
	};
	
	public double f(double t);
}
