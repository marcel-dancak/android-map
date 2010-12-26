package sk.gista.android.utils;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import sk.gista.android.maps.Layer.Tile;

public class NetworkDebugger {

	public enum Signal {
		PROGRESS_UPDATE(0),
		FINISHED(1),
		ABORTED(2),
		ERROR(3);
		
		public final int code;
		
		private Signal(int code) {
			this.code = code;
		}
	}
	
	public static String server = "10.0.2.2";
	public static int serverPort = 8090;
	
	public static void sendProgress(Tile tile, int progress, int bytes) {
		sendBinaryData(tile, Signal.PROGRESS_UPDATE, new int[] {progress, bytes});
	}
	
	public static void sendFinished(Tile tile) {
		sendBinaryData(tile, Signal.FINISHED, new int[0]);
	}
	
	public static void sendSignal(Tile tile, Signal signal) {
		sendBinaryData(tile, signal, new int[0]);
	}
	
	private static void sendBinaryData(Tile tile, Signal signal, int[] data) {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(bos);
		try {
			out.writeInt(signal.code);
			out.writeInt(tile.getX());
			out.writeInt(tile.getY());
			out.writeInt(tile.getZoomLevel());
			for (int i : data) {
				out.writeInt(i);
			}
			byte[] packetData = bos.toByteArray();
			sendPacket(packetData, packetData.length);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void sendBinaryData(Tile tile, int progress, int bytes) {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(bos);
		try {
			out.writeInt(0);
			out.writeInt(tile.getX());
			out.writeInt(tile.getY());
			out.writeInt(tile.getZoomLevel());
			out.writeInt(bytes);
			byte[] data = bos.toByteArray();
			sendPacket(data, data.length);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void sendPacket(byte[] data, int length) {
		DatagramSocket socket;
		try {
			socket = new DatagramSocket();
			DatagramPacket packet = new DatagramPacket(data, length, InetAddress.getByName(server), serverPort);
			socket.send(packet);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
