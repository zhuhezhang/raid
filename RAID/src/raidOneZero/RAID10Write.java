package raidOneZero;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;

/**
 * 模拟RAID10写
 * 
 * @author zhz
 */
public class RAID10Write implements Runnable {

	private ArrayList<ArrayList<String>> dataDiskPath;// 磁盘路径，一维数表示raid0的盘数（数据分为几份），二维数表示raid1的盘数（复制几份）
	private String sfp;// srcFilePath，源文件路径
	private String sfn;// srcFileName，源文件名
	private String sft;// srcFileType，源文件类型（后缀）
	private int loc;// 当前线程的文件索引
	private long dflel;// desFileLengthExceptLast，除最后一个文件条带的大小
	private long dfll;// desFileLengthLast，最后一个文件条带的大小

	public RAID10Write(ArrayList<ArrayList<String>> dataDiskPath, String sfp) throws IOException {
		this.dataDiskPath = dataDiskPath;
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
		int r0dfpNum = dataDiskPath.size();
		long sfpLength = new File(sfp).length();// 计算每个文件条带大小
		dflel = sfpLength / r0dfpNum;
		dfll = dflel + sfpLength % r0dfpNum;

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
		if (loc != dataDiskPath.size() - 1) {
			fileLength = dflel;
		} else {
			fileLength = dfll;
		}

		String desFilePath = dataDiskPath.get(loc).get(0) + sfn + "-" + loc * dflel + sft;// 目标文件路径
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
			for (int i = 0; i < dataDiskPath.get(loc).size(); i++) {
				Files.copy(desFile.toPath(), new File(dataDiskPath.get(loc).get(i) + desFileName).toPath(),
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
