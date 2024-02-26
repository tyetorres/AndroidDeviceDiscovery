package com.tyetorresdev.alldevicesgetter;

import static android.content.ContentValues.TAG;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.print.PrintManager;
import android.print.PrintJob;
import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;

public class PluginInstance {
    private static String _serviceName = "NsdChat";
    private static final String SERVICE_TYPE = "_http._tcp.";
    private int _port;
    private InetAddress _host;
    private NsdManager _nsdManager;
    private NsdManager.DiscoveryListener _discoveryListener;
    private NsdManager.RegistrationListener _registrationListener;
    private NsdManager.ResolveListener _resolveListener;
    private NsdServiceInfo _serviceInfo;
    private ServerSocket _serverSocket;
    private Activity unityActivity;
    public int count;
    public List<NsdServiceInfo> ips;
    public static PluginInstance instance;

    public PluginInstance() {
        instance = this;
    }
    public static String pingPlugin() {
        return "Pinged!";
    }

    public static void receiveUnityActivity(Activity tActivity){
        instance.unityActivity = tActivity;
    }

    public boolean CheckForPermission(String permission) {
        return unityActivity.getApplicationContext().checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
    }

    public void RequestPermission(String permission) {
        unityActivity.requestPermissions(new String[]{permission},1024);
    }

    // request permissions for printer discovery:
    public void SetInternetAndMulticastPermissions() {
        // internet / network permissions:
        RequestPermission("android.permission.INTERNET");
        RequestPermission("android.permission.NEARBY_WIFI_DEVICES");
        RequestPermission("android.permission.ACCESS_NETWORK_STATE");
        RequestPermission("android.permission.ACCESS_WIFI_STATE");
        RequestPermission("android.permission.CHANGE_NETWORK_STATE");
        RequestPermission("android.permission.CHANGE_WIFI_MULTICAST_STATE");
        RequestPermission("android.permission.CHANGE_WIFI_STATE");
        RequestPermission("android.permission.GLOBAL_SEARCH");

        // used to opt in to metric collection:
        RequestPermission("android.permission.PACKAGE_USAGE_STATS");

        // internal / external storage
        RequestPermission("android.permission.READ_EXTERNAL_STORAGE");

        // location / file / print
        RequestPermission("android.permission.ACCESS_MEDIA_LOCATION");
        RequestPermission("android.permission.BIND_PRINT_SERVICE");
    }

    public void GetPrinters()
    {
        // Get a PrintManager instance
        PrintManager printManager = (PrintManager) unityActivity.getSystemService(Context.PRINT_SERVICE);
    }

    public void startListening() {
        count = 0;
        ips = new ArrayList<>();
        initializeServerSocket();
        initializeRegistrationListener();
        registerService(_port);
        initializeResolveListener();
        initializeDiscoveryListener();

        _nsdManager = (NsdManager) unityActivity.getSystemService(Context.NSD_SERVICE);
        _nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, _discoveryListener);
    }

    public String printDeviceIp(int id)
    {
        if ((NsdServiceInfo)ips.get(id) != null) {
            NsdServiceInfo info = (NsdServiceInfo)ips.get(id);
            return info.getHost().getHostAddress();
        }
        return null;
    }

    public String printDeviceName(int id)
    {
        if ((NsdServiceInfo)ips.get(id) != null) {
            NsdServiceInfo info = (NsdServiceInfo)ips.get(id);
            return info.getHost().getHostName();
        }
        return null;
    }

    public void stopListening() {
        _nsdManager.unregisterService(_registrationListener);
        _nsdManager.stopServiceDiscovery(_discoveryListener);
    }

    public void initializeServerSocket() {
        // Initialize a server socket on the next available port.
        try {
            _serverSocket = new ServerSocket(0);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Store the chosen port.
        _port = _serverSocket.getLocalPort();
    }

    public void registerService(int port) {
        _serviceInfo = new NsdServiceInfo();
        _serviceInfo.setServiceName("NsdChat");
        _serviceInfo.setServiceType("_http._tcp.");
        _serviceInfo.setPort(port);

        _nsdManager = (NsdManager) unityActivity.getSystemService(Context.NSD_SERVICE);

        _nsdManager.registerService(_serviceInfo, NsdManager.PROTOCOL_DNS_SD, _registrationListener);
    }

    public void initializeDiscoveryListener() {

        // Instantiate a new DiscoveryListener
        _discoveryListener = new NsdManager.DiscoveryListener() {

            // Called as soon as service discovery begins.
            @Override
            public void onDiscoveryStarted(String regType) {
                Log.d(TAG, "Service discovery started");
            }

            @Override
            public void onServiceFound(NsdServiceInfo service) {
                // A service was found! Do something with it.
                Log.d(TAG, "Service discovery success" + service);
                if (!service.getServiceType().equals(SERVICE_TYPE)) {
                    // Service type is the string containing the protocol and
                    // transport layer for this service.
                    Log.d(TAG, "Unknown Service Type: " + service.getServiceType());
                } else if (service.getServiceName().equals(_serviceName)) {
                    // The name of the service tells the user what they'd be
                    // connecting to. It could be "Bob's Chat App".
                    Log.d(TAG, "Same machine: " + _serviceName);
                } else if (service.getServiceName().contains("NsdChat")){
                    _nsdManager.resolveService(service, _resolveListener);
                }
            }

            @Override
            public void onServiceLost(NsdServiceInfo service) {
                // When the network service is no longer available.
                // Internal bookkeeping code goes here.
                Log.e(TAG, "service lost: " + service);
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.i(TAG, "Discovery stopped: " + serviceType);
            }

            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code:" + errorCode);
                _nsdManager.stopServiceDiscovery(this);
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code:" + errorCode);
                _nsdManager.stopServiceDiscovery(this);
            }
        };
    }

    public void initializeRegistrationListener() {
        _registrationListener = new NsdManager.RegistrationListener() {

            @Override
            public void onServiceRegistered(NsdServiceInfo NsdServiceInfo) {
                // Save the service name. Android may have changed it in order to
                // resolve a conflict, so update the name you initially requested
                // with the name Android actually used.
                _serviceName = NsdServiceInfo.getServiceName();
            }

            @Override
            public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                // Registration failed! Put debugging code here to determine why.
            }

            @Override
            public void onServiceUnregistered(NsdServiceInfo arg0) {
                // Service has been unregistered. This only happens when you call
                // NsdManager.unregisterService() and pass in this listener.
            }

            @Override
            public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                // Unregistration failed. Put debugging code here to determine why.
            }
        };
    }

    public void initializeResolveListener() {
        _resolveListener = new NsdManager.ResolveListener() {

            @Override
            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                // Called when the resolve fails. Use the error code to debug.
                Log.e(TAG, "Resolve failed: " + errorCode);
            }

            @Override
            public void onServiceResolved(NsdServiceInfo serviceInfo) {
                Log.e(TAG, "Resolve Succeeded. how to" + serviceInfo);

                if (serviceInfo.getServiceName().equals(_serviceName)) {
                    Log.d(TAG, "Same IP.");
                    return;
                }
                count = count + 1;
                _serviceInfo = serviceInfo;
                int port = _serviceInfo.getPort();
                InetAddress host = _serviceInfo.getHost();
                ips.add(count, serviceInfo);

                Log.i(TAG, host.toString() + " Port: " + port);
            }
        };
    }
}

