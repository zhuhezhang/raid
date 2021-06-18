package raidThree;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;

/**
 * 模拟RAID3读
 * 
 * @author zhz
 */
public class RAID3Read implements Runnable {

	private ArrayList<String> diskPath;// raid3磁盘路径
	private String desFilePath;// 目标文件路径
	private String regex;// 用于查找源文件的正则表达式
	private int loc;// 当前线程的文件索引

	public RAID3Read(ArrayList<String> diskPath, String desFilePath) throws IOException {
		this.diskPath = diskPath;
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
		String desFileType;
		if (pointIndex == -1) {// 源文件没有后缀
			desFileType = "";// 源文件后缀
		} else {
			desFileType = desFileName.substring(desFileName.lastIndexOf("."));
			desFileName = desFileName.substring(0, desFileName.lastIndexOf("."));
		}
		regex = desFileName + "-\\d+" + desFileType;// 匹配文件表达式
	}

	@Override
	public void run() {
		int loc;
		synchronized ("") {// 线程同步块，避免读写错乱
			loc = this.loc++;
		}
		File srcFolder = new File(diskPath.get(loc));
		String[] srcFolderFilesName = srcFolder.list();// 列出该目录下的所有文件
		String srcFileName = null;
		for (int i = 0; i < srcFolderFilesName.length; i++) {// 找到指定源文件
			if (srcFolderFilesName[i].matches(regex)) {
				srcFileName = srcFolderFilesName[i];
				break;
			}
		}
		if (srcFileName == null) {
			System.out.println("文件" + desFilePath + "不存在");
			return;
		}
		int srcPos;// 源文件在目标文件中的位置
		if (srcFileName.lastIndexOf(".") == -1) {
			srcPos = Integer.parseInt(srcFileName.substring(srcFileName.lastIndexOf("-") + 1));
		} else {
			srcPos = Integer
					.parseInt(srcFileName.substring(srcFileName.lastIndexOf("-") + 1, srcFileName.lastIndexOf(".")));
		}

		RandomAccessFile rafSrc = null;
		RandomAccessFile rafDes = null;
		try {
			rafSrc = new RandomAccessFile(diskPath.get(loc) + srcFileName, "r");
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
