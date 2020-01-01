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

package jsmpl.io;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * This file implements an outputstream for .WAV files.
 */
public class WAVFileOutputStream extends FileOutputStream {
	private File file;
	private AudioFormat format;
	private int bitSize;
	
	/**
	 * @param file the file to write to
	 * @throws UnsupportedAudioFileException
	 * @throws IOException
	 */
	public WAVFileOutputStream(File file) throws IOException {
		this(file, 1);
	}
	
	/**
	 * @param file the file to write to 
	 * @param channels the number of channels in the wav file
	 * @throws FileNotFoundException 
	 * @throws UnsupportedAudioFileException
	 * @throws IOException
	 */
	public WAVFileOutputStream(File file, int channels) throws FileNotFoundException {
		this(file, channels, 32);
	}
	
	/**
	 * @param file the file to write to
	 * @param channels the number of channels in the wav file
	 * @param bitSize the bit size of the wav file. Note that for WAV files,
	 * 		8 bit (byte quality) to 32 bit (integer quality) are considered standard.
	 * @throws FileNotFoundException
	 */
	public WAVFileOutputStream(File file, int channels, int bitSize) throws FileNotFoundException {
		this(file, channels, bitSize, 44100.0F);
	}
	
	public WAVFileOutputStream(File file, int channels, int bitSize, double samplingRate) throws FileNotFoundException {
		super(file);
		this.file = file;
		this.format = new AudioFormat((float) samplingRate, bitSize, channels, true, false);
		this.bitSize = bitSize;
	}
	
	/**
	 * @param name the path of the file to write to
	 * @throws UnsupportedAudioFileException
	 * @throws IOException
	 */
	public WAVFileOutputStream(String name) throws IOException {
		this(name, 1);
	}
	
	/**
	 * @param name the path of the file to write to
	 * @param channels the number of channels in the wav file
	 * @throws UnsupportedAudioFileException
	 * @throws IOException
	 */
	public WAVFileOutputStream(String name, int channels) throws IOException {
		this(new File(name), channels);
	}
	
	public WAVFileOutputStream(String name, int channels, int bitSize) throws FileNotFoundException {
		this(new File(name), channels, bitSize);
	}
	
	public WAVFileOutputStream(String name, int channels, int bitSize, double samplingRate) throws FileNotFoundException {
		this(new File(name), channels, bitSize, samplingRate);
	}
	
	public void write(int b) throws IOException {
		write(new Integer[] { b });
	}
	
	public void write(byte[] b) throws IOException {
		if (b.length == 0) {
			throw new IllegalArgumentException("The byte array is empty.");
		}
		
		AudioInputStream istream = new AudioInputStream(new ByteArrayInputStream(b), format, b.length);
		AudioSystem.write(istream, Type.WAVE, file);
	}
	
	public <T extends Number> void write(T[] b) throws IOException {
		write(convertToByteArray(b));
	}
	
	public void write(byte[] b, int off, int len) throws IOException {
		if (off < 0 || off > b.length || len > b.length) {
			throw new IllegalArgumentException("The offset or length is invalid.");
		}
		
		if (b.length == 0) {
			throw new IllegalArgumentException("The byte array is empty.");
		}
		
		byte[] samples = new byte[len];
		
		for (int i = off; i < off + len; i++) {
			samples[i - off] = b[i];
		}
		
		write(samples);
	}
	
	public <T extends Number> void write(T[] b, int off, int len) throws IOException {
		write(convertToByteArray(b), off * bitSize / 8, len * bitSize / 8);
	}
	
	public void write(byte[][] b) throws IOException {
		if (b.length == 0 || b[0].length == 0) {
			throw new IllegalArgumentException("The byte array is empty.");
		}
		
		if (b[0].length != format.getChannels()) {
			throw new IllegalArgumentException("The byte array has more channels than defined for this object.");
		}
		
		byte[] samples = new byte[b.length * b[0].length];
		
		for (int i = 0; i < b.length; i++) {  // flatten the input array
			for (int j = 0; j < format.getChannels(); j++) {
				samples[i * format.getChannels() + j] = b[i][j];
			}
		}
		
		write(samples);
	}
	
	public <T extends Number> void write(T[][] b) throws IOException {
		write(flatten(b));
	}
	
	public <T extends Number> void write(T[][] b, int off, int len) throws IOException {
		write(flatten(b), off * b[0].length, len * b[0].length);  // note these are adjusted for flattened parameters
	}
	
	private <T extends Number> byte[] convertToByteArray(T[] arr) {
		int endianCount = bitSize / 8;  // bitSize / 8 values required to convert to little endian.
		byte[] b = new byte[arr.length * endianCount];
		
		long smin = -(1 << (bitSize - 1));
		long smax = (1 << (bitSize - 1)) - 1;
		
		for (int i = 0; i < arr.length; i++) {
			long sample = (long) (smax * (arr[i] == null ? 0.0 : arr[i].doubleValue()));
			
			if (sample > smax) {
				sample = smax;
			} 
			
			if (sample < smin) {
				sample = smin;
			}
			
			for (int j = 0; j < endianCount; j++) {
				b[endianCount * i + j] = (byte) ((sample >> (8 * j)) & 255);
			}
		}
		
		return b;
	}
	
	@SuppressWarnings("unchecked")
	private <T extends Number> T[] flatten(T[][] arr) {
		T[] samples = (T[]) new Number[arr.length * arr[0].length];

		for (int i = 0; i < arr.length; i++) {  // flatten the input array
			for (int j = 0; j < arr[i].length; j++) {
				samples[i * arr[i].length + j] = arr[i][j];
			}
		}
		
		return samples;
	}
}
