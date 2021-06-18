package raidOne;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;

/**
 * 模拟RAID1数据写
 * 
 * @author zhz
 */
public class RAID1Write implements Runnable {

	private ArrayList<String> dataDiskPath;// 磁盘路径
	private String srcFilePath;// 源文件路径
	private String srcFileName;// 源文件名(包括后缀)
	private int loc;// 当前线程的文件索引

	public RAID1Write(ArrayList<String> dataDiskPath, String srcFilePath) {
		this.dataDiskPath = dataDiskPath;
		this.srcFilePath = srcFilePath;
		this.loc = 0;
		srcFileName = new File(srcFilePath).getName();// 获取源文件名
	}

	@Override
	public void run() {
		int loc;
		synchronized ("") {// 线程同步块，避免读写错乱
			loc = this.loc++;
		}
		try {
			File desFile = new File(dataDiskPath.get(loc) + srcFileName);
			if (desFile.exists()) {
				desFile.delete();
			}
			Files.copy(new File(srcFilePath).toPath(), desFile.toPath());// 文件复制
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
