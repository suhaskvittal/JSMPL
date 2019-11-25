package jsmpl.io;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

public class WAVFileOutputStream extends FileOutputStream {
	private File file;
	private AudioFormat format;
	
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
	 * @param append whether or not to append to the file
	 * @throws UnsupportedAudioFileException
	 * @throws IOException
	 */
	public WAVFileOutputStream(File file, int channels) throws IOException {
		super(file);
		this.file = file;
		this.format = new AudioFormat(44100.0F, 8, channels, true, true);;
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
	 * @param append whether or not to append to the file
	 * @throws UnsupportedAudioFileException
	 * @throws IOException
	 */
	public WAVFileOutputStream(String name, int channels) throws IOException {
		this(new File(name), channels);
	}
	
	public void write(int b) throws IOException {
		write(new byte[] {(byte) b, (byte) b});
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
		write(convertToByteArray(b), off, len);
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
	
	private <T extends Number> byte[] convertToByteArray(T[] arr) {
		byte[] b = new byte[arr.length];
		
		for (int i = 0; i < arr.length; i++) {
			b[i] = (byte) (128 * arr[i].doubleValue());
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
