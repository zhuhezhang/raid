package raidMain;

import java.io.IOException;
import java.util.Scanner;

import raidZero.RAID0Main;
import raidOne.RAID1Main;
import raidThree.RAID3Main;
import raidFive.RAID5Main;
import raidSix.RAID6Main;
import raidZeroOne.RAID01Main;
import raidOneZero.RAID10Main;

/**
 * RAID0，1，3，5，6，01，10主类，负责调用RAID进行测试
 * 
 * @author zhz
 */
public class Main {

	public static void main(String[] args) throws IOException, InterruptedException {
		Scanner scanner;
		breakPoint: while (true) {
			System.out.println("----------模拟磁盘阵列系统---------");
			System.out.println("1.raid0     2.raid1     3.raid3");
			System.out.println("4.raid5     5.raid6     6.raid01");
			System.out.println("7.raid10    0.退出");
			System.out.print("请选择：");
			scanner = new Scanner(System.in);
			String opt = scanner.nextLine();
			switch (opt) {
			case "1":
				RAID0Main.start(scanner);
				break;
			case "2":
				RAID1Main.start(scanner);
				break;
			case "3":
				RAID3Main.start(scanner);
				break;
			case "4":
				RAID5Main.start(scanner);
				break;
			case "5":
				RAID6Main.start(scanner);
				break;
			case "6":
				RAID01Main.start(scanner);
				break;
			case "7":
				RAID10Main.start(scanner);
				break;
			case "0":
				break breakPoint;
			default:
				System.out.println("请输入正确指令");
				break;
			}
		}
		scanner.close();
	}

}
