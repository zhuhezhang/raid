package raidOne;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;

/**
 * 模拟RAID1数据读
 * 
 * @author zhz
 */
public class RAID1Read {

	private ArrayList<String> dataDiskPath;// 磁盘路径
	private String desFilePath;// 目标文件路径
	private String desFileName;// 目标文件名(包括后缀)

	public RAID1Read(ArrayList<String> dataDiskPath, String desFilePath) {
		this.dataDiskPath = dataDiskPath;
		this.desFilePath = desFilePath;
		desFileName = new File(desFilePath).getName();// 获取目标文件名
	}

	/**
	 * 下载入口函数
	 * 
	 * @throws IOException
	 */
	public void download() throws IOException {
		File srcFile = null;// 源文件对象
		File desFile = new File(desFilePath);// 目标文件对象
		File srcFolder = new File(dataDiskPath.get(0));// 默认第一个磁盘为数据盘，其他为备份盘

		if (srcFolder.exists()) {// 如果数据盘没有损坏，直接读取
			File[] srcFolderFiles = srcFolder.listFiles();
			for (File file : srcFolderFiles) {
				if (file.getName().equals(desFileName)) {
					Files.copy(file.toPath(), desFile.toPath(), StandardCopyOption.REPLACE_EXISTING);// 复制文件，最后一个参数表示文件存在替换
					srcFile = file;
					break;
				}
			}
		}

		if (srcFile == null) {// 如果数据盘损坏，则从备份盘读取
			System.out.println("数据盘已损坏，正在从备份硬盘中读取数据...");
			int srcFoldersPathLength = dataDiskPath.size();

			breakPoint: for (int i = 1; i < srcFoldersPathLength; i++) {// 备份盘逐盘读取
				srcFolder = new File(dataDiskPath.get(i));
				if (srcFolder.exists()) {
					for (File file : new File(dataDiskPath.get(i)).listFiles()) {
						if (file.getName().matches(desFileName)) {// 找到要读取的文件
							Files.copy(file.toPath(), desFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
							srcFile = desFile;
							System.out.println("数据读取完成");
							break breakPoint;
						}
					}
				}
			}

			if (srcFile == null) {// 如果备份盘仍然不存在该文件
				System.out.println("磁盘均已损坏，数据获取失败");
			}
		}
	}

}
