package raidZeroOne;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;

/**
 * 模拟RAID01数据恢复
 * 
 * @author zhz
 */
public class RAID01Restore {

	private ArrayList<String> r0DiskPath;// raid0磁盘路径（数据分为几份）
	private ArrayList<String> r1DiskPath;// raid1磁盘路径（复制了几份）
	private ArrayList<String> backupDisk;// 备份数据的空磁盘

	public RAID01Restore(ArrayList<String> r0DiskPath, ArrayList<String> r1DiskPath, ArrayList<String> backupDisk) {
		this.r0DiskPath = r0DiskPath;
		this.r1DiskPath = r1DiskPath;
		this.backupDisk = backupDisk;
	}

	/**
	 * 数据恢复，将损坏的数据盘的数据转移至空磁盘
	 * 
	 * @throws IOException
	 */
	public void restore() throws IOException {
		int r0dpSize = r0DiskPath.size();
		int r1dpSize = r1DiskPath.size();
		File[] fodersRestore = new File[r0dpSize];// 磁盘损坏前的r0的磁盘数据（文件夹）
		File foderRestore = null;
		for (int i = 0; i < r0dpSize; i++) {// 找到要恢复的文件夹
			foderRestore = new File(r0DiskPath.get(i));// 在r0盘中查找
			if (foderRestore.exists() && fodersRestore[i] == null) {
				fodersRestore[i] = foderRestore;
				continue;
			}

			for (int j = 0; j < r1dpSize / r0dpSize; j++) {// 在r1中查找
				foderRestore = new File(r1DiskPath.get(j * r0dpSize + i));
				if (foderRestore.exists() && fodersRestore[i] == null) {
					fodersRestore[i] = foderRestore;
					break;
				}
			}
		}

		File[] filesRetore;
		for (int i = 0; i < r0dpSize; i++) {// 恢复r0文件
			foderRestore = new File(r0DiskPath.get(i));
			if (!foderRestore.exists() && fodersRestore[i] != null) {// 判断要恢复的数据是否为空
				String buDiskName = backupDisk.get(backupDisk.size() - 1);// 将要变成数据盘的空备份盘名
				String newDataDiskName = buDiskName.replaceFirst("backup", "raid-0");// 新的数据盘路径
				
				filesRetore = fodersRestore[i].listFiles();
				for (int j = 0; j < filesRetore.length; j++) {
					File file = filesRetore[j];
					Files.copy(file.toPath(), new File(buDiskName + file.getName()).toPath());
				}
				
				File newDataFile = new File(buDiskName);// 重命名新数据盘
				newDataFile.renameTo(new File(newDataDiskName));
				r0DiskPath.set(i, newDataDiskName);
				backupDisk.remove(backupDisk.size() - 1);
			}
		}

		for (int i = 0; i < r1dpSize / r0dpSize; i++) {// 恢复r1
			for (int j = 0; j < r0dpSize; j++) {// 以r0sfp为一组进行恢复
				foderRestore = new File(r1DiskPath.get(i * r0dpSize + j));
				if (!foderRestore.exists() && fodersRestore[j] != null) {
					String buDiskName = backupDisk.get(backupDisk.size() - 1);// 将要变成数据盘的空备份盘名
					String newDataDiskName = buDiskName.replaceFirst("backup", "raid-1");// 新的数据盘路径
					
					filesRetore = fodersRestore[j].listFiles();
					for (int z = 0; z < filesRetore.length; z++) {
						File file = filesRetore[z];
						Files.copy(file.toPath(), new File(buDiskName + file.getName()).toPath());
					}
					
					File newDataFile = new File(buDiskName);// 重命名新数据盘
					newDataFile.renameTo(new File(newDataDiskName));
					r0DiskPath.set(i * r0dpSize + j, newDataDiskName);
					backupDisk.remove(backupDisk.size() - 1);
				}
			}
		}
		
		int failureNum = 0;
		for (int i = 0; i < r0dpSize; i++) {// 输出磁盘损坏情况
			if (fodersRestore[i] == null) {
				failureNum++;
				System.out.println("存储在磁盘" + r0DiskPath.get(i) + "的数据备份盘和该磁盘已经损坏，该部分数据已无法恢复");
			} else {
				System.out.println("存储在磁盘" + r0DiskPath.get(i) + "的数据已恢复");
			}
		}
		if (r0dpSize == failureNum) {
			System.out.println("磁盘已经全部损坏，数据恢复失败");
			return;
		}
	}

}
