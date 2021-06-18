package raidThree;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * raid3入口函数
 * 
 * @author zhz
 */
public class RAID3Main {

	public static void start(Scanner scanner) throws IOException {
		String diskParentPath = "disk/raid3/";// raid3磁盘父路径
		ArrayList<String> dataDisk = new ArrayList<String>();// 数据磁盘路径
		ArrayList<String> parityCkechDisk = new ArrayList<>();// 奇偶校验盘路径
		ArrayList<String> backupDisk = new ArrayList<>();// 备份数据盘路径

		String[] disks = new File(diskParentPath).list();
		for (int i = 0; i < disks.length; i++) {// 找出各个磁盘路径
			if (disks[i].matches("data-disk\\d+")) {
				dataDisk.add(diskParentPath + disks[i] + "/");
			} else if (disks[i].matches("backup-disk\\d+")) {
				backupDisk.add(diskParentPath + disks[i] + "/");
			} else {
				parityCkechDisk.add(diskParentPath + disks[i] + "/");
			}
		}

		breakPoint: while (true) {
			System.out.println("----模拟磁盘阵列raid3----");
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

				int pointIndex = srcFilePath.lastIndexOf(".");
				String srcFileName = new File(srcFilePath).getName();// 源文件名
				String srcFileType;
				if (pointIndex == -1) {// 源文件没有后缀
					srcFileType = "";// 源文件后缀
				} else {
					srcFileName = srcFileName.substring(0, srcFileName.lastIndexOf("."));
					srcFileType = srcFilePath.substring(pointIndex);
				}
				String[] file = new File(dataDisk.get(0)).list();
				for (int i = 0; i < file.length; i++) {
					if (file[i].matches(".*" + srcFileName + "-\\d+" + srcFileType)
							|| file[i].matches(".*" + srcFileName + "-pcd" + srcFileType)) {
						System.out.println("文件已存在");
						break breakPoint1;
					}
				}

				ExecutorService threadPool = Executors.newCachedThreadPool();// 初始化线程池
				RAID3Write raid3Write = new RAID3Write(dataDisk, parityCkechDisk, srcFilePath);
				for (int i = 0; i < dataDisk.size(); i++) {// 有多少个磁盘则启动多少个线程
					threadPool.submit(new Thread(raid3Write));// 提交到线程池
				}
				threadPool.shutdown();// 执行完线程池里面的线程并关闭线程池，禁止提交新的线程
				while (true) {// 等待所有任务都执行结束
					if (threadPool.isTerminated()) {// 所有的子线程都结束了
						raid3Write.writeToParityCheckDisk();
						break;
					}
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
				String[] filesName = new File(dataDisk.get(0)).list();
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

				RAID3Read raid3Read = new RAID3Read(dataDisk, desFilePath);
				for (int i = 0; i < dataDisk.size(); i++) {// 有多少个磁盘则启动多少个线程
					new Thread(raid3Read).start();
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
