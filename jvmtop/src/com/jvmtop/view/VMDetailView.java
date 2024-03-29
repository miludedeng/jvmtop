/**
 * jvmtop - java monitoring for the command-line
 *
 * Copyright (C) 2013 by Patric Rufflar. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package com.jvmtop.view;

import java.lang.management.ThreadInfo;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.jvmtop.monitor.JstatInfo;
import com.jvmtop.monitor.VMInfo;
import com.jvmtop.monitor.VMInfoState;
import com.jvmtop.openjdk.tools.LocalVirtualMachine;

/**
 * "detail" view, printing detail metrics of a specific jvm. Also printing the
 * top threads (based on the current CPU usage)
 *
 * @author paru
 *
 */
public class VMDetailView extends AbstractConsoleView {

	private VMInfo			vmInfo_;

	private JstatInfo		jstatInfo_;

	private boolean			sortByTotalCPU_			= false;

	private Map<Long, Long>	previousThreadCPUMillis	= new HashMap<Long, Long>();

	public VMDetailView(int vmid) throws Exception {
		LocalVirtualMachine localVirtualMachine = LocalVirtualMachine.getLocalVirtualMachine( vmid );
		vmInfo_ = VMInfo.processNewVM( localVirtualMachine, vmid );
		jstatInfo_ = new JstatInfo( vmid );
	}

	public boolean isSortByTotalCPU() {
		return sortByTotalCPU_;
	}

	public void setSortByTotalCPU(boolean sortByTotalCPU) {
		sortByTotalCPU_ = sortByTotalCPU;
	}

	@Override
	public void printView() throws Exception {
		vmInfo_.update();
		jstatInfo_.update();
		if (vmInfo_.getState() == VMInfoState.ATTACHED_UPDATE_ERROR) {
			System.out.println( "ERROR: Could not fetch telemetries - Process terminated?" );
			exit();
			return;
		}
		if (vmInfo_.getState() != VMInfoState.ATTACHED) {
			System.out.println( "ERROR: Could not attach to process." );
			exit();
			return;
		}

		Map<String, String> properties = vmInfo_.getSystemProperties();

		String command = properties.get( "sun.java.command" );
		if (command != null) {
			String[] commandArray = command.split( " " );

			List<String> commandList = Arrays.asList( commandArray );
			commandList = commandList.subList( 1, commandList.size() );

			System.out.printf( " PID %d: %s %n", vmInfo_.getId(), commandArray[ 0 ] );

			String argJoin = join( commandList, " " );
			if (argJoin.length() > 67) {
				System.out.printf( " ARGS: %s[...]%n", leftStr( argJoin, 67 ) );
			} else {
				System.out.printf( " ARGS: %s%n", argJoin );
			}
		} else {
			System.out.printf( " PID %d: %n", vmInfo_.getId() );
			System.out.printf( " ARGS: [UNKNOWN] %n" );
		}

		String join = join( vmInfo_.getRuntimeMXBean().getInputArguments(), " " );
		if (join.length() > 65) {
			System.out.printf( " VMARGS: %s[...]%n", leftStr( join, 65 ) );
		} else {
			System.out.printf( " VMARGS: %s%n", join );
		}

		System.out.printf( " VM: %s %s %s%n", properties.get( "java.vendor" ), properties.get( "java.vm.name" ), properties.get( "java.version" ) );
		System.out.printf( " UP: %-7s #THR: %-4d #THRPEAK: %-4d #THRCREATED: %-4d USER: %-12s%n", toHHMMSS( vmInfo_.getRuntimeMXBean().getUptime() ),
				vmInfo_.getThreadCount(), vmInfo_.getThreadMXBean().getPeakThreadCount(), vmInfo_.getThreadMXBean().getTotalStartedThreadCount(),
				vmInfo_.getOSUser() );

		System.out.printf( " GC-Time: %-7s  #GC-Runs: %-8d  #TotalLoadedClasses: %-8d%n", toHHMMSS( vmInfo_.getGcTime() ), vmInfo_.getGcCount(),
				vmInfo_.getTotalLoadedClassCount() );

		System.out.printf( " CPU: %5.2f%% GC: %5.2f%% HEAP:%5s /%5s NONHEAP:%5s /%5s%n", vmInfo_.getCpuLoad() * 100, vmInfo_.getGcLoad() * 100,
				toMB( vmInfo_.getHeapUsed() ), toMB( vmInfo_.getHeapMax() ), toMB( vmInfo_.getNonHeapUsed() ), toMB( vmInfo_.getNonHeapMax() ) );

		System.out.printf( " Eden: %s/%s(%s) from: %s/%s(%s) to: %s/%s(%s)\r\n Old: %s/%s(%s)", 
				toMB(Long.parseLong( jstatInfo_.getEdenUsed())), toMB(Long.parseLong(jstatInfo_.getEdenCapacity())),toMB(Long.parseLong( jstatInfo_.getEdenMaxCapacity())),
				toMB(Long.parseLong( jstatInfo_.getSurvival1Used())), toMB(Long.parseLong(jstatInfo_.getSurvival1Capacity())),toMB(Long.parseLong(jstatInfo_.getSurvival1MaxCapacity())),
				toMB(Long.parseLong( jstatInfo_.getSurvival2Used())), toMB(Long.parseLong(jstatInfo_.getSurvival2Capacity())),toMB(Long.parseLong(jstatInfo_.getSurvival2MaxCapacity())),
				toMB(Long.parseLong( jstatInfo_.getOldUsed())), toMB(Long.parseLong(jstatInfo_.getOldCapacity())), toMB(Long.parseLong(jstatInfo_.getOldMaxCapacity())));
		if("NaN".equals( jstatInfo_.getPermUsed())){
			System.out.printf( " Metaspace: %s/%s(%s)", toMB(Long.parseLong( jstatInfo_.getMetaUsed())), toMB(Long.parseLong(jstatInfo_.getMetaCapacity())), toMB(Long.parseLong(jstatInfo_.getMetaMaxCapacity())));
		}else{
			System.out.printf( " Perm: %s/%s", toMB(Long.parseLong( jstatInfo_.getPermUsed())), toMB(Long.parseLong(jstatInfo_.getPermCapacity())), toMB(Long.parseLong(jstatInfo_.getPermMaxCapacity())));
		}
		System.out.println();
		printTopThreads();
	}


