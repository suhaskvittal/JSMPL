package jsmpl.function;

/**
 * This interface implements a waveform with a specified period.
 * The basic waveforms (sine, triangle, sawtooth, and square) implement
 * this interface.
 */
public interface PeriodicWaveform extends Waveform {
	public static final PeriodicWaveform SINE = new PeriodicWaveform() {
		/**
		 * 
		 */
		private static final long serialVersionUID = -1042376563684595649L;

		@Override
		public double f(double t) {
			return Math.sin(t);
		}

		@Override
		public double getPeriod() {
			return 2 * Math.PI;
		}
	};
	
	public static final PeriodicWaveform TRIANGLE = new PeriodicWaveform() {
		/**
		 * 
		 */
		private static final long serialVersionUID = -1420538486804710828L;

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

		@Override
		public double getPeriod() {
			return 2 * Math.PI;
		}
	};
	
	public static final PeriodicWaveform SQUARE = new PeriodicWaveform() {
		/**
		 * 
		 */
		private static final long serialVersionUID = 4675539134474565448L;

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
		
		@Override
		public double getPeriod() {
			return 2 * Math.PI;
		}
	};
	
	public static final PeriodicWaveform SAWTOOTH = new PeriodicWaveform() {
		/**
		 * 
		 */
		private static final long serialVersionUID = -6021071172863735772L;

		@Override
		public double f(double t) {
			double modt = (t + Math.PI) % (2 * Math.PI);  // shift by pi by for easy computation
			if (modt < 0) {
				modt += 2 * Math.PI;
			}
			
			return (1 / Math.PI) * modt - 1;
		}
		
		@Override
		public double getPeriod() {
			return 2 * Math.PI;
		}
	};
	
	public double getPeriod();
}
