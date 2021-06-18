package raidOneZero;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;

/**
 * 模拟RAID10数据恢复
 * 
 * @author zhz
 */
public class RAID10Restore {

	private ArrayList<ArrayList<String>> r10disk;// 数据磁盘路径，一维数表示raid0的盘数（数据分为几份），二维数表示raid1的盘数（复制几份）
	private ArrayList<String> budp;// backupDiskPath，备份数据的空磁盘路径

	public RAID10Restore(ArrayList<ArrayList<String>> r10disk, ArrayList<String> budp) {
		this.r10disk = r10disk;
		this.budp = budp;
	}

	/**
	 * 数据恢复，将损坏的数据盘的数据转移至空磁盘
	 * 
	 * @throws IOException
	 */
	public void restore() throws IOException {
		int r10diskSize = r10disk.size();
		File[] disksRestore = new File[r10diskSize];// 找出要恢复的磁盘数据
		continuePoint: for (int i = 0; i < r10diskSize; i++) {
			for (int j = 0; j < r10disk.get(i).size(); j++) {
				File disk = new File(r10disk.get(i).get(j));
				if (disk.exists()) {
					disksRestore[i] = disk;
					continue continuePoint;
				}
			}
		}

		File[] filesRetore;
		for (int i = 0; i < r10diskSize; i++) {// 恢复数据
			for (int j = 0; j < r10disk.get(i).size(); j++) {
				File diskRestore = new File(r10disk.get(i).get(j));
				if (!diskRestore.exists() && disksRestore[i] != null) {
					String buDiskName = budp.get(budp.size() - 1);// 将要变成数据盘的空备份盘名
					String dataDiskName = r10disk.get(i).get(j);// 坏掉的数据盘路径
					int pos = Integer.parseInt(
							dataDiskName.substring(dataDiskName.lastIndexOf("-") + 1, dataDiskName.length() - 1));
					String tmp = buDiskName.replaceFirst("backup", "data");
					String newDataDiskName = tmp.substring(0, tmp.length() - 1) + "-" + pos + "/";// 新的数据盘名

					filesRetore = disksRestore[i].listFiles();
					for (int k = 0; k < filesRetore.length; k++) {
						File file = filesRetore[k];
						Files.copy(file.toPath(), new File((buDiskName) + file.getName()).toPath());
					}

					File newDataFile = new File(buDiskName);// 重命名新数据盘
					newDataFile.renameTo(new File(newDataDiskName));
					r10disk.get(i).set(j, newDataDiskName);
					budp.remove(budp.size() - 1);
				}
			}
		}

		int failureNum = 0;
		for (int i = 0; i < r10diskSize; i++) {// 输出磁盘损坏情况
			if (disksRestore[i] == null) {
				failureNum++;
				System.out.println("存储在磁盘" + r10disk.get(i) + "的数据备份盘和该磁盘已经损坏，该部分数据已无法恢复");
			} else {
				System.out.println("存储在磁盘" + r10disk.get(i) + "的数据已恢复");
			}
		}
		if (r10diskSize == failureNum) {
			System.out.println("磁盘已经全部损坏，数据恢复失败");
		}
	}

}
