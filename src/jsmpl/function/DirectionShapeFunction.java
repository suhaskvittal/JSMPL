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
 * An interface for functions that represent the shape of a 
 * musical direction's transformation
 * */
@FunctionalInterface
public interface DirectionShapeFunction {
	public static final DirectionShapeFunction linear = 
			new DirectionShapeFunction() 
	{
		@Override
		public double f(double t) {
			return t;
		}
	};
	
	public double f(double t);
}
