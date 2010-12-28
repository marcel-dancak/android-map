package sk.gista.android.utils;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import sk.gista.android.maps.Layer;
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
	public static boolean debuggingEnabled = true;
	
	public static void sendProgress(Tile tile, int progress, int bytes) {
		if (debuggingEnabled) {
			sendBinaryData(tile, Signal.PROGRESS_UPDATE, new int[] {progress, bytes});
		}
	}
	
	public static void sendFinished(Tile tile) {
		if (debuggingEnabled) {
			sendBinaryData(tile, Signal.FINISHED, new int[0]);
		}
	}
	
	public static void sendSignal(Tile tile, Signal signal) {
		if (debuggingEnabled) {
			sendBinaryData(tile, signal, new int[0]);
		}
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
			sendPacket(packetData);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				out.close();
			} catch (IOException e) {}
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
			sendPacket(data);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	/*
	private static void sendPacket(byte[] data) {
		DatagramSocket socket;
		try {
			socket = new DatagramSocket();
			DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName(server), serverPort);
			socket.send(packet);
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	*/
	
	private static void sendPacket(byte[] data) {
		sendingThread.addPacketToSend(data);
	}
	
	private static NetThread sendingThread = new NetThread();
	
	public static void startSendingThread() {
		sendingThread.start();
	}
	
	public static void stopSendingThread() {
		sendingThread.run = false;
	}
	
	static class NetThread extends Thread {
		
		private BlockingQueue<byte[]> queue = new LinkedBlockingQueue<byte[]>(1000);
		private boolean run = true;
		
		@Override
		public void run() {
			DatagramSocket socket;
			try {
				socket = new DatagramSocket();
				while(run) {
					try {
						byte[] packetData = queue.poll(1000, TimeUnit.MILLISECONDS);
						if (packetData != null) {
							DatagramPacket packet = new DatagramPacket(packetData, packetData.length, InetAddress.getByName(server), serverPort);
							socket.send(packet);
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		public void addPacketToSend(byte[] data) {
			queue.offer(data);
		}
	}
}
