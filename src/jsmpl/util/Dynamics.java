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

package jsmpl.util;

public enum Dynamics {
	PPP (computeAmplitude(16)),
	PP (computeAmplitude(32)),
	P (computeAmplitude(48)),
	MP (computeAmplitude(64)),
	MF (computeAmplitude(80)),
	F (computeAmplitude(96)),
	FF (computeAmplitude(112)),
	FFF (computeAmplitude(127));
	
	public final double value;
	
	private Dynamics(double v) {
		value = v;
	}
	
	public static double computeAmplitude(double midiKeyVelocity) {
		double db = 40.0 * Math.log10(midiKeyVelocity / 127.0);
		return Math.pow(10.0, db / 20.0);
	}
}
