package raidZero;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;

/**
 * 模拟RAID0数据写
 * 
 * @author zhz
 */
public class RAID0Write implements Runnable {

	private ArrayList<String> diskPath;// raid0磁盘路径
	private String srcFilePath;// 源文件路径
	private String srcFileName;// 源文件名
	private String srcFileType;// 源文件类型（后缀）
	private int loc;// 当前线程的文件索引
	private long dflel;// desFileLengthExceptLast，除最后一个文件条带的大小
	private long dfll;// desFileLengthLast，最后一个文件条带的大小

	public RAID0Write(ArrayList<String> diskPath, String srcFilePath) throws IOException {
		this.diskPath = diskPath;
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
		int desFoldersNum = diskPath.size();
		long srcFileLength = new File(srcFilePath).length();// 计算每个文件条带大小
		dflel = srcFileLength / desFoldersNum;
		dfll = dflel + srcFileLength % desFoldersNum;

		int pointIndex = srcFilePath.lastIndexOf(".");
		srcFileName = new File(srcFilePath).getName();// 源文件名
		if (pointIndex == -1) {// 源文件没有后缀
			srcFileType = "";// 源文件后缀
		} else {
			srcFileName = srcFileName.substring(srcFileName.lastIndexOf(".") + 1);
			srcFileType = srcFilePath.substring(pointIndex);
		}
	}

	@Override
	public void run() {
		int loc;
		synchronized ("") {// 线程同步块，避免读写错乱
			loc = this.loc++;
		}
		long fileLength;// 分割后的文件长度
		if (loc != diskPath.size() - 1) {
			fileLength = dflel;
		} else {
			fileLength = dfll;
		}
		
		File desFile = new File(diskPath.get(loc) + srcFileName + "-" + loc * dflel + srcFileType);// 目标文件路径
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
			} else {
				while (true) {
					rafDes.write(buffer, 0, rafSrc.read(buffer));
					fileLength -= bufferLen;
					if (fileLength >= bufferLen) {// 剩余未读长度大于缓冲区长度
						continue;
					} else {
						rafSrc.read(buffer);
						rafDes.write(buffer, 0, (int) fileLength);
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

}