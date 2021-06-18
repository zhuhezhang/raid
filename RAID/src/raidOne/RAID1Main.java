package raidOne;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

/**
 * raid1入口函数
 * 
 * @author zhz
 */
public class RAID1Main {

	public static void start(Scanner scanner) throws IOException {
		String diskParentPath = "disk/raid1/";// 磁盘父路径
		ArrayList<String> dataDiskPath = new ArrayList<String>();// 磁盘路径
		ArrayList<String> buDiskPath = new ArrayList<String>();// 备份空磁盘路径

		String[] disks = new File(diskParentPath).list();
		for (int i = 0; i < disks.length; i++) {// 找出数据盘和空备份盘
			if (disks[i].matches("data-disk\\d+")) {
				dataDiskPath.add(diskParentPath + disks[i] + "/");
			} else {
				buDiskPath.add(diskParentPath + disks[i] + "/");
			}
		}

		breakPoint: while (true) {
			System.out.println("---------模拟磁盘阵列raid1---------");
			System.out.println("1.写     2.读     3.恢复     0.退出");
			System.out.print("请选择：");
			String opt = scanner.nextLine();
			breakPoint1: switch (opt) {
			case "1":
				String srcFilePath;
				System.out.print("请输入要写入的文件：");
				srcFilePath = scanner.nextLine();
				if (!new File(srcFilePath).exists()) {
					System.out.println("文件不存在");
					break breakPoint1;
				}
				RAID1Write raid1Write = new RAID1Write(dataDiskPath, srcFilePath);
				for (int i = 0; i < dataDiskPath.size(); i++) {// 有多少个磁盘则启动多少个线程
					new Thread(raid1Write).start();
				}
				break;
			case "2":
				String desFilePath;
				System.out.print("请输入要读出的文件：");
				desFilePath = scanner.nextLine();
				String name = new File(desFilePath).getName();

				boolean flag = false;
				String[] filesName = new File(dataDiskPath.get(0)).list();
				for (int i = 0; i < filesName.length; i++) {
					if (filesName[i].matches(".*" + name)) {
						flag = true;
						break;
					}
				}
				if (!flag) {
					System.out.println("文件不存在");
					break breakPoint1;
				}
				new RAID1Read(dataDiskPath, desFilePath).download();
				break;
			case "3":
				new RAID1Restore(dataDiskPath, buDiskPath).restore();
				break;
			case "0":
				break breakPoint;
			default:
				System.out.println("请输入正确指令");
				break;
			}
		}
	}

}
