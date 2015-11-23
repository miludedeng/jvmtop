package com.jvmtop.monitor;

import sun.jvmstat.monitor.MonitorException;
import sun.jvmstat.monitor.MonitoredHost;
import sun.jvmstat.monitor.MonitoredVm;
import sun.jvmstat.monitor.VmIdentifier;
import sun.tools.jstat.Arguments;
import sun.tools.jstat.OptionFormat;
import sun.tools.jstat.OptionOutputFormatter;
import sun.tools.jstat.OutputFormatter;

/**
 * Application to output jvmstat statistics exported by a target Java Virtual
 * Machine. The jstat tool gets its inspiration from the suite of 'stat' tools,
 * such as vmstat, iostat, mpstat, etc., available in various UNIX platforms.
 *
 * @author Brian Doherty
 * @version 1.2, 03/01/04
 * @since 1.5
 */
public class JstatInfo {

	static final String	arg	= "-gcutil";
	private String		vmid_;
	private String		header;
	private String		row;

	public JstatInfo(String vmID) {
		try {
			vmid_ = vmID;
			init( vmID );
		} catch (MonitorException e) {
			e.printStackTrace();
		}
	}

	public JstatInfo(int vmID) {
		try {
			vmid_ = vmID + "";
			init( vmid_ );
		} catch (MonitorException e) {
			e.printStackTrace();
		}
	}

	public void update(){
		try {
			init(vmid_);
		} catch (MonitorException e) {
			e.printStackTrace();
		}
	}

	private void init(String vmID) throws MonitorException {
		Arguments arguments = new Arguments( new String[] { arg, vmID } );
		final VmIdentifier vmId = arguments.vmId();
		int interval = arguments.sampleInterval();
		final MonitoredHost monitoredHost = MonitoredHost.getMonitoredHost( vmId );
		MonitoredVm monitoredVm = monitoredHost.getMonitoredVm( vmId, interval );
		OutputFormatter formatter = null;

		OptionFormat format = arguments.optionFormat();
		formatter = new OptionOutputFormatter( monitoredVm, format );
		setHeader( formatter.getHeader() );
		setRow( formatter.getRow() );
	}

	public static void main(String[] args) {
		JstatInfo info = new JstatInfo( "1427" );
		System.out.println( info.getRow() );
	}

	/**
	 * @return the header
	 */
	public String getHeader() {
		return header;
	}

	/**
	 * @param header
	 *            the header to set
	 */
	public void setHeader(String header) {
		this.header = header;
	}

	/**
	 * @return the row
	 */
	public String getRow() {
		return row;
	}

	/**
	 * @param row
	 *            the row to set
	 */
	public void setRow(String row) {
		this.row = row;
	}
}
