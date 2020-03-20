import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.StringTokenizer;

public class Upload implements Runnable {

	ServerSocket server;
	Socket socket;
	InetSocketAddress peerAddress;
	BufferedReader dis;
	DataOutputStream dos;
	FileInputStream fis;
	File file;
	OutputStream os;
	int port;
	String filename;
	String fileInfo;
	int tradeNum;
	int remain;
	int bitmapLength;
	int[] bitmapIndex;
	int[] position;
	StringTokenizer st;
	long check_start;
	long check_end;
	int tmpPresentBitmap;
	int requestIndex;
	String peerInput;

	long totalReadBytes = 0;
	byte[] buffer;

	public Upload() {
		try {
			buffer = new byte[Peer.BUFFER_SIZE];
			bitmapIndex = new int[3];
			position = new int[3];
			port = Peer.myPort;
			server = new ServerSocket(port);
			fis = null;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void run() {
		try {
			mostOut: while (true) {
				socket = server.accept();
				socket.setSoTimeout(20000);
				peerAddress = (InetSocketAddress) socket.getRemoteSocketAddress();

				dis = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				dos = new DataOutputStream(socket.getOutputStream());
				os = socket.getOutputStream();

				try {
					peerInput = dis.readLine();

					filename = ".\\Files\\" + peerInput;

					file = new File(filename);

					if (Peer.fileSize == 0 || file.length() == 0) {
						dos.writeBytes("-1\n");
						dos.close();
						dis.close();
						socket.close();
						os.close();
						continue mostOut;
					}
					dos.writeBytes("1\n");

				} catch (Exception e) {
					dis.close();
					dos.close();
					os.close();
					socket.close();
					continue mostOut;
				}
				
				fis = new FileInputStream(filename);

				while (true) {
					boolean first = true;
					while (true) {
						try {
							peerInput = dis.readLine();
							if (peerInput.equals("fileInfo")) {
								String fileInfo = Peer.bitmapLength + " " + Peer.finalBitmap[Peer.bitmapLength - 1]
										+ " " + Peer.fileSize + "\n";
								dos.writeBytes(fileInfo);

							} else if (peerInput.equals("end")) {
								break;
							} else if (peerInput.equals("wait")) {
								check_start = System.currentTimeMillis();
								check_end = check_start;
								while (check_end - check_start < 5000) {
									if (first) {
										first = false;
										dos.writeBytes(Peer.presentBitmap[requestIndex] + "\n");
										break;
									} else if (Peer.presentBitmap[requestIndex] != tmpPresentBitmap) {
										dos.writeBytes(Peer.presentBitmap[requestIndex] + "\n");
										break;
									}
									check_end = System.currentTimeMillis();
								}
								if (check_end - check_start >= 5000) {
									dos.writeBytes(Peer.presentBitmap[requestIndex] + "\n");
								}
							} else if (peerInput.equals("disconnect")) {
								dis.close();
								dos.close();
								os.close();
								socket.close();
								continue mostOut;
							} else {
								st = new StringTokenizer(peerInput);
								st.nextToken();
								requestIndex = Integer.parseInt(st.nextToken());
								tmpPresentBitmap = Peer.presentBitmap[requestIndex];
								dos.writeBytes(tmpPresentBitmap + "\n");
							}
						} catch (Exception e) {
							dis.close();
							dos.close();
							os.close();
							socket.close();
							continue mostOut;
						}
					}

					for (int i = 0; i < 3; i++) {
						bitmapIndex[i] = -1;
						position[i] = -1;
					}

					for (int i = 0; i < 3; i++) {
						peerInput = dis.readLine();
						st = new StringTokenizer(peerInput);
						bitmapIndex[i] = Integer.parseInt(st.nextToken());
						position[i] = Integer.parseInt(st.nextToken());
						if (bitmapIndex[i] != -1) {
							fis.getChannel().position(Peer.BUFFER_SIZE * (32 * (long) bitmapIndex[i] + position[i]));
							int readBytes = fis.read(buffer);
							os.write(buffer, 0, readBytes);
						} else {
							break;
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				dos.close();
				dis.close();
				os.close();
				fis.close();
			} catch (Exception e) {
				e.printStackTrace();
			}

		}
	}
}
