package raidOneZero;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

/**
 * raid10入口函数
 * 
 * @author zhz
 */
public class RAID10Main {

	public static void start(Scanner scanner) throws IOException, InterruptedException {
		String diskParentPath = "disk/raid10/";// 磁盘父路径
		ArrayList<ArrayList<String>> dataDiskPath = new ArrayList<ArrayList<String>>();// 磁盘路径，一维数表示raid0的盘数（数据分为几份），二维数表示raid1的盘数（复制几份）
		ArrayList<String> buDiskPath = new ArrayList<String>();// 备份空磁盘路径

		String[] disks = new File(diskParentPath).list();
		int blockNum = 0;// 磁盘阵列中的文件块数(0开始)
		for (int i = 0; i < disks.length; i++) {// 计算出文件在磁盘阵列中的块数
			String diskName = disks[i];
			if (diskName.matches("data-disk\\d+-\\d+")) {
				int pos = Integer.parseInt(diskName.substring(diskName.lastIndexOf("-") + 1));
				blockNum = pos > blockNum ? pos : blockNum;
			}
		}
		for (int i = 0; i <= blockNum; i++) {// 初始化
			dataDiskPath.add(new ArrayList<String>());
		}
		for (int i = 0; i < disks.length; i++) {// 找出数据盘和空备份盘
			String diskName = disks[i];
			if (diskName.matches("data-disk\\d+-\\d+")) {
				int pos = Integer.parseInt(diskName.substring(diskName.lastIndexOf("-") + 1));
				dataDiskPath.get(pos).add(diskParentPath + diskName + "/");
			} else {
				buDiskPath.add(diskParentPath + diskName + "/");
			}
		}

		breakPoint: while (true) {
			System.out.println("--------模拟磁盘阵列raid10---------");
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
				RAID10Write raid10Write = new RAID10Write(dataDiskPath, srcFilePath);
				for (int i = 0; i < dataDiskPath.size(); i++) {// 有多少个raid0磁盘则启动多少个线程
					new Thread(raid10Write).start();
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
				String[] filesName = new File(dataDiskPath.get(0).get(0)).list();
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
				RAID10Read raid10Read = new RAID10Read(dataDiskPath, desFilePath);
				for (int i = 0; i < dataDiskPath.size(); i++) {// 有多少个磁盘则启动多少个线程
					new Thread(raid10Read).start();
				}
				break;
			case "3":
				new RAID10Restore(dataDiskPath, buDiskPath).restore();
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