	/**
	 * @throws Exception
	 */
	private void printTopThreads() throws Exception {
		System.out.println( "  Thread count: " + vmInfo_.getThreadCount() );
		System.out.printf( "  TID   NAME                                    STATE    CPU  TOTALCPU BLOCKEDBY%n" );

		if (vmInfo_.getThreadMXBean().isThreadCpuTimeSupported()) {
			Map<Long, Long> newThreadCPUMillis = new HashMap<Long, Long>();

			Map<Long, Long> cpuTimeMap = new TreeMap<Long, Long>();

			for (Long tid : vmInfo_.getThreadMXBean().getAllThreadIds()) {
				long threadCpuTime = vmInfo_.getThreadMXBean().getThreadCpuTime( tid );
				long deltaThreadCpuTime = 0;
				if (previousThreadCPUMillis.containsKey( tid )) {
					deltaThreadCpuTime = threadCpuTime - previousThreadCPUMillis.get( tid );

					cpuTimeMap.put( tid, deltaThreadCpuTime );
				}
				newThreadCPUMillis.put( tid, threadCpuTime );
			}

			cpuTimeMap = sortByValue( cpuTimeMap, true );

			int displayedThreads = 0;
			for (Long tid : cpuTimeMap.keySet()) {
				ThreadInfo info = vmInfo_.getThreadMXBean().getThreadInfo( tid );
				displayedThreads++;
				if (displayedThreads > 15) {
					break;
				}
				if (info != null) {
					System.out.printf( " %6d %-30s  %13s %5.2f%%    %5.2f%% %5s %n", tid, leftStr( info.getThreadName(), 30 ), info.getThreadState(),
							getThreadCPUUtilization( cpuTimeMap.get( tid ), vmInfo_.getDeltaUptime() ),
							getThreadCPUUtilization( vmInfo_.getThreadMXBean().getThreadCpuTime( tid ), vmInfo_.getProxyClient().getProcessCpuTime(),
									1 ),
							getBlockedThread( info ) );
				}
			}
			if (newThreadCPUMillis.size() >= 15) {

				System.out.println( " Note: Only top 15 threads (according cpu load) are shown!" );
			}
			previousThreadCPUMillis = newThreadCPUMillis;
		} else {

			System.out.printf( "%n -Thread CPU telemetries are not available on the monitored jvm/platform-%n" );
		}
	}

	private String getBlockedThread(ThreadInfo info) {
		if (info.getLockOwnerId() >= 0) {
			return "" + info.getLockOwnerId();
		} else {
			return "";
		}
	}

	private double getThreadCPUUtilization(long deltaThreadCpuTime, long totalTime) {
		return getThreadCPUUtilization( deltaThreadCpuTime, totalTime, 1000 * 1000 );
	}

	private double getThreadCPUUtilization(long deltaThreadCpuTime, long totalTime, double factor) {
		if (totalTime == 0) {
			return 0;
		}
		return deltaThreadCpuTime / factor / totalTime * 100d;
	}
}
