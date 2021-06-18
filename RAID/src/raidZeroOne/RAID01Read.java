package raidZeroOne;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;

/**
 * 模拟RAID01读
 * 
 * @author zhz
 */
public class RAID01Read implements Runnable {

	private ArrayList<String> r0DiskPath;// raid0磁盘路径（数据分为几份）
	private ArrayList<String> r1DiskPath;// raid1磁盘路径（复制了几份）
	private String desFilePath;// 目标文件路径
	private String desFileType;// 目标文件类型（后缀）
	private String regex;// 用于查找源文件的正则表达式
	private int loc;// 当前线程的文件索引
	private int timesOfRaid1T0;// r1磁盘个数 / r0磁盘个鼠标

	public RAID01Read(ArrayList<String> r0DiskPath, ArrayList<String> r1DiskPath, String desFilePath) throws IOException {
		this.r0DiskPath = r0DiskPath;
		this.r1DiskPath = r1DiskPath;
		this.desFilePath = desFilePath;
		this.loc = 0;
		initialize();// 初始化
	}

	/**
	 * 初始化
	 * 
	 * @throws IOException
	 */
	public void initialize() throws IOException {
		File desFile = new File(desFilePath);// 目标文件
		if (desFile.exists()) {// 文件已存在则删除且新建
			desFile.delete();
		}
		desFile.createNewFile();
		String desFileName = desFile.getName();// 目标文件名
		int pointIndex = desFileName.lastIndexOf(".");
		if (pointIndex == -1) {// 源文件没有后缀
			desFileType = "";// 源文件后缀
		} else {
			desFileType = desFileName.substring(desFileName.lastIndexOf("."));
			desFileName = desFileName.substring(0, desFileName.lastIndexOf("."));
		}
		regex = desFileName + "-" + "\\d+" + desFileType;// 匹配文件表达式
		int r0dpNum = r0DiskPath.size();
		int r1dpNum = r1DiskPath.size();
		if (r1DiskPath.size() % r0DiskPath.size() != 0) {
			System.out.println("raid1磁盘数必须为raid0的正整数倍");
			return;
		}
		timesOfRaid1T0 = r1dpNum / r0dpNum;
	}

	@Override
	public void run() {
		int loc;
		synchronized ("") {// 线程同步块，避免读写错乱
			loc = this.loc++;
		}
		File srcFolder = new File(r0DiskPath.get(loc));
		if (!srcFolder.exists()) {// 磁盘损坏
			System.out.println("存储数据块" + loc + "的主数据盘已经损坏，正在从备份数据盘读取数据...");
			int flag = 0;
			for (int i = 0; i < timesOfRaid1T0; i++) {// 备份数据盘读取
				srcFolder = new File(r1DiskPath.get(i * timesOfRaid1T0 + loc));
				if (srcFolder.exists()) {
					break;
				} else {
					flag++;
				}
			}
			if (flag == timesOfRaid1T0) {
				System.out.println("存储数据块" + loc + "的磁盘均已损坏，该数据块读取失败");
				return;
			}
		}

		String[] srcFolderFilesName = srcFolder.list();// 列出该目录下的所有文件
		String srcFileName = null;
		for (int i = 0; i < srcFolderFilesName.length; i++) {// 找到指定源文件
			if (srcFolderFilesName[i].matches(regex)) {
				srcFileName = srcFolderFilesName[i];
				break;
			}
		}
		int srcPos;
		if (desFileType == "") {// 目标文件没有后缀
			srcPos = Integer.parseInt(srcFileName.substring(srcFileName.lastIndexOf("-") + 1));// 源文件在目标文件中的位置
		} else {
			srcPos = Integer
					.parseInt(srcFileName.substring(srcFileName.lastIndexOf("-") + 1, srcFileName.lastIndexOf(".")));
		}

		RandomAccessFile rafSrc = null;
		RandomAccessFile rafDes = null;
		try {
			rafSrc = new RandomAccessFile(srcFolder.getPath() + "/" + srcFileName, "r");
			rafDes = new RandomAccessFile(desFilePath, "rw");
			rafDes.seek(srcPos);// 设置文件指针位置
			byte[] buffer = new byte[1024];
			int len;
			while ((len = rafSrc.read(buffer)) != -1) {
				rafDes.write(buffer, 0, len);
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
