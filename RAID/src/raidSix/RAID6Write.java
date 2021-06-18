package raidSix;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;

/**
 * 模拟RAID6写
 * 
 * @author zhz
 */
public class RAID6Write implements Runnable {

	private ArrayList<String> dataDisk;// 数据磁盘路径
	private ArrayList<String> parityCkechDisk;// 奇偶校验盘路径
	private String srcFilePath;// 源文件路径
	private String srcFileName;// 源文件名
	private String srcFileType;// 源文件类型（后缀）
	private int loc;// 当前线程的文件索引
	private long dflel;// desFileLengthExceptLast，除最后一个文件条带的大小
	private long dfll;// desFileLengthLast，最后一个文件条带的大小
	private byte[][] fileBlockByte;// 文件块的字节形式，一维表示哪一块，二维表示该块的数据大小

	public RAID6Write(ArrayList<String> dataDisk, ArrayList<String> parityCheckDisk, String srcFilePath)
			throws IOException {
		this.dataDisk = dataDisk;
		this.parityCkechDisk = parityCheckDisk;
		this.srcFilePath = srcFilePath;
		this.loc = 0;
		initialize();// 初始化
	}

	/**
	 * 初始化
	 * 
	 * @throws IOException
	 */
	public void initialize() throws IOException {
		int dataDiskNum = dataDisk.size();
		long srcFileLength = new File(srcFilePath).length();// 计算每个文件条带大小
		dflel = srcFileLength / dataDiskNum;
		dfll = dflel + srcFileLength % dataDiskNum;
		fileBlockByte = new byte[dataDiskNum][(int) dfll];

		int pointIndex = srcFilePath.lastIndexOf(".");
		srcFileName = new File(srcFilePath).getName();// 源文件名
		if (pointIndex == -1) {// 源文件没有后缀
			srcFileType = "";// 源文件后缀
		} else {
			srcFileName = srcFileName.substring(0, srcFileName.lastIndexOf("."));
			srcFileType = srcFilePath.substring(pointIndex);
		}
	}

	/**
	 * 文件分块写入磁盘
	 */
	@Override
	public void run() {
		int loc;
		synchronized ("") {// 线程同步块，避免读写错乱
			loc = this.loc++;
		}

		long fileLength;// 分割后的文件长度
		long startPos = loc * dflel;
		if (loc != dataDisk.size() - 1) {
			fileLength = dflel;
		} else {
			fileLength = dfll;
		}
		File desFile = new File(dataDisk.get(loc) + srcFileName + "-" + startPos + srcFileType);// 目标文件路径
		if (desFile.exists()) {// 存在则删除
			desFile.delete();
		}
		RandomAccessFile rafSrc = null;
		RandomAccessFile rafDes = null;

		try {
			desFile.createNewFile();// 创建文件
			rafSrc = new RandomAccessFile(srcFilePath, "r");// 随机读方式打开
			rafDes = new RandomAccessFile(desFile, "rw");// 随机写方式打开
			rafSrc.seek(loc * dflel);// 设置读文件指针位置
			int bufferLen = 1024;
			byte[] buffer = new byte[bufferLen];

			if (fileLength <= bufferLen) {// 一次读出来的字节数大于该文件条带的大小
				rafSrc.read(buffer);
				rafDes.write(buffer, 0, (int) fileLength);
				for (int i = 0; i < dfll; i++) {// 将读取到的每组数据保存起来，用于奇偶校验码文件的生成
					fileBlockByte[loc][i] = buffer[i];
				}
			} else {
				while (true) {
					rafDes.write(buffer, 0, rafSrc.read(buffer));
					for (int i = 0; i < bufferLen; i++) {
						fileBlockByte[loc][i] = buffer[i];
					}
					fileLength -= bufferLen;
					if (fileLength >= bufferLen) {// 剩余未读长度大于缓冲区长度
						continue;
					} else {
						rafSrc.read(buffer);
						rafDes.write(buffer, 0, (int) fileLength);
						for (int i = 0; i < fileLength; i++) {
							fileBlockByte[loc][i] = buffer[i];
						}
						break;
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				rafDes.close();// 关闭流
				rafSrc.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 将奇偶校验码的数据写进奇偶校验盘
	 * 
	 * @throws IOException
	 */
	public void writeToParityCheckDisk() throws IOException {
		byte[] parityCheck = new byte[(int) dfll];
		for (int i = 0; i < dflel; i++) {
			for (int j = 0; j < fileBlockByte.length; j++) {
				parityCheck[i] ^= fileBlockByte[j][i];
			}
		}
		for (int i = (int) dflel; i < dfll; i++) {// 最后一组多余的字节直接补充
			parityCheck[i] = fileBlockByte[fileBlockByte.length - 1][i];
		}

		for (int i = 0; i < 2; i++) {
			String toDataDiskPath = parityCkechDisk.get(i);// 将要变成数据盘的奇偶校验盘路径
			RandomAccessFile ras = new RandomAccessFile(toDataDiskPath + srcFileName + "-pcd" + srcFileType, "rw");// 随机写方式打开
			ras.write(parityCheck);
			ras.close();
			parityCkechDisk.set(0, dataDisk.get(0));
			dataDisk.remove(0);
			dataDisk.add(toDataDiskPath);
		}
	}

}
