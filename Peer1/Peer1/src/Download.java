import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Random;
import java.util.StringTokenizer;

public class Download implements Runnable {

	int[] bitmapIndex;
	int[] position;
	int port;
	String peerIp;
	int beforePort;
	String beforePeerIp;
	FileOutputStream fos;
	DataOutputStream dos;
	BufferedReader dis;
	InputStream is;
	Socket socket;
	int beforeConfigIndex;
	StringTokenizer st;
	int peerBitmap;
	int canGetBitmap;
	byte[] buffer;
	int tmpBitmapIndex;
	int tmpPosition;
	int tmpBit;
	int where;
	int uniqueIndex;
	boolean uniqueFinished;
	static boolean getFileInfo = false;
	long check_start;
	long check_end;

	static int tmp = 0;

	public Download(int uniqueIndex) {
		try {
			this.uniqueIndex = uniqueIndex;
			this.uniqueFinished = false;
			buffer = new byte[Peer.BUFFER_SIZE];
			bitmapIndex = new int[] { -1, -1, -1 };
			position = new int[] { -1, -1, -1 };
			fos = new FileOutputStream(".\\Files\\" + Peer.filename);
			beforePort = -1;
			beforePeerIp = "";
			beforeConfigIndex = -1;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void updateFileInfo() throws Exception {
		Download.getFileInfo = true;
		dos.writeBytes("fileInfo\n");
		String info = dis.readLine();
		st = new StringTokenizer(info);
		int bitmapLength = Integer.parseInt(st.nextToken());
		int remain = Integer.parseInt(st.nextToken());
		long fileSize = Long.parseLong(st.nextToken());
		Peer.updateFileInfo(bitmapLength, remain, fileSize);

	}

	private synchronized void occupiedPeer() {

		int index = 0;
		do {
			index = new Random().nextInt(Peer.port.length);
		} while ((Peer.occupied[index] == true));
		if (this.beforeConfigIndex != -1) {
			Peer.occupied[this.beforeConfigIndex] = false;
		}
		Peer.occupied[index] = true;
		beforeConfigIndex = index;
		peerIp = Peer.peerIp[index];
		port = Peer.port[index];
	}

	private void fileDownload() throws Exception {

		while (true) {

			for (int i = 0; i < 3; i++) {
				bitmapIndex[i] = -1;
				position[i] = -1;
			}

			where = 0;
			int tmpBitmapIndex;

			check_start = System.currentTimeMillis();
			if (!uniqueFinished) {
				tmpBitmapIndex = uniqueIndex;
				while (tmpBitmapIndex < Peer.bitmapLength && where < 3) {
					dos.writeBytes("bitmap " + tmpBitmapIndex + "\n");
					peerBitmap = Integer.parseInt(dis.readLine());
					canGetBitmap = peerBitmap & (~Peer.willBeBitmap[tmpBitmapIndex]);

					check_end = check_start;
					if (canGetBitmap == 0) {
						if (where > 0) {
							break;
						} else if (Peer.willBeBitmap[tmpBitmapIndex] == Peer.finalBitmap[tmpBitmapIndex]) {
							tmpBitmapIndex += 3;
							continue;
						}
						dos.writeBytes("wait\n");
						check_end = System.currentTimeMillis();
						while (check_end - check_start < 10000) {
							peerBitmap = Integer.parseInt(dis.readLine());
							canGetBitmap = peerBitmap & (~Peer.willBeBitmap[tmpBitmapIndex]);
							if (canGetBitmap == 0) {
								dos.writeBytes("wait\n");

							} else {
								check_start = System.currentTimeMillis();
								break;
							}
							check_end = System.currentTimeMillis();
						}
						if (check_end - check_start >= 10000) {
							dos.writeBytes("disconnect\n");

							return;
						}
					}

					tmpPosition = 0;
					tmpBit = 1;
					while (canGetBitmap != 0 && where < 3) {
						if ((tmpBit & canGetBitmap) != 0) {
							bitmapIndex[where] = tmpBitmapIndex;
							position[where++] = tmpPosition++;
							Peer.willBeBitmap[tmpBitmapIndex] |= tmpBit;
							canGetBitmap ^= tmpBit;
							tmpBit <<= 1;
						} else {
							tmpPosition++;
							tmpBit <<= 1;
						}
					}
					tmpBitmapIndex += 3;
				}
			}

			for (int j = uniqueIndex; j < Peer.bitmapLength; j += 3) {
				if (Peer.presentBitmap[j] == Peer.finalBitmap[j]) {
					uniqueIndex += 3;
				}
				if (uniqueIndex >= Peer.bitmapLength) {
					uniqueFinished = true;
				}
			}

			check_start = System.currentTimeMillis();
			if (uniqueFinished || bitmapIndex[0] == -1) {
				tmpBitmapIndex = Peer.bottom;
				synchronized (Peer.class) {
					while (tmpBitmapIndex < Peer.bitmapLength && where < 3) {
						dos.writeBytes("bitmap " + tmpBitmapIndex + "\n");
						peerBitmap = Integer.parseInt(dis.readLine());
						canGetBitmap = peerBitmap & (~Peer.willBeBitmap[tmpBitmapIndex]);

						check_end = check_start;
						if (canGetBitmap == 0) {
							if (where > 0) {
								break;
							} else if (Peer.willBeBitmap[tmpBitmapIndex] == Peer.finalBitmap[tmpBitmapIndex]) {
								tmpBitmapIndex += 3;
								continue;
							}
							dos.writeBytes("wait\n");

							check_end = System.currentTimeMillis();
							while (check_end - check_start < 10500) {
								peerBitmap = Integer.parseInt(dis.readLine());
								canGetBitmap = peerBitmap & (~Peer.willBeBitmap[tmpBitmapIndex]);
								if (canGetBitmap == 0) {
									dos.writeBytes("wait\n");

								} else {
									check_start = System.currentTimeMillis();
									break;
								}
								check_end = System.currentTimeMillis();
							}
							if (check_end - check_start >= 10500) {
								dos.writeBytes("disconnect\n");
								return;
							}
						}

						tmpPosition = 0;
						tmpBit = 1;
						while (canGetBitmap != 0 && where < 3) {
							if ((tmpBit & canGetBitmap) != 0) {
								bitmapIndex[where] = tmpBitmapIndex;
								position[where++] = tmpPosition++;
								Peer.willBeBitmap[tmpBitmapIndex] |= tmpBit;
								canGetBitmap ^= tmpBit;
								tmpBit <<= 1;
							} else {
								tmpPosition++;
								tmpBit <<= 1;
							}
						}
						tmpBitmapIndex++;
					}
				}
			}

			dos.writeBytes("end\n");

			int tmp = 0;
			for (int i = 0; i < 3; i++) {
				dos.writeBytes(bitmapIndex[i] + " " + position[i] + "\n");
				if (bitmapIndex[i] != -1) {
					tmp++;
				} else {
					break;
				}
			}

			for (int i = 0; i < tmp; i++) {
				int readBytes = is.read(buffer);
				fos.getChannel().position(Peer.BUFFER_SIZE * (32 * (long) bitmapIndex[i] + position[i]));
				fos.write(buffer, 0, readBytes);
				fos.flush();
				synchronized (Peer.class) {
					Peer.totalReadBytes += readBytes;
					Peer.presentBitmap[bitmapIndex[i]] |= (1 << position[i]);
				}
			}
			if ((Peer.fileSize != 0) && (Peer.totalReadBytes == Peer.fileSize)) {
				dos.writeBytes("disconnect\n");
				return;
			}
		}

	}

	public void run() {
		try {
			while (true) {

				do {
					try {
						occupiedPeer();
						socket = new Socket();

						SocketAddress address = new InetSocketAddress(peerIp, port);

						socket.connect(address, 10000);
					} catch (Exception e) {
						System.out.println("another exception");
					}
					if ((Peer.fileSize != 0) && (Peer.totalReadBytes == Peer.fileSize)) {
						return;
					}
				} while (!socket.isConnected());

				socket.setSoTimeout(11000);
				dos = new DataOutputStream(socket.getOutputStream());
				dis = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				is = socket.getInputStream();

				try {
					dos.writeBytes(Peer.filename + "\n");
					if ((dis.readLine()).equals("-1")) {
						dos.close();
						dis.close();
						is.close();
						socket.close();
						continue;
					}
				} catch (Exception e) {
					dos.close();
					dis.close();
					is.close();
					socket.close();
					continue;
				}

				if (Peer.fileSize == 0) {
					synchronized (Peer.class) {
						if (Peer.fileSize == 0) {
							updateFileInfo();
						}
					}
				}

				// System.out.println("Peer1 " + Thread.currentThread().getName() + " connects Peer" + port % 10);
				try {
					fileDownload();
				} catch (Exception e) {
					System.out.println("Error!");
					e.printStackTrace();
					dos.close();
					dis.close();
					is.close();
					socket.close();
				}
				dos.close();
				dis.close();
				is.close();
				socket.close();

				// System.out.println("Peer1 " + Thread.currentThread().getName() + " disconnects Peer" + port % 10);
				if ((Peer.fileSize != 0) && (Peer.totalReadBytes == Peer.fileSize)) {
					return;
				}
			}
		} catch (BindException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				fos.close();
			} catch (Exception e) {

			}
		}

	}
}
