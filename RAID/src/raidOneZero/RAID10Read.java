package raidOneZero;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;

public class RAID10Read implements Runnable {

	private ArrayList<ArrayList<String>> dataDiskPath;// 磁盘路径，一维数表示raid0的盘数（数据分为几份），二维数表示raid1的盘数（复制几份）
	private String desFilePath;// 目标文件路径
	private String desFileType;// 目标文件类型（后缀）
	private String regex;// 用于查找源文件的正则表达式
	private int loc;// 当前线程的文件索引

	public RAID10Read(ArrayList<ArrayList<String>> dataDiskPath, String desFilePath) throws IOException {
		this.dataDiskPath = dataDiskPath;
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
	}

	@Override
	public void run() {
		int loc;
		synchronized ("") {// 线程同步块，避免读写错乱
			loc = this.loc++;
		}
		File srcFolder = new File(dataDiskPath.get(loc).get(0));
		if (!srcFolder.exists()) {// 磁盘损坏
			System.out.println("存储数据块" + loc + "的主数据盘已经损坏，正在从备份数据盘读取数据...");
			int flag = 1;
			for (int i = 1; i < dataDiskPath.get(loc).size(); i++) {// 备份数据盘读取
				srcFolder = new File(dataDiskPath.get(loc).get(i));
				if (srcFolder.exists()) {
					break;
				} else {
					flag++;
				}
			}
			if (flag == dataDiskPath.get(loc).size()) {
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
