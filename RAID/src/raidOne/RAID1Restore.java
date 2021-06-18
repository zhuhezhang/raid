package raidOne;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;

/**
 * 模拟RAID1数据恢复
 * 
 * @author zhz
 */
public class RAID1Restore {

	private ArrayList<String> dataDiskPath;// 数据磁盘路径
	private ArrayList<String> buDiskPath;// 空磁盘路径，用于存储恢复的数据

	public RAID1Restore(ArrayList<String> dataDiskPath, ArrayList<String> buDiskPath) {
		this.dataDiskPath = dataDiskPath;
		this.buDiskPath = buDiskPath;
	}

	/**
	 * 数据恢复，将损坏的数据盘的数据转移至空磁盘
	 * 
	 * @throws IOException
	 */
	public void restore() throws IOException {
		int dataFoldersNum = dataDiskPath.size();// 数据盘个数
		int failureNum = 0;// 故障盘个数
		for (int i = 0; i < dataDiskPath.size(); i++) {// 找到故障盘并移除
			if (!new File(dataDiskPath.get(i)).exists()) {
				dataDiskPath.remove(i);
				i--;
				failureNum++;
			}
		}

		if (failureNum == 0) {
			System.out.println("数据正常，不需要做数据恢复");
		} else if (buDiskPath.size() == 0) {
			System.out.println("没有多余的磁盘，数据恢复失败");
		} else if (failureNum == dataFoldersNum) {
			System.out.println("磁盘均已损坏，数据恢复失败");
		} else {// failureNum < dataFoldersNum
			if (failureNum > buDiskPath.size()) {// 如果空备份盘不够，则备份的个数为备份盘的个数
				failureNum = buDiskPath.size();
			}
			for (int i = 0; i < failureNum; i++) {// 根据故障盘个数恢复数据
				String buDiskName = buDiskPath.get(buDiskPath.size() - 1);// 将要变成数据盘的空备份盘名
				String newDataDiskName = buDiskName.replaceFirst("backup", "data");// 新的数据盘名

				File[] fileRestore = new File(dataDiskPath.get(0)).listFiles();
				for (int j = 0; j < fileRestore.length; j++) {// 将文件逐个复制到空备份盘
					Files.copy(fileRestore[i].toPath(), new File(buDiskName + fileRestore[i].getName()).toPath(),
							StandardCopyOption.REPLACE_EXISTING);
				}
				File newDataFile = new File(buDiskName);// 重命名新数据盘
				newDataFile.renameTo(new File(newDataDiskName));
				dataDiskPath.add(newDataDiskName);// 更新数据盘路径
				buDiskPath.remove(buDiskPath.size() - 1);// 更新备份盘路径
			}
			System.out.println("数据恢复成功");
		}
	}

}
