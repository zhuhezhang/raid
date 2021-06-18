package raidZeroOne;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

/**
 * raid01入口函数
 * 
 * @author zhz
 */
public class RAID01Main {

	public static void start(Scanner scanner) throws IOException, InterruptedException {
		String diskParentPath = "disk/raid01/";// 磁盘父路径
		ArrayList<String> r0DiskPath = new ArrayList<String>();// raid0磁盘路径（数据分为几份）
		ArrayList<String> r1DiskPath = new ArrayList<String>();// raid1磁盘路径（复制了几份）
		ArrayList<String> bakupDisk = new ArrayList<String>();// 备份数据的空磁盘

		String[] disks = new File(diskParentPath).list();
		for (int i = 0; i < disks.length; i++) {// 找出各个磁盘
			String diskName = disks[i];
			if (diskName.matches("raid-0-disk\\d+")) {
				r0DiskPath.add(diskParentPath + diskName + "/");
			} else if (diskName.matches("raid-1-disk\\d+")) {
				r1DiskPath.add(diskParentPath + diskName + "/");
			} else {
				bakupDisk.add(diskParentPath + diskName + "/");
			}
		}

		breakPoint: while (true) {
			System.out.println("--------模拟磁盘阵列raid01---------");
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
				RAID01Write raid01Write = new RAID01Write(r0DiskPath, r1DiskPath, srcFilePath);
				for (int i = 0; i < r0DiskPath.size(); i++) {// 有多少个raid0磁盘则启动多少个线程
					new Thread(raid01Write).start();
				}
				break;
			case "2":
				String desFilePath;
				System.out.print("请输入要读出的文件：");
				desFilePath = scanner.nextLine();
				String name = new File(desFilePath).getName();
				String type = "";
				if (name.lastIndexOf(".") == -1) {
				} else {
					type = name.substring(name.lastIndexOf("."));
					name = name.substring(0, name.lastIndexOf("."));
				}

				boolean flag = false;
				String[] filesName = new File(r0DiskPath.get(0)).list();
				for (int i = 0; i < filesName.length; i++) {
					if (filesName[i].matches(".*" + name + "-\\d+" + type)) {
						flag = true;
						break;
					}
				}
				if (!flag) {
					System.out.println("文件不存在");
					break breakPoint1;
				}
				RAID01Read raid01Read = new RAID01Read(r0DiskPath, r1DiskPath, desFilePath);
				for (int i = 0; i < r0DiskPath.size(); i++) {// 有多少个磁盘则启动多少个线程
					new Thread(raid01Read).start();
				}
				break;
			case "3":
				new RAID01Restore(r0DiskPath, r1DiskPath, bakupDisk).restore();
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
