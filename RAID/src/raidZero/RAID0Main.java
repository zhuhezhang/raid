package raidZero;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

/**
 * raid0入口函数
 * 
 * @author zhz
 */
public class RAID0Main {

	public static void start(Scanner scanner) throws IOException {
		String diskParentPath = "disk/raid0/";// raid0磁盘父路径
		ArrayList<String> diskPath = new ArrayList<String>();// raid0磁盘路径
		String[] disks = new File(diskParentPath).list();
		for (int i = 0; i < disks.length; i++) {
			diskPath.add(diskParentPath + disks[i] + "/");
		}

		breakPoint: while (true) {
			System.out.println("----模拟磁盘阵列raid0----");
			System.out.println("1.写     2.读     0.退出");
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
				RAID0Write raid0Write = new RAID0Write(diskPath, srcFilePath);
				for (int i = 0; i < diskPath.size(); i++) {// 有多少个磁盘则启动多少个线程
					new Thread(raid0Write).start();
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
				String[] filesName = new File(diskPath.get(0)).list();
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

				RAID0Read raid0Read = new RAID0Read(diskPath, desFilePath);
				for (int i = 0; i < diskPath.size(); i++) {// 有多少个磁盘则启动多少个线程
					new Thread(raid0Read).start();
				}
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
