package raidZeroOne;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;

/**
 * 模拟RAID01写
 * 
 * @author zhz
 */
public class RAID01Write implements Runnable {

	private ArrayList<String> r0DiskPath;// raid0磁盘路径（数据分为几份）
	private ArrayList<String> r1DiskPath;// raid1磁盘路径（复制了几份）
	private String sfp;// srcFilePath，源文件路径
	private String sfn;// srcFileName，源文件名
	private String sft;// srcFileType，源文件类型（后缀）
	private int loc;// 当前线程的文件索引
	private long dflel;// desFileLengthExceptLast，除最后一个文件条带的大小
	private long dfll;// desFileLengthLast，最后一个文件条带的大小
	private int timesOfRaid1T0;// r1磁盘个数 / r0磁盘个鼠标

	public RAID01Write(ArrayList<String> r0DiskPath, ArrayList<String> r1DiskPath, String sfp) throws IOException {
		this.r0DiskPath = r0DiskPath;
		this.r1DiskPath = r1DiskPath;
		this.sfp = sfp;
		this.loc = 0;
		initialize();// 初始化
	}

	/**
	 * 初始化
	 * 
	 * @throws IOException
	 */
	public void initialize() throws IOException {
		int r0dpNum = r0DiskPath.size();
		int r1dpNum = r1DiskPath.size();
		if (r1dpNum % r0dpNum != 0) {
			System.out.println("raid1磁盘数必须为raid0的正整数倍");
			return;
		}
		timesOfRaid1T0 = r1dpNum / r0dpNum;
		int sfpLength = (int) new File(sfp).length();// 计算每个文件条带大小
		dflel = sfpLength / r0dpNum;
		dfll = dflel + sfpLength % r0dpNum;

		int pointIndex = sfp.lastIndexOf(".");
		sfn = new File(sfp).getName();// 源文件名
		if (pointIndex == -1) {// 源文件没有后缀
			sft = "";// 源文件后缀
		} else {
			sfn = sfn.substring(sfn.lastIndexOf(".") + 1);
			sft = sfp.substring(pointIndex);
		}
	}

	@Override
	public void run() {
		int loc;
		synchronized ("") {// 线程同步块，避免读写错乱
			loc = this.loc++;
		}
		long fileLength;// 分割后的文件长度
		if (loc != r0DiskPath.size() - 1) {
			fileLength = dflel;
		} else {
			fileLength = dfll;
		}

		String desFilePath = r0DiskPath.get(loc) + sfn + "-" + loc * dflel + sft;// 目标文件路径
		File desFile = new File(desFilePath);// 目标文件
		if (desFile.exists()) {// 存在则删除
			desFile.delete();
		}
		RandomAccessFile rafSrc = null;
		RandomAccessFile rafDes = null;

		try {
			desFile.createNewFile();// 创建文件
			rafSrc = new RandomAccessFile(sfp, "r");// 随机读方式打开
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

			String desFileName = desFile.getName();// raid1：文件复制备份
			for (int i = 0; i < timesOfRaid1T0; i++) {
				Files.copy(desFile.toPath(), new File(r1DiskPath.get(i + timesOfRaid1T0 * loc) + desFileName).toPath(),
						StandardCopyOption.REPLACE_EXISTING);
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
