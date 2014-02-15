package com.magic09.magicnetworkexplorer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jcifs.netbios.NbtAddress;

import org.apache.http.conn.util.InetAddressUtils;

import com.magic09.magicfileexplorer.R;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

/**
 * MagicNetworkExplorer provides a simple local (WiFi) network attached device
 * selection activity that returns the ip and name of the device selected.
 * @author dream09
 *
 */
public class MagicNetworkExplorer extends ListActivity {
	
	static final String TAG = "MainActivity";
	
	/* Variables */
	public static final int NETWORK_REQUEST = 0;
	public static final String KEY_SEND_TITLE = "title";
	public static final String KEY_RETURN_IPADDRESS = "ipaddress";
	public static final String KEY_RETURN_NAME = "name";
	
	NetworkScanner scanner;
	NetworkScanResultAdapter adapter;
	
	String myIPAddress;
	
	List<Map<String, String>> IPAddresses;
	
	private Menu actionMenu;
	
	/* Overridden methods */
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		// Set title to sent title.
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			String myTitle = extras.getString(KEY_SEND_TITLE);
			if (myTitle != null && myTitle.length() > 0)
				this.setTitle(myTitle);
		}
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		//Log.d(TAG, "onResume");
		
		// Start initial scan.
		startNetworkScanner();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		
		//Log.d(TAG, "onPause");
		
		if (scanner != null) {
			//Log.d(TAG, "cancelling scanner");
			scanner.cancel(true);
			cleanUpNetworkScanner();
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		
		this.actionMenu = menu;
		if (scanner != null)
			setRefreshActionButtonState(true);
		
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		
		int itemId = item.getItemId();
		if (itemId == R.id.action_refresh) {
			startNetworkScanner();
		}
		
		return super.onOptionsItemSelected(item);
	}
	
	
	
	/* Methods */
	
	/**
	 * Method shows the indeterminate progess circle in the
	 * action bar if refreshing is true else hides it.
	 * @param refreshing
	 */
	private void setRefreshActionButtonState (final boolean refreshing) {
		if (actionMenu != null) {
			final MenuItem refreshItem = actionMenu.findItem(R.id.action_refresh);
			if (refreshItem != null) {
				if (refreshing) {
					refreshItem.setActionView(R.layout.actionbar_indeterminate_progress);
				} else {
					refreshItem.setActionView(null);
				}
			}
		}
	}
	
	/**
	 * Method gets the ipaddress and starts the network 
	 * scan based on this.
	 */
	private void startNetworkScanner() {
		
		//Log.d(TAG, "Starting network scanner");
		
		// Get our ipaddress.
		myIPAddress = getMyIPAddress();
		if (myIPAddress != null) {
			TextView ipaddressTV = (TextView) findViewById(R.id.main_ipaddress);
			ipaddressTV.setText(myIPAddress);
		} else {
			TextView ipaddressTV = (TextView) findViewById(R.id.main_ipaddress);
			ipaddressTV.setText(getString(R.string.main_ipaddress_error));
			return;
		}
		
		// Check if wifi connected.
		ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		if (!mWifi.isConnected()) {
			setWifiError();
			return;
		}
		
		// Start scanner if not running.
		if (scanner != null)
			return;
		
		scanner = new NetworkScanner();
		scanner.execute(new String[] {myIPAddress});
	}
	
	/**
	 * Method cleans up the scanner.
	 */
	private void cleanUpNetworkScanner() {
		scanner = null;
		setRefreshActionButtonState(false);
		clearScanningIP();
	}
	
	/**
	 * Method displays results (ipaddresses found) in the list.
	 * @param result
	 */
	private void displayScanResult(List<Map<String, String>> result) {
		if (result != null && result.size() > 0) {
			IPAddresses = result;
			if (adapter == null){
				adapter = new NetworkScanResultAdapter(this, R.layout.scanner_ipaddress_list_view, IPAddresses);
				setListAdapter(adapter);
			} else {
				adapter.notifyDataSetChanged();
			}
		} else {
			Toast.makeText(this, getString(R.string.main_scanner_toast_noresults), Toast.LENGTH_SHORT).show();
		}
	}
	
	/**
	 * Method returns the IPAddress of the current device.
	 * @return
	 */
	private String getMyIPAddress() {
		
		String myIP = null;
		boolean useIPv4 = true;	// Force IPv4 as IPv6 not checked.
		try {
			List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
			for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress().toUpperCase();
                        boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr); 
                        if (useIPv4) {
                            if (isIPv4)
                            	myIP = sAddr;
                        } else {
                            if (!isIPv4) {
                                //int delim = sAddr.indexOf('%'); // drop ip6 port suffix
                                //return delim<0 ? sAddr : sAddr.substring(0, delim);
                            	//TODO: check out IPv6!
                            }
                        }
                    }
                }
            }
			
		} catch (SocketException e) {
			//e.printStackTrace();
		}
		
		//Log.d(TAG, "My ipaddress is " + myIP);
		
		return myIP;
	}
	
	/**
	 * Method updates the scanning status area
	 * with the argument.
	 * @param ip
	 */
	private void setScanningIP(CharSequence ip) {
		TextView tv = (TextView) findViewById(R.id.main_scanner_status);
		if (tv != null) {
			tv.setText(getString(R.string.main_scanner_status_scanning) + " " + ip);
		}
	}
	
	/**
	 * Method displays the wifi not connected error message.
	 */
	private void setWifiError() {
		TextView tv = (TextView) findViewById(R.id.main_scanner_status);
		if (tv != null) {
			tv.setText(getString(R.string.main_scanner_status_wifierror));
		}
	}
	
	/**
	 * Method sets the scanning status to idle.
	 */
	private void clearScanningIP() {
		TextView tv = (TextView) findViewById(R.id.main_scanner_status);
		if (tv != null) {
			tv.setText(getString(R.string.main_scanner_status_idle));
		}
	}
	
	/**
	 * Method handles a click on a device in the list.
	 */
	protected void onListItemClick(android.widget.ListView l, android.view.View v, int position, long id) {
		HashMap<String, String> o = (HashMap<String, String>) adapter.getItem(position);
		Intent data = new Intent();
		data.putExtra(KEY_RETURN_IPADDRESS, o.get(NetworkScanResultAdapter.KEY_IPADDRESS));
		data.putExtra(KEY_RETURN_NAME, o.get(NetworkScanResultAdapter.KEY_NAME));
		setResult(RESULT_OK, data);
		finish();
	};
	
	
	
	/**
	 * NetworkScanner provides a network scanner off the UI thread.
	 * it returns a list containing maps (IP address and name) of devices found.
	 * @author dream09
	 *
	 */
	private class NetworkScanner extends AsyncTask<String, Void, List<Map<String, String>>> {
		
		//static final String TAG = "NetworkScanner";
		
		/* Variables */
		public static final int SCAN_TIMEOUT = 200;
		private List<Map<String, String>> result;
		private String currentHost;
		
		
		
		/* Overridden methods */
		
		@Override
		protected List<Map<String, String>> doInBackground(String... subnetList) {
			
			// Get subnet.
			String ipAddress = subnetList[0];
			String subnet = ipAddress.substring(0, ipAddress.lastIndexOf("."));
			
			// Prepare return.
			result = new ArrayList<Map<String,String>>();
			
			// Loop through all possible values.
			for (int i=1; i<=255; i++) {
				
				// Continually check if we've been cancelled and handle.
				if (isCancelled()) {
					//Log.d(TAG, "We've been canceled!");
					break;
				}
				
				// Setup host to check.
				currentHost = subnet + "." + i;
				if (currentHost.equals(ipAddress))
					continue;
				
				// Update status display.
				publishProgress();
				//System.out.println("scanning " + host);
				
				// Check if we can reach this address.
				boolean reachable = false;
				try {
					InetAddress checkAddress = InetAddress.getByName(currentHost);
					try {
						// This method fails on Windows.
						reachable = checkAddress.isReachable(SCAN_TIMEOUT);
						if (!reachable) {
							// Check for Windows CIFS (default port 139).
							try {
								Socket sock = new Socket();
								sock.connect(new InetSocketAddress(currentHost, 139), SCAN_TIMEOUT);
								if (sock.isConnected()) {
									sock.close();
									reachable = true;
								}
							} catch (IOException e) {
								//System.out.println(host + " is not reachable (socket exception)");
							}
						}
					} catch (IOException e) {
						//System.out.println("Error checking if " + host + " is reachable");
					}
				} catch (UnknownHostException e1) {
					//System.out.println("Error getting by name " + host);
				}
				
				// If reachable get details and store.
				if (reachable) {
					try {
						NbtAddress addressInfo = NbtAddress.getByName(currentHost);
						
						boolean ipActive;
						try {
							ipActive = addressInfo.isActive();
						} catch (Exception e) {
							ipActive = false;
						}
						
						if (ipActive) {
							HashMap<String, String> addressToSave = new HashMap<String, String>();
							addressToSave.put(NetworkScanResultAdapter.KEY_NAME, addressInfo.getHostName());
							addressToSave.put(NetworkScanResultAdapter.KEY_IPADDRESS, currentHost);
							result.add(addressToSave);
							publishProgress();
						}
						
					} catch (UnknownHostException e) {
					}
				}
			}
			
			return result;
		}
		
		@Override
		protected void onPreExecute() {
			setRefreshActionButtonState(true);
		}
		
		@Override
		protected void onPostExecute(List<Map<String, String>> result) {
			cleanUpNetworkScanner();
			displayScanResult(result);
		}
		
		@Override
		protected void onProgressUpdate(Void... values) {
			setScanningIP(currentHost);
			displayScanResult(result);
		}
	}
	
}
